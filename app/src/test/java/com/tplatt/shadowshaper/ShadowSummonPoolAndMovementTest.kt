package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test

/**
 * Verified summoning eligibility, occupied-slot behavior and movement edges.
 * Local JVM tests; no GitHub Actions required.
 */
class ShadowSummonPoolAndMovementTest {
    private fun card(
        name: String, faction: String, power: Int, cost: Int, effect: String,
        classification: String = ""
    ) = ShadowData.Card().apply {
        this.name = name
        this.faction = faction
        this.power = power.toString()
        this.cost = cost.toString()
        this.effect = effect
        this.classification = classification
        this.rarity = "Rare"
    }

    private fun catalog(): Map<String, ShadowData.Card> = listOf(
        card("Tomb Ship", "Hive", 6, 4,
            "When Played: Summons 2 random Hive units to its left and right."),
        card("Paradoxical Neophyte's Shade", "Hive", 1, 6,
            "Sustained: Gains +4 Power for each Inspired Hive unit.", "Summoned unit"),
        card("Thrall", "Hive", 2, 1, "No special effects."),
        card("Cursed Thrall", "Hive", 1, 1, "When Played: Moves to opposing field."),
        card("Drop Pod", "Cabal", 2, 3,
            "When Played (2 Unit Line): Summons a random Line unit to the left."),
        card("Shinka Psion", "Cabal", 3, 2,
            "When Played (2-Unit Line): Deals 3 damage."),
        card("Line Mention Only", "Cabal", 3, 2,
            "When Played: Grant Power to a nearby Line unit."),
        card("Rock", "Humans", 1, 1, "No special effects.", "Summoned unit"),
        card("Xuan Wei", "Humans", 6, 3,
            "When Played (Front Row): Deals 5 damage to a unit and moves it."),
        card("Tracker", "Fallen", 20, 1, "No special effects."),
        card("Zhiyao", "Humans", 3, 2,
            "Sustained: On opposing Front Row placement, gain +1 Power for 3 turns.")
    ).associateBy { it.name }

    private fun engine(seed: Long = 37L) = ShadowEngine(catalog(), seed).apply {
        humanFirst = true
        humanTurn = true
        human.points = 80
    }

    private fun put(e: ShadowEngine, name: String, side: ShadowEngine.Player,
                    row: Int, column: Int): ShadowEngine.Unit {
        val unit = ShadowEngine.Unit(side, e.catalog.getValue(name), row, column)
        side.board[row][column] = unit
        return unit
    }

    private fun play(
        e: ShadowEngine, name: String, row: Int, column: Int,
        target: ShadowEngine.Target? = null
    ): Boolean {
        e.human.hand.add(e.catalog.getValue(name))
        return e.play(e.human, e.human.hand.lastIndex, row, column, target)
    }

    @Test fun tombShipPoolContainsSummonOnlyShadeWithoutOpeningDeckConstruction() {
        val e = engine()
        val pool = ShadowSummonRules.tombShipPool(e.catalog)
        assertTrue(pool.any { it.name == "Paradoxical Neophyte's Shade" })
        assertTrue(pool.any { it.name == "Tomb Ship" })
        assertTrue(pool.any { it.name == "Thrall" })
        assertFalse(pool.any { it.name == "Rock" })
        assertFalse(e.catalog.getValue("Paradoxical Neophyte's Shade").isPlayable)

        e.human.hand.add(e.catalog.getValue("Paradoxical Neophyte's Shade"))
        assertFalse(e.play(e.human, e.human.hand.lastIndex, 1, 3, null))
        assertTrue(e.lastActionError.contains("summon-only"))
    }

    @Test fun tombShipCanActuallyRollShadeAtEitherAdjacentSlot() {
        var sawShade = false
        for (seed in 0L..100L) {
            val e = engine(seed)
            assertTrue(e.lastActionError, play(e, "Tomb Ship", 0, 2))
            for (col in listOf(1, 3)) {
                val summoned = e.human.board[0][col]
                assertNotNull(summoned)
                assertEquals("Hive", summoned!!.card.faction)
                if (summoned.card.name == "Paradoxical Neophyte's Shade") sawShade = true
            }
            if (sawShade) break
        }
        assertTrue("Tomb Ship must be able to select the summoned Shade", sawShade)
    }

    @Test fun tombShipRespectsExistingOccupantsAndTheBoardEdge() {
        val e = engine()
        val left = put(e, "Thrall", e.human, 0, 1)
        assertTrue(e.lastActionError, play(e, "Tomb Ship", 0, 2))
        assertSame(left, e.human.board[0][1])
        assertNotNull(e.human.board[0][3])
        assertEquals(0, e.unresolvedEffects)

        val edge = engine()
        assertTrue(edge.lastActionError, play(edge, "Tomb Ship", 0, 0))
        assertNotNull(edge.human.board[0][1])
        assertEquals(2, edge.human.board[0].filterNotNull().size)
    }

