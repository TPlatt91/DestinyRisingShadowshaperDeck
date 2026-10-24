package com.tplatt.shadowshaper

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Original artwork drawn procedurally as a tactile leather binder. This is
 * a skin for the EXISTING deck builder, not a port of the original game's
 * landscape editor. No external art, card metadata or rules are changed.
 */
internal object BinderPalette {
    val edge = Color(0xFF120D0B)
    val leather = Color(0xFF352217)
    val leatherDark = Color(0xFF1C130F)
    val leatherLight = Color(0xFF493023)
    val inset = Color(0xFF110F0D)
    val bronze = Color(0xFFC69E67)
    val brass = Color(0xFF8F673C)
    val ivory = Color(0xFFEFE0C5)
    val muted = Color(0xFFC5AF8C)
}

private val binderCut = CutCornerShape(
    topStart = 8.dp, topEnd = 3.dp,
    bottomEnd = 8.dp, bottomStart = 3.dp
)

/** Leather, stitched side seams, lightly worn center, and inset leaf edges. */
@Composable
internal fun DuelBinderPage(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier
            .clip(binderCut)
            .background(BinderPalette.edge)
            .border(1.25.dp, BinderPalette.brass, binderCut)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xFF513422),
                        BinderPalette.leather,
                        BinderPalette.leatherLight,
                        BinderPalette.leatherDark
                    )
                )
            )
            // Center has the subtly illuminated grain of an open binder
            // page, but the high contrast card illustrations still dominate.
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFF110B08).copy(alpha = .42f),
                        Color(0xFF64432B).copy(alpha = .12f),
                        Color(0xFFAD8151).copy(alpha = .10f),
                        Color(0xFF130C08).copy(alpha = .47f)
                    )
                )
            )
            val seamLeft = 7.dp.toPx()
            val seamRight = size.width - 7.dp.toPx()
            val stitch = BinderPalette.bronze.copy(alpha = .51f)
            for (x in listOf(seamLeft, seamRight)) {
                drawLine(
                    BinderPalette.edge.copy(alpha = .83f),
                    Offset(x + 1.dp.toPx(), 8.dp.toPx()),
                    Offset(x + 1.dp.toPx(), size.height - 8.dp.toPx()),
                    2.dp.toPx()
                )
                var y = 15.dp.toPx()
                while (y < size.height - 11.dp.toPx()) {
                    drawLine(
                        stitch,
                        Offset(x - 2.dp.toPx(), y - 3.dp.toPx()),
                        Offset(x + 2.dp.toPx(), y + 3.dp.toPx()),
                        .8.dp.toPx()
                    )
                    y += 13.dp.toPx()
                }
            }
            drawLine(
                BinderPalette.bronze.copy(alpha = .55f),
                Offset(13.dp.toPx(), 7.dp.toPx()),
                Offset(size.width - 13.dp.toPx(), 7.dp.toPx()),
                .9.dp.toPx()
            )
            drawLine(
                BinderPalette.edge.copy(alpha = .91f),
                Offset(13.dp.toPx(), size.height - 7.dp.toPx()),
                Offset(size.width - 13.dp.toPx(), size.height - 7.dp.toPx()),
                1.2.dp.toPx()
            )
        }
        content()
    }
}

