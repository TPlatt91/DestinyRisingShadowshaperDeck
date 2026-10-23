package com.tplatt.shadowshaper

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.Shader
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Static decorative scenery ONLY. All geometry is painted offscreen once,
 * then composited underneath the real ShadowEngine card grid/HUD.
 *
 * Based on the user's original Destiny: Rising reference: worn slate blocks,
 * asymmetrical broken masonry, yellow-green ferns, moss, leaf litter, and
 * recessed shadows framing a relatively clean, pale-gray battle surface.
 *
 * Never includes screenshots, fake cards, scores, hitboxes, or tap targets.
 */
internal object DuelArenaScenery {
    private const val W = 640f
    private const val H = 1024f

    private val charcoal = AColor.rgb(34, 39, 39)
    private val mortar = AColor.rgb(65, 70, 68)
    private val slate = AColor.rgb(92, 98, 94)
    private val olive = intArrayOf(
        AColor.rgb(54, 68, 43),
        AColor.rgb(75, 91, 52),
        AColor.rgb(95, 108, 62),
        AColor.rgb(119, 125, 69),
        AColor.rgb(147, 143, 80),
        AColor.rgb(176, 160, 97)
    )

    private fun tone(color: Int, shift: Int): Int = AColor.rgb(
        (AColor.red(color) + shift).coerceIn(0, 255),
        (AColor.green(color) + shift).coerceIn(0, 255),
        (AColor.blue(color) + shift).coerceIn(0, 255)
    )

    private fun rgba(color: Int, opacity: Int): Int =
        AColor.argb(
            opacity.coerceIn(0, 255),
            AColor.red(color), AColor.green(color), AColor.blue(color)
        )

    private fun shape(points: List<Pair<Float, Float>>): AndroidPath =
        AndroidPath().apply {
            if (points.isNotEmpty()) {
                moveTo(points.first().first, points.first().second)
                for (i in 1 until points.size) lineTo(points[i].first, points[i].second)
                close()
            }
        }

    private fun polygon(
        canvas: AndroidCanvas,
        paint: Paint,
        points: List<Pair<Float, Float>>,
        color: Int
    ) {
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.color = color
        canvas.drawPath(shape(points), paint)
    }

    private fun stroke(
        canvas: AndroidCanvas, paint: Paint, path: AndroidPath,
        color: Int, width: Float
    ) {
        paint.shader = null
        paint.color = color
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = width
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL
    }

