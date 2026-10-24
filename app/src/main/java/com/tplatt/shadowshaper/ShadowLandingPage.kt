package com.tplatt.shadowshaper

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Distinct Shadow Shaper Duels landing page, inspired by cinematic sci-fi
 * game interfaces. Graphics are original lightweight Compose vectors:
 * no borrowed Bungie logo, fan art, network images or extra build assets.
 *
 * Four primary destinations, exactly as requested. An unfinished match
 * optionally gets a separate Continue ribbon; gameplay callbacks are intact.
 */
private val landingInk = Color(0xFF080D14)
private val landingNight = Color(0xFF111A25)
private val landingPale = Color(0xFFF2EADC)
private val landingGold = Color(0xFFE4C68F)
private val landingGoldDark = Color(0xFF957A55)
private val landingMuted = Color(0xFF9DA5AC)
private val landingIce = Color(0xFF9BCCEE)
private val landingRule = Color(0xFF465364)

@Composable
internal fun ShadowLandingPage(
    onSkirmish: () -> Unit,
    onContinue: () -> Unit,
    hasSavedMatch: Boolean,
    onDeckConstruction: () -> Unit,
    onCardCollection: () -> Unit,
    onRules: () -> Unit,
    catalogCount: Int
) {
    val ambient = rememberInfiniteTransition(label = "quiet landing ambience")
    val glow by ambient.animateFloat(
        initialValue = .36f,
        targetValue = .78f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4300),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbital light"
    )
    Box(Modifier.fillMaxSize().background(landingInk)) {
        LandingAtmosphere(glow, Modifier.fillMaxSize())
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 22.dp, end = 22.dp, top = 18.dp, bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            item { LandingHero(glow) }
            if (hasSavedMatch) {
                item {
                    LandingContinue(
                        onClick = onContinue,
                        description = "RETURN TO YOUR UNFINISHED SKIRMISH"
                    )
                }
            }
            item {
                LandingAction(
                    index = "01",
                    title = "SKIRMISH",
                    subtitle = "SIX ROUNDS  /  SINGLE-PLAYER BATTLE",
                    kind = LandingGlyphKind.BATTLE,
                    emphasized = true,
                    onClick = onSkirmish
                )
            }
            item {
                LandingAction(
                    index = "02",
                    title = "DECK CONSTRUCTION",
                    subtitle = "BUILD AND REFINE YOUR 15-CARD DECKS",
                    kind = LandingGlyphKind.DECK,
                    onClick = onDeckConstruction
                )
            }
            item {
                LandingAction(
                    index = "03",
                    title = "CARD COLLECTION",
                    subtitle = "EXPLORE $catalogCount RECORDED CARDS",
                    kind = LandingGlyphKind.COLLECTION,
                    onClick = onCardCollection
                )
            }
            item {
                LandingAction(
                    index = "04",
                    title = "RULES",
                    subtitle = "LEARN THE FLOW OF BATTLE",
                    kind = LandingGlyphKind.RULES,
                    onClick = onRules
                )
            }
            item { LandingFooter(catalogCount) }
        }
    }
}

@Composable
private fun LandingAtmosphere(glow: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val width = size.width
        val height = size.height
        drawRect(
            Brush.verticalGradient(
                listOf(
                    Color(0xFF0A0F17),
                    Color(0xFF13202B),
                    Color(0xFF101722),
                    Color(0xFF080D13)
                )
            )
        )

        // Restrained atmospheric glow and technical lines; low contrast
        // guarantees readable text even on smaller Android screens.
        val lightOrigin = Offset(width * .84f, height * .14f)
        drawCircle(
            brush = Brush.radialGradient(
                0f to landingGold.copy(alpha = .11f * glow),
                .45f to landingIce.copy(alpha = .045f * glow),
                1f to Color.Transparent,
                center = lightOrigin,
                radius = width * .76f
            ),
            radius = width * .76f,
            center = lightOrigin
        )
        for (i in 0..8) {
            val x = width * (-.2f + i * .18f)
            drawLine(
                color = landingIce.copy(alpha = .043f),
                start = Offset(x, 0f),
                end = Offset(x + width * .22f, height),
                strokeWidth = .7.dp.toPx()
            )
        }
        for (i in 0..12) {
            val y = height * (.06f + i * .075f)
            drawLine(
                color = landingGold.copy(alpha = if (i % 4 == 0) .045f else .018f),
                start = Offset(width * .06f, y),
                end = Offset(width * .94f, y),
                strokeWidth = .6.dp.toPx()
            )
        }
        // Upper-right survey rings appear behind the hero emblem.
        repeat(3) { idx ->
            drawCircle(
                color = landingGold.copy(alpha = .07f - idx * .015f),
                radius = width * (.20f + idx * .085f),
                center = lightOrigin,
                style = Stroke(width = .7.dp.toPx())
            )
        }
        // Bottom left diagonal architectural sweeps.
        val band = Path().apply {
            moveTo(0f, height * .77f)
            lineTo(width * .24f, height)
            lineTo(width * .13f, height)
            lineTo(0f, height * .88f)
            close()
        }
        drawPath(band, color = landingIce.copy(alpha = .045f))
    }
}

