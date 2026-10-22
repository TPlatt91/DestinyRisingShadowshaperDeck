package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test

/** AI must obey the same targeting rules as a human player. */
class ShadowCpuLegalPlacementTest {
    private fun card(name: String, effect: String = "No special effects.") =
        ShadowData.Card().apply {
            this.name = name
            this.faction = "Humans"
            this.rarity = "Rare"
            this.cost = "1"
            this.power = "4"
            this.effect = effect
        }

    @Test fun cpuDoesNotWasteTurnAttemptingAnIllegalTargetedPlacement() {
        val cards = listOf(
            card("Gladiator", "When Played: Deals damage to a front-row opposing unit."),
            card("Dreg")
        ).associateBy { it.name }
        val e = ShadowEngine(cards, 9L)
        e.humanTurn = false
        e.cpu.points = 1
        e.cpu.hand.add(cards.getValue("Gladiator"))
        e.cpu.hand.add(cards.getValue("Dreg"))
        val human = ShadowEngine.Unit(e.human, cards.getValue("Dreg"), 0, 0)
        e.human.board[0][0] = human
        assertTrue(e.cpuPlayOne())
        assertEquals(1, e.cpu.board.flatMap { it.toList() }.filterNotNull().size)
        assertEquals("Dreg", e.cpu.board.flatMap { it.toList() }.filterNotNull().single().card.name)
        assertEquals(1, e.cpu.hand.size)
        assertEquals("Gladiator", e.cpu.hand.single().name)
    }

    @Test fun cpuUsesLegalTargetWhenAvailable() {
        val cards = listOf(
            card("Gladiator", "When Played: Deals damage to a front-row opposing unit."),
            card("Dreg")
        ).associateBy { it.name }
        val e = ShadowEngine(cards, 11L)
        e.humanTurn = false
        e.cpu.points = 1
        e.cpu.hand.add(cards.getValue("Gladiator"))
        val human = ShadowEngine.Unit(e.human, cards.getValue("Dreg"), 1, 0)
        e.human.board[1][0] = human
        assertTrue(e.cpuPlayOne())
        assertTrue(e.cpu.board.flatMap { it.toList() }.filterNotNull().any { it.card.name == "Gladiator" })
        assertTrue(e.cpu.hand.isEmpty())
    }
}
