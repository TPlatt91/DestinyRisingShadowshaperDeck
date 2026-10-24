package com.tplatt.shadowshaper

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Local-only source regression checks for portrait leather-binder visuals.
 * All deck edits still pass through the existing ShadowDeckViewModel.
 */
class DuelBinderDeckGuardTest {
    private fun source(file: String): String =
        listOf(
            File("src/main/java/com/tplatt/shadowshaper/$file"),
            File("app/src/main/java/com/tplatt/shadowshaper/$file")
        ).firstOrNull(File::isFile)?.readText() ?: error("Missing source: $file")

    @Test fun deckGalleryHasBinderAndExactThreeColumnPortraitLayout() {
        val screen = source("ShadowComposeActivity.kt")
        val binder = source("DuelBinderGallery.kt")
        assertTrue(screen.contains("DuelBinderPage(Modifier.fillMaxSize())"))
        assertTrue(screen.contains("DuelBinderTab("))
        assertTrue(screen.contains("GridCells.Fixed(3)"))
        assertTrue(screen.contains("showDeckPane = false"))
        assertTrue(screen.contains("showDeckPane = true"))
        assertTrue(binder.contains("internal fun DuelBinderPage("))
        assertTrue(binder.contains("internal fun DuelBinderTab("))
        assertTrue(screen.contains("SCREEN_ORIENTATION_PORTRAIT"))
        assertFalse(screen.contains("a browseable card gallery at left, and the selected 15-card deck at right"))
    }

    @Test fun artworkIsFullyPresentInShadedBinderSleeves() {
        val binder = source("DuelBinderGallery.kt")
        val screen = source("ShadowComposeActivity.kt")
        assertTrue(binder.contains("internal fun DuelBinderCardSleeve("))
        assertTrue(binder.contains(".aspectRatio(.665f)"))
        assertTrue(binder.contains("artwork(Modifier.fillMaxSize())"))
        assertTrue(binder.contains("Modifier.fillMaxWidth().height(5.dp)"))
        assertTrue(binder.contains("card.isPlayable"))
        assertTrue(screen.contains("CardArtPreview(card, artModifier)"))
        assertTrue(screen.contains("DuelBinderCardSleeve("))
        assertTrue(binder.contains("onClick = onInspect"))
        assertTrue(binder.contains("onClick = onAdd"))
    }

    @Test fun leatherAndTrimAreOriginalCanvasDesignsWithoutExternalAssets() {
        val binder = source("DuelBinderGallery.kt")
        assertTrue(binder.contains("BinderPalette.leather"))
        assertTrue(binder.contains("BinderPalette.bronze"))
        assertTrue(binder.contains("Canvas(Modifier.fillMaxSize())"))
        assertTrue(binder.contains("while (y < size.height"))
        assertTrue(binder.contains("Brush.verticalGradient("))
        assertFalse(binder.contains("http://"))
        assertFalse(binder.contains("https://"))
        assertFalse(binder.contains("R.drawable."))
    }

    @Test fun addInspectSearchAndDeckManagementActionsUnchanged() {
        val screen = source("ShadowComposeActivity.kt")
        assertTrue(screen.contains("onInspect = { onCard(card) }"))
        assertTrue(screen.contains("onAdd = { vm.add(card) }"))
        assertTrue(screen.contains("vm.remove(card.name)"))
        assertTrue(screen.contains("LibrarySearchBar("))
        assertTrue(screen.contains("binder = true"))
        assertTrue(screen.contains("LibraryFilterSelector("))
        assertTrue(screen.contains("vm.importText(importText)"))
        assertTrue(screen.contains("vm.delete(); confirmDelete = false"))
        assertTrue(screen.contains("vm.refresh()"))
        assertTrue(screen.contains("onClick = onBattle"))
        assertTrue(screen.contains("enabled = legality.isEmpty()"))
    }

    @Test fun collectionRetainsOriginalCardGalleryPresentation() {
        val screen = source("ShadowComposeActivity.kt")
        assertTrue(screen.contains("private fun CollectionScreen("))
        assertTrue(screen.contains("LibraryCardFace(card, state.cards[card.name] ?: 0, { onCard(card) })"))
        assertTrue(screen.contains("binder: Boolean = false"))
        assertTrue(screen.contains("private fun LibraryFilterSelector("))
        assertTrue(screen.contains("private fun LibrarySearchBar("))
        assertTrue(screen.contains("CardDetailsDialog("))
    }
}
