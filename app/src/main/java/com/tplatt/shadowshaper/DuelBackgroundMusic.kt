package com.tplatt.shadowshaper

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Every track is bundled in app/src/main/assets/background_music. Preserve
 * original file names on disk; UI uses the published game/expansion title.
 * Base-game music is intentionally NOT mislabeled as a paid DLC.
 */
internal data class DuelMusicTrack(
    val id: String,
    val title: String,
    val series: String,
    val assetFile: String
) {
    val assetPath: String get() = "background_music/$assetFile"
}

internal object DuelMusicCatalog {
    val tracks = listOf(
        DuelMusicTrack("001", "Destiny — Base Game (2014)", "DESTINY", "001. Destiny 1 - Vanilla (2014).opus"),
        DuelMusicTrack("002", "The Taken King (2015)", "DESTINY", "002. Destiny 1 - The Taken King (2015).opus"),
        DuelMusicTrack("003", "Rise of Iron (2016)", "DESTINY", "003. Destiny 1 - Rise of Iron (2016).opus"),
        DuelMusicTrack("004", "Destiny 2 — Base Game (2017)", "DESTINY 2", "004. Destiny 2 - Vanilla (2017).opus"),
        DuelMusicTrack("005", "Curse of Osiris (2017)", "DESTINY 2", "005. Destiny 2 - Curse of Osiris (2017).opus"),
        DuelMusicTrack("006", "Warmind (2018)", "DESTINY 2", "006. Destiny 2 - Warmind (2018).opus"),
        DuelMusicTrack("007", "Forsaken (2018)", "DESTINY 2", "007. Destiny 2 - Forsaken (2018).opus"),
        DuelMusicTrack("008", "Shadowkeep (2019)", "DESTINY 2", "008. Destiny 2 - Shadowkeep (2019).opus"),
        DuelMusicTrack("009", "Beyond Light (2020)", "DESTINY 2", "009. Destiny 2 - Beyond Light (2020).opus"),
        DuelMusicTrack("010", "The Witch Queen (2022)", "DESTINY 2", "010. Destiny 2 - The Witch Queen (2022).opus"),
        DuelMusicTrack("011", "Lightfall (2023)", "DESTINY 2", "011. Destiny 2 - Lightfall (2023).opus"),
        DuelMusicTrack("012", "The Final Shape (2024)", "DESTINY 2", "012. Destiny 2 - The Final Shape (2024).opus"),
        DuelMusicTrack("013", "The Edge of Fate (2025)", "DESTINY 2", "013. Destiny 2 - The Edge of Fate (2025).opus")
    )
    fun byId(id: String): DuelMusicTrack? = tracks.firstOrNull { it.id == id }
}

/**
 * Dedicated long-form music player. SFX remain owned by DuelSoundPlayer/
 * SoundPool and retain their independent mute/volume settings.
 *
 * Android MediaPlayer decodes the bundled Ogg/Opus tracks asynchronously.
 * Gradle androidResources.noCompress must include "opus" for openFd().
 * If the device cannot decode Opus, a visible status is shown and combat
 * continues silently; no network or runtime storage permission required.
 */
internal class DuelBackgroundMusic(context: Context) {
    private val app = context.applicationContext
    private val preferences = app.getSharedPreferences("shadow_duel_background_music", Context.MODE_PRIVATE)
    private val audioManager = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private var mediaPlayer: MediaPlayer? = null
    private var loadedId: String? = null
    private var prepared = false
    private var foreground = false
    private var destroyed = false
    private var focusHeld = false
    private var ducked = false

    var status by mutableStateOf("READY")
        private set

    var enabled: Boolean
        get() = preferences.getBoolean("music_enabled", true)
        set(value) {
            preferences.edit().putBoolean("music_enabled", value).apply()
            if (!value) {
                pauseInternal()
                releaseFocus()
                status = "MUSIC OFF"
            } else {
                startSelected()
            }
        }

    var volume: Float
        get() = preferences.getFloat("music_volume", .27f).coerceIn(0f, 1f)
        set(value) {
            preferences.edit().putFloat("music_volume", value.coerceIn(0f, 1f)).apply()
            applyVolume()
        }

