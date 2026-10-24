# Card viewer swipe navigation — Android portrait

## Behavior

In **Deck Construction** and **Card Collection**, tap an artwork thumbnail to open the full card-inspection dialog. Swipe **left** over the viewer to inspect the **next** card, or **right** for the **previous** card. The `PREV` / `NEXT` controls and `n / total` display provide the same navigation without gestures.

Navigation stays within the actual displayed list and ordering:
- Card Collection — filtered/sorted archive, including summon-only cards.
- Deck Construction: CARD GALLERY — filtered playable card gallery.
- Deck Construction: MY DECK — cards presently in the selected deck.

The expanded **VIEW LARGE CARD ART** dialog also supports left/right swipes. Switching cards does not collapse the large-art window. When the inspector changes cards, the scrolling printed-effect area returns to the top; vertical scrolling of long card text remains possible. Reaching the first or last card stops navigation rather than wrapping.

**Skirmish hand:** The ordinary hand is deliberately NOT swipe-navigable. Tapping continues to select the card and dragging continues to play it to a glowing legal board slot. Long-press a hand card to open its card-inspection popup; **only inside that popup** can a left/right swipe browse adjacent hand cards. The indexed hand position is tracked even when two copies share the same card name / catalog reference. Board-unit inspection is not changed to hand browsing.

## Implementation

- `DuelCardSwipe.kt` — horizontal gesture detector with a 56dp threshold, bounded adjacent-index arithmetic, small portrait pager controls, and explicit no-wrap behavior.
- `ShadowComposeActivity.kt` — captures the relevant filtered gallery or deck-tab list when the viewer opens; both normal and expanded viewers use the shared gesture; Add/Remove still apply only to the currently inspected card.
- `DuelCardInspection.kt` — allows swiping only when viewing a hand card with an active index, never when inspecting a battlefield unit.
- `ShadowBattleActivity.kt` — remembers the exact hand index on a long press and changes only the viewer's card/index on a swipe. Does not attach new horizontal gestures to `LazyRow`, its card tiles, `detectDragGestures`, or the board.
- `DuelCardSwipeNavigationTest.kt` and revised `DuelBinderDeckGuardTest.kt` — local regression checks for gesture direction, list edges, filter scope, duplicate index stability, retained drag-to-play, and binder compatibility.

No changes to card art or engine rules, no landscape mode, and no GitHub Actions tests.

## Local QA

After pulling `main`, run `run_local_engine_checks.bat`. On the phone:

1. Filter Collection by Hive and inspect a card. Swipe left/right; only filtered Hive cards should appear.
2. Open Deck Construction, repeat in the filtered gallery and in MY DECK. Verify Add/Remove adjusts the displayed card only.
3. Open VIEW LARGE CARD ART and swipe two cards while staying zoomed into the artwork.
4. In Skirmish, **tap** a hand card to select it; **drag** it to a valid row. Verify neither action changed.
5. Long-press a hand card, then swipe left/right in the enlarged viewer. Test first/last boundaries and duplicate copies.
6. Inspect a card already on the board; it should remain a view of that unit, with live Power and statuses.
7. Scroll a long card effect vertically; swiping horizontally should page, not scroll its text.

Only source-level audits are available in the current environment; a local Kotlin compile/unit-test run and Android-device touch test are still needed.
