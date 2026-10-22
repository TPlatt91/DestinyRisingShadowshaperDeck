package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test

/**
 * CPU card-effect targeting regressions. These are strategic heuristics;
 * the same legal-target filter continues to be used by both players.
 */
class ShadowCpuTargetEvaluationTest {
    private fun card(name: String, faction: String, power: Int, effect: String) =
        ShadowData.Card().apply {
            this.name = name
            this.faction = faction
            this.power = power.toString()
            cost = "1"
            rarity = "Rare"
            this.effect = effect
        }

    private fun fixture(): ShadowEngine {
        val cards = listOf(
            card("Vandal", "Fallen", 2, "When Played: Deals 2 damage to an opposing Front Row unit."),
            card("Psion", "Cabal", 2, "When Played (Back Row): Deals 2 damage to a unit."),
            card("Ikora", "Humans", 13, "When Played: Deals 2 damage to all opposing units in a row."),
            card("Wu Ming", "Humans", 3, "When Played (Back Row): Suppresses a unit."),
            card("Wizard", "Hive", 4, "When Played: Grants Inspired to 1 of your Hive units."),
            card("Rossi-11", "Humans", 3, "When Played: Grants an Overshield to an allied unit."),
            card("Gladiator", "Cabal", 4, "When Played (Front Row): Allied Front Row target and its neighbors gain 1 outgoing damage."),
            card("Mind Disruptor", "Fallen", 2, "Sustained: Unit to the left cannot be Suppressed."),
            card("Dreg", "Fallen", 2, "No special effects."),
            card("Thrall", "Hive", 2, "No special effects."),
            card("Cabal Soldier", "Cabal", 2, "No special effects.")
        ).associateBy { it.name }
        return ShadowEngine(cards, 47L).apply {
            cpu.points = 20
            human.points = 20
            humanTurn = false
        }
    }

    private fun put(e: ShadowEngine, name: String, owner: ShadowEngine.Player,
                    row: Int, column: Int, rawPower: Int? = null) =
        ShadowEngine.Unit(owner, e.catalog.getValue(name), row, column).also {
            if (rawPower != null) it.power = rawPower
            owner.board[row][column] = it
        }

    private fun selected(
        e: ShadowEngine, cardName: String, row: Int = 0, column: Int = 5
    ): ShadowEngine.Target? = e.chooseAI(e.cpu, e.catalog.getValue(cardName), row, column)

    @Test fun vandalPrefersAConfirmedEliminationOverWeakeningAHealthyEnemy() {
        val e = fixture()
        put(e, "Dreg", e.human, 1, 0, rawPower = 2)
        put(e, "Dreg", e.human, 1, 2, rawPower = 12)
        val target = selected(e, "Vandal")
        assertNotNull(target)
        assertEquals(0, target!!.column)
    }

    @Test fun lethalVandalHitBeatsAnExtremelyHighPowerEnemy() {
        val e = fixture()
        put(e, "Dreg", e.human, 1, 0, rawPower = 2)
        put(e, "Dreg", e.human, 1, 2, rawPower = 80)

        assertEquals(0, selected(e, "Vandal")!!.column)
    }

    @Test fun fixedDamagePrefersAUnitNearerToEliminationWhenNeitherIsLethal() {
        val e = fixture()
        put(e, "Dreg", e.human, 1, 0, rawPower = 3)
        put(e, "Dreg", e.human, 1, 2, rawPower = 50)

        assertEquals(0, selected(e, "Vandal")!!.column)
    }

    @Test fun areaDamageStillFavorsClusterOverHugeSingleEnemy() {
        val e = fixture()
        for (col in 0..2) put(e, "Dreg", e.human, 0, col, rawPower = 4)
        put(e, "Dreg", e.human, 1, 3, rawPower = 80)

        assertEquals(0, selected(e, "Ikora")!!.row)
    }

    @Test fun directDamagePrefersAnUnshieldedUnitToAnOvershieldedOne() {
        val e = fixture()
        val shielded = put(e, "Dreg", e.human, 1, 1, rawPower = 16)
        shielded.overshield = true
        put(e, "Dreg", e.human, 1, 4, rawPower = 2)
        assertEquals(4, selected(e, "Vandal")!!.column)
    }

    @Test fun splashDamageTargetsTheMorePopulatedEnemyRow() {
        val e = fixture()
        put(e, "Dreg", e.human, 0, 1, rawPower = 7)
        put(e, "Dreg", e.human, 0, 2, rawPower = 7)
        put(e, "Dreg", e.human, 0, 3, rawPower = 7)
        put(e, "Dreg", e.human, 1, 0, rawPower = 9)
        assertEquals(0, selected(e, "Ikora")!!.row)
    }

    @Test fun suppressionPrioritizesEnemyWithMeaningfulPowerBonuses() {
        val e = fixture()
        val buffed = put(e, "Dreg", e.human, 1, 0, rawPower = 2)
        e.buff(buffed, 8)
        put(e, "Dreg", e.human, 1, 2, rawPower = 20)
        assertEquals(0, selected(e, "Wu Ming")!!.column)
    }

    @Test fun suppressionAvoidsMindDisruptorProtectedTarget() {
        val e = fixture()
        val protected = put(e, "Dreg", e.human, 1, 0, rawPower = 2)
        e.buff(protected, 6)
        put(e, "Mind Disruptor", e.human, 1, 1)
        val alternative = put(e, "Dreg", e.human, 1, 3, rawPower = 2)
        e.buff(alternative, 4)
        assertEquals(3, selected(e, "Wu Ming")!!.column)
    }

    @Test fun wizardInspiresAnUninspiredHiveAllyFirst() {
        val e = fixture()
        val alreadyInspired = put(e, "Thrall", e.cpu, 0, 0, rawPower = 20)
        alreadyInspired.inspired = true
        put(e, "Thrall", e.cpu, 1, 2, rawPower = 2)
        assertEquals(2, selected(e, "Wizard")!!.column)
    }

    @Test fun rossiShieldsItselfWhenAllOtherAlliesHaveOvershield() {
        val e = fixture()
        val protected = put(e, "Dreg", e.cpu, 0, 0)
        protected.overshield = true
        assertNull(selected(e, "Rossi-11"))
        protected.overshield = false
        assertEquals(0, selected(e, "Rossi-11")!!.column)
    }

    @Test fun gladiatorChoosesCenterOfAllyFrontRowGroup() {
        val e = fixture()
        for (col in 0..3) put(e, "Cabal Soldier", e.cpu, 1, col)
        val target = selected(e, "Gladiator", 1, 5)
        assertNotNull(target)
        assertTrue(target!!.column in 1..2)
    }

    @Test fun cpuPlayUsesHeuristicLethalTargetInActualEngine() {
        val e = fixture()
        put(e, "Dreg", e.human, 1, 0, rawPower = 2)
        val strong = put(e, "Dreg", e.human, 1, 2, rawPower = 12)
        e.cpu.hand.add(e.catalog.getValue("Vandal"))
        assertTrue(e.cpuPlayOne())
        assertNull(e.human.board[1][0])
        assertSame(strong, e.human.board[1][2])
        assertEquals(12, strong.power)
        assertTrue(e.cpu.hand.isEmpty())
        assertEquals(0, e.journal.count { it.contains("CPU rejected move") })
    }
}
