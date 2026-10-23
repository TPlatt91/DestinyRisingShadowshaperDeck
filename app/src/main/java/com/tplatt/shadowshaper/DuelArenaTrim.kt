package com.tplatt.shadowshaper

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Decorative steel/stone rim around the arena. Subtle blue-gray metal corner
 * brackets and tiny worn gold inlays echo the original Destiny: Rising HUD.
 * Does not cover legal positions, receive touch input or draw mock card zones.
 */
@Composable
internal fun DuelArenaTrim(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val steel = Color(0xFF4A5663)
        val edge = Color(0xFF838C8D)
        val highlight = Color(0xFFBDC0B7)
        val brass = Color(0xFFB7A079)

        // Recessed outer seam, narrow enough to stay off card thumbnails.
        drawRect(
            color = Color(0xFF21292D),
            topLeft = Offset(0.7.dp.toPx(), 0.7.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(
                (w - 1.4.dp.toPx()).coerceAtLeast(0f),
                (h - 1.4.dp.toPx()).coerceAtLeast(0f)
            ),
            style = Stroke(width = 2.2.dp.toPx())
        )
        drawRect(
            color = edge.copy(alpha = .78f),
            topLeft = Offset(3.dp.toPx(), 3.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(
                (w - 6.dp.toPx()).coerceAtLeast(0f),
                (h - 6.dp.toPx()).coerceAtLeast(0f)
            ),
            style = Stroke(width = .9.dp.toPx())
        )

        // Broken metallic corner plates are small enough to leave the
        // seven-column battle area unobstructed.
        val dx = 12.dp.toPx()
        val dy = 18.dp.toPx()
        val cornerPaths = listOf(
            Path().apply {
                moveTo(0f, 0f); lineTo(dx, 0f)
                lineTo(dx * .67f, dy * .22f)
                lineTo(dx * .35f, dy); lineTo(0f, dy * 1.15f); close()
            },
            Path().apply {
                moveTo(w, 0f); lineTo(w - dx, 0f)
                lineTo(w - dx * .67f, dy * .22f)
                lineTo(w - dx * .35f, dy); lineTo(w, dy * 1.15f); close()
            },
            Path().apply {
                moveTo(0f, h); lineTo(dx, h)
                lineTo(dx * .67f, h - dy * .22f)
                lineTo(dx * .35f, h - dy); lineTo(0f, h - dy * 1.15f); close()
            },
            Path().apply {
                moveTo(w, h); lineTo(w - dx, h)
                lineTo(w - dx * .67f, h - dy * .22f)
                lineTo(w - dx * .35f, h - dy); lineTo(w, h - dy * 1.15f); close()
            }
        )
        cornerPaths.forEach {
            drawPath(it, steel.copy(alpha = .89f))
            drawPath(
                it, highlight.copy(alpha = .47f),
                style = Stroke(width = .65.dp.toPx())
            )
        }

        for (x in listOf(7f, w - 7.dp.toPx())) {
            for (y in listOf(8f, h - 8.dp.toPx())) {
                drawCircle(Color(0xD4161A1D), 2.5.dp.toPx(), Offset(x, y))
                drawCircle(
                    Color(0xFFC5BBA5), .9.dp.toPx(),
                    Offset(x - .25.dp.toPx(), y - .35.dp.toPx())
                )
            }
        }

        // Thin worn brass bottom rail; the reference has a similar gold
        // separator between the arena and its hand/control area.
        drawLine(
            brass.copy(alpha = .68f),
            Offset(w * .025f, h - 3.3.dp.toPx()),
            Offset(w * .975f, h - 3.3.dp.toPx()),
            1.2.dp.toPx()
        )
        drawLine(
            highlight.copy(alpha = .40f),
            Offset(w * .028f, 3.5.dp.toPx()),
            Offset(w * .972f, 3.5.dp.toPx()),
            .8.dp.toPx()
        )
    }
}
