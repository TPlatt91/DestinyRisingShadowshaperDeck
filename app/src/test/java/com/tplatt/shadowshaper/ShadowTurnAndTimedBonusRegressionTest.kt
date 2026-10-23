package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test

/** Local six-round, temporary Power and Sustained timing regressions. */
class ShadowTurnAndTimedBonusRegressionTest {
    private fun card(name: String, faction: String = "Humans", power: Int = 10,
                     effect: String = "No special effects.") =
        ShadowData.Card().apply {
            this.name = name
            this.faction = faction
            this.rarity = "Rare"
            this.cost = "1"
            this.power = power.toString()
            this.effect = effect
        }

    private fun fixture(): ShadowEngine {
        val cards = listOf(
            card("Zhiyao", effect =
                "Sustained: When the opponent deploys a Front Row unit, gain +1 Power for 3 turns."),
            card("Dreg", "Fallen"),
            card("Elite Sniper Vandal", "Fallen", 4,
                "Sustained (Back Row): Opponent's Power bonuses trigger 1 damage once per target per turn."),
            card("Saladin", "Humans", 6,
                "Sustained: When a Human ally takes damage, grants +3 Power."),
            card("Tan-2", "Humans", 2,
                "Sustained: Doubles the next acquired Power bonus.")
        )
        return ShadowEngine(cards.associateBy { it.name }, 73L)
    }

    private fun put(e: ShadowEngine, name: String, side: ShadowEngine.Player,
                    row: Int, col: Int): ShadowEngine.Unit {
        val unit = ShadowEngine.Unit(side, e.catalog.getValue(name), row, col)
        side.board[row][col] = unit
        return unit
    }

    @Test fun consecutiveZhiyaoBonusesExpireIndependently() {
        val e = fixture()
        val zhiyao = put(e, "Zhiyao", e.human, 0, 0)
        e.effects.fireEnemyPlacement(put(e, "Dreg", e.cpu, 1, 0))
        assertEquals(11, zhiyao.power)
        assertEquals(listOf(3), zhiyao.timedBonuses.map { it.turns })
        e.effects.endSide(e.human)
        e.effects.fireEnemyPlacement(put(e, "Dreg", e.cpu, 1, 1))
        assertEquals(12, zhiyao.power)
        assertEquals(listOf(2, 3), zhiyao.timedBonuses.map { it.turns })
        e.effects.endSide(e.human)
        assertEquals(12, zhiyao.power)
        e.effects.endSide(e.human)
        assertEquals(11, zhiyao.power)
        assertEquals(1, zhiyao.bonus)
        assertEquals(1, zhiyao.timedBonuses.size)
        e.effects.endSide(e.human)
        assertEquals(10, zhiyao.power)
        assertEquals(0, zhiyao.bonus)
        assertTrue(zhiyao.timedBonuses.isEmpty())
    }

    @Test fun twoZhiyaosReactSeparatelyToOneOpposingPlacement() {
        val e = fixture()
        val first = put(e, "Zhiyao", e.human, 0, 0)
        val second = put(e, "Zhiyao", e.human, 0, 1)
        e.effects.fireEnemyPlacement(put(e, "Dreg", e.cpu, 1, 0))
        assertEquals(11, first.power)
        assertEquals(11, second.power)
        assertEquals(1, first.timedBonuses.size)
        assertEquals(1, second.timedBonuses.size)
        repeat(3) { e.effects.endSide(e.human) }
        assertEquals(10, first.power)
        assertEquals(10, second.power)
        assertTrue(first.timedBonuses.isEmpty())
        assertTrue(second.timedBonuses.isEmpty())
    }

    @Test fun suppressionClearsOverlappingBonusesWithoutDoubleExpiration() {
        val e = fixture()
        val target = put(e, "Zhiyao", e.human, 0, 0)
        e.effects.fireEnemyPlacement(put(e, "Dreg", e.cpu, 1, 0))
        e.effects.fireEnemyPlacement(put(e, "Dreg", e.cpu, 1, 1))
        assertEquals(12, target.power)
        assertEquals(2, target.timedBonuses.size)
        e.suppress(target)
        assertTrue(target.suppressed)
        assertEquals(10, target.power)
        assertEquals(0, target.bonus)
        repeat(4) { e.effects.endSide(e.human) }
        assertEquals(10, target.power)
        assertTrue(target.timedBonuses.isEmpty())
    }

    @Test fun sniperRetaliatesOnlyOncePerTargetPerTurn() {
        val e = fixture()
        e.humanFirst = true
        e.humanTurn = true
        val target = put(e, "Dreg", e.human, 1, 0)
        put(e, "Elite Sniper Vandal", e.cpu, 0, 0)
        e.buff(target, 1)
        assertEquals(10, target.power)
        e.buff(target, 1)
        assertEquals(11, target.power)
        e.endTurn()
        e.endTurn()
        e.buff(target, 1)
        assertEquals(11, target.power)
    }

