package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * Local integration tests using the SAME catalog TSVs shipped in Android.
 *
 * No Android context or GitHub Actions is needed: Gradle's local unit test
 * worker reads the app's assets from the project checkout.
 */
class ShadowFullMatchSmokeTest {
    private fun assetFolder(): File = listOf(
        File("src/main/assets"), File("app/src/main/assets")
    ).firstOrNull { File(it, "verified_video_cards.tsv").isFile }
        ?: error("Card assets not found relative to Gradle's working directory")

    private fun liveCatalog(): Map<String, ShadowData.Card> {
        val root = assetFolder()
        val cards = linkedMapOf<String, ShadowData.Card>()
        fun read(name: String, expanded: Boolean, override: Boolean = false) {
            File(root, name).forEachLine { line ->
                if (line.isBlank()) return@forEachLine
                val fields = line.split('|', limit = if (expanded) 6 else 2)
                if (fields.size != if (expanded) 6 else 2) return@forEachLine
                val existing = cards[fields[0]]
                if (existing != null && !override) return@forEachLine
                val card = existing ?: ShadowData.Card().apply { this.name = fields[0] }
                if (override) card.source = ShadowData.VIDEO_SOURCE
                if (expanded) {
                    card.rarity = fields[1]
                    card.faction = fields[2]
                    card.cost = fields[3]
                    card.power = fields[4]
                    card.effect = fields[5]
                } else card.effect = fields[1]
                cards[card.name] = card
            }
        }
        read("cards.tsv", false)
        read("community_cards.tsv", true)
        read("expanded_cards.tsv", true)
        read("verified_video_cards.tsv", true, override = true)
        File(root, "card_details.tsv").forEachLine { line ->
            val f = line.split('|', limit = 11)
            if (f.size != 10) return@forEachLine
            cards[f[0]]?.let { ShadowData.applyDetailedMetadata(it, f) }
        }
        return cards
    }

    /** Treat a relocated target as a movement choice, never as a new play. */
    private fun resolvePlayerMovement(e: ShadowEngine) {
        if (e.pendingMoveCaster === e.cpu) {
            e.resolveCpuMove()
            return
        }
        if (e.pendingMoveCaster !== e.human) return
        val moving = e.pendingMovedUnit ?: return
        for (row in 0 until ShadowRules.ROWS) for (col in 0 until ShadowRules.COLUMNS) {
            if (e.empty(moving.owner, row, col)) {
                assertTrue("Could not resolve pending relocation: " + e.diagnosticReport(),
                    e.resolveMove(e.human, row, col))
                return
            }
        }
        // An impossible full-board move must not leave a match deadlocked.
        e.endTurn()
        assertNull(e.pendingMovedUnit)
    }

    private fun playPlayerTurn(e: ShadowEngine) {
        var actions = 0
        while (e.humanTurn && !e.finished && actions++ < 15) {
            resolvePlayerMovement(e)
            if (!e.humanTurn) return
            val actionsAvailable = ArrayList<Triple<Int, Int, Int>>()
            for (index in e.human.hand.indices) {
                for (row in 0 until ShadowRules.ROWS)
                    for (col in 0 until ShadowRules.COLUMNS) {
                        if (e.isPlayablePlacement(e.human, index, row, col))
                            actionsAvailable.add(Triple(index, row, col))
                    }
            }
            if (actionsAvailable.isEmpty()) break
            // Spend the most Light we can afford; prefer row-restricted
            // effects where legal targets exist.
            val chosen = actionsAvailable.maxWithOrNull(
                compareBy<Triple<Int, Int, Int>> {
                    e.human.hand[it.first].numericCost()
                }.thenBy {
                    val card = e.human.hand[it.first]
                    if (e.targetOptions(e.human, card, it.second, it.third).isNotEmpty()) 1 else 0
                }
            ) ?: break
            val card = e.human.hand[chosen.first]
            val candidates = e.targetOptions(e.human, card, chosen.second, chosen.third)
            val target = candidates.maxByOrNull { e.power(it.unit()!!) }
            assertTrue(
                "Human play failed: " + e.validatePlay(e.human, chosen.first,
                    chosen.second, chosen.third, target) + "\n" + e.diagnosticReport(),
                e.play(e.human, chosen.first, chosen.second, chosen.third, target)
            )
            resolvePlayerMovement(e)
        }
        assertTrue("Exceeded 15 player actions; potential infinite loop", actions <= 15)
        if (!e.finished && e.humanTurn) e.endTurn()
    }

    private fun playCpuTurn(e: ShadowEngine) {
        var actions = 0
        while (!e.humanTurn && !e.finished && actions++ < 15) {
            if (!e.cpuPlayOne()) break
        }
        assertTrue("CPU exceeded action cap", actions <= 15)
        if (!e.finished && !e.humanTurn) e.endTurn()
    }

