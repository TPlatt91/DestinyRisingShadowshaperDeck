package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test

/** Batch 06 and 07 original in-game card-detail video regressions.
 * Runs locally with :app:testDebugUnitTest, no GitHub Actions needed.
 */
class ShadowBatch0607EffectsTest {
    private fun card(name: String, faction: String, rarity: String, light: Int, power: Int,
                     effect: String = "No special effects.") =
        ShadowData.Card().apply {
            this.name = name; this.faction = faction; this.rarity = rarity
            this.cost = light.toString(); this.power = power.toString(); this.effect = effect
        }

    private fun engine(seed: Long = 17L): ShadowEngine {
        val cards = listOf(
            card("Umeko", "Humans", "Exotic", 2, 3, "Sustained (Back Row): Your units randomly deal 1-3 additional damage."),
            card("Xur", "Humans", "Mythic", 2, 7, "Sustained: When you acquire a bonus, randomly places a Rock in the opponent's front row."),
            card("Wu Ming", "Humans", "Mythic", 2, 3, "When Played (Back Row): Suppresses a unit."),
            card("Ketch", "Fallen", "Mythic", 2, 3, "When Played: Summons 2 Dregs on the left and right."),
            card("Tracker", "Fallen", "Mythic", 2, 7),
            card("Tenacious Hexguard", "Hive", "Mythic", 2, 3, "When Played: Grants Inspired to the Hive units to its left and right."),
            card("The Disfavored", "Hive", "Mythic", 2, 3, "Sustained (Front Row): When acquiring a buff, grants +2 Power to one of your different Hive units at random."),
            card("Centurion", "Cabal", "Mythic", 2, 5, "Sustained (4-Unit Line): Grants +2 Power to units on the left and right."),
            card("Umbliks Prime", "Fallen", "Mythic", 2, 4, "When Played: Toss a coin. If the result is heads, grants one of your units +6 Power."),
            card("Mu Ying", "Humans", "Legendary", 2, 3, "When Played: Summons 1 Guerrilla on the left and 1 on the right."),
            card("The Speaker", "Humans", "Legendary", 2, 1, "Sustained (Back Row): When acquiring a Power bonus, grants +2 Power to the unit on the right."),
            card("Denzin", "Humans", "Legendary", 2, 3, "Sustained: When acquiring a Power bonus, summons a Guerrilla on the left."),
            card("Hyssop", "Humans", "Legendary", 2, 2, "When Played: Grants +3 Power to a unit with a bonus."),
            card("Elite Vandal", "Fallen", "Legendary", 2, 3, "When Played: If there are units in the opponent's front row, gains +3 Power."),
            card("Elite Sniper Vandal", "Fallen", "Legendary", 2, 4, "Sustained (Back Row): When an opposing unit acquires a bonus, deals 1 damage to it; lasts 2 turns."),
            card("Tenacious Thrall", "Hive", "Legendary", 2, 5),
            card("Rock", "Humans", "Rare", 1, 1),
            card("Guerrilla", "Humans", "Rare", 1, 2),
            card("Dreg", "Fallen", "Rare", 1, 1),
            card("Thrall", "Hive", "Rare", 1, 2),
            card("Legionary", "Cabal", "Rare", 1, 2)
        )
        cards.first { it.name == "Rock" }.classification = "Summoned unit"
        cards.first { it.name == "Guerrilla" }.classification = "Summoned unit"
        return ShadowEngine(cards.associateBy { it.name }, seed).apply {
            humanTurn = true
            human.points = 30
        }
    }

    private fun put(e: ShadowEngine, name: String, row: Int, col: Int,
                    side: ShadowEngine.Player = e.human): ShadowEngine.Unit {
        val unit = ShadowEngine.Unit(side, e.catalog.getValue(name), row, col)
        side.board[row][col] = unit
        return unit
    }

    private fun play(e: ShadowEngine, name: String, row: Int = 0, col: Int = 2,
                     target: ShadowEngine.Target? = null): ShadowEngine.Unit {
        e.human.hand.add(e.catalog.getValue(name))
        assertTrue(e.lastActionError, e.play(e.human, e.human.hand.lastIndex, row, col, target))
        return e.human.board[row][col]!!
    }

