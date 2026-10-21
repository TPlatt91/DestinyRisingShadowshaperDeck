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
}