    private fun assertBoardInvariant(e: ShadowEngine) {
        for (owner in listOf(e.human, e.cpu)) {
            var occupied = 0
            for (row in 0 until ShadowRules.ROWS)
                for (col in 0 until ShadowRules.COLUMNS) {
                    val u = owner.board[row][col] ?: continue
                    occupied++
                    assertSame("Unit ownership mismatch", owner, u.owner)
                    assertEquals("Row coordinate desynced", row, u.row)
                    assertEquals("Column coordinate desynced", col, u.column)
                    assertTrue("Defeated unit was not swept: " + u.card.name, e.power(u) > 0)
                }
            assertTrue(occupied <= ShadowRules.ROWS * ShadowRules.COLUMNS)
            assertTrue("Negative Light", owner.points >= 0)
        }
    }

    @Test fun liveFullMatchCatalogPreservesRecordedRowRequirements() {
        // These are printed in the user's card-detail recordings; an old
        // card_details.tsv trigger must not silently override the footage.
        val catalog = liveCatalog()
        val engine = ShadowEngine(catalog, 123L)
        for ((name, row) in listOf(
            "Jaren" to ShadowRules.FRONT_ROW,
            "Bazinet" to ShadowRules.BACK_ROW
        )) {
            val card = catalog.getValue(name)
            assertEquals("Missing direct video source for $name",
                ShadowData.VIDEO_SOURCE, card.source)
            assertEquals("Recorded row lost for $name",
                row, ShadowData.videoRequiredRow(card))
            assertTrue("$name should activate in recorded row",
                engine.activeInRow(card, row))
            assertFalse("$name should not activate in opposite row",
                engine.activeInRow(card, 1 - row))
        }
        val harrow = catalog.getValue("Harrow")
        assertEquals(ShadowData.VIDEO_SOURCE, harrow.source)
        assertNull("Harrow's recorded effect has no row restriction",
            ShadowData.videoRequiredRow(harrow))
        assertTrue(engine.activeInRow(harrow, ShadowRules.BACK_ROW))
        assertTrue(engine.activeInRow(harrow, ShadowRules.FRONT_ROW))
    }

    @Test fun everyVideoVerifiedCardUsesRecordedRowHeaderNotLegacyMetadata() {
        val catalog = liveCatalog()
        val evidence = catalog.values.filter { it.source == ShadowData.VIDEO_SOURCE }
        assertTrue("Recorded video overlay unexpectedly missing", evidence.size >= 80)
        val e = ShadowEngine(catalog, 600L)
        for (card in evidence) {
            if (!ShadowData.videoHasExplicitAbilityHeader(card)) continue
            val required = ShadowData.videoRequiredRow(card)
            if (required == null) {
                assertTrue("Invented Back Row restriction: " + card.name,
                    e.activeInRow(card, ShadowRules.BACK_ROW))
                assertTrue("Invented Front Row restriction: " + card.name,
                    e.activeInRow(card, ShadowRules.FRONT_ROW))
            } else {
                assertTrue("Recorded required row rejected: " + card.name,
                    e.activeInRow(card, required))
                assertFalse("Out-of-row effect incorrectly active: " + card.name,
                    e.activeInRow(card, 1 - required))
            }
        }
    }

    @Test fun everyPrintedVideoCardHasValidNumericStatsAndSourceName() {
        val catalog = liveCatalog()
        assertTrue("Unexpectedly short card catalog", catalog.size >= 121)
        assertTrue(catalog.containsKey("Common Cover"))
        assertTrue(catalog.containsKey("Tomb Ship"))
        assertTrue(catalog.containsKey("Xuan Wei"))
        assertTrue(catalog.containsKey("Paradoxical Neophyte's Shade"))
        assertFalse(catalog.getValue("Rock").isPlayable)
        assertFalse(catalog.getValue("Paradoxical Neophyte's Shade").isPlayable)
        for (card in catalog.values) {
            assertTrue("Power missing: " + card.name, card.numericPower() >= 0)
            assertTrue("Cost missing: " + card.name, card.numericCost() > 0)
        }
    }

