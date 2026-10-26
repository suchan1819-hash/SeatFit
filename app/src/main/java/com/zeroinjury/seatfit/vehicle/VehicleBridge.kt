package com.zeroinjury.seatfit.vehicle

import android.car.Car
import android.car.VehicleGear
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.util.Log
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.control.MoveControl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 차량과 시트핏 사이의 연결 지점.
 *
 * - 정차 감지: AAOS 표준 VehicleProperty (GEAR_SELECTION, PARKING_BRAKE_ON)
 * - 시트·앰비언트 조명 제어: Pleos Vehicle SDK (Vehicle Control 권한 승인 시 실제 동작)
 *
 * 차량 제어가 아직 열려 있지 않거나 실패하면 앱은 화면 애니메이션으로 동일한 경험을 보여 준다.
 */
class VehicleBridge(context: Context) {

    enum class Gear { PARK, REVERSE, NEUTRAL, DRIVE, UNKNOWN }

    data class Status(
        val gear: Gear = Gear.UNKNOWN,
        val parkingBrake: Boolean? = null,
        val carConnected: Boolean = false,
        val pleosConnected: Boolean = false,
        val seatControlAvailable: Boolean? = null,
        val lightControlAvailable: Boolean? = null,
    ) {
        /** 주차(P) 또는 주차 브레이크가 체결된 상태 */
        val isParked: Boolean get() = gear == Gear.PARK || parkingBrake == true
    }

    private val tag = "SeatFit/Vehicle"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val appContext = context.applicationContext

    private val _status = MutableStateFlow(Status())
    val status: StateFlow<Status> = _status.asStateFlow()

    /** 데모·테스트용: 차량 신호가 없을 때 수동으로 기어를 바꿔 본다. */
    private val _simulated = MutableStateFlow<Gear?>(null)

    private var car: Car? = null
    private var propertyManager: CarPropertyManager? = null
    private var vehicle: Vehicle? = null

    private val driverSeat = SeatArea(SeatValue.ROW_1_LEFT)
    private var seatRecline = 0          // 이번 루틴에서 뒤로 젖힌 단계 수
    private var lightJob: Job? = null
    private var lightMin = 1
    private var lightMax = 10

