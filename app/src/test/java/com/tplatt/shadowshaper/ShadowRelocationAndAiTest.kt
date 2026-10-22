package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test

/** Board-movement and computer-placement regression scenarios.
 *  Run locally with run_local_engine_checks.bat (no GitHub Actions). */
class ShadowRelocationAndAiTest {
    private fun card(name: String, faction: String, power: Int, cost: Int, effect: String) =
        ShadowData.Card().apply {
            this.name = name
            this.faction = faction
            this.rarity = "Legendary"
            this.power = power.toString()
            this.cost = cost.toString()
            this.effect = effect
        }

    private fun engine(): ShadowEngine {
        val cards = listOf(
            card("Xuan Wei", "Humans", 6, 3,
                "When Played (Front Row): Deals 5 damage to a unit and moves it."),
            card("Inquisitor", "Hive", 3, 3,
                "When Played (Back Row): Deals 4 damage to an opposing unit."),
            card("Tracker", "Fallen", 30, 1, "No special effects."),
            card("Dreg", "Fallen", 10, 1, "No special effects.")
        ).associateBy { it.name }
        return ShadowEngine(cards, 91L).apply {
            humanTurn = true
            humanFirst = true
            human.points = 80
            cpu.points = 80
        }
    }

    private fun put(e: ShadowEngine, name: String, owner: ShadowEngine.Player,
                    row: Int, col: Int): ShadowEngine.Unit {
        val u = ShadowEngine.Unit(owner, e.catalog.getValue(name), row, col)
        owner.board[row][col] = u
        return u
    }

    private fun playXuan(e: ShadowEngine, caster: ShadowEngine.Player,
                         target: ShadowEngine.Target): Boolean {
        caster.hand.add(e.catalog.getValue("Xuan Wei"))
        return e.play(caster, caster.hand.lastIndex, ShadowRules.FRONT_ROW, 2, target)
    }

    @Test fun fullBoardAfterTargetSelectionDoesNotLockTheHumanTurn() {
        val e = engine()
        val victim = put(e, "Tracker", e.cpu, 0, 0)
        assertTrue(playXuan(e, e.human, ShadowEngine.Target(e.cpu, 0, 0)))
        assertSame(victim, e.pendingMovedUnit)

        for (row in 0 until ShadowRules.ROWS) {
            for (col in 0 until ShadowRules.COLUMNS) {
                if (e.cpu.board[row][col] == null) put(e, "Dreg", e.cpu, row, col)
            }
        }
        e.endTurn()
        assertNull(e.pendingMovedUnit)
        assertNull(e.pendingMoveCaster)
        assertFalse(e.humanTurn)
        assertTrue(e.journal.any { it.contains("RELOCATION CANCELLED") })
        assertSame(victim, e.cpu.board[0][0])
    }

    @Test fun destroyedPendingTargetDoesNotPreventEndTurn() {
        val e = engine()
        val victim = put(e, "Tracker", e.cpu, 0, 0)
        assertTrue(playXuan(e, e.human, ShadowEngine.Target(e.cpu, 0, 0)))
        val source = e.human.board[ShadowRules.FRONT_ROW][2]!!
        e.dealDirect(source, victim, 100)
        e.sweep()
        assertNull(e.cpu.board[0][0])
        e.endTurn()
        assertNull(e.pendingMovedUnit)
        assertNull(e.pendingMoveCaster)
        assertFalse(e.humanTurn)
    }

    @Test fun rejectedDestinationKeepsThePendingChoice() {
        val e = engine()
        val victim = put(e, "Tracker", e.cpu, 0, 0)
        put(e, "Dreg", e.cpu, 0, 1)
        assertTrue(playXuan(e, e.human, ShadowEngine.Target(e.cpu, 0, 0)))
        assertFalse(e.resolveMove(e.human, 0, 1))
        assertSame(victim, e.pendingMovedUnit)
        assertSame(victim, e.cpu.board[0][0])
        assertTrue(e.resolveMove(e.human, 0, 5))
        assertSame(victim, e.cpu.board[0][5])
        assertNull(e.pendingMovedUnit)
    }

    @Test fun cpuAutomaticallyRelocatesWhenItPlaysXuanWei() {
        val e = engine()
        e.humanTurn = false
        val victim = put(e, "Tracker", e.human, 0, 0)
        assertTrue(playXuan(e, e.cpu, ShadowEngine.Target(e.human, 0, 0)))
        assertNull(e.pendingMovedUnit)
        assertEquals(25, victim.power)
        assertEquals(1, e.human.board.flatMap { it.toList() }.count { it === victim })
        assertFalse(e.human.board[0][0] === victim)
    }

    @Test fun aiPlacesBackRowCardWhereItsTargetedAbilityCanResolve() {
        val e = engine()
        e.humanTurn = false
        val victim = put(e, "Dreg", e.human, ShadowRules.FRONT_ROW, 0)
        e.cpu.hand.add(e.catalog.getValue("Inquisitor"))
        assertTrue(e.cpuPlayOne())
        assertEquals(6, victim.power)
        assertEquals(1, e.cpu.board[ShadowRules.BACK_ROW].filterNotNull()
            .count { it.card.name == "Inquisitor" })
        assertEquals(0, e.cpu.board[ShadowRules.FRONT_ROW].filterNotNull()
            .count { it.card.name == "Inquisitor" })
    }

    @Test fun aiCanStillPlayTargetedCardWithoutTargetsForBoardPower() {
        val e = engine()
        e.humanTurn = false
        e.cpu.hand.add(e.catalog.getValue("Inquisitor"))
        assertTrue(e.cpuPlayOne())
        assertEquals(1, e.cpu.board.flatMap { it.toList() }
            .filterNotNull().count { it.card.name == "Inquisitor" })
        assertEquals(0, e.cpu.points)
    }
}
