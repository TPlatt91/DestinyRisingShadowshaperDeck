package com.tplatt.shadowshaper

/**
 * Shared filtering rules for Deck Construction's Card Gallery and Card Collection.
 * Values are derived from the loaded catalog; no faction, keyword or stat values
 * are invented or silently left out of a hard-coded dropdown.
 */
internal data class ShadowLibraryCriteria(
    val search: String = "",
    val faction: String = "All",
    val rarity: String = "All",
    val keyword: String = "All",
    val light: String = "All",
    val power: String = "All",
    val type: String = "All"
) {
    val hasSelection: Boolean
        get() = search.isNotBlank() || listOf(faction, rarity, keyword, light, power, type)
            .any { it != "All" }

    val advancedCount: Int
        get() = listOf(keyword, light, power, type).count { it != "All" }
}

internal data class ShadowLibraryOptions(
    val factions: List<String>,
    val rarities: List<String>,
    val keywords: List<String>,
    val light: List<String>,
    val power: List<String>,
    val types: List<String>
)

internal object ShadowLibraryFiltering {
    private fun labels(cards: List<ShadowData.Card>, property: (ShadowData.Card) -> String): List<String> =
        listOf("All") + cards.map(property).map { it.trim() }
            .filter { it.isNotEmpty() && !it.equals("All", ignoreCase = true) }
            .distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)

    private fun stats(cards: List<ShadowData.Card>, property: (ShadowData.Card) -> Int): List<String> =
        listOf("All") + cards.map(property).filter { it >= 0 }
            .distinct().sorted().map(Int::toString)

    fun options(
        cards: List<ShadowData.Card>,
        glossary: List<ShadowKeywordGlossary.Entry>
    ): ShadowLibraryOptions {
        val cardKeywords = cards.flatMap { ShadowKeywordGlossary.onCard(it, glossary) }
            .map { it.name }.distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)
        return ShadowLibraryOptions(
            factions = labels(cards) { it.faction },
            rarities = labels(cards) { it.rarity },
            keywords = listOf("All") + cardKeywords,
            light = stats(cards) { it.numericCost() },
            power = stats(cards) { it.numericPower() },
            types = labels(cards) { it.classification }
        )
    }

    fun matches(
        card: ShadowData.Card,
        criteria: ShadowLibraryCriteria,
        glossary: List<ShadowKeywordGlossary.Entry>
    ): Boolean {
        if (criteria.faction != "All" && !card.faction.equals(criteria.faction, true)) return false
        if (criteria.rarity != "All" && !card.rarity.equals(criteria.rarity, true)) return false
        if (criteria.type != "All" && !card.classification.equals(criteria.type, true)) return false
        if (criteria.light != "All" && card.numericCost().toString() != criteria.light) return false
        if (criteria.power != "All" && card.numericPower().toString() != criteria.power) return false
        if (criteria.keyword != "All" &&
            ShadowKeywordGlossary.onCard(card, glossary).none { it.name.equals(criteria.keyword, true) }
        ) return false
        val search = criteria.search.trim()
        if (search.isEmpty()) return true
        return listOf(
            card.name, card.effect, card.trigger, card.faction, card.rarity,
            card.classification, card.cardNumber
        ).any { it.contains(search, ignoreCase = true) }
    }
}
