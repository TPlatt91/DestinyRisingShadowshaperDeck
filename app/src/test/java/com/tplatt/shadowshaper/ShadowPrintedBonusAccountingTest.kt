package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test

/**
 * Printed "bonus" must refer to active positive Power modifiers, not
 * current Power minus the original stat after unrelated damage events.
 */
class ShadowPrintedBonusAccountingTest {
    private fun card(name: String, faction: String, power: Int, effect: String) =
        ShadowData.Card().apply {
            this.name = name
            this.faction = faction
            this.power = power.toString()
            this.cost = "1"
            this.rarity = "Rare"
            this.effect = effect
        }

    private fun engine() = ShadowEngine(listOf(
        card("Dreg", "Fallen", 10, "No special effects."),
        card("Thrall", "Hive", 20, "No special effects."),
        card("Bazinet", "Humans", 2,
            "When Played (Back Row): Deals 3 damage to all opposing units with a bonus."),
        card("Thinktank", "Humans", 7,
            "When Played: Deals damage to a unit equal to your highest Power bonus."),
        card("Hyssop", "Humans", 2,
            "When Played: Grants +3 Power to a unit with a bonus."),
        card("Decurion", "Cabal", 3,
            "Sustained (Column): Grants +2 Power to a unit directly in front of or behind.")
    ).associateBy { it.name }, 28L).apply {
        humanTurn = true
        humanFirst = true
        human.points = 40
    }

    private fun put(e: ShadowEngine, name: String, side: ShadowEngine.Player,
                    row: Int, col: Int): ShadowEngine.Unit {
        val unit = ShadowEngine.Unit(side, e.catalog.getValue(name), row, col)
        side.board[row][col] = unit
        return unit
    }

    @Test fun truePowerBonusRemainsAfterTheUnitTakesDamage() {
        val e = engine()
        val unit = put(e, "Dreg", e.human, 1, 2)
        val attacker = put(e, "Dreg", e.cpu, 1, 2)

        e.buff(unit, 4)
        assertEquals(14, unit.power)
        e.damage(attacker, unit, 6)
        assertEquals(8, unit.power) // below its printed 10
        assertEquals(4, unit.bonus)
        assertEquals(4, e.powerBonus(unit))
    }

