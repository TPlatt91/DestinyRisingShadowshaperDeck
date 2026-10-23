# Shadowshaper Duels — Interface fidelity audit

**Goal:** Match the look and interaction patterns of the real *Destiny: Rising* minigame while implementing actual, verifiable rules. The current result is a **functional recreation, not a pixel-identical extraction**.

## Visual references checked

- [Original Destiny: Rising deck construction screenshot](https://www.shacknews.com/article/146121/otk-triumph-destiny-rising): dark bronze/gold panels, prominent `POWER` aggregate, `15/15` card counter, six-cost distribution, card images with Power and blue cost indicator.
- [Cabal gameplay: eight games, line and column strategies](https://www.youtube.com/watch?v=6azKczzbE9w): reference recording for match pacing and positional play.
- [Destiny: Rising — Shadowshaper Duels guide](https://game8.co/games/Destiny-Rising/archives/546603): six-round match, offensive/defensive turn order, score comparison at right, End Turn at right, front/back-row play.
- [Official keyword reference surfaced by TodayInDestiny](https://www.todayindestiny.com/rising/shadowshaper_duels/keywords).

## Implemented in this UI pass

| In-game concept | Native Android implementation |
|---|---|
| Bronze/gold card-game appearance | Warm brown and gold home, deck editor, rules and landscape match palette |
| Power/cost chips on cards | Power upper-left, blue Light cost upper-right |
| Round progress | Six visible round indicators; current-round highlight and single animation |
| Current score | Power scoreboard to the right of the battlefield |
| Opponent card count | CPU concealed-hand counter |
| Two-sided battlefield | Enemy Front/Back and player Front/Back rows |
| Player hand and Light | Overlapping cards in bottom dock, Light remaining at left |
| Touch cards and move onto field | Tap to select, drag-and-drop, press/hold card for details |
| Targeting a card effect | Highlight eligible board units, tap a highlighted card to select; Cancel Target |
| End of turn | Clear End Turn on the right with remaining-Light confirmation |
| AI actions | Sequential CPU card placements (~560ms apart), not instant complete-board resolution |
| Feedback | One-time placement scaling, visible scores and match-event log |
| Deck construction | Base Power total, 15-card count, rarity limits and cost curve |
| Rules | Expanded in-app touch, target, round and keyword instructions |

## Kotlin battle UI fidelity pass — October 20, 2026

The active battle UI is now the Kotlin Compose `ShadowBattleActivity.kt`. Its new reusable presentation components are in `ShadowBattleVisuals.kt`. The original Java battle UI is **not** used.

- Bronze rim and dark tabletop gradients distinguish the battlefield from controls without introducing any additional play zones.
- Each unit on the battlefield renders as a vertically proportioned original card face, inside its existing six-column front/back-row slot. No in-game art was recreated.
- Every card displays its current Power near the upper-left, blue Light cost at upper-right, and name along the bottom. Rarity determines its outer frame color.
- Inspiration and Scorched appear as small status emblems; Suppressed units receive a distinct overlay.
- Six numbered/colored round milestones, Light crystals, two-sided Power scores, and facedown opponent-hand silhouettes are separate elements.
- The hand is slightly fanned. Selecting a card lifts it; pressing and dragging displays a portrait card following the finger. Only eligible placements glow.
- Target choice and Xuan Wei's relocation still use the actual underlying engine's legal target/empty-position checks.
- Accessibility descriptions identify side, row, column, unit and legal target/placement state.
- The design keeps the game as **single-player against an AI opponent**.

**Source grounding:** The [Game8 mechanics guide](https://game8.co/games/Destiny-Rising/archives/546603) identifies the two rows, six rounds, card Power/cost positions, scoring to the right and End Turn to the right. The [Shacknews deck screenshot](https://www.shacknews.com/article/146121/otk-triumph-destiny-rising) illustrates the game's physical card faces and bronze/gold palette. These sources support the directions above, but **do not provide an exact full-resolution battle-field skin or asset set**.

## Follow-up UI and touch pass — October 20, 2026

This pass updates the live Kotlin landscape simulator, without touching the gameplay effect resolvers:

- **Window-coordinate drag fix:** all hand-card drag pointers and battlefield hit rectangles use window coordinates. The floating card preview converts back to coordinates relative to the root Compose Box; status-bar height and root offsets no longer shift it.
- **Drop restrictions:** drop hit-testing accepts only the player battlefield, then delegates to the engine's legality checks. Drops on opponent slots, outside the field, or on malformed positions cannot place cards.
- **Hand visibility:** cards overlap in a compact, slightly fanned arrangement; selected cards draw in front with enough vertical inset not to clip the lift/preview.
- **Turn and ability readability:** short animated overlays announce new turns and the most recent card event, while the full event log remains accessible.
- **Decision clarity:** the top-right action label distinguishes ordinary turns from ability target selection and a pending relocation; a compact on-field instruction identifies the required next action.
- **Deck-to-battle continuity:** launching from an already selected legal deck opens the CPU faction choice directly rather than forcing another deck-selection prompt.
- **Offline local tests:** `DuelHitTestingTest.kt` adds five tests for legal player slots, rejection of enemy slots, rejection of off-board/malformed cells, and window-to-root overlay conversion. Tests were committed, **not executed**.

These updates preserve the six-row-wide prototype board and simulated card effects, which still require game-client verification. The drag bug fix is source-reviewed but has not been validated on Android hardware.

## Next UI implementation pass — October 20, 2026

Implemented directly in the Kotlin/Compose repository:

- **Drag hit testing:** `DuelHitTesting.kt` makes it explicit that both card pointer and placement cells use the same window coordinate system. The card-preview location is converted to coordinates relative to the root Compose overlay, preventing offset problems from device insets.
- **5 new local hit-testing tests:** valid player slots, enemy-slot rejection, out-of-board rejection, malformed slot IDs, and coordinate transforms.
- **Contextual match feedback:** fades in turn announcements and recent card events. Pending on-play targets and Xuan Wei-style relocation show prominent instructions above the hand.
- **Hand interaction:** compact overlapping cards with selected card drawn above its neighbors, extra vertical clearance for the lifted card, and constrained dragging while another decision is unresolved.
- **Deck-to-match flow:** a named legal deck passed by the Compose deck editor is selected automatically on entry to Battle, reducing duplicate deck selection.
- **Enlarged landscape card inspector:** `DuelCardInspection.kt` replaces the small popup. Players can inspect enemy/friendly cards or hand cards; current Power, Light, rarity, detailed effect, slot, Inspired/Suppressed/Scorched state, and coverage caveats are shown without altering engine state.

The revised screens are still **reference-inspired**, not verified pixel-identical: original high-resolution game battle textures, proprietary UI assets, and client-recorded animations are not present in the repository. The board dimensions/draw timing remain provisional. All updated files passed static bracket/string checks; no Android build or device execution was performed.

## Responsive battlefield and authentic card-back pass — October 20, 2026

Changes now implemented in Kotlin/Compose:

- **Correct portrait card fitting:** `DuelLayoutMetrics.kt` computes the largest 0.72 width-to-height card that fits into each actual board-slot constraint (with a small inset). `ShadowBattleActivity.kt` uses `BoxWithConstraints` to avoid spilling card faces into neighboring columns or rows when the landscape viewport is short.
- **Compact device layout:** On narrower landscape configurations the left and right HUD panels shrink; the bottom hand dock reduces height on shorter screens so the four battlefield rows remain visible. The number and identity of game slots never change.
- **Original Shadowshaper cover art:** Opponent card backs now use the public TodayInDestiny default-cover image, replacing the hand-drawn diamond placeholder. The app checks `assets/card_cover/<cover-id>.webp` first, then the app-private disk cache, then a verified HTTPS resource; an unobtrusive placeholder remains when none is available.
- **Per-deck card-back selection:** `DuelCoverCatalog.kt` lists nine original Shadowshaper covers seen in [TodayInDestiny Covers](https://www.todayindestiny.com/rising/shadowshaper_duels/covers): Default, Denzin, Elite Vandal, Marauder, Morgran, Sniper Vandal, Tracer Shank, Wolf and Zhong Wenbin. Deck Construction displays a cover picker; its selection is stored in `cover_<deck name>` separately from the existing `deck_<deck name>` JSON card quantities. Renaming/deleting the deck carries/removes the cover preference.
- **Player deck visualization:** The chosen original cover is shown on the player's face-down draw pile in battle, while the opponent uses the default cover.
- **Image loading stability:** Cover fetches share a loading lock to avoid repeated simultaneous downloads corrupting the same cache path.
- **Local checks:** Five added size-fitting tests plus two original-cover catalog tests (seven authored in this pass). All updated Kotlin source files passed a lexical bracket/string-balance audit.

**Important limitations:** The official covers are fetched at runtime unless their WebP files are bundled into `assets/card_cover/`; the app currently includes the loader but not downloaded binary cover assets. On first install without internet, the placeholder is shown. Display proportions, textures, drawing animations, and zone spacing still require real-device screenshots to confirm parity with the Destiny: Rising minigame. The game still has provisional rules and incomplete abilities. Android Studio compilation, the new tests, and hardware layout inspection have not been run in this environment. No GitHub Actions work was started.

## Animated placement and match-result pass — October 20, 2026

This pass implements the next Kotlin/Compose battle presentation milestones:

- `DuelBoardAnimations.kt`: entry scales a newly played card from 79% to 100% and fades it into view over roughly 0.3 seconds. The effect is keyed to the engine's placement revision and recorded side/row/column; other battlefield cards remain static.
- `DuelPlacementCue.kt`: a pure predicate determines which exact card slot may animate. Four local JUnit test methods cover an empty match, latest player card, same-column enemy card and invalid placement state. Five predicate assertions additionally passed a local command-line Kotlin/JVM check (not an Android build or the JUnit suite).
- `DuelTableEtching`: unobtrusive low-contrast copper lines and circles are painted **behind** the existing rows. This is decorative and does not create zones or imitate unavailable original assets.
- `DuelScore`: current totals interpolate visually when Power changes; the engine's exact integer totals are still used for targeting, effects, victory and draw decisions.
- `DuelResultPresentation.kt`: animated finish screen shows the true winner, player and CPU Power, a warning where unresolved effects exist, and Home/New Match actions. The previous plain AlertDialog was removed.
- All of these additions are presentation-only. The Kotlin card-effect rules, 15-card deck legality, the six-round match sequence and the AI were not modified.

**Authenticity limitation:** These timings, textures and transitions are close visual recreations rather than claimed direct resource extractions or pixel-identical recordings. The public [Game8 Shadowshaper rules guide](https://game8.co/games/Destiny-Rising/archives/546603) confirms the six rounds, the right-side scores, Power positions and end-of-match criteria, but does not specify original animation easing or millisecond timings.

## Still not confirmed identical

- **Precise board zone count and spacing:** the simulator uses 6 columns and 2 rows per side pending frame comparison.
- **The art assets:** original card faces are not yet verified for most of the 110 catalog entries; the simulator does not substitute unrelated Destiny art.
- **Rendering:** card frames, card-back silhouettes, table textures, animations, audio, custom game fonts, and transitions remain *similarity-oriented recreations*, not original imported game resources.
- **Rules:** draw schedule, exact Line adjacency behavior, triggers on card movement, and advanced status resolution require in-client verification. Card effect code remains incomplete.
- **Build and device verification:** these source changes were pushed, but no Android device test or Gradle compilation has been performed in this environment.

## Fidelity and bug-testing workflow

1. Compare a **full landscape battle screenshot** from Destiny: Rising against a matching simulator screen at the same aspect ratio (preferably at 16:9 or the device's native landscape resolution).
2. Verify row/column count, card aspect ratio, empty slots, scoring area, opponent hand backs, Light counter, opening prompt, ability targeting prompts, and End Turn placement.
3. Test a full match with one card from each key shared mechanic (When Played, Sustained, Inspired, Scorched, Suppressed, line, column, summon, damage, control).
4. Use the in-app `LOG / DETAILS` → `COPY REPORT` control to export reproducible seeds, board positions and effect logs for mismatches.
5. Run `gradlew.bat :app:testDebugUnitTest` and `gradlew.bat :app:assembleDebug` locally (no GitHub Actions usage).
6. Only mark a UI/mechanic as exact after observed side-by-side comparisons pass.

## Card-text correction documented

A [Shadowshaper community discussion](https://www.reddit.com/r/destinyrisingmobile/comments/1nk0bde/shadowshaper_duel_4unit_line_how_does_it_work/) reports a correction to **Scorpius**: a Back Row sustained ability dealing 1 damage to two random opposing cards at end of turn. The catalog, runtime, and two local regression tests now use that reading. This should still be checked against the user's current game client.


## Battle UI authenticity pass 01 — 23 October 2026

**Scope:** Live landscape `ShadowBattleActivity.kt`, reusable visuals and layout sizing; no game-engine/card-mechanic edits.

- **Gameplay controls:** Moved End Turn/Cancel Target and conditional Rossi-11 Shield Self next to the right-side scoreboard (matching the documented in-game control region), retaining the existing Light-spending confirmation and pending-relocation guards. The bottom dock remains dedicated to playable hand cards, the Light meter, chosen cover/deck count, and concise guidance.
- **More readable field:** Shortened the top header; reduced left HUD width and horizontal row-label footprint; compacted all six round pips so they fit short landscape phones; removed placeholder middle dots from unoccupied card slots. Board width remains the same six legal columns and row ordering remains CPU Back/Front then Player Front/Back.
- **Visual hierarchy:** Darkened the central board and surrounding backdrop; retained bronze/gold and the mirrored position semantics. Removed the large default match-event transcript from the right sidebar; latest event appears only with enough room, with the full event log still accessible from its dedicated button.
- **Card art reliability:** Eliminated `LocalContext.current as? Activity` assumptions in both battlefield card-face and card-back composables. The underlying `CardArt` loaders now accept ordinary Android Context instances and post decoded bitmap/GIF results back onto each target ImageView. That fixes a cause of art silently failing to load under Compose context wrappers. The full original card-image assets are still used unmodified.
- **Regression expectations:** Updated `DuelLayoutMetricsTest.kt` to reflect narrowed left HUD sizes without changing playmat slot count.

**Reference scope and limitations:** The right-side End Turn location and scoring concepts are supported by the Game8 Destiny: Rising Shadowshaper Duels gameplay guide, and the use of original card art is supported by the user's existing assets. This is **the first fidelity pass**, not a claim of identical texture art, exact typography, unit animations, original audio, or proprietary visual assets. The exact screen geometry still requires comparison with user-recorded original-client battle footage on matching landscape dimensions.

**Verification:** Changes have been pushed to GitHub but no Android device run or Gradle compilation was performed for this UI pass. Run `run_local_engine_checks.bat` locally after pulling, install the new APK, and compare the same battlefield state against the game's recorded screenshots. No GitHub Actions are needed.


## Screenshot-driven interface redesign — 23 October 2026

**User-supplied visual targets:**
- Battle screenshot: gray stone/metal board, four field rows (CPU Back/Front above player Front/Back), utility stack at left, right-side CPU/player medallion scores, blue Light indicators, circular golden End Turn, compact bottom hand.
- Deck editor screenshot: landscape metallic shell; left four-column artwork gallery with search/filter; right current-deck grid, Power/Light-cost distribution, name/count and bottom SAVE.
- Collection requirement: reuse the deck-editor artwork/browser look but allocate the whole content width to a full-width grid, without a right deck panel.

**Implemented and pushed:**
1. `ShadowBattleVisuals.kt`: procedural gray stone slab texture with slate-course markings behind the four actual game rows; subdued transparent slot backgrounds; CPU/player medallion-style Power totals. No screenshots or synthetic card faces are used as actual battlefield background.
2. `ShadowBattleActivity.kt`: narrow left utility/deck display, larger central battlefield, right HUD with actual CPU/player Light, round count, live Power, a circular gold End Turn button and preserved targeting, shield-self, confirmation and event-log actions. Existing drag interactions, legal placement coordinates, match state, deck selections and AI gameplay remain engine-controlled.
3. `ShadowComposeActivity.kt`: Deck Construction and Card Collection explicitly use landscape orientation, returning Home/Rules to normal orientation. Deck has independent left gallery and right deck panel with four-column original card faces, live counts, curve, SAVE (the repository already autosaves edits), Battle, export/import, cover and delete. Collection uses the same visual language with a full-width eight-column gallery and no deck panel.
4. Card gallery artwork loads from the same local PNG/WebP/GIF files already used elsewhere. No changes to the verified card metadata or printed effects.

**Verification boundary:** Repository edits were made directly; a device screenshot comparison and `run_local_engine_checks.bat` execution are still required. These are **reference-matched layouts and custom-drawn materials**, not the proprietary game's original texture/image resources. Exact pixel parity, fonts, HUD ornaments, animation frames, and sound effects remain further fidelity work. Do not use Today in Destiny as gameplay effect authority. No GitHub Actions/credits have been used.


## Portrait orientation restoration — 23 October 2026

**Explicit user correction:** The app must remain **upright (portrait)** on the phone. The preceding screenshot-driven landscape UI work should not turn the device sideways, even though source Destiny: Rising screenshots were landscape.

- `AndroidManifest.xml` now locks both the Compose launcher and live battle activity to `portrait`; the runtime activities explicitly request `SCREEN_ORIENTATION_PORTRAIT`. The earlier navigation effect that rotated Deck Construction and Card Collection to landscape has been removed.
- **Battle:** left utility and right scoreboard sidebars were rearranged into a compact opponent/round header above the board and a horizontal score, Light, End Turn, Shield Self (if relevant), and Log strip below it. All six columns now use the available portrait width. The hand still fans and supports drag/drop, long-press inspection and target prompts; system status/navigation bars are respected.
- **Deck Construction:** replaced narrow side-by-side panels with **CARD GALLERY / MY DECK** tabs, preserving the card filters, 15-card deck, cost distribution, save/play, import/export, covers and deck management. The gallery and deck each use a portrait three-column card grid.
- **Card Collection:** the same visual library shell uses a three-column full-width grid and two-row search/filter controls, without a deck panel.
- **In-battle inspection:** card artwork is above its stats, original printed effect, and keyword explanations in a scrollable portrait dialog, rather than squeezed alongside them.
- Added `ShadowPortraitOrientationTest.kt` to catch accidental activity landscape locks and runtime orientation regressions in future local checks.

**Verification:** Latest user-pushed local build **before** these portrait edits was successful. The new portrait code still requires `run_local_engine_checks.bat` and a fresh on-phone screenshot/layout inspection. These are layout fixes, not new card rules. Zero GitHub Actions usage.


## Portrait battle proportion correction — 23 October 2026

**Motivated by the user's actual installed-app screenshot** (portrait battlefield with excessively tall empty slots, wrapping CPU/Your Back/Front labels, clipped Light line in the score bar, cramped overlapping hand cards, and large surrounding panels).

Code changes to the *active Kotlin Compose battle*, not just an illustrative plan:

- `DuelLayoutMetrics.portraitBoardHeight(width,height)` now bounds the four-row stone field by phone width and reserves vertical space for the round ribbon, scores, status bar and hand. The stone field is **centered inside its remaining viewport**, instead of forcing all four rows to stretch to the bottom. It also contracts gracefully on shorter screens. Gameplay row and column counts stay untouched.
- Long wrapping row labels are replaced with tiny color-coded **B / F** markers (Back/Front). Accessibility descriptions still include the full side/row/slot and targeting state; no hit-target semantics were removed.
- Score/action strip reduced to 59dp; CPU/You Power and Light use compact line heights; the circular End Turn shrinks to 50dp. End Turn confirmations, pending-target cancel and Rossi-11 shield actions retain their engine callbacks.
- Opponent ribbon and top title strip are thinner. The left-side Light/Deck/Hand dock is now narrow enough for portrait; the Light gem display has a compact mode and actual deck cover/count stack instead of overflowing a horizontal row.
- The hand height and spacing adapt to device dimensions and number of held cards; four cards can lay side-by-side, while larger hands overlap minimally before scrolling. Card panels use their actual **0.72 portrait aspect ratio** rather than filling an arbitrary taller hand dock. Tap, drag/drop, long-press inspect, and legal-target highlights remain connected to the same engine and hit-testing.
- Reduced ordinary slot outline weight and corner engraving intensity. Selected legal placements and effect targets still glow.

**Verification:** Added three pure Kotlin/JUnit regression cases to `DuelLayoutMetricsTest.kt` for board sizing, hand height, and overlap budget, including short-screen layouts. Source files passed a bracket/quote lexical scan. Android compilation and on-phone screenshot review still require the user's next local `run_local_engine_checks.bat` run. No GitHub Actions credits were used.


## Slate arena reconstruction and full-card visibility — 23 October 2026

**Direct user feedback:** The earlier portrait build showed a flat gray arena, four rows of tall outlined empty slots, and a short Cursed Thrall card whose name overlay obscured the printed art. The user supplied their original Destiny: Rising Shadowshaper battle screenshot again for side-by-side comparison.

**Implemented in the Android/Kotlin repository:**

1. `DuelStoneArena.kt` provides a cached, seeded stone-grain bitmap, gentle mottled gray marbling, chipped rock edges, small fissures and dark vignette rather than painting the battlefield with a flat color. It is a **procedural approximation of the original stone surface**, not extracted proprietary artwork, and includes no fake cards or scores.
2. The stone fills the **entire portrait arena** between the opponent/round ribbon and score controls. The four-row legal card formation remains centered within it; black gutters above/below the old compact board are gone.
3. `DuelArenaGrid` draws thin course lines and faint column guides relative to the actual card-lane coordinates, not the full-height stone floor. The original B/F debug labels and surrounding empty slot rectangles/corner marks are gone. Legal empty placements, relocations and ability targets still receive visible gold outlines/plus markers.
4. `DuelCardFrame` now leaves the original source image **fully visible**, without a painted name/footer over its bottom. Field miniatures display their live Power (green for buffed, red for reduced, otherwise neutral). Hand cards retain the printed Light indicator. Suppressed, Inspired and Scorched indicators are small lower-corner emblems rather than full-art-covering center banners. The same original embedded WebP/PNG/GIF assets remain in use.
5. Hit rectangles, event log, card selection, drag/drop, battle inspection and game engine functions are unchanged.
6. Added two source-level JVM regressions in `DuelVisualParityGuardTest.kt` to prevent accidentally bringing back debug row labels and the obstructing name footer.

**Remaining fidelity boundary:** Procedural stone grain and primitive medallion/status shapes are not pixel-identical to the proprietary Destiny: Rising UI. A fresh on-phone battle screenshot is required to evaluate real density, texture, cards and positioning. The new code has been statically source-checked for delimiter balance but **has not yet passed the user's local Android build or on-device inspection**. Run `run_local_engine_checks.bat` after pulling; do not use GitHub Actions.
