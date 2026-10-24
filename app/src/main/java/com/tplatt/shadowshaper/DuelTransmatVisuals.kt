package com.tplatt.shadowshaper

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Lightweight, deterministic Destiny-inspired transmat visual effects.
 * Drawn BEHIND the real card, only during a confirmed placement/summon.
 * No Android bitmaps, random-per-frame particle allocation, input targets,
 * gameplay timers, or changes to ShadowEngine.
 */
@Composable
internal fun DuelTransmatArrival(
    style: DuelTransmatStyle,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val p = progress.coerceIn(0f, 1f)
        val alpha = DuelTransmatResolver.visibility(p)
        if (alpha <= .001f) return@Canvas

        val c = Offset(size.width * .5f, size.height * .55f)
        val radius = size.width * .69f
        when (style) {
            DuelTransmatStyle.DEFAULT -> defaultTransmat(c, radius, p, alpha)
            DuelTransmatStyle.HIVE -> hiveTransmat(c, radius, p, alpha)
            DuelTransmatStyle.CABAL -> cabalTransmat(c, radius, p, alpha)
            DuelTransmatStyle.VEX -> vexTransmat(c, radius, p, alpha)
            DuelTransmatStyle.FALLEN -> fallenTransmat(c, radius, p, alpha)
        }
        // Every type ends with a faint energy-dispersal halo, not a second
        // gameplay event and not an additional card frame.
        val finalPulse = ((p - .50f) / .38f).coerceIn(0f, 1f)
        if (finalPulse > 0f) {
            val tone = when (style) {
                DuelTransmatStyle.DEFAULT -> Color(0xFFC3E8FF)
                DuelTransmatStyle.HIVE -> Color(0xFFB4DD68)
                DuelTransmatStyle.CABAL -> Color(0xFFFFA868)
                DuelTransmatStyle.VEX -> Color(0xFFFFD17C)
                DuelTransmatStyle.FALLEN -> Color(0xFF7AEFFF)
            }
            drawCircle(
                color = tone.copy(alpha = alpha * .24f * (1f - finalPulse)),
                radius = radius * (1f + .60f * finalPulse),
                center = c,
                style = Stroke(width = 1.25.dp.toPx())
            )
        }
    }
}

private fun DrawScope.energyBloom(c: Offset, r: Float, tint: Color, alpha: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                tint.copy(alpha = alpha * .43f),
                tint.copy(alpha = alpha * .13f),
                tint.copy(alpha = 0f)
            ),
            center = c, radius = r * 1.7f
        ),
        radius = r * 1.7f, center = c
    )
}

private fun DrawScope.energyRing(
    c: Offset, r: Float, tint: Color, opacity: Float,
    width: Float = 1.4f
) {
    drawCircle(
        color = tint.copy(alpha = opacity.coerceIn(0f, 1f)),
        radius = r, center = c,
        style = Stroke(width = width.dp.toPx())
    )
}

private fun DrawScope.energyBeam(
    c: Offset, height: Float, width: Float, tint: Color, alpha: Float
) {
    drawRect(
        brush = Brush.horizontalGradient(
            listOf(
                tint.copy(alpha = 0f),
                tint.copy(alpha = alpha * .17f),
                Color.White.copy(alpha = alpha * .48f),
                tint.copy(alpha = alpha * .17f),
                tint.copy(alpha = 0f)
            ),
            startX = c.x - width, endX = c.x + width
        ),
        topLeft = Offset(c.x - width, c.y - height * .66f),
        size = Size(width * 2f, height)
    )
}

private fun DrawScope.sparks(
    c: Offset, r: Float, tint: Color, alpha: Float,
    count: Int, progress: Float, angularShift: Float = 0f
) {
    for (i in 0 until count) {
        val angle = 2.0 * PI * i / count + angularShift
        val dx = cos(angle).toFloat()
        val dy = sin(angle).toFloat()
        val travel = .34f + progress * (if (i % 3 == 0) .90f else .58f)
        val point = c + Offset(dx * r * travel, dy * r * travel)
        drawCircle(
            color = tint.copy(alpha = alpha * (if (i % 4 == 0) .9f else .53f)),
            radius = (if (i % 4 == 0) 1.25f else .75f).dp.toPx(),
            center = point
        )
    }
}

