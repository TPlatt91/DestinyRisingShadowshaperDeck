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
 * Faceted command medallion, a game control rather than a default material
 * button. It exposes the SAME enabled/label/onClick contract as before.
 * In particular: CANCEL still exits target selection; MOVE still prompts
 * the real movement choice; END TURN still goes through the coordinator.
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
        cancelling -> Color(0xFF94D5F5)
        moving -> Color(0xFFCAD7E5)
        else -> Color(0xFFF3CF83)
    }
    val muted = Color(0xFF817F76)
    val metal = if (enabled) accent else muted
    val interactivity = remember { MutableInteractionSource() }
    val pressed by interactivity.collectIsPressedAsState()
    val glint = rememberInfiniteTransition(label = "turn dial")
    val dialLight by glint.animateFloat(
        initialValue = .48f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 1850), repeatMode = RepeatMode.Reverse
        ),
        label = "gold bezel glow"
    )
    Box(
        Modifier
            .size(78.dp)
            .graphicsLayer {
                val pressScale = if (pressed && enabled) .955f else 1f
                scaleX = pressScale
                scaleY = pressScale
            }
            .shadow(8.dp, CircleShape)
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
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension * .472f
            val glow = if (enabled) dialLight else .18f

            // Diffuse edge, brushed-metal bezel, faceted inner core.
            drawCircle(Color(0xB5080E15), radius * 1.06f, center)
            drawCircle(
                brush = Brush.sweepGradient(
                    listOf(
                        Color(0xFF443C2E), metal.copy(alpha = .90f),
                        Color(0xFF554536), Color(0xFFB29A70),
                        Color(0xFF272D31), metal.copy(alpha = .94f),
                        Color(0xFF443C2E)
                    ),
                    center = center
                ),
                radius = radius,
                center = center
            )
            drawCircle(
                color = Color(0xFF0A1118),
                radius = radius * .886f,
                center = center
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        if (cancelling) Color(0xFF2C5870) else Color(0xFF684C2B),
                        if (cancelling) Color(0xFF173747) else Color(0xFF3D3025),
                        Color(0xFF101921)
                    ),
                    center = Offset(center.x - radius * .25f, center.y - radius * .37f),
                    radius = radius * 1.39f
                ),
                radius = radius * .78f,
                center = center
            )

            drawCircle(
                color = metal.copy(alpha = .66f + .30f * glow),
                radius = radius * .88f,
                center = center,
                style = Stroke(width = (1f + glow * .55f).dp.toPx())
            )
            drawCircle(
                color = metal.copy(alpha = .31f),
                radius = radius * .735f,
                center = center,
                style = Stroke(.8.dp.toPx())
            )
            // Six radial accents and precision-machined ordinal teeth.
            for (index in 0 until 24) {
                val a = 2.0 * PI * index / 24.0 - PI / 2.0
                val dx = cos(a).toFloat()
                val dy = sin(a).toFloat()
                val major = index % 4 == 0
                val inner = radius * (if (major) .87f else .924f)
                val outer = radius * (if (major) 1.025f else .973f)
                drawLine(
                    color = metal.copy(alpha = if (major) .90f else .54f),
                    start = center + Offset(dx * inner, dy * inner),
                    end = center + Offset(dx * outer, dy * outer),
                    strokeWidth = (if (major) 1.45f else .73f).dp.toPx()
                )
            }
            // Short arcs imply a precision-engineered mechanism; they do not
            // falsely indicate a time limit or change real game turns.
            drawArc(
                color = metal.copy(alpha = .37f + .36f * glow),
                startAngle = 209f,
                sweepAngle = 73f,
                useCenter = false,
                topLeft = Offset(center.x - radius * .96f, center.y - radius * .96f),
                size = androidx.compose.ui.geometry.Size(radius * 1.92f, radius * 1.92f),
                style = Stroke(1.7.dp.toPx())
            )
            drawArc(
                color = metal.copy(alpha = .40f),
                startAngle = 24f,
                sweepAngle = 85f,
                useCenter = false,
                topLeft = Offset(center.x - radius * .96f, center.y - radius * .96f),
                size = androidx.compose.ui.geometry.Size(radius * 1.92f, radius * 1.92f),
                style = Stroke(1.15.dp.toPx())
            )
            val topY = center.y - radius * .59f
            val mark = Path().apply {
                moveTo(center.x, topY - 2.dp.toPx())
                lineTo(center.x + 3.dp.toPx(), topY)
                lineTo(center.x, topY + 2.dp.toPx())
                lineTo(center.x - 3.dp.toPx(), topY)
                close()
            }
            drawPath(mark, metal.copy(alpha = if (enabled) .90f else .38f))
            drawLine(
                color = metal.copy(alpha = .64f),
                start = Offset(center.x - radius * .34f, center.y + radius * .54f),
                end = Offset(center.x + radius * .34f, center.y + radius * .54f),
                strokeWidth = .9.dp.toPx()
            )
        }
        Text(
            label,
            color = if (enabled) Color(0xFFFFF2D9) else Color(0xFFC6C4BE),
            fontSize = if (label.length > 7) 10.sp else 11.sp,
            lineHeight = 12.sp,
            letterSpacing = .85.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}