    @Test fun zhiyaoTemporaryBonusExcludesSaladinTriggeredBySniperDamage() {
        val e = fixture()
        val zhiyao = put(e, "Zhiyao", e.human, 0, 0)
        put(e, "Saladin", e.human, 0, 1)
        put(e, "Elite Sniper Vandal", e.cpu, 0, 0)
        e.effects.fireEnemyPlacement(put(e, "Dreg", e.cpu, 1, 2))

        // Zhiyao's +1 triggers the sniper. That 1 damage triggers
        // Saladin's separate, permanent +3 bonus.
        assertEquals(13, zhiyao.power) // 10 + 1 - 1 + 3
        assertEquals(4, zhiyao.bonus)
        assertEquals(listOf(1), zhiyao.timedBonuses.map { it.value })
        assertEquals(listOf(3), zhiyao.timedBonuses.map { it.turns })

        repeat(3) { e.effects.endSide(e.human) }
        assertEquals(12, zhiyao.power) // Saladin's +3 never expires
        assertEquals(3, zhiyao.bonus)
        assertTrue(zhiyao.timedBonuses.isEmpty())
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun zhiyaoTan2TemporaryBonusStillExcludesSaladinRetaliation() {
        val e = fixture()
        val zhiyao = put(e, "Zhiyao", e.human, 0, 0)
        val tan2 = put(e, "Tan-2", e.human, 1, 3)
        put(e, "Saladin", e.human, 0, 1)
        put(e, "Elite Sniper Vandal", e.cpu, 0, 0)
        e.effects.fireEnemyPlacement(put(e, "Dreg", e.cpu, 1, 2))

        assertTrue(tan2.bonusDoublerConsumed)
        assertEquals(14, zhiyao.power) // 10 + doubled 2 - 1 + Saladin 3
        assertEquals(5, zhiyao.bonus)
        assertEquals(listOf(2), zhiyao.timedBonuses.map { it.value })
        repeat(3) { e.effects.endSide(e.human) }
        assertEquals(12, zhiyao.power)
        assertEquals(3, zhiyao.bonus)
        assertTrue(zhiyao.timedBonuses.isEmpty())
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun secondZhiyaoPlacementHasOwnExpirationWithoutRecountingSaladin() {
        val e = fixture()
        val zhiyao = put(e, "Zhiyao", e.human, 0, 0)
        put(e, "Saladin", e.human, 0, 1)
        put(e, "Elite Sniper Vandal", e.cpu, 0, 0)
        e.effects.fireEnemyPlacement(put(e, "Dreg", e.cpu, 1, 2))
        e.effects.endSide(e.human) // First Zhiyao stack: 2 turns remain
        e.effects.fireEnemyPlacement(put(e, "Dreg", e.cpu, 1, 3))

        // Once-per-target sniper cannot trigger again this turn (the
        // fixture did not advance the turn ID), but the new Zhiyao
        // placement should still award a separate +1.
        assertEquals(14, zhiyao.power)
        assertEquals(5, zhiyao.bonus)
        assertEquals(listOf(2, 3), zhiyao.timedBonuses.map { it.turns })
        assertEquals(listOf(1, 1), zhiyao.timedBonuses.map { it.value })

        e.effects.endSide(e.human)
        e.effects.endSide(e.human)
        assertEquals(13, zhiyao.power) // first temporary +1 expired
        assertEquals(4, zhiyao.bonus)
        e.effects.endSide(e.human)
        assertEquals(12, zhiyao.power) // only Saladin's +3 remains
        assertEquals(3, zhiyao.bonus)
    }

    private fun cardsAndDeck(): Pair<Map<String, ShadowData.Card>, Map<String, Int>> {
        val cards = (1..15).associate { i ->
            val name = "Filler $i"
            name to card(name, power = 2)
        }
        return cards to cards.keys.associateWith { 1 }
    }

    @Test fun seedRestartReproducesOpeningCoinAndCards() {
        val (cards, deck) = cardsAndDeck()
        val e = ShadowEngine(cards, 38291L)
        e.start(deck, deck)
        fun opening(): List<Any> = listOf(
            e.humanFirst, e.human.hand.map { it.name },
            e.cpu.hand.map { it.name },
            e.human.drawPile.map { it.name },
            e.cpu.drawPile.map { it.name }
        )
        val expected = opening()
        repeat(11) { e.rng.nextInt() }
        e.endTurn()
        e.draw(e.human, 2)
        e.start(deck, deck)
        assertEquals(expected, opening())
        assertEquals(1, e.round)
        assertEquals(0, e.completedTurns)
        assertEquals(1, e.human.points)
        assertEquals(1, e.cpu.points)
        assertFalse(e.finished)
        assertNull(e.pendingMovedUnit)
    }

    @Test fun sixRoundsEndAfterExactlyTwelveTurnsForEitherOpeningSide() {
        val (cards, deck) = cardsAndDeck()
        for (seed in 0L..4L) {
            val e = ShadowEngine(cards, seed)
            e.start(deck, deck)
            val first = e.humanFirst
            for (round in 1..6) {
                assertEquals(round, e.round)
                assertEquals(round, e.human.points)
                assertEquals(round, e.cpu.points)
                assertEquals(first, e.humanTurn)
                e.human.points = 0
                e.cpu.points = 0
                e.endTurn()
                assertEquals(!first, e.humanTurn)
                assertFalse(e.finished)
                e.endTurn()
                if (round < 6) {
                    assertEquals(round + 1, e.round)
                    assertFalse(e.finished)
                }
            }
            assertTrue(e.finished)
            assertEquals(6, e.round)
            assertEquals(12, 2 * (e.round - 1) + e.completedTurns)
            assertEquals("DRAW", e.result())
            assertEquals(if (first) 10 else 9, e.human.hand.size)
            assertEquals(if (first) 9 else 10, e.cpu.hand.size)
            val endRound = e.round
            e.endTurn()
            assertEquals(endRound, e.round)
        }
    }
}
