package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test

/**
 * Real activation cases based on the user's original Shadowshaper card-detail
 * recordings, not TodayInDestiny/community text. Run with local JVM checks.
 */
class ShadowOriginalVideoActivationTest {
    private fun card(
        name: String, faction: String, power: Int, cost: Int,
        effect: String, source: String = ShadowData.VIDEO_SOURCE
    ) = ShadowData.Card().apply {
        this.name = name
        this.faction = faction
        this.power = power.toString()
        this.cost = cost.toString()
        this.effect = effect
        this.rarity = "Rare"
        this.source = source
    }

    private fun engine(): ShadowEngine {
        val cards = listOf(
            card("Resilient Marauder", "Fallen", 6, 4,
                "When Played (Front Row): Deals 5 damage to an opposing unit."),
            card("Shinka Incendior", "Cabal", 5, 3,
                "When Played (3-Unit Line): Deals 3 damage to a unit in the back row."),
            card("Shinka Psion", "Cabal", 3, 2,
                "When Played (2-Unit Line): Deals 3 damage to a unit."),
            card("Shinka Decurion", "Cabal", 1, 1,
                "Sustained (3-Unit Line): Your Cabal units deal 2 additional damage.",
                ShadowData.VIDEO_NOTES_SOURCE),
            card("Tenacious Wizard", "Hive", 5, 4,
                "When Played (Back Row): Grants Inspired to 2 random Hive units."),
            card("Thrall", "Hive", 2, 1, "No special effects.",
                ShadowData.VIDEO_NOTES_SOURCE),
            card("Ayuela", "Humans", 1, 1,
                "Sustained (Back Row): When the opponent places a unit in the front row, deals 2 damage for 2 turns.",
                ShadowData.VIDEO_NOTES_SOURCE),
            card("Wolf", "Humans", 3, 3,
                "When Played: If you have more than 5 units, destroys an opposing unit with 5 Power or more.",
                ShadowData.VIDEO_NOTES_SOURCE),
            card("Dreg", "Fallen", 15, 1, "No special effects.",
                ShadowData.VIDEO_NOTES_SOURCE),
            card("Line Mention Only", "Cabal", 3, 1,
                "Sustained: Help an adjacent Line unit.", ""),
            card("Cabal Trooper", "Cabal", 8, 1, "No special effects.")
        ).associateBy { it.name }
        return ShadowEngine(cards, 10L).apply {
            humanFirst = true
            humanTurn = true
            human.points = 50
        }
    }

    private fun place(e: ShadowEngine, name: String, owner: ShadowEngine.Player,
                      row: Int, col: Int, newPower: Int? = null) =
        ShadowEngine.Unit(owner, e.catalog.getValue(name), row, col).also {
            if (newPower != null) it.power = newPower
            owner.board[row][col] = it
        }

    private fun play(e: ShadowEngine, name: String, row: Int, col: Int,
                     target: ShadowEngine.Target? = null) {
        e.human.hand.add(e.catalog.getValue(name))
        assertTrue(e.validatePlay(e.human, e.human.hand.lastIndex, row, col, target),
            e.play(e.human, e.human.hand.lastIndex, row, col, target))
    }

    @Test fun recordedMarauderDealsFiveOnlyFromFrontRow() {
        val front = engine()
        val victim = place(front, "Dreg", front.cpu, 0, 0)
        play(front, "Resilient Marauder", 1, 2,
            ShadowEngine.Target(front.cpu, 0, 0))
        assertEquals(10, victim.power)

        val back = engine()
        val untouched = place(back, "Dreg", back.cpu, 0, 0)
        play(back, "Resilient Marauder", 0, 2)
        assertEquals(15, untouched.power)
        assertEquals(0, back.unresolvedEffects)
    }

