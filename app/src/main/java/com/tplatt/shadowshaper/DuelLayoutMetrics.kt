package com.tplatt.shadowshaper

import kotlin.math.min

/**
 * Pure sizing rules used to fit actual portrait-shaped cards inside the
 * 4-row landscape game board. Inputs/outputs are logical dp values.
 * This never changes the number of slots or their hit regions.
 */
internal data class DuelCardDimensions(val width: Float, val height: Float)

internal object DuelLayoutMetrics {
    const val CARD_WIDTH_TO_HEIGHT = 0.72f
    private const val WIDTH_INSET_DP = 2f
    private const val HEIGHT_INSET_DP = 2f

    fun fittingCard(slotWidth: Float, slotHeight: Float): DuelCardDimensions {
        if (slotWidth <= 0f || slotHeight <= 0f)
            return DuelCardDimensions(0f, 0f)
        val availableWidth = (slotWidth - WIDTH_INSET_DP).coerceAtLeast(0f)
        val availableHeight = (slotHeight - HEIGHT_INSET_DP).coerceAtLeast(0f)
        val width = min(availableWidth, availableHeight * CARD_WIDTH_TO_HEIGHT)
        return DuelCardDimensions(width, width / CARD_WIDTH_TO_HEIGHT)
    }

    fun handHeight(screenHeightDp: Int): Int = when {
        screenHeightDp < 340 -> 94
        screenHeightDp < 410 -> 108
        else -> 128
    }

    fun leftHudWidth(screenWidthDp: Int): Int =
        if (screenWidthDp < 790) 80 else 102

    fun rightHudWidth(screenWidthDp: Int): Int =
        if (screenWidthDp < 790) 112 else 132
}
