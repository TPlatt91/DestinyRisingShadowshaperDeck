package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test

/** CPU validation failures must be visible after long matches. */
class ShadowCpuDiagnosticsTest {
    private fun card(name: String) = ShadowData.Card().apply {
        this.name = name
        faction = "Humans"
        rarity = "Rare"
        cost = "1"
        power = "2"
        effect = "No special effects."
    }

    @Test fun cpuRejectionCountPersistsAfterJournalRollsOver() {
        val e = ShadowEngine(mapOf("Dreg" to card("Dreg")), 5L)
        e.rejectedCpuMoves = 3
        repeat(140) { e.log("Event " + it) }
        assertEquals(90, e.journal.size)
        assertEquals(3, e.rejectedCpuMoves)
        assertTrue(e.diagnosticReport().contains("Rejected CPU moves: 3"))
    }

    @Test fun restartingMatchResetsCpuRejectionCount() {
        val cards = (0 until 15).associate { i ->
            val c = card("Card " + i)
            c.name to c
        }
        val deck = cards.keys.associateWith { 1 }
        val e = ShadowEngine(cards, 5L)
        e.rejectedCpuMoves = 6
        e.start(deck, deck)
        assertEquals(0, e.rejectedCpuMoves)
        assertTrue(e.diagnosticReport().contains("Rejected CPU moves: 0"))
    }

    @Test fun successfulCpuPlacementDoesNotChangeRejectionCount() {
        val cards = mapOf("Dreg" to card("Dreg"))
        val e = ShadowEngine(cards, 5L)
        e.cpu.points = 1
        e.humanTurn = false
        e.cpu.hand.add(cards.getValue("Dreg"))
        assertTrue(e.cpuPlayOne())
        assertEquals(0, e.rejectedCpuMoves)
        assertTrue(e.cpu.hand.isEmpty())
    }
}
