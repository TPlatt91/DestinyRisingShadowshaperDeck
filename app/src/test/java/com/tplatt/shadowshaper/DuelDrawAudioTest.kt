package com.tplatt.shadowshaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Actual draw counters, not guesses from mutable hand sizes. */
class DuelDrawAudioTest {
    @Test fun oneSuccessfulDrawPlaysOneCardSlide() {
        val previous = DuelDrawAudioSnapshot(4, 5, 0, 0)
        val next = DuelDrawAudioSnapshot(5, 5, 0, 0)
        val change = next.since(previous)
        assertEquals(1, change.playerDrawn)
        assertEquals(0, change.opponentDrawn)
        assertEquals(1, change.totalDrawn)
        assertEquals(DuelSound.DRAW_SINGLE, change.sound)
    }

    @Test fun kevarrhCanDrawTwoInOneResolution() {
        val previous = DuelDrawAudioSnapshot(5, 4, 0, 0)
        val change = DuelDrawAudioSnapshot(7, 4, 0, 0).since(previous)
        assertEquals(2, change.playerDrawn)
        assertEquals(DuelSound.DRAW_MULTI, change.sound)
    }

    @Test fun roundDrawsBothDecksWithoutGuessingFromHandCounts() {
        val previous = DuelDrawAudioSnapshot(5, 4, 0, 0)
        val change = DuelDrawAudioSnapshot(6, 5, 0, 0).since(previous)
        assertEquals(2, change.totalDrawn)
        assertEquals(DuelSound.DRAW_MULTI, change.sound)
    }

    @Test fun exhaustedDeckUsesWarningOnlyWhenDrawAttemptFails() {
        val previous = DuelDrawAudioSnapshot(15, 15, 0, 0)
        val change = DuelDrawAudioSnapshot(15, 15, 1, 1).since(previous)
        assertEquals(0, change.totalDrawn)
        assertEquals(2, change.emptyAttempts)
        assertEquals(DuelSound.DECK_EMPTY, change.sound)
    }

    @Test fun noNewDrawAndUnchangedSnapshotRemainSilent() {
        val previous = DuelDrawAudioSnapshot(5, 4, 0, 0)
        val change = previous.since(previous)
        assertEquals(0, change.totalDrawn)
        assertEquals(0, change.emptyAttempts)
        assertNull(change.sound)
    }

    @Test fun resetsCannotMakeNegativeOrFakeDrawEvents() {
        val previous = DuelDrawAudioSnapshot(17, 16, 2, 1)
        val current = DuelDrawAudioSnapshot(3, 4, 0, 0)
        assertEquals(0, current.since(previous).totalDrawn)
        assertEquals(0, current.since(previous).emptyAttempts)
        assertNull(current.since(previous).sound)
    }

    @Test fun engineCountsOnlyCardsActuallyTakenFromDrawPile() {
        val paths = listOf(
            File("src/main/java/com/tplatt/shadowshaper/ShadowEngine.kt"),
            File("app/src/main/java/com/tplatt/shadowshaper/ShadowEngine.kt")
        )
        val source = paths.firstOrNull(File::isFile)?.readText()
            ?: error("ShadowEngine.kt missing")
        assertTrue(source.contains("if (side.drawPile.isNotEmpty())"))
        assertTrue(source.contains("if (side === human) humanDrawTotal++"))
        assertTrue(source.contains("else if (side === cpu) cpuDrawTotal++"))
        assertTrue(source.contains("if (side === human) humanEmptyDrawTotal++"))
        assertTrue(source.contains("else if (side === cpu) cpuEmptyDrawTotal++"))
        assertTrue(source.contains("Collections.shuffle(human.drawPile, rng)"))
        assertTrue(source.contains("Collections.shuffle(cpu.drawPile, rng)"))
    }

    @Test fun newCuesStayQuietUnlessTheirActionsExist() {
        val registered = DuelSound.entries.map { it.assetName }.toSet()
        assertTrue("draw_single" in registered)
        assertTrue("draw_multi" in registered)
        assertTrue("shuffle" in registered)
        assertTrue("opening_deal" in registered)
        assertTrue("mulligan" in registered)
        assertTrue("discard" in registered)
        assertTrue("card_lift" in registered)
        assertTrue("target_lock" in registered)
        assertTrue("ability_trigger" in registered)
        assertTrue("card_break" in registered)
        assertTrue("deck_empty" in registered)
    }
}
