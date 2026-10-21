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
