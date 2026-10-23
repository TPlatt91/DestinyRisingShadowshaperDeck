package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test
import java.util.Random

/**
 * Regression scenarios based on the user's original Batch 09 and Batch 10
 * card-detail videos. Run locally with :app:testDebugUnitTest, without Actions.
 */
class ShadowBatch0910VideoRulesTest {
    private fun card(name: String, faction: String, power: Int, cost: Int, effect: String) =
        ShadowData.Card().apply {
            this.name = name
            this.faction = faction
            this.power = power.toString()
            this.cost = cost.toString()
            this.rarity = "Legendary"
            this.effect = effect
        }

    private fun engine(seed: Long = 17L): ShadowEngine {
        val cards = listOf(
            card("Xuan Wei", "Humans", 6, 3, "When Played (Front Row): Deals 5 damage and moves the unit."),
            card("Jaren", "Humans", 4, 3, "When Played (Front Row): Deals total 6 damage at random."),
            card("Kevarrh", "Fallen", 7, 3, "When Played: Draw 2 cards."),
            card("Guarding Servitor", "Fallen", 4, 3, "When Played: Grants Inspired to a unit."),
            card("Blinder Shank", "Fallen", 8, 3, "When Played: Suppresses units on the left and right."),
            card("Resilient Grenade Dreg", "Fallen", 7, 3, "Sustained (Back Row): Enemy back placements deal 2 damage for 2 turns."),
            card("Chinnawat", "Humans", 4, 3, "When Played (Front Row): +1 Power to a unit and adjacent units."),
            card("Common Cover", "Humans", 7, 3, "When Played (Front Row): Summons 2 Rocks on enemy front row."),
            card("Guerrilla Captain", "Humans", 3, 3, "Sustained: On Power bonus, deal 2 damage to random enemy."),
            card("Wretch", "Fallen", 6, 3, "When Played (Front Row): Deal 1 damage to enemy front."),
            card("Inquisitor", "Hive", 3, 3, "When Played (Back Row): Deal 4 damage."),
            card("Knight", "Hive", 5, 3, "When Played: Give an Inspired Hive unit +3 Power."),
            card("Wizard", "Hive", 4, 3, "When Played: Grant Inspired to Hive unit."),
            card("Ogre", "Hive", 2, 3, "Sustained: On buff, deal 1 damage to 2 random enemy units."),
            card("Shrieker", "Hive", 2, 3, "Sustained (Back Row): On buff, deal 2 damage to enemy."),
            card("Vigile", "Cabal", 5, 3, "When Played (Front Row): Give +2 Power to Line unit."),
            card("Scorpius", "Cabal", 4, 3, "Sustained: End of turn, deal 1 damage to a random enemy."),
            card("Colossus", "Cabal", 6, 3, "Sustained (Column): Give Cabal unit above/below +1 Power."),
            card("Phalanx", "Cabal", 4, 3, "Sustained (3-Unit Line): Cabal units take 1 less damage."),
            card("Drop Pod", "Cabal", 2, 3, "When Played (2 Unit Line): Summons a random Line unit on left."),
            card("Shinka Incendior", "Cabal", 5, 3, "When Played (3-Unit Line): Deal 3 damage to Back Row unit."),
            card("Devil Sentry", "Fallen", 5, 3, "When Played: Coin toss heads deal 5 damage."),
            card("Gladiator", "Cabal", 4, 3, "When Played (Front Row): Selected front row unit and neighbors deal +1 damage."),
            card("Rock", "Humans", 1, 1, "No special effects."),
            card("Tracker", "Fallen", 30, 2, "No special effects."),
            card("Dreg", "Fallen", 10, 1, "No special effects."),
            card("Thrall", "Hive", 10, 1, "No special effects."),
            card("Legionary", "Cabal", 10, 1, "No special effects."),
            card("Shinka Psion", "Cabal", 3, 2, "When Played (2-Unit Line): Deals 3 damage."),
        )
        cards.first { it.name == "Rock" }.classification = "Summoned unit"
        return ShadowEngine(cards.associateBy { it.name }, seed).apply {
            humanFirst = true
            humanTurn = true
            human.points = 80
        }
    }

