package com.tplatt.shadowshaper

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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

    @JvmStatic
    fun bind(activity: Activity, card: ShadowData.Card, view: ImageView) {
        val id = slug(card.name)
        view.tag = id
        view.contentDescription = card.name + " card art"
        view.setImageResource(android.R.color.transparent)

        memory[id]?.let {
            view.setImageBitmap(it)
            return
        }
        val context = activity.applicationContext
        workers.execute {
            var bitmap: Bitmap? = null
            for (extension in listOf("webp", "png", "jpg", "jpeg")) {
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
