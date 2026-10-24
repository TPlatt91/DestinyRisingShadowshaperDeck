package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test

class ShadowLibraryFilteringTest {
    private val glossary = listOf(
        ShadowKeywordGlossary.Entry("When Played", "Recorded", true),
        ShadowKeywordGlossary.Entry("Sustained", "Recorded", true),
        ShadowKeywordGlossary.Entry("Inspired", "Recorded", true),
        ShadowKeywordGlossary.Entry("Line", "Recorded", true)
    )

    private fun card(
        name: String,
        faction: String,
        rarity: String,
        light: String,
        power: String,
        text: String,
        type: String = "Common"
    ): ShadowData.Card = ShadowData.Card().apply {
        this.name = name
        this.faction = faction
        this.rarity = rarity
        this.cost = light
        this.power = power
        this.effect = text
        this.classification = type
    }

    private val cards by lazy {
        listOf(
            card("Ketch", "Fallen", "Mythic", "2", "3",
                "When Played: Summons 2 Dregs."),
            card("Tenacious Hexguard", "Hive", "Mythic", "2", "3",
                "When Played: Grants Inspired to nearby units."),
            card("Legionary", "Cabal", "Rare", "1", "2",
                "Sustained (Column): Gains +1 Power."),
            card("Saladin", "Humans", "Exotic", "4", "6",
                "Sustained: Buffs allied Humans.", "Hero"),
            card("Rock", "Humans", "Rare", "1", "1", "",
                "Summoned unit")
        )
    }

    @Test fun offersOnlyValuesPresentInCatalog() {
        val options = ShadowLibraryFiltering.options(cards, glossary)
        assertEquals(listOf("All", "Cabal", "Fallen", "Hive", "Humans"), options.factions)
        assertEquals(listOf("All", "Exotic", "Mythic", "Rare"), options.rarities)
        assertEquals(listOf("All", "1", "2", "4"), options.light)
        assertEquals(listOf("All", "1", "2", "3", "6"), options.power)
        assertTrue(options.keywords.contains("When Played"))
        assertTrue(options.keywords.contains("Inspired"))
        assertFalse(options.keywords.contains("Line"))
        assertTrue(options.types.contains("Summoned unit"))
    }

    @Test fun combinesFactionKeywordLightAndPower() {
        val exact = ShadowLibraryCriteria(
            faction = "Hive", rarity = "Mythic", keyword = "When Played",
            light = "2", power = "3"
        )
        assertEquals(
            listOf("Tenacious Hexguard"),
            cards.filter { ShadowLibraryFiltering.matches(it, exact, glossary) }.map { it.name }
        )
        assertFalse(ShadowLibraryFiltering.matches(cards[0], exact, glossary))
        assertFalse(ShadowLibraryFiltering.matches(cards[2], exact, glossary))
        assertFalse(ShadowLibraryFiltering.matches(cards[3], exact, glossary))
    }

    @Test fun keywordMatchesPrintedEffectOrTriggerButNotUnrelatedCards() {
        val inspired = ShadowLibraryCriteria(keyword = "Inspired")
        assertTrue(ShadowLibraryFiltering.matches(cards[1], inspired, glossary))
        assertFalse(ShadowLibraryFiltering.matches(cards[0], inspired, glossary))
        val cardWithTrigger = cards[0].apply { trigger = "Sustained" }
        assertTrue(ShadowLibraryFiltering.matches(
            cardWithTrigger, ShadowLibraryCriteria(keyword = "Sustained"), glossary
        ))
    }

    @Test fun searchAndTypeFilterAndResetWork() {
        val criteria = ShadowLibraryCriteria(
            search = "salad", type = "Hero", light = "4", power = "6"
        )
        assertEquals("Saladin", cards.single {
            ShadowLibraryFiltering.matches(it, criteria, glossary)
        }.name)
        assertFalse(ShadowLibraryFiltering.matches(
            cards[3], criteria.copy(power = "1"), glossary
        ))
        assertTrue(criteria.hasSelection)
        assertEquals(3, criteria.advancedCount)
        assertEquals(5, cards.count {
            ShadowLibraryFiltering.matches(it, ShadowLibraryCriteria(), glossary)
        })
        assertFalse(ShadowLibraryCriteria().hasSelection)
    }

    @Test fun summonOnlyCardsStayOutOfDeckGalleryButRemainInCollection() {
        assertFalse(cards.last().isPlayable)
        assertTrue(ShadowLibraryFiltering.matches(
            cards.last(), ShadowLibraryCriteria(type = "Summoned unit"), glossary
        ))
        val deckCards = cards.filter { it.isPlayable }
        assertFalse(ShadowLibraryFiltering.options(deckCards, glossary)
            .types.contains("Summoned unit"))
    }

    @Test fun unknownStatsOnlyAppearWithAllSelected() {
        val unknown = card("Unknown", "Hive", "Rare", "", "", "No special effects.")
        assertTrue(ShadowLibraryFiltering.matches(
            unknown, ShadowLibraryCriteria(), glossary
        ))
        assertFalse(ShadowLibraryFiltering.matches(
            unknown, ShadowLibraryCriteria(light = "1"), glossary
        ))
        assertFalse(ShadowLibraryFiltering.matches(
            unknown, ShadowLibraryCriteria(power = "2"), glossary
        ))
    }
}
