package com.tplatt.shadowshaper

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Local JVM layout/interaction guards for battle chrome. Android rendering
 * must still be evaluated with a local APK; no GitHub Actions are required.
 */
class DuelBattleChromeGuardTest {
    private fun source(name: String) = listOf(
        File("src/main/java/com/tplatt/shadowshaper/$name"),
        File("app/src/main/java/com/tplatt/shadowshaper/$name")
    ).firstOrNull(File::isFile)?.readText() ?: error("Missing battle source $name")

    @Test fun headerIsARealStatusHudInsteadOfTheOld29dpTextStrip() {
        val battle = source("ShadowBattleActivity.kt")
        val chrome = source("DuelBattleChrome.kt")
        assertTrue(battle.contains("DuelBattleTopBar("))
        assertFalse(battle.contains("Row(Modifier.fillMaxWidth().height(29.dp)"))
        assertTrue(chrome.contains(".height(49.dp)"))
        assertTrue(chrome.contains("\"SHADOW SHAPER\""))
        assertTrue(chrome.contains("\"D U E L S\""))
        assertTrue(chrome.contains("turn indicator breathing"))
        assertTrue(chrome.contains("Brush.verticalGradient("))
    }

    @Test fun headerMaintainsMenuAndFinalBoardResultsExit() {
        val battle = source("ShadowBattleActivity.kt")
        val chrome = source("DuelBattleChrome.kt")
        assertTrue(battle.contains("reviewingBoard = inspectingFinalBoard"))
        assertTrue(battle.contains("if (inspectingFinalBoard) inspectingFinalBoard = false"))
        assertTrue(battle.contains("else settingsDialog = true"))
        assertTrue(battle.contains("sounds.play(DuelSound.UI_TAP)"))
        assertTrue(chrome.contains("contentDescription ="))
        assertTrue(chrome.contains("\"Return to match results\""))
        assertTrue(chrome.contains("\"Open battle menu\""))
    }

    @Test fun statusLabelsAreDrivenOnlyByActualGameConditions() {
        val chrome = source("DuelBattleChrome.kt")
        val battle = source("ShadowBattleActivity.kt")
        for (status in listOf(
            "YOUR TURN", "CPU TURN", "TARGET", "MOVE UNIT", "DUEL COMPLETE"
        )) {
            assertTrue("Missing HUD label $status", chrome.contains("\"$status\""))
            assertTrue("Missing game status source $status", battle.contains("\"$status\""))
        }
        assertTrue(battle.contains("snapshot?.playerTurn == true"))
        assertTrue(battle.contains("snapshot?.finished == true"))
        assertTrue(battle.contains("pendingPosition != null"))
        assertTrue(battle.contains("engine.pendingMoveCaster === engine.human"))
    }

    @Test fun enlargedTurnDialUsesWholeRingAsTouchTarget() {
        val dial = source("DuelBattleHud.kt")
        val battle = source("ShadowBattleActivity.kt")
        assertTrue(dial.contains("Modifier\n            .size(78.dp)"))
        assertTrue(dial.contains("Modifier.size(76.dp)"))
        assertTrue(dial.contains("collectIsPressedAsState()"))
        assertTrue(dial.contains("gold bezel glow"))
        assertTrue(dial.contains("Brush.sweepGradient("))
        assertTrue(dial.contains(".clickable(\n                enabled = enabled"))
        assertTrue(dial.contains("role = Role.Button"))
        assertTrue(dial.contains("onClick = onClick"))
        assertTrue(battle.contains("enabled = snapshot.playerTurn && !snapshot.finished"))
        assertTrue(battle.contains("DuelGoldTurnDial("))
        assertTrue(battle.contains("current.endTurn()"))
    }

    @Test fun dialHasSeparateCancelMoveAndTurnStatesWithoutGameplayChanges() {
        val dial = source("DuelBattleHud.kt")
        val battle = source("ShadowBattleActivity.kt")
        assertTrue(dial.contains("label.contains(\"CANCEL\""))
        assertTrue(dial.contains("label.contains(\"MOVE\""))
        assertTrue(dial.contains("\"End turn\""))
        assertTrue(dial.contains("\"Cancel card targeting\""))
        assertTrue(dial.contains("\"Choose a unit relocation slot\""))
        assertTrue(battle.contains("pendingPosition != null -> \"CANCEL\""))
        assertTrue(battle.contains("engine.pendingMovedUnit != null -> \"MOVE\""))
        assertTrue(battle.contains("}) endConfirmation = true"))
        assertFalse(dial.contains("engine.endTurn("))
        assertFalse(dial.contains("engine.play("))
        assertTrue(battle.contains(".padding(end = 12.dp, bottom = 95.dp)"))
    }

    @Test fun sevenColumnsOriginalArtAndPortraitLockStayIntact() {
        val battle = source("ShadowBattleActivity.kt")
        val art = source("DuelStoneArena.kt")
        assertTrue(battle.contains("for (col in 0 until ShadowRules.COLUMNS)"))
        assertTrue(battle.contains("DuelStoneArena(Modifier.fillMaxSize())"))
        assertTrue(art.contains("R.drawable.shadow_arena_original_portrait_hq"))
        assertTrue(battle.contains("SCREEN_ORIENTATION_PORTRAIT"))
    }
}
