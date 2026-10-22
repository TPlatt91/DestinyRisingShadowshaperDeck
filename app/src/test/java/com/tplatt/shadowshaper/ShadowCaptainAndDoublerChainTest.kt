package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test

/**
 * Local-only regression coverage: Fallens' summon-on-damage and deployment
 * bonuses, Tan-2 next-bonus consumption, and Jolder next-damage consumption.
 * Uses the real engine's nested trigger dispatch, without GitHub Actions.
 */
class ShadowCaptainAndDoublerChainTest {
    private fun card(
        name: String, faction: String, power: Int, text: String
    ): ShadowData.Card = ShadowData.Card().apply {
        this.name = name
        this.faction = faction
        this.power = power.toString()
        cost = "1"
        rarity = "Rare"
        effect = text
    }

    private fun fixture(): ShadowEngine {
        val cards = listOf(
            card("Resilient Captain", "Fallen", 1,
                "Sustained: When an allied Fallen deals damage, summons a Dreg on the right, maximum six."),
            card("Unrelenting Captain", "Fallen", 1,
                "Sustained: Every Dreg deployed gains 1 Power."),
            card("Tan-2", "Humans", 2,
                "Sustained: Doubles your next Power bonus."),
            card("Jolder", "Humans", 3,
                "Sustained: Doubles the next damage event."),
            card("Elite Sniper Vandal", "Fallen", 6,
                "Sustained (Back Row): Deals 1 damage when an opponent gains a Power bonus."),
            card("Shinka Phalanx", "Cabal", 4,
                "Sustained (Column): Reduces 1 damage to a unit in its column."),
            card("Tracker", "Fallen", 10, "No special effects."),
            card("Dreg", "Fallen", 1, "No special effects."),
            card("Thrall", "Hive", 100, "No special effects.")
        ).associateBy { it.name }
        return ShadowEngine(cards, 43L).apply {
            humanFirst = true
            humanTurn = true
            human.points = 30
            cpu.points = 30
        }
    }

    private fun put(
        engine: ShadowEngine, name: String, side: ShadowEngine.Player,
        row: Int, column: Int
    ): ShadowEngine.Unit {
        val unit = ShadowEngine.Unit(side, engine.catalog.getValue(name), row, column)
        side.board[row][column] = unit
        return unit
    }

    @Test fun twoUnrelentingCaptainsGrantTwoSeparateBonuses() {
        val e = fixture()
        put(e, "Unrelenting Captain", e.human, 0, 0)
        put(e, "Unrelenting Captain", e.human, 1, 0)

        assertTrue(e.spawnAt(e.human, 0, 2, "Dreg"))

        val dreg = e.human.board[0][2]!!
        assertEquals(3, dreg.power) // one base +1 +1
        assertEquals(2, dreg.bonus)
        assertEquals(2, e.journal.count { it == "BUFF +1 ▶ Dreg" })
    }

    @Test fun tan2DoublesOnlyFirstOfTwoCaptainBonuses() {
        val e = fixture()
        val doubler = put(e, "Tan-2", e.human, 1, 4)
        put(e, "Unrelenting Captain", e.human, 0, 0)
        put(e, "Unrelenting Captain", e.human, 1, 0)

        assertTrue(e.spawnAt(e.human, 0, 2, "Dreg"))

        val dreg = e.human.board[0][2]!!
        assertEquals(4, dreg.power) // base 1 + doubled 2 + normal 1
        assertEquals(3, dreg.bonus)
        assertTrue(doubler.bonusDoublerConsumed)
        assertEquals(1, e.journal.count { it == "BUFF +2 ▶ Dreg" })
        assertEquals(1, e.journal.count { it == "BUFF +1 ▶ Dreg" })
    }

    @Test fun fatalSniperResponseStopsSecondCaptainBonusOnDeadDreg() {
        val e = fixture()
        put(e, "Unrelenting Captain", e.human, 0, 0)
        put(e, "Unrelenting Captain", e.human, 1, 0)
        put(e, "Elite Sniper Vandal", e.cpu, 0, 0)
        put(e, "Elite Sniper Vandal", e.cpu, 0, 1)

        assertTrue(e.spawnAt(e.human, 0, 2, "Dreg"))

        assertNull(e.human.board[0][2])
        assertEquals(1, e.journal.count { it == "BUFF +1 ▶ Dreg" })
        assertEquals(1, e.journal.count { it == "DESTROYED ▶ Dreg" })
    }

