package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test

/**
 * Local-only certification batch for round lifecycle and match determinism.
 * These exercise known engine invariants, not unrecorded Destiny: Rising rules.
 */
class ShadowRoundLifecycleCertificationTest {
    private fun card(index: Int) = ShadowData.Card().apply {
        name = "Ordinary $index"
        rarity = "Rare"
        faction = "Humans"
        cost = "1"
        power = "2"
        effect = "No special effects."
    }

    private fun catalog(): LinkedHashMap<String, ShadowData.Card> =
        linkedMapOf<String, ShadowData.Card>().apply {
            for (index in 0 until ShadowRules.DECK_SIZE)
                put("Ordinary $index", card(index))
        }

    private fun deck(): LinkedHashMap<String, Int> =
        linkedMapOf<String, Int>().apply {
            for (index in 0 until ShadowRules.DECK_SIZE) put("Ordinary $index", 1)
        }

    @Test fun restartingSameSeedResetsBattleAndReproducesOpeningHands() {
        val cards = catalog()
        val deck = deck()
        val e = ShadowEngine(cards, 114L)
        e.start(deck, deck)
        val openingHuman = e.human.hand.map { it.name }
        val openingCpu = e.cpu.hand.map { it.name }
        val first = e.humanFirst
        val firstDrawPile = e.human.drawPile.map { it.name }

        e.human.board[0][0] = ShadowEngine.Unit(e.human, cards.getValue("Ordinary 0"), 0, 0)
        e.human.points = 0
        e.unresolvedEffects = 5
        e.rejectedCpuMoves = 2
        e.placementRevision = 7
        e.finished = true

        e.start(deck, deck)

        assertEquals(first, e.humanFirst)
        assertEquals(openingHuman, e.human.hand.map { it.name })
        assertEquals(openingCpu, e.cpu.hand.map { it.name })
        assertEquals(firstDrawPile, e.human.drawPile.map { it.name })
        assertNull(e.human.board[0][0])
        assertEquals(1, e.round)
        assertEquals(0, e.completedTurns)
        assertFalse(e.finished)
        assertEquals(0, e.unresolvedEffects)
        assertEquals(0, e.rejectedCpuMoves)
        assertEquals(0, e.placementRevision)
        assertEquals(1, e.human.points)
        assertEquals(1, e.cpu.points)
    }

    @Test fun roundLightResetsAndDrawsOneCardPerSideUntilDeckExhausts() {
        val e = ShadowEngine(catalog(), 19L)
        val deck = deck()
        e.start(deck, deck)
        val humanBefore = e.human.hand.size
        val cpuBefore = e.cpu.hand.size
        e.human.points = 0
        e.cpu.points = 0

        e.endTurn()
        assertEquals(1, e.round)
        assertEquals(1, e.completedTurns)
        e.endTurn()
        assertEquals(2, e.round)
        assertEquals(0, e.completedTurns)
        assertEquals(2, e.human.points)
        assertEquals(2, e.cpu.points)
        assertEquals(humanBefore + 1, e.human.hand.size)
        assertEquals(cpuBefore + 1, e.cpu.hand.size)

        e.human.points = 1
        e.cpu.points = 0
        e.endTurn()
        e.endTurn()
        assertEquals(3, e.round)
        assertEquals(3, e.human.points)
        assertEquals(3, e.cpu.points)
    }

    @Test fun emptyBoardSixRoundMatchEndsAfterExactlyTwelveTurns() {
        val e = ShadowEngine(catalog(), 23L)
        val deck = deck()
        e.start(deck, deck)
        val expectedFirst = e.humanFirst
        for (turn in 1..(ShadowRules.ROUNDS * 2)) {
            assertFalse("Early finish at turn $turn", e.finished)
            e.endTurn()
        }
        assertTrue(e.finished)
        assertEquals(ShadowRules.ROUNDS, e.round)
        assertEquals(2, e.completedTurns)
        assertEquals("DRAW", e.result())
        assertEquals(0, e.unresolvedEffects)
        assertEquals(0, e.rejectedCpuMoves)
        assertEquals("DRAW", ShadowRules.result(e.score(e.human), e.score(e.cpu)))

        // No postgame extra round or turn may be scheduled.
        e.endTurn()
        assertEquals(ShadowRules.ROUNDS, e.round)
        assertEquals(2, e.completedTurns)
        assertEquals(expectedFirst, e.humanFirst)
    }

    @Test fun finalResultUsesCurrentSurvivingBattlefieldPower() {
        val catalog = catalog()
        val e = ShadowEngine(catalog, 29L)
        val deck = deck()
        e.start(deck, deck)
        val ally = ShadowEngine.Unit(e.human, catalog.getValue("Ordinary 0"), 0, 0)
        val enemy = ShadowEngine.Unit(e.cpu, catalog.getValue("Ordinary 1"), 1, 0)
        e.human.board[0][0] = ally
        e.cpu.board[1][0] = enemy
        ally.power = 7
        enemy.power = 3
        repeat(ShadowRules.ROUNDS * 2) { e.endTurn() }
        assertEquals(7, e.score(e.human))
        assertEquals(3, e.score(e.cpu))
        assertEquals("VICTORY", e.result())
        assertTrue(e.finished)
    }
}
