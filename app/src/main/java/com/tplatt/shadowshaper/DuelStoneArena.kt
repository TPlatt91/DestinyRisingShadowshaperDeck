package com.tplatt.shadowshaper

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/**
 * Uses the actual cracked-stone and foliage artwork supplied by the user.
 *
 * The 720x1280 portrait derivative has approximately 5% narrow edge
 * borders, keeping natural cracks and nearly all width for the seven cards.
 * It is bundled offline at drawable-nodpi without density scaling. This is NOT the former generated masonry/noise
 * scene: it contains the user's real cracks, carved stones, moss and foliage.
 * No overlay in this composable intercepts any touch or drag event.
 */
@Composable
internal fun DuelStoneArena(modifier: Modifier = Modifier) {
    Box(modifier) {
        Image(
            painter = painterResource(R.drawable.shadow_arena_narrow_ruins_v2),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            // Crops a few top/bottom pixels rather than stretching the
            // stone, preserving cracks, rocks, and foliage proportions.
            contentScale = ContentScale.Crop
        )
    }
}

/**
 * Very light placement incisions behind real unit cards. Coordinates follow
 * the actual centered seven-column/four-row engine board, not the surrounding
 * stone-rubble image. No artificial empty-card rectangles are painted.
 */
@Composable
internal fun DuelArenaGrid(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = 0.7.dp.toPx()

        // Three finely etched row boundaries. Player/opponent mid-line is
        // a little brighter, but the cracked stone remains fully visible.
        for (row in 1..3) {
            val y = h * row / 4f
            drawLine(
                if (row == 2) Color(0xBCE0DDD1) else Color(0x7ACBCAC5),
                Offset(0f, y),
                Offset(w, y),
                if (row == 2) stroke * 1.4f else stroke
            )
            drawLine(
                Color(0x4F414543),
                Offset(0f, y + stroke * 1.7f),
                Offset(w, y + stroke * 1.7f),
                stroke
            )
        }

        // The grid is intentionally implied rather than boxed. All seven
        // target areas are still present in Compose for placement and drag.
        for (column in 1 until ShadowRules.COLUMNS) {
            val x = w * column / ShadowRules.COLUMNS
            drawLine(
                Color(0x2AB6B7B3),
                Offset(x, 0f), Offset(x, h),
                stroke * 0.6f
            )
        }
    }
}