/** Brushed bronze tab attached to the binder leaf. Portrait width preserved. */
@Composable
internal fun DuelBinderTab(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val color = if (active) BinderPalette.bronze else BinderPalette.brass
    Box(
        modifier
            .height(43.dp)
            .graphicsLayer {
                if (pressed) {
                    scaleX = .984f
                    scaleY = .975f
                }
            }
            .clip(binderCut)
            .background(
                Brush.verticalGradient(
                    if (active) listOf(Color(0xFF5A3E2B), Color(0xFF302116))
                    else listOf(Color(0xFF291B15), Color(0xFF1B1411))
                )
            )
            .border(if (active) 1.2.dp else .85.dp, color.copy(alpha = .86f), binderCut)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val y = size.height - 2.dp.toPx()
            drawLine(
                if (active) BinderPalette.bronze else BinderPalette.brass.copy(alpha = .35f),
                Offset(10.dp.toPx(), y),
                Offset(size.width - 10.dp.toPx(), y),
                (if (active) 2f else .8f).dp.toPx()
            )
            drawLine(
                Color.White.copy(alpha = if (active) .15f else .045f),
                Offset(9.dp.toPx(), 2.dp.toPx()),
                Offset(size.width - 9.dp.toPx(), 2.dp.toPx()),
                .7.dp.toPx()
            )
        }
        Text(
            label,
            color = if (active) BinderPalette.ivory else BinderPalette.muted,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = .72.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Framed, recessed 3-column binder pocket. The complete original artwork is
 * drawn above the dark sleeve, with transparent film reflections and a
 * thin leather retaining lip at the bottom. No text/effects are obscured.
 *
 * Tapping the art inspects the card. The separate + control retains the
 * exact DeckViewModel add callback/legality that was already present.
 */
@Composable
internal fun DuelBinderCardSleeve(
    card: ShadowData.Card,
    copies: Int,
    onInspect: () -> Unit,
    onAdd: (() -> Unit)? = null,
    artwork: @Composable (Modifier) -> Unit
) {
    val cut = CutCornerShape(topStart = 3.dp, topEnd = 3.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
    val occupied = copies > 0
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(.665f)
            .clip(cut)
            .background(Color(0xFF120D0B))
            .border(
                1.1.dp,
                if (occupied) BinderPalette.bronze else BinderPalette.brass.copy(alpha = .85f),
                cut
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xFF241812),
                        Color(0xFF0D0C0B),
                        Color(0xFF271910)
                    )
                )
            )
            val inset = 3.dp.toPx()
            drawRect(
                Color(0xFF000000).copy(alpha = .68f),
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2, size.height - inset * 2),
                style = Stroke(1.8.dp.toPx())
            )
            // Etched pocket corners echo the brass binding without taking
            // space from card names or game text.
            val stroke = .8.dp.toPx()
            val gold = BinderPalette.bronze.copy(alpha = .64f)
            val left = inset + 1.2.dp.toPx()
            val right = size.width - left
            val top = inset + 1.2.dp.toPx()
            val bottom = size.height - top
            val segment = 10.dp.toPx()
            for (x in listOf(left, right)) {
                val sign = if (x == left) 1f else -1f
                drawLine(gold, Offset(x, top), Offset(x + sign * segment, top), stroke)
                drawLine(gold, Offset(x, top), Offset(x, top + segment), stroke)
                drawLine(gold, Offset(x, bottom), Offset(x + sign * segment, bottom), stroke)
                drawLine(gold, Offset(x, bottom), Offset(x, bottom - segment), stroke)
            }
        }

        Box(Modifier.fillMaxSize().padding(3.dp).clickable(onClick = onInspect)) {
            artwork(Modifier.fillMaxSize())
            // A very faint film highlight: keeps the underlying card face
            // fully visible rather than introducing a heavy dark overlay.
            Canvas(Modifier.fillMaxSize()) {
                val sheen = Path().apply {
                    moveTo(size.width * .07f, 0f)
                    lineTo(size.width * .41f, 0f)
                    lineTo(size.width * .08f, size.height * .45f)
                    lineTo(0f, size.height * .45f)
                    close()
                }
                drawPath(sheen, Color.White.copy(alpha = .045f))
            }
        }

        // Narrow leather pocket lip at the very bottom. This reads like a
        // binder sleeve rather than covering a large portion of card art.
        Canvas(
            Modifier.fillMaxWidth().height(5.dp).align(Alignment.BottomCenter)
        ) {
            drawRect(
                Brush.verticalGradient(
                    listOf(Color(0xA57C5C3C), Color(0xF018110C))
                )
            )
            drawLine(
                BinderPalette.bronze.copy(alpha = .66f),
                Offset(0f, 0f), Offset(size.width, 0f),
                .8.dp.toPx()
            )
        }

        if (occupied) {
            Text(
                "×$copies",
                color = BinderPalette.ivory,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.TopEnd)
                    .padding(5.dp)
                    .background(Color(0xE21A120E), CutCornerShape(2.dp))
                    .border(.6.dp, BinderPalette.bronze, CutCornerShape(2.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
        if (onAdd != null && card.isPlayable) {
            TextButton(
                onClick = onAdd,
                modifier = Modifier.align(Alignment.BottomEnd).padding(3.dp)
                    .height(28.dp)
                    .background(Color(0xEF36281B), CutCornerShape(3.dp))
                    .border(1.dp, BinderPalette.bronze, CutCornerShape(3.dp)),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text(
                    "+",
                    color = BinderPalette.ivory,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
