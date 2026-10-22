package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test
import java.util.Random

/** Overshield, protection, damage reduction and next-event modifier coverage.
 * Execute locally through run_local_engine_checks.bat; does not use Actions.
 */
class ShadowProtectionAndDamageTest {
    private fun card(name: String, faction: String, effect: String, power: Int = 10) =
        ShadowData.Card().apply {
            this.name = name
            this.faction = faction
            this.effect = effect
            this.power = power.toString()
            this.cost = "1"
            this.rarity = "Rare"
        }

    private fun engine(seed: Long = 0L): ShadowEngine {
        val cards = listOf(
            card("Dreg", "Fallen", "No special effects."),
            card("Legionary", "Cabal", "No special effects."),
            card("Human Guard", "Humans", "No special effects."),
            card("Mind Disruptor", "Fallen",
                "Sustained: The unit on its left cannot be Suppressed."),
            card("Jolder", "Humans",
                "Sustained: The next instance of damage dealt is doubled."),
            card("Umeko", "Humans",
                "Sustained (Back Row): Your units randomly deal 1-3 additional damage."),
            card("Shinka Phalanx", "Cabal",
                "Sustained (Column): Units directly in front of or behind take 1 less damage."),
            card("Saladin", "Humans",
                "Sustained: Human allies receiving damage gain +3 Power."),
            card("Rossi-11", "Humans",
                "When Played: Grants Overshield to one of your units.", 3)
        ).associateBy { it.name }
        return ShadowEngine(cards, seed)
    }

    private fun place(e: ShadowEngine, name: String, side: ShadowEngine.Player,
                      row: Int, col: Int): ShadowEngine.Unit {
        val unit = ShadowEngine.Unit(side, e.catalog.getValue(name), row, col)
        side.board[row][col] = unit
        return unit
    }

    @Test fun mindDisruptorPreventsSuppressionWithoutSpendingOvershield() {
        val e = engine()
        val ally = place(e, "Dreg", e.human, 1, 2)
        place(e, "Mind Disruptor", e.human, 1, 3)
        e.buff(ally, 3)
        ally.overshield = true
        e.suppress(ally)
        assertFalse(ally.suppressed)
        assertTrue(ally.overshield)
        assertEquals(13, ally.power)
        assertEquals(3, ally.bonus)

        e.human.board[1][3] = null
        e.suppress(ally)
        assertFalse(ally.suppressed)
        assertFalse(ally.overshield)
        assertEquals(13, ally.power)

        e.suppress(ally)
        assertTrue(ally.suppressed)
        assertEquals(10, ally.power)
        assertEquals(0, ally.bonus)
    }

    @Test fun overshieldAbsorbsDamageAndPreservesNextJolderHit() {
        val e = engine()
        val source = place(e, "Dreg", e.human, 1, 1)
        val jolder = place(e, "Jolder", e.human, 0, 1)
        val target = place(e, "Legionary", e.cpu, 1, 2)
        target.overshield = true

        e.damage(source, target, 2)
        assertEquals(10, target.power)
        assertFalse(target.overshield)
        assertFalse(jolder.damageDoublerConsumed)

        e.damage(source, target, 2)
        assertEquals(6, target.power)
        assertTrue(jolder.damageDoublerConsumed)
        e.damage(source, target, 1)
        assertEquals(5, target.power)
    }

    @Test fun damageFullyPreventedByReductionDoesNotConsumeJolder() {
        val e = engine()
        val source = place(e, "Dreg", e.human, 1, 1)
        val jolder = place(e, "Jolder", e.human, 0, 1)
        val defender = place(e, "Legionary", e.cpu, 1, 2)
        place(e, "Shinka Phalanx", e.cpu, 0, 2)

        e.damage(source, defender, 1)
        assertEquals(10, defender.power)
        assertFalse(jolder.damageDoublerConsumed)
        assertTrue(e.journal.any { it.contains("DAMAGE PREVENTED") })

        e.cpu.board[0][2] = null
        e.damage(source, defender, 2)
        assertEquals(6, defender.power)
        assertTrue(jolder.damageDoublerConsumed)
    }

    @Test fun umekoDamageBonusIsSubjectToColumnReduction() {
        val seed = 31L
        val e = engine(seed)
        place(e, "Umeko", e.human, ShadowRules.BACK_ROW, 0)
        val source = place(e, "Dreg", e.human, ShadowRules.FRONT_ROW, 1)
        val defender = place(e, "Legionary", e.cpu, ShadowRules.FRONT_ROW, 3)
        place(e, "Shinka Phalanx", e.cpu, ShadowRules.BACK_ROW, 3)

        // Raw 1 + Umeko [1,3] - 1 column reduction = Umeko roll.
        val umekoRoll = 1 + Random(seed).nextInt(3)
        e.damage(source, defender, 1)
        assertEquals(10 - umekoRoll, defender.power)
    }

    @Test fun overshieldIsNotConsumedByFullyPreventedDamage() {
        val e = engine()
        val source = place(e, "Dreg", e.human, 1, 1)
        val defender = place(e, "Legionary", e.cpu, 1, 2)
        place(e, "Shinka Phalanx", e.cpu, 0, 2)
        defender.overshield = true

        e.damage(source, defender, 1)
        assertEquals(10, defender.power)
        assertTrue(defender.overshield)
        e.damage(source, defender, 2)
        assertEquals(10, defender.power)
        assertFalse(defender.overshield)
        e.damage(source, defender, 2)
        assertEquals(9, defender.power)
    }

    @Test fun rossiCanShieldItselfEvenWhenOtherAlliesAreAvailable() {
        val e = engine()
        e.humanTurn = true
        e.human.points = 2
        place(e, "Dreg", e.human, 0, 0)
        val rossi = e.catalog.getValue("Rossi-11")
        e.human.hand.add(rossi)
        val options = e.targetOptions(e.human, rossi, 1, 2)
        assertEquals(1, options.size)
        assertTrue(e.validatePlay(e.human, 0, 1, 2, null).isEmpty())
        assertTrue(e.play(e.human, 0, 1, 2, null))
        assertTrue(e.human.board[1][2]!!.overshield)
        assertEquals(3, e.human.board[1][2]!!.power)
        assertEquals(1, e.human.points)
    }

    @Test fun saladinBuffsOnlyAfterActualDamageNotBlockedHit() {
        val e = engine()
        val attacker = place(e, "Dreg", e.cpu, 1, 1)
        val victim = place(e, "Human Guard", e.human, 1, 2)
        place(e, "Saladin", e.human, 0, 0)
        victim.overshield = true

        e.damage(attacker, victim, 2)
        assertEquals(10, victim.power)
        assertEquals(0, victim.bonus)

        e.damage(attacker, victim, 2)
        assertEquals(11, victim.power)
        assertEquals(3, victim.bonus)
    }
}
