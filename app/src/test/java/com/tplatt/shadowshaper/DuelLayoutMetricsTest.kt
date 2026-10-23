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
        assertEquals(80, DuelLayoutMetrics.leftHudWidth(740))
        assertEquals(112, DuelLayoutMetrics.rightHudWidth(740))
        assertEquals(102, DuelLayoutMetrics.leftHudWidth(900))
        assertEquals(132, DuelLayoutMetrics.rightHudWidth(900))
        assertEquals(6, ShadowRules.COLUMNS)
    }
    @Test fun portraitBoardHeightUsesPhoneWidthInsteadOfStretchingAllRows() {
        assertEquals(358.8f, DuelLayoutMetrics.portraitBoardHeight(390, 820), 0.01f)
        assertEquals(294.4f, DuelLayoutMetrics.portraitBoardHeight(320, 630), 0.01f)
        assertEquals(124f, DuelLayoutMetrics.portraitBoardHeight(520, 420), 0.01f)
        assertEquals(322f, DuelLayoutMetrics.portraitBoardHeight(390, 640), 0.01f)
    }

    @Test fun portraitHandDockIsRoomierOnTallPhonesButBounded() {
        assertEquals(122, DuelLayoutMetrics.portraitHandHeight(610))
        assertEquals(144, DuelLayoutMetrics.portraitHandHeight(680))
        assertEquals(156, DuelLayoutMetrics.portraitHandHeight(820))
    }

    @Test fun handCardsOnlyOverlapWhenThePhoneCannotFitThemSideBySide() {
        assertEquals(0, DuelLayoutMetrics.portraitHandSpacing(1, 390))
        assertEquals(3, DuelLayoutMetrics.portraitHandSpacing(4, 390))
        assertEquals(-16, DuelLayoutMetrics.portraitHandSpacing(5, 390))
        assertEquals(-19, DuelLayoutMetrics.portraitHandSpacing(8, 390))
    }


}
