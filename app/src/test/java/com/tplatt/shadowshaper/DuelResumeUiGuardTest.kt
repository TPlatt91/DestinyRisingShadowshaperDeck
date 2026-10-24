package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * UI-source-level guards for the save/replay/final-board requirements.
 * JVM only; no emulator and never GitHub Actions.
 */
class DuelResumeUiGuardTest {
    private fun source(file: String): String =
        listOf(
            File("src/main/java/com/tplatt/shadowshaper/$file"),
            File("app/src/main/java/com/tplatt/shadowshaper/$file")
        ).firstOrNull(File::isFile)?.readText()
            ?: error("$file not available")
    @Test fun launcherShowsContinueOnlyWhenUnfinishedSaveExists() {
        val home = source("ShadowComposeActivity.kt")
        assertTrue(home.contains("hasUnfinishedMatch = remember(resumePulse)"))
        assertTrue(home.contains("matchRepository.load() != null"))
        assertTrue(home.contains("\"CONTINUE BATTLE\""))
        assertTrue(home.contains("\"NEW BATTLE\""))
        assertTrue(home.contains("putExtra(\"resume_last_match\", true)"))
        assertTrue(home.contains("override fun onResume()"))
    }

    @Test fun ordinaryBattleOffersContinueOrFreshStartAndDoesNotOverwriteSave() {
        val battle = source("ShadowBattleActivity.kt")
        assertTrue(battle.contains("\"CONTINUE LAST MATCH?\""))
        assertTrue(battle.contains("resumeMatch(savedAtLaunch)"))
        assertTrue(battle.contains("matchRepository.clear()"))
        assertTrue(battle.contains("\"START NEW\""))
        assertTrue(battle.contains("restored.resume(saved.engine)"))
        assertTrue(battle.contains("freshOpeningAudio = false"))
        assertTrue(battle.contains("resumedAtRevision = restored.engine.placementRevision"))
        assertTrue(battle.contains("engine.placementRevision > resumedAtRevision"))
        assertTrue(battle.contains("resumedAtRevision = -1"))
        assertTrue(battle.contains("ShadowMatchCodec.capture(live)"))
        assertTrue(battle.contains("candidate.start(chosenDeck, cpu)"))
        assertTrue(battle.contains("System.nanoTime()"))
        assertTrue(battle.contains("setCheckpointListener"))
    }

    @Test fun resultHasBoardLogAndDirectResultsReturn() {
        val result = source("DuelResultPresentation.kt")
        val battle = source("ShadowBattleActivity.kt")
        assertTrue(result.contains("\"VIEW BOARD\""))
        assertTrue(result.contains("\"BATTLE LOG\""))
        assertTrue(result.contains("\"BATTLE AGAIN\""))
        assertTrue(result.contains("\"SELECT DECKS\""))
        assertTrue(result.contains("onInspectBoard: () -> Unit"))
        assertTrue(result.contains("onReviewLog: () -> Unit"))
        assertTrue(battle.contains("inspectingFinalBoard = true"))
        assertTrue(battle.contains("inspectingFinalBoard = false"))
        assertTrue(battle.contains("reviewingBoard = inspectingFinalBoard"))
        assertTrue(source("DuelBattleChrome.kt").contains("Return to match results"))
        assertTrue(battle.contains("if (!snapshot.finished &&"))
        assertTrue(battle.contains("snapshot?.finished == true && !inspectingFinalBoard"))
        assertTrue(battle.contains("!showLog && inspectedUnit == null && inspectedCard == null"))
        assertTrue(battle.contains("onRematch = {"))
        assertTrue(battle.contains("startMatch(selectedCpuFaction)"))
        assertTrue(battle.contains("onReviewLog = { showLog = true }"))
        assertTrue(battle.contains("onHome = onExit"))
    }

    @Test fun journalCanBeReviewedAndCardsInspectedWithoutFinishingEarly() {
        val battle = source("ShadowBattleActivity.kt")
        assertTrue(battle.contains("else if (unit != null) Modifier.clickable { inspectedUnit = unit }"))
        assertTrue(battle.contains("if (showLog && engine != null) {"))
        assertTrue(battle.contains("engine.fullJournal.forEachIndexed"))
        assertTrue(battle.contains("onClose = { inspectedUnit = null; inspectedCard = null }"))
        assertTrue(battle.contains("snapshot?.finished == true -> Unit"))
    }

    @Test fun recoverySnapshotIncludesAllEffectStateAndOrderedDecks() {
        val codec = source("ShadowMatchState.kt")
        val engine = source("ShadowEngine.kt")
        val repo = source("ShadowMatchRepository.kt")
        assertTrue(codec.contains("randomState: Random"))
        assertTrue(codec.contains("sniperLastHits: List<HitState>"))
        assertTrue(codec.contains("scorchEffects: List<ScorchState>"))
        assertTrue(codec.contains("timedBonuses: List<BonusState>"))
        assertTrue(codec.contains("pendingUnit: Int?"))
        assertTrue(codec.contains("pendingCaster: Int?"))
        assertTrue(codec.contains("val journal: List<String>"))
        assertTrue(codec.contains("val fullJournal: List<String>"))
        assertTrue(codec.contains("deck = side.drawPile.map { it.name }"))
        assertTrue(codec.contains("hand = side.hand.map { it.name }"))
        assertTrue(codec.contains("e.rng = state.randomState"))
        assertTrue(engine.contains("@JvmField var rng = Random(seed)"))
        assertTrue(repo.contains("AtomicFile("))
        assertTrue(repo.contains("file.finishWrite(stream)"))
        assertTrue(repo.contains("!save.engine.finished"))
        assertFalse(repo.contains("getExternalStorageDirectory"))
    }
}
