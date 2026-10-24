package com.tplatt.shadowshaper

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.matchParentSize
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

/**
 * Display-only battlefield animations driven exclusively by the real engine.
 * Entrance occurs once for each newly placed/summoned unit. A change in
 * resolved Power produces a brief buff (gold/green) or impact (red) pulse.
 * No turn, ability, target, or placement rule is altered by these effects.
 */
@Composable
internal fun DuelAnimatedUnit(
    unit: ShadowEngine.Unit,
    currentPower: Int,
    placementRevision: Int,
    isNewestPlacement: Boolean,
    modifier: Modifier = Modifier
) {
    val scale = remember(unit) { Animatable(1f) }
    val opacity = remember(unit) { Animatable(1f) }
    val arrival = remember(unit) { Animatable(0f) }
    val impact = remember(unit) { Animatable(0f) }
    var previousPower by remember(unit) { mutableIntStateOf(currentPower) }
    var powerDelta by remember(unit) { mutableIntStateOf(0) }

    LaunchedEffect(unit, placementRevision, isNewestPlacement) {
        if (isNewestPlacement && placementRevision > 0) {
            scale.snapTo(0.72f)
            opacity.snapTo(0.38f)
            arrival.snapTo(1f)
            launch { opacity.animateTo(1f, tween(durationMillis = 270)) }
            launch {
                arrival.animateTo(
                    0f, tween(durationMillis = 510, easing = FastOutSlowInEasing)
                )
            }
            scale.animateTo(1f, tween(durationMillis = 360, easing = FastOutSlowInEasing))
        } else {
            scale.snapTo(1f)
            opacity.snapTo(1f)
            arrival.snapTo(0f)
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

    val entrance = arrival.value
    val hit = impact.value
    val healing = powerDelta > 0
    Box(
        modifier
            .shadow(
                elevation = if (entrance > 0f) 10.dp else 5.dp,
                shape = RoundedCornerShape(4.dp)
            )
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                alpha = opacity.value
            }
    ) {
        // Light spills outside the card during entry, including token
        // summoning; negative-Power impact flashes crimson instead.
        if (entrance > 0f || hit > 0f) {
            Canvas(
                Modifier.matchParentSize().graphicsLayer {
                    scaleX = 1f + (entrance * .21f + hit * .12f)
                    scaleY = 1f + (entrance * .21f + hit * .12f)
                }
            ) {
                val entryColor = if (unit.card.rarity.equals("Exotic", true))
                    Color(0xFFEBD08C)
                else Color(0xFFADD8FF)
                val hitColor = if (healing) Color(0xFF98F2C3) else Color(0xFFFF716A)
                val color = if (hit > entrance) hitColor else entryColor
                val intensity = maxOf(entrance, hit)
                drawRoundRect(
                    color = color.copy(alpha = (intensity * .77f).coerceIn(0f, 1f)),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                    style = Stroke(width = (1f + intensity * 2.3f).dp.toPx())
                )
                // Brief diagonal strike/ability tracer conveys impact without
                // covering the printed card art or inventing an attack event.
                if (hit > .12f) {
                    drawLine(
                        color = hitColor.copy(alpha = hit * .85f),
                        start = Offset(size.width * .12f, size.height * .77f),
                        end = Offset(size.width * .83f, size.height * .20f),
                        strokeWidth = (1f + hit * 2f).dp.toPx()
                    )
                }
            }
        }
        DuelCardFrame(
            card = unit.card,
            power = currentPower,
            modifier = Modifier.fillMaxSize(),
            inspired = unit.inspired,
            suppressed = unit.suppressed,
            scorched = unit.scorchTurns > 0
        )
        if (hit > .025f) {
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
