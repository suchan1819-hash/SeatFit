package com.zeroinjury.seatfit.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeroinjury.seatfit.routine.BodyPart
import com.zeroinjury.seatfit.routine.Routines
import com.zeroinjury.seatfit.routine.PainLevel
import com.zeroinjury.seatfit.routine.Routine
import com.zeroinjury.seatfit.routine.Screen
import com.zeroinjury.seatfit.routine.SeatFitViewModel
import com.zeroinjury.seatfit.routine.Stats
import com.zeroinjury.seatfit.vehicle.VehicleBridge
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

/** 디자인 기준 캔버스. 어떤 창 크기에서도 시안과 같은 비율로 보이도록 전체를 맞춰 줄인다/키운다. */
private const val DESIGN_W = 1600f
private const val DESIGN_H = 1100f

private const val SEAT_BASE = 112
private const val SEAT_RECLINED = 127

@Composable
fun SeatFitApp(vm: SeatFitViewModel) {
    AppBackground {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val scale = min(maxWidth.value / DESIGN_W, maxHeight.value / DESIGN_H)
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density * scale, d.fontScale)) {
                AppContent(vm)
            }
        }
    }
}

@Composable
private fun AppContent(vm: SeatFitViewModel) {
    val screen by vm.screen.collectAsState()
    val status by vm.vehicle.status.collectAsState()
    val demoFast by vm.demoFast.collectAsState()
    var showDemo by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(start = 52.dp, end = 52.dp, top = 36.dp, bottom = 44.dp)) {
        TopBar(status, demoFast, onLogoLongPress = { showDemo = !showDemo }, onToggleFast = vm::toggleDemoFast)
        if (showDemo) DemoBar(vm)
        Spacer(Modifier.height(28.dp))
        AnimatedContent(
            targetState = screen,
            contentKey = { it::class },
            transitionSpec = { fadeIn(tween(450)) togetherWith fadeOut(tween(300)) },
            modifier = Modifier.fillMaxSize(),
            label = "screen",
        ) { s ->
            when (s) {
                Screen.Driving -> DrivingScreen(vm)
                Screen.Offer -> OfferScreen(vm)
                is Screen.PainCheck -> PainCheckScreen(vm, s)
                is Screen.Running -> RunningScreen(vm, s)
                is Screen.Done -> DoneScreen(vm, s)
                Screen.Interrupted -> InterruptedScreen(vm)
            }
        }
    }
}

// ───────────────────────── 상단 바 ─────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TopBar(status: VehicleBridge.Status, demoFast: Boolean, onLogoLongPress: () -> Unit, onToggleFast: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.combinedClickable(onClick = {}, onLongClick = onLogoLongPress),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LogoMark()
            Spacer(Modifier.width(16.dp))
            Column {
                Text("시트핏", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 30.sp)
                Text("차 안의 통증 관리 트레이너", color = SF.Sub, fontSize = 16.sp)
            }
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (demoFast) Pill("시연 모드 · 동작 30초", textColor = SF.Sub, onClick = onToggleFast)
            when (status.gear) {
                VehicleBridge.Gear.PARK -> Pill("P 주차", dot = SF.Mint)
                VehicleBridge.Gear.DRIVE -> Pill("D 주행", dot = SF.Amber, textColor = Color(0xFFFFD7A6))
                VehicleBridge.Gear.REVERSE -> Pill("R 후진", dot = SF.Amber, textColor = Color(0xFFFFD7A6))
                VehicleBridge.Gear.NEUTRAL -> Pill("N 중립", dot = SF.Amber, textColor = Color(0xFFFFD7A6))
                VehicleBridge.Gear.UNKNOWN -> Pill(if (status.isParked) "주차 브레이크" else "기어 확인 중", dot = SF.Sub)
            }
            Pill(
                if (status.carConnected) "차량 연결됨" else "차량 신호 없음",
                dot = if (status.carConnected) SF.Blue else SF.Coral,
            )
            if (!demoFast) Pill("기본 모드 · 동작 60초", textColor = SF.Sub, onClick = onToggleFast)
        }
    }
}

@Composable
private fun DemoBar(vm: SeatFitViewModel) {
    Row(
        Modifier.padding(top = 16.dp).glass(20.dp).padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("테스트용 기어", color = SF.Sub, fontSize = 16.sp)
        Pill("P 주차", dot = SF.Mint, onClick = { vm.simulate(VehicleBridge.Gear.PARK) })
        Pill("D 주행", dot = SF.Amber, onClick = { vm.simulate(VehicleBridge.Gear.DRIVE) })
        Pill("실제 차량 신호", dot = SF.Blue, onClick = { vm.simulate(null) })
    }
}

