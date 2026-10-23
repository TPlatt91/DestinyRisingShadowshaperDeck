package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test

/**
 * Bazinet's printed "all opposing units with a bonus" resolves the
 * eligible group at activation. Later reactive Power grants do not
 * retroactively join the original damage wave.
 */
class ShadowBazinetTargetSnapshotTest {
    private fun card(
        name: String, faction: String, power: Int, effect: String,
        classification: String = ""
    ): ShadowData.Card = ShadowData.Card().apply {
        this.name = name
        this.faction = faction
        this.power = power.toString()
        cost = "1"
        rarity = "Rare"
        this.effect = effect
        this.classification = classification
    }

    private fun fixture(): ShadowEngine {
        val cards = listOf(
            card("Bazinet", "Humans", 2,
                "When Played (Back Row): Deals 3 damage to all opposing units with a bonus."),
            card("Xur", "Humans", 7,
                "Sustained: When acquiring a bonus, places a Rock in the opposing Front Row."),
            card("Saladin", "Humans", 6,
                "Sustained: When one of your Human units takes damage, gains +3 Power."),
            card("Zhiyao", "Humans", 3,
                "Sustained: When an opponent plays a Front Row unit, gain +1 Power for 3 turns."),
            card("Rock", "Humans", 1, "No special effects.", "Summoned unit"),
            card("Dreg", "Fallen", 12, "No special effects.")
        ).associateBy { it.name }
        return ShadowEngine(cards, 129L).apply {
            humanFirst = true
            humanTurn = true
            human.points = 20
        }
    }

    private fun put(
        e: ShadowEngine, name: String, side: ShadowEngine.Player,
        row: Int, col: Int
    ): ShadowEngine.Unit =
        ShadowEngine.Unit(side, e.catalog.getValue(name), row, col).also {
            side.board[row][col] = it
        }

    private fun playBazinet(e: ShadowEngine, col: Int) {
        e.human.hand.add(e.catalog.getValue("Bazinet"))
        assertTrue(e.lastActionError, e.play(
            e.human, e.human.hand.lastIndex,
            ShadowRules.BACK_ROW, col, null
        ))
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun reactiveZhiyaoBonusDoesNotCreateANewBazinetTargetMidWave() {
        val e = fixture()
        put(e, "Saladin", e.cpu, 0, 0)
        val xur = put(e, "Xur", e.cpu, 0, 1)
        val zhiyao = put(e, "Zhiyao", e.cpu, 0, 2)
        // This is a pre-existing bonus, not a new Xur activation.
        xur.power++
        xur.bonus++

        assertEquals(1, e.powerBonus(xur))
        assertEquals(0, e.powerBonus(zhiyao))
        playBazinet(e, 4)

        // Bazinet hits the pre-buffed Xur. Saladin buffs the damaged
        // Human Xur, which summons Rock on the opposite side. That new
        // enemy arrival buffs CPU Zhiyao, but it wasn't eligible when
        // Bazinet's original targets were determined.
        assertEquals(8, xur.power) // base 7 +1 -3 +3
        assertEquals(4, zhiyao.power)
        assertEquals(1, zhiyao.bonus)
        assertEquals(1, e.human.board[1].filterNotNull()
            .count { it.card.name == "Rock" })
        assertEquals(1, e.journal.count { it == "DAMAGE 3 ▶ Xur" })
        assertEquals(0, e.journal.count { it == "DAMAGE 3 ▶ Zhiyao" })
    }

    @Test fun newlyBuffedZhiyaoIsEligibleForANewBazinetPlay() {
        val e = fixture()
        put(e, "Saladin", e.cpu, 0, 0)
        val xur = put(e, "Xur", e.cpu, 0, 1)
        val zhiyao = put(e, "Zhiyao", e.cpu, 0, 2)
        xur.power++
        xur.bonus++

        playBazinet(e, 4)
        assertEquals(0, e.journal.count { it == "DAMAGE 3 ▶ Zhiyao" })

        playBazinet(e, 5)
        assertEquals(1, e.journal.count { it == "DAMAGE 3 ▶ Zhiyao" })
        assertEquals(2, e.human.board[0].filterNotNull()
            .count { it.card.name == "Bazinet" })
    }

    @Test fun allInitiallyBonusBearingEnemiesAreHitExactlyOnce() {
        val e = fixture()
        val first = put(e, "Dreg", e.cpu, 1, 0)
        val second = put(e, "Dreg", e.cpu, 1, 1)
        val untouched = put(e, "Dreg", e.cpu, 1, 2)
        first.power += 2
        first.bonus += 2
        second.power += 2
        second.bonus += 2

        playBazinet(e, 0)

        assertEquals(11, first.power)
        assertEquals(11, second.power)
        assertEquals(12, untouched.power)
        assertEquals(2, e.journal.count { it == "DAMAGE 3 ▶ Dreg" })
    }
}
