package com.tplatt.shadowshaper

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Source-level regression guards for continuous soundtrack across the
 * two app Activities. Android's lifecycle/player behavior is device-tested.
 * No GitHub Actions are needed to execute these in the local Gradle project.
 */
class DuelMusicSessionGuardTest {
    private fun source(name: String): String =
        listOf(
            File("src/main/java/com/tplatt/shadowshaper/$name"),
            File("app/src/main/java/com/tplatt/shadowshaper/$name")
        ).firstOrNull(File::isFile)?.readText() ?: error("Missing $name")

    @Test fun bothActivitiesUseTheSameApplicationOwnedMusicPlayer() {
        val session = source("DuelMusicSession.kt")
        val home = source("ShadowComposeActivity.kt")
        val battle = source("ShadowBattleActivity.kt")
        assertTrue(session.contains("internal object DuelMusicSession"))
        assertTrue(session.contains("private var instance: DuelBackgroundMusic? = null"))
        assertTrue(session.contains("registerActivityLifecycleCallbacks(callbacks)"))
        assertTrue(home.contains("DuelMusicSession.get(applicationContext)"))
        assertTrue(battle.contains("DuelMusicSession.get(applicationContext)"))
        assertFalse(home.contains("DuelBackgroundMusic(applicationContext)"))
        assertFalse(battle.contains("DuelBackgroundMusic(applicationContext)"))
        assertFalse(battle.contains("backgroundMusic?.close()"))
    }

    @Test fun backgroundingPausesButSwitchingScreensDoesNotRestart() {
        val session = source("DuelMusicSession.kt")
        assertTrue(session.contains("onActivityStarted(activity: Activity)"))
        assertTrue(session.contains("onActivityStopped(activity: Activity)"))
        assertTrue(session.contains("handler.removeCallbacks(pauseWhenBackground)"))
        assertTrue(session.contains("handler.postDelayed(pauseWhenBackground, TRANSITION_GRACE_MS)"))
        assertTrue(session.contains("if (active.isEmpty()) instance?.onBackground()"))
        assertTrue(session.contains("active.add(activity)"))
        assertTrue(session.contains("active.remove(activity)"))
        assertTrue(session.contains("instance?.onForeground()"))
        assertTrue(session.contains("activity !is ShadowComposeActivity"))
        assertTrue(session.contains("activity !is ShadowBattleActivity"))
    }

    @Test fun musicRemainsOnAllMainPagesWithFourDestinations() {
        val home = source("ShadowComposeActivity.kt")
        val landing = source("ShadowLandingPage.kt")
        assertTrue(home.contains("\"home\" -> ShadowLandingPage("))
        assertTrue(home.contains("\"deck\" -> DeckScreen("))
        assertTrue(home.contains("\"collection\" -> CollectionScreen("))
        assertTrue(home.contains("\"rules\" -> RulesScreen("))
        assertTrue(landing.contains("\"SKIRMISH\""))
        assertTrue(landing.contains("\"DECK CONSTRUCTION\""))
        assertTrue(landing.contains("\"CARD COLLECTION\""))
        assertTrue(landing.contains("\"RULES\""))
    }

    @Test fun soundtrackAndSoundEffectsStayIndependentlyControlled() {
        val player = source("DuelBackgroundMusic.kt")
        val battle = source("ShadowBattleActivity.kt")
        assertTrue(player.contains("shadow_duel_background_music"))
        assertTrue(player.contains("music_enabled"))
        assertTrue(player.contains("music_volume"))
        assertTrue(player.contains("selected_track"))
        assertTrue(player.contains("loadedId == song.id"))
        assertTrue(battle.contains("music.selectedTrackId = track.id"))
        assertTrue(battle.contains("music.enabled = value"))
        assertTrue(battle.contains("music.volume = value"))
        assertTrue(battle.contains("sounds.enabled = value"))
        assertTrue(battle.contains("sounds.volume = value"))
        assertFalse(source("DuelMusicSession.kt").contains("DuelSoundPlayer("))
    }
}
