package com.zeroinjury.seatfit.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ───────────────────────── 표면 ─────────────────────────

/** 반투명 유리 카드 */
fun Modifier.glass(radius: Dp = 32.dp): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .clip(shape)
        .background(Brush.verticalGradient(listOf(SF.Glass0, SF.Glass1)))
        .border(1.dp, SF.Stroke, shape)
}

/** 앱 전체 배경: 깊은 네이비 + 은은한 빛 번짐 */
@Composable
fun AppBackground(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SF.Bg1, SF.Bg0)))
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(SF.Glow, Color.Transparent),
                        center = Offset(size.width * 0.30f, size.height * 0.45f),
                        radius = size.width * 0.55f,
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(SF.Mint.copy(alpha = 0.07f), Color.Transparent),
                        center = Offset(size.width, 0f),
                        radius = size.width * 0.5f,
                    )
                )
            }
    ) { content() }
}

/** 왼쪽이 투명하게 사라지는 마스크 (카드 속 일러스트) */
fun Modifier.fadeFromLeft(solidAt: Float = 0.45f): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawRect(
            Brush.horizontalGradient(0f to Color.Transparent, solidAt to Color.Black),
            blendMode = BlendMode.DstIn,
        )
    }

/** 가장자리가 둥글게 사라지는 마스크 (큰 일러스트) */
fun Modifier.fadeRadial(solid: Float = 0.55f, rx: Float = 0.62f): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawRect(
            Brush.radialGradient(
                0f to Color.Black, solid to Color.Black, 1f to Color.Transparent,
                center = Offset(size.width / 2, size.height / 2),
                radius = size.maxDimension * rx,
            ),
            blendMode = BlendMode.DstIn,
        )
    }

// ───────────────────────── 일러스트 ─────────────────────────

@Composable
fun Art(@DrawableRes res: Int, modifier: Modifier = Modifier, scale: ContentScale = ContentScale.Crop) {
    Image(painterResource(res), contentDescription = null, modifier = modifier, contentScale = scale)
}

/** 일러스트의 특정 지점을 확대해서 보여 준다 */
@Composable
fun FocusArt(@DrawableRes res: Int, focusX: Float, focusY: Float, zoom: Float, modifier: Modifier = Modifier) {
    Box(modifier.clipToBounds()) {
        Image(
            painterResource(res), contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoom; scaleY = zoom
                    transformOrigin = TransformOrigin(focusX, focusY)
                },
        )
    }
}

/** 작은 동작 썸네일 */
@Composable
fun Thumb(@DrawableRes res: Int, w: Dp, h: Dp, radius: Dp = 14.dp) {
    Box(Modifier.size(w, h).clip(RoundedCornerShape(radius)).background(SF.Img)) {
        FocusArt(res, 0.45f, 0.42f, 1.5f, Modifier.fillMaxSize())
    }
}

// ───────────────────────── 텍스트 조각 ─────────────────────────

