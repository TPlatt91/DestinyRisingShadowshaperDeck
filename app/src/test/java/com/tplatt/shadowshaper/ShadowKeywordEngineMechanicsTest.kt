package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test

/** Keyword and event-origin regressions; run locally without GitHub Actions. */
class ShadowKeywordEngineMechanicsTest {
    private fun card(name: String, faction: String, effect: String) =
        ShadowData.Card().apply {
            this.name = name; this.faction = faction
            rarity = "Rare"; cost = "1"; power = "10"; this.effect = effect
        }

    private fun engine(): ShadowEngine {
        val data = listOf(
            card("Resilient Grenade Dreg", "Fallen",
                 "Sustained (Back Row): Opponent Back Row placements are Scorched for 2 turns."),
            card("Resilient Captain", "Fallen",
                 "Sustained: When a Fallen ally deals damage, summon a Dreg to the right."),
            card("Ayuela", "Humans",
                 "Sustained (Back Row): Scorches opposing Front Row cards."),
            card("Zhiyao", "Humans",
                 "Sustained: Opponent Front Row placements grant +1 Power for 3 turns."),
            card("Tan-2", "Humans",
                 "Sustained: Doubles the next Power bonus."),
            card("Dreg", "Fallen", "No special effects."),
            card("Legionary", "Cabal", "Sustained (Column): Gains +1 Power.")
        ).associateBy { it.name }
        return ShadowEngine(data, 7L)
    }

    private fun put(e: ShadowEngine, name: String, side: ShadowEngine.Player,
                    row: Int, col: Int): ShadowEngine.Unit {
        val result = ShadowEngine.Unit(side, e.catalog.getValue(name), row, col)
        side.board[row][col] = result
        return result
    }

    @Test fun scorchedCreditsTheCasterRatherThanTheVictim() {
        val e = engine()
        val caster = put(e, "Resilient Grenade Dreg", e.human, 0, 0)
        put(e, "Resilient Captain", e.human, 0, 3)
        put(e, "Resilient Captain", e.cpu, 0, 3)
        val enemy = put(e, "Dreg", e.cpu, 0, 0)
        e.effects.fireEnemyPlacement(enemy)
        assertSame(caster, enemy.scorchSource)
        assertEquals(2, enemy.scorchTurns)
        e.effects.endSide(e.cpu)
        assertEquals(8, enemy.power)
        assertEquals("Dreg", e.human.board[0][4]?.card?.name)
        assertNull("Victim's Captain cannot claim the opponent's damage", e.cpu.board[0][4])
        assertEquals(1, enemy.scorchTurns)
        e.effects.endSide(e.cpu)
        assertEquals(6, enemy.power)
        assertEquals(0, enemy.scorchTurns)
        assertNull(enemy.scorchSource)
    }

    @Test fun suppressionImmediatelyRemovesScorchedAndInspired() {
        val e = engine()
        val ally = put(e, "Dreg", e.human, 1, 2)
        ally.inspired = true
        ally.scorched = 2
        ally.scorchTurns = 2
        ally.scorchSource = put(e, "Ayuela", e.cpu, 0, 2)
        e.buff(ally, 3)
        assertEquals(13, ally.power)
        e.suppress(ally)
        assertTrue(ally.suppressed)
        assertFalse(ally.inspired)
        assertEquals(10, ally.power)
        assertEquals(0, ally.scorchTurns)
        assertEquals(0, ally.scorched)
        assertNull(ally.scorchSource)
        e.effects.endSide(e.human)
        assertEquals(10, ally.power)
    }

    @Test fun zhiyaoThreeTurnBonusUsesBuffTriggersAndExpires() {
        val e = engine()
        val watcher = put(e, "Zhiyao", e.human, 0, 1)
        val opponent = put(e, "Dreg", e.cpu, 1, 1)
        e.effects.fireEnemyPlacement(opponent)
        assertEquals(11, watcher.power)
        assertEquals(1, watcher.bonus)
        assertEquals(1, watcher.timedBonuses.size)
        e.effects.endSide(e.human)
        e.effects.endSide(e.human)
        assertEquals(11, watcher.power)
        e.effects.endSide(e.human)
        assertEquals(10, watcher.power)
        assertEquals(0, watcher.bonus)
        assertTrue(watcher.timedBonuses.isEmpty())
    }

    @Test fun zhiyaoUsesNextBonusDoublerWithoutLeakingTemporaryPower() {
        val e = engine()
        put(e, "Tan-2", e.human, 0, 0)
        val watcher = put(e, "Zhiyao", e.human, 0, 1)
        val opponent = put(e, "Dreg", e.cpu, 1, 1)
        e.effects.fireEnemyPlacement(opponent)
        assertEquals(12, watcher.power)
        assertEquals(2, watcher.timedBonuses.single().value)
        repeat(3) { e.effects.endSide(e.human) }
        assertEquals(10, watcher.power)
    }

    @Test fun normalColumnUnitDoesNotCountAsPrintedLineUnit() {
        val e = engine()
        val legionary = put(e, "Legionary", e.human, 0, 1)
        assertEquals(0, e.lineLength(legionary))
        assertEquals(0, e.projectedFactionLine(e.human, legionary.card, 0, 2))
    }
}
