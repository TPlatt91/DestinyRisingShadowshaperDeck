package com.tplatt.shadowshaper

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import java.util.Collections
import java.util.IdentityHashMap

/**
 * One music player for the complete Android application:
 *
 * Home / Deck Construction / Card Collection / Rules <-> Skirmish.
 *
 * Both Activities share the same MediaPlayer and persisted soundtrack
 * settings. ActivityLifecycleCallbacks are installed once on the Application
 * before either Activity starts. Starting the next Activity keeps playback
 * continuous; leaving the whole app pauses it, without any foreground service
 * or background audio notification.
 */
internal object DuelMusicSession {
    private var app: Application? = null
    private var instance: DuelBackgroundMusic? = null
    private val active = Collections.newSetFromMap(
        IdentityHashMap<Activity, Boolean>()
    )
    private val handler = Handler(Looper.getMainLooper())
    private const val TRANSITION_GRACE_MS = 300L
    private val pauseWhenBackground = Runnable {
        if (active.isEmpty()) instance?.onBackground()
    }

    /**
     * Call in Activity.onCreate, before that Activity's onStart callback.
     * All registration, playback and Compose state mutations occur on main.
     */
    fun get(context: Context): DuelBackgroundMusic {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "Music session must be initialized on Android's main thread"
        }
        val application = context.applicationContext as Application
        if (app == null) {
            app = application
            instance = DuelBackgroundMusic(application)
            application.registerActivityLifecycleCallbacks(callbacks)
        }
        check(app === application) { "Music session cannot cross Applications" }
        return requireNotNull(instance)
    }

    private val callbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityStarted(activity: Activity) {
            // Only the two Shadow Shaper screens should activate the music.
            if (activity !is ShadowComposeActivity &&
                activity !is ShadowBattleActivity) return
            val changed = active.add(activity)
            handler.removeCallbacks(pauseWhenBackground)
            if (changed) instance?.onForeground()
        }
        override fun onActivityResumed(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) {
            if (active.remove(activity) && active.isEmpty()) {
                // Covers brief Activity transitions and configuration changes
                // without a pause/restart or another concurrent music player.
                handler.removeCallbacks(pauseWhenBackground)
                handler.postDelayed(pauseWhenBackground, TRANSITION_GRACE_MS)
            }
        }
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) {
            // Defensive cleanup for any unusual destroyed-before-stopped path.
            if (active.remove(activity) && active.isEmpty()) {
                handler.removeCallbacks(pauseWhenBackground)
                handler.postDelayed(pauseWhenBackground, TRANSITION_GRACE_MS)
            }
        }
    }
}
