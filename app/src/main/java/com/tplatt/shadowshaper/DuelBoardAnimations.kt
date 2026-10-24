package com.tplatt.shadowshaper

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * All movement is presentation-only. The ShadowEngine commits a play before
 * this composable appears; tap, drag, targeted effects and game rules remain
 * authoritative. A real newly placed card:
 *
 *  1. travels from the hand (or drops from above when AI/summoned),
 *  2. floats briefly above its legal position,
 *  3. slams down and compresses before recoiling into place,
 *  4. emits a short shock ring and rarity-tinted sparks.
 *
 * It cannot replay just because a user opens the battle log or recomposes.
 * Resolved Power changes retain their separate hit/buff cue.
 */
@Composable
internal fun DuelAnimatedUnit(
    unit: ShadowEngine.Unit,
    currentPower: Int,
    placementRevision: Int,
    isNewestPlacement: Boolean,
    modifier: Modifier = Modifier,
    entryFromHand: Offset? = null
) {
    // Initial 0 prevents a one-frame settled-card flash at first placement.
    val landing = remember(unit) {
        Animatable(if (isNewestPlacement && placementRevision > 0) 0f else 1f)
    }
    var animatedRevision by remember(unit) { mutableIntStateOf(-1) }
    val impact = remember(unit) { Animatable(0f) }
    var previousPower by remember(unit) { mutableIntStateOf(currentPower) }
    var powerDelta by remember(unit) { mutableIntStateOf(0) }

    LaunchedEffect(unit, placementRevision, isNewestPlacement) {
        if (isNewestPlacement && placementRevision > 0 &&
            animatedRevision != placementRevision) {
            animatedRevision = placementRevision
            landing.snapTo(0f)
            landing.animateTo(
                1f,
                tween(
                    durationMillis = DuelLandingMotion.DURATION_MS,
                    easing = LinearEasing
                )
            )
        } else if (!isNewestPlacement || placementRevision <= 0) {
            landing.snapTo(1f)
        }
    }

    LaunchedEffect(unit, currentPower) {
        val delta = currentPower - previousPower
        previousPower = currentPower
        if (delta != 0) {
            powerDelta = delta
            impact.snapTo(1f)
            impact.animateTo(
                0f, tween(durationMillis = 630, easing = FastOutSlowInEasing)
            )
        }
    }

    val pixelsPerDp = androidx.compose.ui.platform.LocalDensity.current.density
    val origin = entryFromHand
    val motion = DuelLandingMotion.at(
        progress = landing.value,
        originX = origin?.x ?: 0f,
        originY = origin?.y ?: -52.dp.value * pixelsPerDp,
        cardHeightPx = 70.dp.value * pixelsPerDp,
        clockwise = unit.column % 2 == 0
    )
    val landingImpact = motion.impact
    val hit = impact.value
    val healing = powerDelta > 0
    val rarity = unit.card.rarity.lowercase()
    val burstColor = when (rarity) {
        "exotic" -> Color(0xFFF2CF76)
        "mythic" -> Color(0xFFE99282)
        "legendary" -> Color(0xFFCCA6F5)
        else -> Color(0xFFB1D9FB)
    }

    Box(modifier) {
        // A brief ring and individual sparks stay anchored to the actual
        // slot while the artwork moves. No extra overlay obscures card art.
        if (landingImpact > .001f) {
            Canvas(Modifier.matchParentSize()) {
                val intensity = landingImpact.coerceIn(0f, 1f)
                val spread = 1f - intensity
                val center = Offset(size.width * .50f, size.height * .72f)
                val radius = size.width * (.20f + spread * .58f)
                drawCircle(
                    color = burstColor.copy(alpha = intensity * .63f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = (1f + intensity * 2.4f).dp.toPx())
                )
                drawLine(
                    color = burstColor.copy(alpha = intensity * .58f),
                    start = Offset(center.x - radius * 1.10f, center.y),
                    end = Offset(center.x + radius * 1.10f, center.y),
                    strokeWidth = (1f + intensity).dp.toPx()
                )
                for (i in 0 until 12) {
                    val angle = 2.0 * PI * (i / 12.0) +
                        (if (i % 2 == 0) .14 else -.10)
                    val dx = cos(angle).toFloat()
                    val dy = sin(angle).toFloat()
                    val particleRadius = size.width * (.16f + spread * (if (i % 3 == 0) .88f else .56f))
                    val point = center + Offset(dx * particleRadius, dy * particleRadius)
                    drawCircle(
                        color = burstColor.copy(alpha = intensity * (if (i % 3 == 0) .95f else .64f)),
                        radius = (if (i % 4 == 0) 1.9f else 1.1f).dp.toPx(),
                        center = point
                    )
                }
            }
        }

        Box(
            Modifier.fillMaxSize()
                .graphicsLayer {
                    translationX = motion.offsetX
                    translationY = motion.offsetY
                    rotationZ = motion.tiltDegrees
                    scaleX = motion.scale
                    scaleY = motion.scale
                    alpha = motion.opacity
                }
                .shadow(
                    elevation = if (landing.value < 1f) 12.dp else 5.dp,
                    shape = RoundedCornerShape(4.dp)
                )
        ) {
            // The full card face remains visible, including the existing
            // on-card Power and genuine status effects.
            DuelCardFrame(
                card = unit.card,
                power = currentPower,
                modifier = Modifier.fillMaxSize(),
                inspired = unit.inspired,
                suppressed = unit.suppressed,
                scorched = unit.scorchTurns > 0
            )

            // Resolved damage/buff effect; never fire on a mere card summon.
            if (hit > .001f) {
                Canvas(Modifier.matchParentSize()) {
                    val color = if (healing) Color(0xFF90F1B7) else Color(0xFFFF786F)
                    drawRoundRect(
                        color = color.copy(alpha = hit * .78f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                        style = Stroke(width = (1f + hit * 2.3f).dp.toPx())
                    )
                    if (!healing) {
                        drawLine(
                            color = color.copy(alpha = hit * .92f),
                            start = Offset(size.width * .12f, size.height * .75f),
                            end = Offset(size.width * .83f, size.height * .20f),
                            strokeWidth = (1f + hit * 2f).dp.toPx()
                        )
                    }
                }
                Text(
                    text = if (powerDelta > 0) "+$powerDelta" else "$powerDelta",
                    color = if (healing) Color(0xFFA8FFCD) else Color(0xFFFFB1AC),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            translationY = -18.dp.toPx() * (1f - hit)
                            alpha = hit
                        }
                        .background(Color(0x99141821), RoundedCornerShape(3.dp))
                )
            }
        }
    }
}
