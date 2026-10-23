package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Exercises the same recorded tooltip TSV consumed by Rules and inspectors. */
class ShadowKeywordGlossaryTest {
    private fun keywordAsset(): File =
        listOf(
            File("src/main/assets/video_keyword_rules_01_14.tsv"),
            File("app/src/main/assets/video_keyword_rules_01_14.tsv")
        ).firstOrNull { it.isFile }
            ?: error("Video keyword reference not found in Android assets")

    private fun videoEntries() =
        ShadowKeywordGlossary.parseVideoRules(keywordAsset().readText())

    private fun card(effect: String, trigger: String = "") =
        ShadowData.Card().apply {
            name = "Keyword test unit"
            this.effect = effect
            this.trigger = trigger
        }

    @Test fun latestOriginalVideoTooltipFileHasAllEightSharedDefinitions() {
        val entries = videoEntries()
        assertEquals(
            setOf("When Played", "Sustained", "Line", "Column", "Back Row",
                "Front Row", "Inspired", "Suppressed"),
            entries.map { it.name }.toSet()
        )
        assertTrue(entries.all { it.recordedTooltip })
        assertTrue(entries.all { it.definition.isNotBlank() })
    }

    @Test fun lineAndRowKeywordsMatchPrintedAbilityNotCardFaction() {
        val entries = videoEntries()
        val result = ShadowKeywordGlossary.onCard(
            card("When Played (3-Unit Line, Front Row): Grants Inspired to an allied unit."),
            entries
        ).map { it.name }.toSet()
        assertEquals(setOf("When Played", "Line", "Front Row", "Inspired"), result)

        val noLine = ShadowKeywordGlossary.onCard(
            card("When Played: Deals 2 damage to a Cabal unit."), entries
        ).map { it.name }.toSet()
        assertEquals(setOf("When Played"), noLine)
    }

    @Test fun suppressedVariantsAndCatalogTriggerUseSharedKeywordLogic() {
        val entries = videoEntries()
        val result = ShadowKeywordGlossary.onCard(
            card("Sustained: Suppresses a target.", "(Back Row)"),
            entries
        ).map { it.name }.toSet()
        assertEquals(setOf("Sustained", "Suppressed", "Back Row"), result)
    }

    @Test fun plainCardHasNoMisleadingKeywordDefinitions() {
        val entries = videoEntries()
        assertTrue(
            ShadowKeywordGlossary.onCard(
                card("No special effects. The unit is a Human."), entries
            ).isEmpty()
        )
    }

    @Test fun tabSeparatedVideoTooltipParserIgnoresIncompleteRows() {
        val sample =
            "keyword\toriginal_video_rule\tbatch_sources\n" +
            "Inspired\tAt the end of your turn, gains 1 Power.\t05\n" +
            "\t\t\n" +
            "Inspired\tDuplicate description\t06\n" +
            "Column\tVertical pair.\t08\n"
        val parsed = ShadowKeywordGlossary.parseVideoRules(sample)
        assertEquals(listOf("Inspired", "Column"), parsed.map { it.name })
        assertEquals(
            "At the end of your turn, gains 1 Power.",
            parsed.first().definition
        )
    }
}