    @Test fun bazinetStillHitsDamagedEnemyWithActivePowerBonus() {
        val e = engine()
        val source = put(e, "Dreg", e.human, 1, 5)
        val buffed = put(e, "Dreg", e.cpu, 1, 1)
        val unbuffed = put(e, "Dreg", e.cpu, 1, 3)
        e.buff(buffed, 3)
        e.damage(source, buffed, 5) // 10 + 3 - 5 = 8
        e.damage(source, unbuffed, 5) // 10 - 5 = 5
        assertEquals(8, buffed.power)
        assertEquals(5, unbuffed.power)

        e.human.hand.add(e.catalog.getValue("Bazinet"))
        assertTrue(e.lastActionError,
            e.play(e.human, e.human.hand.lastIndex, 0, 2, null))
        assertEquals(5, buffed.power)
        assertEquals(5, unbuffed.power)
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun thinktankUsesHighestActiveBonusDespiteEarlierDamage() {
        val e = engine()
        val buffed = put(e, "Dreg", e.human, 1, 1)
        val attacker = put(e, "Dreg", e.cpu, 1, 5)
        val victim = put(e, "Thrall", e.cpu, 1, 2)
        e.buff(buffed, 5)
        e.damage(attacker, buffed, 7) // 10 + 5 - 7 = 8
        assertEquals(8, buffed.power)

        e.human.hand.add(e.catalog.getValue("Thinktank"))
        assertTrue(e.lastActionError, e.play(e.human, e.human.hand.lastIndex,
            0, 2, ShadowEngine.Target(e.cpu, 1, 2)))
        assertEquals(15, victim.power)
        assertEquals(5, e.powerBonus(buffed))
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun sustainedColumnBonusCountsEvenWhenDamageObscuresNetPower() {
        val e = engine()
        val source = put(e, "Decurion", e.human, 0, 1)
        val ally = put(e, "Dreg", e.human, 1, 1)
        val attacker = put(e, "Dreg", e.cpu, 1, 5)

        e.damage(attacker, ally, 5)
        assertEquals(7, e.power(ally)) // 10 - 5 + 2 aura
        assertEquals(2, e.powerBonus(ally))
        e.human.board[source.row][source.column] = null
        assertEquals(0, e.powerBonus(ally))
    }

    @Test fun suppressingTargetClearsStoredBonusesAndDisablesColumnAura() {
        val e = engine()
        put(e, "Decurion", e.human, 0, 1)
        val ally = put(e, "Dreg", e.human, 1, 1)
        e.buff(ally, 3)
        assertEquals(5, e.powerBonus(ally))
        e.suppress(ally)
        assertTrue(ally.suppressed)
        assertEquals(0, e.powerBonus(ally))
        assertEquals(10, ally.power)
    }
    @Test fun hyssopCanTargetAllyWithOnlyAnActiveColumnBonus() {
        val e = engine()
        put(e, "Decurion", e.human, 0, 1)
        val ally = put(e, "Dreg", e.human, 1, 1)
        val hyssop = e.catalog.getValue("Hyssop")
        assertEquals(0, ally.bonus)
        assertEquals(2, e.powerBonus(ally))
        val candidates = e.targetOptions(e.human, hyssop, 0, 3)
        assertEquals(1, candidates.size)
        assertEquals(1, candidates.single().column)
        assertEquals(1, candidates.single().row)

        e.human.hand.add(hyssop)
        assertTrue(e.lastActionError, e.play(e.human, 0, 0, 3,
            ShadowEngine.Target(e.human, 1, 1)))
        assertEquals(13, ally.power)
        assertEquals(3, ally.bonus)
        assertEquals(5, e.powerBonus(ally))
        assertEquals(15, e.power(ally))
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun hyssopCannotTargetAuraOnlyAllyAfterSourceIsSuppressed() {
        val e = engine()
        val decurion = put(e, "Decurion", e.human, 0, 1)
        put(e, "Dreg", e.human, 1, 1)
        val hyssop = e.catalog.getValue("Hyssop")
        val selection = ShadowEngine.Target(e.human, 1, 1)
        e.human.hand.add(hyssop)
        assertTrue(e.validatePlay(e.human, 0, 0, 3, selection).isEmpty())

        e.suppress(decurion)
        assertEquals(0, e.powerBonus(selection.unit()!!))
        assertTrue(e.targetOptions(e.human, hyssop, 0, 3).isEmpty())
        val beforeLight = e.human.points
        assertFalse(e.play(e.human, 0, 0, 3, selection))
        assertEquals(beforeLight, e.human.points)
        assertEquals(1, e.human.hand.size)
        assertNull(e.human.board[0][3])
    }

    @Test fun hyssopAcceptsDamagedRecipientStillCarryingStoredBonus() {
        val e = engine()
        val ally = put(e, "Dreg", e.human, 1, 1)
        val attacker = put(e, "Dreg", e.cpu, 1, 5)
        e.buff(ally, 3)
        e.damage(attacker, ally, 5)
        assertEquals(8, ally.power)
        assertEquals(3, e.powerBonus(ally))

        e.human.hand.add(e.catalog.getValue("Hyssop"))
        assertTrue(e.lastActionError, e.play(e.human, 0, 0, 3,
            ShadowEngine.Target(e.human, 1, 1)))
        assertEquals(11, ally.power)
        assertEquals(6, ally.bonus)
        assertEquals(6, e.powerBonus(ally))
    }

    @Test fun cpuHyssopAlsoRecognizesAnActiveAuraOnlyTarget() {
        val e = engine()
        e.humanTurn = false
        e.cpu.points = 1
        put(e, "Decurion", e.cpu, 0, 1)
        val ally = put(e, "Dreg", e.cpu, 1, 1)
        assertEquals(0, ally.bonus)
        assertEquals(2, e.powerBonus(ally))
        e.cpu.hand.add(e.catalog.getValue("Hyssop"))
        assertTrue(e.cpuPlayOne())
        assertEquals(13, ally.power)
        assertEquals(5, e.powerBonus(ally))
        assertTrue(e.cpu.hand.isEmpty())
        assertEquals(0, e.rejectedCpuMoves)
        assertEquals(0, e.unresolvedEffects)
    }

}
