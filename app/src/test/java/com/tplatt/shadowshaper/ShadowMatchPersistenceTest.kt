package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * Pure JVM tests: no Android SDK emulator or GitHub Actions.
 * A restored battle must preserve RNG, hidden deck order and effect identity.
 */
class ShadowMatchPersistenceTest {
    private fun card(name: String) = ShadowData.Card().apply {
        this.name = name
        rarity = "Rare"
        faction = "Humans"
        cost = "1"
        power = "3"
        effect = ""
    }

    private fun catalog(): Map<String, ShadowData.Card> =
        (1..17).associate { "Test Card $it" to card("Test Card $it") }

    private fun serializedCopy(saved: ShadowMatchState): ShadowMatchState {
        val bytes = ByteArrayOutputStream().apply {
            ObjectOutputStream(this).use { it.writeObject(saved) }
        }.toByteArray()
        return ObjectInputStream(ByteArrayInputStream(bytes)).use {
            it.readObject() as ShadowMatchState
        }
    }

    @Test fun restoresBothHandsDeckOrderAndExactFutureRandomNumbers() {
        val catalog = catalog()
        val e = ShadowEngine(catalog, 727272L)
        e.human.drawPile.addAll((1..11).map { catalog.getValue("Test Card $it") })
        e.cpu.drawPile.addAll((3..16).map { catalog.getValue("Test Card $it") })
        e.human.hand.addAll(listOf(catalog.getValue("Test Card 2"),
            catalog.getValue("Test Card 9")))
        e.cpu.hand.add(catalog.getValue("Test Card 15"))
        e.humanFirst = true
        e.humanTurn = true
        e.round = 3
        e.human.points = 2
        e.cpu.points = 3
        e.rng.nextInt(10000)
        e.rng.nextDouble()
        e.journal.addAll(listOf("MATCH SEED: 727272", "YOU ▶ Test Card 2"))
        e.fullJournal.addAll(e.journal)
        e.fullJournal.add("EARLY MATCH EVENT")
        val snapshot = serializedCopy(ShadowMatchCodec.capture(e))
        val resumed = ShadowMatchCodec.restore(snapshot, catalog)
        assertEquals(3, resumed.round)
        assertEquals(2, resumed.human.points)
        assertEquals(3, resumed.cpu.points)
        assertEquals(e.human.hand.map { it.name }, resumed.human.hand.map { it.name })
        assertEquals(e.cpu.hand.map { it.name }, resumed.cpu.hand.map { it.name })
        assertEquals(e.human.drawPile.map { it.name }, resumed.human.drawPile.map { it.name })
        assertEquals(e.cpu.drawPile.map { it.name }, resumed.cpu.drawPile.map { it.name })
        assertEquals(e.journal, resumed.journal)
        assertEquals(e.fullJournal, resumed.fullJournal)
        repeat(30) { assertEquals(e.rng.nextInt(), resumed.rng.nextInt()) }
    }

