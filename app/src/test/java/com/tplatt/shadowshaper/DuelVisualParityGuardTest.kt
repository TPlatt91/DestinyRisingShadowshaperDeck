package com.tplatt.shadowshaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

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
            File("src/main/res/drawable-nodpi/shadow_arena_original_portrait.jpg"),
            File("app/src/main/res/drawable-nodpi/shadow_arena_original_portrait.jpg")
        ).firstOrNull(File::isFile)
            ?: error("The uploaded stone ruins picture must be bundled offline")

    @Test fun bundledOriginalArtworkIsAnActualReadablePortraitImage() {
        val original = actualArenaImage()
        assertTrue("Replacement scene is missing or empty", original.length() > 25000L)
        val bitmap = ImageIO.read(original)
        assertTrue("Image decoder could not open the artwork", bitmap != null)
        assertEquals(480, bitmap.width)
        assertEquals(864, bitmap.height)
        assertTrue(bitmap.height > bitmap.width)
    }

    @Test fun arenaRendersUploadedBitmapInsteadOfOldProceduralRubble() {
        val stone = source("DuelStoneArena.kt")
        val battle = source("ShadowBattleActivity.kt")
        assertTrue(stone.contains("painterResource(R.drawable.shadow_arena_original_portrait)"))
        assertTrue(stone.contains("ContentScale.FillBounds"))
        assertFalse(stone.contains("generateSlateTexture"))
        assertFalse(stone.contains("DuelArenaPatina"))
        assertFalse(stone.contains("DuelArenaEnvironment"))
        assertFalse(stone.contains("DuelArenaTrim"))
        assertTrue(battle.contains("DuelStoneArena(Modifier.fillMaxSize())"))
        assertTrue(battle.contains("DuelArenaGrid(Modifier.fillMaxSize())"))
    }

    @Test fun genuineSevenColumnRowGuidesStayBehindLiveCards() {
        val battle = source("ShadowBattleActivity.kt")
        val stone = source("DuelStoneArena.kt")
        val metrics = source("DuelLayoutMetrics.kt")
        val visuals = source("ShadowBattleVisuals.kt")
        assertTrue(metrics.contains("PORTRAIT_LANE_WIDTH_FRACTION = 0.70f"))
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
}