    private fun set(e: ShadowEngine, name: String, row: Int, column: Int,
                    side: ShadowEngine.Player = e.human): ShadowEngine.Unit {
        val unit = ShadowEngine.Unit(side, e.catalog.getValue(name), row, column)
        side.board[row][column] = unit
        return unit
    }

    private fun play(e: ShadowEngine, name: String, row: Int, column: Int,
                     target: ShadowEngine.Target? = null): ShadowEngine.Unit? {
        e.human.hand.add(e.catalog.getValue(name))
        assertTrue(e.lastActionError, e.play(e.human, e.human.hand.lastIndex, row, column, target))
        return e.human.board[row][column]
    }

    @Test fun metadataRelatedRockIsNotDeckConstructible() {
        val e = engine()
        assertEquals(1, e.catalog.getValue("Rock").numericPower())
        assertEquals(1, e.catalog.getValue("Rock").numericCost())
        assertFalse(e.catalog.getValue("Rock").isPlayable)
    }

    @Test fun xuanWeiNeedsFrontRowAndCanRelocateSurvivingVictim() {
        val e = engine()
        val victim = set(e, "Tracker", 0, 0, e.cpu)
        assertFalse(e.activeInRow(e.catalog.getValue("Xuan Wei"), 0))
        play(e, "Xuan Wei", 1, 2, ShadowEngine.Target(e.cpu, 0, 0))
        assertEquals(25, victim.power)
        assertSame(victim, e.pendingMovedUnit)
        assertTrue(e.resolveMove(e.human, 0, 4))
        assertSame(victim, e.cpu.board[0][4])
        assertNull(e.cpu.board[0][0])
    }

    @Test fun jarenDealsExactlySixRandomDamagePointsWithEnemyPresent() {
        val e = engine()
        val victim = set(e, "Tracker", 1, 1, e.cpu)
        play(e, "Jaren", 1, 2)
        assertEquals(24, victim.power)
    }

    @Test fun kevarrhDrawsTwo() {
        val e = engine()
        repeat(3) { e.human.drawPile.add(e.catalog.getValue("Dreg")) }
        play(e, "Kevarrh", 0, 2)
        assertEquals(2, e.human.hand.size)
        assertEquals(1, e.human.drawPile.size)
    }

    @Test fun guardingServitorCanInspireAlly() {
        val e = engine()
        val ally = set(e, "Tracker", 0, 0)
        play(e, "Guarding Servitor", 0, 2, ShadowEngine.Target(e.human, 0, 0))
        assertTrue(ally.inspired)
    }

    @Test fun blinderShankSuppressesLeftAndRightNotOtherRows() {
        val e = engine()
        val left = set(e, "Dreg", 0, 1)
        val right = set(e, "Dreg", 0, 3)
        val other = set(e, "Dreg", 1, 2)
        play(e, "Blinder Shank", 0, 2)
        assertTrue(left.suppressed)
        assertTrue(right.suppressed)
        assertFalse(other.suppressed)
    }

    @Test fun resilientGrenadeDregTriggersOnlyOnEnemyBackPlacement() {
        val e = engine()
        set(e, "Resilient Grenade Dreg", 0, 2)
        val enemy = set(e, "Dreg", 0, 1, e.cpu)
        e.effects.fireEnemyPlacement(enemy)
        assertEquals(2, enemy.scorched)
        assertEquals(2, enemy.scorchTurns)
        e.effects.endSide(e.cpu)
        assertEquals(8, enemy.power)
        assertEquals(1, enemy.scorchTurns)
    }

    @Test fun chinnawatBuffsSelectedAllyAndHorizontalNeighbors() {
        val e = engine()
        val left = set(e, "Dreg", 1, 0)
        val target = set(e, "Dreg", 1, 1)
        val right = set(e, "Dreg", 1, 2)
        val unselected = set(e, "Dreg", 0, 1)
        play(e, "Chinnawat", 1, 4, ShadowEngine.Target(e.human, 1, 1))
        assertEquals(11, left.power)
        assertEquals(11, target.power)
        assertEquals(11, right.power)
        assertEquals(10, unselected.power)
    }

