package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test

/**
 * Batch 11: card-detail recording is the source of printed mechanics.
 * Local JVM regressions; GitHub Actions are not needed.
 */
class ShadowBatch11VideoRulesTest {
    private fun card(name: String, faction: String, rarity: String, power: Int, cost: Int,
                     effect: String) = ShadowData.Card().apply {
        this.name = name
        this.faction = faction
        this.rarity = rarity
        this.power = power.toString()
        this.cost = cost.toString()
        this.effect = effect
    }

    private fun engine(): ShadowEngine {
        val cards = listOf(
            card("Tomb Ship", "Hive", "Exotic", 6, 4,
                 "When Played: Summons 2 random Hive units to its left and right."),
            card("Saladin", "Humans", "Exotic", 6, 4,
                 "Sustained: Whenever one of your Human units takes damage, they gain +3 Power."),
            card("Radegast", "Humans", "Mythic", 7, 4,
                 "When Played: If Fallen units are on opponent's field, grants all Guerrillas in this row +4 Power."),
            card("Bazinet", "Humans", "Mythic", 2, 4,
                 "When Played (Back Row): Deals 3 damage to all opposing units with a bonus."),
            card("Elite Walker", "Fallen", "Mythic", 2, 4,
                 "Sustained (Back Row): When your turn ends, deals 2 damage to 2 random opposing units."),
            card("Resilient Marauder", "Fallen", "Mythic", 6, 4,
                 "When Played (Front Row): Deals 5 damage to an opposing unit."),
            card("Luksis-3", "Fallen", "Mythic", 5, 4,
                 "When Played: Grants Power equal to 2x the number of Rocks on your opponent's field."),
            card("Tenacious Knight", "Hive", "Mythic", 6, 4,
                 "When Played (Front Row): Randomly grants +3 Power to 2 of your Inspired Hive units."),
            card("Thrall", "Hive", "Rare", 10, 1, "No special effects."),
            card("Inquisitor", "Hive", "Legendary", 10, 3,
                 "When Played (Back Row): Deals 4 damage to an opposing unit."),
            card("Guerrilla", "Humans", "Rare", 10, 1, "No special effects."),
            card("Dreg", "Fallen", "Rare", 10, 1, "No special effects."),
            card("Rock", "Humans", "Rare", 1, 1, "No special effects."),
            card("Rock Pile", "Humans", "Rare", 10, 1, "No special effects.")
        )
        return ShadowEngine(cards.associateBy { it.name }, 17L).apply {
            humanTurn = true
            humanFirst = true
            human.points = 80
        }
    }

    private fun place(e: ShadowEngine, name: String, row: Int, col: Int,
                      owner: ShadowEngine.Player = e.human): ShadowEngine.Unit {
        val u = ShadowEngine.Unit(owner, e.catalog.getValue(name), row, col)
        owner.board[row][col] = u
        return u
    }

    private fun play(e: ShadowEngine, name: String, row: Int, col: Int,
                     target: ShadowEngine.Target? = null): ShadowEngine.Unit {
        e.human.hand.add(e.catalog.getValue(name))
        assertTrue(e.lastActionError, e.play(e.human, e.human.hand.lastIndex, row, col, target))
        return e.human.board[row][col]!!
    }

    @Test fun allEightVideoCardsHavePowerAndLightCost() {
        val e = engine()
        val stats = mapOf(
            "Tomb Ship" to Pair(6, 4),
            "Saladin" to Pair(6, 4),
            "Radegast" to Pair(7, 4),
            "Bazinet" to Pair(2, 4),
            "Elite Walker" to Pair(2, 4),
            "Resilient Marauder" to Pair(6, 4),
            "Luksis-3" to Pair(5, 4),
            "Tenacious Knight" to Pair(6, 4)
        )
        for ((name, values) in stats) {
            val card = e.catalog.getValue(name)
            assertEquals(name, values.first, card.numericPower())
            assertEquals(name, values.second, card.numericCost())
            assertTrue(name, card.isPlayable)
        }
    }

