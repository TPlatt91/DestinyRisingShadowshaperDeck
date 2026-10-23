package com.tplatt.shadowshaper

import android.content.Context
import java.util.Locale

/**
 * Shared video-backed keyword reference for Rules and both card inspectors.
 *
 * The eight recorded tooltip definitions live in video_keyword_rules_01_14.tsv.
 * Scorched and Overshield are explicitly marked as simulator explanations
 * rather than invented verbatim quotations from an original-game tooltip.
 */
object ShadowKeywordGlossary {
    data class Entry(
        val name: String,
        val definition: String,
        val recordedTooltip: Boolean
    )

    private val supplemental = listOf(
        Entry(
            "Scorched",
            "A delayed damage effect. Each applied Scorched effect deals its stated damage on the affected unit's owner's turn while it remains active. Suppression clears it.",
            false
        ),
        Entry(
            "Overshield",
            "Blocks the next damage, destruction, or Suppression effect that reaches the unit, then is consumed. Power does not decrease from the blocked effect.",
            false
        )
    )

    /** Pure TSV parser so the video records are auditable in local JVM tests. */
    fun parseVideoRules(tsv: String): List<Entry> =
        tsv.lineSequence().drop(1).mapNotNull { line ->
            val fields = line.trimEnd('\r').split('\t')
            if (fields.size < 2 || fields[0].isBlank() || fields[1].isBlank()) null
            else Entry(fields[0].trim(), fields[1].trim(), true)
        }.toList().distinctBy { it.name.lowercase(Locale.ROOT) }

    private var cached: List<Entry>? = null

    @Synchronized
    fun all(context: Context): List<Entry> {
        cached?.let { return it }
        val fromVideo = runCatching {
            context.assets.open("video_keyword_rules_01_14.tsv")
                .bufferedReader().use { parseVideoRules(it.readText()) }
        }.getOrDefault(emptyList())
        return (fromVideo + supplemental).distinctBy {
            it.name.lowercase(Locale.ROOT)
        }.also { cached = it }
    }

    /** Match only keywords actually mentioned in the printed effect/trigger. */
    fun onCard(card: ShadowData.Card, entries: List<Entry>): List<Entry> {
        val wording = card.effect + " " + card.trigger
        return entries.filter { entry ->
            val pattern = when (entry.name.lowercase(Locale.ROOT)) {
                "suppressed" -> Regex(
                    """\bsuppress(?:ed|es|ing|ion)?\b""", RegexOption.IGNORE_CASE
                )
                "inspired" -> Regex(
                    """\binspir(?:e|ed|es|ing)\b""", RegexOption.IGNORE_CASE
                )
                "scorched" -> Regex(
                    """\bscorch(?:ed|es|ing)?\b""", RegexOption.IGNORE_CASE
                )
                else -> Regex(
                    """\b""" + Regex.escape(entry.name) + """\b""",
                    RegexOption.IGNORE_CASE
                )
            }
            pattern.containsMatchIn(wording)
        }
    }
}