// ───────────────────────── 정차: 부위 선택 ─────────────────────────

@Composable
private fun OfferScreen(vm: SeatFitViewModel) {
    val selected by vm.selected.collectAsState()
    val demoFast by vm.demoFast.collectAsState()
    val lastDrive by vm.lastDriveMinutes.collectAsState()
    val recommended = remember { vm.recommended() }
    val full by vm.full.collectAsState()
    val routine = vm.sessionFor(selected, full)
    val perMove = if (demoFast) SeatFitViewModel.DEMO_SECONDS else 60
    val total = routine.moves.size * perMove
    val quickCount = Routines.QUICK_MOVES
    val fullCount = Routines.of(selected).moves.size

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(40.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Eyebrow(
                if (lastDrive >= 1) "주차 감지 · ${SeatFitViewModel.durationText(lastDrive)} 운전했어요"
                else "주차 감지 · 잠깐 몸을 풀고 갈까요"
            )
            Spacer(Modifier.height(14.dp))
            Text("어디가 뻐근하세요?", fontSize = 64.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 72.sp)
            Spacer(Modifier.height(14.dp))
            Text("부위를 고르면 시트와 조명이 함께 움직이고, 코치 음성이 하나씩 알려 드려요.", color = SF.Sub, fontSize = 22.sp)
            Spacer(Modifier.height(34.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                BodyPart.entries.chunked(2).forEach { pair ->
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        pair.forEach { p ->
                            PartCard(
                                part = p,
                                selected = p == selected,
                                recommended = p == recommended,
                                minutes = Routines.approxMinutes(if (full) Routines.of(p).moves.size else quickCount, perMove),
                                moves = if (full) Routines.of(p).moves.size else quickCount,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            ) { vm.select(p) }
                        }
                    }
                }
            }
        }

        Column(Modifier.width(560.dp).fillMaxHeight().glass(32.dp).padding(32.dp)) {
            // 전체 코스(5동작)일 때는 목록이 길어지므로 큰 미리보기를 접는다
            AnimatedVisibility(visible = !full) {
                Column {
                    // 왼쪽 부위 카드와 같은 이미지·같은 초점으로 맞춘다
                    Crossfade(selected, label = "preview") { part ->
                        CssCover(
                            part.cardImage, part.focusX, part.focusY, 1.4f,
                            Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(20.dp)).background(SF.Img)
                                .border(1.dp, SF.Stroke, RoundedCornerShape(20.dp)),
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                }
            }
            Text(routine.title, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Chip("${routine.moves.size}가지 동작")
                Chip("약 ${Routines.approxMinutes(routine.moves.size, perMove)}분")
                Chip(if (full) "전체 코스" else "통증 맞춤 조합")
            }
            Spacer(Modifier.height(16.dp))
            LengthPicker(
                full = full,
                quickLabel = "빠르게 · ${quickCount}동작",
                fullLabel = "전체 · ${fullCount}동작",
                onChange = { vm.setFull(it) },
            )
            Spacer(Modifier.height(16.dp))
            if (!full) {
                // 빠른 운동: 시작하면 통증 정도를 묻고 그에 맞춘 3동작을 고른다
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.035f)).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("시작하면 오늘 통증 정도를 물어보고\n그에 맞는 3동작을 골라 드려요", fontSize = 19.sp, fontWeight = FontWeight.Bold, lineHeight = 27.sp)
                    PainLevel.entries.forEach { level ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(painColor(level)))
                            Spacer(Modifier.width(12.dp))
                            Text(level.label, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(118.dp))
                            Text(level.sub, color = SF.Sub, fontSize = 16.sp, maxLines = 1)
                        }
                    }
                }
            } else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                routine.moves.forEach { m ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.035f)).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Thumb(m.image, 80.dp, 58.dp)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(m.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(m.sub, color = SF.Sub, fontSize = 16.sp, maxLines = 1)
                        }
                        Text(clock(perMove), color = SF.Sub, fontSize = 17.sp, style = Tnum, modifier = Modifier.padding(end = 8.dp))
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SyncTile("등받이", "$SEAT_BASE°→$SEAT_RECLINED°") { IconSeat(22.dp, SF.Mint) }
                SyncTile("실내 조명", "운동 모드") { IconLight(22.dp, SF.Mint) }
                SyncTile("음성 코칭", "켜짐") { IconVoice(22.dp, SF.Mint) }
            }
            Spacer(Modifier.height(16.dp))
            PrimaryButton("운동 시작", Modifier.fillMaxWidth(), trailing = clock(total)) { vm.askPain() }
        }
    }
}

