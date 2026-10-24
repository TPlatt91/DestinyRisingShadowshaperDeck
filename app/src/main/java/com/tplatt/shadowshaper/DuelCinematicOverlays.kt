package com.tplatt.shadowshaper

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Presentation-only classification of existing engine journal events. */
internal enum class DuelVisualEventKind {
    SUMMON, IMPACT, BUFF, SHIELD, STATUS, OTHER
}

internal object DuelVisualEvent {
    fun kind(message: String): DuelVisualEventKind {
        val event = message.uppercase()
        return when {
            "SUMMON RESTRICTED" in event || "NO OPEN SLOT" in event ->
                DuelVisualEventKind.OTHER
            "OVERSHIELD" in event || "DAMAGE PREVENTED" in event ->
                DuelVisualEventKind.SHIELD
            "DAMAGE " in event || "DESTROYED" in event ||
                "DETONAT" in event || "DESTROY EFFECT" in event ->
                DuelVisualEventKind.IMPACT
            "SUMMON" in event || " ▶ " in event && ("YOU " in event || "CPU " in event) ->
                DuelVisualEventKind.SUMMON
            "BUFF " in event || "INSPIRED" in event ||
                "POWER BONUS" in event || " + " in event ->
                DuelVisualEventKind.BUFF
            "SUPPRESSED" in event || "SCORCHED" in event || "RELOCAT" in event ->
                DuelVisualEventKind.STATUS
            else -> DuelVisualEventKind.OTHER
        }
    }

    fun caption(message: String): String = when (kind(message)) {
        DuelVisualEventKind.SUMMON -> "UNIT DEPLOYED"
        DuelVisualEventKind.IMPACT -> "COMBAT IMPACT"
        DuelVisualEventKind.BUFF -> "POWER SHIFT"
        DuelVisualEventKind.SHIELD -> "SHIELD ACTIVATED"
        DuelVisualEventKind.STATUS -> "FIELD EFFECT"
        DuelVisualEventKind.OTHER -> "BATTLE EVENT"
    }

    fun accent(kind: DuelVisualEventKind): Color = when (kind) {
        DuelVisualEventKind.SUMMON -> Color(0xFF98CFF6)
        DuelVisualEventKind.IMPACT -> Color(0xFFFF988C)
        DuelVisualEventKind.BUFF -> Color(0xFFA6EDBA)
        DuelVisualEventKind.SHIELD -> Color(0xFF91D8FF)
        DuelVisualEventKind.STATUS -> Color(0xFFD8B3EE)
        DuelVisualEventKind.OTHER -> Color(0xFFE4C68E)
    }
}

/** Brief full-field round transition with engraved circular rune and light sweep. */
@Composable
internal fun DuelCinematicTurnBanner(text: String) {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(text) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, tween(280, easing = FastOutSlowInEasing))
    }
    Box(
        Modifier.fillMaxWidth(.83f)
            .height(89.dp)
            .graphicsLayer {
                scaleX = .91f + reveal.value * .09f
                scaleY = .91f + reveal.value * .09f
                alpha = reveal.value
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxWidth().height(89.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val gold = Color(0xFFDFBE82)
                drawRect(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, Color(0xDD111A27), Color.Transparent)
                ),
                topLeft = Offset(0f, cy - 22.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(size.width, 44.dp.toPx())
            )
            val radius = 34.dp.toPx()
            val diamond = Path().apply {
                moveTo(cx, cy - radius)
                lineTo(cx + radius, cy)
                lineTo(cx, cy + radius)
                lineTo(cx - radius, cy)
                close()
            }
            drawPath(
                diamond, color = gold.copy(alpha = .38f),
                style = Stroke(width = 1.dp.toPx())
            )
            for (distance in listOf(.10f, .90f)) {
                val x = size.width * distance
                drawLine(
                    color = gold.copy(alpha = .6f * reveal.value),
                    start = Offset(x, cy - 17.dp.toPx()),
                    end = Offset(x, cy + 17.dp.toPx()),
                    strokeWidth = 1.2.dp.toPx()
                )
            }
            drawLine(
                color = gold.copy(alpha = .68f),
                start = Offset(0f, cy - 22.dp.toPx()),
                end = Offset(size.width, cy - 22.dp.toPx()),
                strokeWidth = .9.dp.toPx()
            )
            drawLine(
                color = gold.copy(alpha = .68f),
                start = Offset(0f, cy + 22.dp.toPx()),
                end = Offset(size.width, cy + 22.dp.toPx()),
                strokeWidth = .9.dp.toPx()
            )
        }
        Text(
            text,
            color = Color(0xFFFFE5AE),
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            letterSpacing = 1.1.sp,
            maxLines = 2,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    }
}

/** Game-log driven status flash; no gameplay updates or made-up attacks. */
@Composable
internal fun DuelCinematicEventRibbon(message: String) {
    val kind = DuelVisualEvent.kind(message)
    val accent = DuelVisualEvent.accent(kind)
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(message) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
    }
    Box(
        modifier = Modifier
            .fillMaxWidth(.90f)
            .graphicsLayer {
                translationX = (1f - reveal.value) * (-22.dp.toPx())
                alpha = reveal.value
            }
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xF0202834), Color(0xF1131922), Color(0xDD18202C))
                ),
                RoundedCornerShape(3.dp)
            )
            .border(1.dp, accent.copy(alpha = .80f), RoundedCornerShape(3.dp))
            .padding(horizontal = 11.dp, vertical = 7.dp)
    ) {
        androidx.compose.foundation.layout.Column {
            Text(
                text = DuelVisualEvent.caption(message),
                color = accent,
                fontWeight = FontWeight.Black,
                fontSize = 10.sp,
                letterSpacing = .9.sp
            )
            Text(
                text = message,
                color = Color(0xFFE6E9EA),
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )
        }
    }
}
