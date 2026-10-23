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
import kotlin.math.abs
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
    val height = 384
    val pixels = IntArray(width * height)
    val noise = Random(204023)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val grain = noise.nextInt(-14, 15)
            val veins = (
                10.0 * sin(x * 0.023 + sin(y * 0.027) * 2.1) +
                    7.0 * sin(y * 0.055 + x * 0.008) +
                    4.0 * sin((x + y) * 0.083)
                ).toInt()
            val crack = if (
                abs(y - (81 + 10 * sin(x * 0.025)).toInt()) < 2 ||
                abs(y - (274 + 12 * sin(x * 0.018)).toInt()) < 2
            ) -17 else 0
            val base = (100 + grain + veins + crack).coerceIn(48, 138)
            pixels[y * width + x] = AndroidColor.rgb(
                (base + 2).coerceAtMost(255),
                (base + 2).coerceAtMost(255),
                (base - 2).coerceAtLeast(0)
            )
        }
    }
    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, width, 0, 0, width, height)
    }
}

@Composable
internal fun DuelStoneArena(modifier: Modifier = Modifier) {
    val stone = remember { generateSlateTexture().asImageBitmap() }
    Box(modifier.background(Color(0xFF464747), RoundedCornerShape(5.dp))) {
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
                        Color(0xD9141718),
                        Color(0x22191B1B),
                        Color(0x00292C2D),
                        Color(0x3317191B),
                        Color(0xD3161819)
                    )
                )
            )
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0x88141617), Color.Transparent,
                        Color.Transparent, Color(0x9A151617)
                    )
                )
            )
            // Cracked stone along the edges echoes the original game's
            // rough slate surround, kept outside the six card columns.
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
            drawPath(leftRock, Color(0xA7191C1D))
            drawPath(rightRock, Color(0xC0181B1D))
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
                    Color(0x4B252828),
                    style = Stroke(width = hairline)
                )
            }
            drawRect(
                color = Color(0xAA929492),
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
