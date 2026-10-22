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

            "Molkrul" -> "SCRIPTED / QA PENDING: Coin flip instant defeat of a selected opponent unit of at most 10 Power, Back Row only."
            "Shinka Gladiator" -> "SCRIPTED / QA PENDING: In a 5-unit Line grants selected allied unit and adjacent allies +2 outgoing damage, not Power."
            "Shinka Centurion" -> "SCRIPTED / QA PENDING: Vertical Column partner required for sustained +3 Power to left/right units."
            "Paradoxical Neophyte's Shade" -> "SCRIPTED / QA PENDING: Related summoned unit, excluded from constructed decks; +4 Power per Inspired Hive ally."
            "Helhest" -> "SCRIPTED / QA PENDING: Back Row damage to a random enemy and adjacent units; Power gain sums successful damage including overkill, excluding Overshield-blocked hits. Exact parity still requires original gameplay verification."
            "Juggernaut" -> "SCRIPTED / QA PENDING: Column condition combined with printed left/right Cabal damage. Confirm Column and adjacency interpretation against original gameplay."
            "Tomb Ship" -> "SCRIPTED / QA PENDING: Summons random Hive units to left/right, including summon-only Neophyte Shade; exact hidden pool and odds remain unverified."
            "Drop Pod" -> "SCRIPTED / QA PENDING: Summons from printed Line-affix card pool; confirm exact random pool against game."
            "Xuan Wei" -> "SCRIPTED / QA PENDING: Front Row 5-damage and relocation; confirm full-board behavior."
            else -> "SCRIPTED / QA PENDING: Engine behavior has not passed full in-game verification."
        }
    }
}
