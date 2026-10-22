package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test

/**
 * Rules confirmed from user card-detail recordings for Batches 01–08.
 * Runs with the local Gradle JVM task; does not require GitHub Actions.
 */
class ShadowBatch0108VideoRulesTest {
    private fun card(name: String, faction: String, power: Int, cost: Int, effect: String) =
        ShadowData.Card().apply {
            this.name = name
            this.faction = faction
            this.power = power.toString()
            this.cost = cost.toString()
            this.effect = effect
            this.rarity = "Legendary"
        }

    private fun engine(): ShadowEngine {
        val cards = listOf(
            card("Rossi-11", "Humans", 3, 1, "When Played: Grants an overshield to one of your units."),
            card("Mind Disruptor", "Fallen", 2, 1, "Sustained: The unit on its left cannot be Suppressed."),
            card("Resilient Captain", "Fallen", 1, 1, "Sustained: When your Fallen units deal damage, summons a Dreg to its right (maximum 6)."),
            card("Unrelenting Captain", "Fallen", 1, 1, "Sustained: Each Dreg deployed gains +1 Power."),
            card("Exploder Shank", "Fallen", 1, 1, "When Played: Moves to opponent's front row and detonates on your next turn."),
            card("Dreg", "Fallen", 1, 1, "No special effects."),
            card("Tracker", "Fallen", 7, 2, "No special effects."),
            card("Thrall", "Hive", 2, 1, "No special effects."),
            card("Saladin", "Humans", 6, 4, "Sustained: When Human unit takes damage, grants +3 Power."),
            card("Finnala", "Humans", 6, 3, "When Played (Front Row): Grants +8 Power to one of your units and gains -3 Power."),
            card("Shinka Psion", "Cabal", 3, 2, "When Played (2-Unit Line): Deals 3 damage to a unit."),
            card("Shinka Legionary", "Cabal", 4, 2, "Sustained (Column): Gains +2 Power."),
            card("Shinka Phalanx", "Cabal", 3, 2, "Sustained (Column): Units directly in front or behind take 1 less damage."),
            card("Incendior", "Cabal", 2, 2, "When Played: Deals 3 damage to an opposing unit in the front row."),
            card("Decurion", "Cabal", 3, 2, "Sustained (Column): Grants +2 Power to units directly in front or behind."),
            card("Psion", "Cabal", 2, 2, "When Played (Back Row): Deals 2 damage to a unit."),
            card("Jolder", "Humans", 3, 3, "Sustained: The next instance of damage dealt is doubled."),
            card("Legionary", "Cabal", 2, 1, "No special effects.")
        )
        return ShadowEngine(cards.associateBy { it.name }, 21L).apply {
            humanTurn = true
            humanFirst = true
            human.points = 40
        }
    }

    private fun set(e: ShadowEngine, name: String, row: Int, col: Int,
                    side: ShadowEngine.Player = e.human): ShadowEngine.Unit {
        val u = ShadowEngine.Unit(side, e.catalog.getValue(name), row, col)
        side.board[row][col] = u
        return u
    }

    private fun play(e: ShadowEngine, name: String, row: Int, col: Int,
                     target: ShadowEngine.Target? = null): ShadowEngine.Unit? {
        e.human.hand.add(e.catalog.getValue(name))
        val index = e.human.hand.lastIndex
        assertTrue(e.lastActionError, e.play(e.human, index, row, col, target))
        return e.human.board[row][col]
    }

    @Test fun rossiShieldBlocksDamageSuppressionAndLethalOnce() {
        val e = engine()
        val ally = set(e, "Tracker", 0, 0)
        play(e, "Rossi-11", 0, 2, ShadowEngine.Target(e.human, 0, 0))
        assertTrue(ally.overshield)
        val attacker = set(e, "Thrall", 1, 1, e.cpu)
        e.damage(attacker, ally, 99)
        assertEquals(7, ally.power)
        assertFalse(ally.overshield)
        e.suppress(ally)
        assertTrue(ally.suppressed)
        ally.suppressed = false
        ally.overshield = true
        e.destroyUnit(ally)
        assertNotNull(e.human.board[0][0])
        assertFalse(ally.overshield)
    }

    @Test fun mindDisruptorProtectsImmediateLeftOnlyUntilSuppressed() {
        val e = engine()
        val left = set(e, "Tracker", 0, 1)
        val disruptor = set(e, "Mind Disruptor", 0, 2)
        val otherRow = set(e, "Thrall", 1, 2)
        e.suppress(left)
        assertFalse(left.suppressed)
        e.suppress(otherRow)
        assertTrue(otherRow.suppressed)
        disruptor.suppressed = true
        e.suppress(left)
        assertTrue(left.suppressed)
    }

