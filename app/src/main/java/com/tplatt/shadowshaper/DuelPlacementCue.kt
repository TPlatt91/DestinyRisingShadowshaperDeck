package com.tplatt.shadowshaper

/**
 * Pure identification of the single, most recent card placement.
 * Compose recomposition or turn changes must not animate every existing unit.
 */
internal object DuelPlacementCue {
    fun isNewlyPlaced(
        placementRevision: Int,
        lastSide: Int,
        lastRow: Int,
        lastColumn: Int,
        side: Int,
        row: Int,
        column: Int
    ): Boolean =
        placementRevision > 0 &&
        lastSide == side &&
        lastRow == row &&
        lastColumn == column
}