    /**
     * Each rock has a different irregular silhouette, cast shadow, edge lip,
     * three or more planar facets, internal fracture and chipped highlights.
     */
    private fun rock(
        canvas: AndroidCanvas,
        paint: Paint,
        cx: Float, cy: Float, rx: Float, ry: Float,
        seed: Int, base: Int = slate, opacity: Int = 255
    ) {
        val random = Random(seed)
        val sides = 16 + random.nextInt(5)
        val outline = (0 until sides).map { i ->
            val angle = 2.0 * PI * i / sides - PI / 2
            val jag = random.nextFloat() * 0.19f + 0.85f
            (cx + cos(angle).toFloat() * rx * jag) to
                (cy + sin(angle).toFloat() * ry * jag)
        }
        val path = shape(outline)

        canvas.save()
        canvas.translate(rx * .12f + 2f, ry * .17f + 3f)
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.color = rgba(charcoal, (opacity * .75f).toInt())
        canvas.drawPath(path, paint)
        canvas.restore()

        paint.shader = LinearGradient(
            cx - rx * .7f, cy - ry,
            cx + rx * .6f, cy + ry,
            intArrayOf(
                rgba(tone(base, 45), opacity),
                rgba(tone(base, 3), opacity),
                rgba(tone(base, -49), opacity)
            ),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawPath(path, paint)
        paint.shader = null

        // Very low-contrast cleavage planes: the previous triangular fan
        // made boulders read like brightly colored low-poly crystals.
        val core = cx + (random.nextFloat() - .5f) * rx * .19f to
            cy + (random.nextFloat() - .5f) * ry * .16f
        for (i in outline.indices) {
            val next = outline[(i + 1) % outline.size]
            val facetColor = when (i % 4) {
                0 -> rgba(tone(base, 29), (opacity * .18f).toInt())
                1 -> rgba(tone(base, 14), (opacity * .13f).toInt())
                2 -> rgba(tone(base, -26), (opacity * .22f).toInt())
                else -> rgba(tone(base, -10), (opacity * .14f).toInt())
            }
            polygon(canvas, paint, listOf(core, outline[i], next), facetColor)
        }

        // Thin sunlit upper bevel, weathered bottom lip.
        val lit = AndroidPath().apply {
            moveTo(outline[0].first, outline[0].second)
            lineTo(outline[1].first, outline[1].second)
            lineTo(outline[2].first, outline[2].second)
        }
        stroke(canvas, paint, lit, rgba(tone(base, 95), (opacity * .65f).toInt()), 1.5f)
        val bottom = AndroidPath().apply {
            moveTo(outline[4].first, outline[4].second)
            lineTo(outline[5].first, outline[5].second)
            lineTo(outline[6].first, outline[6].second)
        }
        stroke(canvas, paint, bottom, rgba(charcoal, (opacity * .64f).toInt()), 2.1f)

        for (n in 0 until 2 + random.nextInt(3)) {
            val x = cx + (random.nextFloat() - .5f) * rx
            val y = cy + (random.nextFloat() - .5f) * ry
            val crack = AndroidPath().apply {
                moveTo(x, y)
                lineTo(x + 2f + random.nextFloat() * rx * .22f,
                    y + ry * .08f)
                lineTo(x + random.nextFloat() * rx * .26f,
                    y + ry * .14f + 2f)
            }
            stroke(canvas, paint, crack, rgba(charcoal, (opacity * .37f).toInt()),
                .65f + random.nextFloat() * .7f)
        }

        // Dense lichen/mineral pores clipped to each rock silhouette.
        // Fine physical-looking abrasion is more faithful than flat facets.
        canvas.save()
        canvas.clipPath(path)
        repeat(95) {
            val x = cx + (random.nextFloat() - .5f) * rx * 2.05f
            val y = cy + (random.nextFloat() - .5f) * ry * 2.04f
            paint.shader = null
            paint.color = rgba(
                if (random.nextInt(5) == 0) tone(base, 67) else
                    if (random.nextBoolean()) tone(base, -64) else tone(base, 29),
                random.nextInt(21, 79)
            )
            canvas.drawCircle(
                x, y, .28f + random.nextFloat() * 1.45f, paint
            )
        }
        // A few tiny weathering streaks, not large geometric triangles.
        repeat(8) {
            val px = cx + (random.nextFloat() - .5f) * rx * 1.64f
            val py = cy + (random.nextFloat() - .5f) * ry * 1.72f
            stroke(
                canvas, paint,
                AndroidPath().apply {
                    moveTo(px, py)
                    lineTo(px + random.nextFloat() * 4.2f,
                        py + 2.5f + random.nextFloat() * 4f)
                },
                rgba(tone(base, 76), random.nextInt(34, 82)), .65f
            )
        }
        canvas.restore()
    }

    private fun leaf(
        canvas: AndroidCanvas, paint: Paint,
        x: Float, y: Float, dx: Float, dy: Float,
        length: Float, width: Float, color: Int,
        opacity: Int = 240
    ) {
        val endX = x + dx * length
        val endY = y + dy * length
        val px = -dy * width
        val py = dx * width
        val leafPath = AndroidPath().apply {
            moveTo(x, y)
            cubicTo(x + px * .8f, y + py * .8f,
                endX - dx * length * .18f + px, endY - dy * length * .18f + py,
                endX, endY)
            cubicTo(endX - dx * length * .17f - px,
                endY - dy * length * .17f - py,
                x - px * .9f, y - py * .9f, x, y)
            close()
        }
        paint.shader = LinearGradient(
            x, y, endX, endY,
            rgba(tone(color, -16), opacity),
            rgba(tone(color, 27), opacity),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(leafPath, paint)
        paint.shader = null

        val vein = AndroidPath().apply {
            moveTo(x, y)
            quadTo(
                x + dx * length * .55f,
                y + dy * length * .55f,
                endX - dx * length * .10f,
                endY - dy * length * .10f
            )
        }
        stroke(canvas, paint, vein, rgba(tone(color, 62), opacity / 2), .62f)
    }

    /** Curved fern stem with individually tapered overlapping leaflets. */
    private fun fern(
        canvas: AndroidCanvas, paint: Paint,
        originX: Float, originY: Float,
        stemLength: Float, angle: Float,
        seed: Int, palette: Int = 0
    ) {
        val rnd = Random(seed)
        val dx = cos(angle).toFloat()
        val dy = sin(angle).toFloat()
        val sideX = -dy
        val sideY = dx
        val bend = if (rnd.nextBoolean()) 1f else -1f
        val stem = AndroidPath().apply {
            moveTo(originX, originY)
            cubicTo(
                originX + dx * stemLength * .26f,
                originY + dy * stemLength * .26f,
                originX + dx * stemLength * .60f + sideX * bend * 10f,
                originY + dy * stemLength * .60f + sideY * bend * 10f,
                originX + dx * stemLength + sideX * bend * 12f,
                originY + dy * stemLength + sideY * bend * 12f
            )
        }
        stroke(canvas, paint, stem, rgba(AColor.rgb(36, 49, 31), 230), 2f)
        for (i in 1..10) {
            val t = i / 11f
            val widthAt = (1f - t * .72f) * stemLength * .26f
            val bx = originX + dx * stemLength * t +
                sideX * bend * (t * t) * 12f
            val by = originY + dy * stemLength * t +
                sideY * bend * (t * t) * 12f
            val forward = .38f + .25f * t
            for (side in listOf(-1, 1)) {
                val vx = dx * forward + sideX * side * .90f
                val vy = dy * forward + sideY * side * .90f
                val mag = kotlin.math.sqrt(vx * vx + vy * vy)
                val tint = olive[(palette + i + if (side > 0) 1 else 0) % olive.size]
                leaf(
                    canvas, paint, bx, by,
                    vx / mag, vy / mag,
                    widthAt * (0.82f + rnd.nextFloat() * .23f),
                    widthAt * .23f, tint
                )
            }
        }
    }

    /** Five-lobed ivy and buried vine roots in broken-stone crevices. */
    private fun vine(
        canvas: AndroidCanvas, paint: Paint,
        x: Float, y: Float, extent: Float,
        angle: Float, seed: Int
    ) {
        val rnd = Random(seed)
        val dx = cos(angle).toFloat()
        val dy = sin(angle).toFloat()
        val perpX = -dy
        val perpY = dx
        val spine = AndroidPath().apply {
            moveTo(x, y)
            cubicTo(
                x + dx * extent * .35f - perpX * extent * .16f,
                y + dy * extent * .35f - perpY * extent * .16f,
                x + dx * extent * .72f + perpX * extent * .12f,
                y + dy * extent * .72f + perpY * extent * .12f,
                x + dx * extent, y + dy * extent
            )
        }
        stroke(canvas, paint, spine, rgba(AColor.rgb(47, 59, 38), 218), 2.3f)
        for (k in 1..7) {
            val t = k / 8f
            val side = if (k % 2 == 0) 1 else -1
            val bx = x + dx * extent * t + perpX * extent * .06f * sin(t.toDouble() * PI).toFloat()
            val by = y + dy * extent * t + perpY * extent * .06f * sin(t * PI).toFloat()
            val dirX = dx * .4f + perpX * side * .91f
            val dirY = dy * .4f + perpY * side * .91f
            leaf(
                canvas, paint, bx, by, dirX, dirY,
                12f + rnd.nextFloat() * 14f,
                5f + rnd.nextFloat() * 3f,
                olive[(k + seed) % olive.size],
                215
            )
        }
    }

    private fun moss(
        canvas: AndroidCanvas, paint: Paint,
        cx: Float, cy: Float,
        rx: Float, ry: Float, seed: Int, density: Int
    ) {
        val rnd = Random(seed)
        // Dark damp undergrowth along the fractured edge.
        paint.color = rgba(AColor.rgb(29, 40, 29), 128)
        canvas.drawOval(
            cx - rx * .75f, cy - ry * .70f,
            cx + rx * .75f, cy + ry * .70f, paint
        )
        repeat(density) {
            val u = rnd.nextFloat() * 2f - 1f
            val v = rnd.nextFloat() * 2f - 1f
            if (u * u + v * v > 1f) return@repeat
            val x = cx + u * rx
            val y = cy + v * ry
            val c = olive[rnd.nextInt(olive.size)]
            paint.color = rgba(c, 74 + rnd.nextInt(111))
            canvas.drawOval(
                x, y, x + 1f + rnd.nextFloat() * 5f,
                y + 1f + rnd.nextFloat() * 3f, paint
            )
            if (rnd.nextInt(9) == 0) {
                paint.color = rgba(tone(c, 32), 125)
                canvas.drawCircle(x + 1.2f, y - .5f, .8f, paint)
            }
        }
    }

    /** Dirt, chips and leaf litter are biased toward edges; never a fake grid. */
    private fun litter(canvas: AndroidCanvas, paint: Paint) {
        val rnd = Random(552840)
        repeat(480) {
            val left = rnd.nextBoolean()
            val edgeDistance = rnd.nextFloat()
            val x = if (left) edgeDistance * 105f else W - edgeDistance * 85f
            val y = rnd.nextFloat() * H
            val rx = .5f + rnd.nextFloat() * 4.5f
            val ry = .4f + rnd.nextFloat() * 2.5f
            paint.shader = null
            paint.color = rgba(
                if (rnd.nextInt(6) == 0) olive[rnd.nextInt(olive.size)]
                else if (rnd.nextBoolean()) AColor.rgb(184, 179, 159)
                else AColor.rgb(35, 37, 37),
                ((20 + rnd.nextInt(112)) *
                    (if (edgeDistance > .8f) .55f else 1f)).toInt()
            )
            canvas.save()
            canvas.rotate(rnd.nextFloat() * 140f, x, y)
            canvas.drawOval(x - rx, y - ry, x + rx, y + ry, paint)
            canvas.restore()
        }
    }

    private fun render(): Bitmap {
        val bitmap = Bitmap.createBitmap(
            W.toInt(), H.toInt(), Bitmap.Config.ARGB_8888
        )
        bitmap.eraseColor(AColor.TRANSPARENT)
        val canvas = AndroidCanvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
            style = Paint.Style.FILL
        }

        // First: distant cliff shadow and dark seams behind loose rock.
        val leftGutter = AndroidPath().apply {
            moveTo(0f, 0f)
            lineTo(63f, 0f)
            lineTo(46f, 135f)
            lineTo(67f, 278f)
            lineTo(42f, 410f)
            lineTo(74f, 562f)
            lineTo(43f, 715f)
            lineTo(59f, 889f)
            lineTo(45f, H)
            lineTo(0f, H)
            close()
        }
        polygon(canvas, paint,
            listOf(
                0f to 0f, 61f to 0f, 49f to 217f, 70f to 382f,
                44f to 561f, 67f to 758f, 46f to H, 0f to H
            ),
            rgba(charcoal, 155)
        )
        paint.shader = null
        paint.color = rgba(charcoal, 140)
        canvas.drawPath(leftGutter, paint)
        polygon(
            canvas, paint,
            listOf(
                595f to 0f, W to 0f, W to H, 593f to H,
                568f to 847f, 598f to 684f,
                565f to 458f, 596f to 280f, 573f to 101f
            ),
            rgba(charcoal, 180)
        )

        // Deep right masonry: original reference has pronounced rock faces
        // at right/top. Some shapes deliberately clip outside the viewport.
        val rightBlocks = listOf(
            floatArrayOf(632f, 23f, 64f, 77f),
            floatArrayOf(613f, 133f, 69f, 102f),
            floatArrayOf(629f, 269f, 75f, 111f),
            floatArrayOf(611f, 422f, 62f, 100f),
            floatArrayOf(629f, 579f, 76f, 106f),
            floatArrayOf(618f, 748f, 64f, 120f),
            floatArrayOf(639f, 941f, 87f, 119f)
        )
        rightBlocks.forEachIndexed { i, item ->
            rock(canvas, paint, item[0], item[1], item[2], item[3],
                311 + i * 31, if (i % 2 == 0) slate else mortar)
        }

        // Left wall is narrower, with irregular fallen chunks and fractured
        // layers instead of a symmetric black vertical stroke.
        val leftBlocks = listOf(
            floatArrayOf(4f, 15f, 60f, 91f),
            floatArrayOf(31f, 142f, 64f, 97f),
            floatArrayOf(9f, 274f, 66f, 107f),
            floatArrayOf(20f, 443f, 73f, 124f),
            floatArrayOf(2f, 633f, 76f, 143f),
            floatArrayOf(21f, 844f, 63f, 107f),
            floatArrayOf(4f, 1015f, 84f, 91f)
        )
        leftBlocks.forEachIndexed { i, item ->
            rock(canvas, paint, item[0], item[1], item[2], item[3],
                205 + i * 29, if (i % 3 == 0) mortar else slate,
                if (i == 3 || i == 4) 234 else 250)
        }

        // Rubble heaps at lower edges: large irregular shards, then chips.
        for (i in 0 until 23) {
            val rng = Random(444 + i)
            val fromLeft = i < 15
            val x = if (fromLeft) rng.nextFloat() * 125f - 23f
                else W + 25f - rng.nextFloat() * 109f
            val y = when {
                i < 11 -> 560f + rng.nextFloat() * 190f
                i < 18 -> 807f + rng.nextFloat() * 226f
                else -> rng.nextFloat() * 192f
            }
            rock(
                canvas, paint, x, y,
                9f + rng.nextFloat() * 26f,
                8f + rng.nextFloat() * 27f,
                900 + i * 17,
                if (i % 4 == 0) mortar else slate,
                175 + rng.nextInt(66)
            )
        }

        // Green-black moss attaches to recessed masonry, particularly the
        // left lower side visible in the original reference.
        moss(canvas, paint, 28f, 592f, 84f, 132f, 610, 340)
        moss(canvas, paint, 48f, 739f, 85f, 119f, 614, 320)
        moss(canvas, paint, 41f, 399f, 55f, 69f, 628, 135)
        moss(canvas, paint, 605f, 318f, 37f, 83f, 631, 135)
        moss(canvas, paint, 611f, 827f, 43f, 83f, 647, 165)

        // Worn climbing vines along masonry joints.
        vine(canvas, paint, 0f, 690f, 161f, -1.18f, 7)
        vine(canvas, paint, 18f, 753f, 118f, -.83f, 11)
        vine(canvas, paint, 8f, 559f, 86f, -.73f, 15)
        vine(canvas, paint, 621f, 390f, 87f, -2.32f, 19)
        vine(canvas, paint, 636f, 854f, 72f, -2.74f, 25)

        // Fern leaves hang *toward* the board but stop at the outer edge
        // of real card lanes; asymmetric cluster is intentional.
        val ferns = listOf(
            floatArrayOf(4f, 776f, 105f, -1.19f),
            floatArrayOf(3f, 675f, 131f, -.90f),
            floatArrayOf(29f, 697f, 98f, -1.74f),
            floatArrayOf(8f, 586f, 87f, -.56f),
            floatArrayOf(0f, 848f, 84f, -1.41f),
            floatArrayOf(41f, 721f, 91f, -1.04f),
            floatArrayOf(20f, 477f, 52f, -1.11f),
            floatArrayOf(627f, 320f, 46f, -2.15f),
            floatArrayOf(646f, 758f, 59f, -2.36f)
        )
        ferns.forEachIndexed { i, item ->
            fern(canvas, paint, item[0], item[1], item[2], item[3],
                730 + i * 31, i % 5)
        }

        // A few dead ocher leaves caught in rubble / muted grass.
        val leafRng = Random(9017)
        repeat(45) { i ->
            val x = if (i < 35) leafRng.nextFloat() * 128f - 12f
                else W - leafRng.nextFloat() * 84f
            val y = 390f + leafRng.nextFloat() * 622f
            val angle = leafRng.nextFloat() * 2f * PI.toFloat()
            leaf(
                canvas, paint, x, y,
                cos(angle), sin(angle),
                6f + leafRng.nextFloat() * 9f,
                2.4f + leafRng.nextFloat() * 2.2f,
                if (i % 3 == 0) olive[4] else olive[2],
                128 + leafRng.nextInt(85)
            )
        }

        // Last: mineral flakes, soil and grit unify environmental layers.
        litter(canvas, paint)
        return bitmap
    }

    val illustration: Bitmap by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        render()
    }
}

/** Decorative-only Image. Does not add any Compose click/semantics surface. */
@Composable
internal fun DuelArenaEnvironment(modifier: Modifier = Modifier) {
    val bitmap = remember { DuelArenaScenery.illustration.asImageBitmap() }
    Image(
        bitmap = bitmap, contentDescription = null,
        modifier = modifier, contentScale = ContentScale.FillBounds
    )
}
