package com.tplatt.shadowshaper

/** Shared mechanic classification for auditing and adding card families.
 * A classification is NOT proof that a card's printed effect is implemented.
 */
enum class AbilityFamily {
    BASIC, ON_PLAY, SUSTAINED, INSPIRED, SUMMON, DAMAGE, BUFF,
    TARGET, MOVEMENT, STATUS, UNKNOWN
}
object ShadowEffectRegistry {
    @JvmStatic
    fun family(card: ShadowData.Card): AbilityFamily {
        val text = card.effect.lowercase()
        return when {
            text.isBlank() || text.contains("no special effects") -> AbilityFamily.BASIC
            "summon" in text || "creates a " in text -> AbilityFamily.SUMMON
            "move" in text || "relocat" in text -> AbilityFamily.MOVEMENT
            "suppress" in text || "scorch" in text -> AbilityFamily.STATUS
            "damage" in text -> AbilityFamily.DAMAGE
            "power bonus" in text || "grants +" in text || "gains +" in text -> AbilityFamily.BUFF
            "inspired" in text -> AbilityFamily.INSPIRED
            "sustained" in text -> AbilityFamily.SUSTAINED
            "when played" in text -> AbilityFamily.ON_PLAY
            else -> AbilityFamily.UNKNOWN
        }
    }

    @JvmStatic
    fun isUnresolved(card: ShadowData.Card): Boolean =
        ShadowEffectCoverage.status(card.name, card.effect, card.cost, card.power)
            .startsWith("INCOMPLETE") ||
        ShadowEffectCoverage.status(card.name, card.effect, card.cost, card.power)
            .startsWith("DATA BLOCKED")
}
