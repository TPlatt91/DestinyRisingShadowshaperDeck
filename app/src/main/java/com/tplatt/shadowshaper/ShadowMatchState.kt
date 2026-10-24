package com.tplatt.shadowshaper

import java.io.Serializable
import java.util.IdentityHashMap
import java.util.Random

/**
 * Exact logical match checkpoint. No Compose state or asset bitmaps live here.
 * Unit identities are indexed explicitly: scorch sources and sniper history
 * must retain object identity even when the source has left the board.
 */
internal data class ShadowMatchState(
    val format: Int,
    val seed: Long,
    val randomState: Random,
    val humanFirst: Boolean,
    val humanTurn: Boolean,
    val finished: Boolean,
    val round: Int,
    val completedTurns: Int,
    val unresolvedEffects: Int,
    val unresolvedDetails: List<String>,
    val rejectedCpuMoves: Int,
    val lastActionError: String,
    val lastPlacedSide: Int,
    val lastPlacedRow: Int,
    val lastPlacedColumn: Int,
    val placementRevision: Int,
    val humanDrawTotal: Int,
    val cpuDrawTotal: Int,
    val humanEmptyDrawTotal: Int,
    val cpuEmptyDrawTotal: Int,
    val journal: List<String>,
    val human: SideState,
    val cpu: SideState,
    val units: List<UnitState>,
    val pendingUnit: Int?,
    val pendingCaster: Int?
) : Serializable {
    companion object { const val VERSION = 1 }
}

internal data class SideState(
    val points: Int,
    val deck: List<String>,
    val hand: List<String>,
    val board: List<Int?>
) : Serializable

internal data class UnitState(
    val id: Int,
    val side: Int,
    val card: String,
    val row: Int,
    val column: Int,
    val power: Int,
    val bonus: Int,
    val scorched: Int,
    val scorchTurns: Int,
    val scorchSource: Int?,
    val scorchEffects: List<ScorchState>,
    val timedBonuses: List<BonusState>,
    val inspired: Boolean,
    val suppressed: Boolean,
    val overshield: Boolean,
    val resilientCaptainDregs: Int,
    val damageBonus: Int,
    val bonusDoublerConsumed: Boolean,
    val damageDoublerConsumed: Boolean,
    val sniperTurnsRemaining: Int,
    val sniperLastHits: List<HitState>,
    val fuseOrigin: Int?,
    val fuseRound: Int
) : Serializable

internal data class BonusState(val value: Int, val turns: Int) : Serializable
internal data class ScorchState(
    val source: Int?, val damage: Int, val turns: Int
) : Serializable
internal data class HitState(val target: Int, val turn: Int) : Serializable

/** The saved-match wrapper carries the deck choices needed for a fresh replay. */
internal data class ShadowSavedMatch(
    val format: Int = ShadowMatchState.VERSION,
    val deckName: String,
    val coverId: String,
    val playerDeck: Map<String, Int>,
    val cpuFaction: String,
    val engine: ShadowMatchState
) : Serializable

/**
 * Capture and restore COMPLETE game logic without replaying effect triggers.
 * This does not invoke start(), play(), draw(), or endTurn() during restore.
 */