private fun DrawScope.defaultTransmat(
    c: Offset, r: Float, p: Float, a: Float
) {
    // Guardian/default: clean white-blue column, circular energy tracers,
    // orderly fragments. Shared by Humans, Exos and Awoken.
    val blue = Color(0xFF9BD5FF)
    energyBloom(c, r, blue, a)
    energyBeam(c, size.height * .99f, r * .66f, blue, a)
    energyRing(c, r * (.42f + p * .25f), Color(0xFFDAEFFF), a * .85f)
    energyRing(c, r * (.68f + p * .26f), blue, a * .57f, .8f)
    drawLine(
        Color.White.copy(alpha = a * .68f),
        Offset(c.x, c.y - r * 1.22f), Offset(c.x, c.y + r * .9f),
        .9.dp.toPx()
    )
    for (i in 0..3) {
        val y = c.y - r * .7f + i * r * .42f
        drawLine(
            blue.copy(alpha = a * .39f),
            Offset(c.x - r * .52f, y),
            Offset(c.x + r * .52f, y),
            .7.dp.toPx()
        )
    }
    sparks(c, r, Color(0xFFEEF8FF), a, 12, p)
}

private fun DrawScope.hiveTransmat(
    c: Offset, r: Float, p: Float, a: Float
) {
    // Hive: verdant ritual aperture, asymmetric thorn runes and rising ash.
    val green = Color(0xFFB4DE72)
    val dark = Color(0xFF436F36)
    energyBloom(c, r, green, a * .88f)
    energyRing(c, r * (.52f + p * .18f), green, a * .68f)
    energyRing(c, r * .82f, dark, a * .75f, .95f)
    for (i in 0 until 7) {
        val angle = 2.0 * PI * i / 7 - PI / 2 + p * .60
        val dx = cos(angle).toFloat()
        val dy = sin(angle).toFloat()
        val base = c + Offset(dx * r * .60f, dy * r * .60f)
        val tip = c + Offset(dx * r * (.95f + .13f * p), dy * r * (.95f + .13f * p))
        val sideways = Offset(-dy * r * .10f, dx * r * .10f)
        val thorn = Path().apply {
            moveTo(base.x - sideways.x, base.y - sideways.y)
            lineTo(tip.x, tip.y)
            lineTo(base.x + sideways.x, base.y + sideways.y)
        }
        drawPath(thorn, green.copy(alpha = a * .78f), style = Stroke(1.25.dp.toPx()))
    }
    for (i in 0..8) {
        val x = c.x + (sin(i.toDouble() * 2.15).toFloat()) * r * .72f
        val drift = (p * (1f + (i % 3) * .26f))
        val y = c.y + r * .65f - r * (1.6f * drift + i * .075f)
        drawCircle(
            dark.copy(alpha = a * (.24f + (i % 4) * .10f)),
            r * (if (i % 3 == 0) .11f else .065f),
            Offset(x, y)
        )
        drawCircle(
            green.copy(alpha = a * .66f),
            .9.dp.toPx(), Offset(x, y - r * .04f)
        )
    }
    sparks(c, r, green, a * .72f, 10, p, .23f)
}

private fun DrawScope.cabalTransmat(
    c: Offset, r: Float, p: Float, a: Float
) {
    // Cabal: heavy red-orange drop aperture, armored struts and dust blast.
    val copper = Color(0xFFFFA261)
    val ember = Color(0xFFE8663F)
    energyBloom(c, r, ember, a * .9f)
    energyBeam(c, size.height * .85f, r * .90f, copper, a * .58f)
    energyRing(c, r * (.43f + .52f * p), ember, a * .91f, 2.45f)
    energyRing(c, r * .67f, copper, a * .68f, 1.5f)
    for (i in 0..3) {
        val side = if (i % 2 == 0) -1f else 1f
        val yy = c.y + (i / 2f - .5f) * r * .70f
        val cut = Path().apply {
            moveTo(c.x + side * r * .50f, yy - r * .17f)
            lineTo(c.x + side * r * .78f, yy)
            lineTo(c.x + side * r * .50f, yy + r * .17f)
        }
        drawPath(
            cut, copper.copy(alpha = a * .86f),
            style = Stroke(width = 2.0.dp.toPx())
        )
    }
    for (i in 0 until 15) {
        val x = c.x + sin(i * 2.33).toFloat() * r * (0.15f + p * 1.1f)
        val y = c.y + r * .50f + p * r * (.22f + (i % 4) * .10f)
        drawCircle(
            color = copper.copy(alpha = a * .53f),
            radius = (if (i % 3 == 0) 1.6f else 1.0f).dp.toPx(),
            center = Offset(x, y)
        )
    }
    sparks(c, r, ember, a, 11, p, .16f)
}

