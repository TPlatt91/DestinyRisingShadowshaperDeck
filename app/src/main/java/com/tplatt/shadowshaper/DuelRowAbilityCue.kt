package com.tplatt.shadowshaper

/**
 * Presentation-only cue for playable hand cards. Never changes placement
 * legality, Light cost, trigger timing, targets, or row restrictions.
 */
internal enum class DuelRowGlow {
    STANDARD,
    FRONT_ABILITY,
    BACK_ABILITY
}

internal object DuelRowAbilityCue {
    /**
     * Ask the same engine resolver used to activate actual effects. A row
     * receives a special cue only when its ability is active there but not
     * on the opposite row. Verified in-game wording takes precedence over
     * older metadata through ShadowEngine.activeInRow.
     */
    fun forCard(engine: ShadowEngine, card: ShadowData.Card): DuelRowGlow {
        if (!card.isPlayable) return DuelRowGlow.STANDARD
        val front = engine.activeInRow(card, ShadowRules.FRONT_ROW)
        val back = engine.activeInRow(card, ShadowRules.BACK_ROW)
        return when {
            front && !back -> DuelRowGlow.FRONT_ABILITY
            back && !front -> DuelRowGlow.BACK_ABILITY
            else -> DuelRowGlow.STANDARD
        }
    }

    fun abilityRow(glow: DuelRowGlow): Int? = when (glow) {
        DuelRowGlow.FRONT_ABILITY -> ShadowRules.FRONT_ROW
        DuelRowGlow.BACK_ABILITY -> ShadowRules.BACK_ROW
        DuelRowGlow.STANDARD -> null
    }
}
