package com.tplatt.shadowshaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behavior boundaries visible on the nine cards in the user's Batch 05 recording.
 * JVM unit tests: intentionally not wired to GitHub Actions.
 */
class ShadowBatch05EffectsTest {
    private fun card(
        name: String, faction: String, rarity: String, power: Int, cost: Int,
        effect: String
    ): ShadowData.Card = ShadowData.Card().apply {
        this.name = name
        this.faction = faction
        this.rarity = rarity
        this.power = power.toString()
        this.cost = cost.toString()
        this.effect = effect
    }

    private fun engine(): ShadowEngine {
        val cards = listOf(
            card("Hexguard", "Hive", "Legendary", 1, 1,
                "When Played: Gains the Inspired effect."),
            card("Cursed Thrall", "Hive", "Legendary", 1, 1,
                "When Played: Randomly moves to the opponent's front row, then dies at the beginning of your next turn, dealing 2 damage each to units on the left and right."),
            card("Thrall", "Hive", "Rare", 2, 1, "No special effects."),
            card("Legionary", "Cabal", "Rare", 2, 1,
                "Sustained (Column): Gains +1 Power."),
            card("Tan-2", "Humans", "Exotic", 2, 2,
                "Sustained: Doubles your next Power bonus."),
            card("Ning Fei", "Humans", "Exotic", 4, 2,
                "When Played: Randomly places a Clone in the opponent's front row."),
            card("Clone", "Humans", "Mythic", 1, 1,
                "Sustained: When Power reaches 0, deals 3 damage to units on the left and right.")
        )
        return ShadowEngine(cards.associateBy { it.name }, 17L).apply {
            humanTurn = true
            human.points = 10
        }
    }

    @Test fun hexguardInspiresItselfOnPlay() {
        val e = engine()
        e.human.hand.add(e.catalog.getValue("Hexguard"))
        assertTrue(e.play(e.human, 0, 0, 0, null))
        assertTrue(e.human.board[0][0]!!.inspired)
    }

    @Test fun legionaryGetsOnePowerWithColumnPartner() {
        val e = engine()
        val legionary = ShadowEngine.Unit(e.human, e.catalog.getValue("Legionary"), 0, 3)
        e.human.board[0][3] = legionary
        assertEquals(2, e.power(legionary))
        e.human.board[1][3] = ShadowEngine.Unit(e.human, e.catalog.getValue("Thrall"), 1, 3)
        assertEquals(3, e.power(legionary))
    }

    @Test fun tanTwoDoublesOnlyNextPowerBonus() {
        val e = engine()
        val tan = ShadowEngine.Unit(e.human, e.catalog.getValue("Tan-2"), 0, 0)
        val friend = ShadowEngine.Unit(e.human, e.catalog.getValue("Thrall"), 0, 1)
        e.human.board[0][0] = tan
        e.human.board[0][1] = friend
        e.buff(friend, 2)
        assertEquals(6, e.power(friend))
        assertTrue(tan.bonusDoublerConsumed)
        e.buff(friend, 1)
        assertEquals(7, e.power(friend))
    }

    @Test fun ningFeiSummonsCloneIntoEnemyFrontRow() {
        val e = engine()
        e.human.hand.add(e.catalog.getValue("Ning Fei"))
        assertTrue(e.play(e.human, 0, 0, 0, null))
        val summoned = e.cpu.board[1].filterNotNull().filter { it.card.name == "Clone" }
        assertEquals(1, summoned.size)
    }

    @Test fun cursedThrallMovesAcrossToOpposingFrontRow() {
        val e = engine()
        e.human.hand.add(e.catalog.getValue("Cursed Thrall"))
        assertTrue(e.play(e.human, 0, 0, 0, null))
        assertNull(e.human.board[0][0])
        val transferred = e.cpu.board[1].filterNotNull()
        assertEquals(1, transferred.size)
        assertEquals("Cursed Thrall", transferred[0].card.name)
        assertTrue(transferred[0].fuseOrigin === e.human)
    }

    @Test fun cloneAtZeroPowerDamagesItsNeighbors() {
        val e = engine()
        val clone = ShadowEngine.Unit(e.cpu, e.catalog.getValue("Clone"), 1, 2)
        val left = ShadowEngine.Unit(e.cpu, e.catalog.getValue("Thrall"), 1, 1)
        val right = ShadowEngine.Unit(e.cpu, e.catalog.getValue("Thrall"), 1, 3)
        e.cpu.board[1][1] = left
        e.cpu.board[1][2] = clone
        e.cpu.board[1][3] = right
        clone.power = 0
        e.sweep()
        assertNull(e.cpu.board[1][2])
        assertNull(e.cpu.board[1][1])
        assertNull(e.cpu.board[1][3])
        assertTrue(e.journal.any { it.contains("DESTROYED") && it.contains("Clone") })
    }
}
