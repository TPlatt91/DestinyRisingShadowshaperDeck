package com.tplatt.shadowshaper

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
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
            repeat(count.coerceIn(0, 3)) {
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

/**
 * Minimal circular command control: an understated sci-fi objective reticle
 * rather than a bronze mechanical cog. The full 78dp surface is interactive,
 * but ONLY through the existing coordinator's enabled/label/onClick contract.
 */
@Composable
internal fun DuelGoldTurnDial(
    enabled: Boolean,
    label: String,
    onClick: () -> Unit
) {
    val cancelling = label.contains("CANCEL", ignoreCase = true)
    val moving = label.contains("MOVE", ignoreCase = true)
    val accent = when {
        cancelling -> Color(0xFF93D2E3)
        moving -> Color(0xFFCEDCE0)
        else -> Color(0xFFDEC28A)
    }
    val muted = Color(0xFF74828A)
    val rim = if (enabled) accent else muted
    val interactivity = remember { MutableInteractionSource() }
    val pressed by interactivity.collectIsPressedAsState()
    val glint = rememberInfiniteTransition(label = "turn dial")
    val dialLight by glint.animateFloat(
        initialValue = .52f,
        targetValue = .90f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 2150), repeatMode = RepeatMode.Reverse
        ),
        label = "gold bezel glow"
    )

    Box(
        Modifier
            .size(78.dp)
            .graphicsLayer {
                val pressScale = if (pressed && enabled) .967f else 1f
                scaleX = pressScale
                scaleY = pressScale
            }
            .shadow(5.dp, CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = interactivity,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .semantics {
                contentDescription = when {
                    cancelling -> "Cancel card targeting"
                    moving -> "Choose a unit relocation slot"
                    else -> "End turn"
                }
                stateDescription = if (enabled) "Available" else "Not your turn"
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(76.dp)) {
            val c = Offset(size.width * .5f, size.height * .5f)
            val radius = size.minDimension * .47f
            val highlight = if (enabled) dialLight else .22f

            // Broad dark recess with a white-metal outer rim. Gold appears
            // only where the command is active, following Destiny's spare HUD.
            drawCircle(
                Color(0xD80A1015),
                radius = radius * 1.02f,
                center = c
            )
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFF4E585D),
                        rim.copy(alpha = .65f),
                        Color(0xFF8D9898),
                        Color(0xFF374047),
                        rim.copy(alpha = .84f),
                        Color(0xFF4E585D)
                    ),
                    center = c
                ),
                radius = radius,
                center = c
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF263139), Color(0xFF131D25), Color(0xFF0A1118)),
                    center = Offset(c.x - radius * .22f, c.y - radius * .25f),
                    radius = radius * 1.2f
                ),
                radius = radius * .895f,
                center = c
            )
            drawCircle(
                color = rim.copy(alpha = .45f + highlight * .30f),
                radius = radius * .89f,
                center = c,
                style = Stroke(width = 1.dp.toPx())
            )

            // Fixed segments are intentional decoration, not a false clock.
            drawArc(
                color = rim.copy(alpha = if (enabled) .61f + .30f * highlight else .33f),
                startAngle = -131f,
                sweepAngle = 76f,
                useCenter = false,
                topLeft = Offset(c.x - radius * .99f, c.y - radius * .99f),
                size = androidx.compose.ui.geometry.Size(radius * 1.98f, radius * 1.98f),
                style = Stroke(1.9.dp.toPx())
            )
            drawArc(
                color = rim.copy(alpha = .48f),
                startAngle = 48f,
                sweepAngle = 77f,
                useCenter = false,
                topLeft = Offset(c.x - radius * .99f, c.y - radius * .99f),
                size = androidx.compose.ui.geometry.Size(radius * 1.98f, radius * 1.98f),
                style = Stroke(1.5.dp.toPx())
            )

            // Four understated alignment ticks and a small focal diamond,
            // original geometry rather than Bungie brand marks.
            for (index in 0 until 4) {
                val angle = 2.0 * PI * index / 4.0 - PI / 2.0
                val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                drawLine(
                    color = rim.copy(alpha = .79f),
                    start = c + direction * (radius * .91f),
                    end = c + direction * (radius * 1.02f),
                    strokeWidth = 1.2.dp.toPx()
                )
            }
            val y = c.y - radius * .62f
            val diamond = Path().apply {
                moveTo(c.x, y - 2.5.dp.toPx())
                lineTo(c.x + 2.6.dp.toPx(), y)
                lineTo(c.x, y + 2.5.dp.toPx())
                lineTo(c.x - 2.6.dp.toPx(), y)
                close()
            }
            drawPath(
                diamond,
                color = rim.copy(alpha = if (enabled) .82f else .36f)
            )
            drawLine(
                color = rim.copy(alpha = .52f),
                start = Offset(c.x - radius * .29f, c.y + radius * .52f),
                end = Offset(c.x + radius * .29f, c.y + radius * .52f),
                strokeWidth = .75.dp.toPx()
            )
        }
        Text(
            label,
            color = if (enabled) Color(0xFFF4F3EE) else Color(0xFF9BA7AA),
            fontSize = if (label.length > 7) 10.sp else 11.sp,
            lineHeight = 12.sp,
            letterSpacing = .7.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}
