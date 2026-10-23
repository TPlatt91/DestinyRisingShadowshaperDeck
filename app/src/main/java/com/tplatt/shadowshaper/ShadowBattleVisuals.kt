package com.tplatt.shadowshaper

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
    // Procedural stone slab visual: no fake card faces or extra play zones.
    // The four legal battle rows are laid over this texture by the engine UI.
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val joint = Color(0xFFAFB0AA).copy(alpha = 0.38f)
        val worn = Color(0xFF17191B).copy(alpha = 0.20f)
        val scratch = Color(0xFFE0DDD2).copy(alpha = 0.14f)
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF4C4D4B), Color(0xFF676763), Color(0xFF484A48))
            )
        )
        // Wide horizontal stone courses behind each opposing/friendly row.
        for (fraction in listOf(0.25f, 0.50f, 0.75f)) {
            val y = h * fraction
            drawLine(joint, Offset(0f, y), Offset(w, y), 1.1.dp.toPx())
            drawLine(worn, Offset(0f, y + 2.dp.toPx()),
                Offset(w, y + 2.dp.toPx()), 1.dp.toPx())
        }
        for (n in 1 until ShadowRules.COLUMNS) {
            val x = w * n / ShadowRules.COLUMNS
            drawLine(joint.copy(alpha = 0.19f),
                Offset(x, h * 0.05f), Offset(x, h * 0.95f), 0.8.dp.toPx())
        }
        // Sparse worn slate scratches, never card or zone decorations.
        for (j in 0..9) {
            val x = w * ((j * 19 + 7) % 97) / 97f
            val y = h * ((j * 31 + 5) % 89) / 89f
            val length = w * (0.025f + (j % 4) * 0.009f)
            drawLine(scratch, Offset(x, y), Offset(x + length, y - length * 0.21f),
                0.8.dp.toPx())
        }
        // Field border: steel instead of the earlier copper filigree.
        drawRect(color = Color(0xFFB2A89B).copy(alpha = 0.33f),
            topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(
                (w - 2.dp.toPx()).coerceAtLeast(0f),
                (h - 2.dp.toPx()).coerceAtLeast(0f)
            ), style = Stroke(width = 1.dp.toPx()))
    }
}

@Composable
internal fun DuelRoundMarkers(round: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        for (n in 1..ShadowRules.ROUNDS) {
            val selected = n == round
            val color by animateColorAsState(
                if (selected) DuelPalette.gold else if (n < round) DuelPalette.rim else Color(0xFF4F413B),
                label = "round marker"
            )
            Box(
                Modifier.size(if (selected) 12.dp else 9.dp)
                    .background(Color(0xFF211A16), CircleShape)
                    .border(1.dp, color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (selected) Text(n.toString(), fontSize = 8.sp, color = DuelPalette.gold,
                    fontWeight = FontWeight.Bold)
                else Box(Modifier.size(3.dp).background(color, CircleShape))
            }
        }
    }
}

