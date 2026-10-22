package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test

/** Opponent deck construction remains playable with sparse faction catalogs. */
class ShadowCpuDeckIntegrityTest {
    private fun card(name: String, faction: String = "Hive",
                     rarity: String = "Rare"): ShadowData.Card =
        ShadowData.Card().apply {
            this.name = name
            this.faction = faction
            this.rarity = rarity
            this.cost = "1"
            this.power = "2"
            this.effect = "No special effects."
        }

    @Test fun eightUniqueFactionCardsBuildLegal15CardDeckUsingCopies() {
        val cards = (0 until 8).associate { index ->
            val c = card("Hive unit $index")
            c.name to c
        }
        val deck = ShadowData.opponentDeck(cards, "Hive")
        assertEquals(15, deck.values.sum())
        assertEquals("", ShadowDeckRules.validate(deck, cards))
        assertEquals(8, deck.size)
        assertTrue(deck.values.all { it in 1..2 })
    }

    @Test fun mixedOpponentDeckUsesPlayableAlternativesWhenPresetsMissing() {
        val cards = (0 until 12).associate { index ->
            val c = card("Other unit $index", "Cabal")
            c.name to c
        }
        val deck = ShadowData.opponentDeck(cards)
        assertEquals(15, deck.values.sum())
        assertEquals("", ShadowDeckRules.validate(deck, cards))
        assertTrue(deck.keys.all { it.startsWith("Other unit ") })
    }

    @Test fun cpuDeckNeverIncludesSummonOnlyTokensOrBreaksRarityCaps() {
        val cards = (0 until 10).map { card("Normal $it") } +
            (0 until 6).map { card("Mythic $it", rarity = "Mythic") } +
            (0 until 5).map { card("Exotic $it", rarity = "Exotic") } +
            listOf(card("Rock").apply { classification = "Summoned unit" })
        val catalog = cards.associateBy { it.name }
        val deck = ShadowData.opponentDeck(catalog, "Hive")
        assertEquals("", ShadowDeckRules.validate(deck, catalog))
        assertEquals(15, deck.values.sum())
        assertEquals(0, deck["Rock"] ?: 0)
        assertTrue(deck.filterKeys { catalog[it]?.rarity == "Mythic" }.values.sum() <= 5)
        assertTrue(deck.filterKeys { catalog[it]?.rarity == "Exotic" }.values.sum() <= 3)
    }

    @Test fun unableToCreateLegalSingleFactionFallsBackToLegalMixedDeck() {
        val cards = (0 until 3).map { card("Hive $it") } +
            (0 until 12).map { card("Cabal $it", "Cabal") }
        val catalog = cards.associateBy { it.name }
        val deck = ShadowData.opponentDeck(catalog, "Hive")
        assertEquals("", ShadowDeckRules.validate(deck, catalog))
        assertTrue(deck.keys.any { it.startsWith("Cabal ") })
    }
}
