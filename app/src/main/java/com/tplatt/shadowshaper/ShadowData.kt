package com.tplatt.shadowshaper

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.LinkedHashMap

/**
 * Single Kotlin data bridge used by Compose, battle engine and original unit tests.
 * The three on-device catalog TSV files and original saved-deck JSON remain intact.
 */
object ShadowData {
    /** Direct in-game card-detail captures outrank community/site metadata. */
    const val VIDEO_SOURCE = "User-supplied Shadowshaper card-detail video"
    /**
     * Effect evidence described in an uploaded-video note without a complete
     * cost/Power/effect TSV overlay. These are NOT full-stat video overrides.
     */
    const val VIDEO_NOTES_SOURCE = "User-supplied Shadowshaper gameplay video notes"
    private val recordedVideoNote = Regex(
        """(?:recorded in-game video|recording screen-|original[^|]*video|card video|video screen|user card-detail video)""",
        RegexOption.IGNORE_CASE
    )
    const val DECK_SIZE = ShadowRules.DECK_SIZE
    const val COPIES_LIMIT = ShadowRules.COPY_LIMIT
    const val EXOTIC_LIMIT = ShadowRules.EXOTIC_LIMIT
    const val MYTHIC_LIMIT = ShadowRules.MYTHIC_LIMIT

    class Card {
        @JvmField var name = ""
        @JvmField var rarity = ""
        @JvmField var faction = ""
        @JvmField var cost = ""
        @JvmField var power = ""
        @JvmField var effect = ""
        @JvmField var classification = ""
        @JvmField var trigger = ""
        @JvmField var cardNumber = ""
        @JvmField var acquisition = ""
        @JvmField var notes = ""
        @JvmField var source = ""
        @JvmField var onlineArt = false

        fun numericCost(): Int = cost.toIntOrNull() ?: -1
        fun numericPower(): Int = power.toIntOrNull() ?: -1
        val isPlayable: Boolean get() =
            numericCost() > 0 && numericPower() >= 0 &&
                // Rock can only enter via a summoning ability (Xur or Common Cover),
                // even when an old catalog omits its "Summoned unit" classification.
                !name.equals("Rock", ignoreCase = true) &&
                !classification.equals("Summoned unit", ignoreCase = true) &&
                !cost.equals("Token", ignoreCase = true)
    }

    @JvmStatic
    fun load(context: Context): LinkedHashMap<String, Card> {
        val result = linkedMapOf<String, Card>()
        readCatalog(context, result, "cards.tsv", expanded = false)
        readCatalog(context, result, "community_cards.tsv", expanded = true)
        readCatalog(context, result, "expanded_cards.tsv", expanded = true)
        // Captured directly from user-supplied in-game card-detail recordings.
        readCatalog(context, result, "verified_video_cards.tsv", expanded = true)
        try {
            context.assets.open("card_details.tsv").bufferedReader(StandardCharsets.UTF_8).use { reader ->
                reader.forEachLine { line ->
                    val fields = line.split('|', limit = 11)
                    if (fields.size != 10) return@forEachLine
                    val card = result[fields[0]] ?: return@forEachLine
                    applyDetailedMetadata(card, fields)
                }
            }
        } catch (_: Exception) {
            // Missing optional metadata never removes the base card catalog.
        }
        return LinkedHashMap(result)
    }

    /**
     * Applies acquisition/classification fields without allowing old metadata
     * to overwrite a verified in-game card's rarity, faction, Light or Power.
     *
     * Card-detail videos supply the printed effect in verified_video_cards.tsv;
     * card_details.tsv is a separate annotation layer, not a source override.
     */
    internal fun applyDetailedMetadata(card: Card, fields: List<String>) {
        if (fields.size != 10) return
        if (card.source != VIDEO_SOURCE) {
            card.faction = fields[1]
            card.rarity = fields[2]
            card.cost = fields[3]
            card.power = fields[4]
        }
        card.classification = fields[5]
        card.trigger = fields[6]
        card.acquisition = fields[7]
        card.notes = fields[8]
        card.cardNumber = fields[9]
        // The detail note may document direct original-game footage even for
        // a card missing from verified_video_cards.tsv. Mark evidence level
        // honestly and use its noted effect ahead of older website triggers.
        if (card.source != VIDEO_SOURCE && recordedVideoNote.containsMatchIn(card.notes))
            card.source = VIDEO_NOTES_SOURCE
    }

