package com.tplatt.shadowshaper

/**
 * Video-confirmed Shadowshaper Duels thresholds for special card effects.
 * The newly played card counts among its owner's units on the field.
 */
internal object ShadowSpecialCardRules {
    fun wolfHasEnoughUnits(alliedUnitsIncludingWolf: Int): Boolean =
        alliedUnitsIncludingWolf > 5

    fun wolfTargetEligible(targetPower: Int): Boolean = targetPower >= 5

    fun wolfCanDestroy(alliedUnitsIncludingWolf: Int, targetPower: Int): Boolean =
        wolfHasEnoughUnits(alliedUnitsIncludingWolf) && wolfTargetEligible(targetPower)
}
