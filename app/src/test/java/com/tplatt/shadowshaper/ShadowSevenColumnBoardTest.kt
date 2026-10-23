package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test

/** Original-game seven-slot board contract, both UI and gameplay engine. */
class ShadowSevenColumnBoardTest {
    private fun card() = ShadowData.Card().apply {
        name = "Board Seven Test"
        faction = "Humans"
        rarity = "Rare"
        cost = "1"
        power = "3"
        effect = "No special effects."
    }

    @Test fun allSevenSlotsExistForBothPlayersAndBothRows() {
        val e = ShadowEngine(mapOf("Board Seven Test" to card()), 70L)
        assertEquals(7, ShadowRules.COLUMNS)
        for (owner in listOf(e.human, e.cpu)) {
            assertEquals(ShadowRules.ROWS, owner.board.size)
            for (row in 0 until ShadowRules.ROWS) {
                assertEquals(7, owner.board[row].size)
                for (col in 0..6) assertTrue(ShadowRules.isOnBoard(row, col))
                assertFalse(ShadowRules.isOnBoard(row, 7))
            }
        }
    }

    @Test fun seventhPositionIsLegallyPlayableWithoutWrappingOrOverwriting() {
        val c = card()
        val e = ShadowEngine(mapOf(c.name to c), 71L)
        e.humanTurn = true
        e.humanFirst = true
        e.human.points = 2
        e.human.hand.add(c)
        assertTrue(e.isPlayablePlacement(e.human, 0, ShadowRules.FRONT_ROW, 6))
        assertTrue(e.play(e.human, 0, ShadowRules.FRONT_ROW, 6, null))
        assertEquals(c.name, e.human.board[ShadowRules.FRONT_ROW][6]?.card?.name)
        for (col in 0 until 6) assertNull(e.human.board[ShadowRules.FRONT_ROW][col])
        assertEquals(1, e.human.points)
        assertFalse(ShadowRules.isOnBoard(ShadowRules.FRONT_ROW, 7))
    }
}
