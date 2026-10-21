package com.tplatt.shadowshaper

import org.junit.Assert.*
import org.junit.Test

/** Pure catalog checks; no Android runtime or GitHub Actions required. */
class DuelCoverCatalogTest {
    @Test fun allNineDocumentedCoversHaveDistinctSupportedIds() {
        assertEquals(9, DuelCoverCatalog.choices.size)
        val ids = DuelCoverCatalog.choices.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.all(DuelCoverCatalog::isKnown))
        assertTrue("default" in ids)
    }

    @Test fun displayNamesMatchTheSourceGallery() {
        assertEquals("Default", DuelCoverCatalog.title("default"))
        assertEquals("Elite Vandal", DuelCoverCatalog.title("elite_vandal"))
        assertEquals("Zhong Wenbin", DuelCoverCatalog.title("zhong_wenbin"))
        assertEquals("Default", DuelCoverCatalog.title("unknown_cover"))
    }
}
