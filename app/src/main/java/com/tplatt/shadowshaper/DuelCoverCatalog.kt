package com.tplatt.shadowshaper

/** Exact filenames verified in the public TodayInDestiny Shadowshaper Covers gallery. */
internal data class DuelCoverOption(val id: String, val title: String)

internal object DuelCoverCatalog {
    val choices: List<DuelCoverOption> = listOf(
        DuelCoverOption("default", "Default"),
        DuelCoverOption("denzin", "Denzin"),
        DuelCoverOption("elite_vandal", "Elite Vandal"),
        DuelCoverOption("marauder", "Marauder"),
        DuelCoverOption("morgran", "Morgran"),
        DuelCoverOption("sniper_vandal", "Sniper Vandal"),
        DuelCoverOption("tracer_shank", "Tracer Shank"),
        DuelCoverOption("wolf", "Wolf"),
        DuelCoverOption("zhong_wenbin", "Zhong Wenbin")
    )
    fun isKnown(id: String): Boolean = choices.any { it.id == id }
    fun title(id: String): String = choices.firstOrNull { it.id == id }?.title ?: "Default"
}
