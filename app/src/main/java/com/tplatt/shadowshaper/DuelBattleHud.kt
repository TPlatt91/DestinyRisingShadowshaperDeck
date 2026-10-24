package com.tplatt.shadowshaper

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val metallicGold = Color(0xFFF0D095)
private val hostileRed = Color(0xFFE9B9AE)
private val orbBlue = Color(0xFF83C7FA)

/** The in-game Power value is displayed in a faceted diamond, not a plain app label. */
@Composable
internal fun DuelPowerCrest(
    value: Int,
    isPlayer: Boolean,
    modifier: Modifier = Modifier
) {
    val displayed by animateIntAsState(value, animationSpec = tween(340), label = "power crest")
    val pulse = remember(isPlayer) { Animatable(0f) }
    var ready by remember(isPlayer) { mutableStateOf(false) }
    LaunchedEffect(value, isPlayer) {
        if (ready) {
            pulse.snapTo(1f)
            pulse.animateTo(0f, tween(durationMillis = 570))
        } else ready = true
    }
    Box(modifier.size(54.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(51.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = size.width * 0.45f
            val diamond = Path().apply {
                moveTo(cx, cy - radius)
                lineTo(cx + radius, cy)
                lineTo(cx, cy + radius)
                lineTo(cx - radius, cy)
                close()
            }
            val edge = if (isPlayer) metallicGold else hostileRed
            drawPath(
                diamond,
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF46413A), Color(0xFF131B26), Color(0xFF343432))
                )
            )
            drawPath(diamond, color = Color(0xFF11171D), style = Stroke(4.dp.toPx()))
            drawPath(diamond, color = edge, style = Stroke((1.5f + pulse.value * 1.4f).dp.toPx()))
            if (pulse.value > .02f) {
                drawPath(
                    diamond, color = edge.copy(alpha = pulse.value * .78f),
                    style = Stroke((2f + pulse.value * 2.4f).dp.toPx())
                )
            }
            drawLine(
                edge.copy(alpha = .60f),
                Offset(cx, cy - radius * 0.83f),
                Offset(cx + radius * 0.83f, cy), .9.dp.toPx()
            )
        }
        Text(
            displayed.toString(),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/** Destiny-style blue Light orb rather than a generic numeric Text chip. */
@Composable
internal fun DuelLightOrb(value: Int, maximum: Int) {
    val shown by animateIntAsState(value, animationSpec = tween(240), label = "light value")
    val glow = remember { Animatable(0f) }
    var initialized by remember { mutableStateOf(false) }
    LaunchedEffect(value) {
        if (initialized) {
            glow.snapTo(1f)
            glow.animateTo(0f, tween(460))
        } else initialized = true
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            Modifier.size(21.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFC6E9FF), Color(0xFF3B90D0), Color(0xFF17456F))
                    ),
                    CircleShape
                )
                .border((1f + glow.value).dp, Color(0xFFE2F3FF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.size(10.dp)
                    .border(.7.dp, Color(0x99EAF6FF), CircleShape)
            )
        }
        Text(
            "$shown/$maximum",
            color = Color(0xFFF2F1E8), fontSize = 12.sp,
            fontWeight = FontWeight.Bold, maxLines = 1
        )
    }
}

/**
 * Staggered card-back pile for actual remaining deck count.
 * Reuses the local cover art already bundled with the game.
 * Purely decorative: cards cannot be accessed through this image.
 */
@Composable
internal fun DuelDeckCounter(coverId: String, label: String, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy((-13).dp)) {
            repeat(minOf(3, maxOf(1, count))) {
                DuelCardCover(
                    coverId,
                    Modifier.width(19.dp).height(29.dp)
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                label, fontSize = 9.sp, color = metallicGold,
                fontWeight = FontWeight.Bold, maxLines = 1
            )
            Text(
                "$count", fontSize = 13.sp,
                color = Color(0xFFF3F1EB),
                fontWeight = FontWeight.Black, maxLines = 1
            )
        }
    }
}

/** Actual CPU deck count and hidden hand, no invented discard-pile mechanics. */
@Composable
internal fun DuelUtilityPill(hand: Int, deck: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        DuelDeckCounter("default", "CPU DECK", deck)
        Text(
            "HAND $hand",
            fontSize = 9.sp,
            color = Color(0xFFE9DDD0),
            maxLines = 1
        )
    }
}

/** Working onClick is supplied by the battle coordinator's existing guarded End Turn action. */
@Composable
internal fun DuelGoldTurnDial(
    enabled: Boolean,
    label: String,
    onClick: () -> Unit
) {
    val glint = rememberInfiniteTransition(label = "turn dial")
    val dialLight by glint.animateFloat(
        initialValue = .45f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 1650), repeatMode = RepeatMode.Reverse
        ),
        label = "gold bezel glow"
    )
    Box(
        Modifier.size(62.dp),
        contentAlignment = Alignment.Center
    ) {
        // Etched rotating-mechanism style bezel: gold teeth, inner black
        // recess, a polished top highlight and an amber outer circumference.
        Canvas(Modifier.size(62.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outer = size.minDimension * .475f
            drawCircle(Color(0xA5101115), outer)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        Color(0x005B3B1B), Color(0xB844311D),
                        Color(0xFFB88B42), Color(0xFF32281E)
                    )
                ),
                radius = outer
            )
            drawCircle(
                Color(0xFFDEB66B).copy(alpha = if (enabled) dialLight else .45f), radius = outer * .91f,
                style = Stroke(1.2.dp.toPx())
            )
            for (index in 0 until 16) {
                val a = 2.0 * PI * index / 16.0 - PI / 2.0
                val direction = Offset(
                    cos(a).toFloat(), sin(a).toFloat()
                )
                val from = center + direction * (outer * .88f)
                val to = center + direction * (outer * if (index % 4 == 0) 1.02f else .97f)
                drawLine(
                    if (index % 4 == 0) Color(0xFFF4D48E)
                    else Color(0xFF8B6938),
                    from, to,
                    if (index % 4 == 0) 1.3.dp.toPx() else .8.dp.toPx()
                )
            }
            drawCircle(
                Color(0xFF261D1A), radius = outer * .70f,
                style = Stroke(1.7.dp.toPx())
            )
            drawLine(
                Color(0xAFFFDE9D),
                Offset(center.x - outer * .26f, center.y - outer * .69f),
                Offset(center.x + outer * .15f, center.y - outer * .69f),
                1.1.dp.toPx()
            )
        }
        Button(
            onClick = onClick, enabled = enabled,
            shape = CircleShape,
            modifier = Modifier.size(47.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF765022),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFF555046)
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp)
        ) {
            Text(
                label, fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                lineHeight = 10.sp,
                maxLines = 2
            )
        }
    }
}
