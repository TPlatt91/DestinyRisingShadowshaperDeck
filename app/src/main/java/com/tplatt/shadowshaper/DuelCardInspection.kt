package com.tplatt.shadowshaper

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * In-match enlarged card inspection. This window uses the ACTUAL board unit
 * for current Power and status, but does not mutate any engine state.
 * It can inspect either side's units or a card held in the player's hand.
 */
@Composable
internal fun DuelCardInspection(
    card: ShadowData.Card,
    unit: ShadowEngine.Unit?,
    power: Int,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val keywords = remember(card.name) {
        ShadowKeywordGlossary.onCard(card, ShadowKeywordGlossary.all(context))
    }
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.92f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DuelPalette.tableLow),
            border = BorderStroke(2.dp, DuelPalette.rim)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                DuelCardFrame(
                    card = card,
                    power = power,
                    modifier = Modifier.fillMaxHeight().width(210.dp),
                    inspired = unit?.inspired == true,
                    suppressed = unit?.suppressed == true,
                    scorched = (unit?.scorchTurns ?: 0) > 0,
                    compact = false
                )
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                card.name, color = DuelPalette.cream,
                                fontWeight = FontWeight.Black, fontSize = 20.sp,
                                maxLines = 2, overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                card.rarity + "  •  " + card.faction,
                                color = duelRarityColor(card.rarity),
                                fontSize = 12.sp, fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(onClick = onClose) {
                            Text("CLOSE ×", color = DuelPalette.gold)
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("POWER  " + power, color = DuelPalette.cream,
                            fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("LIGHT  " + card.cost.ifBlank { "?" }, color = DuelPalette.blue,
                            fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    if (unit != null) {
                        val statuses = listOfNotNull(
                            if (unit.inspired && !unit.suppressed) "INSPIRED" else null,
                            if (unit.suppressed) "SUPPRESSED" else null,
                            if (unit.scorchTurns > 0) "SCORCHED (" + unit.scorchTurns + ")" else null
                        )
                        Text(
                            "FIELD: " + (if (unit.row == ShadowRules.FRONT_ROW) "FRONT" else "BACK") +
                                " / SLOT " + (unit.column + 1) +
                                (if (statuses.isNotEmpty()) "  •  " + statuses.joinToString(" / ") else ""),
                            color = DuelPalette.dullGold, fontSize = 11.sp
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        Text("CARD EFFECT", color = DuelPalette.gold,
                            fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
                        Spacer(Modifier.height(3.dp))
                        Text(
                            card.effect.ifBlank { "No special effect." },
                            color = DuelPalette.cream, fontSize = 14.sp, lineHeight = 19.sp
                        )
                        if (card.trigger.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text("ACTIVATES: " + card.trigger,
                                color = DuelPalette.dullGold, fontSize = 11.sp)
                        }
                        if (card.cardNumber.isNotBlank()) {
                            Text("CARD ID: " + card.cardNumber,
                                color = DuelPalette.dullGold, fontSize = 11.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("CARD KEYWORDS", color = DuelPalette.gold,
                            fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        if (keywords.isEmpty()) {
                            Text("No defined keywords in this printed ability.",
                                color = DuelPalette.dullGold, fontSize = 11.sp)
                        } else {
                            ShadowKeywordDefinitions(
                                keywords,
                                accent = DuelPalette.gold,
                                foreground = DuelPalette.cream,
                                secondary = DuelPalette.dullGold
                            )
                        }
                    }
                }
            }
        }
    }
}
