package com.tplatt.shadowshaper

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** No Android or GitHub Actions required; deterministic presentation eligibility. */
class DuelPlacementCueTest {
    @Test fun noCardIsAnimatedBeforeTheFirstPlacement() {
        assertFalse(DuelPlacementCue.isNewlyPlaced(0, -1, -1, -1, 0, 1, 0))
    }

    @Test fun onlyMostRecentPlayerSlotIsEligible() {
        assertTrue(DuelPlacementCue.isNewlyPlaced(4, 0, 1, 3, 0, 1, 3))
        assertFalse(DuelPlacementCue.isNewlyPlaced(4, 0, 1, 3, 0, 1, 2))
        assertFalse(DuelPlacementCue.isNewlyPlaced(4, 0, 1, 3, 0, 0, 3))
    }

    @Test fun sameColumnOnOppositeSideIsNotAnimated() {
        assertTrue(DuelPlacementCue.isNewlyPlaced(7, 1, 0, 4, 1, 0, 4))
        assertFalse(DuelPlacementCue.isNewlyPlaced(7, 1, 0, 4, 0, 0, 4))
    }

    @Test fun invalidLastPlacementCannotTriggerAnEntryAnimation() {
        assertFalse(DuelPlacementCue.isNewlyPlaced(1, -1, -1, -1, 1, 0, 0))
    }
}
