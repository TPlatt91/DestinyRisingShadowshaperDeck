package com.tplatt.shadowshaper

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Results are never independently computed by the UI. The original match
 * buttons and read-only board/log callbacks are retained in a popup with
 * the same cohesive Destiny-inspired presentation as the battle menus.
 */
@Composable
internal fun DuelMatchResult(
    playerPower: Int,
    cpuPower: Int,
    unresolvedEffects: Int,
    onRematch: () -> Unit,
    onSelectDecks: () -> Unit,
    onInspectBoard: () -> Unit,
    onReviewLog: () -> Unit,
    onHome: () -> Unit
) {
    val result = ShadowRules.result(playerPower, cpuPower)
    val accent = when (result) {
        "VICTORY" -> DuelPopupColors.gold
        "DRAW" -> DuelPopupColors.ice
        else -> Color(0xFFDFA69A)
    }
    var shown by remember(result, playerPower, cpuPower) { mutableStateOf(false) }
    LaunchedEffect(result, playerPower, cpuPower) {
        delay(105)
        shown = true
    }

    DuelPopupDialog(
        title = result,
        subtitle = "SKIRMISH COMPLETE  /  FINAL POWER",
        symbol = DuelPopupSymbol.ORBIT,
        onDismissRequest = {},
        allowDismiss = false
    ) {
        AnimatedVisibility(
            visible = shown,
            enter = fadeIn(tween(320)) + scaleIn(
                initialScale = .96f, animationSpec = tween(340)
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            playerPower.toString(),
                            fontSize = 31.sp,
                            color = DuelPopupColors.white,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            "YOU",
                            fontSize = 10.sp,
                            color = DuelPopupColors.ice,
                            letterSpacing = 1.1.sp
                        )
                    }
                    Text(
                        "   /   ",
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            cpuPower.toString(),
                            fontSize = 31.sp,
                            color = DuelPopupColors.white,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            "OPPONENT",
                            fontSize = 10.sp,
                            color = Color(0xFFE1B3A7),
                            letterSpacing = 1.1.sp
                        )
                    }
                }
                if (unresolvedEffects > 0) {
                    Text(
                        "$unresolvedEffects printed-effect clauses remain incomplete. " +
                            "The simulation may differ from the game client.",
                        color = DuelPopupColors.goldSoft,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp
                    )
                }
            }
        }
        DuelPopupRule()
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DuelPopupAction(
                label = "VIEW BOARD",
                modifier = Modifier.weight(1f),
                onClick = onInspectBoard
            )
            DuelPopupAction(
                label = "BATTLE LOG",
                modifier = Modifier.weight(1f),
                onClick = onReviewLog
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DuelPopupAction(
                label = "BATTLE AGAIN",
                style = DuelPopupActionStyle.PRIMARY,
                modifier = Modifier.weight(1f),
                onClick = onRematch
            )
            DuelPopupAction(
                label = "SELECT DECKS",
                modifier = Modifier.weight(1f),
                onClick = onSelectDecks
            )
        }
        DuelPopupAction(
            label = "HOME",
            modifier = Modifier.fillMaxWidth(),
            onClick = onHome
        )
    }
}