@Composable
private fun PartCard(part: BodyPart, selected: Boolean, recommended: Boolean, minutes: Int, moves: Int, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier
            .let { if (selected) it.shadow(30.dp, shape, ambientColor = SF.Mint, spotColor = SF.Mint) else it }
            .clip(shape)
            .background(SF.Img)
            .border(if (selected) 2.dp else 1.dp, if (selected) SF.Mint else SF.Stroke, shape)
            .clickable { onClick() },
    ) {
        Box(Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(0.62f)) {
            CssCover(part.cardImage, part.focusX, part.focusY, 2.5f, Modifier.fillMaxSize())
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(0f to SF.Img, 0.40f to SF.Img, 0.62f to SF.Img.copy(alpha = 0f))
            )
        )
        val tag = (if (recommended) "추천 · " else "") + "${moves}동작 · 약 ${minutes}분"
        Text(
            tag,
            color = if (selected) SF.Ink else SF.Sub,
            fontSize = 15.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(start = 26.dp, top = 20.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) SF.Mint else Color.White.copy(alpha = 0.06f))
                .padding(horizontal = 12.dp, vertical = 7.dp),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(start = 26.dp, bottom = 20.dp)) {
            Text(part.label, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Text(part.sub, color = if (selected) SF.Mint else SF.Sub, fontSize = 18.sp)
        }
    }
}

/** 빠른 운동 / 전체 운동 선택 (세그먼트 컨트롤) */
@Composable
private fun LengthPicker(full: Boolean, quickLabel: String, fullLabel: String, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.05f)).border(1.dp, SF.Stroke, RoundedCornerShape(18.dp)).padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        listOf(false to quickLabel, true to fullLabel).forEach { (value, label) ->
            val on = value == full
            Box(
                Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(14.dp))
                    .background(if (on) SF.Mint else Color.Transparent)
                    .clickable { onChange(value) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = if (on) SF.Ink else SF.Sub, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * CSS background-size / background-position 과 같은 방식으로 일러스트 일부를 확대해 그린다.
 * widthRatio = 2.5 → 이미지 폭을 상자 폭의 250%로, (px, py) = 보이는 위치 비율(0~1).
 */
@Composable
private fun CssCover(@DrawableRes res: Int, px: Float, py: Float, widthRatio: Float, modifier: Modifier) {
    val img: ImageBitmap = ImageBitmap.imageResource(res)
    Canvas(modifier) {
        val bw = size.width
        val bh = size.height
        if (bw <= 0f || bh <= 0f) return@Canvas
        val iw = img.width.toFloat()
        val ih = img.height.toFloat()
        val k = max(widthRatio * bw / iw, bh / ih)
        val sw = bw / k
        val sh = bh / k
        val sx = (px * (iw - sw)).coerceIn(0f, iw - sw)
        val sy = (py * (ih - sh)).coerceIn(0f, ih - sh)
        drawImage(
            img,
            srcOffset = IntOffset(sx.toInt(), sy.toInt()),
            srcSize = IntSize(sw.toInt(), sh.toInt()),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(bw.toInt(), bh.toInt()),
            filterQuality = FilterQuality.High,
        )
    }
}

// ───────────────────────── 루틴 진행 ─────────────────────────

/** 운동 시작 전, 시트가 운동 자세로 움직이는 동안 보여 주는 안내 */
@Composable
private fun BoxScope.SeatAdjustOverlay(vm: SeatFitViewModel, angle: Int, moving: Boolean, done: Boolean) {
    val status by vm.vehicle.status.collectAsState()
    val p = ((angle - SEAT_BASE).toFloat() / (SEAT_RECLINED - SEAT_BASE)).coerceIn(0f, 1f)
    Box(Modifier.matchParentSize().background(SF.Bg0.copy(alpha = 0.72f)))
    Column(
        Modifier.align(Alignment.Center).width(460.dp).glass(32.dp).padding(horizontal = 40.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(92.dp).clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(listOf(SF.Mint, SF.Cyan))),
            contentAlignment = Alignment.Center,
        ) { IconSeat(54.dp, SF.Ink) }
        Spacer(Modifier.height(22.dp))
        Text(
            when {
                done -> "준비 완료"
                moving -> "등받이를 젖히는 중"
                else -> "시트를 운동 자세로 맞출게요"
            },
            fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            when {
                done -> "이제 첫 동작을 시작할게요"
                moving -> "움직일 공간을 만드는 동안 편하게 기대 주세요"
                else -> "곧 등받이가 천천히 뒤로 움직여요"
            },
            color = SF.Sub, fontSize = 18.sp,
        )
        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$angle", fontSize = 64.sp, fontWeight = FontWeight.ExtraBold, style = Tnum, lineHeight = 64.sp)
            Text("°", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = SF.Mint)
        }
        Text("$SEAT_BASE° → $SEAT_RECLINED°", color = SF.Sub, fontSize = 16.sp, style = Tnum)
        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(SF.Track)) {
            Box(
                Modifier.fillMaxHeight().fillMaxWidth(p)
                    .background(Brush.horizontalGradient(listOf(SF.Mint, SF.Cyan)))
            )
        }
        if (status.seatControlAvailable == false) {
            Spacer(Modifier.height(16.dp))
            Text(
                "시트가 움직이지 않으면 레버로 등받이를 조금 젖혀 주세요",
                color = SF.Amber, fontSize = 16.sp,
            )
        }
    }
}

