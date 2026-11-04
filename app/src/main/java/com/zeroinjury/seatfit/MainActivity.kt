package com.zeroinjury.seatfit

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.zeroinjury.seatfit.routine.BodyPart
import com.zeroinjury.seatfit.routine.PainLevel
import com.zeroinjury.seatfit.routine.SeatFitViewModel
import com.zeroinjury.seatfit.ui.SeatFitApp
import com.zeroinjury.seatfit.ui.SeatFitTheme
import com.zeroinjury.seatfit.vehicle.VehicleBridge

class MainActivity : ComponentActivity() {
    private val vm: SeatFitViewModel by viewModels()

    /** 시연·테스트용: adb shell am broadcast -a com.zeroinjury.seatfit.DEMO -p com.zeroinjury.seatfit --es cmd park */
    private val demo = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            val cmd = i.getStringExtra("cmd") ?: return
            if (cmd == "clearlog") runCatching { java.io.File(getExternalFilesDir(null), "vlog.mp4").delete() }
            com.zeroinjury.seatfit.routine.VoiceLog.write(this@MainActivity, "mark_$cmd")
            when {
                cmd == "park" -> vm.simulate(VehicleBridge.Gear.PARK)
                cmd == "drive" -> vm.simulate(VehicleBridge.Gear.DRIVE)
                cmd == "ask" -> vm.askPain()
                cmd == "start" -> vm.startRoutine()
                cmd == "skip" -> vm.skip()
                cmd == "home" -> vm.backToHome()
                cmd == "seed" -> vm.seedDemoRecords()
                cmd.startsWith("part_") -> runCatching { vm.select(BodyPart.valueOf(cmd.removePrefix("part_"))) }
                cmd.startsWith("pain_") -> runCatching { vm.startWithPain(PainLevel.valueOf(cmd.removePrefix("pain_"))) }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val f = IntentFilter("com.zeroinjury.seatfit.DEMO")
        androidx.core.content.ContextCompat.registerReceiver(this, demo, f, androidx.core.content.ContextCompat.RECEIVER_EXPORTED)
        setContent {
            SeatFitTheme { SeatFitApp(vm) }
        }
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(demo) }
        super.onDestroy()
    }
}
