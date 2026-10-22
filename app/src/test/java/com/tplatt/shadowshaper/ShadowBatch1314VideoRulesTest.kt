package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test
import java.util.Random

/** Original in-game Batch 13–14 card-viewer rules. Run locally, no GitHub Actions. */
class ShadowBatch1314VideoRulesTest {
    private fun card(n: String, faction: String, rarity: String, power: Int,
                     light: Int, effect: String) = ShadowData.Card().apply {
        name = n
        this.faction = faction
        this.rarity = rarity
        this.power = power.toString()
        cost = light.toString()
        this.effect = effect
    }

    private fun engine(seed: Long = 0L): ShadowEngine {
        val cards = listOf(
            card("Thinktank", "Humans", "Mythic", 7, 5, "When Played: Deals damage to a unit equal to the highest bonus on your field."),
            card("High Servitor", "Fallen", "Mythic", 7, 5, "When Played: Deals damage to a unit equal to the number of Fallen units on your field."),
            card("Shinka Gladiator", "Cabal", "Mythic", 8, 5, "When Played (5-Unit Line): A selected unit and the units to its left and right deal 2 additional damage."),
            card("Molkrul", "Fallen", "Mythic", 8, 5, "When Played (Back Row): Toss a coin. If the result is heads, instantly defeats an opposing unit with no more than 10 Power."),
            card("Lady Skorri", "Humans", "Legendary", 7, 5, "When Played: If there are units in the opponent's back row, grants +5 Power to the unit on the left."),
            card("Amu-Atu", "Humans", "Legendary", 8, 5, "When Played: Deals damage to an opposing unit equal to the number of units in the opponent's back row."),
            card("Augmented Shank", "Fallen", "Legendary", 5, 5, "When Played (Front Row): Deals 2 damage to a unit and the units on its left and right."),
            card("Shinka Centurion", "Cabal", "Legendary", 6, 5, "Sustained (Column): Grants +3 Power to units on the left and right."),
            card("Maru", "Humans", "Exotic", 13, 6, "When Played: Grants all units in this row (except for this unit) +2 Power."),
            card("Kabr", "Humans", "Exotic", 7, 6, "When Played: Grants all Guerrilla units +3 Power."),
            card("Ikora", "Humans", "Exotic", 13, 6, "When Played: Deals 2 damage to all opposing units in a row."),
            card("Attal", "Humans", "Exotic", 6, 6, "When Played: Doubles the Power of a unit."),
            card("Estela", "Humans", "Exotic", 13, 6, "When Played: Grants a unit Power equal to 2x the number of units in the opponent's front row."),
            card("Vanok", "Fallen", "Exotic", 10, 6, "When Played: Grants +2 Power to all your Fallen on the field."),
            card("Paradoxical Neophyte", "Hive", "Exotic", 10, 6, "When Played: Summons 1 Shade on the left."),
            card("Paradoxical Neophyte's Shade", "Hive", "Exotic", 1, 6, "Sustained: Gains 4 Power for every Inspired Hive on your field.").apply {
                classification = "Summoned unit"
            },
            card("Goliath Tank", "Cabal", "Exotic", 10, 6, "When Played (5-Unit Line): Deals 3 damage to all units in a row."),
            card("Buhr", "Cabal", "Exotic", 10, 6, "When Played (Column): Grants +3 Power to all units in a Column."),
            card("Thagrius", "Cabal", "Exotic", 10, 6, "When Played (6-Unit Line): Deals damage to a unit equal to double the number of Line units."),
            card("Sabikis", "Fallen", "Mythic", 8, 6, "When Played (Back Row): Grants a unit the Power of the unit to its left."),
            card("Legionary", "Cabal", "Rare", 10, 1, "No special effects."),
            card("Guerrilla", "Humans", "Rare", 10, 1, "No special effects."),
            card("Dreg", "Fallen", "Rare", 10, 1, "No special effects."),
            card("Thrall", "Hive", "Rare", 10, 1, "No special effects."),
            card("Tracker", "Fallen", "Rare", 30, 2, "No special effects.")
        ).associateBy { it.name }
        return ShadowEngine(cards, seed).apply {
            humanTurn = true
            humanFirst = true
            human.points = 80
        }
    }

    private fun put(e: ShadowEngine, name: String, row: Int, column: Int,
                    owner: ShadowEngine.Player = e.human): ShadowEngine.Unit {
        val u = ShadowEngine.Unit(owner, e.catalog.getValue(name), row, column)
        owner.board[row][column] = u
        return u
    }

    private fun play(e: ShadowEngine, name: String, row: Int, column: Int,
                     target: ShadowEngine.Target? = null): ShadowEngine.Unit {
        e.human.hand.add(e.catalog.getValue(name))
        assertTrue(e.lastActionError, e.play(e.human, e.human.hand.lastIndex, row, column, target))
        return e.human.board[row][column]!!
    }

    @Test fun batch13And14StatsAndShadeClassification() {
        val e = engine()
        val stats = mapOf(
            "Thinktank" to Pair(7,5), "High Servitor" to Pair(7,5),
            "Shinka Gladiator" to Pair(8,5), "Molkrul" to Pair(8,5),
            "Lady Skorri" to Pair(7,5), "Amu-Atu" to Pair(8,5),
            "Augmented Shank" to Pair(5,5), "Shinka Centurion" to Pair(6,5),
            "Maru" to Pair(13,6), "Kabr" to Pair(7,6),
            "Ikora" to Pair(13,6), "Attal" to Pair(6,6),
            "Estela" to Pair(13,6), "Vanok" to Pair(10,6),
            "Paradoxical Neophyte" to Pair(10,6),
            "Paradoxical Neophyte's Shade" to Pair(1,6),
            "Goliath Tank" to Pair(10,6), "Buhr" to Pair(10,6),
            "Thagrius" to Pair(10,6), "Sabikis" to Pair(8,6)
        )
        for ((name, powerCost) in stats) {
            assertEquals(name, powerCost.first, e.catalog.getValue(name).numericPower())
            assertEquals(name, powerCost.second, e.catalog.getValue(name).numericCost())
            assertEquals(name, name != "Paradoxical Neophyte's Shade", e.catalog.getValue(name).isPlayable)
        }
    }

