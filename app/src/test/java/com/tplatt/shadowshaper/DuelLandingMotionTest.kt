package com.tplatt.shadowshaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure Kotlin timeline checks; no emulator or GitHub Actions required. */
class DuelLandingMotionTest {
    @Test fun newlyPlayedCardBeginsAtActualHandPosition() {
        val frame = DuelLandingMotion.at(
            progress = 0f, originX = 140f, originY = 390f,
            cardHeightPx = 72f, clockwise = true
        )
        assertEquals(140f, frame.offsetX, .001f)
        assertEquals(390f, frame.offsetY, .001f)
        assertEquals(.83f, frame.scale, .001f)
        assertEquals(9f, frame.tiltDegrees, .001f)
        assertEquals(0f, frame.impact, .001f)
    }

    @Test fun placementArcsUpwardThenHoversBeforeDrop() {
        val travel = DuelLandingMotion.at(.28f, 120f, 420f, 70f, true)
        val hover = DuelLandingMotion.at(.50f, 120f, 420f, 70f, true)
        assertTrue(travel.offsetX > 0f && travel.offsetX < 120f)
        assertTrue(travel.offsetY < 420f)
        assertEquals(0f, hover.offsetX, .001f)
        assertTrue(hover.offsetY < -20f)
        assertTrue(hover.scale > 1.1f)
    }

    @Test fun cardSlamsOntoStoneAndReboundsBeforeSettling() {
        val fall = DuelLandingMotion.at(.62f, 0f, -55f, 72f, false)
        val slam = DuelLandingMotion.at(.72f, 0f, -55f, 72f, false)
        val rebound = DuelLandingMotion.at(.84f, 0f, -55f, 72f, false)
        assertTrue(fall.offsetY < slam.offsetY)
        assertTrue(slam.offsetY > 0f)
        assertTrue(slam.scale < 1f)
        assertEquals(1f, slam.impact, .001f)
        assertTrue(rebound.offsetY < 0f)
        assertTrue(rebound.scale > 1f)
    }

    @Test fun animationFinishesExactlyOnActualBoardPosition() {
        val frame = DuelLandingMotion.at(1f, 200f, 610f, 80f, true)
        assertEquals(0f, frame.offsetX, .001f)
        assertEquals(0f, frame.offsetY, .001f)
        assertEquals(1f, frame.scale, .001f)
        assertEquals(0f, frame.tiltDegrees, .001f)
        assertEquals(1f, frame.opacity, .001f)
        assertEquals(0f, frame.impact, .001f)
    }

    @Test fun invalidProgressCannotMoveBeyondAnimationBounds() {
        val before = DuelLandingMotion.at(-100f, 55f, 330f, 70f, false)
        val after = DuelLandingMotion.at(100f, 55f, 330f, 70f, false)
        assertEquals(55f, before.offsetX, .001f)
        assertEquals(330f, before.offsetY, .001f)
        assertEquals(0f, after.offsetY, .001f)
        assertEquals(1f, after.scale, .001f)
    }

    @Test fun oppositeSlotParityChangesTiltNotFinalPosition() {
        val a = DuelLandingMotion.at(.20f, 20f, 250f, 60f, true)
        val b = DuelLandingMotion.at(.20f, 20f, 250f, 60f, false)
        assertEquals(a.offsetX, b.offsetX, .001f)
        assertEquals(a.offsetY, b.offsetY, .001f)
        assertEquals(a.tiltDegrees, -b.tiltDegrees, .001f)
    }

    @Test fun handOriginOnlyAppliesToExactSuccessfulPlacement() {
        val launch = DuelHandLaunch(15, row = 1, column = 6, offsetX = 50f, offsetY = 400f)
        assertTrue(launch.matches(15, 1, 6))
        assertFalse(launch.matches(14, 1, 6))
        assertFalse(launch.matches(15, 0, 6))
        assertFalse(launch.matches(15, 1, 5))
    }
}
