package com.tplatt.shadowshaper

/**
 * Kotlin source of truth for constructed-deck legality.
 * Pure game logic: independently testable without an Activity or Compose.
 */
object ShadowDeckRules {
    @JvmStatic
    fun validate(deck: Map<String, Int>, catalog: Map<String, ShadowData.Card>): String {
        var total = 0
        var exotic = 0
        var mythic = 0
        for ((name, quantity) in deck) {
            val card = catalog[name] ?: return "Unknown card: $name"
            if (quantity !in 1..ShadowRules.COPY_LIMIT) return "Maximum two copies of ${card.name}"
            if (!card.isPlayable) return "${card.name} is a token or missing its Power / cost."
            total += quantity
            if (card.rarity.equals("Exotic", ignoreCase = true)) exotic += quantity
            if (card.rarity.equals("Mythic", ignoreCase = true)) mythic += quantity
        }
        if (total != ShadowRules.DECK_SIZE)
            return "A playable deck needs exactly 15 cards ($total/15)."
        if (exotic > ShadowRules.EXOTIC_LIMIT)
            return "No more than 3 Exotic cards (currently $exotic)."
        if (mythic > ShadowRules.MYTHIC_LIMIT)
            return "No more than 5 Mythic cards (currently $mythic)."
        return ""
    }

    /** Used by both Compose and the original engine; incomplete decks can be saved. */
    @JvmStatic
    fun canAdd(deck: Map<String, Int>, card: ShadowData.Card, catalog: Map<String, ShadowData.Card>): String {
        if (!card.isPlayable) return "${card.name} cannot be added (token or incomplete stats)."
        if ((deck[card.name] ?: 0) >= ShadowRules.COPY_LIMIT) return "Maximum 2 copies of ${card.name}."
        val next = deck.toMutableMap().apply { put(card.name, (deck[card.name] ?: 0) + 1) }
        if (next.values.sum() > ShadowRules.DECK_SIZE) return "Deck limit: 15 cards."
        val exotic = next.entries.sumOf { (name, count) ->
            if (catalog[name]?.rarity.equals("Exotic", true)) count else 0
        }
        if (exotic > ShadowRules.EXOTIC_LIMIT) return "Maximum 3 Exotic cards."
        val mythic = next.entries.sumOf { (name, count) ->
            if (catalog[name]?.rarity.equals("Mythic", true)) count else 0
        }
        if (mythic > ShadowRules.MYTHIC_LIMIT) return "Maximum 5 Mythic cards."
        return ""
    }
}
