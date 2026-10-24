package com.tplatt.shadowshaper

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * One swipe = one adjacent card. Swipe LEFT -> next, RIGHT -> previous.
 * No wraparound at either end; this works for filtered binder pages, a
 * selected deck, and the live hand without selecting a hidden/unlisted card.
 */
internal object DuelCardSwipeNavigation {
    const val MIN_SWIPE_DP = 56f

    fun adjacent(index: Int, count: Int, delta: Int): Int? {
        if (count <= 0 || index !in 0 until count || delta !in -1..1 || delta == 0)
            return null
        val next = index + delta
        return next.takeIf { it in 0 until count }
    }

    fun direction(horizontalPixels: Float, minimumPixels: Float): Int {
        if (minimumPixels <= 0f || abs(horizontalPixels) < minimumPixels) return 0
        return if (horizontalPixels < 0f) 1 else -1
    }

}

/**
 * Attach to a card-inspection surface, NOT its vertical scrollable effect text.
 * Compose's horizontal drag recognizer only claims horizontal movement;
 * reading long printed effects still scrolls vertically.
 */
internal fun Modifier.duelSwipeToBrowse(
    enabled: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
): Modifier = if (!enabled) this else this.pointerInput(enabled, onPrevious, onNext) {
    var accumulatedX = 0f
    val minimum = DuelCardSwipeNavigation.MIN_SWIPE_DP.dp.toPx()
    detectHorizontalDragGestures(
        onDragStart = { accumulatedX = 0f },
        onHorizontalDrag = { change, delta ->
            accumulatedX += delta
            change.consume()
        },
        onDragEnd = {
            when (DuelCardSwipeNavigation.direction(accumulatedX, minimum)) {
                -1 -> onPrevious()
                1 -> onNext()
            }
        },
        onDragCancel = { accumulatedX = 0f }
    )
}

/** Buttons mirror the gesture and stay usable with touch accessibility. */
@Composable
internal fun DuelCardPagerControls(
    index: Int,
    count: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (count <= 1) return
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(
            onClick = onPrevious,
            enabled = DuelCardSwipeNavigation.adjacent(index, count, -1) != null
        ) {
            Text("‹ PREV", color = if (index > 0) DuelPopupColors.goldSoft
                else DuelPopupColors.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            "${index + 1} / $count  •  SWIPE ↔",
            color = DuelPopupColors.muted,
            fontSize = 10.sp
        )
        TextButton(
            onClick = onNext,
            enabled = DuelCardSwipeNavigation.adjacent(index, count, 1) != null
        ) {
            Text("NEXT ›", color = if (index < count - 1) DuelPopupColors.goldSoft
                else DuelPopupColors.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