internal object ShadowMatchCodec {
    fun capture(e: ShadowEngine): ShadowMatchState {
        val ids = IdentityHashMap<ShadowEngine.Unit, Int>()
        val discovered = ArrayList<ShadowEngine.Unit>()
        fun register(unit: ShadowEngine.Unit?): Int? {
            if (unit == null) return null
            ids[unit]?.let { return it }
            val id = discovered.size
            ids[unit] = id
            discovered.add(unit)
            return id
        }

        // Board reading order is stable: player first, CPU second.
        for (side in listOf(e.human, e.cpu))
            for (row in side.board)
                for (unit in row) register(unit)
        register(e.pendingMovedUnit)
        // Follow all references, including those pointing at destroyed units.
        var index = 0
        while (index < discovered.size) {
            val unit = discovered[index++]
            register(unit.scorchSource)
            for (effect in unit.scorchEffects) register(effect.source)
            for (target in unit.sniperLastHitTurn.keys) register(target)
        }

        val states = discovered.map { unit ->
            UnitState(
                id = ids.getValue(unit),
                side = if (unit.owner === e.human) 0 else 1,
                card = unit.card.name,
                row = unit.row, column = unit.column,
                power = unit.power, bonus = unit.bonus,
                scorched = unit.scorched, scorchTurns = unit.scorchTurns,
                scorchSource = register(unit.scorchSource),
                scorchEffects = unit.scorchEffects.map {
                    ScorchState(register(it.source), it.damage, it.turns)
                },
                timedBonuses = unit.timedBonuses.map { BonusState(it.value, it.turns) },
                inspired = unit.inspired, suppressed = unit.suppressed,
                overshield = unit.overshield,
                resilientCaptainDregs = unit.resilientCaptainDregs,
                damageBonus = unit.damageBonus,
                bonusDoublerConsumed = unit.bonusDoublerConsumed,
                damageDoublerConsumed = unit.damageDoublerConsumed,
                sniperTurnsRemaining = unit.sniperTurnsRemaining,
                sniperLastHits = unit.sniperLastHitTurn.entries.map {
                    HitState(register(it.key)!!, it.value)
                },
                fuseOrigin = if (unit.fuseOrigin == null) null
                    else if (unit.fuseOrigin === e.human) 0 else 1,
                fuseRound = unit.fuseRound
            )
        }

        fun sideState(side: ShadowEngine.Player): SideState = SideState(
            points = side.points,
            deck = side.drawPile.map { it.name },
            hand = side.hand.map { it.name },
            board = side.board.flatMap { row -> row.map { register(it) } }
        )
        return ShadowMatchState(
            format = ShadowMatchState.VERSION,
            seed = e.seed,
            randomState = e.rng,
            humanFirst = e.humanFirst, humanTurn = e.humanTurn,
            finished = e.finished, round = e.round,
            completedTurns = e.completedTurns,
            unresolvedEffects = e.unresolvedEffects,
            unresolvedDetails = e.unresolvedEffectDetails.toList(),
            rejectedCpuMoves = e.rejectedCpuMoves,
            lastActionError = e.lastActionError,
            lastPlacedSide = e.lastPlacedSide,
            lastPlacedRow = e.lastPlacedRow,
            lastPlacedColumn = e.lastPlacedColumn,
            placementRevision = e.placementRevision,
            humanDrawTotal = e.humanDrawTotal,
            cpuDrawTotal = e.cpuDrawTotal,
            humanEmptyDrawTotal = e.humanEmptyDrawTotal,
            cpuEmptyDrawTotal = e.cpuEmptyDrawTotal,
            journal = e.journal.toList(),
            human = sideState(e.human), cpu = sideState(e.cpu),
            units = states,
            pendingUnit = register(e.pendingMovedUnit),
            pendingCaster = when (e.pendingMoveCaster) {
                e.human -> 0
                e.cpu -> 1
                else -> null
            }
        )
    }

