package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test

/**
 * Every incoming Scorched source maintains its own damage, lifetime and
 * source attribution. Executed locally with run_local_engine_checks.bat.
 */
class ShadowStackedScorchTest {
    private fun card(name: String, faction: String, power: Int, effect: String) =
        ShadowData.Card().apply {
            this.name = name
            this.faction = faction
            this.power = power.toString()
            cost = "1"
            rarity = "Rare"
            this.effect = effect
        }

    private fun engine(): ShadowEngine {
        val cards = listOf(
            card("Ayuela", "Humans", 6,
                "Sustained (Back Row): When opponent places a unit in Front Row, Scorches it for 2 turns."),
            card("Resilient Grenade Dreg", "Fallen", 7,
                "Sustained (Back Row): When opponent places a Back Row unit, deals 2 damage for 2 turns."),
            card("Dreg", "Fallen", 12, "No special effects."),
            card("Human Guard", "Humans", 12, "No special effects."),
            card("Saladin", "Humans", 6,
                "Sustained: Each Human ally damaged gains +3 Power."),
            card("Elite Sniper Vandal", "Fallen", 6,
                "Sustained (Back Row): Damages a bonus-gaining opposing unit once per turn.")
        ).associateBy { it.name }
        return ShadowEngine(cards, 84L)
    }

    private fun put(
        e: ShadowEngine, name: String, side: ShadowEngine.Player,
        row: Int, col: Int, power: Int? = null
    ) = ShadowEngine.Unit(side, e.catalog.getValue(name), row, col).also {
        if (power != null) it.power = power
        side.board[row][col] = it
    }

    @Test fun twoAyuelasEachApplyTwoTurnsOfIndependentScorch() {
        val e = engine()
        put(e, "Ayuela", e.human, 0, 0)
        put(e, "Ayuela", e.human, 0, 1)
        assertTrue(e.spawnAt(e.cpu, 1, 4, "Dreg"))
        val dreg = e.cpu.board[1][4]!!

        assertEquals(2, dreg.scorchEffects.size)
        assertEquals(4, dreg.scorched)
        assertEquals(2, dreg.scorchTurns)
        assertNull(dreg.scorchSource)

        e.effects.endSide(e.cpu)
        assertEquals(8, dreg.power)
        assertEquals(2, dreg.scorchEffects.size)
        assertEquals(1, dreg.scorchTurns)

        e.effects.endSide(e.cpu)
        assertEquals(4, dreg.power)
        assertTrue(dreg.scorchEffects.isEmpty())
        assertEquals(0, dreg.scorched)
        assertEquals(0, dreg.scorchTurns)
    }

    @Test fun twoResilientGrenadeDregsStackTheirBackRowScorch() {
        val e = engine()
        put(e, "Resilient Grenade Dreg", e.cpu, 0, 0)
        put(e, "Resilient Grenade Dreg", e.cpu, 0, 1)
        assertTrue(e.spawnAt(e.human, 0, 4, "Human Guard"))
        val guard = e.human.board[0][4]!!

        assertEquals(2, guard.scorchEffects.size)
        assertEquals(4, guard.scorched)
        e.effects.endSide(e.human)
        assertEquals(8, guard.power)
        e.effects.endSide(e.human)
        assertEquals(4, guard.power)
        assertTrue(guard.scorchEffects.isEmpty())
    }

    @Test fun removedScorchSourceDoesNotCancelAnAlreadyAppliedStatus() {
        val e = engine()
        val source = put(e, "Ayuela", e.human, 0, 0)
        assertTrue(e.spawnAt(e.cpu, 1, 4, "Dreg"))
        val target = e.cpu.board[1][4]!!
        assertSame(source, target.scorchSource)

        e.human.board[0][0] = null
        e.effects.endSide(e.cpu)
        assertEquals(10, target.power)
        e.effects.endSide(e.cpu)
        assertEquals(8, target.power)
        assertEquals(0, target.scorchTurns)
    }

    @Test fun suppressionClearsAllPendingScorchInstances() {
        val e = engine()
        put(e, "Ayuela", e.human, 0, 0)
        put(e, "Ayuela", e.human, 0, 1)
        assertTrue(e.spawnAt(e.cpu, 1, 4, "Dreg"))
        val target = e.cpu.board[1][4]!!

        e.suppress(target)
        assertTrue(target.suppressed)
        assertTrue(target.scorchEffects.isEmpty())
        assertEquals(0, target.scorched)
        assertEquals(0, target.scorchTurns)
        e.effects.endSide(e.cpu)
        assertEquals(12, target.power)
    }

    @Test fun staggeredScorchExpirationsKeepTheLaterSource() {
        val e = engine()
        val first = put(e, "Ayuela", e.human, 0, 0)
        val second = put(e, "Ayuela", e.human, 0, 1)
        val target = put(e, "Dreg", e.cpu, 1, 4)

        e.addScorch(target, first, 2, 2)
        e.effects.endSide(e.cpu)
        assertEquals(10, target.power)
        assertEquals(1, target.scorchTurns)

        e.addScorch(target, second, 2, 2)
        assertEquals(2, target.scorchEffects.size)
        assertEquals(4, target.scorched)
        e.effects.endSide(e.cpu)
        assertEquals(6, target.power)
        assertEquals(1, target.scorchEffects.size)
        assertSame(second, target.scorchSource)
        assertEquals(1, target.scorchTurns)
        assertEquals(2, target.scorched)

        e.effects.endSide(e.cpu)
        assertEquals(4, target.power)
        assertTrue(target.scorchEffects.isEmpty())
    }

    @Test fun firstLethalScorchTickPreventsSecondTickFromTargetingCorpse() {
        val e = engine()
        put(e, "Ayuela", e.human, 0, 0)
        put(e, "Ayuela", e.human, 0, 1)
        val dying = put(e, "Dreg", e.cpu, 1, 4, power = 2)
        e.effects.fireEnemyPlacement(dying)

        e.effects.endSide(e.cpu)
        assertNull(e.cpu.board[1][4])
        assertEquals(1, e.journal.count { it == "DAMAGE 2 ▶ Dreg" })
        assertEquals(1, e.journal.count { it == "DESTROYED ▶ Dreg" })
    }

    @Test fun scorchHitsTriggerSaladinAndSniperAsSeparateDamageEvents() {
        val e = engine()
        put(e, "Resilient Grenade Dreg", e.cpu, 0, 0)
        put(e, "Resilient Grenade Dreg", e.cpu, 0, 1)
        val sniper = put(e, "Elite Sniper Vandal", e.cpu, 0, 2)
        put(e, "Saladin", e.human, 1, 1)
        assertTrue(e.spawnAt(e.human, 0, 4, "Human Guard"))
        val guard = e.human.board[0][4]!!

        e.effects.endSide(e.human)

        // Two 2-damage Scorched hits grant two +3 Saladin buffs;
        // one sniper responds to the first bonus, causing another +3.
        assertEquals(16, guard.power)
        assertEquals(3, e.journal.count { it == "BUFF +3 ▶ Human Guard" })
        assertEquals(2, e.journal.count { it == "DAMAGE 2 ▶ Human Guard" })
        assertEquals(1, e.journal.count { it == "DAMAGE 1 ▶ Human Guard" })
        assertEquals(0, e.unresolvedEffects)
        assertEquals(1, sniper.sniperLastHitTurn.size)
    }
}
