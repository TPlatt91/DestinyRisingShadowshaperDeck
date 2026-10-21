package com.tplatt.shadowshaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Local-only regression coverage for recording
 * screen-20261021-134608-1792604739278.mp4~2.mp4.
 *
 * No CI workflow runs when these tests are authored.
 */
class ShadowBatch04EffectsTest {
    private fun card(name: String, cost: Int, power: Int, effect: String): ShadowData.Card =
        ShadowData.Card().apply {
            this.name = name
            this.rarity = "Rare"
            this.faction = "Fallen"
            this.cost = cost.toString()
            this.power = power.toString()
            this.effect = effect
            this.trigger = if (name == "Grenade Dreg" || name == "Servitor")
                "Sustained" else ""
        }

    private fun engine(): ShadowEngine {
        val grenade = card("Grenade Dreg", 2, 1,
            "Sustained: Every unit deployed in this row grants this unit +1 Power.")
        val dreg = card("Dreg", 1, 1, "No special effects.")
        val vandal = card("Vandal", 2, 2,
            "When Played: Deals 2 damage to an opposing unit in the front row.")
        val servitor = card("Servitor", 2, 2,
            "Sustained: Your units deal 1 additional damage.")
        val e = ShadowEngine(mapOf(
            grenade.name to grenade,
            dreg.name to dreg,
            vandal.name to vandal,
            servitor.name to servitor
        ), 5L)
        e.humanTurn = true
        e.human.points = 10
        return e
    }

    @Test fun grenadeDregAccumulatesBonusesFromSameRowDeployments() {
        val e = engine()
        val grenade = e.catalog.getValue("Grenade Dreg")
        val dreg = e.catalog.getValue("Dreg")
        e.human.hand.add(grenade)
        assertTrue(e.play(e.human, 0, 1, 0, null))
        // Its own placement counts under the literal "Every unit deployed".
        val watcher = e.human.board[1][0]!!
        assertEquals(2, e.power(watcher))
        e.human.hand.add(dreg)
        assertTrue(e.play(e.human, 0, 1, 1, null))
        assertEquals(3, e.power(watcher))
        e.human.hand.add(dreg)
        assertTrue(e.play(e.human, 0, 0, 0, null))
        assertEquals(3, e.power(watcher))
    }

    @Test fun summonedUnitTriggersSameRowSustainedBonus() {
        val e = engine()
        val grenade = e.catalog.getValue("Grenade Dreg")
        e.human.hand.add(grenade)
        assertTrue(e.play(e.human, 0, 0, 0, null))
        val watcher = e.human.board[0][0]!!
        assertEquals(2, e.power(watcher))
        e.spawnAt(e.human, 0, 1, "Dreg")
        assertEquals(3, e.power(watcher))
    }

    @Test fun suppressedGrenadeDregDoesNotReactToDeployments() {
        val e = engine()
        e.human.hand.add(e.catalog.getValue("Grenade Dreg"))
        assertTrue(e.play(e.human, 0, 0, 0, null))
        val watcher = e.human.board[0][0]!!
        e.suppress(watcher)
        val powerAfterSuppression = e.power(watcher)
        e.spawnAt(e.human, 0, 1, "Dreg")
        assertEquals(powerAfterSuppression, e.power(watcher))
    }

    @Test fun servitorAuraAppliesToAlliedDamage() {
        val e = engine()
        val servitor = ShadowEngine.Unit(e.human, e.catalog.getValue("Servitor"), 0, 0)
        val vandal = ShadowEngine.Unit(e.human, e.catalog.getValue("Vandal"), 0, 1)
        e.human.board[0][0] = servitor
        e.human.board[0][1] = vandal
        assertEquals(1, e.effects.extraDamage(vandal))
        servitor.suppressed = true
        assertEquals(0, e.effects.extraDamage(vandal))
    }

    @Test fun vandalMayTargetOnlyEnemyFrontRow() {
        val e = engine()
        val front = ShadowEngine.Unit(e.cpu, e.catalog.getValue("Dreg"), 1, 0)
        val back = ShadowEngine.Unit(e.cpu, e.catalog.getValue("Dreg"), 0, 1)
        e.cpu.board[1][0] = front
        e.cpu.board[0][1] = back
        val choices = e.targetOptions(e.human, e.catalog.getValue("Vandal"), 0, 0)
        assertEquals(1, choices.size)
        assertEquals(1, choices[0].row)
        assertEquals(0, choices[0].column)
        assertFalse(choices.any { it.row == 0 })
    }
}
