package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test

/** Video-derived Batch 12 mechanics. Run locally without GitHub Actions. */
class ShadowBatch12VideoRulesTest {
    private fun card(n: String, f: String, r: String, p: Int, cost: Int, effect: String) =
        ShadowData.Card().apply {
            name = n; faction = f; rarity = r; power = p.toString()
            this.cost = cost.toString(); this.effect = effect
        }

    private fun engine(seed: Long = 17L): ShadowEngine {
        val catalog = listOf(
            card("Tenacious Ogre", "Hive", "Mythic", 5, 4,
                 "Sustained: When acquiring a buff, deals 1 damage to 4 random opposing units."),
            card("Tenacious Wizard", "Hive", "Mythic", 5, 4,
                 "When Played (Back Row): Grants Inspired to 2 random Hive units."),
            card("Shinka Vigile", "Cabal", "Mythic", 8, 4,
                 "When Played (3-Unit Line): Deals 4 damage to a unit in the back row."),
            card("Shinka Scorpius", "Cabal", "Mythic", 7, 4,
                 "Sustained (3-Unit Line): At the end of your turn, deals 2 damage to a random opposing unit."),
            card("Juggernaut", "Cabal", "Mythic", 8, 4,
                 "Sustained (Column): Cabal units to the left and right deal 1 additional damage."),
            card("Pandora-7", "Humans", "Legendary", 5, 4,
                 "Sustained: When your turn ends, grants the unit on the right +2 Power."),
            card("Harrow", "Humans", "Legendary", 7, 4,
                 "When Played: Toss a coin. If the result is heads, this unit gains +5 Power."),
            card("Helhest", "Humans", "Exotic", 6, 5,
                 "Sustained (Back Row): When your turn ends, deal 1 damage to 1 random opposing unit and the units to its left and right, while also gaining Power based on the damage dealt."),
            card("Thrall", "Hive", "Rare", 10, 1, "No special effects."),
            card("Legionary", "Cabal", "Rare", 10, 1, "No special effects."),
            card("Dreg", "Fallen", "Rare", 10, 1, "No special effects."),
            card("Tracker", "Fallen", "Rare", 10, 1, "No special effects.")
        ).associateBy { it.name }
        return ShadowEngine(catalog, seed).apply {
            humanTurn = true; humanFirst = true; human.points = 60
        }
    }
    private fun place(e: ShadowEngine, n: String, row: Int, col: Int,
                      side: ShadowEngine.Player = e.human): ShadowEngine.Unit {
        val u = ShadowEngine.Unit(side, e.catalog.getValue(n), row, col)
        side.board[row][col] = u
        return u
    }
    private fun play(e: ShadowEngine, n: String, row: Int, col: Int,
                     target: ShadowEngine.Target? = null): ShadowEngine.Unit {
        e.human.hand.add(e.catalog.getValue(n))
        assertTrue(e.lastActionError, e.play(e.human, e.human.hand.lastIndex, row, col, target))
        return e.human.board[row][col]!!
    }

    @Test fun allEightStatsAreFromCardDetailVideo() {
        val e = engine()
        val stats = mapOf(
            "Tenacious Ogre" to Pair(5, 4), "Tenacious Wizard" to Pair(5, 4),
            "Shinka Vigile" to Pair(8, 4), "Shinka Scorpius" to Pair(7, 4),
            "Juggernaut" to Pair(8, 4), "Pandora-7" to Pair(5, 4),
            "Harrow" to Pair(7, 4), "Helhest" to Pair(6, 5)
        )
        stats.forEach { (name, stats) ->
            assertEquals(name, stats.first, e.catalog.getValue(name).numericPower())
            assertEquals(name, stats.second, e.catalog.getValue(name).numericCost())
            assertTrue(name, e.catalog.getValue(name).isPlayable)
        }
    }

    @Test fun tenaciousOgreBuffDamagesUpToFourDistinctOpponents() {
        val e = engine()
        val ogre = place(e, "Tenacious Ogre", 0, 2)
        val victims = (0..3).map { place(e, "Dreg", 0, it, e.cpu) }
        e.buff(ogre, 1)
        assertTrue(victims.all { it.power == 9 })
    }

