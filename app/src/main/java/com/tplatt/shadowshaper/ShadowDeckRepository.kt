package com.tplatt.shadowshaper

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Uses EXACTLY the same SharedPreferences filename/keys and JSON schema as
 * MainActivity.java so pre-migration decks remain accessible, not converted
 * or deleted.
 */
class ShadowDeckRepository(context: Context) {
    private val preferences = context.getSharedPreferences("MainActivity", Context.MODE_PRIVATE)

    fun deckNames(): List<String> {
        val input = preferences.getString("deck_names", """["My Deck"]""") ?: """["My Deck"]"""
        return runCatching {
            val array = JSONArray(input)
            (0 until array.length()).map { array.getString(it) }
                .filter { it.isNotBlank() }.distinct()
                .ifEmpty { listOf("My Deck") }
        }.getOrDefault(listOf("My Deck"))
    }

    fun selectedDeck(): String = preferences.getString("selected_deck", "My Deck")
        ?.takeIf { it in deckNames() } ?: deckNames().first()

    fun read(name: String): Map<String, Int> {
        val raw = preferences.getString("deck_$name", "{}") ?: "{}"
        return runCatching {
            val objectData = JSONObject(raw)
            buildMap {
                for (key in objectData.keys()) {
                    val count = objectData.optInt(key, 0)
                    if (count > 0) put(key, count)
                }
            }
        }.getOrDefault(emptyMap())
    }

    /** Additional visual preference only; does not modify card quantities. */
    fun cover(name: String): String {
        val id = preferences.getString("cover_$name", "default") ?: "default"
        return if (DuelCoverCatalog.isKnown(id)) id else "default"
    }

    fun setCover(name: String, coverId: String) {
        require(name in deckNames()) { "Deck does not exist." }
        require(DuelCoverCatalog.isKnown(coverId)) { "Unknown Shadowshaper card back." }
        preferences.edit().putString("cover_$name", coverId).apply()
    }

    fun save(name: String, cards: Map<String, Int>) {
        val json = JSONObject()
        cards.forEach { (card, count) -> if (count > 0) json.put(card, count) }
        preferences.edit().putString("deck_$name", json.toString()).apply()
    }

    fun select(name: String) {
        require(name in deckNames()) { "Deck does not exist: $name" }
        preferences.edit().putString("selected_deck", name).apply()
    }

    fun create(name: String) {
        validateName(name)
        require(name !in deckNames()) { "Deck already exists." }
        val names = deckNames() + name
        preferences.edit()
            .putString("deck_names", JSONArray(names).toString())
            .putString("selected_deck", name)
            .putString("deck_$name", "{}")
            .apply()
    }

    fun rename(old: String, new: String) {
        validateName(new)
        require(old in deckNames()) { "Deck does not exist." }
        require(new == old || new !in deckNames()) { "Deck already exists." }
        if (old == new) return
        val names = deckNames().map { if (it == old) new else it }
        val original = preferences.getString("deck_$old", "{}") ?: "{}"
        val originalCover = cover(old)
        preferences.edit()
            .putString("deck_$new", original)
            .putString("cover_$new", originalCover)
            .remove("deck_$old")
            .remove("cover_$old")
            .putString("deck_names", JSONArray(names).toString())
            .putString("selected_deck", new)
            .apply()
    }

    fun delete(name: String) {
        require(name in deckNames()) { "Deck does not exist." }
        val remaining = deckNames().filterNot { it == name }.ifEmpty { listOf("My Deck") }
        val edit = preferences.edit().remove("deck_$name").remove("cover_$name")
            .putString("deck_names", JSONArray(remaining).toString())
            .putString("selected_deck", remaining.first())
        if (remaining.size == 1 && remaining.first() == "My Deck" && name == "My Deck")
            edit.putString("deck_My Deck", "{}")
        edit.apply()
    }

    companion object {
        fun validateName(name: String) {
            require(name.isNotBlank() && name.length <= 60 &&
                listOf('/', '\\', '|').none { it in name }) {
                "Deck name must be 1–60 characters without /, \\, or |."
            }
        }
    }
}