    fun restore(state: ShadowMatchState, catalog: Map<String, ShadowData.Card>): ShadowEngine {
        require(state.format == ShadowMatchState.VERSION) { "Unsupported saved-match version" }
        require(state.round in 1..ShadowRules.ROUNDS)
        require(state.human.board.size == ShadowRules.ROWS * ShadowRules.COLUMNS)
        require(state.cpu.board.size == ShadowRules.ROWS * ShadowRules.COLUMNS)
        val e = ShadowEngine(catalog, state.seed)
        fun card(name: String): ShadowData.Card =
            catalog[name] ?: throw IllegalArgumentException("Missing saved card: $name")
        val byId = HashMap<Int, ShadowEngine.Unit>()
        fun side(index: Int): ShadowEngine.Player = when (index) {
            0 -> e.human
            1 -> e.cpu
            else -> throw IllegalArgumentException("Invalid saved owner $index")
        }
        // First pass constructs every identity, including off-board effect
        // sources. Second pass reconnects references between those identities.
        for (it in state.units) {
            require(it.id !in byId)
            byId[it.id] = ShadowEngine.Unit(side(it.side), card(it.card), it.row, it.column)
        }
        fun unit(id: Int?): ShadowEngine.Unit? =
            if (id == null) null else byId[id]
                ?: throw IllegalArgumentException("Missing referenced saved unit $id")
        for (it in state.units) {
            val target = unit(it.id)!!
            target.power = it.power
            target.bonus = it.bonus
            target.scorched = it.scorched
            target.scorchTurns = it.scorchTurns
            target.scorchSource = unit(it.scorchSource)
            target.scorchEffects.clear()
            for (effect in it.scorchEffects) {
                target.scorchEffects.add(ShadowEngine.ScorchEffect(
                    unit(effect.source), effect.damage, effect.turns
                ))
            }
            target.timedBonuses.clear()
            for (bonus in it.timedBonuses)
                target.timedBonuses.add(ShadowEngine.TimedBonus(bonus.value, bonus.turns))
            target.inspired = it.inspired
            target.suppressed = it.suppressed
            target.overshield = it.overshield
            target.resilientCaptainDregs = it.resilientCaptainDregs
            target.damageBonus = it.damageBonus
            target.bonusDoublerConsumed = it.bonusDoublerConsumed
            target.damageDoublerConsumed = it.damageDoublerConsumed
            target.sniperTurnsRemaining = it.sniperTurnsRemaining
            target.sniperLastHitTurn.clear()
            for (hit in it.sniperLastHits) target.sniperLastHitTurn[unit(hit.target)!!] = hit.turn
            target.fuseOrigin = it.fuseOrigin?.let(::side)
            target.fuseRound = it.fuseRound
        }
        fun restoreSide(target: ShadowEngine.Player, saved: SideState) {
            target.points = saved.points
            target.drawPile.clear()
            target.drawPile.addAll(saved.deck.map(::card))
            target.hand.clear()
            target.hand.addAll(saved.hand.map(::card))
            for (row in 0 until ShadowRules.ROWS) {
                for (column in 0 until ShadowRules.COLUMNS) {
                    val restored = unit(saved.board[row * ShadowRules.COLUMNS + column])
                    require(restored == null || restored.owner === target) {
                        "Saved unit has incorrect side"
                    }
                    target.board[row][column] = restored
                }
            }
        }
        restoreSide(e.human, state.human)
        restoreSide(e.cpu, state.cpu)
        e.rng = state.randomState
        e.humanFirst = state.humanFirst
        e.humanTurn = state.humanTurn
        e.finished = state.finished
        e.round = state.round
        e.completedTurns = state.completedTurns
        e.unresolvedEffects = state.unresolvedEffects
        e.unresolvedEffectDetails.clear()
        e.unresolvedEffectDetails.addAll(state.unresolvedDetails)
        e.rejectedCpuMoves = state.rejectedCpuMoves
        e.lastActionError = state.lastActionError
        e.lastPlacedSide = state.lastPlacedSide
        e.lastPlacedRow = state.lastPlacedRow
        e.lastPlacedColumn = state.lastPlacedColumn
        e.placementRevision = state.placementRevision
        e.humanDrawTotal = state.humanDrawTotal
        e.cpuDrawTotal = state.cpuDrawTotal
        e.humanEmptyDrawTotal = state.humanEmptyDrawTotal
        e.cpuEmptyDrawTotal = state.cpuEmptyDrawTotal
        e.journal.clear()
        e.journal.addAll(state.journal)
        e.pendingMovedUnit = unit(state.pendingUnit)
        e.pendingMoveCaster = state.pendingCaster?.let(::side)
        return e
    }
}
