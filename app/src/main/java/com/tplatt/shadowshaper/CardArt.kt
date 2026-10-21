package com.tplatt.shadowshaper

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ImageView
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Kotlin original-card-face loader. Local card_art images take priority.
 * Never replaces a missing card face with unrelated Destiny art.
 */
object CardArt {
    private val workers = Executors.newFixedThreadPool(3)
    private val memory = ConcurrentHashMap<String, Bitmap>()

    private fun slug(name: String): String =
        name.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')

    /**
     * Reference card back from the Shadowshaper Covers gallery on TodayInDestiny.
     * A bundled card_cover/default.webp overrides online access. Remote art is
     * cached to app-private storage and never fetched on the main thread.
     * The Compose cover component supplies its own offline placeholder frame.
     */
    private val coverLock = Any()

    /**
     * Covers verified at TodayInDestiny's Shadowshaper gallery. The original
     * Default cover has a local asset override and cached-network fallback.
     * A single loading lock prevents multiple opponent cards downloading
     * or corrupting the same on-disk image concurrently.
     */
    @JvmStatic
    fun bindCover(activity: Activity, view: ImageView, name: String = "default") {
        val id = slug(name)
        if (id !in setOf("default", "denzin", "elite_vandal", "marauder",
                "morgran", "sniper_vandal", "tracer_shank", "wolf", "zhong_wenbin")) {
            return
        }
        val memoryId = "cover_$id"
        view.tag = memoryId
        view.contentDescription = "$name Shadowshaper card back"
        view.setImageResource(android.R.color.transparent)
        memory[memoryId]?.let { image ->
            view.setImageBitmap(image)
            return
        }
        val context = activity.applicationContext
        workers.execute {
            val image = synchronized(coverLock) {
                memory[memoryId] ?: run {
                    var loaded: Bitmap? = try {
                        context.assets.open("card_cover/$id.webp").use(BitmapFactory::decodeStream)
                    } catch (_: Exception) { null }
                    if (loaded == null) {
                        val folder = File(context.cacheDir, "shadowshaper_covers")
                        folder.mkdirs()
                        val cache = File(folder, "$id.webp")
                        try {
                            if (cache.isFile)
                                loaded = BitmapFactory.decodeFile(cache.absolutePath)
                            if (loaded == null) {
                                val url = URL(
                                    "https://www.media.todayindestiny.com/images/rising/shadowshaper_duels/cover/$id.webp"
                                )
                                val connection = url.openConnection() as HttpURLConnection
                                connection.connectTimeout = 6000
                                connection.readTimeout = 9000
                                try {
                                    if (connection.responseCode == HttpURLConnection.HTTP_OK &&
                                        connection.contentType?.startsWith("image/") == true
                                    ) {
                                        connection.inputStream.use { input ->
                                            cache.outputStream().use { output -> input.copyTo(output) }
                                        }
                                        loaded = BitmapFactory.decodeFile(cache.absolutePath)
                                    }
                                } finally {
                                    connection.disconnect()
                                }
                                if (loaded == null) cache.delete()
                            }
                        } catch (_: Exception) {
                            // Offline or missing image: Compose placeholder remains.
                        }
                    }
                    if (loaded != null) memory[memoryId] = loaded
                    loaded
                }
            }
            if (image != null) {
                activity.runOnUiThread {
                    if (view.tag == memoryId && !activity.isFinishing)
                        view.setImageBitmap(image)
                }
            }
        }
    }

    @JvmStatic
    fun bind(activity: Activity, card: ShadowData.Card, view: ImageView) {
        val id = slug(card.name)
        // Keep an already-running local GIF playing through Compose recompositions.
        if (view.tag == id && view.drawable is Animatable) return
        (view.drawable as? Animatable)?.stop()
        view.tag = id
        view.contentDescription = card.name + " card art"
        view.setImageResource(android.R.color.transparent)

        // Only static artwork is shared as a Bitmap. AnimatedImageDrawable is
        // deliberately decoded per ImageView so animations do not share state.
        val context = activity.applicationContext
        workers.execute {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val animated: Drawable? = try {
                    ImageDecoder.decodeDrawable(
                        ImageDecoder.createSource(context.assets, "card_art/$id.gif")
                    )
                } catch (_: Exception) {
                    null
                }
                if (animated != null) {
                    activity.runOnUiThread {
                        if (view.tag == id && !activity.isFinishing) {
                            (view.drawable as? Animatable)?.stop()
                            view.setImageDrawable(animated)
                            (animated as? Animatable)?.start()
                        }
                    }
                    return@execute
                }
            }

            memory[id]?.let { cached ->
                activity.runOnUiThread {
                    if (view.tag == id && !activity.isFinishing) view.setImageBitmap(cached)
                }
                return@execute
            }
            var bitmap: Bitmap? = null
            // On Android 8-8.1, GIFs gracefully display their first frame.
            // Android 9+ instead plays the complete animation above.
            for (extension in listOf("gif", "webp", "png", "jpg", "jpeg")) {
                val path = "card_art/$id.$extension"
                bitmap = try {
                    context.assets.open(path).use(BitmapFactory::decodeStream)
                } catch (_: Exception) {
                    null
                }
                if (bitmap != null) break
            }

            if (bitmap == null && card.onlineArt) {
                val folder = File(context.cacheDir, "shadowshaper_cards")
                folder.mkdirs()
                val cache = File(folder, "$id.webp")
                try {
                    if (cache.isFile) bitmap = BitmapFactory.decodeFile(cache.absolutePath)
                    if (bitmap == null) {
                        val url = URL(
                            "https://www.media.todayindestiny.com/images/rising/shadowshaper_duels/card/$id.webp"
                        )
                        val connection = url.openConnection() as HttpURLConnection
                        connection.connectTimeout = 6000
                        connection.readTimeout = 9000
                        try {
                            if (connection.responseCode == HttpURLConnection.HTTP_OK &&
                                connection.contentType?.startsWith("image/") == true
                            ) {
                                connection.inputStream.use { input ->
                                    cache.outputStream().use { output -> input.copyTo(output) }
                                }
                                bitmap = BitmapFactory.decodeFile(cache.absolutePath)
                            }
                        } finally {
                            connection.disconnect()
                        }
                        if (bitmap == null) cache.delete()
                    }
                } catch (_: Exception) {
                    // A missing or inaccessible remote image must not block card details.
                }
            }

            if (bitmap != null) {
                val finishedImage = bitmap
                memory[id] = finishedImage
                activity.runOnUiThread {
                    if (view.tag == id && !activity.isFinishing) {
                        view.setImageBitmap(finishedImage)
                    }
                }
            }
        }
    }
}
