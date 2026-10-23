package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test

/**
 * Full-board movement lifecycle regressions. These check that valid game
 * actions cannot become stuck behind an impossible relocation prompt.
 *
 * The original client's behavior when a target has no free destination
 * remains unverified in recorded footage. These tests cover simulator safety,
 * not claimed original-client equivalence.
 */
class ShadowFullBoardRelocationSafetyTest {
    private fun card(name: String) = ShadowData.Card().apply {
        this.name = name
        faction = "Humans"
        rarity = "Rare"
        cost = "1"
        power = "10"
        effect = "No special effects."
    }

    private fun engine() = ShadowEngine(mapOf("Unit" to card("Unit")), 19L).apply {
        humanFirst = true
        humanTurn = true
        human.points = 1
        cpu.points = 1
    }

    private fun put(e: ShadowEngine, row: Int, col: Int): ShadowEngine.Unit =
        ShadowEngine.Unit(e.cpu, e.catalog.getValue("Unit"), row, col).also {
            e.cpu.board[row][col] = it
        }

    @Test fun fullBoardCancelsPendingRelocationAndAllowsTurnToFinish() {
        val e = engine()
        for (row in 0 until ShadowRules.ROWS)
            for (col in 0 until ShadowRules.COLUMNS) put(e, row, col)
        val moving = e.cpu.board[0][0]!!
        e.pendingMovedUnit = moving
        e.pendingMoveCaster = e.human

        e.endTurn()

        assertNull(e.pendingMovedUnit)
        assertNull(e.pendingMoveCaster)
        assertFalse(e.humanTurn)
        assertSame(moving, e.cpu.board[0][0])
        assertTrue(e.journal.any { it.contains("RELOCATION CANCELLED") })
    }

    @Test fun defeatedTargetCancelsPendingRelocationWithoutBlockingTurn() {
        val e = engine()
        val moving = put(e, 0, 0)
        e.pendingMovedUnit = moving
        e.pendingMoveCaster = e.human
        e.cpu.board[0][0] = null

        e.endTurn()

        assertNull(e.pendingMovedUnit)
        assertFalse(e.humanTurn)
        assertNull(e.cpu.board[0][0])
    }

    @Test fun invalidDestinationKeepsSelectionAndDoesNotMutateBoard() {
        val e = engine()
        val moving = put(e, 0, 0)
        val occupied = put(e, 1, 1)
        e.pendingMovedUnit = moving
        e.pendingMoveCaster = e.human
        val priorRevision = e.placementRevision

        assertFalse(e.resolveMove(e.human, 1, 1))
        assertFalse(e.resolveMove(e.human, 2, 0))
        assertSame(moving, e.pendingMovedUnit)
        assertSame(moving, e.cpu.board[0][0])
        assertSame(occupied, e.cpu.board[1][1])
        assertEquals(priorRevision, e.placementRevision)
        assertTrue(e.resolveMove(e.human, 1, 2))
        assertNull(e.cpu.board[0][0])
        assertSame(moving, e.cpu.board[1][2])
        assertNull(e.pendingMovedUnit)
        assertEquals(priorRevision + 1, e.placementRevision)
    }
}
