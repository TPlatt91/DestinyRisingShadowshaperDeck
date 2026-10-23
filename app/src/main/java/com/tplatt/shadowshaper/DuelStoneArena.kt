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
            // Real rock faces, fern fronds and loose rubble now come from
            // DuelArenaEnvironment: a cached, detailed decorative layer
            // composited immediately over this underlying stone surface.
            // Keeping this Canvas clear avoids the old mirrored black seams.
            drawRect(
                color = Color(0x999B9E9B),
                style = Stroke(width = 1.2.dp.toPx())
            )
        }
        // All environmental scenery remains behind the legal lane overlay.
        // Image has no pointer handler or semantic role.
        DuelArenaEnvironment(Modifier.fillMaxSize())
        // Bracket/rivet inlays are strictly decorative and at the perimeter.
        DuelArenaTrim(Modifier.fillMaxSize())
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
        val stroke = .85.dp.toPx()
        val inset = 2.dp.toPx()

        // A shallow inset slate panel under the four actual rows, in the
        // original game's lightly chiseled stone style. It's deliberately
        // translucent so the masonry grain survives the overlay.
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(
                    Color(0x245E6261),
                    Color(0x129FA19A),
                    Color(0x20D5D4CA),
                    Color(0x28505352)
                )
            ),
            topLeft = Offset(inset, inset),
            size = androidx.compose.ui.geometry.Size(
                (w - inset * 2f).coerceAtLeast(0f),
                (h - inset * 2f).coerceAtLeast(0f)
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
        )
        // Top and bottom slab courses provide definition without making
        // permanent outlined empty card rectangles.
        for (y in listOf(inset, h - inset)) {
            drawLine(
                Color(0x88D7D6D0),
                Offset(w * .018f, y),
                Offset(w * .982f, y), stroke
            )
        }
        for (row in 1..3) {
            val y = h * row / 4f
            val centerSeam = row == 2
            drawLine(
                if (centerSeam) Color(0xBBD9D2B9) else Color(0x8FD8D8D1),
                Offset(w * .018f, y),
                Offset(w * .982f, y),
                if (centerSeam) stroke * 1.7f else stroke
            )
            drawLine(
                Color(0x66424748),
                Offset(w * .018f, y + stroke * 1.8f),
                Offset(w * .982f, y + stroke * 1.8f), stroke
            )
        }

        // Light vertical joinery and tiny registration nicks between
        // positions, not full-size empty-card placeholders.
        val columns = ShadowRules.COLUMNS
        for (col in 1 until columns) {
            val x = w * col / columns
            for (row in 0 until 4) {
                val y0 = h * row / 4f
                val y1 = h * (row + 1) / 4f
                drawLine(
                    Color(0x36DCDDD3),
                    Offset(x, y0 + h * .012f),
                    Offset(x, y1 - h * .012f),
                    stroke * .60f
                )
                drawLine(
                    Color(0x76E0DED5),
                    Offset(x, y0 + h * .007f),
                    Offset(x, y0 + h * .027f),
                    stroke * .90f
                )
            }
        }
        // Recessed stone side seams: thin, faint and separate from touch.
        for (x in listOf(inset, w - inset)) {
            drawLine(
                Color(0x6A666A69),
                Offset(x, inset),
                Offset(x, h - inset), stroke
            )
        }
    }
}
