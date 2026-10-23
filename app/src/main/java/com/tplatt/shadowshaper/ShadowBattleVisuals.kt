package com.tplatt.shadowshaper

import android.app.Activity
import android.widget.ImageView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Non-interactive tabletop ornamentation + reusable card faces.
 * Layout proportions are reference-inspired, not an assertion of exact pixel parity.
 * Slots remain attached to the actual ShadowEngine positions.
 */
internal object DuelPalette {
    val ink = Color(0xFF15100E)
    val table = Color(0xFF30231C)
    val tableLow = Color(0xFF1D1819)
    val rim = Color(0xFF7D583A)
    val gold = Color(0xFFEBC686)
    val dullGold = Color(0xFFB79B74)
    val cream = Color(0xFFF6EAD9)
    val blue = Color(0xFF77B9E5)
    val red = Color(0xFFD89E9A)
    val green = Color(0xFF9EDEBF)
}

internal fun duelRarityColor(rarity: String): Color = when (rarity.lowercase()) {
    "exotic" -> Color(0xFFE3B86B)
    "mythic" -> Color(0xFFCFB8E8)
    "legendary" -> Color(0xFF9D79C9)
    else -> Color(0xFF7A7585)
}

@Composable
internal fun DuelTableBackground(modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF39291F), Color(0xFF241E1C), Color(0xFF32241E))
                ), RoundedCornerShape(10.dp)
            )
            .border(2.dp, DuelPalette.rim, RoundedCornerShape(10.dp))
            .padding(4.dp)
            .border(1.dp, Color(0x777C6148), RoundedCornerShape(6.dp))
    )
}

/**
 * Low-contrast decorative board engraving rendered BEHIND the legal slots.
 * This supplies depth, not a new game zone or a claimed original game asset.
 */
@Composable
internal fun DuelTableEtching(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val copper = Color(0xFFB28A56).copy(alpha = 0.12f)
        val pale = Color(0xFFE8C89C).copy(alpha = 0.07f)
        val midX = size.width / 2f
        val midY = size.height / 2f
        val thickness = 0.8.dp.toPx()
        // Symmetric filigree lines follow the existing background only.
        for (fraction in listOf(0.14f, 0.27f, 0.73f, 0.86f)) {
            drawLine(copper,
                Offset(size.width * fraction, 0f),
                Offset(size.width * fraction, size.height),
                strokeWidth = thickness)
        }
        drawCircle(
            color = pale, radius = minOf(size.width, size.height) * 0.31f,
            center = Offset(midX, midY),
            style = Stroke(width = thickness * 2f)
        )
        drawCircle(
            color = copper, radius = minOf(size.width, size.height) * 0.22f,
            center = Offset(midX, midY),
            style = Stroke(width = thickness)
        )
        drawLine(pale, Offset(0f, midY), Offset(size.width, midY), thickness)
    }
}

@Composable
internal fun DuelRoundMarkers(round: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        for (n in 1..ShadowRules.ROUNDS) {
            val selected = n == round
            val color by animateColorAsState(
                if (selected) DuelPalette.gold else if (n < round) DuelPalette.rim else Color(0xFF4F413B),
                label = "round marker"
            )
            Box(
                Modifier.size(if (selected) 15.dp else 12.dp)
                    .background(Color(0xFF211A16), CircleShape)
                    .border(1.dp, color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (selected) Text(n.toString(), fontSize = 9.sp, color = DuelPalette.gold,
                    fontWeight = FontWeight.Bold)
                else Box(Modifier.size(4.dp).background(color, CircleShape))
            }
        }
    }
}

@Composable
internal fun DuelLightMeter(light: Int, round: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text("LIGHT", fontSize = 10.sp, letterSpacing = 1.sp,
            color = DuelPalette.dullGold, fontWeight = FontWeight.Bold)
        Text(light.toString() + " / " + round,
            color = DuelPalette.blue, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (i in 0 until ShadowRules.ROUNDS) {
                val active = i < light
                Box(Modifier.size(9.dp)
                    .background(if (active) DuelPalette.blue else Color(0xFF463C3C),
                        RoundedCornerShape(3.dp))
                    .border(1.dp, if (active) Color(0xFFCAE9FB) else DuelPalette.rim,
                        RoundedCornerShape(3.dp)))
            }
        }
    }
}

