package com.tplatt.shadowshaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Pure JVM verification of bundled songs and restored, scrollable inspector. */
class DuelMusicAndInspectorGuardTest {
    private fun source(name: String): String =
        listOf(
            File("src/main/java/com/tplatt/shadowshaper/$name"),
            File("app/src/main/java/com/tplatt/shadowshaper/$name")
        ).firstOrNull(File::isFile)?.readText() ?: error("Missing $name")

    private fun trackDirectory(): File =
        listOf(
            File("src/main/assets/background_music"),
            File("app/src/main/assets/background_music")
        ).firstOrNull(File::isDirectory) ?: error("Bundled music directory missing")

    @Test fun allThirteenNamedExpansionsMapToRealPackagedOpusFiles() {
        assertEquals(13, DuelMusicCatalog.tracks.size)
        assertEquals((1..13).map { "%03d".format(it) },
            DuelMusicCatalog.tracks.map { it.id })
        val directory = trackDirectory()
        val names = DuelMusicCatalog.tracks.map { it.assetFile }
        assertEquals(13, names.distinct().size)
        DuelMusicCatalog.tracks.forEach {
            val file = File(directory, it.assetFile)
            assertTrue("Missing bundled music: ${it.assetFile}", file.isFile)
            assertTrue("Empty music file: ${it.assetFile}", file.length() > 50_000L)
            assertTrue(it.title.isNotBlank())
            assertTrue(it.assetPath.startsWith("background_music/"))
        }
    }

    @Test fun originalReleasesAreBaseGamesNotDlc() {
        assertTrue(DuelMusicCatalog.tracks[0].title.contains("Base Game"))
        assertTrue(DuelMusicCatalog.tracks[3].title.contains("Base Game"))
        assertTrue(DuelMusicCatalog.tracks[1].title.contains("The Taken King"))
        assertTrue(DuelMusicCatalog.tracks[2].title.contains("Rise of Iron"))
        assertTrue(DuelMusicCatalog.tracks.last().title.contains("The Edge of Fate"))
        assertEquals("DESTINY", DuelMusicCatalog.tracks[0].series)
        assertEquals("DESTINY 2", DuelMusicCatalog.tracks.last().series)
        assertEquals(DuelMusicCatalog.tracks[9],
            DuelMusicCatalog.byId("010"))
        assertTrue(DuelMusicCatalog.byId("999") == null)
    }

    @Test fun musicHasSeparatePreferencesAndRespectsAndroidActivityLifecycle() {
        val player = source("DuelBackgroundMusic.kt")
        val battle = source("ShadowBattleActivity.kt")
        val gradle = listOf(File("app/build.gradle.kts"), File("build.gradle.kts"))
            .firstOrNull(File::isFile)?.readText() ?: error("Gradle script missing")
        assertTrue(gradle.contains("noCompress += \"opus\""))
        assertTrue(player.contains("MediaPlayer()"))
        assertTrue(player.contains("instance.prepareAsync()"))
        assertTrue(player.contains("instance.isLooping = true"))
        assertTrue(player.contains("openFd(song.assetPath)"))
        assertTrue(player.contains("setDataSource("))
        assertTrue(player.contains("music_enabled"))
        assertTrue(player.contains("music_volume"))
        assertTrue(player.contains("selected_track"))
        assertTrue(player.contains("AudioFocusRequest.Builder("))
        assertTrue(player.contains("AudioManager.AUDIOFOCUS_LOSS"))
        assertTrue(battle.contains("backgroundMusic?.onForeground()"))
        assertTrue(battle.contains("backgroundMusic?.onBackground()"))
        assertTrue(battle.contains("backgroundMusic?.close()"))
        assertFalse(player.contains("SoundPool.Builder()"))
        assertFalse(player.contains("http://"))
        assertFalse(player.contains("https://"))
    }

    @Test fun settingsSelectEveryTrackAndPersistIndependentVolumes() {
        val battle = source("ShadowBattleActivity.kt")
        val popup = source("DuelPopupChrome.kt")
        assertTrue(battle.contains("title = \"BACKGROUND MUSIC\""))
        assertTrue(battle.contains("volumeLabel = \"MUSIC VOLUME\""))
        assertTrue(battle.contains("label = \"SELECT MUSIC TRACK\""))
        assertTrue(battle.contains("title = \"SELECT MUSIC\""))
        assertTrue(battle.contains("DuelMusicCatalog.tracks.forEach"))
        assertTrue(battle.contains("music.selectedTrackId = track.id"))
        assertTrue(battle.contains("music.enabled = value"))
        assertTrue(battle.contains("music.volume = value"))
        assertTrue(battle.contains("sounds.enabled = value"))
        assertTrue(battle.contains("sounds.volume = value"))
        assertTrue(popup.contains("title: String = \"SOUND EFFECTS\""))
        assertTrue(popup.contains("volumeLabel: String = \"SFX VOLUME\""))
    }

    @Test fun inspectorCloseButtonCannotConsumeCardArtAndEffectViewport() {
        val popup = source("DuelPopupChrome.kt")
        val inspector = source("DuelCardInspection.kt")
        assertTrue(popup.contains(".height(if (style == DuelPopupActionStyle.QUIET) 43.dp else 50.dp)"))
        assertFalse(popup.contains(".heightIn(min = if (style == DuelPopupActionStyle.QUIET)"))
        assertTrue(inspector.contains(".height(61.dp)"))
        assertTrue(inspector.contains(".fillMaxHeight(0.88f)"))
        assertTrue(inspector.contains("DuelCardFrame("))
        assertTrue(inspector.contains(".aspectRatio(0.72f)"))
        assertTrue(inspector.contains(".weight(1f)"))
        assertTrue(inspector.contains(".verticalScroll(rememberScrollState())"))
        assertTrue(inspector.contains("card.effect.ifBlank"))
        assertTrue(inspector.contains("ShadowKeywordDefinitions("))
        assertTrue(inspector.contains("onClick = onClose"))
        assertTrue(inspector.contains("label = \"CLOSE\""))
    }
}
