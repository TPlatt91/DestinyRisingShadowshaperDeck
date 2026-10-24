# Shadow Shaper Duels — battle HUD chrome polish

## Source screenshots and intent

The player provided crops of the current thin MENU / SHADOWSHAPER DUELS / YOUR TURN strip and brown circular END TURN dial. The goal was to raise the UI finish to premium sci-fi card-game standards without changing the game rules, seven-column board, portrait orientation, original stone/foliage artwork, the Player's ability to inspect the finished board, or any existing callbacks.

## Delivered changes

**Battle header, `DuelBattleChrome.kt`:**
- Replaces the cramped 29dp default-looking row with a compact 49dp beveled graphite-blue status panel.
- Title becomes carefully centered two-line SHADOW SHAPER / D U E L S lettering, with restrained metallic filigree, thin golden baseline, panel separators, asymmetrical highlights and side etching.
- Original MENU interaction is a full-height 81dp-wide touch target with an original vector hamburger glyph; when reviewing the final battlefield it becomes a clear RESULTS back arrow.
- A live status station on the right displays YOUR TURN / CPU TURN / TARGET / MOVE UNIT / DUEL COMPLETE, paired with contextual microtext and a subtle animated indicator only while it is the player's turn.
- Only the menu/results station accepts input; the status/title are not interactive and cannot consume card drag gestures.

**End Turn, `DuelBattleHud.kt`:**
- Replaces the 47dp inner Material Button inside a 62dp dial with a single responsive **78dp clickable command medallion**, accentuating gold, cobalt/ice, or steel based on actual action type.
- Faceted metal sweep highlights, dual etched concentric rings, 24 machined teeth, circuit-like arc glints and a small ornamental diamond instead of the previous flat brown face.
- Smoother slow active bezel breathing, subtle pressed scale, and a distinctly subdued disabled state.
- Still receives exactly `enabled`, `label` and `onClick` from the existing battle coordinator: END TURN, CANCEL targeting and MOVE relocation logic are unchanged. The full medallion is keyboard/touch accessible with meaningful control semantics.
- The Player's Power/Light UI moved a small 17dp upward so the larger dial doesn't collide with it.

## Technical details

- `ShadowBattleActivity.kt`: one new `DuelBattleTopBar(status, reviewingBoard, yourTurn, onNavigation)` call replacing the old inline banner; engine-derived status logic and existing menu actions retained unchanged.
- `DuelBattleChromeGuardTest.kt`: pure JVM source guards for active labels, menu/results navigation, exact original arena artwork, seven slots, portrait lock, pressed/disabled dial states, audio callback and no gameplay mutations.
- No new image assets, copyrighted sprites, textures, dependencies, network calls, permissions or GitHub Actions.
- Source structural checking and 44 source-guard comparisons passed; full Android compilation and visual/interaction acceptance still require a local build and installation.

## Recommended visual acceptance test

On the user's portrait phone, check MENU and RESULTS tap targets; "YOUR TURN"/"CPU TURN" transition; glowing disabled/enabled End Turn dial; CANCEL while targeting; MOVE during legal relocation; and verify that the Player's score/Light badge doesn't overlap the circular button. Inspect the final battlefield and return to Results. Pull from GitHub and run local build as usual. No CI/GitHub Actions required.


## Follow-up: lighter Destiny-inspired HUD pass (user screenshot, 23 October 2026)

The first 49dp chrome looked too much like a generic framed RPG panel, with a cramped **SHADOW SHAPER / D U E L S** title and undersized turn indication. The user asked for a more authentic Destiny-like direction. Bungie's published Destiny UI principles emphasize the important mission/ability information and preserve screen space, rather than using maximal ornament.

**New implementation:**
- **DuelBattleChrome.kt:** 54dp translucent charcoal glass header. An accurately readable **SHADOWSHAPER** title above a subtler **DUELS / SKIRMISH** label; no spaced-out micro-letters. Fine white/grey dividing strokes, a tiny original geometric objective diamond, and a restrained warm active accent replace most gold metallic framing.
- Menu and Results action remains full-height touchable and reads **MENU** or **RESULTS** with an original white directional glyph. No extra buttons interfere with battle gestures.
- Right status station now prints the real **ROUND n / 6** under **YOUR TURN**, **CPU TURN**, **TARGET**, **MOVE UNIT** or **DUEL COMPLETE**. The round value comes directly from `snapshot.round`, and the indicator uses minimal animation only for the player's active turn. No fake countdown.
- **DuelBattleHud.kt:** 78dp End Turn / Cancel / Move medallion keeps the full click area, accessibility descriptions and existing coordinator callbacks, but its ornate 24 teeth and gold mechanical interior were replaced with a cleaner segmented white-metal ring, recessed gunmetal face, tiny focal diamond and restrained command-specific coloring.
- **DuelBattleChromeGuardTest.kt:** updated for the readable labels, actual round binding, segmented dial geometry and unchanged menu, target, end-turn and Results callbacks.
- **Asset safety:** no external Bungie/Bungie trademark imagery, copyrighted icons, network requests, extra permissions, fonts, or image files. No change to the original arena image, seven legal slots, portrait lock, animations or game engine.

Source-structure and navigation string assertions pass. A local Android compilation and phone screenshot are still required to verify exact font fit, readability and behavior. No GitHub Actions were invoked.
