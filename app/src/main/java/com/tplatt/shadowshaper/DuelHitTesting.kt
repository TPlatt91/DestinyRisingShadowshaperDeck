package com.tplatt.shadowshaper

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/**
 * Screen-coordinate hit testing for the portrait battlefield.
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

    /**
     * Resolve the visual drag position using the SAME window-space cell
     * rectangles used for tapping. The floating card is drawn 0.64 of its
     * height above the finger; its visible center is therefore 0.14 of its
     * height above the finger. A finger just below a highlighted row should
     * still land the card if its visible center occupies that row.
     *
     * Never guess across occupied/illegal cells or onto the CPU field.
     * [legal] comes from the engine's isPlayablePlacement(), so drag
     * acceptance cannot become more permissive than tap acceptance.
     */
    fun playerSlotForDrop(
        pointerInWindow: Offset,
        boundsInWindow: Map<String, Rect>,
        cardHeightPx: Float,
        legal: Set<DuelDropCell>
    ): DuelDropCell? {
        if (legal.isEmpty()) return null

        // An actual fingertip hit is authoritative. Do not snap from an
        // explicitly illegal cell into its legal neighbor.
        val fingertip = playerSlotAt(pointerInWindow, boundsInWindow)
        if (fingertip != null) return fingertip.takeIf { it in legal }

        val cardCenter = Offset(
            pointerInWindow.x,
            pointerInWindow.y - cardHeightPx.coerceAtLeast(0f) * 0.14f
        )
        val visibleCenter = playerSlotAt(cardCenter, boundsInWindow)
        if (visibleCenter != null) return visibleCenter.takeIf { it in legal }

        // Small tolerance for finger release near the edge of the final
        // highlighted slot, not a far-away or nearest-anywhere teleport.
        var best: DuelDropCell? = null
        var bestDistance = Float.POSITIVE_INFINITY
        for (cell in legal) {
            val rect = boundsInWindow["0-${cell.row}-${cell.column}"] ?: continue
            val marginX = rect.width * 0.18f
            val marginY = rect.height * 0.22f
            if (cardCenter.x < rect.left - marginX ||
                cardCenter.x > rect.right + marginX ||
                cardCenter.y < rect.top - marginY ||
                cardCenter.y > rect.bottom + marginY) continue

            val dx = when {
                cardCenter.x < rect.left -> rect.left - cardCenter.x
                cardCenter.x > rect.right -> cardCenter.x - rect.right
                else -> 0f
            }
            val dy = when {
                cardCenter.y < rect.top -> rect.top - cardCenter.y
                cardCenter.y > rect.bottom -> cardCenter.y - rect.bottom
                else -> 0f
            }
            val distance = dx * dx + dy * dy
            if (distance < bestDistance) {
                best = cell
                bestDistance = distance
            }
        }
        return best
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
