package com.tplatt.shadowshaper

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * JVM source guards for Android-only SoundPool integration. Real playback
 * must also be verified with a local APK; never run GitHub Actions.
 */
class DuelAudioIntegrationGuardTest {
    private fun source(filename: String) =
        listOf(
            File("src/main/java/com/tplatt/shadowshaper/$filename"),
            File("app/src/main/java/com/tplatt/shadowshaper/$filename")
        ).firstOrNull(File::isFile)?.readText()
            ?: error("Audio integration source missing: $filename")

    @Test fun soundEngineIsCachedOfflineAndDisposedWithBattleScreen() {
        val runtime = source("DuelSoundPlayer.kt")
        val screen = source("ShadowBattleActivity.kt")
        assertTrue(runtime.contains("SoundPool.Builder()"))
        assertTrue(runtime.contains("Dispatchers.IO"))
        assertTrue(runtime.contains("DuelSoundSynthesis.wav(sound)"))
        assertTrue(runtime.contains("shadow_sfx/"))
        assertTrue(runtime.contains("fun close()"))
        assertTrue(runtime.contains("if (released || !enabled) return"))
        assertTrue(screen.contains("LaunchedEffect(sounds) { sounds.prepare() }"))
        assertTrue(screen.contains("DisposableEffect(sounds) { onDispose { sounds.close() } }"))
        assertFalse(runtime.contains("http://"))
        assertFalse(runtime.contains("https://"))
    }

    @Test fun everyRealUnitTransmatPlaysBeforeMatchingImpact() {
        val anim = source("DuelBoardAnimations.kt")
        assertTrue(anim.contains("DuelSoundEvents.transmatFor(unit.card.faction)"))
        assertTrue(anim.contains("sounds?.play(DuelSound.CARD_LAND)"))
        assertTrue(anim.contains("DuelTransmatResolver.PRECHARGE_MS +"))
        assertTrue(anim.contains("DuelLandingMotion.DURATION_MS * .72f"))
        assertTrue(anim.contains("animatedRevision != placementRevision"))
        assertFalse(anim.contains("engine.play("))
        assertFalse(anim.contains("engine.damage("))
    }

    @Test fun audioOnlyTracksSuccessfulActionsAndActualJournalChanges() {
        val screen = source("ShadowBattleActivity.kt")
        val classifier = source("DuelSoundDesign.kt")
        assertTrue(screen.contains("DuelSoundEvents.newLines(prior, journalNow)"))
        assertTrue(screen.contains("lastAudioJournal = journalNow"))
        assertTrue(screen.contains("lastAudioTurn = key"))
        assertTrue(screen.contains("matchOutcomePlayed = true"))
        assertTrue(screen.contains("sounds.play(DuelSound.CARD_SELECT)"))
        assertTrue(classifier.contains("DuelSound.DAMAGE"))
        assertTrue(classifier.contains("DAMAGE PREVENTED"))
        assertTrue(classifier.contains("DESTROYED ▶"))
        assertTrue(classifier.contains("DuelSound.TRANSMAT_DEFAULT"))
    }

    @Test fun muteAndVolumeSettingsPersistWithoutChangingGameRules() {
        val runtime = source("DuelSoundPlayer.kt")
        val screen = source("ShadowBattleActivity.kt")
        val popup = source("DuelPopupChrome.kt")
        assertTrue(runtime.contains("getSharedPreferences("))
        assertTrue(runtime.contains("sound_on"))
        assertTrue(runtime.contains("sfx_volume"))
        assertTrue(popup.contains("SOUND EFFECTS"))
        assertTrue(popup.contains("SFX VOLUME"))
        assertTrue(screen.contains("TEST SOUND"))
        assertTrue(screen.contains("sounds.enabled = value"))
        assertTrue(screen.contains("sounds.volume = value"))
        assertFalse(runtime.contains("engine.endTurn("))
        assertFalse(runtime.contains("engine.play("))
    }
    @Test fun drawSoundsAreBasedOnEngineDrawEventsRatherThanHandSizeGuesswork() {
        val engine = source("ShadowEngine.kt")
        val sounds = source("DuelSoundDesign.kt")
        val battle = source("ShadowBattleActivity.kt")
        assertTrue(engine.contains("humanDrawTotal++"))
        assertTrue(engine.contains("cpuDrawTotal++"))
        assertTrue(engine.contains("humanEmptyDrawTotal++"))
        assertTrue(engine.contains("cpuEmptyDrawTotal++"))
        assertTrue(sounds.contains("data class DuelDrawAudioSnapshot("))
        assertTrue(sounds.contains("DuelSound.DRAW_MULTI"))
        assertTrue(sounds.contains("DuelSound.DRAW_SINGLE"))
        assertTrue(battle.contains("DuelDrawAudioSnapshot("))
        assertTrue(battle.contains("lastAudioDraws = drawNow"))
        assertTrue(battle.contains("change.totalDrawn.coerceAtMost(4)"))
        assertTrue(battle.contains("snapshot?.playerHand, snapshot?.cpuHand"))
    }

    @Test fun openingShuffleAndNewActionsPlayOnlyAtConfirmedInteractionPoints() {
        val battle = source("ShadowBattleActivity.kt")
        assertTrue(battle.contains("LaunchedEffect(current, freshOpeningAudio)"))
        assertTrue(battle.contains("sounds.isReady(DuelSound.SHUFFLE)"))
        assertTrue(battle.contains("sounds.play(DuelSound.OPENING_DEAL)"))
        assertTrue(battle.contains("sounds.play(DuelSound.CARD_LIFT)"))
        assertTrue(battle.contains("sounds.play(DuelSound.TARGET_LOCK)"))
        assertTrue(battle.contains("sounds.play(DuelSound.ABILITY_TRIGGER)"))
        assertTrue(battle.contains("sounds.play(DuelSound.CARD_BREAK)"))
        assertTrue(battle.contains("if (canPlace)"))
        assertFalse(battle.contains("sounds.play(DuelSound.MULLIGAN)"))
        assertFalse(battle.contains("sounds.play(DuelSound.DISCARD)"))
    }

}
