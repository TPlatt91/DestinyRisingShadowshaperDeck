package com.tplatt.shadowshaper

import android.content.Context

/**
 * Device-local presentation preference. Gameplay and match saves do not
 * reference this value, so changing it cannot change ability resolution.
 */
internal enum class DuelFxMode {
    FULL, REDUCED, OFF;

    fun next(): DuelFxMode = when (this) {
        FULL -> REDUCED
        REDUCED -> OFF
        OFF -> FULL
    }
}

internal class DuelFxSettings(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "shadow_shaper_visual_effects", Context.MODE_PRIVATE
    )

    var mode: DuelFxMode
        get() = DuelFxMode.entries.firstOrNull {
            it.name == preferences.getString("fx_mode", "FULL")
        } ?: DuelFxMode.FULL
        set(value) {
            preferences.edit().putString("fx_mode", value.name).apply()
        }
}
