package com.tplatt.shadowshaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Local JVM guards for viewer-only left/right paging, not card drag-to-play. */
class DuelCardSwipeNavigationTest {
    private fun source(file: String): String = listOf(
        File("src/main/java/com/tplatt/shadowshaper/$file"),
        File("app/src/main/java/com/tplatt/shadowshaper/$file")
    ).firstOrNull { it.isFile }?.readText() ?: error("Missing $file")

    @Test fun leftGoesToNextAndRightGoesToPrevious() {
        assertEquals(1, DuelCardSwipeNavigation.direction(-60f, 56f))
        assertEquals(-1, DuelCardSwipeNavigation.direction(60f, 56f))
        assertEquals(0, DuelCardSwipeNavigation.direction(-55.9f, 56f))
        assertEquals(0, DuelCardSwipeNavigation.direction(0f, 56f))
    }

    @Test fun adjacentStopsAtEndsWithoutWrapping() {
        assertNull(DuelCardSwipeNavigation.adjacent(0, 4, -1))
        assertEquals(1, DuelCardSwipeNavigation.adjacent(0, 4, 1))
        assertEquals(2, DuelCardSwipeNavigation.adjacent(3, 4, -1))
        assertNull(DuelCardSwipeNavigation.adjacent(3, 4, 1))
        assertNull(DuelCardSwipeNavigation.adjacent(0, 0, 1))
        assertNull(DuelCardSwipeNavigation.adjacent(-1, 4, 1))
        assertNull(DuelCardSwipeNavigation.adjacent(0, 4, 0))
    }

    @Test fun thresholdPreventsAccidentalShortSwipes() {
        assertEquals(0, DuelCardSwipeNavigation.direction(45f, 56f))
        assertEquals(0, DuelCardSwipeNavigation.direction(-45f, 56f))
        assertEquals(1, DuelCardSwipeNavigation.direction(-56f, 56f))
        assertEquals(-1, DuelCardSwipeNavigation.direction(56f, 56f))
    }

    @Test fun collectionAndBothDeckTabsPageThroughTheirVisibleCardsOnly() {
        val src = source("ShadowComposeActivity.kt")
        assertTrue(src.contains("onCard(card, filtered)"))
        assertTrue(src.contains("onCard(card, suggestions)"))
        assertTrue(src.contains("onCard(card, deckCards)"))
        assertTrue(src.contains("inspectionCards = visibleCards.toList()"))
        assertTrue(src.contains("inspectionCards.indexOfFirst { it.name == card.name }"))
        assertTrue(src.contains("DuelCardPagerControls("))
        assertTrue(src.contains(".duelSwipeToBrowse("))
        assertFalse(src.contains("onCard = { detail = it }"))
    }

    @Test fun swipingTheEnlargedArtKeepsItEnlarged() {
        val src = source("ShadowComposeActivity.kt")
        assertTrue(src.contains("var enlarged by remember { mutableStateOf(false) }"))
        assertFalse(src.contains("var enlarged by remember(card.name)"))
        assertTrue(src.contains("if (enlarged)"))
        assertTrue(src.contains("onNext = onNext"))
        assertTrue(src.contains("onPrevious = onPrevious"))
    }

    @Test fun skirmishHandSwipesOnlyInsideItsCardViewerNotTheHandItself() {
        val battle = source("ShadowBattleActivity.kt")
        val viewer = source("DuelCardInspection.kt")
        val swipe = source("DuelCardSwipe.kt")
        assertTrue(battle.contains("onLongClick = {"))
        assertTrue(battle.contains("inspectedHandIndex = index"))
        assertTrue(battle.contains("inspectedCard = card"))
        assertTrue(battle.contains("val hand = engine?.human?.hand.orEmpty()"))
        assertTrue(battle.contains("DuelCardSwipeNavigation.adjacent(handIndex, hand.size"))
        assertTrue(battle.contains("onClose = {"))
        assertTrue(battle.contains("inspectedHandIndex = -1"))
        assertTrue(viewer.contains("enabled = unit == null && browseIndex >= 0 && browseCount > 1"))
        assertTrue(viewer.contains("DuelCardPagerControls("))
        assertTrue(swipe.contains("detectHorizontalDragGestures"))
        assertTrue(battle.contains("detectDragGestures("))
        assertTrue(battle.contains("onGloballyPositioned { cardBounds[index]"))
        assertTrue(battle.contains("onDragEnd = {"))
        assertFalse(battle.contains(".duelSwipeToBrowse("))
    }

    @Test fun swipeNeverChangesDeckCountsOrGameEngineState() {
        val navigation = source("DuelCardSwipe.kt")
        assertFalse(navigation.contains("ShadowEngine("))
        assertFalse(navigation.contains("vm.add("))
        assertFalse(navigation.contains("vm.remove("))
        assertFalse(navigation.contains("playAt("))
        assertFalse(navigation.contains("engine.play("))
    }
}
