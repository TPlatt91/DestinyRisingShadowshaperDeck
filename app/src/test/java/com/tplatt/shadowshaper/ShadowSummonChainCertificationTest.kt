package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test

/**
 * Video-derived token summoning and arrival interaction regressions.
 * No TodayInDestiny metadata is used as a rule source.
 */
class ShadowSummonChainCertificationTest {
    private fun card(name: String, faction: String, power: Int, effect: String) =
        ShadowData.Card().apply {
            this.name = name
            this.faction = faction
            this.power = power.toString()
            this.cost = "1"
            this.rarity = "Rare"
            this.effect = effect
        }

    private fun game(): ShadowEngine {
        val cards = listOf(
            card("Common Cover", "Humans", 6, "When Played (Front Row): Summons 2 Rocks on opposing Front Row."),
            card("Ning Fei", "Humans", 5, "When Played: Summons a Clone on opposing Front Row."),
            card("Rock", "Neutral", 1, "Summoned unit."),
            card("Clone", "Humans", 1, "When destroyed: Deals 3 damage to units on left and right."),
            card("Zhiyao", "Humans", 5, "Sustained: When an opposing unit enters the Front Row, gains +1 Power."),
            card("Blocker", "Fallen", 10, "No special effects.")
        )
        return ShadowEngine(cards.associateBy { it.name }, 41L).apply {
            humanFirst = true
            humanTurn = true
            human.points = 30
        }
    }

    private fun put(e: ShadowEngine, side: ShadowEngine.Player, name: String, col: Int) =
        ShadowEngine.Unit(side, e.catalog.getValue(name), 1, col).also {
            side.board[1][col] = it
        }

    private fun play(e: ShadowEngine, name: String, col: Int) {
        e.human.hand.add(e.catalog.getValue(name))
        assertTrue(e.lastActionError, e.play(e.human, e.human.hand.lastIndex, 1, col, null))
    }

    @Test fun commonCoverUsesOnlyOneRemainingEnemyFrontSlot() {
        val e = game()
        val free = 3
        for (col in 0 until ShadowRules.COLUMNS)
            if (col != free) put(e, e.cpu, "Blocker", col)
        val initialRevision = e.placementRevision
        play(e, "Common Cover", 0)
        assertEquals("Rock", e.cpu.board[1][free]?.card?.name)
        assertEquals(2, e.placementRevision - initialRevision)
        assertEquals(1, (0 until ShadowRules.COLUMNS).count {
            e.cpu.board[1][it]?.card?.name == "Rock"
        })
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun fullEnemyFrontRowBlocksNingFeiCloneWithoutOverwriting() {
        val e = game()
        val originals = (0 until ShadowRules.COLUMNS).map {
            put(e, e.cpu, "Blocker", it)
        }
        val startRevision = e.placementRevision
        play(e, "Ning Fei", 0)
        for (col in 0 until ShadowRules.COLUMNS)
            assertSame(originals[col], e.cpu.board[1][col])
        assertEquals(startRevision + 1, e.placementRevision)
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun eachRockArrivalTriggersOpposingZhiyaoSeparately() {
        val e = game()
        val zhiyao = ShadowEngine.Unit(e.human, e.catalog.getValue("Zhiyao"), 0, 5)
        e.human.board[0][5] = zhiyao
        play(e, "Common Cover", 0)
        assertEquals(2, (0 until ShadowRules.COLUMNS).count {
            e.cpu.board[1][it]?.card?.name == "Rock"
        })
        assertEquals(2, zhiyao.bonus)
    }
    @Test fun ningFeiCloneDestructionDamagesHostRowNeighborsOnly() {
        val e = game()
        // Leave one enemy Front Row opening so Ning Fei's summoned Clone has
        // an unambiguous position between two surviving enemy units.
        val left = put(e, e.cpu, "Blocker", 0)
        val right = put(e, e.cpu, "Blocker", 2)
        for (col in 3 until ShadowRules.COLUMNS) put(e, e.cpu, "Blocker", col)
        val otherRow = ShadowEngine.Unit(e.cpu, e.catalog.getValue("Blocker"), 0, 1)
        e.cpu.board[0][1] = otherRow
        val zhiyao = ShadowEngine.Unit(e.human, e.catalog.getValue("Zhiyao"), 0, 5)
        e.human.board[0][5] = zhiyao

        play(e, "Ning Fei", 0)
        val clone = e.cpu.board[1][1]!!
        assertEquals("Clone", clone.card.name)
        assertEquals(1, zhiyao.bonus) // Enemy Front Row arrival happened once.

        val attacker = put(e, e.human, "Blocker", 4)
        e.dealDirect(attacker, clone, 1)
        e.sweep()

        assertNull(e.cpu.board[1][1])
        assertEquals(7, left.power)
        assertEquals(7, right.power)
        assertEquals(10, otherRow.power) // Left/right does not cross rows.
        assertEquals(1, zhiyao.bonus) // A destruction is not a new placement.
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun twoSimultaneousCloneSplashesConsumeShieldThenDamageSharedNeighbor() {
        val e = game()
        val first = put(e, e.cpu, "Clone", 1)
        val second = put(e, e.cpu, "Clone", 3)
        val middle = put(e, e.cpu, "Blocker", 2)
        middle.overshield = true
        val attacker = put(e, e.human, "Blocker", 4)

        // Both Clone deaths belong to the same sweep; the first splash
        // spends Overshield and the second is an independent damaging event.
        e.dealDirect(attacker, first, 1)
        e.dealDirect(attacker, second, 1)
        e.sweep()

        assertNull(e.cpu.board[1][1])
        assertNull(e.cpu.board[1][3])
        assertFalse(middle.overshield)
        assertEquals(7, middle.power)
        assertEquals(0, e.unresolvedEffects)
    }

}