    internal fun hasVideoEffectEvidence(card: Card): Boolean =
        card.source == VIDEO_SOURCE || card.source == VIDEO_NOTES_SOURCE

    /**
     * Only the recorded effect text may introduce a row restriction for
     * video-verified cards. When the recorded card says "When Played:" with
     * no row condition, older community trigger metadata must not invent one.
     *
     * Returns the required row, or null when there is no printed requirement
     * or this entry has no verified video effect.
     */
    internal fun videoRequiredRow(card: Card): Int? {
        if (!hasVideoEffectEvidence(card)) return null
        val header = card.effect.substringBefore(':').trim()
        val match = Regex(
            """\b(?:when played|sustained)\s*\([^)]*\b(back|front)\s+row\b[^)]*\)""",
            RegexOption.IGNORE_CASE
        ).find(header) ?: return null
        return if (match.groupValues[1].equals("back", true))
            ShadowRules.BACK_ROW else ShadowRules.FRONT_ROW
    }

    internal fun videoHasExplicitAbilityHeader(card: Card): Boolean {
        if (!hasVideoEffectEvidence(card)) return false
        val header = card.effect.substringBefore(':').trim()
        return header.startsWith("When Played", true) ||
            header.startsWith("Sustained", true) ||
            card.effect.contains("no special effects", true)
    }

    private fun readCatalog(
        context: Context,
        result: MutableMap<String, Card>,
        file: String,
        expanded: Boolean
    ) {
        try {
            context.assets.open(file).bufferedReader(StandardCharsets.UTF_8).use { reader ->
                reader.forEachLine { line ->
                    val fields = line.split('|', limit = if (expanded) 6 else 2)
                    if (fields.size != if (expanded) 6 else 2) return@forEachLine
                    // A recorded card-detail view overrides earlier community/wiki text.
                    // This retains each card's identity in saved decks and collection filters.
                    if (fields[0] in result) {
                        if (file == "verified_video_cards.tsv") {
                            val existing = result.getValue(fields[0])
                            existing.rarity = fields[1]
                            existing.faction = fields[2]
                            existing.cost = fields[3]
                            existing.power = fields[4]
                            existing.effect = fields[5]
                            existing.source = VIDEO_SOURCE
                        }
                        return@forEachLine
                    }
                    val card = Card()
                    card.name = fields[0]
                    card.onlineArt = !expanded
                    card.source = when (file) {
                        "verified_video_cards.tsv" -> VIDEO_SOURCE
                        "cards.tsv" -> "https://www.todayindestiny.com/rising/shadowshaper_duels/cards"
                        else -> "https://www.reddit.com/r/ShadowshaperDuels/comments/1nklv84/"
                    }
                    if (expanded) {
                        card.rarity = fields[1]
                        card.faction = fields[2]
                        card.cost = fields[3]
                        card.power = fields[4]
                        card.effect = fields[5]
                        if (card.cost.equals("Token", ignoreCase = true))
                            card.classification = "Summoned unit"
                    } else {
                        card.effect = fields[1]
                    }
                    result[card.name] = card
                }
            }
        } catch (_: Exception) {
            // The other catalog files still load when a supplemental list is absent.
        }
    }

    @JvmStatic
    fun readDeck(context: Context, name: String): LinkedHashMap<String, Int> =
        LinkedHashMap(ShadowDeckRepository(context).read(name))