    @Test fun tombShipSummonsOneRandomHiveUnitOnEachAvailableSide() {
        val e = engine()
        play(e, "Tomb Ship", 0, 2)
        for (col in listOf(1, 3)) {
            val summoned = e.human.board[0][col]
            assertNotNull("Expected random Hive at column $col", summoned)
            assertEquals("Hive", summoned!!.card.faction)
        }
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun tombShipLeavesOccupiedNeighborUntouchedAndSummonsOnlyInFreeSlot() {
        val e = engine()
        val occupied = place(e, "Dreg", 0, 1)
        play(e, "Tomb Ship", 0, 2)
        assertSame(occupied, e.human.board[0][1])
        assertEquals("Hive", e.human.board[0][3]!!.card.faction)
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun saladinGrantsBonusToDamagedHumanNotUnrelatedFallen() {
        val e = engine()
        place(e, "Saladin", 0, 2)
        val human = place(e, "Guerrilla", 0, 1)
        val fallen = place(e, "Dreg", 1, 2)
        val attacker = place(e, "Dreg", 1, 3, e.cpu)
        e.damage(attacker, human, 1)
        e.damage(attacker, fallen, 1)
        assertEquals(12, human.power)
        assertEquals(9, fallen.power)
    }

    @Test fun radegastBuffsAllGuerrillasInItsRowOnlyIfEnemyFallenPresent() {
        val e = engine()
        val sameRow = place(e, "Guerrilla", 0, 1)
        val anotherRow = place(e, "Guerrilla", 1, 0)
        place(e, "Dreg", 1, 2, e.cpu)
        play(e, "Radegast", 0, 2)
        assertEquals(14, sameRow.power)
        assertEquals(10, anotherRow.power)
    }

    @Test fun bazinetFromBackRowDamagesEveryBuffedEnemyOnly() {
        val e = engine()
        val buffed = place(e, "Dreg", 0, 0, e.cpu)
        val normal = place(e, "Dreg", 1, 2, e.cpu)
        e.buff(buffed, 2)
        play(e, "Bazinet", 0, 2)
        assertEquals(9, buffed.power)
        assertEquals(10, normal.power)
    }

    @Test fun eliteWalkerOnlyFiresFromBackRowAndHitsTwoDistinctTargets() {
        val e = engine()
        place(e, "Elite Walker", 0, 2)
        val a = place(e, "Dreg", 0, 0, e.cpu)
        val b = place(e, "Dreg", 1, 1, e.cpu)
        e.effects.endSide(e.human)
        assertEquals(8, a.power)
        assertEquals(8, b.power)

        val front = engine()
        place(front, "Elite Walker", 1, 2)
        val protected = place(front, "Dreg", 1, 0, front.cpu)
        front.effects.endSide(front.human)
        assertEquals(10, protected.power)
    }

    @Test fun resilientMarauderFrontRowDealsFiveToSelectedEnemy() {
        val e = engine()
        val enemy = place(e, "Dreg", 0, 0, e.cpu)
        play(e, "Resilient Marauder", 1, 2, ShadowEngine.Target(e.cpu, 0, 0))
        assertEquals(5, enemy.power)
    }

    @Test fun luksisCountsOnlyRockAndNotRockPile() {
        val e = engine()
        place(e, "Rock", 1, 0, e.cpu)
        place(e, "Rock Pile", 1, 1, e.cpu)
        val luksis = play(e, "Luksis-3", 0, 2)
        assertEquals(7, luksis.power)
    }

    @Test fun tenaciousKnightBuffsTwoDifferentInspiredHiveUnitsOnlyFromFrontRow() {
        val e = engine()
        val a = place(e, "Thrall", 0, 0)
        val b = place(e, "Thrall", 0, 1)
        val c = place(e, "Thrall", 1, 4)
        a.inspired = true; b.inspired = true; c.inspired = true
        val neutral = place(e, "Dreg", 0, 3)
        play(e, "Tenacious Knight", 1, 2)
        assertEquals(2, listOf(a, b, c).count { it.power == 13 })
        assertEquals(10, neutral.power)

        val back = engine()
        val inspired = place(back, "Thrall", 1, 0)
        inspired.inspired = true
        play(back, "Tenacious Knight", 0, 2)
        assertEquals(10, inspired.power)
    }
}
