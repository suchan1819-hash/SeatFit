package com.zeroinjury.seatfit.routine

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zeroinjury.seatfit.R
import com.zeroinjury.seatfit.vehicle.VehicleBridge
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface Screen {
    /** 주행 중: 화면 조작 없이 상태만 보여 준다. */
    data object Driving : Screen
    /** 정차 감지: 루틴 제안 */
    data object Offer : Screen
    /** 운동 전 통증 점수(1~5) 체크 */
    data class PainCheck(val part: BodyPart) : Screen
    data class Running(
        val routine: Routine,
        val index: Int,
        val remaining: Int,
        val paused: Boolean = false,
        val seatReclined: Boolean = false,
        /** true = 동작 설명을 듣고 자세를 잡는 준비 단계 */
        val preparing: Boolean = true,
        /** true = 운동 전, 등받이를 운동 자세로 젖히는 중 */
        val seatAdjusting: Boolean = false,
        /** true = 등받이가 실제로 움직이는 중 (안내 음성이 끝난 뒤) */
        val seatMoving: Boolean = false,
    ) : Screen {
        val move: Move get() = routine.moves[index]
    }
    data class Done(val routine: Routine, val stats: Stats) : Screen
    /** 루틴 도중 출발 감지 */
    data object Interrupted : Screen
}

data class Stats(
    val today: Int,
    val week: Int,
    val streak: Int,
    /** 최근 7일(오래된 날 → 오늘) 요일 라벨과 횟수 */
    val days: List<Pair<String, Int>>,
    /** 이번 주 부위별 횟수 */
    val parts: List<Pair<BodyPart, Int>>,
)

class SeatFitViewModel(app: Application) : AndroidViewModel(app) {

    val vehicle = VehicleBridge(app).also { it.start() }

    private val _screen = MutableStateFlow<Screen>(Screen.Offer)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _selected = MutableStateFlow(BodyPart.LOWER_BACK)
    val selected: StateFlow<BodyPart> = _selected.asStateFlow()

    /** false = 빠른 운동(3동작), true = 전체 운동(5동작). 마지막 선택을 기억한다. */
    private val _full = MutableStateFlow(false)
    val full: StateFlow<Boolean> = _full.asStateFlow()

    /** 시연용: 동작당 시간을 30초로 줄인다. */
    private val _demoFast = MutableStateFlow(true)
    val demoFast: StateFlow<Boolean> = _demoFast.asStateFlow()

    /** 마지막으로 P에서 벗어난 시각 (연속 운전 시간 계산) */
    private val _drivingSince = MutableStateFlow<Long?>(null)
    val drivingSince: StateFlow<Long?> = _drivingSince.asStateFlow()

    /** 방금 끝난 주행 시간(분). 정차 화면에서 "1시간 52분 운전했어요"로 보여 준다. */
    private val _lastDriveMinutes = MutableStateFlow(0)
    val lastDriveMinutes: StateFlow<Int> = _lastDriveMinutes.asStateFlow()

    /** 빠른 운동에서 고른 오늘의 통증 정도 (건너뛰면 null) */
    private val _pain = MutableStateFlow<PainLevel?>(null)
    val pain: StateFlow<PainLevel?> = _pain.asStateFlow()

    /** 연속 운전이 휴식 권장 시점에 가까워졌는지 (음성 알림은 주행당 한 번) */
    private val _restDue = MutableStateFlow(false)
    val restDue: StateFlow<Boolean> = _restDue.asStateFlow()

    private val voice = Voice(app)
    private var timerJob: Job? = null
    private val prefs = app.getSharedPreferences("seatfit", Context.MODE_PRIVATE)

    init {
        _full.value = prefs.getBoolean("full", false)
        // 휴식 권장 알림: 주행 중 연속 운전 시간을 확인해 한 번만 음성으로 알린다
        viewModelScope.launch {
            while (true) {
                delay(5_000)
                val since = _drivingSince.value
                if (since == null || _screen.value != Screen.Driving) continue
                val minutes = (System.currentTimeMillis() - since) / 60_000
                if (!_restDue.value && minutes >= REST_REMIND_MIN) {
                    _restDue.value = true
                    voice.play(R.raw.s_rest)
                }
            }
        }
        viewModelScope.launch {
            var wasParked: Boolean? = null
            vehicle.status.collect { s ->
                val parked = s.isParked
                if (wasParked == parked) return@collect
                wasParked = parked
                if (parked) onParked() else onDriving()
            }
        }
    }

    fun select(part: BodyPart) { _selected.value = part }
    fun setFull(value: Boolean) {
        _full.value = value
        prefs.edit().putBoolean("full", value).apply()
    }

    /** 지금 선택으로 진행할 세션 (빠른 운동은 날짜별로 조합이 바뀐다) */
    fun sessionFor(part: BodyPart, full: Boolean = _full.value): Routine =
        Routines.session(part, full, LocalDate.now().dayOfYear)
    fun toggleDemoFast() { _demoFast.value = !_demoFast.value }

