package com.tplatt.shadowshaper

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Original Destiny-inspired language: HUD-white typography, neutral black
// glass, a restrained warm focus color, and functional faction/phase accents.
private val hudWhite = Color(0xFFE9EDEB)
private val hudDim = Color(0xFFA5AFB4)
private val hudFocus = Color(0xFFD5BD87)
private val hudBlue = Color(0xFF9ACDDC)
private val hudEnemy = Color(0xFFDAB2A5)
private val hudLine = Color(0xFF7E898B)

private fun phaseColor(status: String): Color = when (status) {
    "YOUR TURN" -> hudFocus
    "CPU TURN" -> hudEnemy
    "TARGET", "MOVE UNIT" -> hudBlue
    "DUEL COMPLETE" -> hudWhite
    else -> hudDim
}

/**
 * Minimal, information-first battle header in a Destiny-inspired idiom.
 *
 * The menu/Results action remains the ONLY tappable area; title and round
 * counter are HUD readouts, not additional gesture targets.
 *
 * No official Bungie icon or asset is copied. The reticle/diamond and light
 * rules below are original vector ornamentation, rendered offline.
 */
@Composable
internal fun DuelBattleTopBar(
    status: String,
    round: Int,
    reviewingBoard: Boolean,
    yourTurn: Boolean,
    onNavigation: () -> Unit
) {
    val accent = phaseColor(status)
    val active = status == "YOUR TURN" && yourTurn
    val ambience = rememberInfiniteTransition(label = "quiet objective HUD")
    val breathing by ambience.animateFloat(
        initialValue = .48f,
        targetValue = .91f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "turn indicator breathing"
    )

    Box(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xF31C252B),
                        Color(0xF5121B21),
                        Color(0xFC0A1117)
                    )
                )
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val top = .9.dp.toPx()
            val bottom = height - 1.4.dp.toPx()
            val leftDivider = 82.dp.toPx()
            val rightDivider = width - 104.dp.toPx()

            // Dim top and side framing, deliberately less ornate than a
            // metallic RPG toolbar. Destiny-style negative space dominates.
            drawLine(
                hudWhite.copy(alpha = .17f),
                Offset(0f, top),
                Offset(width, top),
                .65.dp.toPx()
            )
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(
                        hudFocus.copy(alpha = .82f),
                        hudLine.copy(alpha = .55f),
                        hudLine.copy(alpha = .39f),
                        Color.Transparent
                    )
                ),
                start = Offset(0f, bottom),
                end = Offset(width, bottom),
                strokeWidth = 1.1.dp.toPx()
            )

            for (x in listOf(leftDivider, rightDivider)) {
                drawLine(
                    hudLine.copy(alpha = .31f),
                    Offset(x, 12.dp.toPx()),
                    Offset(x, height - 11.dp.toPx()),
                    .75.dp.toPx()
                )
            }

            // Precision ticks instead of large beveled borders.
            val tickY = height * .50f
            for (x in listOf(4.dp.toPx(), width - 4.dp.toPx())) {
                drawLine(
                    hudFocus.copy(alpha = .68f),
                    Offset(x, tickY - 5.dp.toPx()),
                    Offset(x, tickY + 5.dp.toPx()),
                    1.1.dp.toPx()
                )
            }

            // Slim center objective marker/diamond under the app title.
            val mid = width * .5f
            val marker = Path().apply {
                moveTo(mid, bottom - 3.dp.toPx())
                lineTo(mid + 3.dp.toPx(), bottom - .7.dp.toPx())
                lineTo(mid, bottom + 1.5.dp.toPx())
                lineTo(mid - 3.dp.toPx(), bottom - .7.dp.toPx())
                close()
            }
            drawPath(marker, hudFocus.copy(alpha = .90f))
        }

        Row(
            Modifier.fillMaxSize().padding(horizontal = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // At least 44dp high; not a tiny text-only hit target.
            Row(
                Modifier
                    .width(77.dp)
                    .fillMaxHeight()
                    .clickable(
                        role = Role.Button,
                        onClick = onNavigation
                    )
                    .semantics {
                        contentDescription = if (reviewingBoard)
                            "Return to match results"
                        else "Open battle menu"
                    }
                    .padding(start = 9.dp, end = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Canvas(Modifier.size(17.dp)) {
                    val midY = size.height * .5f
                    if (reviewingBoard) {
                        drawLine(
                            hudWhite,
                            Offset(size.width * .22f, midY),
                            Offset(size.width * .85f, midY),
                            1.6.dp.toPx()
                        )
                        drawLine(
                            hudWhite,
                            Offset(size.width * .23f, midY),
                            Offset(size.width * .48f, midY - size.height * .22f),
                            1.6.dp.toPx()
                        )
                        drawLine(
                            hudWhite,
                            Offset(size.width * .23f, midY),
                            Offset(size.width * .48f, midY + size.height * .22f),
                            1.6.dp.toPx()
                        )
                    } else {
                        for (index in 0..2) {
                            val y = size.height * (.29f + index * .21f)
                            drawLine(
                                hudWhite,
                                Offset(size.width * .20f, y),
                                Offset(size.width * (if (index == 1) .67f else .81f), y),
                                1.45.dp.toPx()
                            )
                        }
                    }
                }
                Text(
                    if (reviewingBoard) "RESULTS" else "MENU",
                    fontSize = 9.sp,
                    letterSpacing = .6.sp,
                    fontWeight = FontWeight.Bold,
                    color = hudWhite,
                    maxLines = 1
                )
            }

            // One clear name, one subordinate mode name; no stretched-out
            // separate letters that collapse into unreadable tiny type.
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "SHADOWSHAPER",
                    color = hudWhite,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    letterSpacing = .38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Clip
                )
                Text(
                    "DUELS  /  SKIRMISH",
                    color = hudDim,
                    fontSize = 8.sp,
                    lineHeight = 10.sp,
                    letterSpacing = .45.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Clip
                )
            }

            // This is the only animated marker. Its color follows the engine
            // snapshot, not an invented countdown or locally managed turn.
            Column(
                Modifier.width(99.dp).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Canvas(Modifier.size(8.dp)) {
                        val c = Offset(size.width * .5f, size.height * .5f)
                        val diamond = Path().apply {
                            moveTo(c.x, size.height * .09f)
                            lineTo(size.width * .92f, c.y)
                            lineTo(c.x, size.height * .91f)
                            lineTo(size.width * .08f, c.y)
                            close()
                        }
                        drawPath(
                            diamond,
                            color = accent.copy(alpha = if (active) breathing else .90f)
                        )
                        if (active) {
                            drawPath(
                                diamond,
                                color = accent.copy(alpha = .31f * breathing),
                                style = Stroke(.75.dp.toPx())
                            )
                        }
                    }
                    Text(
                        status,
                        color = if (status == "YOUR TURN") hudWhite else accent,
                        fontSize = if (status.length > 11) 7.sp else 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = .33.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
                Text(
                    "ROUND  ${round.coerceIn(1, ShadowRules.ROUNDS)} / ${ShadowRules.ROUNDS}",
                    color = hudDim,
                    fontSize = 8.sp,
                    lineHeight = 12.sp,
                    letterSpacing = .35.sp,
                    maxLines = 1
                )
            }
        }
    }
}
