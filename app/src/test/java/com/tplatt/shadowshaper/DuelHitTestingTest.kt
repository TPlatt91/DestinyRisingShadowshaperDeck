package com.tplatt.shadowshaper

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.*
import org.junit.Test

/**
 * These are local JVM tests. They do not launch Android, GitHub Actions, or CI.
 */
class DuelHitTestingTest {
    private val cells = mapOf(
        "0-1-2" to Rect(200f, 100f, 270f, 180f),
        "0-0-3" to Rect(280f, 100f, 350f, 180f),
        "1-0-2" to Rect(200f, 5f, 270f, 85f)
    )

    @Test fun validPlayerDropFindsCorrectRowAndColumn() {
        assertEquals(DuelDropCell(1, 2),
            DuelHitTesting.playerSlotAt(Offset(240f, 140f), cells))
        assertEquals(DuelDropCell(0, 3),
            DuelHitTesting.playerSlotAt(Offset(320f, 120f), cells))
    }

    @Test fun enemySlotsNeverAcceptPlayerCards() {
        assertNull(DuelHitTesting.playerSlotAt(Offset(230f, 40f), cells))
    }

    @Test fun dropsOutsideAnySlotAreRejected() {
        assertNull(DuelHitTesting.playerSlotAt(Offset(900f, 900f), cells))
        assertNull(DuelHitTesting.playerSlotAt(Offset(-10f, 120f), cells))
    }

    @Test fun malformedAndOutOfBoundsIdsAreIgnored() {
        val invalid = mapOf(
            "0-9-0" to Rect(0f, 0f, 50f, 50f),
            "wrong" to Rect(60f, 0f, 100f, 50f)
        )
        assertNull(DuelHitTesting.playerSlotAt(Offset(20f, 20f), invalid))
        assertNull(DuelHitTesting.playerSlotAt(Offset(70f, 20f), invalid))
    }

    @Test fun dragOverlayUsesRootRelativeNotWindowCoordinates() {
        val absolutePointer = Offset(440f, 270f)
        val windowRoot = Offset(24f, 50f)
        val overlay = DuelHitTesting.floatingCardTopLeft(
            absolutePointer, windowRoot, 96f, 130f
        )
        assertEquals(368f, overlay.x, 0.01f)
        assertEquals(136.8f, overlay.y, 0.01f)
    }
    @Test fun seventhColumnCanAcceptDropButEighthCannot() {
        val lastColumn = mapOf(
            "0-1-6" to Rect(350f, 100f, 402f, 174f),
            "0-1-7" to Rect(410f, 100f, 462f, 174f)
        )
        assertEquals(
            DuelDropCell(1, 6),
            DuelHitTesting.playerSlotAt(Offset(370f, 130f), lastColumn)
        )
        assertNull(
            DuelHitTesting.playerSlotAt(Offset(430f, 130f), lastColumn)
        )
    }

    @Test fun visibleCardCenterReachesLegalSlotEvenWhenFingerIsBelowIt() {
        val legal = setOf(DuelDropCell(1, 2))
        // The floating artwork is drawn 64% of a card above the finger,
        // so its actual center lands 14% of the card height above the touch.
        assertEquals(
            DuelDropCell(1, 2),
            DuelHitTesting.playerSlotForDrop(
                Offset(240f, 192f), cells, 130f, legal
            )
        )
    }

    @Test fun fingertipDirectlyInsideLegalPositionAlwaysWins() {
        assertEquals(
            DuelDropCell(1, 2),
            DuelHitTesting.playerSlotForDrop(
                Offset(240f, 140f), cells, 130f, setOf(DuelDropCell(1, 2))
            )
        )
    }

    @Test fun releaseJustOutsideGlowingEdgeCanStillDropSameCard() {
        // The visible card center is two px beyond the right edge. The
        // tolerance is limited to part of a single real slot rectangle.
        assertEquals(
            DuelDropCell(1, 2),
            DuelHitTesting.playerSlotForDrop(
                Offset(272f, 180f), cells, 130f, setOf(DuelDropCell(1, 2))
            )
        )
    }

    @Test fun dragCannotSnapOntoOccupiedOrIllegalHumanPosition() {
        val occupied = cells + ("0-1-1" to Rect(120f, 100f, 190f, 180f))
        assertNull(
            DuelHitTesting.playerSlotForDrop(
                Offset(160f, 145f), occupied, 130f,
                setOf(DuelDropCell(1, 2))
            )
        )
    }

    @Test fun dragCannotDropOnCpuSideOrOutsideAnyHighlightedSlot() {
        assertNull(
            DuelHitTesting.playerSlotForDrop(
                Offset(240f, 40f), cells, 130f,
                setOf(DuelDropCell(1, 2))
            )
        )
        assertNull(
            DuelHitTesting.playerSlotForDrop(
                Offset(900f, 900f), cells, 130f,
                setOf(DuelDropCell(1, 2))
            )
        )
        assertNull(
            DuelHitTesting.playerSlotForDrop(
                Offset(240f, 140f), cells, 130f,
                emptySet()
            )
        )
    }

    @Test fun playerCanDragDirectlyIntoLegalSeventhColumn() {
        val last = mapOf("0-1-6" to Rect(350f, 100f, 400f, 180f))
        assertEquals(
            DuelDropCell(1, 6),
            DuelHitTesting.playerSlotForDrop(
                Offset(380f, 147f), last, 130f, setOf(DuelDropCell(1, 6))
            )
        )
    }


}
