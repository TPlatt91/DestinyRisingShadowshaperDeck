package com.tplatt.shadowshaper

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Exact thresholds read from the Wolf card in recording 38645.mp4. */
class ShadowSpecialCardRulesTest {
    @Test fun fewerThanSixAlliesCannotTriggerWolf() {
        assertFalse(ShadowSpecialCardRules.wolfHasEnoughUnits(4))
        assertFalse(ShadowSpecialCardRules.wolfHasEnoughUnits(5))
    }

    @Test fun wolfItselfCountsAsTheSixthAlliedUnit() {
        assertTrue(ShadowSpecialCardRules.wolfHasEnoughUnits(6))
        assertTrue(ShadowSpecialCardRules.wolfHasEnoughUnits(7))
    }

    @Test fun exactlyFivePowerIsAValidTarget() {
        assertFalse(ShadowSpecialCardRules.wolfTargetEligible(4))
        assertTrue(ShadowSpecialCardRules.wolfTargetEligible(5))
        assertTrue(ShadowSpecialCardRules.wolfTargetEligible(9))
    }

    @Test fun aHighPowerTargetStillRequiresEnoughAllies() {
        assertFalse(ShadowSpecialCardRules.wolfCanDestroy(5, 9))
        assertFalse(ShadowSpecialCardRules.wolfCanDestroy(6, 4))
        assertTrue(ShadowSpecialCardRules.wolfCanDestroy(6, 5))
    }
}