    @Test fun restoresScorchAndSniperIdentityLinksWithoutReplayingEffects() {
        val cards = catalog()
        val engine = ShadowEngine(cards, 382L)
        engine.humanTurn = true
        engine.round = 2
        val guardian = ShadowEngine.Unit(engine.human, cards.getValue("Test Card 1"), 0, 6)
        val enemy = ShadowEngine.Unit(engine.cpu, cards.getValue("Test Card 2"), 1, 4)
        val oldSource = ShadowEngine.Unit(engine.cpu, cards.getValue("Test Card 3"), 0, 1)
        engine.human.board[0][6] = guardian
        engine.cpu.board[1][4] = enemy
        guardian.power = 7
        guardian.bonus = 4
        guardian.inspired = true
        guardian.overshield = true
        guardian.resilientCaptainDregs = 2
        guardian.damageBonus = 1
        guardian.bonusDoublerConsumed = true
        guardian.damageDoublerConsumed = true
        guardian.sniperTurnsRemaining = 1
        guardian.sniperLastHitTurn[enemy] = 4
        guardian.scorchEffects.add(ShadowEngine.ScorchEffect(oldSource, 2, 2))
        guardian.scorchSource = oldSource
        guardian.scorchTurns = 2
        guardian.scorched = 2
        guardian.timedBonuses.add(ShadowEngine.TimedBonus(3, 2))
        guardian.fuseOrigin = engine.cpu
        guardian.fuseRound = 2
        engine.pendingMovedUnit = guardian
        engine.pendingMoveCaster = engine.human
        engine.placementRevision = 8
        engine.lastPlacedSide = 0
        engine.lastPlacedRow = 0
        engine.lastPlacedColumn = 6
        val resumed = ShadowMatchCodec.restore(
            serializedCopy(ShadowMatchCodec.capture(engine)), cards
        )
        val u = resumed.human.board[0][6]!!
        val foe = resumed.cpu.board[1][4]!!
        assertEquals(7, u.power)
        assertEquals(4, u.bonus)
        assertTrue(u.inspired)
        assertTrue(u.overshield)
        assertEquals(2, u.resilientCaptainDregs)
        assertTrue(u.bonusDoublerConsumed)
        assertTrue(u.damageDoublerConsumed)
        assertEquals(1, u.sniperTurnsRemaining)
        assertEquals(4, u.sniperLastHitTurn[foe])
        assertEquals(1, u.scorchEffects.size)
        assertEquals("Test Card 3", u.scorchEffects[0].source?.card?.name)
        assertSame(u.scorchSource, u.scorchEffects[0].source)
        assertEquals(2, u.scorchEffects[0].turns)
        assertEquals(3, u.timedBonuses.single().value)
        assertEquals(2, u.timedBonuses.single().turns)
        assertSame(resumed.cpu, u.fuseOrigin)
        assertSame(u, resumed.pendingMovedUnit)
        assertSame(resumed.human, resumed.pendingMoveCaster)
        assertEquals(8, resumed.placementRevision)
        assertEquals(6, resumed.lastPlacedColumn)
        assertTrue(resumed.journal.isEmpty())
    }

    @Test fun newMatchRebuildsAndReshufflesBothDecks() {
        val cards = catalog()
        val deck = (1..15).associate { "Test Card $it" to 1 }
        val first = ShadowEngine(cards, 413528L)
        val replay = ShadowEngine(cards, 948192L)
        first.start(deck, deck)
        replay.start(deck, deck)
        assertEquals(15, first.human.drawPile.size + first.human.hand.size)
        assertEquals(15, replay.human.drawPile.size + replay.human.hand.size)
        assertEquals(15, first.cpu.drawPile.size + first.cpu.hand.size)
        assertEquals(15, replay.cpu.drawPile.size + replay.cpu.hand.size)
        assertEquals(1, replay.round)
        assertEquals(0, replay.placementRevision)
        assertTrue(replay.human.board.all { row -> row.all { it == null } })
        assertTrue(replay.cpu.board.all { row -> row.all { it == null } })
        val orderFirst = first.human.hand.map { it.name } +
            first.human.drawPile.map { it.name } +
            first.cpu.hand.map { it.name } + first.cpu.drawPile.map { it.name }
        val orderReplay = replay.human.hand.map { it.name } +
            replay.human.drawPile.map { it.name } +
            replay.cpu.hand.map { it.name } + replay.cpu.drawPile.map { it.name }
        assertNotEquals("New seed must produce newly shuffled deck order",
            orderFirst, orderReplay)
    }

    @Test fun versionAndMissingCardsFailClosedInsteadOfCorruptingMatch() {
        val cards = catalog()
        val engine = ShadowEngine(cards, 51L)
        engine.human.hand.add(cards.getValue("Test Card 1"))
        val saved = ShadowMatchCodec.capture(engine)
        assertThrows(IllegalArgumentException::class.java) {
            ShadowMatchCodec.restore(saved.copy(format = 900), cards)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ShadowMatchCodec.restore(saved, emptyMap())
        }
    }
}