    @JvmStatic
    fun deckNames(context: Context): ArrayList<String> =
        ArrayList(ShadowDeckRepository(context).deckNames())

    @JvmStatic
    fun selectedDeck(context: Context): String = ShadowDeckRepository(context).selectedDeck()

    @JvmStatic
    fun validate(deck: Map<String, Int>, catalog: Map<String, Card>): String =
        ShadowDeckRules.validate(deck, catalog)

    @JvmStatic
    fun expanded(deck: Map<String, Int>, catalog: Map<String, Card>): ArrayList<Card> {
        val output = ArrayList<Card>()
        deck.forEach { (name, count) ->
            catalog[name]?.let { card ->
                repeat(count.coerceIn(0, 15)) { output.add(card) }
            }
        }
        return output
    }

    /**
     * Build a legal CPU deck using actual card identities. A faction with
     * fewer than 15 distinct playable cards may still form a legal 15-card
     * deck with up to two copies of its available cards.
     *
     * Older code counted *names*, not *copies*, and silently fell back to a
     * mixed deck (or an empty deck) even when a legal faction deck existed.
     */
    private fun completeCpuDeck(
        catalog: Map<String, Card>, preferredNames: List<String>, candidates: List<Card>
    ): LinkedHashMap<String, Int> {
        val result = linkedMapOf<String, Int>()
        val eligible = candidates.filter { it.isPlayable }.distinctBy { it.name }
        val priority = (preferredNames.mapNotNull { catalog[it] }.filter { it in eligible } +
            eligible).distinctBy { it.name }
        var exotic = 0
        var mythic = 0
        var total = 0
        // First take one copy of each preferred card, then fill using the
        // permitted second copy. Both passes respect deck rarity caps.
        for (pass in 1..2) {
            for (card in priority) {
                if (total >= DECK_SIZE) break
                if ((result[card.name] ?: 0) >= pass) continue
                if (card.rarity.equals("Exotic", true) && exotic >= EXOTIC_LIMIT) continue
                if (card.rarity.equals("Mythic", true) && mythic >= MYTHIC_LIMIT) continue
                result[card.name] = (result[card.name] ?: 0) + 1
                if (card.rarity.equals("Exotic", true)) exotic++
                if (card.rarity.equals("Mythic", true)) mythic++
                total++
            }
        }
        return if (total == DECK_SIZE && ShadowDeckRules.validate(result, catalog).isEmpty())
            LinkedHashMap(result) else linkedMapOf()
    }

    @JvmStatic
    fun opponentDeck(catalog: Map<String, Card>, faction: String?): LinkedHashMap<String, Int> {
        if (faction.isNullOrBlank() || faction.equals("Mixed", ignoreCase = true))
            return opponentDeck(catalog)
        val choices = catalog.values
            .filter { it.isPlayable && it.faction.equals(faction, ignoreCase = true) }
            .sortedWith(compareBy<Card> { it.numericCost() }.thenBy { it.name })
        val factionDeck = completeCpuDeck(catalog, emptyList(), choices)
        return if (factionDeck.isNotEmpty()) factionDeck else opponentDeck(catalog)
    }

    @JvmStatic
    fun opponentDeck(catalog: Map<String, Card>): LinkedHashMap<String, Int> {
        val presets = listOf(
            "Dreg", "Resilient Dreg", "Novice Marauder", "Acolyte", "Thrall",
            "Legionary", "War Beast", "Vandal", "Rock Pile", "Shank",
            "Marauder", "Guerrilla", "Zhiyao", "Shinka Legionary",
            "Donovan-17", "Mu Ying", "Sparrow", "Resilient Captain",
            "Scorpius", "Knight", "Carrier"
        )
        val alternatives = catalog.values.filter { it.isPlayable }
            .sortedWith(compareBy<Card> { it.numericCost() }.thenBy { it.name })
        return completeCpuDeck(catalog, presets, alternatives)
    }
}