    /** 이번 주에 가장 많이 푼 부위를 기본 추천으로 */
    fun recommended(): BodyPart =
        weekStats().parts.maxByOrNull { it.second }?.takeIf { it.second > 0 }?.first ?: BodyPart.LOWER_BACK

    private fun onParked() {
        _drivingSince.value?.let { since ->
            _lastDriveMinutes.value = ((System.currentTimeMillis() - since) / 60000).toInt()
        }
        _drivingSince.value = null
        _restDue.value = false
        when (_screen.value) {
            is Screen.Running -> Unit
            else -> {
                _screen.value = Screen.Offer
                voice.play(R.raw.s_parked)
            }
        }
    }

    private fun onDriving() {
        _drivingSince.value = System.currentTimeMillis()
        if (_screen.value is Screen.Running) {
            // 안전: 출발하면 즉시 멈추고 시트를 원위치
            stopRoutine(interrupted = true)
        } else {
            _screen.value = Screen.Driving
        }
    }

    /** "운동 시작" → 빠른 운동이면 먼저 통증 정도를 묻고, 전체 운동이면 바로 시작 */
    fun askPain() {
        if (!vehicle.status.value.isParked) return
        _pain.value = null
        if (_full.value) startRoutine() else _screen.value = Screen.PainCheck(_selected.value)
    }

    /** 통증 정도를 고르면(또는 건너뛰면) 그에 맞춘 조합으로 바로 시작 */
    fun startWithPain(level: PainLevel?) {
        _pain.value = level
        startRoutine()
    }

    fun cancelPain() {
        _screen.value = if (vehicle.status.value.isParked) Screen.Offer else Screen.Driving
    }

