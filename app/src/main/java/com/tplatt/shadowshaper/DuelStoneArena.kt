package com.tplatt.shadowshaper

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Slate arena built for the portrait viewport from the user's Destiny: Rising
 * reference. Generates a reusable pixel-grain bitmap once, not on every frame.
 *
 * This is a textured board *surface*, never a screenshot pasted behind cards:
 * it does not embed fake units, scores, or controls. All real zones, players
 * and hit boxes are still drawn on top by ShadowBattleActivity.
 */
private fun generateSlateTexture(): Bitmap {
    val width = 384
    val height = 512
    val pixels = IntArray(width * height)
    val grain = Random(204023)
    for (y in 0 until height) for (x in 0 until width) {
        val px = x.toDouble() / width
        val py = y.toDouble() / height
        val edge = kotlin.math.abs(px - 0.5) * 2.0
        // Broad eroded sediment bands under irregular, fine pixel-grain.
        val strata = (
            8.0 * sin(px * 10.3 + sin(py * 7.4) * 0.9) +
            5.5 * sin(py * 19.0 + px * 4.7) +
            4.5 * sin((px * 13.0 - py * 21.0)) +
            2.5 * sin((px + py) * 64.0)
        )
        val weathering = grain.nextInt(-13, 14)
        val centerLight = 9.0 * (1.0 - edge * edge)
        val edgeShade = -34.0 * edge * edge * edge
        val base = (150.0 + strata + weathering + centerLight + edgeShade)
            .toInt().coerceIn(64, 201)
        pixels[y * width + x] = AndroidColor.rgb(
            (base + 2).coerceAtMost(255),
            (base + 2).coerceAtMost(255),
            (base - 2).coerceAtLeast(0)
        )
    }
    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, width, 0, 0, width, height)
    }
}

@Composable
internal fun DuelStoneArena(modifier: Modifier = Modifier) {
    val stone = remember { generateSlateTexture().asImageBitmap() }
    Box(modifier.background(Color(0xFF8F908D), RoundedCornerShape(5.dp))) {
        Image(
            bitmap = stone,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val hairline = 0.9.dp.toPx()
            // Original-like atmospheric edge shading over the stone surface.
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0x99272A2C),
                        Color(0x202A2B2C),
                        Color(0x00292C2D),
                        Color(0x30292A2A),
                        Color(0xA727292B)
                    )
                )
            )
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0x5027292A), Color.Transparent,
                        Color.Transparent, Color(0x7927292A)
                    )
                )
            )
            // Cracked stone along the edges echoes the original game's
            // rough slate surround, kept outside the seven card columns.
            val leftRock = Path().apply {
                moveTo(0f, 0f)
                lineTo(w * 0.035f, 0f)
                lineTo(w * 0.016f, h * 0.12f)
                lineTo(w * 0.05f, h * 0.24f)
                lineTo(w * 0.020f, h * 0.37f)
                lineTo(w * 0.045f, h * 0.53f)
                lineTo(w * 0.015f, h * 0.69f)
                lineTo(w * 0.038f, h * 0.84f)
                lineTo(w * 0.02f, h)
                lineTo(0f, h)
                close()
            }
            val rightRock = Path().apply {
                moveTo(w, 0f)
                lineTo(w * 0.963f, 0f)
                lineTo(w * 0.980f, h * 0.13f)
                lineTo(w * 0.950f, h * 0.25f)
                lineTo(w * 0.982f, h * 0.40f)
                lineTo(w * 0.955f, h * 0.59f)
                lineTo(w * 0.976f, h * 0.74f)
                lineTo(w * 0.944f, h * 0.88f)
                lineTo(w * 0.971f, h)
                lineTo(w, h)
                close()
            }
            drawPath(leftRock, Color(0xC02A2E31))
            drawPath(rightRock, Color(0xB8292D30))
            // A few irregular fissures, not uniform graphics-card outlines.
            for (n in 0..5) {
                val x = w * (0.10f + n * 0.155f)
                val y = h * (0.10f + (n % 3) * 0.28f)
                val fissure = Path().apply {
                    moveTo(x, y)
                    lineTo(x + w * 0.014f, y + h * 0.018f)
                    lineTo(x + w * 0.008f, y + h * 0.034f)
                    lineTo(x + w * 0.028f, y + h * 0.048f)
                }
                drawPath(
                    fissure,
                    Color(0x693D4140),
                    style = Stroke(width = hairline)
                )
            }
            drawRect(
                color = Color(0x999B9E9B),
                style = Stroke(width = 1.2.dp.toPx())
            )
        }
    }
}

/**
 * Lines live in the card-lane coordinate system, not the larger stone floor,
 * so the courses match the four actual Engine row hit rectangles.
 */
@Composable
internal fun DuelArenaGrid(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = 0.8.dp.toPx()
        for (row in 1..3) {
            val y = h * row / 4f
            drawLine(
                Color(0x96E1DED5),
                Offset(w * 0.015f, y),
                Offset(w * 0.985f, y),
                stroke
            )
            drawLine(
                Color(0x5D343635),
                Offset(w * 0.015f, y + stroke * 1.8f),
                Offset(w * 0.985f, y + stroke * 1.8f),
                stroke
            )
        }
        for (col in 1 until ShadowRules.COLUMNS) {
            val x = w * col / ShadowRules.COLUMNS
            drawLine(
                Color(0x2FCDCDC5),
                Offset(x, h * 0.25f),
                Offset(x, h * 0.75f),
                stroke
            )
        }
    }
}
