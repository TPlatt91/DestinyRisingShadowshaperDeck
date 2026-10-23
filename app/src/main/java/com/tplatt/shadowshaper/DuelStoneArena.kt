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
            // Hairline stone courses where the actual battle rows meet.
            for (row in 1..3) {
                val y = h * row / 4f
                drawLine(
                    Color(0x7CC7C6BF),
                    Offset(w * 0.055f, y),
                    Offset(w * 0.945f, y),
                    hairline
                )
                drawLine(
                    Color(0x55404040),
                    Offset(w * 0.055f, y + hairline * 2),
                    Offset(w * 0.945f, y + hairline * 2),
                    hairline
                )
            }
            // Column registration marks, deliberately much fainter than a
            // box around every empty slot.
            for (column in 1 until ShadowRules.COLUMNS) {
                val x = w * (column + 0.2f) / (ShadowRules.COLUMNS + 0.4f)
                drawLine(
                    Color(0x2CCBCBC6),
                    Offset(x, h * 0.23f),
                    Offset(x, h * 0.77f),
                    hairline
                )
            }
            drawRect(
                color = Color(0xAA929492),
                style = Stroke(width = 1.2.dp.toPx())
            )
        }
    }
}