    fun startRoutine() {
        if (!vehicle.status.value.isParked) return
        val level = _pain.value
        val base = if (!_full.value && level != null) Routines.painSession(_selected.value, level) else sessionFor(_selected.value)
        val routine = base.let { r ->
            if (_demoFast.value) r.copy(moves = r.moves.map { it.copy(seconds = DEMO_SECONDS) }) else r
        }
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            // 1) 시트 조절 단계
            //  ① 안내: "시트를 맞출게요" 음성을 먼저 끝까지 들려준다 (화면엔 시트 카드)
            _screen.value = Screen.Running(
                routine, 0, routine.moves[0].seconds,
                seatReclined = false, preparing = true, seatAdjusting = true,
            )
            vehicle.startBreathingLight()
            voice.playAndWait(R.raw.s_start, 6_000)
            delay(300)
            //  ② 실제 이동: 등받이가 젖혀지는 동안 기다리며 각도를 112° → 127° 로 보여준다
            (_screen.value as? Screen.Running)?.let { _screen.value = it.copy(seatMoving = true) }
            vehicle.reclineForStretch(SEAT_MOVE_MS)
            delay(SEAT_MOVE_MS)
            //  ③ 완료: "준비 완료"를 잠깐 보여준 뒤 첫 동작으로
            (_screen.value as? Screen.Running)?.let {
                _screen.value = it.copy(seatMoving = false, seatReclined = true)
            }
            delay(900)
            (_screen.value as? Screen.Running)?.let { _screen.value = it.copy(seatAdjusting = false) }
            // 2) 첫 동작
            runMove(routine, 0)
        }
    }

    /** 한 동작: 준비(설명 음성 + 셋·둘·하나) → 운동 타이머 */
    private suspend fun runMove(routine: Routine, index: Int) {
        val move = routine.moves[index]
        _screen.value = Screen.Running(routine, index, move.seconds, seatReclined = true, preparing = true)
        voice.playAndWait(move.voiceClip, 35_000)
        waitWhilePaused()
        voice.playAndWait(R.raw.s_ready, 6_000)
        waitWhilePaused()
        (_screen.value as? Screen.Running)?.let { _screen.value = it.copy(preparing = false) }

        var left = move.seconds
        val half = move.seconds / 2
        while (left > 0) {
            delay(1000)
            val cur = _screen.value as? Screen.Running ?: return
            if (cur.paused) continue
            left -= 1
            _screen.value = cur.copy(remaining = left)
            if (move.switchSides && left == half) voice.play(R.raw.s_switch)
        }
        if (index < routine.moves.lastIndex) {
            voice.playAndWait(R.raw.s_next, 4_000)
            runMove(routine, index + 1)
        } else finish(routine)
    }

    private suspend fun waitWhilePaused() {
        while ((_screen.value as? Screen.Running)?.paused == true) delay(200)
    }

    private fun startFrom(routine: Routine, index: Int) {
        timerJob?.cancel()
        voice.stop()
        timerJob = viewModelScope.launch { runMove(routine, index) }
    }

    fun togglePause() {
        val cur = _screen.value as? Screen.Running ?: return
        _screen.value = cur.copy(paused = !cur.paused)
        if (!cur.paused) voice.stop()
    }

    fun skip() {
        val cur = _screen.value as? Screen.Running ?: return
        if (cur.index < cur.routine.moves.lastIndex) startFrom(cur.routine, cur.index + 1)
        else { timerJob?.cancel(); voice.stop(); finish(cur.routine) }
    }

    fun stopRoutine(interrupted: Boolean = false) {
        timerJob?.cancel()
        voice.stop()
        vehicle.stopBreathingLight()
        vehicle.restoreDrivingPosition()
        if (interrupted) {
            _screen.value = Screen.Interrupted
            voice.play(R.raw.s_stop)
        } else {
            _screen.value = if (vehicle.status.value.isParked) Screen.Offer else Screen.Driving
        }
    }

    private fun finish(routine: Routine) {
        vehicle.stopBreathingLight()
        vehicle.restoreDrivingPosition()
        record(routine.part)
        _screen.value = Screen.Done(routine, weekStats())
        voice.play(R.raw.s_done)
    }

    fun backToHome() {
        _screen.value = if (vehicle.status.value.isParked) Screen.Offer else Screen.Driving
    }

    fun simulate(gear: VehicleBridge.Gear?) = vehicle.simulateGear(gear)

    // ---------- 기록 ----------

    private fun record(part: BodyPart) {
        val today = LocalDate.now()
        val dayKey = "d_$today"
        val partKey = "p_${today}_${part.name}"
        prefs.edit()
            .putInt(dayKey, prefs.getInt(dayKey, 0) + 1)
            .putInt(partKey, prefs.getInt(partKey, 0) + 1)
            .apply()
    }

    /** 시연 영상용: 자연스러운 최근 6일 기록(하루 1~2회)을 채운다. 기존 기록은 지운다. */
    fun seedDemoRecords() {
        clearRecords()
        val today = LocalDate.now()
        // (며칠 전, 부위) — 6일 연속, 총 8회
        val plan = listOf(
            5 to BodyPart.LOWER_BACK,
            4 to BodyPart.NECK, 4 to BodyPart.LOWER_BACK,
            3 to BodyPart.HIP,
            2 to BodyPart.SHIN_ANKLE, 2 to BodyPart.UPPER_BACK,
            1 to BodyPart.NECK,
            0 to BodyPart.LOWER_BACK,
        )
        val e = prefs.edit()
        val day = mutableMapOf<String, Int>(); val part = mutableMapOf<String, Int>()
        plan.forEach { (ago, p) ->
            val d = today.minusDays(ago.toLong())
            day["d_$d"] = (day["d_$d"] ?: 0) + 1
            part["p_${d}_${p.name}"] = (part["p_${d}_${p.name}"] ?: 0) + 1
        }
        (day + part).forEach { (k, v) -> e.putInt(k, v) }
        e.apply()
    }

    /** 운동 기록만 모두 지운다 (설정은 유지) */
    fun clearRecords() {
        val e = prefs.edit()
        prefs.all.keys.filter { it.startsWith("d_") || it.startsWith("p_") || it.startsWith("pain_") }.forEach { e.remove(it) }
        e.apply()
    }

    fun weekStats(): Stats {
        val today = LocalDate.now()
        val names = listOf("월", "화", "수", "목", "금", "토", "일")
        val days = (6 downTo 0).map { i ->
            val d = today.minusDays(i.toLong())
            names[d.dayOfWeek.value - 1] to prefs.getInt("d_$d", 0)
        }
        var streak = 0
        var d = today
        while (prefs.getInt("d_$d", 0) > 0 && streak < 365) { streak++; d = d.minusDays(1) }
        val parts = BodyPart.entries.map { p ->
            p to (0..6).sumOf { i -> prefs.getInt("p_${today.minusDays(i.toLong())}_${p.name}", 0) }
        }
        return Stats(
            today = days.last().second,
            week = days.sumOf { it.second },
            streak = streak,
            days = days,
            parts = parts,
        )
    }

    override fun onCleared() {
        timerJob?.cancel()
        voice.stop()
        vehicle.release()
    }

    companion object {
        const val DEMO_SECONDS = 30
        /** 연속 운전 120분 = "다음 휴식 권장 0분 후"가 되는 순간 알린다 */
        const val REST_REMIND_MIN = 120
        /** 등받이를 운동 자세로 젖히는 시간 */
        const val SEAT_MOVE_MS = 4000L

        fun durationText(minutes: Int): String = when {
            minutes < 60 -> "${minutes}분"
            minutes % 60 == 0 -> "${minutes / 60}시간"
            else -> "${minutes / 60}시간 ${minutes % 60}분"
        }
    }
}
