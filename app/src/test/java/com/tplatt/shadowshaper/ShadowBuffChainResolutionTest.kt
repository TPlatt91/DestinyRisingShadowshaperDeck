package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test
import java.util.Collections
import java.util.Random

/**
 * Nested bonus effects: one causal buff chain, without suppressing separate
 * bonus events within the same turn. Run locally; no GitHub Actions needed.
 */
class ShadowBuffChainResolutionTest {
    private fun card(
        name: String, faction: String, power: Int, effect: String,
        classification: String = ""
    ) = ShadowData.Card().apply {
        this.name = name
        this.faction = faction
        this.power = power.toString()
        rarity = "Rare"
        cost = "1"
        this.effect = effect
        this.classification = classification
    }

    private fun engine(seed: Long = 7L): ShadowEngine {
        val cards = listOf(
            card("The Disfavored", "Hive", 3,
                "Sustained (Front Row): When acquiring a buff, grants +2 Power to one of your different Hive units at random."),
            card("Ogre", "Hive", 3,
                "Sustained: When acquiring a buff, deals 1 damage to 2 random opposing units."),
            card("Tenacious Ogre", "Hive", 5,
                "Sustained: When acquiring a buff, deals 1 damage to 4 random opposing units."),
            card("The Speaker", "Humans", 2,
                "Sustained (Back Row): When acquiring a Power bonus, grants +2 Power to the unit on the right."),
            card("Denzin", "Humans", 3,
                "Sustained: When acquiring a Power bonus, summons a Guerrilla to the left."),
            card("Xur", "Humans", 7,
                "Sustained: When acquiring a bonus, places a Rock in the opposing Front Row."),
            card("Guerrilla", "Humans", 4, "No special effects."),
            card("Grenade Dreg", "Fallen", 3,
                "Sustained: When an allied unit enters this row, gains +1 Power."),
            card("Zhiyao", "Humans", 3,
                "Sustained: When an enemy enters the Front Row, gains +1 Power for three turns."),
            card("Rock", "Humans", 1, "No special effects.", "Summoned unit"),
            card("Clone", "Humans", 1,
                "When defeated: deals 3 damage to units immediately left and right."),
            card("Dreg", "Fallen", 10, "No special effects.")
        ).associateBy { it.name }
        return ShadowEngine(cards, seed)
    }

    private fun place(
        e: ShadowEngine, name: String, side: ShadowEngine.Player,
        row: Int, column: Int, power: Int? = null
    ): ShadowEngine.Unit {
        val unit = ShadowEngine.Unit(side, e.catalog.getValue(name), row, column)
        if (power != null) unit.power = power
        side.board[row][column] = unit
        return unit
    }