    @Test fun everyConstructibleCardCanBePlacedWithoutUnresolvedEffectBranches() {
        val catalog = liveCatalog()
        val playable = catalog.values.filter { it.isPlayable }
        assertTrue(playable.size >= 100)
        val support = catalog.getValue("Dreg")
        val opponent = catalog.getValue("Thrall")
        for (card in playable) {
            // Isolate card dispatch from turn economy and unrelated summons.
            val e = ShadowEngine(catalog, 41L)
            e.humanFirst = true
            e.humanTurn = true
            e.human.points = 50
            e.human.board[0][4] = ShadowEngine.Unit(e.human, support, 0, 4)
            e.human.board[1][4] = ShadowEngine.Unit(e.human, support, 1, 4)
            e.cpu.board[0][0] = ShadowEngine.Unit(e.cpu, opponent, 0, 0)
            e.cpu.board[1][0] = ShadowEngine.Unit(e.cpu, opponent, 1, 0)
            val preferredRow =
                if (ShadowData.videoHasExplicitAbilityHeader(card)) {
                    // Match the runtime engine: newer in-game footage outranks
                    // an obsolete row qualifier in community metadata.
                    ShadowData.videoRequiredRow(card) ?: ShadowRules.FRONT_ROW
                } else if (card.trigger.contains("Back Row", ignoreCase = true) ||
                    card.effect.startsWith("When Played (Back Row)", ignoreCase = true)) {
                    ShadowRules.BACK_ROW
                } else ShadowRules.FRONT_ROW
            e.human.hand.add(card)
            val options = e.targetOptions(e.human, card, preferredRow, 2)
            val target = options.maxByOrNull { e.power(it.unit()!!) }
            assertTrue(
                "Card play failed: " + card.name + ": " +
                    e.validatePlay(e.human, 0, preferredRow, 2, target),
                e.play(e.human, 0, preferredRow, 2, target)
            )
            resolvePlayerMovement(e)
            assertBoardInvariant(e)
            assertEquals(
                "Unresolved effect branch in " + card.name + ":\\n" +
                    e.diagnosticReport(), 0, e.unresolvedEffects
            )
        }
    }

    @Test fun fullSixRoundMatchesAgainstEachCpuFactionDoNotSoftLock() {
        val catalog = liveCatalog()
        val deck = ShadowData.opponentDeck(catalog)
        assertEquals("Mixed practice deck invalid",
            "", ShadowDeckRules.validate(deck, catalog))
        for (faction in listOf("Mixed", "Hive", "Fallen", "Cabal", "Humans")) {
            val enemyDeck = ShadowData.opponentDeck(catalog, faction)
            assertEquals("Bad CPU deck for " + faction,
                "", ShadowDeckRules.validate(enemyDeck, catalog))
            for (seed in listOf(0L, 1L, 7L, 13L, 31L, 49L, 1024L)) {
                val e = ShadowEngine(catalog, seed)
                e.start(deck, enemyDeck)
                for (turn in 0 until ShadowRules.ROUNDS * 2) {
                    if (e.humanTurn) playPlayerTurn(e) else playCpuTurn(e)
                    assertBoardInvariant(e)
                    // A match that reaches round six while silently skipping
                    // a printed effect is not a clean gameplay-parity pass.
                    // This cumulative counter remains meaningful even when
                    // older event-log entries roll off.
                    assertEquals(
                        "Unresolved printed card effect in faction=" + faction +
                            " seed=" + seed + " turn=" + (turn + 1) +
                            "\n" + e.diagnosticReport(),
                        0, e.unresolvedEffects
                    )
                    // Any rejected CPU move indicates a targeting/placement
                    // legality mismatch that can waste remaining Light.
                    assertFalse(
                        "CPU attempted an invalid move: faction=" + faction +
                            " seed=" + seed + "\\n" + e.diagnosticReport(),
                        e.rejectedCpuMoves > 0
                    )
                    if (!e.finished) {
                        assertNull("Pending relocation after end of turn", e.pendingMovedUnit)
                    }
                }
                assertTrue(
                    "Match stuck: faction=" + faction + " seed=" + seed +
                        "\n" + e.diagnosticReport(), e.finished
                )
                assertTrue(e.result() in setOf("VICTORY", "DEFEAT", "DRAW"))
                assertEquals(
                    "Match must finish after exactly twelve player turns",
                    ShadowRules.ROUNDS * 2,
                    (e.round - 1) * 2 + e.completedTurns
                )
                assertNull("Relocation left pending at final score", e.pendingMovedUnit)
                assertEquals("CPU rejected a move during the match", 0, e.rejectedCpuMoves)
                assertEquals("Unresolved gameplay effects at final score", 0, e.unresolvedEffects)
                assertEquals(
                    "Final player score differs from visible board Power",
                    e.human.board.flatMap { it.toList() }.filterNotNull()
                        .sumOf { e.power(it) },
                    e.score(e.human)
                )
                assertEquals(
                    "Final CPU score differs from visible board Power",
                    e.cpu.board.flatMap { it.toList() }.filterNotNull()
                        .sumOf { e.power(it) },
                    e.score(e.cpu)
                )
            }
        }
    }
}