    @Test fun dropPodCandidatesOnlyHavePrintedLineAffixes() {
        val e = engine()
        val pool = ShadowSummonRules.dropPodPool(e.catalog)
        assertEquals(listOf("Shinka Psion"), pool.map { it.name })
        assertFalse(pool.any { it.name == "Drop Pod" })
        assertFalse(pool.any { it.name == "Line Mention Only" })
        assertFalse(pool.any { it.name == "Paradoxical Neophyte's Shade" })
    }

    @Test fun dropPodSummonsOneLineCardOnItsLeftWhenLineExists() {
        val e = engine()
        put(e, "Shinka Psion", e.human, 1, 3)
        assertTrue(e.lastActionError, play(e, "Drop Pod", 1, 2))
        assertEquals("Shinka Psion", e.human.board[1][1]?.card?.name)
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun dropPodDoesNotOverwriteOccupiedLeftSlot() {
        val e = engine()
        val blocked = put(e, "Tracker", e.human, 1, 1)
        put(e, "Shinka Psion", e.human, 1, 3)
        assertTrue(e.lastActionError, play(e, "Drop Pod", 1, 2))
        assertSame(blocked, e.human.board[1][1])
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun xuanWeiFullOpponentBoardDealsDamageButNeverBlocksTurn() {
        val e = engine()
        for (row in 0 until ShadowRules.ROWS)
            for (col in 0 until ShadowRules.COLUMNS)
                put(e, "Tracker", e.cpu, row, col)
        val victim = e.cpu.board[0][0]!!
        assertTrue(e.lastActionError,
            play(e, "Xuan Wei", 1, 2, ShadowEngine.Target(e.cpu, 0, 0)))
        assertEquals(15, victim.power)
        assertSame(victim, e.cpu.board[0][0])
        assertNull(e.pendingMovedUnit)
        assertEquals(0, e.unresolvedEffects)
        assertTrue(e.journal.any { it.contains("No open destination") })
        assertTrue(e.humanTurn)
    }

    @Test fun xuanWeiKillingItsTargetLeavesNoPendingMovement() {
        val e = engine()
        val victim = put(e, "Thrall", e.cpu, 0, 0)
        assertTrue(e.lastActionError,
            play(e, "Xuan Wei", 1, 2, ShadowEngine.Target(e.cpu, 0, 0)))
        assertNull(e.cpu.board[0][0])
        assertNull(e.pendingMovedUnit)
        assertTrue(victim.power <= 0)
    }

    @Test fun cpuPrefersTombShipPositionWithTwoOpenNeighbors() {
        val e = engine()
        e.humanTurn = false
        e.cpu.points = 4
        e.cpu.hand.add(e.catalog.getValue("Tomb Ship"))
        assertTrue(e.cpuPlayOne())
        // The placement cue tracks the MOST RECENT arrival. Tomb Ship
        // summons twice after its hand play, so lastPlacedRow/Column now
        // correctly identify the right-hand summon, not the original card.
        val playEvent = e.journal.single { it.startsWith("CPU ▶ Tomb Ship • ") }
        val playRow = if (playEvent.contains("• BACK ")) 0 else 1
        val playCol = playEvent.substringAfterLast(' ').toInt() - 1
        val ship = e.cpu.board[playRow][playCol]!!
        assertEquals("Tomb Ship", ship.card.name)
        assertTrue(ship.column in 1 until ShadowRules.COLUMNS - 1)
        assertNotNull(e.cpu.board[ship.row][ship.column - 1])
        assertNotNull(e.cpu.board[ship.row][ship.column + 1])
        assertEquals(3, e.placementRevision) // play + two summons
        assertEquals(1, e.lastPlacedSide)
        assertEquals(playRow, e.lastPlacedRow)
        assertEquals(playCol + 1, e.lastPlacedColumn)
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun cpuDropPodUsesAnEligibleLeftSlotInsteadOfWastingSummon() {
        val e = engine()
        e.humanTurn = false
        e.cpu.points = 3
        put(e, "Shinka Psion", e.cpu, 1, 3)
        e.cpu.hand.add(e.catalog.getValue("Drop Pod"))
        assertEquals(2, e.effects.requiredFactionLine("Drop Pod"))
        assertTrue(e.cpuPlayOne())
        assertEquals("Drop Pod", e.cpu.board[1][2]?.card?.name)
        assertEquals("Shinka Psion", e.cpu.board[1][1]?.card?.name)
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun relocationDoesNotCountAsNewEnemyPlacement() {
        val e = engine()
        val watcher = put(e, "Zhiyao", e.human, 0, 1)
        put(e, "Tracker", e.cpu, 0, 0)
        assertTrue(e.lastActionError,
            play(e, "Xuan Wei", 1, 2, ShadowEngine.Target(e.cpu, 0, 0)))
        assertNotNull(e.pendingMovedUnit)
        assertTrue(e.resolveMove(e.human, 1, 4))
        assertEquals(3, watcher.power)
        assertTrue(watcher.timedBonuses.isEmpty())
        assertEquals("Tracker", e.cpu.board[1][4]?.card?.name)
    }
}
