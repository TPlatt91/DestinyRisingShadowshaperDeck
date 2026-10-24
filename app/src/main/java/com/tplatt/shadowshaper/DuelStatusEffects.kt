package com.tplatt.shadowshaper

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Distinct colours are consistent on the user's and opponent's cards. */
internal object DuelFxPalette {
    val scorch = Color(0xFFFF742F)
    val suppression = Color(0xFFAD86FF)
    val inspired = Color(0xFF89FFD2)
    val shield = Color(0xFF67CFFF)
    val power = Color(0xFFFFD57E)

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
 * Actual status is read from the engine; an activation is only displayed
 * after a typed engine event. None of these drawings affect targeting,
 * touch hitboxes, combat outcomes or turn order.
 *
 * Full: bold, moving rim + embers/arcs + 900ms ability flash.
 * Reduced: static colored outline + shorter event flash.
 * Off: no new effect; the existing status dots remain visible.
 */
@Composable
internal fun DuelCardStatusFx(
    unit: ShadowEngine.Unit,
    events: List<DuelEffectEvent>,
    modifier: Modifier = Modifier,
    mode: DuelFxMode = DuelFxMode.FULL
) {
    if (mode == DuelFxMode.OFF) return

    val burst = remember(unit) { Animatable(0f) }
    var burstKind by remember(unit) { mutableStateOf<DuelEffectKind?>(null) }
    var burstAmount by remember(unit) { mutableIntStateOf(0) }
    var seenSerial by remember(unit) { mutableStateOf(0L) }
    val hasStatus = (unit.scorchTurns > 0 && !unit.suppressed) ||
        unit.overshield || unit.inspired || unit.suppressed

    // Only cards with active status spend frames animating. Many ordinary
    // cards in a full 28-slot field have no permanent effects at all.
    val orbit = if (mode == DuelFxMode.FULL && hasStatus) {
        val timeline = rememberInfiniteTransition(label = "continuous card status")
        val frame by timeline.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(2200, easing = LinearEasing), RepeatMode.Restart
            ),
            label = "ember travel / electric phase / shield scan"
        )
        frame
    } else .5f
    val wave = .5f + .5f * sin(orbit * 2.0 * PI).toFloat()

    LaunchedEffect(unit, events.lastOrNull()?.serial, mode) {
        // Each confirmed trigger is replayed at most once. Even if the
        // same card receives a second buff in this action, its activation
        // does not require a separate engine turn or layout pass.
        val fresh = DuelEffectEvents.forUnit(events, unit, seenSerial)
        if (fresh.isNotEmpty()) {
            seenSerial = fresh.last().serial
            for (event in fresh.takeLast(5)) {
                burstKind = event.kind
                burstAmount = event.amount
                burst.snapTo(1f)
                burst.animateTo(
                    0f, tween(
                        if (mode == DuelFxMode.FULL) 850 else 310,
                        easing = LinearEasing
                    )
                )
            }
            burstKind = null
        }
    }

    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w < 8f || h < 8f) return@Canvas
            val rim = 1.3.dp.toPx()
            val thick = if (mode == DuelFxMode.FULL) 2.8.dp.toPx() else 1.65.dp.toPx()
            val rounding = CornerRadius(4.dp.toPx())

            // Status identity is visible even on small, crowded cards:
            // broad animated outline and a second inner highlight.
            val statusColor = when {
                unit.suppressed -> DuelFxPalette.suppression
                unit.scorchTurns > 0 -> DuelFxPalette.scorch
                unit.overshield -> DuelFxPalette.shield
                unit.inspired -> DuelFxPalette.inspired
                else -> null
            }
            if (statusColor != null) {
                drawRoundRect(
                    color = statusColor.copy(alpha = if (mode == DuelFxMode.FULL)
                        .20f + wave * .24f else .24f),
                    cornerRadius = rounding,
                    style = Stroke(width = 7.5.dp.toPx())
                )
                drawRoundRect(
                    color = statusColor.copy(alpha = if (mode == DuelFxMode.FULL)
                        .82f + wave * .18f else .66f),
                    cornerRadius = rounding,
                    style = Stroke(width = thick)
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = if (mode == DuelFxMode.FULL)
                        .28f + wave * .27f else .23f),
                    topLeft = Offset(rim * 2f, rim * 2f),
                    size = androidx.compose.ui.geometry.Size(
                        (w - rim * 4f).coerceAtLeast(1f),
                        (h - rim * 4f).coerceAtLeast(1f)
                    ),
                    cornerRadius = rounding,
                    style = Stroke(width = .65.dp.toPx())
                )
            }

            if (mode == DuelFxMode.FULL) {
                if (unit.scorchTurns > 0 && !unit.suppressed) {
                    // Rising flame tongues are constrained to the outer
                    // 10-14% of the art instead of masking the character.
                    for (side in 0..1) {
                        val x = if (side == 0) 0f else w
                        val dir = if (side == 0) 1f else -1f
                        for (i in 0 until 4) {
                            val lift = (orbit + i * .29f) % 1f
                            val baseY = h * (1.03f - lift * 1.18f)
                            val height = h * (.16f + .12f * ((i % 3) / 2f))
                            val tongue = Path().apply {
                                moveTo(x, baseY + height * .20f)
                                quadraticBezierTo(
                                    x + dir * w * .17f, baseY - height * .18f,
                                    x + dir * w * .045f, baseY - height
                                )
                                quadraticBezierTo(
                                    x + dir * w * .10f, baseY - height * .24f,
                                    x, baseY + height * .20f
                                )
                                close()
                            }
                            drawPath(
                                tongue,
                                DuelFxPalette.scorch.copy(
                                    alpha = (.22f + .60f * (1f - lift)).coerceIn(0f, 1f)
                                )
                            )
                        }
                    }
                    for (i in 0 until 13) {
                        val rise = (orbit + i * .137f) % 1f
                        val left = i % 2 == 0
                        val x = if (left) w * (.045f + (i % 4) * .028f)
                            else w * (.955f - (i % 4) * .028f)
                        drawCircle(
                            color = if (i % 3 == 0) Color(0xFFFFE7A2)
                                else DuelFxPalette.scorch,
                            radius = (1.0f + (i % 3) * .42f).dp.toPx(),
                            center = Offset(x, h * (1f - rise)),
                            alpha = (.35f + .65f * (1f - rise)).coerceIn(0f, 1f)
                        )
                    }
                }

                if (unit.suppressed) {
                    // Zigzag violet lightning on BOTH edges, with bright
                    // white hot-spots. Suppression is not just a tiny icon.
                    for (side in 0..1) {
                        val x = if (side == 0) rim else w - rim
                        val dir = if (side == 0) 1f else -1f
                        val firstY = h * ((orbit + side * .41f) % 1f)
                        val sparkPath = Path().apply {
                            moveTo(x, firstY - h * .25f)
                            lineTo(x + dir * w * .13f, firstY - h * .13f)
                            lineTo(x + dir * w * .04f, firstY + h * .02f)
                            lineTo(x + dir * w * .20f, firstY + h * .13f)
                            lineTo(x, firstY + h * .28f)
                        }
                        drawPath(
                            sparkPath, DuelFxPalette.suppression.copy(alpha = .9f),
                            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawCircle(
                            Color.White.copy(alpha = .55f + .40f * wave),
                            radius = 2.dp.toPx(),
                            center = Offset(x + dir * w * .04f, firstY + h * .02f)
                        )
                    }
                    for (i in 0 until 3) {
                        drawLine(
                            Color(0xFFD5C8FF).copy(alpha = .32f + .48f * wave),
                            Offset(w * (.13f + i * .29f), h * .035f),
                            Offset(w * (.21f + i * .29f), h * .11f),
                            strokeWidth = 1.35.dp.toPx()
                        )
                    }
                }

                if (unit.inspired && !unit.suppressed) {
                    // Three sustained rising columns and a luminous base;
                    // show that the card is energized without a solid tint.
                    drawLine(
                        DuelFxPalette.inspired.copy(alpha = .80f + .20f * wave),
                        Offset(w * .08f, h - rim),
                        Offset(w * .92f, h - rim),
                        strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round
                    )
                    for (i in 0 until 7) {
                        val lift = (orbit + i * .169f) % 1f
                        val x = w * (.07f + i * .144f)
                        val y = h * (.90f - lift * .62f)
                        drawLine(
                            DuelFxPalette.inspired.copy(alpha = .25f + .66f * (1f - lift)),
                            Offset(x, y + h * .12f),
                            Offset(x + w * .025f, y),
                            strokeWidth = (1.0f + (i % 3) * .4f).dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawCircle(
                            Color(0xFFD9FFF0).copy(alpha = .75f * (1f - lift)),
                            radius = 1.25.dp.toPx(),
                            center = Offset(x + w * .025f, y)
                        )
                    }
                }

                if (unit.overshield) {
                    // Multiple layers make this look like a real glowing
                    // energy bubble, not a near-invisible 1dp border.
                    val shield = Path().apply {
                        moveTo(w * .18f, rim)
                        lineTo(w * .82f, rim)
                        lineTo(w - rim, h * .10f)
                        lineTo(w - rim, h * .90f)
                        lineTo(w * .82f, h - rim)
                        lineTo(w * .18f, h - rim)
                        lineTo(rim, h * .90f)
                        lineTo(rim, h * .10f)
                        close()
                    }
                    drawPath(
                        shield, DuelFxPalette.shield.copy(alpha = .87f),
                        style = Stroke(width = 2.6.dp.toPx())
                    )
                    drawPath(
                        shield, Color.White.copy(alpha = .24f + .37f * wave),
                        style = Stroke(width = .8.dp.toPx())
                    )
                    // Traveling horizontal light bar follows the protected
                    // card. Limited to the card edges, not its printed text.
                    val scanY = h * (.08f + .84f * orbit)
                    drawLine(
                        Color(0xFFE0F8FF).copy(alpha = .66f),
                        Offset(rim, scanY),
                        Offset(w * .16f, scanY - h * .06f),
                        1.8.dp.toPx(), cap = StrokeCap.Round
                    )
                    drawLine(
                        Color(0xFFE0F8FF).copy(alpha = .66f),
                        Offset(w - rim, scanY),
                        Offset(w * .84f, scanY - h * .06f),
                        1.8.dp.toPx(), cap = StrokeCap.Round
                    )
                    for (i in 0 until 4) {
                        val x = if (i % 2 == 0) w * .18f else w * .82f
                        val y = if (i < 2) rim else h - rim
                        drawCircle(
                            Color(0xFFE4FBFF).copy(alpha = .55f + .45f * wave),
                            radius = 1.75.dp.toPx(), center = Offset(x, y)
                        )
                    }
                }
            }

            // Strong activation shock: ~850ms, expanding card-sized ring,
            // bright corner sparks and a short lateral energy flash.
            val kind = burstKind
            val energy = burst.value
            if (kind != null && energy > .005f) {
                val color = DuelFxPalette.forEvent(kind)
                val expanded = (1f - energy)
                val outer = expanded * 5.5.dp.toPx()
                drawRoundRect(
                    color.copy(alpha = .70f * energy),
                    topLeft = Offset(-outer, -outer),
                    size = androidx.compose.ui.geometry.Size(
                        w + outer * 2f, h + outer * 2f
                    ),
                    cornerRadius = CornerRadius(5.dp.toPx()),
                    style = Stroke(width = (3.2f + energy * 2.8f).dp.toPx())
                )
                drawRoundRect(
                    Color.White.copy(alpha = energy * .53f),
                    cornerRadius = rounding,
                    style = Stroke(width = (1f + energy * 3f).dp.toPx())
                )
                for (i in 0 until 10) {
                    val angle = (2.0 * PI * i / 10.0)
                    val cx = if (i % 2 == 0) 0f else w
                    val cy = h * (.12f + i * .083f)
                    val dx = cos(angle).toFloat() * 9.dp.toPx() * expanded
                    val dy = sin(angle).toFloat() * 12.dp.toPx() * expanded
                    drawLine(
                        color.copy(alpha = .92f * energy),
                        Offset(cx, cy),
                        Offset(cx + dx, cy + dy),
                        strokeWidth = (1.5f + energy * 1.5f).dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
                if (kind == DuelEffectKind.SCORCH_APPLIED ||
                    kind == DuelEffectKind.SCORCH_TICK ||
                    kind == DuelEffectKind.OVERSHIELD_BLOCKED) {
                    drawCircle(
                        color.copy(alpha = .45f * energy),
                        center = Offset(w * .5f, h * .52f),
                        radius = w * (.17f + expanded * .65f),
                        style = Stroke(width = 2.3.dp.toPx())
                    )
                }
            }
        }

        // One-off readable status name: stays <1 second and vanishes
        // completely, so it never obscures the printed card art afterward.
        val active = burstKind
        val alpha = (burst.value * 1.4f).coerceIn(0f, 1f)
        if (active != null && alpha > .08f && mode == DuelFxMode.FULL) {
            val label = when (active) {
                DuelEffectKind.SCORCH_APPLIED -> "SCORCHED"
                DuelEffectKind.SCORCH_TICK -> "BURN"
                DuelEffectKind.SUPPRESSED -> "JAMMED"
                DuelEffectKind.INSPIRED -> "INSPIRED"
                DuelEffectKind.OVERSHIELD_APPLIED -> "SHIELD"
                DuelEffectKind.OVERSHIELD_BLOCKED -> "BLOCKED"
                DuelEffectKind.POWER_TRANSFER, DuelEffectKind.POWER_GAINED ->
                    if (burstAmount > 0) "+" + burstAmount else "POWER"
            }
            Text(
                text = label,
                color = DuelFxPalette.forEvent(active),
                fontSize = 7.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                modifier = Modifier.align(Alignment.Center)
                    .graphicsLayer {
                        this.alpha = alpha
                        scaleX = 1.05f + (1f - burst.value) * .06f
                        scaleY = scaleX
                    }
                    .background(Color(0xE60A1118), RoundedCornerShape(3.dp))
                    .padding(horizontal = 3.dp, vertical = 3.dp)
            )
        }
    }
}

/**
 * Brighter, three-layer Power stream. Source flare, curved charged trail,
 * repeated moving sparks and target impact are drawn entirely over the
 * measured field without changing the card or intercepting gestures.
 */
@Composable
internal fun DuelPowerTransferBeam(
    event: DuelEffectEvent?,
    boundsInWindow: Map<String, Rect>,
    rootTopLeftInWindow: Offset,
    modifier: Modifier = Modifier
) {
    val travel = remember { Animatable(1f) }
    LaunchedEffect(event?.serial, event?.source, event?.target) {
        if (event?.kind == DuelEffectKind.POWER_TRANSFER) {
            travel.snapTo(0f)
            travel.animateTo(1f, tween(1050, easing = LinearEasing))
        } else {
            travel.snapTo(1f)
        }
    }
    if (event?.kind != DuelEffectKind.POWER_TRANSFER) return
    Canvas(modifier) {
        val source = event.source ?: return@Canvas
        val target = event.target
        if (source.owner.board[source.row][source.column] !== source ||
            target.owner.board[target.row][target.column] !== target) return@Canvas
        val fromId = (if (source.owner.human) "0" else "1") +
            "-" + source.row + "-" + source.column
        val toId = (if (target.owner.human) "0" else "1") +
            "-" + target.row + "-" + target.column
        val from = boundsInWindow[fromId]?.center?.minus(rootTopLeftInWindow)
            ?: return@Canvas
        val destination = boundsInWindow[toId]?.center?.minus(rootTopLeftInWindow)
            ?: return@Canvas
        if (from == destination || travel.value >= 1f) return@Canvas

        val progress = travel.value
        val dx = destination.x - from.x
        val dy = destination.y - from.y
        val distance = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        val bend = (distance * .19f).coerceAtMost(56.dp.toPx())
        val sideX = -dy / distance
        val sideY = dx / distance
        fun point(t: Float): Offset {
            val curve = sin(t * PI).toFloat() * bend
            return Offset(
                from.x + dx * t + sideX * curve,
                from.y + dy * t + sideY * curve
            )
        }

        // A faint guide reveals which ally is being empowered even before
        // the gold bolt reaches it.
        drawLine(
            DuelFxPalette.power.copy(alpha = .16f * (1f - progress)),
            from, destination, strokeWidth = 3.dp.toPx()
        )
        val trailStart = (progress - .46f).coerceAtLeast(0f)
        val steps = 14
        for (i in 0 until steps) {
            val t0 = trailStart + (progress - trailStart) * i / steps
            val t1 = trailStart + (progress - trailStart) * (i + 1) / steps
            val brightness = (i + 1f) / steps
            val p0 = point(t0)
            val p1 = point(t1)
            val color = DuelFxPalette.power.copy(alpha = brightness * .76f)
            // Wide diffuse halo followed by a brilliant central filament.
            drawLine(color.copy(alpha = brightness * .22f),
                p0, p1, strokeWidth = 13.dp.toPx(), cap = StrokeCap.Round)
            drawLine(color, p0, p1,
                strokeWidth = 4.5.dp.toPx(), cap = StrokeCap.Round)
            drawLine(Color.White.copy(alpha = brightness * .70f),
                p0, p1, strokeWidth = 1.6.dp.toPx(), cap = StrokeCap.Round)
        }
        val tip = point(progress)
        drawCircle(
            DuelFxPalette.power.copy(alpha = .26f),
            radius = 13.dp.toPx(), center = tip
        )
        drawCircle(
            Color(0xFFFFF5D1), radius = 4.dp.toPx(), center = tip
        )
        for (i in 0 until 9) {
            val angle = 2.0 * PI * i / 9.0 + progress * PI * 4.0
            val radius = (4.0f + (i % 3) * 3f).dp.toPx()
            drawCircle(
                Color(0xFFFFE3A2).copy(alpha = .55f + .35f * (i % 2)),
                radius = (1.1f + (i % 3) * .4f).dp.toPx(),
                center = tip + Offset(
                    cos(angle).toFloat() * radius,
                    sin(angle).toFloat() * radius
                )
            )
        }

        // Distinct source and target rings make the transfer visible even
        // when cards are adjacent and the beam is physically very short.
        if (progress < .60f) {
            val halo = 1f - progress / .60f
            drawCircle(
                DuelFxPalette.power.copy(alpha = halo * .85f),
                radius = 7.dp.toPx() + (1f - halo) * 10.dp.toPx(),
                center = from, style = Stroke(width = 3.dp.toPx())
            )
        }
        if (progress > .62f) {
            val arrived = (progress - .62f) / .38f
            val alpha = (1f - arrived).coerceIn(0f, 1f)
            drawCircle(
                DuelFxPalette.power.copy(alpha = .96f * alpha),
                radius = (7f + arrived * 22f).dp.toPx(),
                center = destination, style = Stroke(width = 4.dp.toPx())
            )
            drawCircle(
                Color.White.copy(alpha = .20f * alpha),
                radius = (3f + arrived * 9f).dp.toPx(),
                center = destination
            )
        }
    }
}
