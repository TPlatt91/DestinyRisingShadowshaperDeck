package com.tplatt.shadowshaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Regression guards for the portrait landing experience.
 * Plain local JVM test: no emulator, GitHub Actions or image snapshots.
 */
class ShadowLandingPageGuardTest {
    private fun source(file: String): String =
        listOf(
            File("src/main/java/com/tplatt/shadowshaper/$file"),
            File("app/src/main/java/com/tplatt/shadowshaper/$file")
        ).firstOrNull(File::isFile)?.readText() ?: error("Missing $file")

    @Test fun exactlyFourNamedPrimaryDestinationsAreDisplayed() {
        val landing = source("ShadowLandingPage.kt")
        val titles = listOf(
            "\"SKIRMISH\"",
            "\"DECK CONSTRUCTION\"",
            "\"CARD COLLECTION\"",
            "\"RULES\""
        )
        titles.forEach { title ->
            assertTrue("Missing primary destination: $title", landing.contains(title))
        }
        assertEquals(4, landing.split("LandingAction(\n").size - 2)
        assertFalse(landing.contains("\"SIM BATTLE\""))
        assertFalse(landing.contains("\"NEW BATTLE\""))
    }

    @Test fun continueBattleIsSeparateAndOnlyShownWhenASaveExists() {
        val landing = source("ShadowLandingPage.kt")
        val home = source("ShadowComposeActivity.kt")
        assertTrue(landing.contains("if (hasSavedMatch)"))
        assertTrue(landing.contains("LandingContinue("))
        assertTrue(landing.contains("\"CONTINUE BATTLE\""))
        assertTrue(home.contains("matchRepository.load() != null"))
        assertTrue(home.contains("onContinue = {"))
        assertTrue(home.contains("putExtra(\"resume_last_match\", true)"))
        assertTrue(home.contains("onSkirmish = {"))
    }

    @Test fun pagesAndSavedMatchBehaviorAreNotAltered() {
        val home = source("ShadowComposeActivity.kt")
        assertTrue(home.contains("onDeckConstruction = { page = \"deck\" }"))
        assertTrue(home.contains("onCardCollection = { page = \"collection\" }"))
        assertTrue(home.contains("onRules = { page = \"rules\" }"))
        assertTrue(home.contains("ShadowBattleActivity::class.java"))
        assertTrue(home.contains("SCREEN_ORIENTATION_PORTRAIT"))
        assertFalse(home.contains("private fun HomeScreen("))
        assertFalse(home.contains("private fun HomeDestination("))
    }

    @Test fun designIsOfflineAndAssetsAreOriginalVectors() {
        val landing = source("ShadowLandingPage.kt")
        assertTrue(landing.contains("LandingAtmosphere("))
        assertTrue(landing.contains("LandingOrbitalEmblem("))
        assertTrue(landing.contains("LandingGlyph("))
        assertTrue(landing.contains("landingCut"))
        assertTrue(landing.contains("collectIsPressedAsState()"))
        assertTrue(landing.contains("CutCornerShape("))
        assertFalse(landing.contains("http://"))
        assertFalse(landing.contains("https://"))
        assertFalse(landing.contains("R.drawable."))
        assertFalse(landing.contains("ImageView"))
    }
}