@Composable
internal fun DuelLightMeter(light: Int, round: Int, compact: Boolean = false) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 1.dp else 3.dp)
    ) {
        Text(
            "LIGHT", fontSize = if (compact) 9.sp else 10.sp,
            letterSpacing = if (compact) 0.sp else 1.sp,
            color = DuelPalette.dullGold, fontWeight = FontWeight.Bold
        )
        Text(
            "$light / $round", color = DuelPalette.blue,
            fontSize = if (compact) 17.sp else 22.sp,
            lineHeight = if (compact) 19.sp else 25.sp,
            fontWeight = FontWeight.Black, maxLines = 1
        )
        Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 4.dp)) {
            for (i in 0 until ShadowRules.ROUNDS) {
                val active = i < light
                Box(
                    Modifier.size(if (compact) 5.dp else 9.dp)
                        .background(
                            if (active) DuelPalette.blue else Color(0xFF463C3C),
                            RoundedCornerShape(2.dp)
                        )
                        .border(
                            0.6.dp,
                            if (active) Color(0xFFCAE9FB) else DuelPalette.rim,
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    }
}

@Composable
internal fun DuelCardCover(
    coverId: String,
    modifier: Modifier = Modifier
) {
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
        key(chosen) {
            AndroidView(
                factory = { ctx ->
                    ImageView(ctx).apply {
                        scaleType = ImageView.ScaleType.FIT_XY
                        CardArt.bindCover(ctx, this, chosen)
                    }
                },
                update = { image ->
                    if (image.tag != "cover_$chosen")
                        CardArt.bindCover(image.context, image, chosen)
                },
                modifier = Modifier.fillMaxSize().padding(1.dp)
            )
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
private fun DuelScoreLine(name: String, value: Int, highlight: Color) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Text(
            name, color = DuelPalette.cream, fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
        Box(
            Modifier.size(36.dp)
                .background(
                    Brush.radialGradient(listOf(Color(0xFF353636), Color(0xFF191B20))),
                    CircleShape
                )
                .border(2.dp, highlight, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                value.toString(), color = DuelPalette.cream,
                fontSize = 20.sp, fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
internal fun DuelScore(player: Int, cpu: Int, playerTurn: Boolean) {
    val displayedPlayer by animateIntAsState(
        targetValue = player, animationSpec = tween(320), label = "player score"
    )
    val displayedCpu by animateIntAsState(
        targetValue = cpu, animationSpec = tween(320), label = "opponent score"
    )
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        DuelScoreLine("CPU", displayedCpu, Color(0xFFBC756E))
        DuelScoreLine("YOU", displayedPlayer, Color(0xFFE4B968))
        Text(
            if (playerTurn) "YOUR TURN" else "OPPONENT TURN",
            color = if (playerTurn) DuelPalette.gold else DuelPalette.red,
            fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1
        )
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
        DuelCardArtwork(card, Modifier.fillMaxSize().padding(if (compact) 1.dp else 0.dp))
        // The source battlefield only shows the CURRENT unit Power on its
        // miniatures. Hand cards also show their printed Light cost.
        Row(
            Modifier.align(Alignment.TopCenter).fillMaxWidth()
                .padding(if (compact) 1.dp else 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DuelStatChip(
                power.toString(),
                when {
                    power > card.numericPower() -> DuelPalette.green
                    power < card.numericPower() -> Color(0xFFFFA8A8)
                    else -> DuelPalette.cream
                },
                Color(0xD8211F22), miniature = compact
            )
            if (!compact) {
                DuelStatChip(
                    card.cost.ifBlank { "?" }, Color(0xFFF5FBFF),
                    Color(0xE6286B9C), miniature = false
                )
            }
        }
        // Status effects stay legible at miniature size without placing a
        // full-screen tint or centered label over the original card artwork.
        if (suppressed || inspired || scorched) {
            Row(
                Modifier.align(Alignment.BottomEnd).padding(1.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                if (suppressed) DuelStatusDot("⊘", Color(0xFFFFC9CB))
                if (inspired && !suppressed) DuelStatusDot("✦", DuelPalette.green)
                if (scorched) DuelStatusDot("✹", Color(0xFFFFB37B))
            }
        }
    }
}

@Composable
private fun DuelStatChip(
    value: String, color: Color, fill: Color, miniature: Boolean = false
) {
    Text(
        value, color = color,
        fontSize = if (miniature) 10.sp else 11.sp,
        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
        modifier = Modifier
            .background(fill, RoundedCornerShape(3.dp))
            .border(0.5.dp, Color(0x997C6B62), RoundedCornerShape(3.dp))
            .padding(
                horizontal = if (miniature) 2.dp else 3.dp,
                vertical = if (miniature) 0.dp else 1.dp
            )
    )
}

@Composable
private fun DuelStatusDot(value: String, color: Color) {
    Text(value, fontSize = 10.sp, color = color,
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
    // Empty cells in the original battle are unframed stone: no tall gray
    // cards, outlines, or corner brackets until a real card is placed.
    // A glowing selection remains for legal placement, movement and targets.
    Box(modifier, contentAlignment = Alignment.Center) {
        if (selected) {
            Box(
                Modifier.fillMaxSize()
                    .background(
                        if (friendly) Color(0x5534BFA3) else Color(0x66BDA67A),
                        RoundedCornerShape(3.dp)
                    )
                    .border(1.6.dp, DuelPalette.gold, RoundedCornerShape(3.dp))
            )
            Text(
                label, color = DuelPalette.gold,
                fontSize = 20.sp, fontWeight = FontWeight.Bold
            )
        }
    }
}
