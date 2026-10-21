package com.tplatt.shadowshaper

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ShadowDeckUiState(
    val names: List<String> = listOf("My Deck"),
    val selected: String = "My Deck",
    val cards: Map<String, Int> = emptyMap(),
    val cover: String = "default",
    val message: String? = null
) {
    val count: Int get() = cards.values.sum()
}

class ShadowDeckViewModel(application: Application) : AndroidViewModel(application) {
    val catalog: Map<String, ShadowData.Card> = ShadowData.load(application)
    val alphabetical: List<ShadowData.Card> = catalog.values.sortedBy { it.name.lowercase() }
    private val repository = ShadowDeckRepository(application)

    private val _state = MutableStateFlow(loadState())
    val state: StateFlow<ShadowDeckUiState> = _state.asStateFlow()

    private fun loadState(message: String? = null): ShadowDeckUiState {
        val selected = repository.selectedDeck()
        return ShadowDeckUiState(
            names = repository.deckNames(),
            selected = selected,
            cards = repository.read(selected),
            cover = repository.cover(selected),
            message = message
        )
    }

    fun refresh() { _state.value = loadState() }
    fun clearMessage() { _state.value = _state.value.copy(message = null) }

    private fun runMutation(action: () -> Unit) {
        try {
            action()
            _state.value = loadState()
        } catch (e: IllegalArgumentException) {
            _state.value = _state.value.copy(message = e.message)
        } catch (e: Exception) {
            _state.value = _state.value.copy(message = "Deck operation failed: ${e.message}")
        }
    }

    fun select(name: String) = runMutation { repository.select(name) }
    fun create(name: String) = runMutation { repository.create(name.trim()) }
    fun rename(new: String) = runMutation { repository.rename(_state.value.selected, new.trim()) }
    fun delete() = runMutation { repository.delete(_state.value.selected) }

    fun selectCover(coverId: String) = runMutation {
        repository.setCover(_state.value.selected, coverId)
    }

    fun add(card: ShadowData.Card) {
        val now = _state.value
        val problem = ShadowDeckRules.canAdd(now.cards, card, catalog)
        if (problem.isNotBlank()) {
            _state.value = now.copy(message = problem)
            return
        }
        runMutation {
            val next = now.cards.toMutableMap()
            next[card.name] = (next[card.name] ?: 0) + 1
            repository.save(now.selected, next)
        }
    }

    fun remove(name: String) = runMutation {
        val now = _state.value
        val next = now.cards.toMutableMap()
        val quantity = next[name] ?: 0
        if (quantity <= 1) next.remove(name) else next[name] = quantity - 1
        repository.save(now.selected, next)
    }

    fun legality(): String = ShadowDeckRules.validate(state.value.cards, catalog)

    fun exportText(): String = buildString {
        append("# Shadowshaper Duels - ").append(state.value.selected).append('\n')
        state.value.cards.toSortedMap(String.CASE_INSENSITIVE_ORDER)
            .forEach { (name, count) -> append(count).append("x ").append(name).append('\n') }
    }

    /** Atomic import: bad names or illegal quantities never overwrite a saved deck. */
    fun importText(text: String) {
        val current = _state.value
        val result = linkedMapOf<String, Int>()
        val pattern = Regex("""^\s*(\d+)\s*[xX×]?\s+(.+?)\s*$""")
        val byName = catalog.values.associateBy { it.name.lowercase() }
        for (line in text.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
            val match = pattern.matchEntire(trimmed)
            if (match == null) {
                _state.value = current.copy(message = "Invalid deck line: $trimmed")
                return
            }
            val amount = match.groupValues[1].toIntOrNull()
            val card = byName[match.groupValues[2].trim().lowercase()]
            if (card == null || amount == null || amount < 1 || amount > 15) {
                _state.value = current.copy(message = "Unknown card or count: $trimmed")
                return
            }
            repeat(amount) {
                val error = ShadowDeckRules.canAdd(result, card, catalog)
                if (error.isNotBlank()) {
                    _state.value = current.copy(message = "Import rejected: $error")
                    return
                }
                result[card.name] = (result[card.name] ?: 0) + 1
            }
        }
        runMutation { repository.save(current.selected, result) }
    }
}
