package com.tplatt.shadowshaper

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Read-only color mapping; never depends on a card's art asset or faction. */
internal object DuelFxPalette {
    val scorch = Color(0xFFFF873D)
    val suppression = Color(0xFFAB8CFF)
    val inspired = Color(0xFFA7EFC0)
    val shield = Color(0xFF81D9FF)
    val power = Color(0xFFEAD39B)

    fun forEvent(kind: DuelEffectKind): Color = when (kind) {
        DuelEffectKind.SCORCH_APPLIED, DuelEffectKind.SCORCH_TICK -> scorch
        DuelEffectKind.SUPPRESSED -> suppression
        DuelEffectKind.INSPIRED -> inspired
        DuelEffectKind.OVERSHIELD_APPLIED, DuelEffectKind.OVERSHIELD_BLOCKED -> shield
        DuelEffectKind.POWER_GAINED -> inspired
        DuelEffectKind.POWER_TRANSFER -> power
    }
}

/**
 * Small peripheral VFX drawn OVER the existing miniature. Only card edges
 * and corners are touched: the full printed illustration and Power values
 * stay readable. No touch handlers, random values or game-state changes.
 */
@Composable
internal fun DuelCardStatusFx(
    unit: ShadowEngine.Unit,
    events: List<DuelEffectEvent>,
    modifier: Modifier = Modifier
) {
    val pulse = remember(unit) { Animatable(0f) }
    var pulseKind by remember(unit) { mutableStateOf<DuelEffectKind?>(null) }
    var seenSerial by remember(unit) { mutableStateOf(0L) }
    // Do not run idle infinite animations on normal, unmarked cards.
    val hasOngoingStatus = unit.scorchTurns > 0 || unit.overshield ||
        unit.inspired || unit.suppressed
    val shimmer = if (hasOngoingStatus) {
        val ambient = rememberInfiniteTransition(label = "status edge shimmer")
        val level by ambient.animateFloat(
            initialValue = .40f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1900, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "embers and status aura"
        )
        level
    } else .6f

    LaunchedEffect(unit, events.lastOrNull()?.serial) {
        // This list is capped and contains only actual engine-confirmed
        // events. Consume each unit's new events in order; recomposition
        // or opening the card inspector cannot repeat a previous pulse.
        val fresh = DuelEffectEvents.forUnit(events, unit, seenSerial)
        if (fresh.isNotEmpty()) {
            seenSerial = fresh.last().serial
            for (event in fresh.takeLast(6)) {
                pulseKind = event.kind
                pulse.snapTo(1f)
                pulse.animateTo(0f, tween(360, easing = LinearEasing))
            }
            pulseKind = null
        }
    }

    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            if (width <= 0f || height <= 0f) return@Canvas
            val inset = 1.1.dp.toPx()
            val corner = CornerRadius(3.dp.toPx())

            if (unit.scorchTurns > 0 && !unit.suppressed) {
                // Two illuminated side seams and deterministically placed
                // rising embers. The art center has no flame texture.
                val ember = DuelFxPalette.scorch
                drawLine(
                    ember.copy(alpha = .39f + shimmer * .43f),
                    Offset(inset, height * .18f),
                    Offset(inset, height * .92f),
                    (1f + .7f * shimmer).dp.toPx()
                )
                drawLine(
                    ember.copy(alpha = .28f + shimmer * .36f),
                    Offset(width - inset, height * .08f),
                    Offset(width - inset, height * .83f),
                    (1f + .6f * shimmer).dp.toPx()
                )
                repeat(7) { index ->
                    val left = index % 2 == 0
                    val x = if (left) inset + (index % 3) * .9.dp.toPx()
                        else width - inset - (index % 3) * .9.dp.toPx()
                    val y = height * (
                        .14f + (index * .113f + shimmer * .17f) % .74f
                    )
                    drawCircle(
                        ember.copy(alpha = .28f + shimmer * .56f),
                        radius = (if (index % 3 == 0) 1.35f else .8f).dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }

            if (unit.overshield) {
                // A restrained hexagonal cyan perimeter with a small
                // animated glint, not an opaque shield over character art.
                val hex = Path().apply {
                    moveTo(width * .12f, inset)
                    lineTo(width * .88f, inset)
                    lineTo(width - inset, height * .12f)
                    lineTo(width - inset, height * .88f)
                    lineTo(width * .88f, height - inset)
                    lineTo(width * .12f, height - inset)
                    lineTo(inset, height * .88f)
                    lineTo(inset, height * .12f)
                    close()
                }
                drawPath(
                    hex, DuelFxPalette.shield.copy(alpha = .38f + shimmer * .38f),
                    style = Stroke(width = 1.3.dp.toPx())
                )
                drawLine(
                    DuelFxPalette.shield.copy(alpha = .35f + shimmer * .5f),
                    Offset(width * (.20f + shimmer * .40f), inset),
                    Offset(width * (.32f + shimmer * .40f), inset),
                    1.7.dp.toPx(), cap = StrokeCap.Round
                )
            }

            if (unit.inspired && !unit.suppressed) {
                drawLine(
                    DuelFxPalette.inspired.copy(alpha = .27f + shimmer * .49f),
                    Offset(width * .16f, height - inset),
                    Offset(width * .78f, height - inset),
                    (1f + shimmer).dp.toPx(), cap = StrokeCap.Round
                )
                for (i in 0 until 3) {
                    drawCircle(
                        DuelFxPalette.inspired.copy(alpha = shimmer * .55f),
                        radius = .9.dp.toPx(),
                        center = Offset(
                            width * (.24f + i * .23f),
                            height * (.83f + shimmer * -.06f)
                        )
                    )
                }
            }

            if (unit.suppressed) {
                val arc = DuelFxPalette.suppression.copy(alpha = .34f + shimmer * .42f)
                drawLine(arc,
                    Offset(width * .65f, inset),
                    Offset(width * .48f, height * .16f),
                    1.1.dp.toPx())
                drawLine(arc,
                    Offset(width * .48f, height * .16f),
                    Offset(width - inset, height * .24f),
                    1.1.dp.toPx())
                drawLine(arc,
                    Offset(inset, height * .69f),
                    Offset(width * .18f, height * .84f),
                    1.1.dp.toPx())
                drawLine(arc,
                    Offset(width * .18f, height * .84f),
                    Offset(width * .10f, height - inset),
                    1.1.dp.toPx())
            }

            val active = pulseKind
            val strength = pulse.value
            if (active != null && strength > .002f) {
                val accent = DuelFxPalette.forEvent(active)
                drawRoundRect(
                    color = accent.copy(alpha = strength * .87f),
                    cornerRadius = corner,
                    style = Stroke(width = (1.1f + 2.0f * strength).dp.toPx())
                )
                // Four outwards-moving corner sparks signal the exact
                // recipient without hiding stats or card illustrations.
                for (i in 0 until 4) {
                    val x = if (i % 2 == 0) inset else width - inset
                    val y = if (i < 2) inset else height - inset
                    val directionX = if (i % 2 == 0) 1f else -1f
                    val directionY = if (i < 2) 1f else -1f
                    drawLine(
                        accent.copy(alpha = strength * .8f),
                        Offset(x, y),
                        Offset(x + directionX * 6.dp.toPx() * strength,
                            y + directionY * 6.dp.toPx() * strength),
                        (1.2f + strength).dp.toPx()
                    )
                }
                if (active == DuelEffectKind.SCORCH_TICK) {
                    drawCircle(
                        color = DuelFxPalette.scorch.copy(alpha = strength * .42f),
                        center = Offset(width * .5f, height * .5f),
                        radius = width * (.26f + (1f - strength) * .28f),
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                }
            }
        }
    }
}

/**
 * A short golden stream between the REAL source/recipient board slots.
 * No gameplay timers wait for this animation; it is laid above the board,
 * below dialogs and floating dragged cards.
 */
@Composable
internal fun DuelPowerTransferBeam(
    event: DuelEffectEvent?,
    boundsInWindow: Map<String, Rect>,
    rootTopLeftInWindow: Offset,
    modifier: Modifier = Modifier
) {
    val travel = remember { Animatable(1f) }
    LaunchedEffect(event?.serial) {
        if (event?.kind == DuelEffectKind.POWER_TRANSFER) {
            travel.snapTo(0f)
            travel.animateTo(1f, tween(630, easing = LinearEasing))
        } else travel.snapTo(1f)
    }
    if (event == null || event.kind != DuelEffectKind.POWER_TRANSFER) return
    Canvas(modifier) {
        val source = event.source ?: return@Canvas
        val fromId = (if (source.owner.human) "0" else "1") +
            "-${source.row}-${source.column}"
        val to = event.target
        val toId = (if (to.owner.human) "0" else "1") +
            "-${to.row}-${to.column}"
        val from = boundsInWindow[fromId]?.center?.minus(rootTopLeftInWindow)
            ?: return@Canvas
        val destination = boundsInWindow[toId]?.center?.minus(rootTopLeftInWindow)
            ?: return@Canvas
        if (from == destination || travel.value >= 1f) return@Canvas

        val fraction = travel.value
        val tip = Offset(
            from.x + (destination.x - from.x) * fraction,
            from.y + (destination.y - from.y) * fraction
        )
        val tailFraction = (fraction - .25f).coerceAtLeast(0f)
        val tail = Offset(
            from.x + (destination.x - from.x) * tailFraction,
            from.y + (destination.y - from.y) * tailFraction
        )
        val fade = (1f - fraction * .45f).coerceAtLeast(0f)
        drawLine(
            DuelFxPalette.power.copy(alpha = .82f * fade),
            tail, tip,
            (1.7f + .9f * fade).dp.toPx(), cap = StrokeCap.Round
        )
        drawCircle(
            DuelFxPalette.power.copy(alpha = fade),
            radius = 2.8.dp.toPx(),
            center = tip
        )
        for (i in 0 until 3) {
            val theta = 2 * PI * i / 3
            val direction = Offset(cos(theta).toFloat(), sin(theta).toFloat())
            drawCircle(
                DuelFxPalette.inspired.copy(alpha = fade * .5f),
                radius = .9.dp.toPx(),
                center = tip + direction * (5.dp.toPx() * (1f - fraction * .4f))
            )
        }
    }
}
