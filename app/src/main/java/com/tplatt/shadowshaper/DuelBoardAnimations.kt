package com.tplatt.shadowshaper

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch

/**
 * Display-only card entry animation. This component never changes Power,
 * turn order, board slots, targets or card-effect resolution.
 *
 * The revision and owner/position originate from the game engine, so an
 * ordinary recomposition (e.g. toggling the match log) never replays an
 * animation. Cards already on the board remain static.
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

    LaunchedEffect(unit, placementRevision, isNewestPlacement) {
        if (isNewestPlacement && placementRevision > 0) {
            scale.snapTo(0.79f)
            opacity.snapTo(0.48f)
            launch { opacity.animateTo(1f, tween(durationMillis = 260)) }
            scale.animateTo(1f, tween(durationMillis = 330, easing = FastOutSlowInEasing))
        } else {
            scale.snapTo(1f)
            opacity.snapTo(1f)
        }
    }

    Box(modifier.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
        alpha = opacity.value
    }) {
        DuelCardFrame(
            card = unit.card,
            power = currentPower,
            modifier = Modifier.fillMaxSize(),
            inspired = unit.inspired,
            suppressed = unit.suppressed,
            scorched = unit.scorchTurns > 0
        )
    }
}
