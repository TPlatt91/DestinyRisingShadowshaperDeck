package com.tplatt.shadowshaper

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CutCornerShape
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
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val chromeGold = Color(0xFFE8C88D)
private val chromePale = Color(0xFFF4ECE0)
private val chromeBlue = Color(0xFF92CDE9)
private val chromeRed = Color(0xFFEAB29F)
private val chromeDull = Color(0xFF8C9CA9)
private val chromeRim = Color(0xFF677078)

/**
 * The battle HUD's primary navigation/status chrome.
 *
 * Only the left control is clickable. The center title and right turn
 * indicator are decorative/read-only: no new input competes with card
 * dragging or with the real ShadowEngine turn decision.
 */
@Composable
internal fun DuelBattleTopBar(
    status: String,
    reviewingBoard: Boolean,
    yourTurn: Boolean,
    onNavigation: () -> Unit
) {
    val accent = when {
        status == "TARGET" || status == "MOVE UNIT" -> chromeBlue
        status == "CPU TURN" -> chromeRed
        status == "YOUR TURN" -> chromeGold
        else -> chromeDull
    }
    val glow = rememberInfiniteTransition(label = "HUD state beacon")
    val pulse by glow.animateFloat(
        initialValue = .47f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1700), repeatMode = RepeatMode.Reverse
        ),
        label = "turn indicator breathing"
    )
    val dotAlpha = if (yourTurn && status == "YOUR TURN") pulse else .80f

    Box(
        Modifier
            .fillMaxWidth()
            .height(49.dp)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF273643), Color(0xFF16232F), Color(0xFF101923))
                )
            )
            .border(.8.dp, Color(0xFF616367))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val gold = chromeGold
            val bottomY = size.height - 1.8.dp.toPx()
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, gold.copy(alpha = .84f),
                        gold.copy(alpha = .60f), Color.Transparent)
                ),
                start = Offset(3.dp.toPx(), bottomY),
                end = Offset(size.width - 3.dp.toPx(), bottomY),
                strokeWidth = 1.2.dp.toPx()
            )
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, chromeBlue.copy(alpha = .37f),
                        Color.Transparent)
                ),
                start = Offset(12.dp.toPx(), 2.dp.toPx()),
                end = Offset(size.width - 12.dp.toPx(), 2.dp.toPx()),
                strokeWidth = .65.dp.toPx()
            )
            val centerX = size.width * .50f
            val y = size.height - 2.dp.toPx()
            val badge = Path().apply {
                moveTo(centerX, y - 2.4.dp.toPx())
                lineTo(centerX + 3.dp.toPx(), y)
                lineTo(centerX, y + .4.dp.toPx())
                lineTo(centerX - 3.dp.toPx(), y)
                close()
            }
            drawPath(badge, gold.copy(alpha = .93f))
            // Thin panel dividers carve out two deliberate side stations.
            drawLine(
                chromeRim.copy(alpha = .49f),
                Offset(86.dp.toPx(), 10.dp.toPx()),
                Offset(86.dp.toPx(), size.height - 10.dp.toPx()),
                .6.dp.toPx()
            )
            drawLine(
                chromeRim.copy(alpha = .49f),
                Offset(size.width - 102.dp.toPx(), 10.dp.toPx()),
                Offset(size.width - 102.dp.toPx(), size.height - 10.dp.toPx()),
                .6.dp.toPx()
            )
            drawRect(
                color = gold.copy(alpha = .82f),
                topLeft = Offset(0f, 9.dp.toPx()),
                size = Size(2.dp.toPx(), 10.dp.toPx())
            )
            drawRect(
                color = gold.copy(alpha = .48f),
                topLeft = Offset(size.width - 2.dp.toPx(), 9.dp.toPx()),
                size = Size(2.dp.toPx(), 10.dp.toPx())
            )
        }

        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                Modifier
                    .width(81.dp)
                    .fillMaxHeight()
                    .clickable(onClick = onNavigation)
                    .semantics {
                        role = Role.Button
                        contentDescription =
                            if (reviewingBoard) "Return to match results"
                            else "Open battle menu"
                    }
                    .padding(start = 6.dp, end = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Canvas(Modifier.size(18.dp)) {
                    val midY = size.height * .5f
                    if (reviewingBoard) {
                        drawLine(
                            chromeGold,
                            Offset(size.width * .21f, midY),
                            Offset(size.width * .80f, midY),
                            1.7.dp.toPx()
                        )
                        drawLine(
                            chromeGold,
                            Offset(size.width * .40f, midY - size.height * .22f),
                            Offset(size.width * .20f, midY),
                            1.7.dp.toPx()
                        )
                        drawLine(
                            chromeGold,
                            Offset(size.width * .20f, midY),
                            Offset(size.width * .40f, midY + size.height * .22f),
                            1.7.dp.toPx()
                        )
                    } else {
                        for (j in 0..2) {
                            val yy = size.height * (.27f + j * .23f)
                            drawLine(
                                chromeGold,
                                Offset(size.width * .15f, yy),
                                Offset(size.width * (if (j == 1) .72f else .86f), yy),
                                1.6.dp.toPx()
                            )
                        }
                    }
                }
                Text(
                    if (reviewingBoard) "RESULTS" else "MENU",
                    color = chromeGold,
                    fontSize = 9.sp,
                    letterSpacing = .55.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "SHADOW SHAPER",
                    color = chromePale,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = .65.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
                Text(
                    "D U E L S",
                    color = chromeGold,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    maxLines = 1
                )
            }

            Row(
                Modifier.width(96.dp).fillMaxHeight(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            Modifier.size(5.dp)
                                .background(accent.copy(alpha = dotAlpha),
                                    CutCornerShape(1.dp))
                        )
                        Text(
                            status,
                            color = accent,
                            fontSize = if (status.length > 10) 7.sp else 8.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = .45.sp,
                            maxLines = 1
                        )
                    }
                    Text(
                        when (status) {
                            "YOUR TURN" -> "PLAYER PHASE"
                            "CPU TURN" -> "OPPONENT"
                            "TARGET" -> "SELECT UNIT"
                            "MOVE UNIT" -> "RELOCATE"
                            "DUEL COMPLETE" -> "FINAL BOARD"
                            else -> "BATTLE STATUS"
                        },
                        color = chromeDull,
                        fontSize = 7.sp,
                        letterSpacing = .35.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
