package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test

/**
 * Damage-event totals and death resolution between arrival phases.
 * All tests run locally through run_local_engine_checks.bat.
 */
class ShadowDamageAccountingAndArrivalTest {
    private fun card(name: String, faction: String, power: Int, effect: String = "No special effects.") =
        ShadowData.Card().apply {
            this.name = name
            this.faction = faction
            this.rarity = "Rare"
            this.cost = "1"
            this.power = power.toString()
            this.effect = effect
        }

    private fun engine(): ShadowEngine {
        val catalog = listOf(
            card("Dreg", "Fallen", 1),
            card("Human Guard", "Humans", 10),
            card("Saladin", "Humans", 6,
                "Sustained: Whenever one of your Human units takes damage, grants them +3 Power."),
            card("Servitor", "Fallen", 2,
                "Sustained: Your units deal 1 extra damage."),
            card("Zhiyao", "Humans", 3,
                "Sustained: When opponent places a Front Row unit, gains 1 Power for three turns."),
            card("Helhest", "Humans", 6,
                "Sustained (Back Row): At the end of your turn deals 1 damage to a random unit and its left and right and gains Power from damage dealt."),
            card("Gwynn", "Humans", 1,
                "When Played (Front Row): Deals 3 damage to target and adjacent units, then loses 2 Power."),
            card("Zero Unit", "Fallen", 0)
        ).associateBy { it.name }
        return ShadowEngine(catalog, 11L).apply {
            humanTurn = true
            humanFirst = true
            human.points = 10
        }
    }

    private fun put(e: ShadowEngine, name: String, side: ShadowEngine.Player,
                    row: Int, col: Int): ShadowEngine.Unit {
        val unit = ShadowEngine.Unit(side, e.catalog.getValue(name), row, col)
        side.board[row][col] = unit
        return unit
    }

    @Test fun overkillReportsTheFullEventNotRemainingVictimPower() {
        val e = engine()
        val source = put(e, "Servitor", e.human, 0, 0)
        val weak = put(e, "Dreg", e.cpu, 1, 0)
        // Raw 1 + Servitor's 1 outgoing damage = 2, despite 1-Power Dreg.
        assertEquals(2, e.resolvedDamage(source, weak, 1))
        assertEquals(-1, weak.power)
        e.sweep()
        assertNull(e.cpu.board[1][0])
    }

    @Test fun targetPowerReactionDoesNotHideDamageThatWasActuallyDealt() {
        val e = engine()
        val attacker = put(e, "Dreg", e.human, 1, 0)
        val target = put(e, "Human Guard", e.cpu, 1, 1)
        put(e, "Saladin", e.cpu, 0, 4)
        val dealt = e.resolvedDamage(attacker, target, 1)
        assertEquals(1, dealt)
        // Hit for 1, then Saladin grants +3: final Power is higher than before.
        assertEquals(12, target.power)
        assertEquals(3, target.bonus)
    }

    @Test fun blockedDamageReportsZeroWithoutChangingTheTarget() {
        val e = engine()
        val attacker = put(e, "Dreg", e.human, 1, 0)
        val target = put(e, "Human Guard", e.cpu, 1, 1)
        target.overshield = true
        assertEquals(0, e.resolvedDamage(attacker, target, 1))
        assertEquals(10, target.power)
        assertFalse(target.overshield)
    }

    @Test fun helhestGainsFullOverkillDamageFromAllThreeTargets() {
        val e = engine()
        val helhest = put(e, "Helhest", e.human, ShadowRules.BACK_ROW, 3)
        put(e, "Servitor", e.human, 0, 0)
        for (column in 1..3) put(e, "Dreg", e.cpu, 1, column)
        // The random central target is one of three consecutive targets.
        // Its 1–3 horizontal neighborhood varies by seed, so select a
        // deterministic seed which selects the center for this scenario.
        val all = listOf(1, 2, 3)
        val chosen = all[e.rng.nextInt(3)]
        // Reset the RNG to preserve the deterministic draw that was inspected.
        e.rng.setSeed(11L)
        val expectedTargets = all.count { kotlin.math.abs(it - chosen) <= 1 }
        e.effects.endSide(e.human)
        assertEquals(6 + expectedTargets * 2, helhest.power)
        assertEquals(expectedTargets, e.journal.count { it.contains("DAMAGE 2 ▶ Dreg") })
    }

    @Test fun defeatedSummonCannotTriggerOpposingArrivalBonuses() {
        val e = engine()
        val zhiyao = put(e, "Zhiyao", e.cpu, 0, 0)
        e.spawnAt(e.human, ShadowRules.FRONT_ROW, 3, "Zero Unit")
        assertNull(e.human.board[ShadowRules.FRONT_ROW][3])
        assertEquals(3, zhiyao.power)
        assertTrue(zhiyao.timedBonuses.isEmpty())
    }

    @Test fun selfDefeatedPlayedUnitCannotTriggerOpposingArrivalBonuses() {
        val e = engine()
        val zhiyao = put(e, "Zhiyao", e.cpu, 0, 0)
        put(e, "Human Guard", e.cpu, ShadowRules.FRONT_ROW, 2)
        e.human.hand.add(e.catalog.getValue("Gwynn"))
        assertTrue(e.lastActionError, e.play(e.human, 0, ShadowRules.FRONT_ROW, 3,
            ShadowEngine.Target(e.cpu, ShadowRules.FRONT_ROW, 2)))
        assertNull(e.human.board[ShadowRules.FRONT_ROW][3])
        assertEquals(3, zhiyao.power)
        assertTrue(zhiyao.timedBonuses.isEmpty())
    }
}