@Composable
fun Eyebrow(text: String, color: Color = SF.Mint) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(22.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) { Box(Modifier.size(10.dp).clip(CircleShape).background(color)) }
        Spacer(Modifier.width(10.dp))
        Text(text, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Pill(text: String, dot: Color? = null, textColor: Color = Color(0xFFD5DEEA), icon: (@Composable () -> Unit)? = null, onClick: (() -> Unit)? = null) {
    Row(
        Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.055f))
            .border(1.dp, SF.Stroke, RoundedCornerShape(22.dp))
            .let { if (onClick != null) it.clickable { onClick() } else it }
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (dot != null) Box(Modifier.size(9.dp).clip(CircleShape).background(dot))
        icon?.invoke()
        Text(text, color = textColor, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun Chip(text: String) {
    Text(
        text, color = SF.Soft, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

// ───────────────────────── 버튼 ─────────────────────────

@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    showPlay: Boolean = true,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(26.dp)
    Row(
        modifier
            .height(84.dp)
            .shadow(24.dp, shape, ambientColor = SF.Cyan, spotColor = SF.Cyan)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(SF.Mint, SF.Cyan)))
            .clickable { onClick() }
            .padding(horizontal = 36.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showPlay) { IconPlay(26.dp, SF.Ink); Spacer(Modifier.width(14.dp)) }
        Text(text, color = SF.Ink, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        if (trailing != null) {
            Spacer(Modifier.width(14.dp))
            Text(trailing, color = SF.Ink.copy(alpha = 0.6f), fontSize = 20.sp, fontWeight = FontWeight.Bold, style = Tnum)
        }
    }
}

@Composable
fun GlassButton(
    text: String,
    modifier: Modifier = Modifier,
    tint: Color = SF.Text,
    danger: Boolean = false,
    icon: @Composable (Color) -> Unit,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val c = if (danger) Color(0xFFFFB4A8) else tint
    Row(
        modifier
            .height(76.dp)
            .clip(shape)
            .background(if (danger) SF.Coral.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.09f))
            .border(1.dp, if (danger) SF.Coral.copy(alpha = 0.25f) else SF.Stroke, shape)
            .clickable { onClick() }
            .padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        icon(c)
        Text(text, color = c, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

// ───────────────────────── 카드 ─────────────────────────

@Composable
fun StatCard(label: String, value: String, unit: String, modifier: Modifier = Modifier, extra: (@Composable () -> Unit)? = null) {
    Column(modifier.glass(24.dp).padding(horizontal = 26.dp, vertical = 22.dp)) {
        Text(label, color = SF.Sub, fontSize = 18.sp)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 46.sp, fontWeight = FontWeight.ExtraBold, style = Tnum)
            Spacer(Modifier.width(6.dp))
            Text(unit, color = SF.Sub, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        }
        extra?.invoke()
    }
}

/** 차량 연동 상태 (아이콘 + 라벨 + 값) */
@Composable
fun RowScope.SyncTile(label: String, value: String, icon: @Composable () -> Unit) {
    Row(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(SF.Mint.copy(alpha = 0.06f))
            .border(1.dp, SF.Mint.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        icon()
        Column {
            Text(label, color = SF.Sub, fontSize = 14.sp)
            Text(value, fontSize = 17.sp, fontWeight = FontWeight.Bold, style = Tnum, maxLines = 1, softWrap = false)
        }
    }
}

// ───────────────────────── 진행 표시 ─────────────────────────

@Composable
fun TimerRing(progress: Float, number: String, label: String, size: Dp = 230.dp) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(900), label = "ring")
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = 12.dp.toPx()
            val inset = sw / 2 + 4.dp.toPx()
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            drawCircle(SF.Mint.copy(alpha = 0.06f), radius = this.size.minDimension * 0.34f)
            drawArc(SF.Track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(sw))
            drawArc(
                Brush.sweepGradient(listOf(SF.Mint, SF.Cyan, SF.Mint)),
                -90f, 360f * p, false, Offset(inset, inset), arcSize,
                style = Stroke(sw, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(number, fontSize = 84.sp, fontWeight = FontWeight.ExtraBold, style = Tnum, lineHeight = 84.sp)
            Text(label, color = SF.Sub, fontSize = 17.sp)
        }
    }
}

@Composable
fun Segments(count: Int, current: Int, progress: Float, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(count) { i ->
            val fill = when {
                i < current -> 1f
                i == current -> progress.coerceIn(0f, 1f)
                else -> 0f
            }
            val f by animateFloatAsState(fill, tween(900), label = "seg")
            Box(
                Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.14f))
            ) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(f).background(SF.Mint))
            }
        }
    }
}

/** 코치 음성 파형 */
@Composable
fun VoiceWave(active: Boolean) {
    val t = rememberInfiniteTransition(label = "wave")
    val phase by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1200)), label = "phase")
    val base = listOf(14f, 26f, 38f, 22f, 30f, 16f, 10f)
    Row(Modifier.height(40.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        base.forEachIndexed { i, h ->
            val k = if (active) 0.55f + 0.45f * kotlin.math.abs(kotlin.math.sin((phase + i * 0.17f) * Math.PI.toFloat())) else 0.35f
            Box(Modifier.width(5.dp).height((h * k).dp).clip(RoundedCornerShape(3.dp)).background(SF.Mint))
        }
    }
}

/** 은은하게 숨 쉬는 점 */
@Composable
fun PulseDot(color: Color, size: Dp = 10.dp) {
    val t = rememberInfiniteTransition(label = "pulse")
    val a by t.animateFloat(0.15f, 0.35f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "a")
    Box(Modifier.size(size * 2.2f).clip(CircleShape).background(color.copy(alpha = a)), contentAlignment = Alignment.Center) {
        Box(Modifier.size(size).clip(CircleShape).background(color))
    }
}

// ───────────────────────── 아이콘 (24 그리드) ─────────────────────────

@Composable
private fun Icon24(size: Dp, draw: DrawScope.(s: Float) -> Unit) {
    Canvas(Modifier.size(size)) { draw(this.size.width / 24f) }
}

