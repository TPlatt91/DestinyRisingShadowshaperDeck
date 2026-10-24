# Shadow Shaper Duels — Deck Construction leather-binder presentation

## Reference and scope

The player provided a screenshot of the app's current three-column portrait deck builder and a Destiny: Rising deck editor reference. The specific requirement was the *feel of cards sitting inside a leather binder*, not a port of the reference's landscape two-column deck overview. The requested functional and layout constraints were retained.

## Visual implementation

The new `DuelBinderGallery.kt` contains four local Compose-only elements:

- `DuelBinderPage`: dark brown/black leather leaf, softly varying directional gradients, deterministic low-opacity leather grain, stitched side seams, a narrow inset brass border, and four subtle rivets.
- `DuelBinderTab`: chamfered bronze-edged Card Gallery and My Deck page tabs with a warmer active face, engraved underscore, and gentle press response.
- `DuelBinderCardSleeve`: each card sits inside an individually recessed black pocket with thin bronze edging, four corner keepers, a faint sleeve-film reflection, and an unobtrusive five-dp leather retaining lip. The full original art is displayed with `CardArtPreview` and isn't color-corrected or replaced.
- `BinderPalette`: centralized leather/bronze/ivory colors for consistent detailing.

`ShadowComposeActivity.kt` applies this to **both Deck Construction tabs**. Card Gallery uses `GridCells.Fixed(3)` in the full portrait width, with existing search, rarity, and faction filtering; search and filter fields receive subtle matching binder tones. My Deck stays a separate tab rather than being moved to the right. Its power, cost curve, legality, covers, import/export, save, rename, delete, and Battle controls remain untouched.

## Interaction guarantees

- Tapping a card still calls `onCard(card)` for inspection.
- The pocket `+` button still directly invokes `vm.add(card)` and is visible only for `card.isPlayable`.
- Owned copies continue to appear as `×N` badges using `state.cards[card.name]`.
- The deck display uses the same actual card catalog and counts. Nothing changes deck limits or gameplay.
- **Card Collection** retains its preexisting `LibraryCardFace` design; the binder skin is deliberately scoped to Deck Construction.
- Portrait orientation remains mandatory. No side-by-side deck panel, new remote artwork, font file, external API or permission was introduced.

## Verification

`DuelBinderDeckGuardTest.kt` validates the three-column layout, separate tabs, existing callbacks, original card images, binder ornamentation, and collection separation.

The GitHub code and regression guards have been updated without invoking GitHub Actions. Kotlin source/structure checks are not an Android compile; pull the branch and build locally, then test the Deck Construction gallery on the phone, tap cards, add to deck, change tabs, and verify a narrow device can scroll normally.
