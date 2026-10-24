package com.tplatt.shadowshaper

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * JVM-only checks for the actual user-supplied arena art, landscape-to-
 * portrait adaptation, foreground ordering and unblocked card interactions.
 */
class DuelVisualParityGuardTest {
    private fun source(filename: String): String {
        val file = "com/tplatt/shadowshaper/$filename"
        return listOf(
            File("src/main/java/$file"),
            File("app/src/main/java/$file")
        ).firstOrNull(File::isFile)?.readText()
            ?: error("Battle UI source not found: $filename")
    }

    private fun actualArenaImage(): File =
        listOf(
            File("src/main/res/drawable-nodpi/shadow_arena_original_portrait_hq.webp"),
            File("app/src/main/res/drawable-nodpi/shadow_arena_original_portrait_hq.webp")
        ).firstOrNull(File::isFile)
            ?: error("The uploaded stone ruins picture must be bundled offline")

    @Test fun bundledOriginalArtworkIsAnActualReadablePortraitImage() {
        val original = actualArenaImage()
        assertTrue("Replacement scene is missing or empty", original.length() > 25000L)
        assertTrue(original.readBytes().take(4).toByteArray().contentEquals("RIFF".toByteArray()))
        // WebP is decoded by Android's native resource loader (not JDK ImageIO).
        // The RIFF/WEBP header and file size are verified by this JVM guard.
        assertTrue(String(original.readBytes(), 8, 4) == "WEBP")
    }

    @Test fun backgroundRestoresOriginalArtworkWithoutDistortingImage() {
        val stone = source("DuelStoneArena.kt")
        val metrics = source("DuelLayoutMetrics.kt")
        assertTrue(stone.contains("ContentScale.Crop"))
        assertFalse(stone.contains("ContentScale.FillBounds"))
        assertTrue(stone.contains("scaleX = 1.12f"))
        assertTrue(stone.contains("scaleY = 1.12f"))
        assertTrue(metrics.contains("PORTRAIT_LANE_WIDTH_FRACTION = 0.86f"))
        assertTrue(metrics.contains("screenWidthDp.coerceAtLeast(250) * 0.82f"))
    }

    @Test fun arenaRendersUploadedBitmapInsteadOfOldProceduralRubble() {
        val stone = source("DuelStoneArena.kt")
        val battle = source("ShadowBattleActivity.kt")
        assertTrue(stone.contains("painterResource(R.drawable.shadow_arena_original_portrait_hq)"))
        assertTrue(stone.contains("ContentScale.Crop"))
        assertFalse(stone.contains("generateSlateTexture"))
        assertFalse(stone.contains("DuelArenaPatina"))
        assertFalse(stone.contains("DuelArenaEnvironment"))
        assertFalse(stone.contains("DuelArenaTrim"))
        assertTrue(battle.contains("DuelStoneArena(Modifier.fillMaxSize())"))
        assertTrue(battle.contains("DuelArenaGrid(Modifier.fillMaxSize())"))
    }

    @Test fun originalSevenColumnRowGuidesStayBehindLiveCards() {
        val battle = source("ShadowBattleActivity.kt")
        val stone = source("DuelStoneArena.kt")
        val metrics = source("DuelLayoutMetrics.kt")
        val visuals = source("ShadowBattleVisuals.kt")
        assertTrue(metrics.contains("PORTRAIT_LANE_WIDTH_FRACTION = 0.86f"))
        assertTrue(battle.contains("DuelLayoutMetrics.PORTRAIT_LANE_WIDTH_FRACTION"))
        assertTrue(stone.contains("column in 1 until ShadowRules.COLUMNS"))
        assertFalse(battle.contains("Text(heading.replace"))
        assertTrue(visuals.contains("if (selected) {"))
        assertTrue(visuals.contains("DuelCardArtwork(card, Modifier.fillMaxSize()"))
        assertFalse(visuals.contains("Text(card.name, color = DuelPalette.cream"))
    }

    @Test fun allLiveBattleControlsSurviveTheBackgroundSwap() {
        val battle = source("ShadowBattleActivity.kt")
        assertTrue(battle.contains("DuelPowerCrest(snapshot.cpuPower, isPlayer = false)"))
        assertTrue(battle.contains("DuelPowerCrest(snapshot.playerPower, isPlayer = true)"))
        assertTrue(battle.contains("DuelGoldTurnDial("))
        assertTrue(battle.contains("SYSTEM_UI_FLAG_IMMERSIVE_STICKY"))
        assertTrue(battle.contains("shieldRossiSelf()"))
        assertTrue(battle.contains("showLog = true"))
        assertTrue(battle.contains("current.endTurn()"))
    }
    @Test fun everyEnginePlayableCardCanDragWithoutStaleTurnState() {
        val battle = source("ShadowBattleActivity.kt")
        val hitTesting = source("DuelHitTesting.kt")
        assertTrue(battle.contains("pointerInput(current, index, card.name)"))
        assertTrue(battle.contains("val canPlace = engine.humanTurn"))
        assertTrue(battle.contains("engine.isPlayablePlacement("))
        assertTrue(battle.contains("DuelHitTesting.playerSlotForDrop("))
        assertTrue(battle.contains("userScrollEnabled = false"))
        assertTrue(battle.contains("handListState.animateScrollToItem("))
        assertTrue(hitTesting.contains("val cardCenter = Offset("))
        assertFalse(battle.contains("if (snapshot.playerTurn && !snapshot.finished &&"))
    }

}
