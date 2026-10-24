package com.tplatt.shadowshaper

/**
 * Original, lightweight SFX palette. These are not recordings ripped from
 * Destiny: Rising; the synthesis is designed for similar sci-fi/game feel.
 *
 * A genuine user-supplied sample can later override each sound by placing
 * assets/shadow_sfx/<assetName>.ogg or .wav in the Android project.
 */
internal enum class DuelSound(val assetName: String, val durationMs: Int, val gain: Float) {
    UI_TAP("ui_tap", 100, .36f),
    CARD_SELECT("card_select", 165, .44f),
    TRANSMAT_DEFAULT("transmat_default", 520, .49f),
    TRANSMAT_HIVE("transmat_hive", 570, .52f),
    TRANSMAT_CABAL("transmat_cabal", 580, .52f),
    TRANSMAT_VEX("transmat_vex", 580, .49f),
    TRANSMAT_FALLEN("transmat_fallen", 580, .50f),
    CARD_LAND("card_land", 320, .68f),
    DAMAGE("damage", 270, .54f),
    DESTROY("destroy", 420, .62f),
    BUFF("buff", 360, .42f),
    SHIELD("shield", 440, .45f),
    STATUS("status", 320, .39f),
    TURN("turn", 500, .50f),
    WIN("win", 800, .54f),
    LOSE("lose", 700, .48f)
}

internal object DuelSoundEvents {
    fun transmatFor(faction: String?): DuelSound = when (
        DuelTransmatResolver.forFaction(faction)
    ) {
        DuelTransmatStyle.DEFAULT -> DuelSound.TRANSMAT_DEFAULT
        DuelTransmatStyle.HIVE -> DuelSound.TRANSMAT_HIVE
        DuelTransmatStyle.CABAL -> DuelSound.TRANSMAT_CABAL
        DuelTransmatStyle.VEX -> DuelSound.TRANSMAT_VEX
        DuelTransmatStyle.FALLEN -> DuelSound.TRANSMAT_FALLEN
    }

    /** Card placement/summon is handled by its own timed transmat animator. */
    fun forJournalLine(line: String): DuelSound? {
        val s = line.uppercase()
        return when {
            s.contains("DAMAGE PREVENTED") || s.contains("OVERSHIELD") ->
                DuelSound.SHIELD
            s.contains("DESTROYED ▶") || s.contains("DESTROY EFFECT ▶") ||
                s.contains("DETONATES ▶") -> DuelSound.DESTROY
            s.startsWith("DAMAGE ") -> DuelSound.DAMAGE
            s.startsWith("BUFF ") || s.startsWith("INSPIRED ▶") ->
                DuelSound.BUFF
            s.startsWith("SUPPRESSED ▶") || s.startsWith("SCORCHED ▶") ||
                s.startsWith("RELOCATED ▶") -> DuelSound.STATUS
            else -> null
        }
    }

    /**
     * Journal has a rolling 90-entry cap. Find the largest ordered suffix of
     * old messages matching the prefix of the new list, so duplicated texts
     * and rolls don't double-play sound effects.
     */
    fun newLines(previous: List<String>, current: List<String>): List<String> {
        if (previous.isEmpty()) return current
        val overlapLimit = minOf(previous.size, current.size)
        for (overlap in overlapLimit downTo 1) {
            if (previous.takeLast(overlap) == current.take(overlap)) {
                return current.drop(overlap)
            }
        }
        // A full journal reset is a new match; no old event should re-sound.
        return emptyList()
    }
}
