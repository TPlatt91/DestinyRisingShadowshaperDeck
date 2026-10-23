package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test

/**
 * Additional deterministic end-to-end summon/death checks. Printed abilities
 * follow the user's recorded original-game footage, never older web text.
 */
class ShadowSummonAndDestructionBatchTest {
    private fun card(name: String, faction: String, power: Int, effect: String) =
        ShadowData.Card().apply {
            this.name = name
            this.faction = faction
            this.power = power.toString()
            cost = "1"
            rarity = "Rare"
            this.effect = effect
        }

    private fun game(seed: Long = 33L): ShadowEngine {
        val cards = listOf(
            card("Common Cover", "Humans", 5,
                "When Played (Front Row): Summons 2 Rocks on opposing Front Row."),
            card("Ning Fei", "Humans", 5,
                "When Played: Summons a Clone onto the opponent's Front Row."),
            card("Rock", "Neutral", 1, "Summoned unit."),
            card("Clone", "Humans", 1,
                "When defeated: deals 3 damage to the units on its left and right."),
            card("Zhiyao", "Humans", 5,
                "Sustained: On an opposing Front Row arrival, gain +1 Power."),
            card("Blocker", "Fallen", 10, "No special effects."),
            card("Hit Source", "Humans", 3, "No special effects.")
        ).associateBy { it.name }
        return ShadowEngine(cards, seed).apply {
            humanFirst = true
            humanTurn = true
            human.points = 40
        }
    }

    private fun put(
        e: ShadowEngine, side: ShadowEngine.Player,
        name: String, row: Int, col: Int
    ): ShadowEngine.Unit =
        ShadowEngine.Unit(side, e.catalog.getValue(name), row, col).also {
            side.board[row][col] = it
        }

    private fun play(e: ShadowEngine, name: String, col: Int = 0) {
        e.human.hand.add(e.catalog.getValue(name))
        assertTrue(e.lastActionError,
            e.play(e.human, e.human.hand.lastIndex, ShadowRules.FRONT_ROW, col, null))
    }

    @Test fun commonCoverFullOpponentFrontRowDoesNotOverwriteOrInventSummons() {
        val e = game()
        val originals = (0 until ShadowRules.COLUMNS).map {
            put(e, e.cpu, "Blocker", ShadowRules.FRONT_ROW, it)
        }
        val before = e.placementRevision
        play(e, "Common Cover")
        assertEquals(before + 1, e.placementRevision)
        for (col in originals.indices)
            assertSame(originals[col], e.cpu.board[ShadowRules.FRONT_ROW][col])
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun suppressedZhiyaoIgnoresBothCommonCoverRockArrivals() {
        val e = game()
        val watcher = put(e, e.human, "Zhiyao", ShadowRules.BACK_ROW, 5)
        watcher.suppressed = true
        play(e, "Common Cover")
        assertEquals(2, (0 until ShadowRules.COLUMNS).count {
            e.cpu.board[ShadowRules.FRONT_ROW][it]?.card?.name == "Rock"
        })
        assertEquals(0, watcher.bonus)
        assertTrue(watcher.timedBonuses.isEmpty())
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun cloneChainFromOneDeathResolvesSecondDeathInNextWave() {
        val e = game()
        val left = put(e, e.cpu, "Blocker", 1, 0)
        val first = put(e, e.cpu, "Clone", 1, 1)
        put(e, e.cpu, "Clone", 1, 2)
        val right = put(e, e.cpu, "Blocker", 1, 3)
        val source = put(e, e.human, "Hit Source", 0, 4)

        e.dealDirect(source, first, 1)
        e.sweep()

        assertNull(e.cpu.board[1][1])
        assertNull(e.cpu.board[1][2])
        assertEquals(7, left.power)
        assertEquals(7, right.power)
        assertEquals(2, e.journal.count { it.startsWith("DESTROYED ▶ Clone") })
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun cloneDestroyedAtFieldEdgeSplashesOnlySameRowNeighbor() {
        val e = game()
        val clone = put(e, e.cpu, "Clone", 1, 0)
        val right = put(e, e.cpu, "Blocker", 1, 1)
        val above = put(e, e.cpu, "Blocker", 0, 0)
        val attacker = put(e, e.human, "Hit Source", 0, 3)
        e.dealDirect(attacker, clone, 1)
        e.sweep()
        assertNull(e.cpu.board[1][0])
        assertEquals(7, right.power)
        assertEquals(10, above.power)
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun summonOnlyRockCannotBePlayedDirectlyFromHand() {
        val e = game()
        val rock = e.catalog.getValue("Rock")
        assertFalse(rock.isPlayable)
        e.human.hand.add(rock)
        val before = e.human.points
        assertFalse(e.play(e.human, e.human.hand.lastIndex, 1, 1, null))
        assertEquals(before, e.human.points)
        assertNull(e.human.board[1][1])
        assertEquals(1, e.human.hand.size)
    }
}
