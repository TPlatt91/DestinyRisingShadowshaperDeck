package com.tplatt.shadowshaper

import kotlin.math.PI
import kotlin.math.sin

/**
 * Pure, deterministic 0..1 timeline for a card travelling to a real board slot.
 * The game engine places the card immediately; only the pixels move.
 *
 * Phase 1: travel from hand (or above the CPU field) with a short arc.
 * Phase 2: hover for a fraction of a second.
 * Phase 3: slam onto stone and squash on impact.
 * Phase 4: recoil and settle at the exact slot coordinates.
 */
internal data class DuelLandingFrame(
    val offsetX: Float,
    val offsetY: Float,
    val scale: Float,
    val tiltDegrees: Float,
    val opacity: Float,
    val impact: Float
)

internal object DuelLandingMotion {
    const val DURATION_MS = 760
    private fun unit(value: Float): Float = value.coerceIn(0f, 1f)
    private fun easeOut(value: Float): Float {
        val x = unit(value)
        return 1f - (1f - x) * (1f - x) * (1f - x)
    }

    fun at(
        progress: Float,
        originX: Float,
        originY: Float,
        cardHeightPx: Float,
        clockwise: Boolean
    ): DuelLandingFrame {
        val t = unit(progress)
        val h = cardHeightPx.coerceAtLeast(0f)
        val tilt = if (clockwise) 1f else -1f

        if (t < .46f) {
            val move = easeOut(t / .46f)
            val arc = sin(PI * move.toDouble()).toFloat() * h * .32f
            return DuelLandingFrame(
                offsetX = originX * (1f - move),
                offsetY = originY * (1f - move) - h * .31f * move - arc,
                scale = .83f + .30f * move,
                tiltDegrees = tilt * (9f * (1f - move) + 2.4f * move),
                opacity = .78f + .22f * move,
                impact = 0f
            )
        }
        if (t < .55f) {
            val hold = (t - .46f) / .09f
            return DuelLandingFrame(
                offsetX = 0f,
                offsetY = -h * (.31f + .02f * hold),
                scale = 1.13f + .035f * hold,
                tiltDegrees = tilt * (2.4f * (1f - hold)),
                opacity = 1f,
                impact = 0f
            )
        }
        if (t < .72f) {
            val fall = (t - .55f) / .17f
            val accelerated = fall * fall
            return DuelLandingFrame(
                offsetX = 0f,
                offsetY = -h * .33f + h * .41f * accelerated,
                scale = 1.165f - .235f * accelerated,
                tiltDegrees = 0f,
                opacity = 1f,
                impact = if (fall > .72f) ((fall - .72f) / .28f) else 0f
            )
        }
        if (t < .84f) {
            val bounce = easeOut((t - .72f) / .12f)
            return DuelLandingFrame(
                offsetX = 0f,
                offsetY = h * (.08f - .13f * bounce),
                scale = .93f + .105f * bounce,
                tiltDegrees = 0f,
                opacity = 1f,
                impact = 1f - .35f * bounce
            )
        }
        val settle = easeOut((t - .84f) / .16f)
        return DuelLandingFrame(
            offsetX = 0f,
            offsetY = -h * .05f * (1f - settle),
            scale = 1.035f - .035f * settle,
            tiltDegrees = 0f,
            opacity = 1f,
            impact = .65f * (1f - settle)
        )
    }
}
