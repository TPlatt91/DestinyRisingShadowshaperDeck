package com.tplatt.shadowshaper

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Kotlin state boundary for the game engine during incremental Java -> Kotlin migration.
 *
 * All mutations go through this coordinator; current complex effect resolution
 * remains in the shared Kotlin ShadowEngine/ShadowCardEffects implementations,
 * with regression tests for each audited mechanic.
 */
data class ShadowBattleSnapshot(
    val round: Int,
    val playerTurn: Boolean,
    val finished: Boolean,
    val playerLight: Int,
    val cpuLight: Int,
    val playerPower: Int,
    val cpuPower: Int,
    val playerHand: Int,
    val cpuHand: Int,
    val unresolvedEffects: Int,
    val revision: Int,
    // Recompose the visual layer even when a status changes without Power.
    // The event stream is transient and intentionally omitted from saves.
    val visualRevision: Long,
    val pendingRelocation: Boolean,
    val lastError: String
)

class ShadowBattleCoordinator(catalog: Map<String, ShadowData.Card>, seed: Long) {
    @JvmField var engine = ShadowEngine(catalog, seed)
    private var checkpoint: ((ShadowEngine) -> Unit)? = null

    fun setCheckpointListener(listener: ((ShadowEngine) -> Unit)?) {
        checkpoint = listener
    }

    internal fun resume(saved: ShadowMatchState) {
        // Restore field/identity graph directly. Never call start() here:
        // that would reshuffle and reroll all subsequent random effects.
        engine = ShadowMatchCodec.restore(saved, engine.catalog)
        _state.value = snapshot()
    }
    private val _state = MutableStateFlow(snapshot())
    val state: StateFlow<ShadowBattleSnapshot> = _state.asStateFlow()

    fun snapshot(): ShadowBattleSnapshot = ShadowBattleSnapshot(
        round = engine.round,
        playerTurn = engine.humanTurn,
        finished = engine.finished,
        playerLight = engine.human.points,
        cpuLight = engine.cpu.points,
        playerPower = engine.score(engine.human),
        cpuPower = engine.score(engine.cpu),
        playerHand = engine.human.hand.size,
        cpuHand = engine.cpu.hand.size,
        unresolvedEffects = engine.unresolvedEffects,
        revision = engine.placementRevision,
        visualRevision = engine.visualSerial,
        pendingRelocation = engine.pendingMovedUnit != null,
        lastError = engine.lastActionError
    )

    private fun publish() {
        _state.value = snapshot()
        checkpoint?.invoke(engine)
    }

    fun start(player: Map<String, Int>, computer: Map<String, Int>) {
        engine.start(player, computer)
        publish()
    }

    fun play(
        player: ShadowEngine.Player,
        index: Int, row: Int, column: Int, target: ShadowEngine.Target?
    ): Boolean = engine.play(player, index, row, column, target).also { publish() }

    fun endTurn() {
        engine.endTurn()
        publish()
    }

    fun cpuPlayOne(): Boolean = engine.cpuPlayOne().also { publish() }

    fun resolveMove(player: ShadowEngine.Player, row: Int, column: Int): Boolean =
        engine.resolveMove(player, row, column).also { publish() }

    fun resetViewSnapshot() = publish()
}
