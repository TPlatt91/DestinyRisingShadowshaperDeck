package com.tplatt.shadowshaper

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Local JVM checks for the custom Destiny-inspired popup surfaces.
 * These tests never use GitHub Actions; the APK still needs a device check.
 */
class DuelPopupChromeGuardTest {
    private fun source(filename: String): String =
        listOf(
            File("src/main/java/com/tplatt/shadowshaper/$filename"),
            File("app/src/main/java/com/tplatt/shadowshaper/$filename")
        ).firstOrNull(File::isFile)?.readText()
            ?: error("Missing popup source: $filename")

    @Test fun oneReusableChromeReplacesStockMaterialDialogs() {
        val chrome = source("DuelPopupChrome.kt")
        val battle = source("ShadowBattleActivity.kt")
        val result = source("DuelResultPresentation.kt")
        assertTrue(chrome.contains("internal fun DuelPopupDialog("))
        assertTrue(chrome.contains("internal fun DuelPopupAction("))
        assertTrue(chrome.contains("internal fun DuelPopupMenuRow("))
        assertTrue(chrome.contains("CutCornerShape("))
        assertTrue(chrome.contains("usePlatformDefaultWidth = false"))
        assertTrue(chrome.contains("verticalScroll(rememberScrollState())"))
        assertTrue(chrome.contains("DuelPopupEmblem(symbol)"))
        assertTrue(chrome.contains("DuelPopupRule()"))
        assertFalse(battle.contains("AlertDialog("))
        assertTrue(result.contains("DuelPopupDialog("))
    }

    @Test fun saveRecoveryCannotBeDismissedWithoutARealChoice() {
        val battle = source("ShadowBattleActivity.kt")
        assertTrue(battle.contains("title = \"CONTINUE LAST MATCH?\""))
        assertTrue(battle.contains("allowDismiss = false"))
        assertTrue(battle.contains("matchRepository.clear()"))
        assertTrue(battle.contains("resumeMatch(savedAtLaunch)"))
        assertTrue(battle.contains("label = \"START NEW\""))
        assertTrue(battle.contains("label = \"CONTINUE\""))
        assertTrue(battle.contains("savedAtLaunch.engine.round"))
        assertTrue(battle.contains("savedAtLaunch.deckName"))
    }

    @Test fun settingsHaveRealPersistedSwitchAndVolumeControls() {
        val chrome = source("DuelPopupChrome.kt")
        val battle = source("ShadowBattleActivity.kt")
        assertTrue(chrome.contains("DuelPopupSoundSettings("))
        assertTrue(chrome.contains("SwitchDefaults.colors("))
        assertTrue(chrome.contains("SliderDefaults.colors("))
        assertTrue(chrome.contains("\"SOUND EFFECTS\""))
        assertTrue(chrome.contains("\"SFX VOLUME\""))
        assertTrue(battle.contains("sounds.enabled = value"))
        assertTrue(battle.contains("sounds.volume = value"))
        assertTrue(battle.contains("sounds.play(DuelSound.CARD_SELECT)"))
        assertTrue(battle.contains("enabled = audioEnabled"))
    }

    @Test fun everyBattlePopupRetainsRequiredActions() {
        val battle = source("ShadowBattleActivity.kt")
        for (title in listOf(
            "CONTINUE LAST MATCH?", "SAVE RECOVERY", "CHOOSE YOUR DECK",
            "SELECT OPPONENT", "YOU ATTACK FIRST", "CPU ATTACKS FIRST",
            "SHADOW SHAPER DUELS", "LEAVE SKIRMISH?", "END THIS TURN?",
            "BATTLE LOG"
        )) {
            assertTrue("Missing popup: $title", battle.contains(title))
        }
        assertTrue(battle.contains("factionDialog = true"))
        assertTrue(battle.contains("startMatch(faction)"))
        assertTrue(battle.contains("openingDialog = false"))
        assertTrue(battle.contains("current?.endTurn()"))
        assertTrue(battle.contains("exitConfirmation = true"))
        assertTrue(battle.contains("onRules()"))
        assertTrue(battle.contains("onExit"))
        assertTrue(battle.contains("engine.fullJournal.forEachIndexed"))
        assertTrue(battle.contains("engine.diagnosticReport()"))
    }

    @Test fun resultAndCardInspectionUseMatchingAppearance() {
        val result = source("DuelResultPresentation.kt")
        val inspect = source("DuelCardInspection.kt")
        for (control in listOf(
            "VIEW BOARD", "BATTLE LOG", "BATTLE AGAIN", "SELECT DECKS", "HOME"
        )) {
            assertTrue("Missing result action: $control", result.contains(control))
        }
        assertTrue(result.contains("onInspectBoard"))
        assertTrue(result.contains("onReviewLog"))
        assertTrue(result.contains("onRematch"))
        assertTrue(result.contains("onSelectDecks"))
        assertTrue(result.contains("onHome"))
        assertTrue(inspect.contains("DuelPopupAction("))
        assertTrue(inspect.contains("DuelPopupRule()"))
        assertTrue(inspect.contains("DuelCardFrame("))
        assertTrue(inspect.contains("ShadowKeywordDefinitions("))
    }

    @Test fun popupDrawingIsOfflineAndDoesNotChangeGameplay() {
        val chrome = source("DuelPopupChrome.kt")
        assertFalse(chrome.contains("http://"))
        assertFalse(chrome.contains("https://"))
        assertFalse(chrome.contains("engine.play("))
        assertFalse(chrome.contains("engine.endTurn("))
        assertFalse(chrome.contains("R.drawable."))
        assertTrue(chrome.contains("role = Role.Button"))
        assertTrue(chrome.contains("DuelPopupActionStyle.PRIMARY"))
    }
}