    @Test fun videoStatsAndTokenDeckLegality() {
        val e = engine()
        val expected = mapOf(
            "Umeko" to Pair(2,3), "Xur" to Pair(2,7), "Wu Ming" to Pair(2,3),
            "Ketch" to Pair(2,3), "Tracker" to Pair(2,7),
            "Tenacious Hexguard" to Pair(2,3), "The Disfavored" to Pair(2,3),
            "Centurion" to Pair(2,5), "Umbliks Prime" to Pair(2,4),
            "Mu Ying" to Pair(2,3), "The Speaker" to Pair(2,1),
            "Denzin" to Pair(2,3), "Hyssop" to Pair(2,2),
            "Elite Vandal" to Pair(2,3), "Elite Sniper Vandal" to Pair(2,4),
            "Tenacious Thrall" to Pair(2,5)
        )
        for ((name, numbers) in expected) {
            val c = e.catalog.getValue(name)
            assertEquals(name, numbers.first, c.numericCost())
            assertEquals(name, numbers.second, c.numericPower())
            assertTrue(name, c.isPlayable)
        }
        assertEquals(1, e.catalog.getValue("Rock").numericPower())
        assertEquals(1, e.catalog.getValue("Rock").numericCost())
        assertFalse(e.catalog.getValue("Rock").isPlayable)
    }

    @Test fun umekoAddsDamageEvenWhenUmekoIsSource() {
        val e = engine()
        val umeko = put(e, "Umeko", 0, 1)
        val victim = put(e, "Tracker", 1, 1, e.cpu)
        val before = victim.power
        e.damage(umeko, victim, 1)
        assertTrue(before - victim.power in 2..4)
    }

    @Test fun rockCannotBePlayedOrDirectlySummonedWithoutXur() {
        val e = engine()
        val rock = e.catalog.getValue("Rock")
        assertFalse(rock.isPlayable)
        assertTrue(ShadowDeckRules.canAdd(emptyMap(), rock, e.catalog).isNotBlank())
        assertTrue(ShadowDeckRules.validate(mapOf("Rock" to 1), e.catalog).isNotBlank())
        e.human.hand.add(rock)
        assertFalse(e.isPlayablePlacement(e.human, e.human.hand.lastIndex, 0, 0))
        assertFalse(e.play(e.human, e.human.hand.lastIndex, 0, 0, null))
        assertEquals(1, e.human.hand.size)
        e.spawnAt(e.cpu, ShadowRules.FRONT_ROW, 0, "Rock")
        assertNull(e.cpu.board[ShadowRules.FRONT_ROW][0])
        e.spawnRandom(e.cpu, ShadowRules.FRONT_ROW, "Rock", e.catalog.getValue("Tracker"))
        assertTrue(e.cpu.board[ShadowRules.FRONT_ROW].all { it == null })
    }