@Composable
private fun LandingHero(glow: Float) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().height(244.dp)
    ) {
        val narrow = maxWidth < 355.dp
        LandingOrbitalEmblem(
            glow = glow,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 5.dp, end = 1.dp)
                .size(if (narrow) 126.dp else 155.dp)
        )
        Column(
            modifier = Modifier.align(Alignment.TopStart).fillMaxWidth(),
            verticalArrangement = Arrangement.Top
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Box(
                    Modifier.size(6.dp)
                        .background(landingGold, CutCornerShape(1.dp))
                )
                Text(
                    "DESTINY: RISING",
                    color = landingGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    maxLines = 1
                )
            }
            Spacer(Modifier.height(38.dp))
            Text(
                "SHADOW",
                fontSize = if (narrow) 33.sp else 39.sp,
                lineHeight = if (narrow) 36.sp else 41.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                color = landingPale,
                letterSpacing = 1.sp,
                maxLines = 1
            )
            Text(
                "SHAPER",
                fontSize = if (narrow) 33.sp else 39.sp,
                lineHeight = if (narrow) 36.sp else 41.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                color = landingPale,
                letterSpacing = 1.sp,
                maxLines = 1
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "DUELS",
                    fontSize = if (narrow) 40.sp else 47.sp,
                    lineHeight = if (narrow) 43.sp else 49.sp,
                    color = landingGold,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                    maxLines = 1
                )
                Canvas(Modifier.width(36.dp).height(20.dp)) {
                    drawLine(
                        color = landingGold.copy(alpha = .78f),
                        start = Offset(0f, size.height * .5f),
                        end = Offset(size.width, size.height * .5f),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
        }

        Row(
            modifier = Modifier.align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(bottom = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(Modifier.size(16.dp)) {
                val c = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    landingGold.copy(alpha = .83f),
                    radius = size.width * .37f,
                    center = c,
                    style = Stroke(.9.dp.toPx())
                )
                drawLine(
                    landingGold,
                    Offset(c.x, 0f), Offset(c.x, size.height),
                    .7.dp.toPx()
                )
            }
            Text(
                "A TACTICAL CARD GAME  /  SOLO AGAINST AI",
                fontSize = if (narrow) 8.sp else 9.sp,
                color = landingMuted,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.05.sp,
                maxLines = 1
            )
        }
        Box(
            Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            landingGold.copy(alpha = .9f),
                            landingRule.copy(alpha = .55f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
private fun LandingOrbitalEmblem(glow: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val c = Offset(size.width * .5f, size.height * .5f)
        val radius = size.width * .435f
        val gold = landingGold.copy(alpha = .57f * glow)
        val thin = .8.dp.toPx()
        drawCircle(landingGold.copy(alpha = .035f * glow), radius * 1.1f, c)
        drawCircle(gold, radius, c, style = Stroke(thin))
        drawCircle(gold.copy(alpha = .6f), radius * .78f, c, style = Stroke(thin))
        drawCircle(gold.copy(alpha = .85f), radius * .13f, c, style = Stroke(thin))
        for (index in 0 until 8) {
            val theta = (index * PI / 4.0 - PI / 2.0)
            val direction = Offset(cos(theta).toFloat(), sin(theta).toFloat())
            drawLine(
                color = gold,
                start = c + direction * (radius * .81f),
                end = c + direction * (radius * 1.02f),
                strokeWidth = if (index % 2 == 0) 1.6.dp.toPx() else thin
            )
        }
        val kite = Path().apply {
            moveTo(c.x, c.y - radius * .60f)
            lineTo(c.x + radius * .36f, c.y)
            lineTo(c.x, c.y + radius * .60f)
            lineTo(c.x - radius * .36f, c.y)
            close()
        }
        drawPath(kite, gold.copy(alpha = .88f), style = Stroke(1.1.dp.toPx()))
        drawLine(
            gold.copy(alpha = .75f),
            Offset(c.x - radius * .66f, c.y),
            Offset(c.x + radius * .66f, c.y),
            thin
        )
        drawLine(
            gold.copy(alpha = .75f),
            Offset(c.x, c.y - radius * .68f),
            Offset(c.x, c.y + radius * .68f),
            thin
        )
    }
}

private enum class LandingGlyphKind { BATTLE, DECK, COLLECTION, RULES }

private val landingCut = CutCornerShape(
    topStart = 2.dp, topEnd = 13.dp,
    bottomEnd = 2.dp, bottomStart = 13.dp
)

@Composable
private fun LandingAction(
    index: String,
    title: String,
    subtitle: String,
    kind: LandingGlyphKind,
    emphasized: Boolean = false,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(140),
        label = "landing tile press"
    )
    val primary = emphasized || pressed
    val edge = if (primary) landingGold.copy(alpha = .88f)
        else landingRule.copy(alpha = .90f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 94.dp)
            .graphicsLayer {
                scaleX = 1f - press * .014f
                scaleY = 1f - press * .026f
            }
            .clip(landingCut)
            .background(
                Brush.horizontalGradient(
                    if (primary) listOf(
                        Color(0xFF26323C), Color(0xFF18232F), Color(0xFF141C26)
                    ) else listOf(
                        Color(0xFF1D2833), Color(0xFF15202A), Color(0xFF121B24)
                    )
                )
            )
            .border(1.dp, edge, landingCut)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .semantics { contentDescription = title + ". " + subtitle }
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(
                color = if (primary) landingGold else landingIce,
                topLeft = Offset.Zero,
                size = Size(3.dp.toPx(), size.height)
            )
            drawLine(
                color = landingGold.copy(alpha = if (primary) .38f else .12f),
                start = Offset(size.width * .71f, 0f),
                end = Offset(size.width * .85f, size.height),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = landingRule.copy(alpha = .5f),
                start = Offset(size.width - 29.dp.toPx(), 0f),
                end = Offset(size.width - 29.dp.toPx(), size.height),
                strokeWidth = .5.dp.toPx()
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 15.dp, end = 12.dp, top = 15.dp, bottom = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                Modifier
                    .size(58.dp)
                    .clip(CutCornerShape(topEnd = 8.dp, bottomStart = 8.dp))
                    .background(Color(0xFF0D1721))
                    .border(
                        .8.dp,
                        if (primary) landingGoldDark else landingRule,
                        CutCornerShape(topEnd = 8.dp, bottomStart = 8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                LandingGlyph(kind, Modifier.size(36.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    index + "  /  TACTICAL MENU",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.35.sp,
                    color = if (primary) landingGold else landingMuted.copy(alpha = .75f),
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    title,
                    fontSize = if (title.length > 15) 16.sp else 20.sp,
                    lineHeight = 22.sp,
                    letterSpacing = .5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = landingPale,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    letterSpacing = .2.sp,
                    color = landingMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            LandingChevron(
                Modifier.width(14.dp).height(24.dp),
                color = if (primary) landingGold else landingIce.copy(alpha = .72f)
            )
        }
    }
}

@Composable
private fun LandingContinue(onClick: () -> Unit, description: String) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val brightness by animateFloatAsState(
        if (pressed) 1f else 0f,
        label = "continue glow"
    )
    val cut = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clip(cut)
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF1B3444), Color(0xFF142836), Color(0xFF111E29))
                )
            )
            .border(
                1.dp,
                landingIce.copy(alpha = .65f + brightness * .30f),
                cut
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 17.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Canvas(Modifier.size(31.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawArc(
                landingIce,
                startAngle = -75f,
                sweepAngle = 285f,
                useCenter = false,
                topLeft = Offset(size.width * .10f, size.height * .10f),
                size = Size(size.width * .8f, size.height * .8f),
                style = Stroke(2.dp.toPx())
            )
            drawLine(
                landingIce, Offset(size.width * .81f, size.height * .08f),
                Offset(size.width * .87f, size.height * .36f), 2.dp.toPx()
            )
            drawCircle(landingIce, 1.5.dp.toPx(), center)
        }
        Column(Modifier.weight(1f)) {
            Text(
                "CONTINUE BATTLE",
                color = landingPale,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                description,
                color = landingIce.copy(alpha = .83f),
                fontSize = 9.sp,
                maxLines = 1,
                letterSpacing = .55.sp,
                overflow = TextOverflow.Ellipsis
            )
        }
        LandingChevron(Modifier.width(13.dp).height(19.dp), landingIce)
    }
}