    @Test fun commonCoverSummonsTwoDistinctRocksIntoOpposingFrontRow() {
        val e = engine()
        play(e, "Common Cover", ShadowRules.FRONT_ROW, 1)
        val rocks = e.cpu.board[ShadowRules.FRONT_ROW].filterNotNull()
            .filter { it.card.name == "Rock" }
        assertEquals(2, rocks.size)
        assertEquals(2, rocks.map { it.column }.toSet().size)
        assertTrue(e.cpu.board[ShadowRules.BACK_ROW].all { it == null })
        assertTrue(rocks.all { !it.card.isPlayable })
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun commonCoverSummonsAtMostAvailableFrontRowSlots() {
        val e = engine()
        for (col in 0 until ShadowRules.COLUMNS - 1) {
            set(e, "Dreg", ShadowRules.FRONT_ROW, col, e.cpu)
        }
        play(e, "Common Cover", ShadowRules.FRONT_ROW, 1)
        val rocks = e.cpu.board[ShadowRules.FRONT_ROW].filterNotNull()
            .count { it.card.name == "Rock" }
        assertEquals(1, rocks)
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun commonCoverDoesNotSummonFromBackRow() {
        val e = engine()
        play(e, "Common Cover", ShadowRules.BACK_ROW, 1)
        assertEquals(0, e.cpu.board[ShadowRules.FRONT_ROW].filterNotNull()
            .count { it.card.name == "Rock" })
    }

    @Test fun guerrillaCaptainDealsTwoUponOwnPowerBonus() {
        val e = engine()
        val captain = set(e, "Guerrilla Captain", 0, 2)
        val foe = set(e, "Dreg", 0, 1, e.cpu)
        e.buff(captain, 1)
        assertEquals(8, foe.power)
    }

    @Test fun wretchRestrictsTargetToFrontAndInquisitorRequiresBack() {
        val e = engine()
        set(e, "Dreg", 0, 0, e.cpu)
        val front = set(e, "Dreg", 1, 0, e.cpu)
        assertEquals(1, e.targetOptions(e.human, e.catalog.getValue("Wretch"), 1, 2).size)
        assertEquals(0, e.targetOptions(e.human, e.catalog.getValue("Inquisitor"), 1, 2).size)
        play(e, "Wretch", 1, 2, ShadowEngine.Target(e.cpu, 1, 0))
        assertEquals(9, front.power)
        play(e, "Inquisitor", 0, 2, ShadowEngine.Target(e.cpu, 0, 0))
        assertEquals(6, e.cpu.board[0][0]!!.power)
    }

    @Test fun knightOnlyTargetsInspiredHiveAndWizardOnlyTargetsHive() {
        val e = engine()
        val inspired = set(e, "Thrall", 0, 0)
        inspired.inspired = true
        set(e, "Thrall", 0, 1)
        set(e, "Jaren", 0, 4)
        val knight = e.targetOptions(e.human, e.catalog.getValue("Knight"), 1, 2)
        assertEquals(1, knight.size)
        val wizard = e.targetOptions(e.human, e.catalog.getValue("Wizard"), 1, 2)
        assertEquals(2, wizard.size)
    }

    @Test fun ogreHitsTwoDistinctEnemiesAndShriekerNeedsBackRow() {
        val e = engine()
        val victimA = set(e, "Dreg", 0, 0, e.cpu)
        val victimB = set(e, "Dreg", 0, 1, e.cpu)
        val ogre = set(e, "Ogre", 0, 2)
        e.buff(ogre, 1)
        assertEquals(9, victimA.power)
        assertEquals(9, victimB.power)
        val shrieker = set(e, "Shrieker", 1, 2)
        e.buff(shrieker, 1)
        assertEquals(18, victimA.power + victimB.power)
        shrieker.row = 0
        e.human.board[1][2] = null
        e.human.board[0][3] = shrieker
        shrieker.column = 3
        e.buff(shrieker, 1)
        assertEquals(16, victimA.power + victimB.power)
    }

    @Test fun vigileSelectsPrintedLineUnitNotJustCardMentioningLine() {
        val e = engine()
        set(e, "Shinka Psion", 0, 0)
        set(e, "Vigile", 0, 1)
        val options = e.targetOptions(e.human, e.catalog.getValue("Vigile"), 1, 2)
        assertEquals(1, options.size)
        play(e, "Vigile", 1, 2, ShadowEngine.Target(e.human, 0, 0))
        assertEquals(5, e.human.board[0][0]!!.power)
    }

    @Test fun scorpiusDealsOneDamageEachEndTurnInEitherRow() {
        val e = engine()
        set(e, "Scorpius", 1, 1)
        val victim = set(e, "Dreg", 1, 1, e.cpu)
        e.effects.endSide(e.human)
        assertEquals(9, victim.power)
    }

    @Test fun colossusOnlyBuffsVerticallyAdjacentCabal() {
        val e = engine()
        set(e, "Colossus", 0, 2)
        val cabal = set(e, "Legionary", 1, 2)
        val nonCabal = set(e, "Dreg", 1, 3)
        assertEquals(12, e.power(cabal)) // +1 Colossus and +1 Legionary Column self-aura
        assertEquals(10, e.power(nonCabal))
    }

    @Test fun phalanxReducesCabalDamageAfterThreeInLine() {
        val e = engine()
        set(e, "Shinka Psion", 1, 0)
        set(e, "Phalanx", 1, 1)
        val cabal = set(e, "Shinka Psion", 1, 2)
        assertEquals(1, e.effects.reduction(cabal))
        assertEquals(0, e.effects.reduction(set(e, "Dreg", 0, 4)))
    }

    @Test fun dropPodSummonsAnActualLineAffixUnitToLeft() {
        val e = engine()
        set(e, "Phalanx", 1, 3)
        play(e, "Drop Pod", 1, 2)
        val unit = e.human.board[1][1]!!
        assertTrue(ShadowPositionRules.hasLineAffix(unit.card.effect))
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun shinkaIncendiorRequiresThreeLineAndEnemyBackRow() {
        val e = engine()
        set(e, "Shinka Psion", 1, 1)
        set(e, "Phalanx", 1, 3)
        val victim = set(e, "Dreg", 0, 0, e.cpu)
        val options=e.targetOptions(e.human, e.catalog.getValue("Shinka Incendior"), 1, 2)
        assertEquals(1, options.size)
        play(e, "Shinka Incendior", 1, 2, ShadowEngine.Target(e.cpu, 0, 0))
        assertEquals(7, victim.power)
    }

    @Test fun devilSentryHeadCoinFlipDamageIsDeterministic() {
        for (seed in listOf(0L, 1L, 234567L, 987654321L)) {
            val e = engine(seed)
            val victim = set(e, "Tracker", 0, 0, e.cpu)
            val expected = if (Random(seed).nextBoolean()) 25 else 30
            play(e, "Devil Sentry", 0, 2, ShadowEngine.Target(e.cpu, 0, 0))
            assertEquals(expected, victim.power)
        }
    }

    @Test fun gladiatorGrantsAdditionalDamageNotPowerToTargetAndNeighbors() {
        val e = engine()
        val left = set(e, "Legionary", 1, 0)
        val target = set(e, "Legionary", 1, 1)
        val right = set(e, "Legionary", 1, 2)
        val other = set(e, "Legionary", 0, 1)
        play(e, "Gladiator", 1, 4, ShadowEngine.Target(e.human, 1, 1))
        assertEquals(1, left.damageBonus)
        assertEquals(1, target.damageBonus)
        assertEquals(1, right.damageBonus)
        assertEquals(0, other.damageBonus)
        assertEquals(11, e.power(target)) // Gladiator changes damage only; paired Legionary retains +1 Column aura
        val foe = set(e, "Dreg", 0, 0, e.cpu)
        e.damage(target, foe, 1)
        assertEquals(8, foe.power)
    }
}