    @Test fun maruBuffsAllOtherUnitsOnlyInItsRow() {
        val e = engine()
        val ally1 = put(e, "Guerrilla", 0, 1)
        val ally2 = put(e, "Guerrilla", 0, 3)
        val otherRow = put(e, "Guerrilla", 1, 2)
        val maru = play(e, "Maru", 0, 2)
        assertEquals(12, ally1.power)
        assertEquals(12, ally2.power)
        assertEquals(10, otherRow.power)
        assertEquals(13, maru.power)
    }

    @Test fun molkrulTargetsOnlyTenOrLowerAndCoinFlipGovernsDestruction() {
        val e = engine(0L)
        put(e, "Tracker", 1, 0, e.cpu)
        val eligible = put(e, "Dreg", 0, 1, e.cpu)
        val choices = e.targetOptions(e.human, e.catalog.getValue("Molkrul"), 0, 2)
        assertEquals(1, choices.size)
        assertEquals(1, choices[0].column)
        assertTrue(Random(0L).nextBoolean())
        play(e, "Molkrul", 0, 2, ShadowEngine.Target(e.cpu, 0, 1))
        assertNull(e.cpu.board[0][1])
        assertEquals(30, e.cpu.board[1][0]!!.power)
        assertFalse(e.catalog.getValue("Molkrul").effect.isBlank())
        assertNotNull(eligible)
    }

    @Test fun shinkaGladiatorAddsOutgoingDamageWithoutChangingPower() {
        val e = engine()
        for (c in listOf(0,1,3,4)) put(e, "Legionary", 0, c)
        val left = put(e, "Legionary", 1, 0)
        val selected = put(e, "Legionary", 1, 1)
        val right = put(e, "Legionary", 1, 2)
        val actor = play(e, "Shinka Gladiator", 0, 2, ShadowEngine.Target(e.human, 1, 1))
        assertEquals(2, selected.damageBonus)
        assertEquals(2, left.damageBonus)
        assertEquals(2, right.damageBonus)
        assertEquals(10, selected.power)
        assertEquals(8, actor.power)
        val enemy = put(e, "Tracker", 1, 1, e.cpu)
        e.damage(selected, enemy, 1)
        assertEquals(27, enemy.power)
    }

    @Test fun shinkaCenturionRequiresVerticalColumnPartner() {
        val e = engine()
        put(e, "Shinka Centurion", 0, 2)
        val left = put(e, "Legionary", 0, 1)
        assertEquals(10, e.power(left))
        put(e, "Legionary", 1, 2)
        assertEquals(13, e.power(left))
    }

    @Test fun neophyteSummonsUndeckableShadeAndShadeScalesWithInspiredHive() {
        val e = engine()
        val neophyte = play(e, "Paradoxical Neophyte", 0, 2)
        val shade = e.human.board[0][1]!!
        assertEquals("Paradoxical Neophyte's Shade", shade.card.name)
        assertFalse(shade.card.isPlayable)
        assertEquals(1, e.power(shade))
        val h = put(e, "Thrall", 1, 1)
        h.inspired = true
        assertEquals(5, e.power(shade))
        h.suppressed = true
        assertEquals(1, e.power(shade))
        assertNotNull(neophyte)
    }

    @Test fun buhrColumnBuffIncludesSelfAndPartner() {
        val e = engine()
        val partner = put(e, "Legionary", 1, 2)
        val buhr = play(e, "Buhr", 0, 2)
        assertEquals(13, buhr.power)
        assertEquals(13, partner.power)
    }

    @Test fun thagriusDealsTwiceLineLengthToOneEnemy() {
        val e = engine()
        for (c in listOf(0,1,2,4,5)) put(e, "Legionary", 0, c)
        val foe = put(e, "Tracker", 0, 2, e.cpu)
        play(e, "Thagrius", 0, 3, ShadowEngine.Target(e.cpu, 0, 2))
        assertEquals(18, foe.power)
    }

    @Test fun sabikisBackRowUsesPowerOfUnitsImmediateLeft() {
        val e = engine()
        put(e, "Guerrilla", 0, 0)
        val chosen = put(e, "Dreg", 0, 1)
        play(e, "Sabikis", 0, 4, ShadowEngine.Target(e.human, 0, 1))
        assertEquals(20, chosen.power)
    }

    @Test fun attalDoublesPowerAndKabrBuffsAllGuerrillas() {
        val e = engine()
        val target = put(e, "Guerrilla", 0, 0)
        play(e, "Attal", 0, 2, ShadowEngine.Target(e.human, 0, 0))
        assertEquals(20, target.power)
        val other = put(e, "Guerrilla", 1, 1)
        play(e, "Kabr", 1, 2)
        assertEquals(23, target.power)
        assertEquals(13, other.power)
    }

    @Test fun vanokBuffsFallenIncludingItself() {
        val e = engine()
        val ally = put(e, "Dreg", 1, 2)
        val vanok = play(e, "Vanok", 0, 2)
        assertEquals(12, ally.power)
        assertEquals(12, vanok.power)
    }
}