    var selectedTrackId: String
        get() = preferences.getString("selected_track", "001")
            ?.takeIf { DuelMusicCatalog.byId(it) != null } ?: "001"
        set(value) {
            if (DuelMusicCatalog.byId(value) == null) return
            if (selectedTrackId == value) return
            preferences.edit().putString("selected_track", value).apply()
            releasePlayer()
            startSelected()
        }

    val selectedTrack: DuelMusicTrack
        get() = DuelMusicCatalog.byId(selectedTrackId) ?: DuelMusicCatalog.tracks.first()

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                focusHeld = false
                pauseInternal()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pauseInternal()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                ducked = true
                applyVolume()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                ducked = false
                focusHeld = true
                applyVolume()
                if (foreground && enabled) playPrepared()
            }
        }
    }
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(audioAttributes)
        .setOnAudioFocusChangeListener(focusListener)
        .build()

    fun onForeground() {
        if (destroyed) return
        foreground = true
        startSelected()
    }

    fun onBackground() {
        foreground = false
        pauseInternal()
        releaseFocus()
    }

    private fun obtainFocus(): Boolean {
        if (focusHeld) return true
        val accepted = audioManager.requestAudioFocus(focusRequest)
        focusHeld = accepted == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return focusHeld
    }

    private fun releaseFocus() {
        if (focusHeld) {
            audioManager.abandonAudioFocusRequest(focusRequest)
            focusHeld = false
        }
    }

    private fun applyVolume() {
        val gain = (volume * if (ducked) .26f else 1f).coerceIn(0f, 1f)
        try { mediaPlayer?.setVolume(gain, gain) } catch (_: Exception) {}
    }

    private fun pauseInternal() {
        if (!prepared) return
        try {
            mediaPlayer?.let { if (it.isPlaying) it.pause() }
        } catch (_: Exception) {}
    }

    private fun playPrepared() {
        if (destroyed || !enabled || !foreground || !prepared) return
        if (!obtainFocus()) {
            status = "AUDIO FOCUS UNAVAILABLE"
            return
        }
        applyVolume()
        try {
            mediaPlayer?.let {
                if (!it.isPlaying) it.start()
                status = "PLAYING"
            }
        } catch (_: Exception) {
            status = "MUSIC PLAYBACK UNAVAILABLE"
        }
    }

    private fun startSelected() {
        if (destroyed || !foreground || !enabled) return
        val song = selectedTrack
        if (mediaPlayer != null && loadedId == song.id) {
            playPrepared()
            return
        }
        releasePlayer()
        status = "LOADING TRACK"
        try {
            // Assets must not be APK-compressed, otherwise openFd() fails.
            app.assets.openFd(song.assetPath).use { descriptor ->
                val instance = MediaPlayer()
                mediaPlayer = instance
                loadedId = song.id
                instance.setAudioAttributes(audioAttributes)
                instance.setDataSource(
                    descriptor.fileDescriptor,
                    descriptor.startOffset,
                    descriptor.length
                )
                instance.isLooping = true
                instance.setOnPreparedListener { ready ->
                    if (mediaPlayer === ready && !destroyed) {
                        prepared = true
                        applyVolume()
                        playPrepared()
                    }
                }
                instance.setOnErrorListener { broken, _, _ ->
                    if (mediaPlayer === broken) {
                        status = "TRACK NOT SUPPORTED ON THIS DEVICE"
                        releasePlayer()
                    }
                    true
                }
                instance.prepareAsync()
            }
        } catch (_: Exception) {
            status = "COULD NOT LOAD MUSIC FILE"
            releasePlayer()
        }
    }

    private fun releasePlayer() {
        prepared = false
        loadedId = null
        val old = mediaPlayer
        mediaPlayer = null
        try {
            old?.setOnPreparedListener(null)
            old?.setOnErrorListener(null)
            old?.release()
        } catch (_: Exception) {}
    }

    fun close() {
        if (destroyed) return
        destroyed = true
        foreground = false
        releasePlayer()
        releaseFocus()
    }
}
