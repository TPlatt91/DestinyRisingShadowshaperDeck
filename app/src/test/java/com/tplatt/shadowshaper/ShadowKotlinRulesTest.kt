package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test

class ShadowKotlinRulesTest {
    private fun card(
        name: String, faction: String = "Humans", cost: Int = 1,
        power: Int = 2, rarity: String = "Rare"
    ) = ShadowData.Card().apply {
        this.name = name
        this.faction = faction
        this.cost = cost.toString()
        this.power = power.toString()
        this.rarity = rarity
        this.effect = "No special effects."
    }

    private fun catalog(): LinkedHashMap<String, ShadowData.Card> =
        linkedMapOf<String, ShadowData.Card>().apply {
            for (i in 0 until 15) put("Unit $i", card("Unit $i"))
        }

    private fun deck(): LinkedHashMap<String, Int> = linkedMapOf<String, Int>().apply {
        for (i in 0 until 15) put("Unit $i", 1)
    }

    @Test fun kotlinDeckRulesMatchLegacyJavaValidation() {
        val cards = catalog()
        val chosen = deck()
        assertEquals("", ShadowDeckRules.validate(chosen, cards))
        assertEquals(ShadowData.validate(chosen, cards), ShadowDeckRules.validate(chosen, cards))
        assertTrue(ShadowDeckRules.canAdd(chosen, cards.getValue("Unit 0"), cards).isNotBlank())
        chosen.remove("Unit 14")
        assertTrue(ShadowDeckRules.canAdd(chosen, cards.getValue("Unit 0"), cards).isEmpty())
        chosen["Unit 0"] = 2
        assertEquals("", ShadowDeckRules.validate(chosen, cards))
        chosen["Unit 0"] = 3
        assertTrue(ShadowDeckRules.validate(chosen, cards).contains("two copies"))
    }

    @Test fun kotlinGeometryRespectsFactionAndVacantSlots() {
        val side = ShadowEngine.Player(true)
        val card = card("Acolyte", "Hive")
        side.board[1][0] = ShadowEngine.Unit(side, card, 1, 0)
        side.board[1][1] = ShadowEngine.Unit(side, card, 1, 1)
        assertEquals(3, ShadowPositionRules.contiguousFactionLine(side, "Hive", 1, 2, true))
        assertEquals(2, ShadowPositionRules.contiguousFactionLine(side, "Hive", 1, 1, false))
        assertEquals(0, ShadowPositionRules.contiguousFactionLine(side, "Hive", 1, 1, true))
        assertTrue(ShadowPositionRules.isAdjacent(0, 1, 0, 2))
        assertFalse(ShadowPositionRules.isAdjacent(0, 1, 1, 1))
        assertTrue(ShadowPositionRules.isSameColumn(0, 1, 1, 1))
    }

    @Test fun kotlinDamageNeverBecomesNegative() {
        assertEquals(0, ShadowDamageRules.afterAuras(2, 0, 5))
        assertEquals(4, ShadowDamageRules.afterAuras(2, 3, 1))
        assertEquals(0, ShadowDamageRules.afterAuras(0, 0, 0))
    }

    @Test fun coordinatorReportsAfterPlayAndTurnChange() {
        val cards = catalog()
        val chosen = deck()
        val coordinator = (0L..500L).map { ShadowBattleCoordinator(cards, it) }
            .first { c -> c.start(chosen, chosen); c.engine.humanFirst }
        val before = coordinator.state.value
        assertEquals(1, before.round)
        assertTrue(before.playerTurn)
        assertTrue(coordinator.play(coordinator.engine.human, 0, 1, 0, null))
        val after = coordinator.state.value
        assertEquals(before.playerHand - 1, after.playerHand)
        assertEquals(0, after.playerLight)
        assertEquals(2, after.playerPower)
        coordinator.endTurn()
        assertFalse(coordinator.state.value.playerTurn)
    }

    @Test fun abilityRegistryClassifiesSummonAndBasicFamilies() {
        val basic = card("Dreg")
        val creator = card("Carrier").apply { effect = "When Played: Summons two Dregs." }
        assertEquals(AbilityFamily.BASIC, ShadowEffectRegistry.family(basic))
        assertEquals(AbilityFamily.SUMMON, ShadowEffectRegistry.family(creator))
    }
}
