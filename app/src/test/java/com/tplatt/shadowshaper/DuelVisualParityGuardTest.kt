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
