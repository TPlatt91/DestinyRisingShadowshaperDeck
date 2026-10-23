package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test

/**
 * A card's projected Power is not enough for CPU placement decisions:
 * summons and transfers also require space on the *opposing* Front Row.
 * These are local JVM regression tests and do not trigger GitHub Actions.
 */
class ShadowCpuOpponentCapacityTest {
    private fun card(name: String, faction: String, power: Int, effect: String,
                     classification: String = "") = ShadowData.Card().apply {
        this.name = name
        this.faction = faction
        this.power = power.toString()
        this.cost = "1"
        this.rarity = "Rare"
        this.effect = effect
        this.classification = classification
    }

    private fun fixture(): ShadowEngine {
        val catalog = listOf(
            card("Dreg", "Fallen", 2, "No special effects."),
            card("Human Guard", "Humans", 10, "No special effects."),
            card("Cursed Thrall", "Hive", 1,
                "When Played: Moves randomly to opponent's Front Row; detonates next turn."),
            card("Exploder Shank", "Fallen", 1,
                "When Played: Moves randomly to opponent's Front Row; detonates next turn."),
            card("Common Cover", "Humans", 1,
                "When Played (Front Row): Places 2 Rocks in random empty opposing Front Row spaces."),
            card("Ning Fei", "Humans", 1,
                "When Played: Summons a Clone to a random empty opposing Front Row space."),
            card("Rock", "Humans", 1, "No special effects.", "Summoned unit"),
            card("Clone", "Humans", 1,
                "When defeated: Deals 3 damage to adjacent allies.", "Summoned unit")
        ).associateBy { it.name }
        return ShadowEngine(catalog, 71L).apply {
            humanFirst = false
            humanTurn = false
            cpu.points = 1
        }
    }

    private fun fillOpponentFront(e: ShadowEngine, leaveOpen: Int = -1) {
        for (col in 0 until ShadowRules.COLUMNS) {
            if (col == leaveOpen) continue
            e.human.board[ShadowRules.FRONT_ROW][col] =
                ShadowEngine.Unit(e.human, e.catalog.getValue("Human Guard"),
                    ShadowRules.FRONT_ROW, col)
        }
    }

    private fun offer(e: ShadowEngine, special: String) {
        e.cpu.hand.add(e.catalog.getValue(special))
        e.cpu.hand.add(e.catalog.getValue("Dreg"))
    }

    private fun played(e: ShadowEngine, name: String): Boolean =
        e.journal.any { it.startsWith("CPU ▶ " + name + " • ") }

    @Test fun cpuPrefersDregWhenFullFrontBlocksEitherTransferCard() {
        for (name in listOf("Cursed Thrall", "Exploder Shank")) {
            val e = fixture()
            fillOpponentFront(e)
            offer(e, name)

            assertTrue(e.cpuPlayOne())

            assertTrue(played(e, "Dreg"))
            assertFalse(played(e, name))
            assertEquals(name, e.cpu.hand.single().name)
            assertEquals(0, e.rejectedCpuMoves)
            assertEquals(0, e.unresolvedEffects)
        }
    }

    @Test fun cpuUsesTransferCardWhenOpponentHasAFreeFrontSlot() {
        for (name in listOf("Cursed Thrall", "Exploder Shank")) {
            val e = fixture()
            fillOpponentFront(e, leaveOpen = 3)
            offer(e, name)

            assertTrue(e.cpuPlayOne())

            assertTrue(played(e, name))
            val arrival = e.human.board[ShadowRules.FRONT_ROW][3]
            assertEquals(name, arrival?.card?.name)
            assertSame(e.cpu, arrival?.fuseOrigin)
            assertEquals("Dreg", e.cpu.hand.single().name)
            assertEquals(0, e.rejectedCpuMoves)
            assertEquals(0, e.unresolvedEffects)
        }
    }

    @Test fun cpuPrioritizesCommonCoverOnlyWhenItCanPlaceRocks() {
        val blocked = fixture()
        fillOpponentFront(blocked)
        offer(blocked, "Common Cover")
        assertTrue(blocked.cpuPlayOne())
        assertTrue(played(blocked, "Dreg"))
        assertFalse(played(blocked, "Common Cover"))

        val available = fixture()
        fillOpponentFront(available, leaveOpen = 4)
        offer(available, "Common Cover")
        assertTrue(available.cpuPlayOne())
        assertTrue(played(available, "Common Cover"))
        assertEquals("Rock", available.human.board[ShadowRules.FRONT_ROW][4]?.card?.name)
        assertEquals(1, available.human.board[ShadowRules.FRONT_ROW]
            .filterNotNull().count { it.card.name == "Rock" })
        assertEquals(0, available.unresolvedEffects)
        assertEquals(0, available.rejectedCpuMoves)
    }

    @Test fun cpuPositionsCommonCoverFrontRowForItsPrintedEffect() {
        val e = fixture()
        offer(e, "Common Cover")

        assertTrue(e.cpuPlayOne())

        assertTrue(played(e, "Common Cover"))
        val source = e.cpu.board[ShadowRules.FRONT_ROW].filterNotNull()
            .single { it.card.name == "Common Cover" }
        assertEquals(ShadowRules.FRONT_ROW, source.row)
        assertEquals(2, e.human.board[ShadowRules.FRONT_ROW]
            .filterNotNull().count { it.card.name == "Rock" })
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun cpuPrefersNingFeiOnlyIfItsCloneCanEnter() {
        val blocked = fixture()
        fillOpponentFront(blocked)
        offer(blocked, "Ning Fei")
        assertTrue(blocked.cpuPlayOne())
        assertTrue(played(blocked, "Dreg"))

        val available = fixture()
        fillOpponentFront(available, leaveOpen = 2)
        offer(available, "Ning Fei")
        assertTrue(available.cpuPlayOne())
        assertTrue(played(available, "Ning Fei"))
        assertEquals("Clone",
            available.human.board[ShadowRules.FRONT_ROW][2]?.card?.name)
        assertEquals(0, available.unresolvedEffects)
        assertEquals(0, available.rejectedCpuMoves)
    }

    @Test fun unsuccessfulOpponentSummonsDoNotCountAsExtraPlacementRevisions() {
        val e = fixture()
        fillOpponentFront(e)
        e.cpu.hand.add(e.catalog.getValue("Common Cover"))
        e.cpu.hand.add(e.catalog.getValue("Ning Fei"))
        e.cpu.hand.add(e.catalog.getValue("Dreg"))

        assertTrue(e.cpuPlayOne())

        assertTrue(played(e, "Dreg"))
        assertEquals(1, e.placementRevision)
        assertEquals(0, e.unresolvedEffects)
        assertEquals(2, e.cpu.hand.size)
    }
}
