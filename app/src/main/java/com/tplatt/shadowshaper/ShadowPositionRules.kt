package com.tplatt.shadowshaper

/** Kotlin geometry used by Java ShadowEngine and future Kotlin effects.
 * Current contiguous interpretation is provisional pending in-game proof.
 */
object ShadowPositionRules {
    /**
     * Only a printed ability header grants the Line keyword. Mentions such
     * as "If a 3-Unit Line exists, gain Power" do not themselves make that
     * unit a Line card (or eligible for Drop Pod's random summon pool).
     * Some recorded cards print "2 Unit Line" without the hyphen.
     */
    @JvmStatic
    fun hasLineAffix(effect: String): Boolean =
        Regex(
            """\b(?:When Played|Sustained)\s*\([^)]*\b\d+[- ]Unit Line\b[^)]*\)""",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(effect)

    /**
     * A Line consists of contiguous units bearing the PRINTED Line affix.
     * Other Cabal cards, even when adjacent, interrupt it. A hypothetical
     * placement supplies its printed card at an empty cell.
     *
     * Source: original Line keyword tooltip and player-reported Centurion
     * combinations. Does not infer Line eligibility merely from faction.
     */
    @JvmStatic
    fun contiguousLineUnits(
        side: ShadowEngine.Player, row: Int, column: Int,
        hypothetical: ShadowData.Card? = null
    ): Int {
        if (!ShadowRules.isOnBoard(row, column)) return 0
        if (hypothetical != null && side.board[row][column] != null) return 0
        val center = hypothetical ?: side.board[row][column]?.card ?: return 0
        if (!hasLineAffix(center.effect)) return 0
        var length = 1
        for (c in column - 1 downTo 0) {
            if (!hasLineAffix(side.board[row][c]?.card?.effect ?: "")) break
            length++
        }
        for (c in column + 1 until ShadowRules.COLUMNS) {
            if (!hasLineAffix(side.board[row][c]?.card?.effect ?: "")) break
            length++
        }
        return length
    }

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
