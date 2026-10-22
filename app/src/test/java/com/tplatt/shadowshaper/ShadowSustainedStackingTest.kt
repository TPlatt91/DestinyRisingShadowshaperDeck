package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test

/** Shared damage aura, defensive aura and nested retaliation tests. */
class ShadowSustainedStackingTest {
    private fun card(name: String, faction: String, power: Int = 10,
                     effect: String = "No special effects.") =
        ShadowData.Card().apply {
            this.name = name
            this.faction = faction
            this.cost = "1"
            this.power = power.toString()
            this.rarity = "Rare"
            this.effect = effect
        }

    private fun engine(): ShadowEngine {
        val cards = listOf(
            card("Dreg", "Fallen"),
            card("Cabal Target", "Cabal"),
            card("Servitor", "Fallen", 3,
                 "Sustained: All your units deal 1 additional damage."),
            card("Elite Sniper Vandal", "Fallen", 4,
                 "Sustained (Back Row): Opponent Power gain causes 1 damage once per target per turn."),
            card("Phalanx", "Cabal", 4,
                 "Sustained (3-Unit Line): All your Cabal units take 1 less damage."),
            card("Line Ally", "Cabal", 3,
                 "Sustained (2-Unit Line): A printed Line affix.")
        ).associateBy { it.name }
        return ShadowEngine(cards, 21L)
    }

    private fun put(e: ShadowEngine, name: String, side: ShadowEngine.Player,
                    row: Int, col: Int): ShadowEngine.Unit {
        val u = ShadowEngine.Unit(side, e.catalog.getValue(name), row, col)
        side.board[row][col] = u
        return u
    }

    @Test fun twoServitorDamageAurasStackThenStopAsSourcesDisappear() {
        val e = engine()
        val first = put(e, "Servitor", e.human, 0, 0)
        put(e, "Servitor", e.human, 0, 1)
        val attacker = put(e, "Dreg", e.human, 1, 1)
        val defender = put(e, "Cabal Target", e.cpu, 1, 1)

        e.damage(attacker, defender, 1)
        assertEquals(7, defender.power) // 1 base + 2 Servitors
        e.suppress(first)
        e.damage(attacker, defender, 1)
        assertEquals(5, defender.power) // 1 base + 1 unsuppressed Servitor
        e.human.board[0][1] = null
        e.damage(attacker, defender, 1)
        assertEquals(4, defender.power) // all sustained damage auras gone
    }

    @Test fun twoPhalanxReductionsStackAndStopWhenSourcesAreSuppressed() {
        val e = engine()
        val attacker = put(e, "Dreg", e.human, 1, 5)
        for (row in 0..1) {
            put(e, "Line Ally", e.cpu, row, 0)
            put(e, "Phalanx", e.cpu, row, 1)
            put(e, "Line Ally", e.cpu, row, 2)
        }
        val defender = put(e, "Cabal Target", e.cpu, 1, 4)
        assertEquals(2, e.effects.reduction(defender))
        e.damage(attacker, defender, 3)
        assertEquals(9, defender.power)
        e.suppress(e.cpu.board[1][1])
        assertEquals(1, e.effects.reduction(defender))
        e.damage(attacker, defender, 3)
        assertEquals(7, defender.power)
        e.cpu.board[0][1] = null
        assertEquals(0, e.effects.reduction(defender))
        e.damage(attacker, defender, 3)
        assertEquals(4, defender.power)
    }

    @Test fun firstSniperKillStopsSecondSniperFromAttackingDefeatedBuffTarget() {
        val e = engine()
        val weak = put(e, "Dreg", e.human, 1, 0)
        weak.power = 1
        put(e, "Elite Sniper Vandal", e.cpu, 0, 0)
        put(e, "Elite Sniper Vandal", e.cpu, 0, 1)
        put(e, "Servitor", e.cpu, 1, 3)

        e.buff(weak, 1) // 2 Power; first sniper hit 1+1 = 2 kills it
        assertNull(e.human.board[1][0])
        assertEquals(1, e.journal.count { it.contains("DAMAGE 2 ▶ Dreg") })
        assertEquals(1, e.journal.count { it.contains("DESTROYED ▶ Dreg") })
    }
}
