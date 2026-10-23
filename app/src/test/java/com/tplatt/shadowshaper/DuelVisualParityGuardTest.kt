package com.tplatt.shadowshaper

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Source-level regression guards for the portrait battle presentation.
 * These tests complement, not replace, on-device screenshot comparison.
 */
class DuelVisualParityGuardTest {
    private fun source(filename: String): String {
        val path = "com/tplatt/shadowshaper/$filename"
        return listOf(
            File("src/main/java/$path"),
            File("app/src/main/java/$path")
        ).firstOrNull(File::isFile)?.readText()
            ?: error("Required battle UI source missing: $filename")
    }

    @Test fun battleUsesSlateArenaRatherThanTallEmptyCardFrames() {
        val battle = source("ShadowBattleActivity.kt")
        val arena = source("DuelStoneArena.kt")
        val visuals = source("ShadowBattleVisuals.kt")
        assertTrue(battle.contains("DuelStoneArena(Modifier.fillMaxSize())"))
        assertTrue(battle.contains("DuelArenaGrid(Modifier.fillMaxSize())"))
        assertTrue(arena.contains("generateSlateTexture()"))
        assertFalse(battle.contains("Text(heading.replace"))
        assertTrue(
            "Unselected slots should not render permanent card rectangles",
            visuals.contains("if (selected) {")
        )
    }

    @Test fun fracturedRockAndFoliageRenderBehindTheRealCardLanes() {
        val stone = source("DuelStoneArena.kt")
        val environment = source("DuelArenaEnvironment.kt")
        val trim = source("DuelArenaTrim.kt")
        val battle = source("ShadowBattleActivity.kt")
        assertTrue(stone.contains("DuelArenaPatina(Modifier.fillMaxSize())"))
        assertTrue(stone.contains("DuelArenaEnvironment(Modifier.fillMaxSize())"))
        assertTrue(stone.contains("DuelArenaTrim(Modifier.fillMaxSize())"))
        val patina = source("DuelArenaPatina.kt")
        assertTrue(patina.contains("internal fun DuelArenaPatina("))
        assertFalse(patina.contains("Modifier.clickable"))
        assertTrue(stone.indexOf("DuelArenaPatina(Modifier.fillMaxSize())") <
            stone.indexOf("DuelArenaEnvironment(Modifier.fillMaxSize())"))
        assertTrue(trim.contains("cornerPaths"))
        assertTrue(trim.contains("brass.copy(alpha"))
        assertFalse(trim.contains("Modifier.clickable"))

        assertTrue(battle.contains("DuelStoneArena(Modifier.fillMaxSize())"))
        assertTrue(battle.contains("DuelArenaGrid(Modifier.fillMaxSize())"))
        assertTrue(environment.contains("private fun rock("))
        assertTrue(environment.contains("private fun fern("))
        assertTrue(environment.contains("private fun vine("))
        assertTrue(environment.contains("private fun moss("))
        assertTrue(environment.contains("private fun litter("))
        assertTrue(environment.contains("by lazy(LazyThreadSafetyMode.SYNCHRONIZED)"))
        assertTrue(environment.contains("contentDescription = null"))
        assertFalse(environment.contains("Modifier.clickable"))
        assertFalse(environment.contains("ShadowEngine."))
    }

    @Test fun sevenLaneFormationStaysInsideStoneAndBothScoreGemsStayRight() {
        val battle = source("ShadowBattleActivity.kt")
        val metrics = source("DuelLayoutMetrics.kt")
        val stone = source("DuelStoneArena.kt")
        assertTrue(metrics.contains("PORTRAIT_LANE_WIDTH_FRACTION = 0.86f"))
        assertTrue(battle.contains("DuelLayoutMetrics.PORTRAIT_LANE_WIDTH_FRACTION"))
        assertTrue(stone.contains("val columns = ShadowRules.COLUMNS"))
        assertTrue(battle.contains("DuelPowerCrest(snapshot.cpuPower, isPlayer = false)"))
        assertTrue(battle.contains("DuelPowerCrest(snapshot.playerPower, isPlayer = true)"))
        assertTrue(battle.contains("DuelGoldTurnDial("))
        assertTrue(battle.contains("DuelCardCover(\\n                            chosenCover"))
        assertTrue(battle.contains("drawBehind {"))
    }

    @Test fun cardFramesPreserveSourceArtworkWithoutCoveringNameStrip() {
        val visuals = source("ShadowBattleVisuals.kt")
        assertTrue(visuals.contains("DuelCardArtwork(card, Modifier.fillMaxSize()"))
        assertFalse(
            "A composited name footer obscures original card art",
            visuals.contains("Text(card.name, color = DuelPalette.cream")
        )
        assertTrue(visuals.contains("power > card.numericPower()"))
    }
}