    private val gearCallback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            when (value.propertyId) {
                VehiclePropertyIds.GEAR_SELECTION,
                VehiclePropertyIds.CURRENT_GEAR -> {
                    val g = (value.value as? Int)?.let(::mapGear) ?: Gear.UNKNOWN
                    if (_simulated.value == null) update { it.copy(gear = g) }
                }
                VehiclePropertyIds.PARKING_BRAKE_ON -> {
                    val on = value.value as? Boolean
                    if (_simulated.value == null) update { it.copy(parkingBrake = on) }
                }
            }
        }

        override fun onErrorEvent(propertyId: Int, areaId: Int) {
            Log.w(tag, "property error $propertyId")
        }
    }

    fun start() {
        connectCar()
        connectPleos()
    }

    private fun connectCar() {
        runCatching {
            val c = Car.createCar(appContext)
            car = c
            val pm = c.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
            propertyManager = pm

            // 현재 값 먼저 읽기
            runCatching {
                val g = pm.getIntProperty(VehiclePropertyIds.GEAR_SELECTION, 0)
                update { it.copy(gear = mapGear(g)) }
            }.onFailure {
                runCatching {
                    val g = pm.getIntProperty(VehiclePropertyIds.CURRENT_GEAR, 0)
                    update { it.copy(gear = mapGear(g)) }
                }
            }
            runCatching {
                val b = pm.getBooleanProperty(VehiclePropertyIds.PARKING_BRAKE_ON, 0)
                update { it.copy(parkingBrake = b) }
            }

            // 변화 구독
            listOf(
                VehiclePropertyIds.GEAR_SELECTION,
                VehiclePropertyIds.CURRENT_GEAR,
                VehiclePropertyIds.PARKING_BRAKE_ON,
            ).forEach { id ->
                runCatching {
                    pm.registerCallback(gearCallback, id, CarPropertyManager.SENSOR_RATE_ONCHANGE)
                }
            }
            update { it.copy(carConnected = true) }
        }.onFailure {
            Log.w(tag, "Car API unavailable: ${it.message}")
            update { it.copy(carConnected = false) }
        }
    }

    private fun connectPleos() {
        runCatching {
            val v = Vehicle(appContext)
            v.initialize()
            vehicle = v
            update { it.copy(pleosConnected = true) }

            runCatching {
                v.getLight().checkAmbientLightBrightnessCapabilities(
                    onComplete = { _, areas ->
                        areas.values.firstOrNull()?.let { b ->
                            lightMin = b.minValue
                            lightMax = b.maxValue
                        }
                        update { it.copy(lightControlAvailable = areas.isNotEmpty()) }
                    },
                    onFailed = { e ->
                        Log.w(tag, "light capability: ${e.message}")
                        update { it.copy(lightControlAvailable = false) }
                    },
                )
            }.onFailure { update { s -> s.copy(lightControlAvailable = false) } }
        }.onFailure {
            Log.w(tag, "Pleos SDK unavailable: ${it.message}")
            update { it.copy(pleosConnected = false) }
        }
    }

    // ---------- 시트 ----------

    /** 스트레칭 공간을 만들기 위해 등받이를 뒤로 젖힌다. */
    fun reclineForStretch(durationMs: Long = 2500) = moveBackrest(MoveControl.BACKWARD, durationMs) {
        seatRecline += 1
    }

    /** 루틴이 끝나면 운전 자세로 되돌린다. */
    fun restoreDrivingPosition() {
        if (seatRecline <= 0) return
        val steps = seatRecline
        seatRecline = 0
        moveBackrest(MoveControl.FORWARD, 2500L * steps) {}
    }

    private fun moveBackrest(direction: MoveControl, durationMs: Long, onStarted: () -> Unit) {
        val seat = runCatching { vehicle?.getSeat() }.getOrNull()
        if (seat == null) {
            update { it.copy(seatControlAvailable = false) }
            return
        }
        scope.launch {
            var ok = true
            runCatching {
                seat.moveSeatBackrestAngle(driverSeat, direction,
                    onComplete = { _, _ -> },
                    onFailed = { e ->
                        ok = false
                        Log.w(tag, "seat move failed: ${e.message}")
                        update { it.copy(seatControlAvailable = false) }
                    })
            }.onFailure { ok = false; update { s -> s.copy(seatControlAvailable = false) } }
            if (ok) onStarted()
            delay(durationMs)
            runCatching {
                seat.moveSeatBackrestAngle(driverSeat, MoveControl.STOP,
                    onComplete = { _, _ -> update { it.copy(seatControlAvailable = true) } },
                    onFailed = { })
            }
        }
    }

    // ---------- 앰비언트 조명 ----------

    /** 호흡 주기(초)에 맞춰 조명 밝기를 천천히 올렸다 내린다. */
    fun startBreathingLight(cycleSec: Float = 8f) {
        lightJob?.cancel()
        val light = runCatching { vehicle?.getLight() }.getOrNull() ?: return
        lightJob = scope.launch {
            val start = System.currentTimeMillis()
            while (isActive) {
                val t = (System.currentTimeMillis() - start) / 1000f
                val phase = (sin(2 * PI * t / cycleSec - PI / 2) + 1) / 2   // 0..1
                val v = (lightMin + (lightMax - lightMin) * phase).roundToInt()
                runCatching {
                    light.setAmbientLightBrightness(driverSeat, v,
                        onComplete = { _, _ -> },
                        onFailed = { update { it.copy(lightControlAvailable = false) } })
                }
                delay(400)
            }
        }
    }

    fun stopBreathingLight() {
        lightJob?.cancel()
        lightJob = null
    }

    // ---------- 데모용 ----------

    fun simulateGear(gear: Gear?) {
        _simulated.value = gear
        if (gear != null) update { it.copy(gear = gear, parkingBrake = gear == Gear.PARK) }
    }

    fun release() {
        stopBreathingLight()
        runCatching { propertyManager?.unregisterCallback(gearCallback) }
        runCatching { car?.disconnect() }
        runCatching { vehicle?.release() }
    }

    private fun update(block: (Status) -> Status) {
        _status.value = block(_status.value)
    }

    private fun mapGear(raw: Int): Gear = when (raw) {
        VehicleGear.GEAR_PARK -> Gear.PARK
        VehicleGear.GEAR_REVERSE -> Gear.REVERSE
        VehicleGear.GEAR_NEUTRAL -> Gear.NEUTRAL
        VehicleGear.GEAR_UNKNOWN -> Gear.UNKNOWN
        else -> Gear.DRIVE
    }
}