private fun DrawScope.vexTransmat(
    c: Offset, r: Float, p: Float, a: Float
) {
    // Vex: luminous amber geometric gate, machine lattice and datashards.
    val gold = Color(0xFFFFD078)
    val bright = Color(0xFFFFEAC2)
    energyBloom(c, r, gold, a)
    energyBeam(c, size.height * .93f, r * .60f, gold, a * .64f)
    for (index in 0..1) {
        val phase = p * .38f * if (index == 0) 1f else -1f
        val radius = r * (if (index == 0) .65f else .90f)
        val shape = Path()
        for (i in 0..6) {
            val angle = (2.0 * PI * i / 6 - PI / 2) + phase
            val pos = c + Offset(
                cos(angle).toFloat() * radius,
                sin(angle).toFloat() * radius
            )
            if (i == 0) shape.moveTo(pos.x, pos.y)
            else shape.lineTo(pos.x, pos.y)
        }
        drawPath(
            shape, gold.copy(alpha = a * (if (index == 0) .83f else .48f)),
            style = Stroke(width = if (index == 0) 1.55.dp.toPx() else .85.dp.toPx())
        )
    }
    val scanY = c.y - r + 2f * r * p
    drawLine(
        bright.copy(alpha = a * .83f),
        Offset(c.x - r * .69f, scanY), Offset(c.x + r * .69f, scanY),
        1.15.dp.toPx()
    )
    for (i in 0..7) {
        val dx = (i % 4 - 1.5f) * r * .35f
        val dy = (i / 4 - .5f) * r * .57f
        val q = c + Offset(dx, dy)
        drawRect(
            color = bright.copy(alpha = a * (.36f + (i % 3) * .13f)),
            topLeft = q, size = Size(r * .15f, r * .07f)
        )
    }
    sparks(c, r, gold, a * .87f, 12, p, .10f)
}

private fun DrawScope.fallenTransmat(
    c: Offset, r: Float, p: Float, a: Float
) {
    // Fallen: crackling cyan Arc/Ether teleport, branching lightning bolts.
    val arc = Color(0xFF6DE5FF)
    val electric = Color(0xFFA9F7FF)
    energyBloom(c, r, arc, a * .94f)
    energyRing(c, r * (.45f + p * .48f), arc, a * .55f)
    for (i in 0..5) {
        val angle = 2.0 * PI * i / 6 + .38
        val ux = cos(angle).toFloat()
        val uy = sin(angle).toFloat()
        val tx = -uy
        val ty = ux
        val length = r * (.57f + (i % 3) * .14f)
        val path = Path().apply {
            moveTo(c.x, c.y - r * .05f)
            lineTo(c.x + ux * length * .27f + tx * r * .14f,
                c.y + uy * length * .27f + ty * r * .14f)
            lineTo(c.x + ux * length * .48f - tx * r * .12f,
                c.y + uy * length * .48f - ty * r * .12f)
            lineTo(c.x + ux * length * .74f + tx * r * .10f,
                c.y + uy * length * .74f + ty * r * .10f)
            lineTo(c.x + ux * length,
                c.y + uy * length)
        }
        drawPath(
            path, arc.copy(alpha = a * (.56f + (i % 3) * .10f)),
            style = Stroke(width = (if (i % 2 == 0) 1.6f else 1.0f).dp.toPx())
        )
        val node = c + Offset(ux * length * .70f, uy * length * .70f)
        drawCircle(electric.copy(alpha = a * .88f), .95.dp.toPx(), node)
    }
    for (i in 0..11) {
        val x = c.x + cos(i * 1.7).toFloat() * r * (.35f + p * .47f)
        val y = c.y + sin(i * 2.3).toFloat() * r * (.22f + p * .62f)
        drawCircle(
            electric.copy(alpha = a * .55f),
            .8.dp.toPx(), Offset(x, y)
        )
    }
    sparks(c, r, arc, a * .80f, 13, p, -.14f)
}
