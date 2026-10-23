package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test

/**
 * CPU placement scores must use the same video-first printed row conditions
 * as card activation and legal target selection.
 */
class ShadowCpuVideoRowPreferenceTest {
    private fun recorded(
        name: String, faction: String, cost: Int, power: Int,
        printedEffect: String, outdatedTrigger: String
    ) = ShadowData.Card().apply {
        this.name = name
        this.faction = faction
        rarity = "Exotic"
        this.cost = cost.toString()
        this.power = power.toString()
        effect = printedEffect
        trigger = outdatedTrigger
        source = ShadowData.VIDEO_SOURCE
    }

    private fun target(name: String, faction: String, power: Int) =
        ShadowData.Card().apply {
            this.name = name
            this.faction = faction
            cost = "1"
            rarity = "Rare"
            this.power = power.toString()
            effect = "No special effects."
        }

    private fun cpuMatch(vararg cards: ShadowData.Card, seed: Long = 9L) =
        ShadowEngine(cards.associateBy { it.name }, seed).apply {
            humanTurn = false
            cpu.points = 12
        }

    @Test fun jarenUsesRecordedFrontRowRatherThanOutdatedBackRowTrigger() {
        val jaren = recorded(
            "Jaren", "Humans", 3, 4,
            "When Played (Front Row): Deals a total of 6 damage to the opponent's units at random.",
            "When Played (Back Row)"
        )
        val victimCard = target("Practice Dreg", "Fallen", 20)
        val e = cpuMatch(jaren, victimCard)
        val victim = ShadowEngine.Unit(e.human, victimCard, 0, 0)
        e.human.board[0][0] = victim
        e.cpu.hand.add(jaren)

        assertTrue(e.cpuPlayOne())
        assertNotNull(e.cpu.board[ShadowRules.FRONT_ROW].filterNotNull()
            .firstOrNull { it.card.name == "Jaren" })
        assertEquals(14, victim.power)
        assertEquals(6, e.journal.count { it == "DAMAGE 1 ▶ Practice Dreg" })
        assertEquals(0, e.rejectedCpuMoves)
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun bazinetUsesRecordedBackRowRatherThanOutdatedFrontRowTrigger() {
        val bazinet = recorded(
            "Bazinet", "Humans", 4, 2,
            "When Played (Back Row): Deals 3 damage to all opposing units with a bonus.",
            "When Played (Front Row)"
        )
        val foe = target("Practice Dreg", "Fallen", 12)
        val e = cpuMatch(bazinet, foe)
        val buffed = ShadowEngine.Unit(e.human, foe, 0, 0)
        val plain = ShadowEngine.Unit(e.human, foe, 1, 1)
        e.human.board[0][0] = buffed
        e.human.board[1][1] = plain
        e.buff(buffed, 2)
        e.cpu.hand.add(bazinet)

        assertTrue(e.cpuPlayOne())
        assertNotNull(e.cpu.board[ShadowRules.BACK_ROW].filterNotNull()
            .firstOrNull { it.card.name == "Bazinet" })
        assertEquals(11, buffed.power) // 12 + 2, then 3 damage
        assertEquals(12, plain.power)
        assertEquals(1, e.journal.count { it == "DAMAGE 3 ▶ Practice Dreg" })
        assertEquals(0, e.rejectedCpuMoves)
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun recordedUnrestrictedWhenPlayedDoesNotInheritOldRowBias() {
        val harrow = recorded(
            "Harrow", "Hive", 1, 2,
            "When Played: Flip a coin; on heads, gains +5 Power.",
            "When Played (Back Row)"
        )
        val rows = mutableSetOf<Int>()
        for (seed in 0L until 40L) {
            val e = cpuMatch(harrow, seed = seed)
            e.cpu.hand.add(harrow)
            assertTrue(e.cpuPlayOne())
            val played = e.cpu.board.flatMap { it.toList() }.filterNotNull()
                .single { it.card.name == "Harrow" }
            rows.add(played.row)
            assertEquals(0, e.rejectedCpuMoves)
        }
        // With no printed row preference, tiny seeded random tie-breaks
        // should select both rows across these deterministic positions.
        assertEquals(setOf(ShadowRules.BACK_ROW, ShadowRules.FRONT_ROW), rows)
    }
}
