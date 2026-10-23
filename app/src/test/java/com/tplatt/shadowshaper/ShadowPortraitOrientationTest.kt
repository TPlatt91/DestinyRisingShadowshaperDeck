package com.tplatt.shadowshaper

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Prevent accidental landscape-only releases of the portrait companion. */
class ShadowPortraitOrientationTest {
    private fun file(vararg candidates: String): File =
        candidates.map(::File).firstOrNull { it.isFile }
            ?: error("Project source file not found: ${candidates.joinToString()}")

    @Test fun launcherAndBattleActivitiesRequirePortrait() {
        val manifest = file(
            "src/main/AndroidManifest.xml",
            "app/src/main/AndroidManifest.xml"
        ).readText()
        val portrait = Regex("""android:screenOrientation="portrait"""")
        for (activity in listOf(".ShadowComposeActivity", ".ShadowBattleActivity")) {
            val tag = Regex(
                """<activity\s+[^>]*android:name="${Regex.escape(activity)}"[^>]*>"""
            ).find(manifest)?.value
                ?: error("Missing Android activity: $activity")
            assertTrue("$activity must launch upright", portrait.containsMatchIn(tag))
        }
        assertFalse(manifest.contains("screenOrientation=\"landscape\""))
    }

    @Test fun screensDoNotForceLandscapeAtRuntime() {
        val main = file(
            "src/main/java/com/tplatt/shadowshaper/ShadowComposeActivity.kt",
            "app/src/main/java/com/tplatt/shadowshaper/ShadowComposeActivity.kt"
        ).readText()
        val battle = file(
            "src/main/java/com/tplatt/shadowshaper/ShadowBattleActivity.kt",
            "app/src/main/java/com/tplatt/shadowshaper/ShadowBattleActivity.kt"
        ).readText()
        assertTrue(main.contains("SCREEN_ORIENTATION_PORTRAIT"))
        assertTrue(battle.contains("SCREEN_ORIENTATION_PORTRAIT"))
        assertFalse(main.contains("SCREEN_ORIENTATION_LANDSCAPE"))
        assertFalse(battle.contains("SCREEN_ORIENTATION_LANDSCAPE"))
    }
}