    @Test fun unrelentingCaptainGivesNewDregBonusWithoutBeingBuffed() {
        val e = engine()
        val captain = play(e, "Unrelenting Captain", 0, 1)!!
        e.spawnAt(e.human, 0, 2, "Dreg")
        assertEquals(2, e.human.board[0][2]!!.power)
        assertEquals(1, captain.power)
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun resilientCaptainSummonsAfterFallenDamageNotOnPlay() {
        val e = engine()
        val captain = play(e, "Resilient Captain", 0, 1)!!
        assertEquals(1, captain.power)
        assertNull(e.human.board[0][2])
        val fallen = set(e, "Tracker", 1, 1)
        val foe = set(e, "Tracker", 1, 0, e.cpu)
        e.damage(fallen, foe, 1)
        assertEquals("Dreg", e.human.board[0][2]?.card?.name)
        assertEquals(1, captain.resilientCaptainDregs)
        e.damage(fallen, foe, 1)
        assertEquals(1, captain.resilientCaptainDregs)
    }

    @Test fun exploderShankTransfersAndDetonatesForTwoDamageToBothNeighbors() {
        val e = engine()
        for (col in 0..4) {
            if (col != 2) set(e, "Thrall", 1, col, e.cpu)
        }
        play(e, "Exploder Shank", 0, 0)
        assertNull(e.human.board[0][0])
        assertEquals("Exploder Shank", e.cpu.board[1][2]?.card?.name)
        e.endTurn()
        e.endTurn()
        assertNull(e.cpu.board[1][2])
        assertNull(e.cpu.board[1][1])
        assertNull(e.cpu.board[1][3])
    }

    @Test fun batch08FinnalaUsesPowerPenaltyNotDamage() {
        val e = engine()
        set(e, "Saladin", 0, 0)
        val ally = set(e, "Tracker", 0, 1)
        val finnala = play(e, "Finnala", 1, 2, ShadowEngine.Target(e.human, 0, 1))!!
        assertEquals(15, ally.power)
        assertEquals(3, finnala.power)
    }

    @Test fun batch08ShinkaLegionaryGainsTwoOnlyWhenColumnIsPaired() {
        val e = engine()
        val shinka = set(e, "Shinka Legionary", 0, 1)
        assertEquals(4, e.power(shinka))
        set(e, "Legionary", 1, 1)
        assertEquals(6, e.power(shinka))
    }

    @Test fun batch08ShinkaPhalanxProtectsColumnPartnerByOne() {
        val e = engine()
        set(e, "Shinka Phalanx", 0, 1)
        val other = set(e, "Legionary", 1, 1)
        assertEquals(1, e.effects.reduction(other))
        assertEquals(0, e.effects.reduction(set(e, "Legionary", 1, 2)))
    }

    @Test fun batch08DecurionBuffsVerticallyAdjacentUnitByTwo() {
        val e = engine()
        val source = set(e, "Decurion", 0, 1)
        val partner = set(e, "Legionary", 1, 1)
        assertEquals(3, e.power(source))
        assertEquals(4, e.power(partner))
    }

    @Test fun batch08PsionBackRowRequirementAndTargetedTwoDamage() {
        val e = engine()
        val foe = set(e, "Tracker", 1, 0, e.cpu)
        assertFalse(e.activeInRow(e.catalog.getValue("Psion"), 1))
        play(e, "Psion", 0, 2, ShadowEngine.Target(e.cpu, 1, 0))
        assertEquals(5, foe.power)
    }

    @Test fun batch08IncendiorTargetsEnemyFrontRowOnly() {
        val e = engine()
        val target = set(e, "Tracker", 1, 0, e.cpu)
        set(e, "Tracker", 0, 0, e.cpu)
        val candidates = e.targetOptions(e.human, e.catalog.getValue("Incendior"), 0, 2)
        assertEquals(1, candidates.size)
        play(e, "Incendior", 0, 2, ShadowEngine.Target(e.cpu, 1, 0))
        assertEquals(4, target.power)
    }

    @Test fun batch08ShinkaPsionRequiresTwoPrintedLineUnits() {
        val e = engine()
        val target = set(e, "Tracker", 1, 0, e.cpu)
        set(e, "Shinka Psion", 0, 1)
        play(e, "Shinka Psion", 0, 2, ShadowEngine.Target(e.cpu, 1, 0))
        assertEquals(4, target.power)
    }

    @Test fun batch08JolderDoublesOnlyNextDamage() {
        val e = engine()
        val jolder = set(e, "Jolder", 0, 1)
        val source = set(e, "Incendior", 1, 1)
        val foe = set(e, "Tracker", 1, 0, e.cpu)
        e.damage(source, foe, 2)
        assertEquals(3, foe.power)
        assertTrue(jolder.damageDoublerConsumed)
        e.damage(source, foe, 1)
        assertEquals(2, foe.power)
    }
}
