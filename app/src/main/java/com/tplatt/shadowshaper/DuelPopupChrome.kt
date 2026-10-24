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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * One consistent, original sci-fi popup language for Shadow Shaper Duels.
 * Replaces stock Material AlertDialog surfaces with a deliberately designed
 * framed panel: black-blue glass, concentric orbit, chamfered corners,
 * warm gold focus actions and quiet blue-gray secondary actions.
 *
 * All visual only: callers still own exactly the same game callbacks.
 */
internal object DuelPopupColors {
    val gold = Color(0xFFE6C78E)
    val goldSoft = Color(0xFFAA946F)
    val white = Color(0xFFF1EDE4)
    val muted = Color(0xFFA2AEB9)
    val ice = Color(0xFF9AB9CB)
    val border = Color(0xFF536674)
}

internal enum class DuelPopupSymbol {
    ORBIT, SETTINGS, CARDS, OPPONENT, READY, WARNING, LOG
}

private val panelCut = CutCornerShape(
    topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp
)
private val actionCut = CutCornerShape(
    topStart = 2.dp, topEnd = 10.dp, bottomEnd = 2.dp, bottomStart = 10.dp
)

@Composable
internal fun DuelPopupDialog(
    title: String,
    subtitle: String,
    symbol: DuelPopupSymbol,
    onDismissRequest: () -> Unit,
    allowDismiss: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = { if (allowDismiss) onDismissRequest() },
        properties = DialogProperties(
            dismissOnBackPress = allowDismiss,
            dismissOnClickOutside = allowDismiss,
            usePlatformDefaultWidth = false
        )
    ) {
        BoxWithConstraints(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val panelHeight = maxHeight * .91f
            Column(
                modifier = modifier
                    .fillMaxWidth(.91f)
                    .widthIn(max = 430.dp)
                    .heightIn(max = panelHeight)
                    .clip(panelCut)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF17232E),
                                Color(0xFF0E1822),
                                Color(0xFF0B121A)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                DuelPopupColors.gold.copy(alpha = .82f),
                                DuelPopupColors.border.copy(alpha = .82f),
                                DuelPopupColors.gold.copy(alpha = .44f)
                            )
                        ),
                        shape = panelCut
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 19.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                // Technological orbital/rune crown. No Bungie mark or
                // generated bitmap is part of the shipped Android UI.
                DuelPopupEmblem(symbol)
                Text(
                    "SHADOW SHAPER  /  DUELS",
                    color = DuelPopupColors.goldSoft,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.7.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Text(
                    title,
                    color = DuelPopupColors.white,
                    fontSize = if (title.length > 20) 20.sp else 23.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 25.sp,
                    letterSpacing = 1.6.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 3
                )
                DuelPopupRule()
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        color = DuelPopupColors.muted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        letterSpacing = .1.sp,
                        textAlign = TextAlign.Center
                    )
                }
                content()
            }
        }
    }
}

@Composable
private fun DuelPopupEmblem(symbol: DuelPopupSymbol) {
    val transition = rememberInfiniteTransition(label = "popup orbital ambient")
    val breathing by transition.animateFloat(
        initialValue = .53f,
        targetValue = .89f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2550),
            repeatMode = RepeatMode.Reverse
        ),
        label = "popup orbital glow"
    )
    Canvas(Modifier.size(78.dp)) {
        val c = Offset(size.width * .5f, size.height * .5f)
        val r = size.width * .425f
        drawCircle(
            Brush.radialGradient(
                listOf(
                    DuelPopupColors.gold.copy(alpha = .13f * breathing),
                    DuelPopupColors.ice.copy(alpha = .045f),
                    Color.Transparent
                ),
                center = c,
                radius = r * 1.2f
            ),
            radius = r * 1.2f,
            center = c
        )
        for (factor in listOf(.55f, .81f, 1f)) {
            drawCircle(
                color = DuelPopupColors.gold.copy(alpha = .16f + factor * .39f),
                radius = r * factor,
                center = c,
                style = Stroke(.82.dp.toPx())
            )
        }
        for (i in 0..7) {
            val angle = 2.0 * PI * i / 8.0 - PI / 2.0
            val dir = Offset(cos(angle).toFloat(), sin(angle).toFloat())
            drawLine(
                color = DuelPopupColors.gold.copy(alpha = if (i % 2 == 0) .78f else .36f),
                start = c + dir * (r * .91f),
                end = c + dir * (r * 1.08f),
                strokeWidth = .95.dp.toPx()
            )
        }
        when (symbol) {
            DuelPopupSymbol.ORBIT, DuelPopupSymbol.READY -> {
                drawCircle(
                    Color(0xFF09131D),
                    radius = r * .35f,
                    center = c
                )
                drawArc(
                    color = DuelPopupColors.gold.copy(alpha = breathing),
                    startAngle = 298f,
                    sweepAngle = 183f,
                    useCenter = false,
                    topLeft = Offset(c.x - r * .37f, c.y - r * .37f),
                    size = Size(r * .74f, r * .74f),
                    style = Stroke(3.8.dp.toPx())
                )
            }
            DuelPopupSymbol.SETTINGS -> {
                for (i in 0..2) {
                    val y = c.y + (i - 1) * r * .24f
                    drawLine(
                        DuelPopupColors.gold,
                        Offset(c.x - r * .42f, y),
                        Offset(c.x + r * .42f, y),
                        1.3.dp.toPx()
                    )
                    drawCircle(
                        DuelPopupColors.gold,
                        radius = 2.6.dp.toPx(),
                        center = Offset(c.x + (if (i == 1) -.15f else .19f) * r, y)
                    )
                }
            }
            DuelPopupSymbol.CARDS -> {
                for (i in 0..2) {
                    drawRect(
                        color = DuelPopupColors.gold.copy(alpha = .60f + i * .14f),
                        topLeft = Offset(
                            c.x - r * .38f + i * r * .12f,
                            c.y - r * .41f + i * r * .085f
                        ),
                        size = Size(r * .50f, r * .65f),
                        style = Stroke(1.2.dp.toPx())
                    )
                }
            }
            DuelPopupSymbol.OPPONENT -> {
                val diamond = Path().apply {
                    moveTo(c.x, c.y - r * .42f)
                    lineTo(c.x + r * .36f, c.y)
                    lineTo(c.x, c.y + r * .42f)
                    lineTo(c.x - r * .36f, c.y)
                    close()
                }
                drawPath(
                    diamond,
                    DuelPopupColors.gold,
                    style = Stroke(1.7.dp.toPx())
                )
                drawCircle(
                    DuelPopupColors.gold,
                    radius = r * .07f,
                    center = c
                )
            }
            DuelPopupSymbol.WARNING -> {
                val triangle = Path().apply {
                    moveTo(c.x, c.y - r * .43f)
                    lineTo(c.x + r * .39f, c.y + r * .34f)
                    lineTo(c.x - r * .39f, c.y + r * .34f)
                    close()
                }
                drawPath(triangle, DuelPopupColors.gold, style = Stroke(1.6.dp.toPx()))
                drawLine(
                    DuelPopupColors.gold,
                    Offset(c.x, c.y - r * .12f),
                    Offset(c.x, c.y + r * .13f),
                    2.dp.toPx()
                )
                drawCircle(DuelPopupColors.gold, 1.4.dp.toPx(), Offset(c.x, c.y + r * .24f))
            }
            DuelPopupSymbol.LOG -> {
                for (i in 0..3) {
                    drawLine(
                        DuelPopupColors.gold.copy(alpha = 1f - i * .14f),
                        Offset(c.x - r * .40f, c.y - r * .32f + i * r * .22f),
                        Offset(c.x + r * .40f, c.y - r * .32f + i * r * .22f),
                        1.15.dp.toPx()
                    )
                }
            }
        }
    }
}

