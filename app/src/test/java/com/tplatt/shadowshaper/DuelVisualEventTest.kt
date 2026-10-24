package com.tplatt.shadowshaper

import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure presentation classification tests; no UI device or game mutations. */
class DuelVisualEventTest {
    @Test fun engineDamageAndDestructionAreImpactEvents() {
        assertEquals(DuelVisualEventKind.IMPACT, DuelVisualEvent.kind("DAMAGE 3 ▶ Dreg"))
        assertEquals(DuelVisualEventKind.IMPACT, DuelVisualEvent.kind("DESTROYED ▶ Dreg"))
        assertEquals(DuelVisualEventKind.IMPACT, DuelVisualEvent.kind("Exploder Shank DETONATES ▶ 2"))
        assertEquals("COMBAT IMPACT", DuelVisualEvent.caption("DAMAGE 2 ▶ Psion"))
    }

    @Test fun actualPlacementAndSummonEventsUseDeployCue() {
        assertEquals(DuelVisualEventKind.SUMMON, DuelVisualEvent.kind("YOU ▶ Thrall • Front 3"))
        assertEquals(DuelVisualEventKind.SUMMON, DuelVisualEvent.kind("CPU ▶ Legionary"))
        assertEquals(DuelVisualEventKind.SUMMON, DuelVisualEvent.kind("SUMMON ▶ Dreg at front 2"))
    }

    @Test fun buffsAndConditionsUseDistinctVisualCues() {
        assertEquals(DuelVisualEventKind.BUFF, DuelVisualEvent.kind("BUFF +2 ▶ Dreg"))
        assertEquals(DuelVisualEventKind.BUFF, DuelVisualEvent.kind("INSPIRED ▶ Legionary"))
        assertEquals(DuelVisualEventKind.STATUS, DuelVisualEvent.kind("SUPPRESSED ▶ Wretch"))
        assertEquals(DuelVisualEventKind.STATUS, DuelVisualEvent.kind("SCORCHED ▶ Wretch"))
        assertEquals(DuelVisualEventKind.STATUS, DuelVisualEvent.kind("RELOCATED ▶ Vandal"))
    }

    @Test fun overshieldAndPreventedDamageDoNotLookLikeHits() {
        assertEquals(DuelVisualEventKind.SHIELD, DuelVisualEvent.kind("OVERSHIELD blocks damage ▶ Thrall"))
        assertEquals(DuelVisualEventKind.SHIELD, DuelVisualEvent.kind("DAMAGE PREVENTED ▶ Thrall"))
        assertEquals("SHIELD ACTIVATED", DuelVisualEvent.caption("OVERSHIELD granted ▶ Acolyte"))
    }

    @Test fun unrecognizedJournalEntriesRemainGeneric() {
        assertEquals(DuelVisualEventKind.OTHER, DuelVisualEvent.kind("Round 2 — 2 play points."))
        assertEquals(DuelVisualEventKind.OTHER, DuelVisualEvent.kind(""))
        assertEquals(DuelVisualEventKind.OTHER, DuelVisualEvent.kind("SUMMON RESTRICTED: Rock cannot be summoned"))
        assertEquals(DuelVisualEventKind.OTHER, DuelVisualEvent.kind("SUMMON: No open slot for Thrall"))
    }
}
