package com.tplatt.shadowshaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** JVM-only sizing checks. They require no Android device or GitHub Actions. */
class DuelLayoutMetricsTest {
    @Test fun cardsFitSmallLandscapeSlotsWithoutCropping() {
        val slot = DuelLayoutMetrics.fittingCard(43f, 62f)
        assertTrue(slot.width <= 43f)
        assertTrue(slot.height <= 62f)
        assertEquals(0.72f, slot.width / slot.height, 0.001f)
    }

    @Test fun wideBoardSlotsStillUsePortraitCardRatio() {
        val slot = DuelLayoutMetrics.fittingCard(110f, 66f)
        assertTrue(slot.width < 55f)
        assertTrue(slot.height <= 66f)
        assertEquals(0.72f, slot.width / slot.height, 0.001f)
    }

    @Test fun nonpositiveMeasurementsCannotProduceInvalidCards() {
        assertEquals(0f, DuelLayoutMetrics.fittingCard(0f, 90f).width, 0.001f)
        assertEquals(0f, DuelLayoutMetrics.fittingCard(30f, -1f).height, 0.001f)
    }

    @Test fun handDockLeavesRoomForBattlefieldInLandscape() {
        assertEquals(94, DuelLayoutMetrics.handHeight(320))
        assertEquals(108, DuelLayoutMetrics.handHeight(390))
        assertEquals(128, DuelLayoutMetrics.handHeight(430))
    }

    @Test fun hudNarrowModeReclaimsBoardWidthWithoutChangingSlotCount() {
        assertEquals(86, DuelLayoutMetrics.leftHudWidth(740))
        assertEquals(112, DuelLayoutMetrics.rightHudWidth(740))
        assertEquals(110, DuelLayoutMetrics.leftHudWidth(900))
        assertEquals(132, DuelLayoutMetrics.rightHudWidth(900))
        assertEquals(6, ShadowRules.COLUMNS)
    }
}