    @Test fun tenaciousWizardBackRowInspiresTwoDifferentHiveAllies() {
        val e = engine()
        val allies = listOf(place(e, "Thrall", 0, 0),
                             place(e, "Thrall", 0, 1),
                             place(e, "Thrall", 0, 3))
        play(e, "Tenacious Wizard", 0, 2)
        assertEquals(2, allies.count { it.inspired })

        val other = engine()
        val hive = place(other, "Thrall", 0, 0)
        play(other, "Tenacious Wizard", 1, 2)
        assertFalse(hive.inspired)
    }

    @Test fun shinkaVigileRequiresThreeUnitLineAndBackRowTarget() {
        val e = engine()
        place(e, "Legionary", 0, 1)
        place(e, "Legionary", 0, 3)
        val target = place(e, "Dreg", 0, 0, e.cpu)
        val front = place(e, "Dreg", 1, 0, e.cpu)
        assertEquals(1, e.targetOptions(e.human, e.catalog.getValue("Shinka Vigile"), 0, 2).size)
        play(e, "Shinka Vigile", 0, 2, ShadowEngine.Target(e.cpu, 0, 0))
        assertEquals(6, target.power)
        assertEquals(10, front.power)
    }

    @Test fun shinkaScorpiusDamagesOnlyWithThreeUnitLineAtEndTurn() {
        val e = engine()
        place(e, "Legionary", 0, 1)
        place(e, "Shinka Scorpius", 0, 2)
        place(e, "Legionary", 0, 3)
        val victim = place(e, "Dreg", 0, 0, e.cpu)
        e.effects.endSide(e.human)
        assertEquals(8, victim.power)

        val unpaired = engine()
        place(unpaired, "Shinka Scorpius", 0, 2)
        val safe = place(unpaired, "Dreg", 0, 0, unpaired.cpu)
        unpaired.effects.endSide(unpaired.human)
        assertEquals(10, safe.power)
    }

    @Test fun juggernautNeedsColumnPartnerAndBuffsLeftRightCabalDamage() {
        val e = engine()
        val jug = place(e, "Juggernaut", 0, 2)
        val source = place(e, "Legionary", 0, 1)
        val enemy = place(e, "Dreg", 0, 1, e.cpu)
        e.damage(source, enemy, 1)
        assertEquals(9, enemy.power)
        place(e, "Legionary", 1, 2)
        e.damage(source, enemy, 1)
        assertEquals(7, enemy.power)
        assertNotNull(jug)
    }

    @Test fun pandora7BuffsImmediateRightUnitAtEndTurn() {
        val e = engine()
        place(e, "Pandora-7", 0, 1)
        val right = place(e, "Dreg", 0, 2)
        val left = place(e, "Dreg", 0, 0)
        e.effects.endSide(e.human)
        assertEquals(12, right.power)
        assertEquals(10, left.power)
    }

    @Test fun harrowCoinFlipHasHeadsAndTails() {
        val powerWhenPlayed = listOf(0L, 123456L).map { seed ->
            val e = engine(seed)
            play(e, "Harrow", 0, 2).power
        }.toSet()
        assertEquals(setOf(7, 12), powerWhenPlayed)
    }

    @Test fun helhestBackRowDamagesTargetAndHorizontalNeighbors() {
        val e = engine()
        val helhest = place(e, "Helhest", 0, 2)
        val foes = (0..2).map { place(e, "Dreg", 0, it, e.cpu) }
        e.effects.endSide(e.human)
        assertTrue(foes.all { it.power == 9 })
        // Provisional conversion: one Power for each observed damage point.
        assertEquals(9, helhest.power)

        val front = engine()
        val inactive = place(front, "Helhest", 1, 2)
        val victim = place(front, "Dreg", 0, 0, front.cpu)
        front.effects.endSide(front.human)
        assertEquals(10, victim.power)
        assertEquals(6, inactive.power)
    }

    @Test fun helhestDoesNotTriggerWhenSuppressed() {
        val e = engine()
        val h = place(e, "Helhest", 0, 2)
        h.suppressed = true
        val victim = place(e, "Dreg", 0, 1, e.cpu)
        e.effects.endSide(e.human)
        assertEquals(10, victim.power)
    }
}
