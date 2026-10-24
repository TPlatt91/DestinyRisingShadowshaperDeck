package com.tplatt.shadowshaper

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** JVM-only sound classification and generated-audio integrity checks. */
class DuelSoundDesignTest {
    @Test fun fiveRequestedTransmatStylesMatchMetadata() {
        for (name in listOf("Human", "Humans", "Exo", "Exos", "Awoken")) {
            assertEquals(DuelSound.TRANSMAT_DEFAULT, DuelSoundEvents.transmatFor(name))
        }
        assertEquals(DuelSound.TRANSMAT_HIVE, DuelSoundEvents.transmatFor("Hive"))
        assertEquals(DuelSound.TRANSMAT_CABAL, DuelSoundEvents.transmatFor("Cabal"))
        assertEquals(DuelSound.TRANSMAT_FALLEN, DuelSoundEvents.transmatFor("Fallen"))
        assertEquals(DuelSound.TRANSMAT_VEX, DuelSoundEvents.transmatFor("Vex"))
        assertEquals(DuelSound.TRANSMAT_DEFAULT, DuelSoundEvents.transmatFor(null))
        assertEquals(DuelSound.TRANSMAT_DEFAULT, DuelSoundEvents.transmatFor("Neutral"))
    }

    @Test fun soundEventsRespondOnlyToActualEngineJournalActions() {
        assertEquals(DuelSound.DAMAGE, DuelSoundEvents.forJournalLine("DAMAGE 2 ▶ Thrall"))
        assertEquals(DuelSound.DESTROY, DuelSoundEvents.forJournalLine("DESTROYED ▶ Dreg"))
        assertEquals(DuelSound.DESTROY, DuelSoundEvents.forJournalLine("Exploder Shank DETONATES ▶ 2"))
        assertEquals(DuelSound.SHIELD, DuelSoundEvents.forJournalLine("OVERSHIELD blocks 2 ▶ Acolyte"))
        assertEquals(DuelSound.SHIELD, DuelSoundEvents.forJournalLine("DAMAGE PREVENTED ▶ Dreg"))
        assertEquals(DuelSound.BUFF, DuelSoundEvents.forJournalLine("BUFF +1 ▶ Psion"))
        assertEquals(DuelSound.BUFF, DuelSoundEvents.forJournalLine("INSPIRED ▶ Psion"))
        assertEquals(DuelSound.STATUS, DuelSoundEvents.forJournalLine("SUPPRESSED ▶ Thrall"))
        assertEquals(DuelSound.STATUS, DuelSoundEvents.forJournalLine("RELOCATED ▶ Thrall"))
        assertNull(DuelSoundEvents.forJournalLine("CPU ▶ Hive Acolyte • front 3"))
        assertNull(DuelSoundEvents.forJournalLine("SUMMON ▶ Dreg at front 1"))
        assertNull(DuelSoundEvents.forJournalLine("SUMMON RESTRICTED: No Rock"))
        assertNull(DuelSoundEvents.forJournalLine("ROUND 2 • 2 play points"))
    }

    @Test fun rollingJournalDoesNotReplayPreviouslyHeardSounds() {
        val old = listOf("ROUND 1", "DAMAGE 2 ▶ Thrall", "BUFF +1 ▶ Dreg")
        val latest = old + listOf("DAMAGE 1 ▶ Psion", "DESTROYED ▶ Psion")
        assertEquals(latest.takeLast(2), DuelSoundEvents.newLines(old, latest))
        assertTrue(DuelSoundEvents.newLines(latest, latest).isEmpty())
        val cappedPrevious = listOf("old", "a", "b", "c")
        val cappedCurrent = listOf("b", "c", "DAMAGE 1 ▶ Dreg")
        assertEquals(listOf("DAMAGE 1 ▶ Dreg"),
            DuelSoundEvents.newLines(cappedPrevious, cappedCurrent))
        // A match reset never plays historical events from the previous duel.
        assertTrue(DuelSoundEvents.newLines(old, listOf("MATCH SEED: 2")).isEmpty())
    }

    @Test fun allTwentySevenEffectsProduceValidNonSilentPcmWav() {
        assertEquals(27, DuelSound.entries.size)
        for (sound in DuelSound.entries) {
            val wav = DuelSoundSynthesis.wav(sound)
            assertEquals(sound.name, 44 + sound.durationMs *
                DuelSoundSynthesis.RATE / 1000 * 2, wav.size)
            assertEquals("RIFF", String(wav.copyOfRange(0, 4), Charsets.US_ASCII))
            assertEquals("WAVE", String(wav.copyOfRange(8, 12), Charsets.US_ASCII))
            assertEquals("fmt ", String(wav.copyOfRange(12, 16), Charsets.US_ASCII))
            assertEquals("data", String(wav.copyOfRange(36, 40), Charsets.US_ASCII))
            assertTrue("PCM must contain audible samples: ${sound.name}",
                wav.copyOfRange(44, wav.size).any { it.toInt() != 0 })
        }
    }

    @Test fun generatedSoundsAreDeterministicAndDifferentByFaction() {
        assertArrayEquals(
            DuelSoundSynthesis.wav(DuelSound.CARD_LAND),
            DuelSoundSynthesis.wav(DuelSound.CARD_LAND)
        )
        val hive = DuelSoundSynthesis.wav(DuelSound.TRANSMAT_HIVE)
        val cabal = DuelSoundSynthesis.wav(DuelSound.TRANSMAT_CABAL)
        assertFalse(hive.contentEquals(cabal))
        assertNotEquals(DuelSound.TRANSMAT_HIVE.assetName, DuelSound.TRANSMAT_CABAL.assetName)
    }

    @Test fun soundGainAndDurationHaveSafeMobileBounds() {
        DuelSound.entries.forEach {
            assertTrue(it.gain > 0f && it.gain <= .9f)
            assertTrue(it.durationMs in 70..1000)
        }
    }
}
