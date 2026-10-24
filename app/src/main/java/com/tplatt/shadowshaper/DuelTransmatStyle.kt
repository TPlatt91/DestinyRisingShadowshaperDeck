package com.tplatt.shadowshaper

/**
 * Shadowshaper faction transmats. Use only factions present in the current
 * game or specifically requested for forward compatibility (Vex).
 * No Scorn, Taken, Dread or invented faction-specific effects.
 */
internal enum class DuelTransmatStyle {
    DEFAULT, HIVE, CABAL, VEX, FALLEN
}

internal object DuelTransmatResolver {
    fun forFaction(faction: String?): DuelTransmatStyle =
        when (faction?.trim()?.lowercase()) {
            "hive" -> DuelTransmatStyle.HIVE
            "cabal" -> DuelTransmatStyle.CABAL
            "vex" -> DuelTransmatStyle.VEX
            "fallen" -> DuelTransmatStyle.FALLEN
            // Human, Exo and Awoken all use the same Guardian transmat.
            "human", "humans", "exo", "exos", "awoken" ->
                DuelTransmatStyle.DEFAULT
            // A token without a recognized faction is neutral/default;
            // don't infer a faction from its artwork or card name.
            else -> DuelTransmatStyle.DEFAULT
        }

    /** A visual-only timeline: precharge, materialization, final fade. */
    fun visibility(progress: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        val rise = (p / .17f).coerceIn(0f, 1f)
        val fade = ((1f - p) / .35f).coerceIn(0f, 1f)
        return rise * fade
    }

    const val PRECHARGE_MS = 170L
    const val EFFECT_MS = 960
}
