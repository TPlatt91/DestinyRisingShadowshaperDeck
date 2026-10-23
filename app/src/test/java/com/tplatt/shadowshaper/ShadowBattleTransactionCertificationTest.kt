package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test

/**
 * Transaction and terminal-state safety for complete six-round battles.
 * These cover simulator state invariants, not original-client timing guesses.
 * Execute locally with run_local_engine_checks.bat, not GitHub Actions.
 */
class ShadowBattleTransactionCertificationTest {
    private fun card(index: Int) = ShadowData.Card().apply {
        name = "Ordinary $index"
        rarity = "Rare"
        faction = "Humans"
        cost = "1"
        power = "5"
        effect = "No special effects."
    }

    private fun catalog() = (0 until ShadowRules.DECK_SIZE)
        .associate { "Ordinary $it" to card(it) }

    private fun deck() = (0 until ShadowRules.DECK_SIZE)
        .associate { "Ordinary $it" to 1 }

    private fun match() = ShadowEngine(catalog(), 42L).apply {
        start(deck(), deck())
        humanFirst = true
        humanTurn = true
        human.points = 5
    }

    private fun pendingEnemy(e: ShadowEngine): ShadowEngine.Unit =
        ShadowEngine.Unit(e.cpu, e.catalog.getValue("Ordinary 0"), 0, 0).also {
            e.cpu.board[0][0] = it
            e.pendingMovedUnit = it
            e.pendingMoveCaster = e.human
        }

    @Test fun pendingRelocationRejectsNewPlayWithoutChargingLightOrHand() {
        val e = match()
        val moving = pendingEnemy(e)
        e.human.hand.clear()
        e.human.hand.add(e.catalog.getValue("Ordinary 1"))
        val originalLight = e.human.points
        val originalRevision = e.placementRevision

        assertFalse(e.play(e.human, 0, 1, 1, null))
        assertTrue(e.lastActionError.contains("Choose a destination"))
        assertEquals(originalLight, e.human.points)
        assertEquals(1, e.human.hand.size)
        assertNull(e.human.board[1][1])
        assertSame(moving, e.pendingMovedUnit)
        assertEquals(originalRevision, e.placementRevision)

        assertTrue(e.resolveMove(e.human, 1, 2))
        assertTrue(e.play(e.human, 0, 1, 1, null))
        assertEquals(originalLight - 1, e.human.points)
        assertEquals(0, e.human.hand.size)
        assertSame(moving, e.cpu.board[1][2])
    }

    @Test fun pendingRelocationBlocksEndTurnAndWrongCasterCannotMoveTarget() {
        val e = match()
        val moving = pendingEnemy(e)
        val beforeRound = e.round
        val beforeTurns = e.completedTurns
        val beforeRevision = e.placementRevision

        assertFalse(e.resolveMove(e.cpu, 1, 2))
        assertSame(moving, e.pendingMovedUnit)
        assertEquals(beforeRevision, e.placementRevision)
        e.endTurn()
        assertEquals(beforeRound, e.round)
        assertEquals(beforeTurns, e.completedTurns)
        assertTrue(e.humanTurn)
        assertSame(moving, e.cpu.board[0][0])

        assertTrue(e.resolveMove(e.human, 1, 2))
        e.endTurn()
        assertEquals(beforeTurns + 1, e.completedTurns)
        assertFalse(e.humanTurn)
        assertSame(moving, e.cpu.board[1][2])
    }

    @Test fun finishedMatchCannotSpendLightMoveOrAdvanceRounds() {
        val e = match()
        e.human.hand.clear()
        e.human.hand.add(e.catalog.getValue("Ordinary 1"))
        val survivor = ShadowEngine.Unit(
            e.human, e.catalog.getValue("Ordinary 2"), 0, 0
        )
        e.human.board[0][0] = survivor
        e.finished = true
        e.round = ShadowRules.ROUNDS
        e.completedTurns = 2
        val light = e.human.points
        val score = e.score(e.human)
        val hand = e.human.hand.size
        val revisions = e.placementRevision

        assertFalse(e.play(e.human, 0, 1, 0, null))
        assertEquals("The match has finished.", e.lastActionError)
        assertFalse(e.cpuPlayOne())
        e.cpuTurn()
        e.endTurn()
        assertEquals(light, e.human.points)
        assertEquals(hand, e.human.hand.size)
        assertSame(survivor, e.human.board[0][0])
        assertEquals(score, e.score(e.human))
        assertEquals(revisions, e.placementRevision)
        assertEquals(ShadowRules.ROUNDS, e.round)
        assertEquals(2, e.completedTurns)
    }

    @Test fun exhaustedDrawPilesNeverCreateCardsOrDeadlockRoundTransition() {
        val e = match()
        e.human.drawPile.clear()
        e.cpu.drawPile.clear()
        val humanCards = e.human.hand.map { it.name }
        val cpuCards = e.cpu.hand.map { it.name }

        e.endTurn()
        e.endTurn()

        assertEquals(2, e.round)
        assertTrue(e.human.drawPile.isEmpty())
        assertTrue(e.cpu.drawPile.isEmpty())
        assertEquals(humanCards, e.human.hand.map { it.name })
        assertEquals(cpuCards, e.cpu.hand.map { it.name })
        assertEquals(2, e.human.points)
        assertEquals(2, e.cpu.points)
        assertEquals(0, e.unresolvedEffects)
    }
}
