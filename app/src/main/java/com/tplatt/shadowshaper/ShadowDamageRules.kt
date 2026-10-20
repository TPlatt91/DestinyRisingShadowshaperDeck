package com.tplatt.shadowshaper

/** Pure resolution math shared by damage effects. */
object ShadowDamageRules {
    @JvmStatic
    fun afterAuras(rawDamage: Int, attackerBonus: Int, defenderReduction: Int): Int =
        (rawDamage.coerceAtLeast(0) + attackerBonus.coerceAtLeast(0) -
            defenderReduction.coerceAtLeast(0)).coerceAtLeast(0)
}