    @Test fun threePrintedLineUnitsActivateShinkaIncendiorAgainstBackRow() {
        val e = engine()
        place(e, "Shinka Psion", e.human, 1, 0)
        place(e, "Shinka Psion", e.human, 1, 1)
        val backEnemy = place(e, "Dreg", e.cpu, 0, 2)
        val frontEnemy = place(e, "Dreg", e.cpu, 1, 3)

        play(e, "Shinka Incendior", 1, 2,
            ShadowEngine.Target(e.cpu, 0, 2))
        assertEquals(12, backEnemy.power)
        assertEquals(15, frontEnemy.power)
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun plainCabalNeighborWithLineMentionCannotSatisfyPrintedLine() {
        val e = engine()
        place(e, "Line Mention Only", e.human, 1, 0)
        place(e, "Shinka Psion", e.human, 1, 1)
        val enemy = place(e, "Dreg", e.cpu, 0, 1)
        val card = e.catalog.getValue("Shinka Incendior")
        assertEquals(2, e.projectedFactionLine(e.human, card, 1, 2))
        assertTrue(e.targetOptions(e.human, card, 1, 2).isEmpty())
        play(e, "Shinka Incendior", 1, 2)
        assertEquals(15, enemy.power)
    }

    @Test fun shinkaDecurionAddsDamageOnlyWhilePrintedThreeLineActive() {
        val e = engine()
        val attacker = place(e, "Shinka Psion", e.human, 1, 0)
        val decurion = place(e, "Shinka Decurion", e.human, 1, 1)
        place(e, "Shinka Psion", e.human, 1, 2)
        val victim = place(e, "Dreg", e.cpu, 1, 5)

        e.damage(attacker, victim, 1)
        assertEquals(12, victim.power)
        e.suppress(decurion)
        e.damage(attacker, victim, 1)
        assertEquals(11, victim.power)
    }

    @Test fun recordedTenaciousWizardInspiresTwoHiveOnlyFromBackRow() {
        val e = engine()
        val left = place(e, "Thrall", e.human, 1, 0)
        val right = place(e, "Thrall", e.human, 1, 1)
        play(e, "Tenacious Wizard", 0, 4)
        assertTrue(left.inspired)
        assertTrue(right.inspired)
        assertEquals(0, e.unresolvedEffects)

        val front = engine()
        val ally = place(front, "Thrall", front.human, 1, 0)
        play(front, "Tenacious Wizard", 1, 4)
        assertFalse(ally.inspired)
    }

    @Test fun videoNotedAyuelaOnlyScorchesOpposingFrontRowArrivals() {
        val e = engine()
        place(e, "Ayuela", e.human, 0, 0)
        assertTrue(e.spawnAt(e.cpu, 0, 3, "Dreg"))
        val enemyBack = e.cpu.board[0][3]!!
        assertTrue(enemyBack.scorchEffects.isEmpty())
        assertTrue(e.spawnAt(e.cpu, 1, 3, "Dreg"))
        val enemyFront = e.cpu.board[1][3]!!
        assertEquals(2, enemyFront.scorchTurns)
        assertEquals(2, enemyFront.scorched)
        assertEquals(1, enemyFront.scorchEffects.size)
    }

    @Test fun videoNotedWolfWorksFromEitherRowAfterSixAlliedUnits() {
        for (row in listOf(0, 1)) {
            val e = engine()
            for (col in 0..4) place(e, "Cabal Trooper", e.human, 1-row, col)
            val enemy = place(e, "Dreg", e.cpu, 1, 1)
            play(e, "Wolf", row, 2, ShadowEngine.Target(e.cpu, 1, 1))
            assertNull(e.cpu.board[1][1])
            assertTrue(enemy.power < 15)
        }
    }

    @Test fun recordedLineRestrictionsAreIndependentOfUnverifiedMetadata() {
        val e = engine()
        val card = e.catalog.getValue("Shinka Psion")
        card.trigger = "When Played; Back Row" // deliberately conflicting
        place(e, "Shinka Psion", e.human, 1, 1)
        val enemy = place(e, "Dreg", e.cpu, 1, 3)
        play(e, "Shinka Psion", 1, 2,
            ShadowEngine.Target(e.cpu, 1, 3))
        assertEquals(12, enemy.power)
    }
}
