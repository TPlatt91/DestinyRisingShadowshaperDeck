package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test

/**
 * User-recorded in-game card text and stats always outrank community/wiki
 * and TodayInDestiny metadata, including active row restrictions.
 * These tests run locally with the Android JVM unit-test task.
 */
class ShadowVideoSourcePriorityTest {
    private fun card(
        name: String, source: String, effect: String
    ): ShadowData.Card = ShadowData.Card().apply {
        this.name = name
        rarity = "Exotic"
        faction = "Hive"
        cost = "3"
        power = "8"
        this.effect = effect
        this.source = source
        classification = "Common"
        trigger = ""
    }

    private fun conflictingMetadata(name: String) = listOf(
        name,
        "Fallen",    // faction from an outdated community entry
        "Rare",      // rarity
        "6",         // Light cost
        "1",         // Power
        "Summoned unit",
        "When Played; Back Row",
        "Vendor",
        "Older community metadata, not current game-client wording",
        "AB-12"
    )

    @Test fun verifiedVideoStatsOutrankLaterDetailedMetadata() {
        val c = card("Captured Unit", ShadowData.VIDEO_SOURCE,
            "When Played (Front Row): Deals 5 damage to a unit.")
        ShadowData.applyDetailedMetadata(c, conflictingMetadata(c.name))
        assertEquals("Hive", c.faction)
        assertEquals("Exotic", c.rarity)
        assertEquals("3", c.cost)
        assertEquals("8", c.power)
        assertEquals("When Played (Front Row): Deals 5 damage to a unit.", c.effect)
        assertEquals(ShadowData.VIDEO_SOURCE, c.source)
        assertEquals("Summoned unit", c.classification)
        assertEquals("Vendor", c.acquisition)
        assertEquals("AB-12", c.cardNumber)
    }

    @Test fun nonVideoCardStillReceivesDetailedMetadataCorrections() {
        val c = card("Community Card", "https://www.todayindestiny.com",
            "No special effects.")
        ShadowData.applyDetailedMetadata(c, conflictingMetadata(c.name))
        assertEquals("Fallen", c.faction)
        assertEquals("Rare", c.rarity)
        assertEquals("6", c.cost)
        assertEquals("1", c.power)
        assertEquals("Summoned unit", c.classification)
        assertEquals("When Played; Back Row", c.trigger)
    }

    @Test fun malformedMetadataDoesNotMutateVerifiedCard() {
        val c = card("Captured Unit", ShadowData.VIDEO_SOURCE,
            "When Played: Draw 2 cards.")
        ShadowData.applyDetailedMetadata(c, listOf("Bad", "Only 2 fields"))
        assertEquals("Hive", c.faction)
        assertEquals("8", c.power)
        assertEquals("", c.cardNumber)
    }

    @Test fun recordedFrontRowTextOverridesContradictoryMetadataRow() {
        val c = card("Captured Front Unit", ShadowData.VIDEO_SOURCE,
            "When Played (Front Row): Deals 5 damage.")
        ShadowData.applyDetailedMetadata(c, conflictingMetadata(c.name))
        val e = ShadowEngine(mapOf(c.name to c), 4L)
        assertEquals(ShadowRules.FRONT_ROW, ShadowData.videoRequiredRow(c))
        assertTrue(e.activeInRow(c, ShadowRules.FRONT_ROW))
        assertFalse(e.activeInRow(c, ShadowRules.BACK_ROW))
    }

    @Test fun recordedUnrestrictedWhenPlayedMustNotGainLegacyRowRequirement() {
        val c = card("Unrestricted Unit", ShadowData.VIDEO_SOURCE,
            "When Played: Draws 2 cards.")
        ShadowData.applyDetailedMetadata(c, conflictingMetadata(c.name))
        val e = ShadowEngine(mapOf(c.name to c), 5L)
        assertNull(ShadowData.videoRequiredRow(c))
        assertTrue(e.activeInRow(c, ShadowRules.FRONT_ROW))
        assertTrue(e.activeInRow(c, ShadowRules.BACK_ROW))
    }

    @Test fun recordedBackRowSustainedHonorsItsOwnPrintedCondition() {
        val c = card("Recorded Back Row", ShadowData.VIDEO_SOURCE,
            "Sustained (Back Row): Deal 1 damage when an enemy enters.")
        c.trigger = "Sustained; Front Row"
        val e = ShadowEngine(mapOf(c.name to c), 3L)
        assertEquals(ShadowRules.BACK_ROW, ShadowData.videoRequiredRow(c))
        assertTrue(e.activeInRow(c, ShadowRules.BACK_ROW))
        assertFalse(e.activeInRow(c, ShadowRules.FRONT_ROW))
    }

    @Test fun unverifiedCardContinuesToUseMetadataRowRestriction() {
        val c = card("Unverified Unit", "Community asset",
            "When Played: Deals 3 damage.")
        c.trigger = "When Played; Back Row"
        val e = ShadowEngine(mapOf(c.name to c), 6L)
        assertTrue(e.activeInRow(c, ShadowRules.BACK_ROW))
        assertFalse(e.activeInRow(c, ShadowRules.FRONT_ROW))
    }

    @Test fun printedLineAbilityDoesNotAccidentallyDemandFrontOrBackRow() {
        val c = card("Line Unit", ShadowData.VIDEO_SOURCE,
            "When Played (3-Unit Line): Deals 3 damage to a unit.")
        c.trigger = "When Played; Front Row"
        val e = ShadowEngine(mapOf(c.name to c), 3L)
        assertNull(ShadowData.videoRequiredRow(c))
        assertTrue(e.activeInRow(c, ShadowRules.BACK_ROW))
        assertTrue(e.activeInRow(c, ShadowRules.FRONT_ROW))
    }

    @Test fun videoCaptureWithCombinedRowConditionUsesItsRecordedRow() {
        val c = card("Combined Header", ShadowData.VIDEO_SOURCE,
            "When Played (3-Unit Line, Back Row): Deals 2 damage.")
        val e = ShadowEngine(mapOf(c.name to c), 7L)
        assertEquals(ShadowRules.BACK_ROW, ShadowData.videoRequiredRow(c))
        assertTrue(e.activeInRow(c, ShadowRules.BACK_ROW))
        assertFalse(e.activeInRow(c, ShadowRules.FRONT_ROW))
    }
}
