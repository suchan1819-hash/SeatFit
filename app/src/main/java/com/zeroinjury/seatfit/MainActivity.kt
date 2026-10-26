package com.zeroinjury.seatfit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.zeroinjury.seatfit.routine.SeatFitViewModel
import com.zeroinjury.seatfit.ui.SeatFitApp
import com.zeroinjury.seatfit.ui.SeatFitTheme

class MainActivity : ComponentActivity() {
    private val vm: SeatFitViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SeatFitTheme { SeatFitApp(vm) }
        }
    }
}
