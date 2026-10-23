package com.tplatt.shadowshaper

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Weathered engraving and low-contrast age marks on the arena floor.
 * These details echo the faint masonry/stenciled ruins in the source
 * Destiny: Rising screenshot without implying fake card positions.
 *
 * Rendered beneath real scenery, row guides, cards, and all game controls.
 */
@Composable
internal fun DuelArenaPatina(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val thin = .75.dp.toPx()
        val grain = Random(702146)
        val softGray = Color(0x4A3F4445)
        val ash = Color(0x39595C57)

        // Centuries-worn circular inscriptions, only partially visible.
        // The original playfield's center has similar rubbed-off markings.
        for ((x, y, radius) in listOf(
            Triple(w * .27f, h * .16f, w * .095f),
            Triple(w * .76f, h * .36f, w * .075f),
            Triple(w * .49f, h * .86f, w * .11f)
        )) {
            val perimeter = Path().apply {
                val start = (-135.0 * PI / 180.0)
                val end = (35.0 * PI / 180.0)
                for (i in 0..21) {
                    val angle = start + (end - start) * i / 21.0
                    val xx = x + cos(angle).toFloat() * radius
                    val yy = y + sin(angle).toFloat() * radius * .57f
                    if (i == 0) moveTo(xx, yy) else lineTo(xx, yy)
                }
            }
            drawPath(
                perimeter,
                Color(0x22535D58), style = Stroke(width = thin)
            )
        }

        // Fine chipped seams and broken cracks at a few stone joints.
        for (i in 0 until 20) {
            val u = grain.nextFloat()
            val edgeBias = if (i % 3 == 0) .10f else .90f
            val x = w * (if (i % 2 == 0) edgeBias * u
                else 1f - edgeBias * u)
            val y = h * (.07f + grain.nextFloat() * .86f)
            val scale = (4f + grain.nextFloat() * 12f).dp.toPx()
            val crack = Path().apply {
                moveTo(x, y)
                lineTo(x + scale * .28f, y + scale * .34f)
                lineTo(x - scale * .16f, y + scale * .67f)
                lineTo(x + scale * .15f, y + scale)
            }
            drawPath(
                crack, softGray.copy(alpha = .17f),
                style = Stroke(width = thin)
            )
        }

        // Scuffs and ground dirt are most noticeable around the border.
        repeat(55) {
            val t = grain.nextFloat()
            val edgeX = if (it % 2 == 0) w * (.025f + t * .15f)
                else w * (.975f - t * .15f)
            val y = h * grain.nextFloat()
            drawLine(
                ash.copy(alpha = .09f + grain.nextFloat() * .13f),
                Offset(edgeX, y),
                Offset(edgeX + w * (.006f + grain.nextFloat() * .028f),
                    y - h * .003f),
                thin * (.7f + grain.nextFloat())
            )
        }

        // Small aged diamond stamps carved into two opposing margins.
        // Their faint opacity prevents confusion with Power-score diamonds.
        for (y in listOf(h * .19f, h * .81f)) {
            for (x in listOf(w * .12f, w * .88f)) {
                val r = 4.dp.toPx()
                val stamp = Path().apply {
                    moveTo(x, y - r)
                    lineTo(x + r, y)
                    lineTo(x, y + r)
                    lineTo(x - r, y)
                    close()
                }
                drawPath(
                    stamp,
                    Color(0x24585C5A),
                    style = Stroke(width = .65.dp.toPx())
                )
            }
        }
    }
}
