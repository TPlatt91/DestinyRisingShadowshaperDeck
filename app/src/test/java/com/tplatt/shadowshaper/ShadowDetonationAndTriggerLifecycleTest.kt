package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test

/**
 * Transfers that detonate on a shared turn boundary, plus watcher lifecycle.
 * Execute on the local workstation using run_local_engine_checks.bat.
 */
class ShadowDetonationAndTriggerLifecycleTest {
    private fun card(name: String, faction: String, power: Int, effect: String) =
        ShadowData.Card().apply {
            this.name = name
            this.faction = faction
            rarity = "Rare"
            cost = "1"
            this.power = power.toString()
            this.effect = effect
        }

    private fun engine(): ShadowEngine {
        val cards = listOf(
            card("Cursed Thrall", "Hive", 1,
                "When Played: Transfers to opponent Front Row; explodes at start of your next turn."),
            card("Exploder Shank", "Fallen", 4,
                "When Played: Transfers to opponent Front Row; damages units to its left and right."),
            card("Dreg", "Fallen", 6, "No special effects."),
            card("Zhiyao", "Humans", 3,
                "Sustained: On opponent Front Row placement, +1 Power for three turns."),
            card("Elite Sniper Vandal", "Fallen", 4,
                "Sustained (Back Row): When enemy gets a bonus, deal 1 damage once per turn."),
            card("Servitor", "Fallen", 2,
                "Sustained: Your units deal one extra damage.")
        ).associateBy { it.name }
        return ShadowEngine(cards, 63L).apply {
            humanFirst = true
            humanTurn = false
            round = 2
            completedTurns = 0
        }
    }

    private fun place(
        e: ShadowEngine, name: String, side: ShadowEngine.Player,
        row: Int, col: Int, power: Int? = null
    ): ShadowEngine.Unit {
        val unit = ShadowEngine.Unit(side, e.catalog.getValue(name), row, col)
        if (power != null) unit.power = power
        side.board[row][col] = unit
        return unit
    }

    private fun scheduledBomb(
        e: ShadowEngine, name: String, col: Int, dueRound: Int
    ): ShadowEngine.Unit = place(e, name, e.cpu, 1, col).apply {
        fuseOrigin = e.human
        fuseRound = dueRound
    }

    @Test fun transferredCardsPublishDestinationArrivalRatherThanVacatedSlot() {
        for (name in listOf("Cursed Thrall", "Exploder Shank")) {
            val e = engine()
            e.humanTurn = true
            e.human.points = 3
            // Only one Front Row slot is open on the receiving side.
            for (column in 0 until ShadowRules.COLUMNS)
                if (column != 2) place(e, "Dreg", e.cpu, 1, column)
            e.human.hand.add(e.catalog.getValue(name))

            assertTrue(e.lastActionError,
                e.play(e.human, e.human.hand.lastIndex, 0, 0, null))
            val transferred = e.cpu.board[1][2]!!
            assertEquals(name, transferred.card.name)
            assertNull(e.human.board[0][0])
            assertSame(e.human, transferred.fuseOrigin)
            assertEquals(3, transferred.fuseRound)
            assertEquals(2, e.placementRevision)
            assertEquals(1, e.lastPlacedSide)
            assertEquals(1, e.lastPlacedRow)
            assertEquals(2, e.lastPlacedColumn)
            assertEquals(0, e.unresolvedEffects)
        }
    }

