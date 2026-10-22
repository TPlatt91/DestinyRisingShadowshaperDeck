package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test

/**
 * Shared event ordering: same-wave deaths, summoned-unit watchers,
 * and original-video target-centered splash damage.
 *
 * Run with run_local_engine_checks.bat; no GitHub Actions required.
 */
class ShadowSimultaneousEffectsTest {
    private fun card(name: String, faction: String, power: Int, cost: Int, effect: String) =
        ShadowData.Card().apply {
            this.name = name
            this.faction = faction
            this.power = power.toString()
            this.cost = cost.toString()
            this.rarity = "Rare"
            this.effect = effect
        }

    private fun engine(): ShadowEngine {
        val catalog = listOf(
            card("Clone", "Humans", 1, 1,
                "When defeated: deals 3 damage to the units on its left and right."),
            card("Dreg", "Fallen", 10, 1, "No special effects."),
            card("Fragile Dreg", "Fallen", 1, 1, "No special effects."),
            card("Unrelenting Captain", "Fallen", 10, 1,
                "Sustained: Newly summoned Dregs gain 1 Power."),
            card("Grenade Dreg", "Fallen", 3, 1,
                "Sustained: When an ally deploys in the same row, this unit gains +1 Power."),
            card("Elite Sniper Vandal", "Fallen", 10, 1,
                "Sustained (Back Row): Damages opposing units that gain Power."),
            card("Zhiyao", "Humans", 10, 1,
                "Sustained: Opponent Front Row placements grant Power for 3 turns."),
            card("Gwynn", "Humans", 6, 4,
                "When Played (Front Row): Deals 3 damage to the target and adjacent units; loses 2 Power."),
            card("Saladin", "Humans", 6, 4,
                "Sustained: When allied Humans take damage, they gain +3 Power."),
            card("Augmented Shank", "Fallen", 5, 5,
                "When Played (Front Row): Deals 2 damage to a unit and units on its left and right.")
        ).associateBy { it.name }
        return ShadowEngine(catalog, 24L).apply {
            humanFirst = true
            humanTurn = true
            human.points = 80
        }
    }

    private fun put(e: ShadowEngine, name: String, side: ShadowEngine.Player,
                    row: Int, col: Int): ShadowEngine.Unit {
        val unit = ShadowEngine.Unit(side, e.catalog.getValue(name), row, col)
        side.board[row][col] = unit
        return unit
    }

    private fun play(e: ShadowEngine, name: String, row: Int, col: Int,
                     target: ShadowEngine.Target): ShadowEngine.Unit {
        e.human.hand.add(e.catalog.getValue(name))
        assertTrue(e.lastActionError,
            e.play(e.human, e.human.hand.lastIndex, row, col, target))
        return e.human.board[row][col]!!
    }

    @Test fun simultaneouslyDefeatedClonesDoNotSplashOtherAlreadyDefeatedClones() {
        val e = engine()
        val attacker = put(e, "Dreg", e.human, 1, 5)
        val neighborLeft = put(e, "Dreg", e.cpu, 1, 0)
        val first = put(e, "Clone", e.cpu, 1, 1)
        val second = put(e, "Clone", e.cpu, 1, 2)
        val neighborRight = put(e, "Dreg", e.cpu, 1, 3)
        e.dealDirect(attacker, first, 2)
        e.dealDirect(attacker, second, 2)
        e.sweep()
        assertNull(e.cpu.board[1][1])
        assertNull(e.cpu.board[1][2])
        assertEquals(7, neighborLeft.power)
        assertEquals(7, neighborRight.power)
        assertFalse(e.journal.any { it.contains("DAMAGE 3 ▶ Clone") })
    }

    @Test fun deadSummonCannotTriggerEnemyPlacementWatcher() {
        val e = engine()
        put(e, "Unrelenting Captain", e.human, 0, 0)
        put(e, "Elite Sniper Vandal", e.cpu, 0, 0)
        put(e, "Elite Sniper Vandal", e.cpu, 0, 1)
        val zhiyao = put(e, "Zhiyao", e.cpu, 0, 2)
        // A fragile Dreg starts at 1 Power, gains 1 from Unrelenting
        // Captain, then receives one retaliatory hit from each sniper.
        e.catalog.getValue("Dreg").power = "1"

        e.spawnAt(e.human, ShadowRules.FRONT_ROW, 2, "Dreg")

        assertNull(e.human.board[ShadowRules.FRONT_ROW][2])
        assertEquals(10, zhiyao.power)
        assertEquals(0, zhiyao.bonus)
        assertTrue(e.journal.any { it.contains("DESTROYED ▶ Dreg") })
    }

    @Test fun deadPlayedCardCannotTriggerEnemyPlacementWatcher() {
        val e = engine()
        put(e, "Unrelenting Captain", e.human, 0, 0)
        put(e, "Elite Sniper Vandal", e.cpu, 0, 0)
        put(e, "Elite Sniper Vandal", e.cpu, 0, 1)
        val zhiyao = put(e, "Zhiyao", e.cpu, 0, 2)
        // Two sniper responses defeat a 1+1 Power Dreg during ally placement.
        e.catalog.getValue("Dreg").power = "1"
        e.human.hand.add(e.catalog.getValue("Dreg"))

        assertTrue(e.lastActionError,
            e.play(e.human, e.human.hand.lastIndex, ShadowRules.FRONT_ROW, 2, null))
        assertNull(e.human.board[ShadowRules.FRONT_ROW][2])
        assertEquals(10, zhiyao.power)
        assertEquals(0, zhiyao.bonus)
        assertTrue(e.journal.any { it.contains("DESTROYED ▶ Dreg") })
    }