@Composable
private fun LandingGlyph(kind: LandingGlyphKind, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val c = Offset(size.width * .5f, size.height * .5f)
        val w = size.width
        val h = size.height
        val fg = landingGold
        val line = 1.65.dp.toPx()
        when (kind) {
            LandingGlyphKind.BATTLE -> {
                // Crossed blades, a deliberate tactical mark without emoji.
                drawLine(fg, Offset(w * .22f, h * .18f), Offset(w * .76f, h * .82f), line)
                drawLine(fg, Offset(w * .76f, h * .18f), Offset(w * .22f, h * .82f), line)
                drawLine(fg, Offset(w * .17f, h * .63f), Offset(w * .39f, h * .78f), line)
                drawLine(fg, Offset(w * .61f, h * .78f), Offset(w * .83f, h * .63f), line)
                drawCircle(fg, 2.dp.toPx(), c, style = Stroke(.8.dp.toPx()))
            }
            LandingGlyphKind.DECK -> {
                for (i in 0..2) {
                    val left = w * (.21f + i * .08f)
                    val top = h * (.18f + i * .08f)
                    drawRect(
                        color = fg.copy(alpha = .58f + .18f * i),
                        topLeft = Offset(left, top),
                        size = Size(w * .43f, h * .57f),
                        style = Stroke(line)
                    )
                }
                drawLine(fg, Offset(w * .44f, h * .51f), Offset(w * .66f, h * .51f), line)
            }
            LandingGlyphKind.COLLECTION -> {
                val diamond = Path().apply {
                    moveTo(c.x, h * .10f)
                    lineTo(w * .87f, c.y)
                    lineTo(c.x, h * .90f)
                    lineTo(w * .13f, c.y)
                    close()
                }
                drawPath(diamond, fg, style = Stroke(line))
                drawCircle(fg, radius = w * .16f, center = c, style = Stroke(line))
                drawLine(fg, Offset(c.x, h * .13f), Offset(c.x, h * .87f), .8.dp.toPx())
            }
            LandingGlyphKind.RULES -> {
                val left = Path().apply {
                    moveTo(w * .13f, h * .21f)
                    lineTo(w * .43f, h * .26f)
                    lineTo(w * .43f, h * .80f)
                    lineTo(w * .13f, h * .73f)
                    close()
                }
                val right = Path().apply {
                    moveTo(w * .57f, h * .26f)
                    lineTo(w * .87f, h * .21f)
                    lineTo(w * .87f, h * .73f)
                    lineTo(w * .57f, h * .80f)
                    close()
                }
                drawPath(left, fg, style = Stroke(line))
                drawPath(right, fg, style = Stroke(line))
                drawLine(fg, Offset(c.x, h * .26f), Offset(c.x, h * .84f), line)
                drawLine(fg.copy(alpha = .6f),
                    Offset(w * .20f, h * .40f), Offset(w * .35f, h * .44f), .8.dp.toPx())
                drawLine(fg.copy(alpha = .6f),
                    Offset(w * .65f, h * .44f), Offset(w * .80f, h * .40f), .8.dp.toPx())
            }
        }
    }
}