    @Test fun consecutiveTransferredCardsKeepIncrementingPlacementRevision() {
        val e = engine()
        e.humanTurn = true
        e.human.points = 3
        val names = listOf("Cursed Thrall", "Exploder Shank")
        names.forEachIndexed { i, name ->
            e.human.hand.add(e.catalog.getValue(name))
            assertTrue(e.lastActionError,
                e.play(e.human, e.human.hand.lastIndex, 0, i, null))
            assertEquals(2 * (i + 1), e.placementRevision)
            assertEquals(1, e.lastPlacedSide)
            assertEquals(1, e.lastPlacedRow)
            val selected = e.cpu.board[1][e.lastPlacedColumn]
            assertNotNull(selected)
            assertEquals(name, selected!!.card.name)
        }
        assertEquals(2, e.cpu.board[1].filterNotNull().size)
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun occupiedEnemyFrontRowDoesNotOverwriteUnitsOrReportUnimplementedTransfer() {
        for (name in listOf("Cursed Thrall", "Exploder Shank")) {
            val e = engine()
            e.humanTurn = true
            e.human.points = 3
            val defending = (0 until ShadowRules.COLUMNS).map { column ->
                place(e, "Dreg", e.cpu, ShadowRules.FRONT_ROW, column)
            }
            e.human.hand.add(e.catalog.getValue(name))
            assertTrue(e.lastActionError,
                e.play(e.human, e.human.hand.lastIndex, 0, 2, null))
            val played = e.human.board[0][2]
            assertNotNull(played)
            assertEquals(name, played!!.card.name)
            assertNull(played.fuseOrigin)
            assertEquals(0, played.fuseRound)
            assertEquals(1, e.placementRevision)
            assertEquals(0, e.lastPlacedSide)
            assertEquals(0, e.lastPlacedRow)
            assertEquals(2, e.lastPlacedColumn)
            for (col in 0 until ShadowRules.COLUMNS)
                assertSame(defending[col], e.cpu.board[ShadowRules.FRONT_ROW][col])
            assertEquals(0, e.unresolvedEffects)
            assertTrue(e.journal.any { it.startsWith("TRANSFER BLOCKED: " + name) })
        }
    }

    @Test fun twoSimultaneousBombsLeaveTheBoardBeforeAnyExplosionHits() {
        val e = engine()
        val left = place(e, "Dreg", e.cpu, 1, 0)
        val right = place(e, "Dreg", e.cpu, 1, 3)
        scheduledBomb(e, "Exploder Shank", 1, 2)
        scheduledBomb(e, "Cursed Thrall", 2, 2)

        e.endTurn() // CPU ends; human starts round 2, both bombs detonate.

        assertNull(e.cpu.board[1][1])
        assertNull(e.cpu.board[1][2])
        assertEquals(4, left.power)
        assertEquals(4, right.power)
        assertEquals(2, e.journal.count { it.contains("DETONATES") })
        assertEquals(2, e.journal.count { it.contains("DAMAGE 2 ▶ Dreg") })
        val lastDetonation = e.journal.indexOfLast { it.contains("DETONATES") }
        val firstBlast = e.journal.indexOfFirst { it.startsWith("DAMAGE 2") }
        assertTrue("All scheduled bombs must leave play before blast damage", lastDetonation < firstBlast)
        assertFalse(e.finished)
    }

    @Test fun bombsOnlyDetonateOnTheirScheduledOriginalOwnerTurn() {
        val e = engine()
        val neighbor = place(e, "Dreg", e.cpu, 1, 0)
        val bomb = scheduledBomb(e, "Exploder Shank", 1, 3)
        e.endTurn() // owner starts round 2; this one is not due
        assertSame(bomb, e.cpu.board[1][1])
        assertEquals(6, neighbor.power)
        assertFalse(e.journal.any { it.contains("DETONATES") })

        e.round = 3
        e.completedTurns = 0
        e.humanTurn = false
        e.endTurn() // now owner starts round 3
        assertNull(e.cpu.board[1][1])
        assertEquals(4, neighbor.power)
        assertEquals(1, e.journal.count { it.contains("DETONATES") })
    }

    @Test fun unscheduledNormalUnitIsNeverRemovedByBombResolution() {
        val e = engine()
        val civilian = place(e, "Dreg", e.cpu, 1, 4)
        scheduledBomb(e, "Exploder Shank", 1, 2)
        e.endTurn()
        assertSame(civilian, e.cpu.board[1][4])
        assertEquals(6, civilian.power)
    }

    @Test fun zhiyaoDefeatedByBonusRetaliationDoesNotKeepTemporaryBuff() {
        val e = engine()
        val zhiyao = place(e, "Zhiyao", e.cpu, 0, 2, power = 1)
        place(e, "Elite Sniper Vandal", e.human, 0, 0)
        place(e, "Servitor", e.human, 0, 1)
        val newUnit = place(e, "Dreg", e.human, 1, 5)

        e.effects.fireEnemyPlacement(newUnit)
        // Zhiyao 1 -> 2 via its own +1; sniper damage 1 + Servitor 1
        // removes it before temporary bonus metadata may be registered.
        assertNull(e.cpu.board[0][2])
        assertEquals(0, zhiyao.timedBonuses.size)
        assertTrue(e.journal.any { it.contains("DESTROYED ▶ Zhiyao") })
    }

    @Test fun existingSniperLimitsStillApplyWhenWatcherSurvives() {
        val e = engine()
        val zhiyao = place(e, "Zhiyao", e.cpu, 0, 2)
        val incoming = place(e, "Dreg", e.human, 1, 5)
        e.effects.fireEnemyPlacement(incoming)
        assertSame(zhiyao, e.cpu.board[0][2])
        assertEquals(4, zhiyao.power)
        assertEquals(1, zhiyao.timedBonuses.size)
        assertEquals(3, zhiyao.timedBonuses.single().turns)
    }
}
