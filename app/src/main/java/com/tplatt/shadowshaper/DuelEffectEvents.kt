package com.tplatt.shadowshaper

/**
 * Presentation-only, identity-based event stream. These records are never
 * used to resolve rules or choose random targets and are not saved with a
 * match. A resumed match starts with no stale animation events.
 */
internal enum class DuelEffectKind {
    SCORCH_APPLIED, SCORCH_TICK, SUPPRESSED, INSPIRED,
    OVERSHIELD_APPLIED, OVERSHIELD_BLOCKED, POWER_GAINED, POWER_TRANSFER
}

internal data class DuelEffectEvent(
    val serial: Long,
    val kind: DuelEffectKind,
    val target: ShadowEngine.Unit,
    val source: ShadowEngine.Unit? = null,
    val amount: Int = 0
)

/** Fixed upper bound prevents long-running matches retaining every event. */
internal object DuelEffectEvents {
    const val MAX_HISTORY = 96

    fun append(
        history: MutableList<DuelEffectEvent>,
        event: DuelEffectEvent
    ) {
        history.add(event)
        if (history.size > MAX_HISTORY) {
            history.subList(0, history.size - MAX_HISTORY).clear()
        }
    }

    fun forUnit(
        history: List<DuelEffectEvent>,
        unit: ShadowEngine.Unit,
        afterSerial: Long
    ): List<DuelEffectEvent> =
        history.filter { it.target === unit && it.serial > afterSerial }

    fun lastTransfer(
        history: List<DuelEffectEvent>
    ): DuelEffectEvent? = history.lastOrNull {
        it.kind == DuelEffectKind.POWER_TRANSFER &&
            it.source != null && it.source !== it.target
    }
}
