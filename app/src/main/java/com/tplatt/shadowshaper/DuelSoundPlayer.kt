package com.tplatt.shadowshaper

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * Low-latency mobile game sound player. Creates deterministic original WAVs in
 * app-private cache on Dispatchers.IO and plays them with SoundPool.
 *
 * Optional replacement: add assets/shadow_sfx/<name>.ogg or .wav. User-supplied
 * clean clips override generated audio without changing battle event mapping.
 *
 * No external audio download, runtime permissions, streaming, or network.
 */
internal class DuelSoundPlayer(context: Context) {
    private val app = context.applicationContext
    private val preferences = app.getSharedPreferences("shadow_duel_audio", Context.MODE_PRIVATE)
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    private val pool: SoundPool? = try {
        SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(attributes)
            .build()
    } catch (_: Exception) {
        // If the device's audio subsystem is unavailable, retain silent
        // functionality rather than preventing the battle from starting.
        null
    }
    private val ids = ConcurrentHashMap<DuelSound, Int>()
    private val readyIds = Collections.newSetFromMap(ConcurrentHashMap<Int, Boolean>())
    private val lastPlayed = ConcurrentHashMap<DuelSound, Long>()
    @Volatile private var released = false
    @Volatile private var isPrepared = false

    var enabled: Boolean
        get() = preferences.getBoolean("sound_on", true)
        set(value) { preferences.edit().putBoolean("sound_on", value).apply() }

    var volume: Float
        get() = preferences.getFloat("sfx_volume", .67f).coerceIn(0f, 1f)
        set(value) {
            preferences.edit().putFloat("sfx_volume", value.coerceIn(0f, 1f)).apply()
        }

    init {
        pool?.setOnLoadCompleteListener { _, sampleId, status ->
            if (!released && status == 0) readyIds.add(sampleId)
        }
    }

    private fun existingOverride(sound: DuelSound, directory: File): File? {
        for (extension in listOf("ogg", "wav")) {
            val asset = "shadow_sfx/${sound.assetName}.$extension"
            try {
                val destination = File(directory, "${sound.assetName}_override.$extension")
                app.assets.open(asset).use { input ->
                    destination.outputStream().use { output -> input.copyTo(output) }
                }
                return destination
            } catch (_: IOException) {
                // No override: synthesize the locally generated effect.
            }
        }
        return null
    }

    suspend fun prepare() {
        if (released || isPrepared || pool == null) return
        // Prepare files off the UI thread. The cache is safe for offline use.
        val prepared = withContext(Dispatchers.IO) {
            val folder = File(app.cacheDir, "shadow_sound_v1")
            if (!folder.isDirectory) folder.mkdirs()
            DuelSound.entries.mapNotNull { sound ->
                try {
                    val file = existingOverride(sound, folder) ?: run {
                        val generated = File(folder, "${sound.assetName}_v1.wav")
                        if (!generated.isFile || generated.length() <= 44L) {
                            generated.writeBytes(DuelSoundSynthesis.wav(sound))
                        }
                        generated
                    }
                    sound to file
                } catch (_: Exception) {
                    // Missing cache/storage permission should never break duel.
                    null
                }
            }
        }
        if (released) return
        for ((sound, file) in prepared) {
            try {
                pool.load(file.absolutePath, 1).takeIf { it > 0 }?.let {
                    ids[sound] = it
                }
            } catch (_: Exception) {
                // Sound is optional; keep actual card gameplay intact.
            }
        }
        isPrepared = true
    }

    /** Allows opening-hand audio to wait for async SoundPool decoding. */
    fun isReady(sound: DuelSound): Boolean =
        !released && ids[sound]?.let { it in readyIds } == true

    /** Limit rapid identical events, especially multi-token or chain effects. */
    fun play(sound: DuelSound) {
        if (released || !enabled) return
        val requestedVolume = volume * sound.gain
        if (requestedVolume <= .001f) return
        val id = ids[sound] ?: return
        if (id !in readyIds) return
        val now = SystemClock.elapsedRealtime()
        val last = lastPlayed[sound] ?: 0L
        if (now - last < when (sound) {
                DuelSound.DAMAGE, DuelSound.BUFF, DuelSound.CARD_LAND -> 90
                DuelSound.TURN, DuelSound.WIN, DuelSound.LOSE -> 240
                else -> 75
            }) return
        lastPlayed[sound] = now
        try {
            pool?.play(id, requestedVolume, requestedVolume, 1, 0, 1f)
        } catch (_: Exception) {
            // Audio failures never block the renderer or action callbacks.
        }
    }

    fun close() {
        released = true
        readyIds.clear()
        ids.clear()
        lastPlayed.clear()
        try { pool?.release() } catch (_: Exception) { }
    }
}