@Composable
internal fun DuelCardCover(
    coverId: String,
    modifier: Modifier = Modifier
) {
    val activity = LocalContext.current as? Activity
    val chosen = if (DuelCoverCatalog.isKnown(coverId)) coverId else "default"
    Box(
        modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF322529))
            .border(1.dp, Color(0xFFAD8D6C), RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Retain this neutral card-back silhouette if assets are unavailable.
        Text("♢", color = Color(0xFFBFA68F), fontSize = 15.sp)
        if (activity != null) {
            key(chosen) {
                AndroidView(
                    factory = { ctx ->
                        ImageView(ctx).apply {
                            scaleType = ImageView.ScaleType.FIT_XY
                            CardArt.bindCover(activity, this, chosen)
                        }
                    },
                    update = { image ->
                        if (image.tag != "cover_$chosen")
                            CardArt.bindCover(activity, image, chosen)
                    },
                    modifier = Modifier.fillMaxSize().padding(1.dp)
                )
            }
        }
    }
}

@Composable
internal fun DuelCardBacks(count: Int, coverId: String = "default") {
    Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
        repeat(count.coerceIn(0, 5)) {
            DuelCardCover(coverId, Modifier.width(20.dp).height(29.dp))
        }
    }
}

@Composable
internal fun DuelScore(player: Int, cpu: Int, playerTurn: Boolean) {
    // Only the displayed number interpolates. Engine Power remains exact and
    // is used for all rules and victory checks.
    val displayedPlayer by animateIntAsState(
        targetValue = player, animationSpec = tween(320), label = "player power display"
    )
    val displayedCpu by animateIntAsState(
        targetValue = cpu, animationSpec = tween(320), label = "cpu power display"
    )
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("TOTAL POWER", color = DuelPalette.dullGold, fontSize = 10.sp,
            letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(displayedPlayer.toString(), fontSize = 28.sp, fontWeight = FontWeight.Black,
                color = if (playerTurn) DuelPalette.cream else DuelPalette.blue)
            Text(":", color = DuelPalette.dullGold, fontSize = 18.sp)
            Text(displayedCpu.toString(), fontSize = 28.sp, fontWeight = FontWeight.Black,
                color = if (playerTurn) DuelPalette.red else DuelPalette.cream)
        }
        Text("YOU                 CPU", color = DuelPalette.dullGold, fontSize = 9.sp)
        Spacer(Modifier.height(4.dp))
        Text(if (playerTurn) "YOUR TURN" else "CPU TURN", fontSize = 10.sp,
            color = if (playerTurn) DuelPalette.blue else DuelPalette.red,
            fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun DuelCardArtwork(card: ShadowData.Card, modifier: Modifier = Modifier) {
    // Compose uses Context wrappers for themed windows. CardArt.bind accepts
    // any Context, so every card must load even without a direct Activity cast.
    key(card.name) {
        AndroidView(
            modifier = modifier,
            factory = { context ->
                ImageView(context).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    CardArt.bind(context, card, this)
                }
            },
            update = { image ->
                if (image.contentDescription != card.name + " card art") {
                    CardArt.bind(image.context, card, image)
                }
            }
        )
    }
}

@Composable
internal fun DuelCardFrame(
    card: ShadowData.Card,
    power: Int,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    playable: Boolean = true,
    suppressed: Boolean = false,
    inspired: Boolean = false,
    scorched: Boolean = false,
    compact: Boolean = true
) {
    val rarity = duelRarityColor(card.rarity)
    val lifted by animateFloatAsState(
        targetValue = if (selected) -8f else 0f,
        label = "hand selected"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        label = "hand scale"
    )
    Box(
        modifier
            .graphicsLayer {
                translationY = lifted * density
                scaleX = scale
                scaleY = scale
                alpha = if (playable) 1f else 0.65f
            }
            .shadow(if (selected) 8.dp else 2.dp, RoundedCornerShape(5.dp))
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0xFF21191C))
            .border(if (selected) 2.dp else 1.dp,
                if (selected) DuelPalette.gold else rarity, RoundedCornerShape(5.dp))
    ) {
        DuelCardArtwork(card, Modifier.fillMaxSize().padding(2.dp))
        // Use the printed Power and blue Light positions seen on in-game cards.
        Row(
            Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(3.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DuelStatChip(power.toString(), DuelPalette.cream, Color(0xE12A2524))
            DuelStatChip(card.cost.ifBlank { "?" }, Color(0xFFF5FBFF), Color(0xEE286B9C))
        }
        Box(
            Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth().background(Color(0xED201A1A))
                .padding(horizontal = 3.dp, vertical = if (compact) 1.dp else 3.dp)
        ) {
            Text(card.name, color = DuelPalette.cream,
                fontSize = if (compact) 8.sp else 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = if (compact) 1 else 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        if (suppressed) {
            Box(Modifier.fillMaxSize().background(Color(0x66141218)),
                contentAlignment = Alignment.Center) {
                Text("SUPPRESSED", fontSize = 8.sp, color = DuelPalette.cream,
                    fontWeight = FontWeight.Bold)
            }
        } else if (inspired || scorched) {
            Row(Modifier.align(Alignment.Center).padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                if (inspired) DuelStatusDot("✦", DuelPalette.gold)
                if (scorched) DuelStatusDot("✹", Color(0xFFFFB37B))
            }
        }
    }
}

@Composable
private fun DuelStatChip(value: String, color: Color, fill: Color) {
    Text(value, color = color,
        fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
        modifier = Modifier.background(fill, RoundedCornerShape(4.dp))
            .border(1.dp, Color(0x777C6B62), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp))
}

@Composable
private fun DuelStatusDot(value: String, color: Color) {
    Text(value, fontSize = 15.sp, color = color,
        modifier = Modifier.background(Color(0xC71E1A1C), CircleShape)
            .padding(horizontal = 4.dp, vertical = 1.dp))
}

@Composable
internal fun DuelSlotBackground(
    selected: Boolean,
    friendly: Boolean,
    occupied: Boolean,
    modifier: Modifier = Modifier,
    label: String = "+"
) {
    val line by animateColorAsState(
        targetValue = when {
            selected -> DuelPalette.gold
            occupied -> Color(0xFF755A48)
            friendly -> Color(0xFF6B5241)
            else -> Color(0xFF624544)
        }, label = "slot edge"
    )
    Box(
        modifier.background(
            Brush.verticalGradient(
                if (selected)
                    listOf(Color(0xFF48584D), Color(0xFF26332D))
                else listOf(Color(0xFF2D2521), Color(0xFF191617))
            ),
            RoundedCornerShape(5.dp)
        ).border(if (selected) 2.dp else 1.dp, line, RoundedCornerShape(5.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (!occupied) {
            Canvas(Modifier.fillMaxSize().padding(4.dp)) {
                val edge = minOf(size.width, size.height) * 0.15f
                val color = line.copy(alpha = if (selected) 0.9f else 0.35f)
                // Subtle engraved corner marks, not extra play zones.
                drawLine(color, Offset(0f, edge), Offset(0f, 0f), 1.dp.toPx(), StrokeCap.Square)
                drawLine(color, Offset(0f, 0f), Offset(edge, 0f), 1.dp.toPx(), StrokeCap.Square)
                drawLine(color, Offset(size.width - edge, size.height),
                    Offset(size.width, size.height), 1.dp.toPx(), StrokeCap.Square)
                drawLine(color, Offset(size.width, size.height),
                    Offset(size.width, size.height - edge), 1.dp.toPx(), StrokeCap.Square)
            }
            Text(label, color = if (selected) DuelPalette.gold else Color(0x666F6157),
                fontSize = if (selected) 22.sp else 15.sp)
        }
    }
}