    @Test fun twoDisfavoredCopiesResolveOneCausalChainWithoutInfiniteFeedback() {
        val e = engine()
        val first = place(e, "The Disfavored", e.human, 1, 0)
        val second = place(e, "The Disfavored", e.human, 1, 1)

        e.buff(first, 1)

        assertEquals(6, first.power) // +1 initiating buff, +2 from the second
        assertEquals(5, second.power) // +2 from the first
        assertEquals(3, e.journal.count { it.startsWith("BUFF +") })
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun independentBonusesRetriggerDisfavoredLaterInTheSameTurn() {
        val e = engine()
        val first = place(e, "The Disfavored", e.human, 1, 0)
        val second = place(e, "The Disfavored", e.human, 1, 1)

        e.buff(first, 1)
        e.buff(first, 1)

        assertEquals(9, first.power)
        assertEquals(7, second.power)
        assertEquals(6, e.journal.count { it.startsWith("BUFF +") })
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun twoInspiredDisfavoredSourcesStartSeparateEndTurnChains() {
        val e = engine()
        val first = place(e, "The Disfavored", e.human, 1, 0)
        val second = place(e, "The Disfavored", e.human, 1, 1)
        first.inspired = true
        second.inspired = true

        e.effects.endSide(e.human)

        // A first Inspired +1 triggers A->B->A. Later B's independent
        // Inspired +1 triggers B->A->B. Only the recursive repeats stop.
        assertEquals(8, first.power)
        assertEquals(8, second.power)
        assertEquals(6, e.journal.count { it.startsWith("BUFF +") })
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun oneDisfavoredCannotChooseItselfAsTheDifferentHiveTarget() {
        val e = engine()
        val only = place(e, "The Disfavored", e.human, 1, 0)

        e.buff(only, 1)

        assertEquals(4, only.power)
        assertEquals(1, e.journal.count { it.startsWith("BUFF +") })
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun disfavoredIsInactiveWhenSuppressedOrOutOfItsRequiredRow() {
        val suppressed = engine()
        val first = place(suppressed, "The Disfavored", suppressed.human, 1, 0)
        val target = place(suppressed, "The Disfavored", suppressed.human, 1, 1)
        suppressed.suppress(first)
        suppressed.buff(first, 1)
        assertEquals(4, first.power)
        assertEquals(3, target.power)

        val back = engine()
        val backDisfavored = place(back, "The Disfavored", back.human, 0, 0)
        val neighbor = place(back, "The Disfavored", back.human, 1, 1)
        back.buff(backDisfavored, 1)
        assertEquals(4, backDisfavored.power)
        assertEquals(3, neighbor.power)
    }

    @Test fun ogreSkipsNextTargetDefeatedByEarlierCloneSplash() {
        val seed = (0L..1000L).first { value ->
            val indices = arrayListOf(0, 1)
            Collections.shuffle(indices, Random(value))
            indices.first() == 0
        }
        val e = engine(seed)
        val ogre = place(e, "Ogre", e.human, 0, 3)
        place(e, "Clone", e.cpu, 0, 0)
        place(e, "Dreg", e.cpu, 0, 1, power = 2)

        e.buff(ogre, 1)

        assertNull(e.cpu.board[0][0])
        assertNull(e.cpu.board[0][1])
        assertEquals(0, e.journal.count { it == "DAMAGE 1 ▶ Dreg" })
        assertEquals(1, e.journal.count { it == "DAMAGE 3 ▶ Dreg" })
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun tenaciousOgreSkipsDefeatedTargetsWithinFourDistinctHits() {
        val seed = (0L..1000L).first { value ->
            val indices = arrayListOf(0, 1, 2, 3)
            Collections.shuffle(indices, Random(value))
            indices.first() == 0
        }
        val e = engine(seed)
        val ogre = place(e, "Tenacious Ogre", e.human, 0, 5)
        place(e, "Clone", e.cpu, 0, 0)
        place(e, "Dreg", e.cpu, 0, 1, power = 2)
        val third = place(e, "Dreg", e.cpu, 0, 2)
        val fourth = place(e, "Dreg", e.cpu, 0, 3)

        e.buff(ogre, 1)

        assertNull(e.cpu.board[0][0])
        assertNull(e.cpu.board[0][1])
        assertEquals(9, third.power)
        assertEquals(9, fourth.power)
        assertEquals(2, e.journal.count { it == "DAMAGE 1 ▶ Dreg" })
        assertEquals(1, e.journal.count { it == "DAMAGE 3 ▶ Dreg" })
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun nestedSpeakerPowerBonusesResolveAcrossAdjacentUnits() {
        val e = engine()
        val first = place(e, "The Speaker", e.human, 0, 0)
        val second = place(e, "The Speaker", e.human, 0, 1)
        val neighbor = place(e, "Dreg", e.human, 0, 2)

        e.buff(first, 1)

        assertEquals(3, first.power)
        assertEquals(4, second.power)
        assertEquals(12, neighbor.power)
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun buffedDenzinSummonsGuerrillaAndTriggersRowWatcherOnce() {
        val e = engine()
        val watcher = place(e, "Grenade Dreg", e.human, 1, 0)
        val denzin = place(e, "Denzin", e.human, 1, 2)

        e.buff(denzin, 1)

        assertEquals("Guerrilla", e.human.board[1][1]?.card?.name)
        assertEquals(4, watcher.power)
        assertEquals(1, watcher.bonus)
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun buffedXurSummonsRockAndTriggersEnemyArrivalReaction() {
        val e = engine()
        val xur = place(e, "Xur", e.human, 0, 2)
        val zhiyao = place(e, "Zhiyao", e.cpu, 0, 4)

        e.buff(xur, 1)

        assertEquals(1, e.cpu.board[1].filterNotNull().count { it.card.name == "Rock" })
        assertEquals(4, zhiyao.power)
        assertEquals(1, zhiyao.timedBonuses.size)
        assertEquals(0, e.unresolvedEffects)
    }
}
