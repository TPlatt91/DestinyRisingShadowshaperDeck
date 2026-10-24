package com.tplatt.shadowshaper

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class DuelRowAbilityCueTest {
    private fun card(
        name: String = "Test Unit",
        effect: String,
        trigger: String = "",
        source: String = "",
        type: String = "Common"
    ): ShadowData.Card = ShadowData.Card().apply {
        this.name = name
        rarity = "Legendary"
        faction = "Humans"
        cost = "2"
        power = "3"
        this.effect = effect
        this.trigger = trigger
        this.source = source
        classification = type
    }

    private fun cue(card: ShadowData.Card): DuelRowGlow =
        DuelRowAbilityCue.forCard(ShadowEngine(mapOf(card.name to card), 77L), card)

    @Test fun videoVerifiedFrontAbilityOverridesOutdatedBackMetadata() {
        val card = card(
            effect = "When Played (Front Row): Grants a unit +3 Power.",
            trigger = "Back Row",
            source = ShadowData.VIDEO_SOURCE
        )
        assertEquals(DuelRowGlow.FRONT_ABILITY, cue(card))
        assertEquals(ShadowRules.FRONT_ROW, DuelRowAbilityCue.abilityRow(cue(card)))
    }

    @Test fun videoVerifiedBackSustainedAbilityGetsBlueCue() {
        val card = card(
            effect = "Sustained (Back Row): Grants adjacent units +1 Power.",
            source = ShadowData.VIDEO_SOURCE
        )
        assertEquals(DuelRowGlow.BACK_ABILITY, cue(card))
        assertEquals(ShadowRules.BACK_ROW, DuelRowAbilityCue.abilityRow(cue(card)))
    }

    @Test fun unverifiedCardUsesExistingTriggerMetadata() {
        assertEquals(
            DuelRowGlow.BACK_ABILITY,
            cue(card(effect = "A unit effect.", trigger = "Sustained; Back Row"))
        )
        assertEquals(
            DuelRowGlow.FRONT_ABILITY,
            cue(card(effect = "An effect.", trigger = "When Played; Front Row"))
        )
    }

    @Test fun withoutTriggerLegacyWhenPlayedHeaderStillMatchesEngine() {
        assertEquals(
            DuelRowGlow.BACK_ABILITY,
            cue(card(effect = "When Played (Back Row): Deals 2 damage."))
        )
    }

    @Test fun unconditionalVideoAbilityNeverGetsFalseRowCue() {
        val card = card(
            effect = "When Played: Deals 1 damage to a unit in the front row.",
            trigger = "Sustained; Front Row",
            source = ShadowData.VIDEO_SOURCE
        )
        assertEquals(DuelRowGlow.STANDARD, cue(card))
        assertEquals(null, DuelRowAbilityCue.abilityRow(cue(card)))
    }

    @Test fun noAbilityAndMixedMetadataUseStandardPlacement() {
        assertEquals(DuelRowGlow.STANDARD, cue(card(effect = "No special effects.")))
        assertEquals(
            DuelRowGlow.STANDARD,
            cue(card(effect = "When Played: Does something.",
                trigger = "Front Row; Back Row"))
        )
    }

    @Test fun summonedCardsCannotReceiveHandPlayCues() {
        val token = card(
            name = "Rock", effect = "When Played (Front Row): Test",
            source = ShadowData.VIDEO_SOURCE, type = "Summoned unit"
        )
        assertEquals(DuelRowGlow.STANDARD, cue(token))
    }

    @Test fun battleGridColorsOnlyLegalEmptySpacesOfSelectedRow() {
        val screen = listOf(
            File("src/main/java/com/tplatt/shadowshaper/ShadowBattleActivity.kt"),
            File("app/src/main/java/com/tplatt/shadowshaper/ShadowBattleActivity.kt")
        ).first(File::isFile).readText()
        val visuals = listOf(
            File("src/main/java/com/tplatt/shadowshaper/ShadowBattleVisuals.kt"),
            File("app/src/main/java/com/tplatt/shadowshaper/ShadowBattleVisuals.kt")
        ).first(File::isFile).readText()
        assertEquals(true, screen.contains("DuelRowAbilityCue.forCard("))
        assertEquals(true, screen.contains("engine.isPlayablePlacement(engine.human, selectedIndex, row, col)"))
        assertEquals(true, screen.contains("glow = if (legal) rowGlow else DuelRowGlow.STANDARD"))
        assertEquals(true, screen.contains("pendingPosition == null &&"))
        assertEquals(true, screen.contains("DuelRowGlow.FRONT_ABILITY"))
        assertEquals(true, screen.contains("DuelRowGlow.BACK_ABILITY"))
        assertEquals(true, visuals.contains("duelRowGlowColor(glow)"))
        assertEquals(true, visuals.contains("Color(0xFFFFAE65)"))
        assertEquals(true, visuals.contains("Color(0xFF73D5FF)"))
    }
}
