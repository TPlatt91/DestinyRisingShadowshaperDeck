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

## Still not confirmed identical

- **Precise board zone count and spacing:** the simulator uses 6 columns and 2 rows per side pending frame comparison.
- **The art assets:** original card faces are not yet verified for most of the 110 catalog entries; the simulator does not substitute unrelated Destiny art.
- **Rendering:** card frames, table textures, animations, audio, custom game fonts, and transitions are *similarity-oriented recreations*, not copied from the game resources.
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