@Composable
private fun LandingChevron(modifier: Modifier = Modifier, color: Color = landingGold) {
    Canvas(modifier) {
        val x = size.width * .33f
        val y = size.height * .5f
        drawLine(
            color,
            Offset(x, y - size.height * .23f),
            Offset(size.width * .76f, y),
            1.7.dp.toPx()
        )
        drawLine(
            color,
            Offset(size.width * .76f, y),
            Offset(x, y + size.height * .23f),
            1.7.dp.toPx()
        )
    }
}

@Composable
private fun LandingFooter(catalogCount: Int) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Canvas(Modifier.fillMaxWidth().height(11.dp)) {
            val mid = size.width * .5f
            drawLine(
                landingRule.copy(alpha = .7f),
                Offset(0f, size.height * .5f),
                Offset(mid - 16.dp.toPx(), size.height * .5f),
                .75.dp.toPx()
            )
            drawLine(
                landingRule.copy(alpha = .7f),
                Offset(mid + 16.dp.toPx(), size.height * .5f),
                Offset(size.width, size.height * .5f),
                .75.dp.toPx()
            )
            drawCircle(
                landingGold.copy(alpha = .72f),
                radius = 2.8.dp.toPx(),
                center = Offset(mid, size.height * .5f),
                style = Stroke(.9.dp.toPx())
            )
        }
        Text(
            "SHADOW SHAPER DUELS   /   $catalogCount CARDS ARCHIVED",
            color = landingMuted,
            fontSize = 9.sp,
            letterSpacing = 1.sp,
            maxLines = 1
        )
        Text(
            "UNVERIFIED EFFECTS ARE IDENTIFIED IN CARD DETAILS",
            color = landingMuted.copy(alpha = .66f),
            fontSize = 8.sp,
            letterSpacing = .35.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
