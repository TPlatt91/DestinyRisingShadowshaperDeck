package com.tplatt.shadowshaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** JVM-only faction/transmat regression checks; no device or Actions needed. */
class DuelTransmatStyleTest {
    @Test fun defaultTransmatIsSharedByHumansExosAndAwoken() {
        for (faction in listOf("Humans", "Human", "Exo", "Exos", "Awoken")) {
            assertEquals(faction, DuelTransmatStyle.DEFAULT,
                DuelTransmatResolver.forFaction(faction))
        }
    }

    @Test fun eachRealEnemyFactionHasItsOwnTransmat() {
        assertEquals(DuelTransmatStyle.HIVE, DuelTransmatResolver.forFaction("Hive"))
        assertEquals(DuelTransmatStyle.CABAL, DuelTransmatResolver.forFaction("Cabal"))
        assertEquals(DuelTransmatStyle.FALLEN, DuelTransmatResolver.forFaction("Fallen"))
    }

    @Test fun vexIsReadyIfItEntersTheGameLater() {
        assertEquals(DuelTransmatStyle.VEX, DuelTransmatResolver.forFaction("Vex"))
    }

    @Test fun mappingIgnoresCaseAndAccidentalWhitespace() {
        assertEquals(DuelTransmatStyle.HIVE, DuelTransmatResolver.forFaction("  hIvE "))
        assertEquals(DuelTransmatStyle.FALLEN, DuelTransmatResolver.forFaction("fALLen"))
        assertEquals(DuelTransmatStyle.DEFAULT, DuelTransmatResolver.forFaction(" aWokEn "))
    }

    @Test fun unknownNeutralAndBlankFactionsStayDefault() {
        for (faction in listOf<String?>(null, "", " ", "Neutral",
                "Unspecified", "Unlisted faction")) {
            assertEquals("$faction must not invent a special arrival",
                DuelTransmatStyle.DEFAULT,
                DuelTransmatResolver.forFaction(faction))
        }
    }

    @Test fun transmatVisibilityBeginsAndEndsAtZero() {
        assertEquals(0f, DuelTransmatResolver.visibility(0f), .0001f)
        assertEquals(0f, DuelTransmatResolver.visibility(1f), .0001f)
        assertEquals(0f, DuelTransmatResolver.visibility(-3f), .0001f)
        assertEquals(0f, DuelTransmatResolver.visibility(3f), .0001f)
        assertTrue(DuelTransmatResolver.visibility(.25f) > .5f)
        assertTrue(DuelTransmatResolver.visibility(.78f) > 0f)
    }

    @Test fun precueOccursBeforeLandingAndCoversImpact() {
        assertTrue(DuelTransmatResolver.PRECHARGE_MS > 0L)
        assertTrue(DuelTransmatResolver.EFFECT_MS >
            DuelTransmatResolver.PRECHARGE_MS)
        assertTrue(DuelTransmatResolver.EFFECT_MS >=
            DuelTransmatResolver.PRECHARGE_MS +
                (DuelLandingMotion.DURATION_MS * .72f).toInt())
    }
}
