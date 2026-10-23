package com.tplatt.shadowshaper

import kotlin.math.min

/**
 * Pure sizing rules used to fit actual portrait-shaped cards inside the
 * 4-row portrait game board. Inputs/outputs are logical dp values.
 * This never changes the number of slots or their hit regions.
 */
internal data class DuelCardDimensions(val width: Float, val height: Float)

internal object DuelLayoutMetrics {
    const val CARD_WIDTH_TO_HEIGHT = 0.72f
    // Preserve the original natural rock borders in the supplied image.
    // Modestly widen real card slots compared with the original 70% grid;
    // hit testing, row guides, and art all share this same lane width.
    const val PORTRAIT_LANE_WIDTH_FRACTION = 0.80f
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

    /**
     * Four card rows, seven legal positions in each. The original
     * client presents the rows inside a larger stone field; the surrounding
     * space is reserved for the Power/Light emblems and End Turn control.
     */
    fun portraitBoardHeight(screenWidthDp: Int, screenHeightDp: Int): Float =
        min(
            screenWidthDp.coerceAtLeast(250) * 0.82f,
            (screenHeightDp - portraitHandHeight(screenHeightDp) - 215f)
                .coerceAtLeast(145f)
        )

    /** More room for card art on phones without obscuring the board. */
    fun portraitHandHeight(screenHeightDp: Int): Int = when {
        screenHeightDp < 630 -> 122
        screenHeightDp < 760 -> 144
        else -> 156
    }

    /** Keep small hands unstacked; overlap only when there are many cards. */
    fun portraitHandSpacing(handCount: Int, screenWidthDp: Int): Int {
        val available = (screenWidthDp - 75).coerceAtLeast(180)
        val width = if (screenWidthDp < 380) 70 else 76
        if (handCount <= 1) return 0
        return ((available - width * handCount) / (handCount - 1))
            .coerceIn(-19, 4)
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
