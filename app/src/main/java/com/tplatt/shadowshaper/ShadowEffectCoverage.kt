package com.tplatt.shadowshaper

/** Conservative feature audit label; a scripted case is never proof of correctness. */
object ShadowEffectCoverage {
    private val blocked = setOf("Rock", "Xur", "Tomb Ship", "Drop Pod")
    @JvmStatic
    fun status(name: String, effect: String?, cost: String?, power: String?): String {
        if (power.isNullOrBlank() || cost.isNullOrBlank())
            return "DATA BLOCKED: Original Power or Light cost is not verified."
        if (effect.isNullOrBlank() || effect.contains("no special effects", true))
            return "BASIC RULES: No special effect; Power and placement supported."
        return when (name) {
            "Rossi-11" -> "INCOMPLETE: Overshield selection and one-time blocking are not implemented."
            "Mind Disruptor" -> "INCOMPLETE: Protection from Suppression for the unit on the left is not implemented."
            "Resilient Captain" -> "INCOMPLETE: Sustained Fallen-damage Dreg summoning with six-token limit needs implementation; existing handler is inaccurate."
            "Unrelenting Captain" -> "INCOMPLETE: Sustained +1 Power to deployed Dregs needs implementation; existing handler is inaccurate."
            "Exploder Shank" -> "INCOMPLETE: Transfer to the enemy Front Row and delayed adjacent 2-damage explosion need implementation."
            "Xur" -> "INCOMPLETE: Rock summoning requires original Rock token statistics."
            "Tomb Ship" -> "INCOMPLETE: Random Hive summon pool needs verification."
            "Drop Pod" -> "INCOMPLETE: Random Cabal Line summon pool needs verification."
            "Xuan Wei" -> "PARTIAL: Damage and relocation scripted; destination rules need verification."
            else -> "SCRIPTED / QA PENDING: Engine behavior has not passed full in-game verification."
        }
    }
}
