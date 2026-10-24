package com.tplatt.shadowshaper

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

/**
 * Presentation-only match result. All scores and result criteria come
 * directly from the engine snapshot, never from these animations.
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
        "VICTORY" -> DuelPalette.gold
        "DRAW" -> DuelPalette.blue
        else -> DuelPalette.red
    }
    var shown by remember(result, playerPower, cpuPower) { mutableStateOf(false) }
    LaunchedEffect(result, playerPower, cpuPower) {
        delay(100)
        shown = true
    }
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        AnimatedVisibility(
            visible = shown,
            enter = fadeIn(tween(320)) + scaleIn(
                initialScale = 0.92f, animationSpec = tween(340)
            )
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(0.90f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DuelPalette.tableLow),
                border = BorderStroke(2.dp, accent)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("SHADOW SHAPER DUELS", fontSize = 11.sp, letterSpacing = 2.sp,
                        color = DuelPalette.dullGold)
                    Text(result, fontSize = 29.sp, letterSpacing = 3.sp,
                        fontWeight = FontWeight.Black, color = accent)
                    Spacer(Modifier.height(8.dp))
                    Text("FINAL POWER", fontSize = 11.sp,
                        color = DuelPalette.dullGold)
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(playerPower.toString(), fontSize = 30.sp,
                                fontWeight = FontWeight.Black, color = DuelPalette.cream)
                            Text("YOU", color = DuelPalette.blue, fontSize = 11.sp)
                        }
                        Text("—", color = accent, fontSize = 27.sp)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(cpuPower.toString(), fontSize = 30.sp,
                                fontWeight = FontWeight.Black, color = DuelPalette.cream)
                            Text("OPPONENT", color = DuelPalette.red, fontSize = 11.sp)
                        }
                    }
                    if (unresolvedEffects > 0) {
                        Text(
                            "$unresolvedEffects card effects are still unverified or incomplete. " +
                                "This simulator result may differ from the game client.",
                            color = DuelPalette.dullGold, fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = onInspectBoard) {
                            Text("VIEW BOARD", fontSize = 11.sp)
                        }
                        OutlinedButton(onClick = onReviewLog) {
                            Text("BATTLE LOG", fontSize = 11.sp)
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(onClick = onRematch) {
                            Text("BATTLE AGAIN", fontSize = 11.sp)
                        }
                        OutlinedButton(onClick = onSelectDecks) {
                            Text("SELECT DECKS", fontSize = 11.sp)
                        }
                    }
                    OutlinedButton(onClick = onHome) {
                        Text("HOME", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