@Composable
internal fun DuelPopupRule() {
    Canvas(Modifier.fillMaxWidth().height(11.dp)) {
        val c = size.width * .5f
        val y = size.height * .5f
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, DuelPopupColors.gold, Color.Transparent)
            ),
            start = Offset(size.width * .04f, y),
            end = Offset(size.width * .96f, y),
            strokeWidth = .75.dp.toPx()
        )
        val diamond = Path().apply {
            moveTo(c, y - 3.dp.toPx())
            lineTo(c + 3.dp.toPx(), y)
            lineTo(c, y + 3.dp.toPx())
            lineTo(c - 3.dp.toPx(), y)
            close()
        }
        drawPath(diamond, DuelPopupColors.gold)
    }
}

internal enum class DuelPopupActionStyle { PRIMARY, SECONDARY, QUIET, DANGER }

@Composable
internal fun DuelPopupAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: DuelPopupActionStyle = DuelPopupActionStyle.SECONDARY,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused = style == DuelPopupActionStyle.PRIMARY
    val danger = style == DuelPopupActionStyle.DANGER
    val tint = when {
        focused -> DuelPopupColors.gold
        danger -> Color(0xFFDFA69A)
        else -> DuelPopupColors.ice
    }
    Box(
        modifier
            .heightIn(min = if (style == DuelPopupActionStyle.QUIET) 43.dp else 50.dp)
            .graphicsLayer {
                val scale = if (pressed && enabled) .983f else 1f
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else .39f
            }
            .clip(actionCut)
            .background(
                Brush.horizontalGradient(
                    when {
                        focused -> listOf(Color(0xFF3E3327), Color(0xFF282923), Color(0xFF252723))
                        danger -> listOf(Color(0xFF30262A), Color(0xFF1F252C))
                        else -> listOf(Color(0xFF1A2A36), Color(0xFF111F2B))
                    }
                )
            )
            .border(
                width = if (focused) 1.25.dp else .8.dp,
                color = tint.copy(alpha = if (focused) .94f else .63f),
                shape = actionCut
            )
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val alpha = if (focused) .67f else .30f
            drawLine(
                tint.copy(alpha = alpha),
                Offset(3.dp.toPx(), 0f),
                Offset(17.dp.toPx(), 0f),
                1.2.dp.toPx()
            )
            drawLine(
                tint.copy(alpha = alpha),
                Offset(size.width - 17.dp.toPx(), size.height),
                Offset(size.width - 3.dp.toPx(), size.height),
                1.2.dp.toPx()
            )
            if (focused) {
                drawLine(
                    tint.copy(alpha = .51f),
                    Offset(size.width * .29f, size.height - 8.dp.toPx()),
                    Offset(size.width * .71f, size.height - 8.dp.toPx()),
                    .65.dp.toPx()
                )
            }
        }
        Text(
            label,
            color = if (focused) DuelPopupColors.gold else DuelPopupColors.white,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 9.dp)
        )
    }
}

@Composable
internal fun DuelPopupMenuRow(
    label: String,
    description: String,
    onClick: () -> Unit,
    highlighted: Boolean = false
) {
    val tint = if (highlighted) DuelPopupColors.gold else DuelPopupColors.ice
    DuelPopupAction(
        label = label,
        onClick = onClick,
        style = if (highlighted) DuelPopupActionStyle.PRIMARY else DuelPopupActionStyle.SECONDARY,
        modifier = Modifier.fillMaxWidth()
    )
}
