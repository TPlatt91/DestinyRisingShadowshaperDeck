package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class DuelEffectEventsTest {
    private fun card(
        name: String, faction: String = "Hive", power: Int = 3,
        text: String = "No special effects."
    ) = ShadowData.Card().apply {
        this.name = name
        this.faction = faction
        this.power = power.toString()
        cost = "1"
        rarity = "Rare"
        effect = text
    }

    private fun engine(): ShadowEngine {
        val entries = listOf(
            card("The Disfavored", text =
                "Sustained (Front Row): When acquiring a buff, grants +2 Power to one of your different Hive units at random."),
            card("Thrall", power = 2),
            card("Hexguard", text = "When Played: Gains Inspired."),
            card("Rossi-11", "Humans", 2, "When Played: Grants an Overshield to an ally."),
            card("Ayuela", "Humans", 3, "Sustained (Back Row): Scorches enemies."),
            card("Dummy", "Humans", 4)
        ).associateBy { it.name }
        return ShadowEngine(entries, 14L)
    }

    private fun put(
        e: ShadowEngine, name: String, side: ShadowEngine.Player,
        row: Int = ShadowRules.FRONT_ROW, column: Int = 0
    ): ShadowEngine.Unit {
        val unit = ShadowEngine.Unit(side, e.catalog.getValue(name), row, column)
        side.board[row][column] = unit
        return unit
    }

    @Test fun scourgedStatusActuallyRecordsApplyAndDamageTick() {
        val e = engine()
        val target = put(e, "Thrall", e.human)
        val caster = put(e, "Ayuela", e.cpu, ShadowRules.BACK_ROW)
        e.addScorch(target, caster, damage = 1, turns = 2)
        assertTrue(target.scorchTurns > 0)
        assertEquals(DuelEffectKind.SCORCH_APPLIED, e.visualEvents.last().kind)
        assertSame(caster, e.visualEvents.last().source)
        val originalPower = target.power
        e.effects.endSide(e.human)
        assertTrue(target.power < originalPower)
        assertTrue(e.visualEvents.any {
            it.kind == DuelEffectKind.SCORCH_TICK && it.target === target && it.amount == 1
        })
    }

    @Test fun inspiredCreatesOnlyOneApplyEventAndPowerPulseOnEndSide() {
        val e = engine()
        val unit = put(e, "Hexguard", e.human)
        e.inspire(unit)
        e.inspire(unit)
        assertEquals(1, e.visualEvents.count { it.kind == DuelEffectKind.INSPIRED })
        assertTrue(unit.inspired)
        e.effects.endSide(e.human)
        assertTrue(e.visualEvents.any {
            it.kind == DuelEffectKind.POWER_GAINED && it.target === unit && it.amount == 1
        })
    }

    @Test fun suppressionRemovesScorchAndEmitsPurpleCue() {
        val e = engine()
        val target = put(e, "Thrall", e.human)
        val attacker = put(e, "Ayuela", e.cpu, ShadowRules.BACK_ROW)
        e.addScorch(target, attacker, 1, 2)
        e.inspire(target)
        e.suppress(target)
        assertTrue(target.suppressed)
        assertFalse(target.inspired)
        assertEquals(0, target.scorchTurns)
        assertEquals(DuelEffectKind.SUPPRESSED, e.visualEvents.last().kind)
    }

    @Test fun overshieldEmitsProtectionAndConsumptionWithoutSuppression() {
        val e = engine()
        val ally = put(e, "Thrall", e.human)
        val rossi = put(e, "Rossi-11", e.human, ShadowRules.BACK_ROW, 1)
        e.effects.onPlayed(rossi, ShadowEngine.Target(e.human, ally.row, ally.column))
        assertTrue(ally.overshield)
        assertTrue(e.visualEvents.any {
            it.kind == DuelEffectKind.OVERSHIELD_APPLIED &&
                it.target === ally && it.source === rossi
        })
        e.suppress(ally)
        assertFalse(ally.overshield)
        assertFalse(ally.suppressed)
        assertEquals(DuelEffectKind.OVERSHIELD_BLOCKED, e.visualEvents.last().kind)
    }

    @Test fun disfavoredPowerBonusProducesRealSourceToRecipientTransfer() {
        val e = engine()
        val disfavored = put(e, "The Disfavored", e.human)
        val thrall = put(e, "Thrall", e.human, ShadowRules.BACK_ROW)
        e.buff(disfavored, 1)
        assertEquals(4, thrall.power)
        val transfer = DuelEffectEvents.lastTransfer(e.visualEvents)!!
        assertEquals(DuelEffectKind.POWER_TRANSFER, transfer.kind)
        assertSame(disfavored, transfer.source)
        assertSame(thrall, transfer.target)
        assertEquals(2, transfer.amount)
        assertEquals(4, disfavored.power)
        assertEquals(0, e.unresolvedEffects)
    }

    @Test fun eventStreamIsBoundedAndUnitFilteredByIdentity() {
        val e = engine()
        val first = put(e, "Thrall", e.human)
        val second = put(e, "Thrall", e.cpu)
        repeat(120) { e.buff(first, 1) }
        assertEquals(DuelEffectEvents.MAX_HISTORY, e.visualEvents.size)
        assertEquals(120L, e.visualSerial)
        assertTrue(DuelEffectEvents.forUnit(e.visualEvents, second, 0).isEmpty())
        assertTrue(DuelEffectEvents.forUnit(e.visualEvents, first, 115L).size == 5)
    }

    @Test fun coordinatorPublishesShieldActivationEvenWithoutPowerChange() {
        val initial = engine()
        val coordinator = ShadowBattleCoordinator(initial.catalog, 14L)
        val e = coordinator.engine
        val ally = put(e, "Thrall", e.human)
        val rossi = put(e, "Rossi-11", e.human, ShadowRules.BACK_ROW, 1)
        coordinator.resetViewSnapshot()
        val before = coordinator.state.value

        e.effects.onPlayed(rossi, ShadowEngine.Target(e.human, ally.row, ally.column))
        coordinator.resetViewSnapshot()
        val after = coordinator.state.value

        assertEquals(before.playerPower, after.playerPower)
        assertEquals(before.revision, after.revision)
        assertTrue(after.visualRevision > before.visualRevision)
        assertEquals(DuelEffectKind.OVERSHIELD_APPLIED, e.visualEvents.last().kind)
    }

    @Test fun fxQualityModeCyclesAndDoesNotChangeEngineState() {
        assertEquals(DuelFxMode.REDUCED, DuelFxMode.FULL.next())
        assertEquals(DuelFxMode.OFF, DuelFxMode.REDUCED.next())
        assertEquals(DuelFxMode.FULL, DuelFxMode.OFF.next())
        val e = engine()
        val unit = put(e, "Thrall", e.human)
        val score = e.score(e.human)
        val mode = DuelFxMode.FULL.next().next()
        assertEquals(DuelFxMode.OFF, mode)
        assertEquals(score, e.score(e.human))
        assertEquals(0L, e.visualSerial)
        assertEquals(2, unit.power)
    }

    @Test fun frontendConsumesEventsWithoutMakingStatusChangesItself() {
        fun read(name: String): String = listOf(
            File("src/main/java/com/tplatt/shadowshaper/$name"),
            File("app/src/main/java/com/tplatt/shadowshaper/$name")
        ).firstOrNull { it.isFile }?.readText() ?: error("Missing $name")
        val fx = read("DuelStatusEffects.kt")
        val board = read("DuelBoardAnimations.kt")
        val battle = read("ShadowBattleActivity.kt")
        val coordinator = read("ShadowBattleCoordinator.kt")
        assertTrue(fx.contains("DuelEffectEvents.forUnit(events, unit, seenSerial)"))
        assertTrue(fx.contains("unit.scorchTurns > 0"))
        assertTrue(fx.contains("unit.overshield"))
        assertTrue(fx.contains("unit.suppressed"))
        assertTrue(fx.contains("unit.inspired"))
        assertTrue(fx.contains("DuelPowerTransferBeam("))
        assertTrue(board.contains("DuelCardStatusFx("))
        assertTrue(battle.contains("effectEvents = engine.visualEvents.filter { it.target === unit }"))
        assertTrue(battle.contains("DuelEffectEvents.lastTransfer(it.visualEvents)"))
        assertTrue(coordinator.contains("visualRevision = engine.visualSerial"))
        assertTrue(battle.contains("DuelFxSettings(context)"))
        assertTrue(battle.contains("fxMode = fxMode"))
        assertTrue(battle.contains("label = \"BATTLE EFFECTS\""))
        assertTrue(fx.contains("if (mode == DuelFxMode.OFF) return"))
        assertFalse(fx.contains("unit.overshield ="))
        assertFalse(fx.contains("unit.power +="))
    }
}