    @Test fun xurSummonsVerifiedRockInEnemyFrontRow() {
        val e = engine()
        val xur = play(e, "Xur", 0, 2)
        assertEquals(0, e.unresolvedEffects)
        e.buff(xur, 1)
        assertEquals(1, e.cpu.board[1].filterNotNull().count { it.card.name == "Rock" })
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun wuMingBackRowSuppressesChosenOpponent() {
        val e = engine()
        val opponent = put(e, "Tracker", 0, 1, e.cpu)
        e.buff(opponent, 2)
        play(e, "Wu Ming", 0, 2, ShadowEngine.Target(e.cpu, 0, 1))
        assertTrue(opponent.suppressed)
        assertEquals(0, opponent.bonus)
    }

    @Test fun ketchSummonsTwoDregs() {
        val e = engine()
        play(e, "Ketch")
        assertEquals("Dreg", e.human.board[0][1]?.card?.name)
        assertEquals("Dreg", e.human.board[0][3]?.card?.name)
    }

    @Test fun trackerAndTenaciousThrallUseBasicPower() {
        val e = engine()
        assertEquals(7, e.power(play(e, "Tracker", 0, 0)))
        assertEquals(5, e.power(play(e, "Tenacious Thrall", 1, 1)))
    }

    @Test fun tenaciousHexguardInspiresOnlyAdjacentHive() {
        val e = engine()
        val left = put(e, "Thrall", 0, 1)
        val right = put(e, "Thrall", 0, 3)
        val far = put(e, "Thrall", 0, 4)
        put(e, "Tracker", 1, 2)
        play(e, "Tenacious Hexguard")
        assertTrue(left.inspired)
        assertTrue(right.inspired)
        assertFalse(far.inspired)
    }

    @Test fun theDisfavoredBuffsDifferentHiveUnit() {
        val e = engine()
        val source = put(e, "The Disfavored", 1, 1)
        val ally = put(e, "Thrall", 1, 2)
        e.buff(source, 1)
        assertEquals(4, source.power)
        assertEquals(4, ally.power)
    }

    @Test fun centurionRequiresFourInLineBeforeBoostingAdjacent() {
        val e = engine()
        val left = put(e, "Legionary", 1, 0)
        val centurion = put(e, "Centurion", 1, 1)
        val right = put(e, "Legionary", 1, 2)
        assertEquals(2, e.power(left))
        put(e, "Legionary", 1, 3)
        assertEquals(4, e.power(left))
        assertEquals(4, e.power(right))
        assertEquals(5, e.power(centurion))
    }

    @Test fun umbliksPrimeCoinFlipCanBuffSelfOnHeads() {
        val observed = listOf(0L, 1L, 40L, 123456L, 4096L, Long.MAX_VALUE).map { seed ->
            val e = engine(seed)
            e.power(play(e, "Umbliks Prime"))
        }.toSet()
        assertEquals(setOf(4, 10), observed)
    }

    @Test fun muYingSummonsBothAdjacentGuerrillas() {
        val e = engine()
        play(e, "Mu Ying")
        assertEquals("Guerrilla", e.human.board[0][1]?.card?.name)
        assertEquals("Guerrilla", e.human.board[0][3]?.card?.name)
    }

    @Test fun speakerBuffsUnitToRightOnlyOnPowerBonus() {
        val e = engine()
        val speaker = put(e, "The Speaker", 0, 2)
        val right = put(e, "Tracker", 0, 3)
        val left = put(e, "Tracker", 0, 1)
        e.buff(speaker, 1)
        assertEquals(9, right.power)
        assertEquals(7, left.power)
    }

    @Test fun denzinOnPowerBonusSummonsGuerrillaToLeft() {
        val e = engine()
        val denzin = put(e, "Denzin", 0, 2)
        e.buff(denzin, 1)
        assertEquals("Guerrilla", e.human.board[0][1]?.card?.name)
    }

    @Test fun hyssopCanSelectOnlyAlreadyBuffedAllies() {
        val e = engine()
        val ally = put(e, "Tracker", 0, 0)
        put(e, "Tracker", 0, 4)
        e.buff(ally, 1)
        val options = e.targetOptions(e.human, e.catalog.getValue("Hyssop"), 1, 2)
        assertEquals(1, options.size)
        play(e, "Hyssop", 1, 2, ShadowEngine.Target(e.human, 0, 0))
        assertEquals(11, ally.power)
    }

    @Test fun eliteVandalBonusDoesNotRequireBackRow() {
        val e = engine()
        put(e, "Tracker", 1, 0, e.cpu)
        val actor = play(e, "Elite Vandal", 1, 2)
        assertEquals(6, actor.power)
    }

    @Test fun sniperHitsEachUnitAtMostOncePerTurnAndExpiresAfterTwoOwnerTurns() {
        val e = engine()
        val sniper = put(e, "Elite Sniper Vandal", 0, 2, e.cpu)
        val target = put(e, "Tracker", 0, 0)
        e.buff(target, 1)
        assertEquals(7, target.power)
        e.buff(target, 1)
        assertEquals(8, target.power)
        e.effects.endSide(e.cpu)
        assertEquals(1, sniper.sniperTurnsRemaining)
        e.effects.endSide(e.cpu)
        assertEquals(0, sniper.sniperTurnsRemaining)
        e.buff(target, 1)
        assertEquals(9, target.power)
    }
}
