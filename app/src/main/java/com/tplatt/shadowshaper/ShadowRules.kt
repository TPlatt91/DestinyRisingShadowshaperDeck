package com.tplatt.shadowshaper

/** Canonical shared Shadowshaper match constants and rule helpers. */
object ShadowRules {
    const val DECK_SIZE = 15
    const val COPY_LIMIT = 2
    const val EXOTIC_LIMIT = 3
    const val MYTHIC_LIMIT = 5
    const val ROUNDS = 6
    const val ROWS = 2
    const val COLUMNS = 6  // Board width remains provisional until direct client verification.
    const val OFFENSIVE_OPENING_HAND = 5
    const val DEFENSIVE_OPENING_HAND = 4
    const val NEW_ROUND_DRAW = 1  // Later-round draw timing remains provisional.
    const val FRONT_ROW = 1
    const val BACK_ROW = 0

    @JvmStatic
    fun lightForRound(round: Int): Int {
        require(round in 1..ROUNDS) { "Round must be in range 1..6" }
        return round
    }

    @JvmStatic
    fun openingHand(offensive: Boolean): Int =
        if (offensive) OFFENSIVE_OPENING_HAND else DEFENSIVE_OPENING_HAND

    @JvmStatic
    fun isOnBoard(row: Int, column: Int): Boolean =
        row in 0 until ROWS && column in 0 until COLUMNS

    @JvmStatic
    fun isLastRound(round: Int): Boolean = round == ROUNDS

    @JvmStatic
    fun result(playerPower: Int, cpuPower: Int): String = when {
        playerPower > cpuPower -> "VICTORY"
        playerPower < cpuPower -> "DEFEAT"
        else -> "DRAW"
    }
}
