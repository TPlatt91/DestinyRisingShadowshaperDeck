package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test

/**
 * Same-hit Saladin responses, Tan-2 event consumption, and once-per-turn
 * Elite Sniper Vandal retaliation. Local JVM tests only.
 */
class ShadowSaladinRetaliationChainTest {
    private fun card(name: String, faction: String, power: Int, effect: String) =
        ShadowData.Card().apply {
            this.name = name
            this.faction = faction
            this.power = power.toString()
            this.cost = "1"
            this.rarity = "Rare"
            this.effect = effect
        }

    private fun engine() = ShadowEngine(listOf(
        card("Saladin", "Humans", 6,
            "Sustained: Whenever one of your Human units takes damage, they gain +3 Power."),
        card("Human Guard", "Humans", 10, "No special effects."),
        card("Dreg", "Fallen", 10, "No special effects."),
        card("Elite Sniper Vandal", "Fallen", 4,
            "Sustained (Back Row): Damages an opposing unit that gains Power, once per unit per turn."),
        card("Tan-2", "Humans", 2,
            "Sustained: Doubles your next Power bonus."),
        card("Shinka Phalanx", "Cabal", 4,
            "Sustained (Column): Unit across the column takes 1 less damage.")
    ).associateBy { it.name }, 67L)

    private fun put(e: ShadowEngine, name: String,
                    owner: ShadowEngine.Player, row: Int, col: Int,
                    currentPower: Int? = null): ShadowEngine.Unit {
        val unit = ShadowEngine.Unit(owner, e.catalog.getValue(name), row, col)
        if (currentPower != null) unit.power = currentPower
        owner.board[row][col] = unit
        return unit
    }

    @Test fun twoSaladinsCanSaveSameHumanFromOneLethalDamageEvent() {
        val e = engine()
        val attacker = put(e, "Dreg", e.cpu, 1, 5)
        val victim = put(e, "Human Guard", e.human, 1, 2, 3)
        put(e, "Saladin", e.human, 0, 0)
        put(e, "Saladin", e.human, 0, 1)

        assertEquals(8, e.resolvedDamage(attacker, victim, 8))
        e.sweep()

        assertSame(victim, e.human.board[1][2])
        assertEquals(1, victim.power)
        assertEquals(6, victim.bonus)
        assertEquals(2, e.journal.count { it == "BUFF +3 ▶ Human Guard" })
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun oneSaladinCannotSaveTargetFromDamageBeyondItsBonus() {
        val e = engine()
        val attacker = put(e, "Dreg", e.cpu, 1, 5)
        val victim = put(e, "Human Guard", e.human, 1, 2, 3)
        put(e, "Saladin", e.human, 0, 0)

        e.damage(attacker, victim, 8)
        e.sweep()

        assertNull(e.human.board[1][2])
        assertEquals(-2, victim.power)
    }

    @Test fun tan2DoublesOnlyFirstOfTwoSameHitSaladinBonuses() {
        val e = engine()
        val attacker = put(e, "Dreg", e.cpu, 1, 5)
        val victim = put(e, "Human Guard", e.human, 1, 2, 3)
        val doubler = put(e, "Tan-2", e.human, 0, 4)
        put(e, "Saladin", e.human, 0, 0)
        put(e, "Saladin", e.human, 0, 1)

        e.damage(attacker, victim, 10)
        e.sweep()

        assertSame(victim, e.human.board[1][2])
        assertEquals(2, victim.power) // 3 - 10 + 6 + 3
        assertEquals(9, victim.bonus)
        assertTrue(doubler.bonusDoublerConsumed)
        assertEquals(1, e.journal.count { it == "BUFF +6 ▶ Human Guard" })
        assertEquals(1, e.journal.count { it == "BUFF +3 ▶ Human Guard" })
    }

    @Test fun twoSnipersEachHitOnceDespiteNestedSaladinBuffs() {
        val e = engine()
        val attacker = put(e, "Dreg", e.cpu, 1, 5)
        val victim = put(e, "Human Guard", e.human, 1, 2)
        put(e, "Saladin", e.human, 0, 0)
        put(e, "Saladin", e.human, 0, 1)
        put(e, "Elite Sniper Vandal", e.cpu, 0, 0)
        put(e, "Elite Sniper Vandal", e.cpu, 0, 1)

        e.damage(attacker, victim, 1)

        assertEquals(25, victim.power) // Three 1-damage hits and 6 Power after each
        assertEquals(18, victim.bonus)
        assertEquals(3, e.journal.count { it == "DAMAGE 1 ▶ Human Guard" })
        assertEquals(6, e.journal.count { it == "BUFF +3 ▶ Human Guard" })
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun suppressedSaladinCannotReactButOtherCopyCan() {
        val e = engine()
        val attacker = put(e, "Dreg", e.cpu, 1, 5)
        val victim = put(e, "Human Guard", e.human, 1, 2)
        val suppressed = put(e, "Saladin", e.human, 0, 0)
        put(e, "Saladin", e.human, 0, 1)
        suppressed.suppressed = true

        e.damage(attacker, victim, 1)

        assertEquals(12, victim.power)
        assertEquals(3, victim.bonus)
        assertEquals(1, e.journal.count { it == "BUFF +3 ▶ Human Guard" })
    }

    @Test fun shieldedAndFullyPreventedHitsGrantNoSaladinPower() {
        val e = engine()
        val attacker = put(e, "Dreg", e.cpu, 1, 5)
        val victim = put(e, "Human Guard", e.human, 1, 2)
        put(e, "Saladin", e.human, 0, 0)
        put(e, "Saladin", e.human, 0, 1)
        victim.overshield = true

        e.damage(attacker, victim, 3)
        assertEquals(10, victim.power)
        assertEquals(0, victim.bonus)
        assertFalse(victim.overshield)

        put(e, "Shinka Phalanx", e.human, 0, 2)
        e.damage(attacker, victim, 1)
        assertEquals(10, victim.power)
        assertEquals(0, victim.bonus)
        assertEquals(0, e.journal.count { it == "BUFF +3 ▶ Human Guard" })
    }

    @Test fun freshSecondDamageEventGrantsFreshSaladinResponses() {
        val e = engine()
        val attacker = put(e, "Dreg", e.cpu, 1, 5)
        val victim = put(e, "Human Guard", e.human, 1, 2)
        put(e, "Saladin", e.human, 0, 0)
        put(e, "Saladin", e.human, 0, 1)

        e.damage(attacker, victim, 1)
        e.damage(attacker, victim, 1)

        assertEquals(20, victim.power) // 10 - 2 + 12
        assertEquals(12, victim.bonus)
        assertEquals(4, e.journal.count { it == "BUFF +3 ▶ Human Guard" })
    }

    @Test fun buffedSaladinItselfStillActivatesItsOwnDamageReaction() {
        val e = engine()
        val attacker = put(e, "Dreg", e.cpu, 1, 5)
        val victim = put(e, "Saladin", e.human, 0, 0)

        e.damage(attacker, victim, 2)

        assertEquals(7, victim.power) // 6 - 2 + 3
        assertEquals(3, victim.bonus)
    }
}
