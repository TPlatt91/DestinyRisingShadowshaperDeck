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
    val pendingRelocation: Boolean,
    val lastError: String
)

class ShadowBattleCoordinator(catalog: Map<String, ShadowData.Card>, seed: Long) {
    @JvmField val engine = ShadowEngine(catalog, seed)
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
        pendingRelocation = engine.pendingMovedUnit != null,
        lastError = engine.lastActionError
    )

    private fun publish() { _state.value = snapshot() }

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
