package com.tplatt.shadowshaper

/** Kotlin geometry used by Java ShadowEngine and future Kotlin effects.
 * Current contiguous interpretation is provisional pending in-game proof.
 */
object ShadowPositionRules {
    /** A printed Line ability, not merely card text referring to Line units. */
    @JvmStatic
    fun hasLineAffix(effect: String): Boolean =
        Regex("""\\b\\d+[- ]Unit Line\\b""", RegexOption.IGNORE_CASE).containsMatchIn(effect)

    @JvmStatic
    fun isAdjacent(rowA: Int, columnA: Int, rowB: Int, columnB: Int): Boolean =
        rowA == rowB && kotlin.math.abs(columnA - columnB) == 1

    @JvmStatic
    fun isSameColumn(rowA: Int, columnA: Int, rowB: Int, columnB: Int): Boolean =
        columnA == columnB && rowA != rowB

    @JvmStatic
    fun contiguousFactionLine(
        side: ShadowEngine.Player, faction: String, row: Int, column: Int, hypothetical: Boolean
    ): Int {
        if (!ShadowRules.isOnBoard(row, column)) return 0
        if (hypothetical && side.board[row][column] != null) return 0
        if (!hypothetical && side.board[row][column] == null) return 0
        var count = 1
        for (c in column - 1 downTo 0) {
            if (!side.board[row][c]?.card?.faction.equals(faction, ignoreCase = true)) break
            count++
        }
        for (c in column + 1 until ShadowRules.COLUMNS) {
            if (!side.board[row][c]?.card?.faction.equals(faction, ignoreCase = true)) break
            count++
        }
        return count
    }
}
