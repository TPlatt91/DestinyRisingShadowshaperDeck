package com.tplatt.shadowshaper

/** Conservative feature audit label; a scripted case is never proof of correctness. */
object ShadowEffectCoverage {
    @JvmStatic
    fun status(name: String, effect: String?, cost: String?, power: String?): String {
        if (power.isNullOrBlank() || cost.isNullOrBlank())
            return "DATA BLOCKED: Original Power or Light cost is not verified."
        if (effect.isNullOrBlank() || effect.contains("no special effects", true))
            return "BASIC RULES: No special effect; Power and placement supported."
        return when (name) {
            // Original video mechanics for these five cards have now been
            // coded. They remain SCRIPTED / QA PENDING until local and device
            // regression tests actually pass.
            "Tomb Ship" -> "INCOMPLETE: Random Hive summon pool needs verification."
            "Drop Pod" -> "SCRIPTED / QA PENDING: Summons from printed Line-affix card pool; confirm exact random pool against game."
            "Xuan Wei" -> "SCRIPTED / QA PENDING: Front Row 5-damage and relocation; confirm full-board behavior."
            else -> "SCRIPTED / QA PENDING: Engine behavior has not passed full in-game verification."
        }
    }
}
