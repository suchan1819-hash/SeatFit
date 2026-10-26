@file:OptIn(ExperimentalTextApi::class)

package com.zeroinjury.seatfit.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.zeroinjury.seatfit.R

/** 시트핏 색상 토큰 — 차량 야간 실내의 네이비 + 민트 앰비언트 라이트 */
object SF {
    val Bg0 = Color(0xFF040B15)
    val Bg1 = Color(0xFF071628)
    val Glow = Color(0xFF0B2038)
    val Img = Color(0xFF05172A)       // 일러스트 배경색 (그림 배경과 동일)
    val Bg = Bg0

    val Text = Color(0xFFF3F7FC)
    val Sub = Color(0xFF8FA0B8)
    val Faint = Color(0xFF55657C)
    val Soft = Color(0xFFC8D3E2)

    val Mint = Color(0xFF4FF0D2)
    val Cyan = Color(0xFF38C6F4)
    val Blue = Color(0xFF7C95FF)
    val Amber = Color(0xFFFFB45C)
    val Coral = Color(0xFFFF7A66)
    val Ink = Color(0xFF03201B)       // 민트 버튼 위 글자

    val Stroke = Color.White.copy(alpha = 0.09f)
    val Glass0 = Color.White.copy(alpha = 0.07f)
    val Glass1 = Color.White.copy(alpha = 0.025f)
    val Track = Color.White.copy(alpha = 0.08f)
}

private fun asta(weight: Int) = Font(
    R.font.asta_sans,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Pleos Connect 공식 서체 Asta Sans (가변 폰트) */
val Asta = FontFamily(asta(300), asta(400), asta(500), asta(600), asta(700), asta(800))

/** 숫자 폭이 일정한 스타일 (타이머·통계) */
val Tnum = TextStyle(fontFamily = Asta, color = SF.Text, fontFeatureSettings = "tnum")

@Composable
fun SeatFitTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = SF.Mint,
            secondary = SF.Cyan,
            background = SF.Bg0,
            surface = SF.Bg1,
            onPrimary = SF.Ink,
            onBackground = SF.Text,
            onSurface = SF.Text,
        ),
    ) {
        ProvideTextStyle(TextStyle(fontFamily = Asta, color = SF.Text)) {
            content()
        }
    }
}