private fun DrawScope.line(c: Color, s: Float, w: Float, vararg pts: Float) {
    val p = Path()
    p.moveTo(pts[0] * s, pts[1] * s)
    var i = 2
    while (i < pts.size) { p.lineTo(pts[i] * s, pts[i + 1] * s); i += 2 }
    drawPath(p, c, style = Stroke(w * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

@Composable
fun IconPlay(size: Dp, c: Color) = Icon24(size) { s ->
    val p = Path().apply { moveTo(7f * s, 4.8f * s); lineTo(7f * s, 19.2f * s); lineTo(19.6f * s, 12f * s); close() }
    drawPath(p, c)
}

@Composable
fun IconPause(size: Dp, c: Color) = Icon24(size) { s ->
    drawRoundRect(c, Offset(5.5f * s, 4f * s), Size(4.5f * s, 16f * s), CornerRadius(1.6f * s))
    drawRoundRect(c, Offset(14f * s, 4f * s), Size(4.5f * s, 16f * s), CornerRadius(1.6f * s))
}

@Composable
fun IconNext(size: Dp, c: Color) = Icon24(size) { s ->
    val p = Path().apply { moveTo(5f * s, 5.6f * s); lineTo(5f * s, 18.4f * s); lineTo(15.6f * s, 12f * s); close() }
    drawPath(p, c)
    drawRoundRect(c, Offset(17f * s, 4.5f * s), Size(3f * s, 15f * s), CornerRadius(1.4f * s))
}

@Composable
fun IconStop(size: Dp, c: Color) = Icon24(size) { s ->
    drawRoundRect(c, Offset(5f * s, 5f * s), Size(14f * s, 14f * s), CornerRadius(3.5f * s))
}

@Composable
fun IconCheck(size: Dp, c: Color, width: Float = 2.6f) = Icon24(size) { s ->
    line(c, s, width, 5f, 12.5f, 10f, 17.5f, 19.5f, 7f)
}

/** 옆에서 본 자동차 시트 (헤드레스트 · 등받이 · 방석 · 레일) */
@Composable
fun IconSeat(size: Dp, c: Color) = Icon24(size) { s ->
    // 헤드레스트
    drawRoundRect(c, Offset(6.2f * s, 1.6f * s), Size(4.6f * s, 3.6f * s), CornerRadius(1.6f * s))
    // 등받이 (살짝 뒤로 기운 형태)
    val back = Path().apply {
        moveTo(5.6f * s, 7.0f * s)
        lineTo(10.6f * s, 6.4f * s)
        lineTo(12.4f * s, 15.2f * s)
        lineTo(7.6f * s, 15.8f * s)
        close()
    }
    drawPath(back, c)
    drawPath(back, c, style = Stroke(1.6f * s, join = StrokeJoin.Round))
    // 방석
    drawRoundRect(c, Offset(7.4f * s, 14.4f * s), Size(12.2f * s, 4.0f * s), CornerRadius(2.0f * s))
    // 레일
    line(c, s, 1.8f, 9.5f, 21f, 18.5f, 21f)
}

@Composable
fun IconLight(size: Dp, c: Color) = Icon24(size) { s ->
    drawCircle(c, 4f * s, Offset(12f * s, 12f * s), style = Stroke(2f * s))
    val rays = listOf(
        floatArrayOf(12f, 2.5f, 12f, 4.7f), floatArrayOf(12f, 19.3f, 12f, 21.5f),
        floatArrayOf(2.5f, 12f, 4.7f, 12f), floatArrayOf(19.3f, 12f, 21.5f, 12f),
        floatArrayOf(5.3f, 5.3f, 6.8f, 6.8f), floatArrayOf(17.2f, 17.2f, 18.7f, 18.7f),
        floatArrayOf(5.3f, 18.7f, 6.8f, 17.2f), floatArrayOf(17.2f, 6.8f, 18.7f, 5.3f),
    )
    rays.forEach { r -> line(c, s, 2f, r[0], r[1], r[2], r[3]) }
}

@Composable
fun IconVoice(size: Dp, c: Color) = Icon24(size) { s ->
    line(c, s, 2f, 4f, 10f, 4f, 14f)
    line(c, s, 2f, 8f, 7f, 8f, 17f)
    line(c, s, 2f, 12f, 4f, 12f, 20f)
    line(c, s, 2f, 16f, 8f, 16f, 16f)
    line(c, s, 2f, 20f, 10.5f, 20f, 13.5f)
}

/** 앱 로고 마크: 민트 그라데이션 위 시트 */
@Composable
fun LogoMark(size: Dp = 52.dp) {
    Box(
        Modifier
            .size(size)
            .shadow(16.dp, RoundedCornerShape(16.dp), ambientColor = SF.Mint, spotColor = SF.Mint)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(SF.Mint, SF.Cyan))),
        contentAlignment = Alignment.Center,
    ) { IconSeat(size * 0.56f, SF.Ink) }
}
