package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test

/** Failure reasons must survive full matches, even when the battle log rolls. */
class ShadowUnresolvedEffectDiagnosticsTest {
    private fun card(name: String) = ShadowData.Card().apply {
        this.name = name
        rarity = "Rare"
        faction = "Humans"
        cost = "1"
        power = "2"
        effect = "No special effects."
    }

    @Test fun firstUnresolvedCardRemainsIdentifiableAfterJournalRollover() {
        val testCard = card("Audit Subject")
        val e = ShadowEngine(mapOf(testCard.name to testCard), 42L)
        e.incomplete(testCard, "Missing deterministic printed branch")
        repeat(120) { e.log("Normal battle event " + it) }

        assertFalse(e.journal.any { it.startsWith("UNIMPLEMENTED:") })
        assertEquals(1, e.unresolvedEffects)
        assertEquals(1, e.unresolvedEffectDetails.size)
        assertTrue(e.unresolvedEffectDetails.single().contains("Audit Subject"))
        assertTrue(e.diagnosticReport().contains(
            "UNIMPLEMENTED: Audit Subject — Missing deterministic printed branch"
        ))
    }

    @Test fun overflowPreservesFirstSixtyFourReasonsAndTotalCount() {
        val testCard = card("Repeated Branch")
        val e = ShadowEngine(mapOf(testCard.name to testCard), 42L)
        repeat(75) { e.incomplete(testCard, "Case " + it) }
        repeat(120) { e.log("Later battle event " + it) }

        assertEquals(75, e.unresolvedEffects)
        assertEquals(64, e.unresolvedEffectDetails.size)
        assertTrue(e.unresolvedEffectDetails.first().endsWith("Case 0"))
        assertTrue(e.unresolvedEffectDetails.last().endsWith("Case 63"))
        assertTrue(e.diagnosticReport().contains(
            "UNRESOLVED EFFECT REASONS (first 64)"
        ))
    }

    @Test fun restartingMatchClearsPreviousUnresolvedReasonsAndCounts() {
        val cards = (0 until ShadowRules.DECK_SIZE).associate { i ->
            val item = card("Practice Unit " + i)
            item.name to item
        }
        val deck = cards.keys.associateWith { 1 }
        val e = ShadowEngine(cards, 42L)
        val example = cards.values.first()
        e.incomplete(example, "Unverified move")
        assertEquals(1, e.unresolvedEffects)
        assertFalse(e.unresolvedEffectDetails.isEmpty())

        e.start(deck, deck)

        assertEquals(0, e.unresolvedEffects)
        assertTrue(e.unresolvedEffectDetails.isEmpty())
        assertFalse(e.diagnosticReport().contains("UNRESOLVED EFFECT REASONS"))
    }
}