    @Test fun augmentedShankSplashStaysOnSelectedEnemyRow() {
        val e = engine()
        val left = put(e, "Dreg", e.cpu, 1, 1)
        val center = put(e, "Dreg", e.cpu, 1, 2)
        val right = put(e, "Dreg", e.cpu, 1, 3)
        val otherRow = put(e, "Dreg", e.cpu, 0, 2)
        play(e, "Augmented Shank", 1, 4, ShadowEngine.Target(e.cpu, 1, 2))
        assertEquals(8, left.power)
        assertEquals(8, center.power)
        assertEquals(8, right.power)
        assertEquals(10, otherRow.power)
    }

    @Test fun lethalArrivalBuffStopsLaterAlliedGrenadeDregWatchers() {
        val e = engine()
        put(e, "Unrelenting Captain", e.human, 0, 3)
        val watcherA = put(e, "Grenade Dreg", e.human, 1, 0)
        val watcherB = put(e, "Grenade Dreg", e.human, 1, 1)
        put(e, "Elite Sniper Vandal", e.cpu, 0, 0)
        put(e, "Elite Sniper Vandal", e.cpu, 0, 1)
        e.catalog.getValue("Dreg").power = "1"

        e.spawnAt(e.human, 1, 2, "Dreg")

        assertNull(e.human.board[1][2])
        assertEquals(3, watcherA.power)
        assertEquals(3, watcherB.power)
        assertEquals(0, watcherA.bonus)
        assertEquals(0, watcherB.bonus)
        assertTrue(e.journal.any { it.contains("DESTROYED ▶ Dreg") })
    }

    @Test fun survivingArrivalStillBuffsEachMatchingGrenadeDregOnce() {
        val e = engine()
        val watcherA = put(e, "Grenade Dreg", e.human, 1, 0)
        val watcherB = put(e, "Grenade Dreg", e.human, 1, 1)

        e.spawnAt(e.human, 1, 2, "Dreg")

        assertNotNull(e.human.board[1][2])
        assertEquals(4, watcherA.power)
        assertEquals(4, watcherB.power)
        assertEquals(1, watcherA.bonus)
        assertEquals(1, watcherB.bonus)
        assertEquals(2, e.journal.count {
            it.contains("Grenade Dreg +1 Power: Dreg deployed")
        })
    }

    @Test fun gwynnSelfPowerPenaltyBypassesOvershieldWithoutConsumingIt() {
        val e = engine()
        put(e, "Saladin", e.human, 0, 0)
        val gwynn = put(e, "Gwynn", e.human, 1, 4)
        gwynn.overshield = true
        val opponent = put(e, "Dreg", e.cpu, 1, 2)

        e.effects.onPlayed(gwynn, ShadowEngine.Target(e.cpu, 1, 2))

        assertEquals(7, opponent.power)
        assertEquals(4, gwynn.power)
        assertEquals(0, gwynn.bonus)
        assertTrue("Losing Power is not an attack on Overshield", gwynn.overshield)
        assertFalse(e.journal.any { it == "DAMAGE 2 ▶ Gwynn" })
        assertFalse(e.journal.any { it == "BUFF +3 ▶ Gwynn" })
    }

    @Test fun gwynnSelfPowerPenaltyDoesNotActivateSaladin() {
        val e = engine()
        put(e, "Saladin", e.human, 0, 0)
        val gwynn = put(e, "Gwynn", e.human, 1, 4)
        val opponent = put(e, "Dreg", e.cpu, 1, 2)

        e.effects.onPlayed(gwynn, ShadowEngine.Target(e.cpu, 1, 2))
        e.sweep()

        assertEquals(7, opponent.power)
        assertEquals(4, gwynn.power)
        assertEquals(0, gwynn.bonus)
        assertTrue(e.journal.any { it.contains("Gwynn: -2 Power") })
        assertEquals(0, e.journal.count { it.contains("DAMAGE") && it.contains("Gwynn") })
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun gwynnSplashStaysOnSelectedEnemyRowAndAppliesSelfPenalty() {
        val e = engine()
        val left = put(e, "Dreg", e.cpu, 1, 1)
        val center = put(e, "Dreg", e.cpu, 1, 2)
        val right = put(e, "Dreg", e.cpu, 1, 3)
        val otherRow = put(e, "Dreg", e.cpu, 0, 2)
        val gwynn = play(e, "Gwynn", 1, 4, ShadowEngine.Target(e.cpu, 1, 2))
        assertEquals(7, left.power)
        assertEquals(7, center.power)
        assertEquals(7, right.power)
        assertEquals(10, otherRow.power)
        assertEquals(4, gwynn.power)
    }
}
