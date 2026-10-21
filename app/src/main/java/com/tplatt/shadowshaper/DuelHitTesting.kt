package com.tplatt.shadowshaper

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/**
 * Screen-coordinate hit testing for the landscape battlefield.
 * Board cells and the pointer MUST both use window coordinates;
 * the floating-card overlay uses coordinates relative to its root Box.
 * Keep these conversions here rather than duplicating gesture math.
 */
internal data class DuelDropCell(val row: Int, val column: Int)

internal object DuelHitTesting {
    fun playerSlotAt(pointerInWindow: Offset, boundsInWindow: Map<String, Rect>): DuelDropCell? {
        for ((id, bounds) in boundsInWindow) {
            if (!bounds.contains(pointerInWindow)) continue
            val parts = id.split('-')
            if (parts.size != 3 || parts[0] != "0") continue
            val row = parts[1].toIntOrNull() ?: continue
            val column = parts[2].toIntOrNull() ?: continue
            if (ShadowRules.isOnBoard(row, column)) return DuelDropCell(row, column)
        }
        return null
    }

    fun floatingCardTopLeft(
        pointerInWindow: Offset,
        rootTopLeftInWindow: Offset,
        cardWidthPx: Float,
        cardHeightPx: Float
    ): Offset = Offset(
        pointerInWindow.x - rootTopLeftInWindow.x - cardWidthPx / 2f,
        pointerInWindow.y - rootTopLeftInWindow.y - cardHeightPx * 0.64f
    )
}