    @Test fun resilientCaptainCountsSummonsEvenWhenSnipersInstantlyKillEachDreg() {
        val e = fixture()
        val captain = put(e, "Resilient Captain", e.human, 0, 1)
        put(e, "Unrelenting Captain", e.human, 1, 1)
        val attacker = put(e, "Tracker", e.human, 1, 5)
        val victim = put(e, "Thrall", e.cpu, 1, 5)
        put(e, "Elite Sniper Vandal", e.cpu, 0, 0)
        put(e, "Elite Sniper Vandal", e.cpu, 0, 1)

        repeat(7) { e.damage(attacker, victim, 1) }

        assertEquals(6, captain.resilientCaptainDregs)
        assertNull(e.human.board[0][2])
        assertEquals(6, e.journal.count { it.startsWith("SUMMON ▶ Dreg ") })
        assertEquals(6, e.journal.count { it == "DESTROYED ▶ Dreg" })
        assertEquals(6, e.placementRevision)
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun blockedDamageDoesNotActivateResilientCaptain() {
        val e = fixture()
        val captain = put(e, "Resilient Captain", e.human, 0, 1)
        val attacker = put(e, "Tracker", e.human, 1, 5)
        val victim = put(e, "Thrall", e.cpu, 1, 0)
        victim.overshield = true

        e.damage(attacker, victim, 2)
        assertEquals(0, captain.resilientCaptainDregs)
        assertEquals(0, e.placementRevision)
        assertNull(e.human.board[0][2])

        e.damage(attacker, victim, 2)
        assertEquals(1, captain.resilientCaptainDregs)
        assertEquals("Dreg", e.human.board[0][2]?.card?.name)
    }

    @Test fun blockedRightSlotDoesNotConsumeSummonAllowance() {
        val e = fixture()
        val captain = put(e, "Resilient Captain", e.human, 0, 1)
        val occupant = put(e, "Tracker", e.human, 0, 2)
        val attacker = put(e, "Tracker", e.human, 1, 5)
        val victim = put(e, "Thrall", e.cpu, 1, 0)

        repeat(3) { e.damage(attacker, victim, 1) }

        assertSame(occupant, e.human.board[0][2])
        assertEquals(0, captain.resilientCaptainDregs)
        assertEquals(0, e.placementRevision)
    }

    @Test fun twoResilientCaptainsReceiveIndependentSummonCounts() {
        val e = fixture()
        val first = put(e, "Resilient Captain", e.human, 0, 1)
        val second = put(e, "Resilient Captain", e.human, 1, 1)
        val attacker = put(e, "Tracker", e.human, 1, 5)
        val victim = put(e, "Thrall", e.cpu, 1, 0)

        e.damage(attacker, victim, 1)

        assertEquals(1, first.resilientCaptainDregs)
        assertEquals(1, second.resilientCaptainDregs)
        assertEquals("Dreg", e.human.board[0][2]?.card?.name)
        assertEquals("Dreg", e.human.board[1][2]?.card?.name)
        assertEquals(2, e.placementRevision)
    }

    @Test fun successfulAndRejectedSpawnsReportPlacementAccurately() {
        val e = fixture()
        assertFalse(e.spawnAt(e.human, -1, 1, "Dreg"))
        assertFalse(e.spawnAt(e.human, 1, -1, "Dreg"))
        assertFalse(e.spawnAt(e.human, 1, 1, "Missing"))
        assertEquals(0, e.placementRevision)
        assertTrue(e.spawnAt(e.human, 1, 1, "Dreg"))
        assertFalse(e.spawnAt(e.human, 1, 1, "Dreg"))
        assertEquals(1, e.placementRevision)
        assertEquals(0, e.lastPlacedSide)
        assertEquals(1, e.lastPlacedRow)
        assertEquals(1, e.lastPlacedColumn)
    }

    @Test fun jolderIgnoresShieldedDamageThenDoublesFirstRealHit() {
        val e = fixture()
        val jolder = put(e, "Jolder", e.human, 0, 0)
        val attacker = put(e, "Tracker", e.human, 1, 4)
        val victim = put(e, "Thrall", e.cpu, 1, 3)
        victim.overshield = true

        e.damage(attacker, victim, 2)
        assertEquals(100, victim.power)
        assertFalse(jolder.damageDoublerConsumed)

        e.damage(attacker, victim, 2)
        assertEquals(96, victim.power)
        assertTrue(jolder.damageDoublerConsumed)

        e.damage(attacker, victim, 2)
        assertEquals(94, victim.power)
    }

    @Test fun twoJoldersDoubleDifferentConsecutiveDamageEvents() {
        val e = fixture()
        val first = put(e, "Jolder", e.human, 0, 0)
        val second = put(e, "Jolder", e.human, 0, 1)
        val attacker = put(e, "Tracker", e.human, 1, 4)
        val victim = put(e, "Thrall", e.cpu, 1, 3)

        e.damage(attacker, victim, 2)
        e.damage(attacker, victim, 2)
        e.damage(attacker, victim, 2)

        assertTrue(first.damageDoublerConsumed)
        assertTrue(second.damageDoublerConsumed)
        assertEquals(90, victim.power) // 4 + 4 + 2
    }

    @Test fun damagePreventedByReductionDoesNotConsumeJolder() {
        val e = fixture()
        val jolder = put(e, "Jolder", e.human, 0, 0)
        val attacker = put(e, "Tracker", e.human, 1, 5)
        put(e, "Shinka Phalanx", e.cpu, 0, 2)
        val protected = put(e, "Thrall", e.cpu, 1, 2)
        val unprotected = put(e, "Thrall", e.cpu, 1, 3)

        e.damage(attacker, protected, 1)
        assertEquals(100, protected.power)
        assertFalse(jolder.damageDoublerConsumed)

        e.damage(attacker, unprotected, 1)
        assertEquals(98, unprotected.power)
        assertTrue(jolder.damageDoublerConsumed)
    }

    @Test fun nestedJolderDamageAndTan2ArrivalBonusBothConsumeExactlyOnce() {
        val e = fixture()
        val jolder = put(e, "Jolder", e.human, 0, 0)
        val tan2 = put(e, "Tan-2", e.human, 1, 0)
        val captain = put(e, "Resilient Captain", e.human, 0, 1)
        put(e, "Unrelenting Captain", e.human, 1, 1)
        val attacker = put(e, "Tracker", e.human, 1, 5)
        val victim = put(e, "Thrall", e.cpu, 1, 3)

        e.damage(attacker, victim, 1)

        assertEquals(98, victim.power)
        assertEquals(3, e.human.board[0][2]?.power)
        assertEquals(1, captain.resilientCaptainDregs)
        assertTrue(jolder.damageDoublerConsumed)
        assertTrue(tan2.bonusDoublerConsumed)
        assertEquals(1, e.journal.count { it == "BUFF +2 ▶ Dreg" })
        assertEquals(1, e.placementRevision)
        assertEquals(0, e.unresolvedEffects)
    }
}