@Composable
private fun RunningScreen(vm: SeatFitViewModel, s: Screen.Running) {
    val move = s.move
    val progress = 1f - s.remaining / move.seconds.toFloat()
    val next = s.routine.moves.getOrNull(s.index + 1)

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(40.dp)) {
        Box(
            Modifier.weight(1.15f).fillMaxHeight().clip(RoundedCornerShape(36.dp)).background(SF.Img)
                .border(1.dp, SF.Stroke, RoundedCornerShape(36.dp))
        ) {
            // 동작 시연: 설명이 나오는 순간부터 시작 자세 ↔ 동작 자세를 일정한 템포로 계속 번갈아 보여 준다
            var showMove by remember(s.index) { mutableStateOf(false) }
            LaunchedEffect(s.index, s.paused) {
                if (!s.paused) {
                    while (true) { delay(1800); showMove = !showMove }
                }
            }
            val frame = if (showMove) move.image else move.startImage
            Crossfade(frame, animationSpec = tween(450), label = "move") { img ->
                Art(img, Modifier.fillMaxSize(), ContentScale.Crop)
            }
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to SF.Bg0.copy(alpha = 0.55f), 0.22f to Color.Transparent,
                        0.70f to Color.Transparent, 1f to SF.Bg0.copy(alpha = 0.75f),
                    )
                )
            )
            Column(Modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp, top = 30.dp)) {
                Segments(s.routine.moves.size, s.index, if (s.preparing) 0f else progress, Modifier.fillMaxWidth())
                Spacer(Modifier.height(18.dp))
                FrameTag("동작 예시", active = true)
            }
            // 등받이 각도: 조절 중에는 112° → 127° 로 실제 움직임에 맞춰 올라간다
            val seatAngle = remember { Animatable(SEAT_BASE.toFloat()) }
            LaunchedEffect(s.seatMoving, s.seatReclined) {
                when {
                    s.seatMoving -> {
                        seatAngle.snapTo(SEAT_BASE.toFloat())
                        seatAngle.animateTo(
                            SEAT_RECLINED.toFloat(),
                            tween(SeatFitViewModel.SEAT_MOVE_MS.toInt(), easing = FastOutSlowInEasing),
                        )
                    }
                    s.seatReclined -> seatAngle.snapTo(SEAT_RECLINED.toFloat())
                    else -> seatAngle.snapTo(SEAT_BASE.toFloat())
                }
            }
            val angle = seatAngle.value.toInt()
            if (s.seatAdjusting) SeatAdjustOverlay(vm, angle, moving = s.seatMoving, done = s.seatReclined)
            Row(Modifier.align(Alignment.BottomStart).padding(28.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill("등받이 $angle°", icon = { IconSeat(20.dp, SF.Mint) })
                Pill("실내 조명 운동 모드", icon = { IconLight(20.dp, SF.Mint) })
            }
        }

        Column(Modifier.weight(0.85f).fillMaxHeight()) {
            Eyebrow(
                if (s.preparing) "${s.routine.title} · 동작 ${s.index + 1} / ${s.routine.moves.size} · 준비"
                else "${s.routine.title} · 동작 ${s.index + 1} / ${s.routine.moves.size}"
            )
            Spacer(Modifier.height(10.dp))
            Text(move.name, fontSize = 58.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 66.sp)
            Spacer(Modifier.height(14.dp))
            Text(move.cue, color = Color(0xFFD9E2EE), fontSize = 26.sp, lineHeight = 38.sp)
            Spacer(Modifier.height(30.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(30.dp)) {
                if (s.seatAdjusting) TimerRing(
                    if (s.seatReclined) 1f else 0f, "시트",
                    when { s.seatReclined -> "준비 완료"; s.seatMoving -> "등받이 조절 중"; else -> "곧 움직여요" },
                )
                else if (s.preparing) TimerRing(0f, "준비", if (s.paused) "일시정지" else "그림을 따라 해 보세요")
                else TimerRing(progress, "${s.remaining}", if (s.paused) "일시정지" else "초 남음")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(move.reps, color = SF.Mint, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                    move.points.forEach { pt ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconCheck(20.dp, SF.Mint)
                            Spacer(Modifier.width(8.dp))
                            Text(pt, color = SF.Soft, fontSize = 19.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(26.dp))
            Row(Modifier.fillMaxWidth().glass(24.dp).padding(horizontal = 22.dp, vertical = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                VoiceWave(active = !s.paused)
                Column {
                    Text("코치 음성", color = SF.Mint, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(move.voice, color = SF.Soft, fontSize = 18.sp, lineHeight = 27.sp, maxLines = 4)
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (next != null) {
                    Thumb(next.image, 72.dp, 54.dp, 12.dp)
                    Spacer(Modifier.width(14.dp))
                    Text("다음 · ${next.name}", color = SF.Sub, fontSize = 18.sp)
                } else {
                    Text("마지막 동작이에요", color = SF.Sub, fontSize = 18.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                GlassButton(if (s.paused) "계속" else "일시정지", icon = { c -> if (s.paused) IconPlay(24.dp, c) else IconPause(24.dp, c) }) { vm.togglePause() }
                GlassButton("다음 동작", icon = { c -> IconNext(24.dp, c) }) { vm.skip() }
                Spacer(Modifier.weight(1f))
                GlassButton("종료", danger = true, icon = { c -> IconStop(22.dp, c) }) { vm.stopRoutine() }
            }
        }
    }
}

// ───────────────────────── 완료 ─────────────────────────

@Composable
private fun DoneScreen(vm: SeatFitViewModel, s: Screen.Done) {
    val st: Stats = s.stats
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(40.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
            Box(
                Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(SF.Mint.copy(alpha = 0.25f), SF.Mint.copy(alpha = 0.05f))))
                    .border(2.dp, SF.Mint.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { IconCheck(56.dp, SF.Mint) }
            Spacer(Modifier.height(30.dp))
            Text("${s.routine.title} 완료", fontSize = 64.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(14.dp))
            Text("시트를 운전 자세(${SEAT_BASE}°)로 되돌렸어요. 남은 길도 편안하게 가세요.", color = SF.Sub, fontSize = 22.sp)
            Spacer(Modifier.height(28.dp))
            PainReasonCard(s.routine)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard("오늘", "${st.today}", "회", Modifier.weight(1f))
                StatCard("이번 주", "${st.week}", "회", Modifier.weight(1f))
                StatCard("연속", "${st.streak}", "일", Modifier.weight(1f))
            }
            Spacer(Modifier.height(34.dp))
            PrimaryButton("처음으로", Modifier.width(380.dp), showPlay = false) { vm.backToHome() }
        }

        Column(Modifier.width(620.dp).fillMaxHeight().glass(32.dp).padding(32.dp)) {
            Text("이번 주 운동 기록", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            val top = st.parts.maxByOrNull { it.second }?.takeIf { it.second > 0 }?.first
            Spacer(Modifier.height(8.dp))
            Text(if (top != null) "가장 많이 푼 부위 · ${top.label}" else "첫 기록을 시작했어요", color = SF.Sub, fontSize = 18.sp)
            Spacer(Modifier.height(20.dp))
            val maxDay = (st.days.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
            Row(
                Modifier.fillMaxWidth().height(250.dp).padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                st.days.forEachIndexed { i, (day, count) ->
                    val today = i == st.days.lastIndex
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(56.dp)) {
                        Text(if (count > 0) "$count" else "", color = SF.Sub, fontSize = 16.sp, style = Tnum)
                        Spacer(Modifier.height(12.dp))
                        val h = if (count > 0) (165f * count / maxDay).coerceAtLeast(24f) else 8f
                        Box(
                            Modifier
                                .width(44.dp)
                                .height(h.dp)
                                .let { if (today && count > 0) it.shadow(18.dp, RoundedCornerShape(14.dp), ambientColor = SF.Mint, spotColor = SF.Mint) else it }
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    when {
                                        count == 0 -> Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.07f), Color.White.copy(alpha = 0.07f)))
                                        today -> Brush.verticalGradient(listOf(SF.Mint, SF.Mint.copy(alpha = 0.25f)))
                                        else -> Brush.verticalGradient(listOf(SF.Blue.copy(alpha = 0.6f), SF.Blue.copy(alpha = 0.15f)))
                                    }
                                )
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(day, color = if (today) SF.Text else SF.Sub, fontSize = 18.sp, fontWeight = if (today) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            val maxPart = (st.parts.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                st.parts.forEach { (p, n) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Thumb(p.cardImage, 56.dp, 42.dp, 10.dp)
                        Text(p.label, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(120.dp))
                        Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(Color.White.copy(alpha = 0.07f))) {
                            Box(
                                Modifier.fillMaxHeight().fillMaxWidth(n / maxPart.toFloat())
                                    .background(Brush.horizontalGradient(listOf(SF.Mint, SF.Cyan)))
                            )
                        }
                        Text("${n}회", color = SF.Sub, fontSize = 18.sp, style = Tnum, modifier = Modifier.width(48.dp))
                    }
                }
            }
        }
    }
}

// ───────────────────────── 통증 체크 (빠른 운동) ─────────────────────────

private fun painColor(level: PainLevel): Color = when (level) {
    PainLevel.MILD -> SF.Mint
    PainLevel.MODERATE -> SF.Amber
    PainLevel.SEVERE -> SF.Coral
}

@Composable
private fun PainCheckScreen(vm: SeatFitViewModel, s: Screen.PainCheck) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Eyebrow("빠른 운동 · ${s.part.label} · 맞춤 조합")
        Spacer(Modifier.height(14.dp))
        Text("오늘 ${s.part.label}, 어느 정도예요?", fontSize = 60.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(12.dp))
        Text("통증 정도에 맞춰 3동작을 골라 드려요. 누르면 바로 시작해요.", color = SF.Sub, fontSize = 22.sp)
        Spacer(Modifier.height(40.dp))
        Row(Modifier.fillMaxWidth().height(330.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            PainLevel.entries.forEach { level ->
                val c = painColor(level)
                val shape = RoundedCornerShape(30.dp)
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(shape)
                        .background(Brush.verticalGradient(listOf(c.copy(alpha = 0.16f), Color.White.copy(alpha = 0.03f))))
                        .border(1.5.dp, c.copy(alpha = 0.55f), shape)
                        .clickable { vm.startWithPain(level) }
                        .padding(30.dp),
                ) {
                    Box(Modifier.size(16.dp).clip(CircleShape).background(c))
                    Spacer(Modifier.height(18.dp))
                    Text(level.label, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(6.dp))
                    Text(level.sub, color = c, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Routines.painPreview(s.part, level).forEachIndexed { i, name ->
                        Text("${i + 1}  $name", color = SF.Sub, fontSize = 19.sp, maxLines = 1)
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            GlassButton("건너뛰고 오늘의 조합으로", icon = { IconPlay(22.dp, it) }) { vm.startWithPain(null) }
            GlassButton("뒤로", icon = { }) { vm.cancelPain() }
        }
    }
}

/** 완료 화면: 오늘 조합을 이렇게 고른 이유 */
@Composable
private fun PainReasonCard(routine: Routine) {
    val level = routine.pain ?: return
    val c = painColor(level)
    Row(
        Modifier.fillMaxWidth().glass(24.dp).padding(horizontal = 26.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(14.dp).clip(CircleShape).background(c))
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text("오늘의 맞춤 조합 · ${level.label}", color = c, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(6.dp))
            Text(level.reason, color = SF.Text, fontSize = 19.sp, lineHeight = 28.sp)
        }
    }
}

// ───────────────────────── 주행 중 ─────────────────────────

@Composable
private fun DrivingScreen(vm: SeatFitViewModel) {
    val since by vm.drivingSince.collectAsState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
    val minutes = since?.let { ((now - it) / 60000).toInt() } ?: 0
    val restIn = (120 - minutes).coerceAtLeast(0)
    val restDue by vm.restDue.collectAsState()

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(40.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
            if (restDue) {
                Eyebrow("휴식 권장 · 다음 정차 때 3분만 풀어요", SF.Coral)
                Spacer(Modifier.height(14.dp))
                Text("곧 쉬어 갈 시간이에요", fontSize = 64.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(14.dp))
                Text("휴게소나 안전한 곳에 차를 세우면 바로 스트레칭을 안내할게요.", color = SF.Sub, fontSize = 22.sp)
            } else {
                Eyebrow("주행 중 · 음성 안내만 켜져 있어요", SF.Amber)
                Spacer(Modifier.height(14.dp))
                Text("운전에 집중하세요", fontSize = 64.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(14.dp))
                Text("차가 멈추면 시트핏이 먼저 말을 걸게요.", color = SF.Sub, fontSize = 22.sp)
            }
            Spacer(Modifier.height(34.dp))
            // 두 카드는 같은 구조(숫자 + 진행 막대)로 크기를 똑같이 맞춘다
            Row(
                Modifier.width(640.dp).height(150.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val (v, u) = if (minutes < 60) "$minutes" to "분" else "${minutes / 60}:${"%02d".format(minutes % 60)}" to "시간"
                val driven = (minutes / 120f).coerceIn(0.02f, 1f)
                StatCard("연속 운전", v, u, Modifier.weight(1f).fillMaxHeight()) {
                    DriveBar(driven, listOf(SF.Mint, SF.Amber))
                }
                val (rv, ru) = if (restIn > 0) "$restIn" to "분 후" else "지금" to ""
                StatCard("다음 휴식 권장", rv, ru, Modifier.weight(1f).fillMaxHeight()) {
                    DriveBar((1f - driven).coerceAtLeast(0.02f), listOf(SF.Cyan, SF.Mint))
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxHeight()) {
            Art(
                com.zeroinjury.seatfit.R.drawable.hero_driving,
                Modifier.fillMaxSize().clip(RoundedCornerShape(36.dp)).background(SF.Img)
                    .border(1.dp, SF.Stroke, RoundedCornerShape(36.dp)),
                ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun DriveBar(fraction: Float, colors: List<Color>) {
    Spacer(Modifier.height(16.dp))
    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.08f))) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction).background(Brush.horizontalGradient(colors)))
    }
}

// ───────────────────────── 출발 감지로 중단 ─────────────────────────

@Composable
private fun InterruptedScreen(vm: SeatFitViewModel) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(40.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
            Eyebrow("출발이 감지됐어요", SF.Amber)
            Spacer(Modifier.height(14.dp))
            Text("운동을 멈추고\n시트를 운전 자세로 되돌렸어요", fontSize = 56.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 66.sp)
            Spacer(Modifier.height(16.dp))
            Text("안전을 위해 주행 중에는 운동을 안내하지 않아요.\n다음 정차 때 이어서 해요.", color = SF.Sub, fontSize = 22.sp, lineHeight = 34.sp)
            Spacer(Modifier.height(36.dp))
            PrimaryButton("확인", Modifier.width(300.dp), showPlay = false) { vm.backToHome() }
        }
        Box(Modifier.weight(0.9f).fillMaxHeight()) {
            Art(
                com.zeroinjury.seatfit.R.drawable.hero_driving,
                Modifier.fillMaxSize().clip(RoundedCornerShape(36.dp)).background(SF.Img)
                    .border(1.dp, SF.Stroke, RoundedCornerShape(36.dp)),
                ContentScale.Crop,
            )
        }
    }
}

// ───────────────────────── 유틸 ─────────────────────────

private fun clock(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

@Composable
private fun FrameTag(text: String, active: Boolean) {
    Text(
        text,
        color = if (active) SF.Ink else SF.Soft,
        fontSize = 16.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) SF.Mint else SF.Bg0.copy(alpha = 0.55f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}
