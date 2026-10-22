package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test
import java.util.Collections
import java.util.Random

/**
 * End-turn sustained abilities are fully resolved one damage event at a time.
 * Run locally through run_local_engine_checks.bat; never invokes GitHub Actions.
 */
class ShadowEndTurnChainedDamageTest {
    private fun card(name: String, faction: String, power: Int, effect: String) =
        ShadowData.Card().apply {
            this.name = name
            this.faction = faction
            rarity = "Rare"
            cost = "1"
            this.power = power.toString()
            this.effect = effect
        }

    private fun fixture(seed: Long): ShadowEngine {
        val cards = listOf(
            card("Helhest", "Humans", 6,
                "Sustained (Back Row): Deal 1 damage to a random enemy and units left and right, gaining Power equal to damage dealt."),
            card("Elite Walker", "Fallen", 5,
                "Sustained (Back Row): Deal 2 damage to two random opposing units."),
            card("Clone", "Humans", 1,
                "When destroyed: Deal 3 damage to the units to its left and right."),
            card("Dreg", "Fallen", 10, "No special effects.")
        ).associateBy { it.name }
        return ShadowEngine(cards, seed)
    }

    private fun put(e: ShadowEngine, name: String, side: ShadowEngine.Player,
                    row: Int, col: Int, power: Int? = null): ShadowEngine.Unit {
        val unit = ShadowEngine.Unit(side, e.catalog.getValue(name), row, col)
        if (power != null) unit.power = power
        side.board[row][col] = unit
        return unit
    }

    @Test fun helhestSkipsCenterDefeatedByFirstCloneDeathSplash() {
        // The first random pick must be center (board index 1), yielding
        // left Clone, center fragile Dreg, right healthy Dreg as the hit list.
        val seed = (0L..1000L).first { Random(it).nextInt(3) == 1 }
        val e = fixture(seed)
        val helhest = put(e, "Helhest", e.human, ShadowRules.BACK_ROW, 3)
        put(e, "Clone", e.cpu, ShadowRules.BACK_ROW, 0)
        put(e, "Dreg", e.cpu, ShadowRules.BACK_ROW, 1, power = 2)
        val right = put(e, "Dreg", e.cpu, ShadowRules.BACK_ROW, 2)

        e.effects.endSide(e.human)

        assertNull(e.cpu.board[0][0]) // First 1-damage hit defeats Clone
        assertNull(e.cpu.board[0][1]) // Clone splash defeats center, no later hit
        assertSame(right, e.cpu.board[0][2])
        assertEquals(9, right.power)
        assertEquals(8, helhest.power) // Only Clone and right received damage
        assertEquals(1, e.journal.count { it == "DAMAGE 1 ▶ Dreg" })
    }

    @Test fun eliteWalkerSkipsSecondTargetDefeatedByFirstCloneSplash() {
        // Shuffle should select Clone before the 2-Power Dreg.
        val seed = (0L..1000L).first { value ->
            val indices = arrayListOf(0, 1)
            Collections.shuffle(indices, Random(value))
            indices[0] == 0
        }
        val e = fixture(seed)
        put(e, "Elite Walker", e.human, ShadowRules.BACK_ROW, 3)
        put(e, "Clone", e.cpu, ShadowRules.BACK_ROW, 0)
        put(e, "Dreg", e.cpu, ShadowRules.BACK_ROW, 1, power = 2)

        e.effects.endSide(e.human)

        assertNull(e.cpu.board[0][0])
        assertNull(e.cpu.board[0][1])
        assertEquals(0, e.journal.count { it == "DAMAGE 2 ▶ Dreg" })
        assertEquals(1, e.journal.count { it == "DAMAGE 3 ▶ Dreg" })
    }

    @Test fun helhestCountsOnlyUnblockedHitsAndDoesNotConsumeShieldedPower() {
        val e = fixture(5L)
        val helhest = put(e, "Helhest", e.human, ShadowRules.BACK_ROW, 3)
        val protected = put(e, "Dreg", e.cpu, ShadowRules.FRONT_ROW, 0)
        protected.overshield = true

        e.effects.endSide(e.human)

        assertEquals(6, helhest.power)
        assertEquals(10, protected.power)
        assertFalse(protected.overshield)
        assertTrue(e.journal.any { it.contains("Helhest: dealt 0 damage") })
    }

    @Test fun suppressedEndTurnSourcesCannotDealChainedDamage() {
        val e = fixture(5L)
        val walker = put(e, "Elite Walker", e.human, 0, 3)
        val helhest = put(e, "Helhest", e.human, 0, 2)
        val enemy = put(e, "Dreg", e.cpu, 1, 3)
        walker.suppressed = true
        helhest.suppressed = true

        e.effects.endSide(e.human)

        assertEquals(10, enemy.power)
        assertEquals(6, helhest.power)
        assertEquals(5, walker.power)
    }
}
