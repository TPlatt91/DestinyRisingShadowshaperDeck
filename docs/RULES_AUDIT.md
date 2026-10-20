# Shadowshaper Duels — Match rules audit

This document distinguishes **verified minigame rules** from engine implementation assumptions. Reference sources: [Game8's gameplay guide](https://game8.co/games/Destiny-Rising/archives/546603), [TodayInDestiny's keyword descriptions](https://www.todayindestiny.com/rising/shadowshaper_duels/keywords), and [a first/second player community report](https://www.reddit.com/r/destinyrisingmobile/comments/1gqz614).

## Verified / coded in the core engine

| Rule | Implementation |
|---|---|
| Deck size exactly 15 | `ShadowData.validate` using `ShadowRules.DECK_SIZE` |
| Max 2 copies of same name | `ShadowData.validate` / deck editor |
| Max 3 Exotic, 5 Mythic | `ShadowData.validate` |
| Six rounds | `ShadowRules.ROUNDS` / `ShadowEngine.endTurn` |
| Random coin toss for Offensive (first) / Defensive (second) | `ShadowEngine.start` |
| Each player has one turn per round; second player acts last in round 6 | `ShadowEngine.endTurn` |
| Light available on round N is N; does not carry over | `ShadowRules.lightForRound` / `ShadowEngine.endTurn` |
| Multiple cards can be placed if affordable | `ShadowEngine.play` |
| Placement in front/back row, vacant spaces only | `ShadowEngine.validatePlay` |
| Player cannot act on the wrong turn | `ShadowEngine.validatePlay` |
| Target must match eligible faction, row, controller and card condition | `ShadowEngine.targetOptions` and `validatePlay` |
| Illegal actions must not spend Light or remove cards | `ShadowEngine.play` performs validation before mutation |
| Total score is sum of surviving units' current Power | `ShadowEngine.score` |
| Higher Power wins after round 6; ties are draws | `ShadowRules.result` |
| Power 0 removes a unit | `ShadowEngine.sweep` |
| Inspired gains 1 Power at end of owner's turn | `ShadowEngine.endSide` |
| Suppressed removes special effects and bonuses | `ShadowEngine.suppress`, ongoing aura logic |

## Supported by gameplay reports, pending direct verification

- **Opening hands:** Offensive draws 5; Defensive draws 4. A Destiny Rising player reported these values; modeled in `ShadowRules.openingHand`.
- **Draw timing:** A new card each subsequent round in the prototype (`ShadowRules.NEW_ROUND_DRAW=1`); this needs gameplay footage confirmation.
- **Board width:** Six slots per row in the prototype (`ShadowRules.COLUMNS=6`); needs comparison with original in-game board layout.
- **Faction lines:** The engine currently interprets Line as a *contiguous run of matching faction units in the same row*. Community discussion establishes that a 4-unit Cabal Line requires four Cabal units in one line, but does not fully establish whether a gap interrupts the line. Keep this flagged as provisional; `ShadowEngine.projectedFactionLine` contains the single entry point to adjust it.

## Core engine integrity / future card work

- The UI uses `ShadowEngine.validatePlay` and `ShadowEngine.isPlayablePlacement` to highlight legal player board targets. Rejections are shown and cannot mutate Light, hand, or score.
- CPU uses the **same** rules as the player and evaluates row requirements, available targets, column partners and projected faction lines.
- Card effects must stay out of `BattleActivity`; put them in the dedicated engine or a reusable effect handler. Whenever adding a card, test its On Played, ongoing, timed, suppressed, removal, targeting, and AI interactions as applicable.
- A reproducible **match seed** is recorded, and the event log can copy/share `ShadowEngine.diagnosticReport` with board, Light, and effect history.
- Individual card logic currently includes preliminary approximations. **A scripted branch is not proof of 100% ability fidelity.** Unresolved effects are reported at runtime.
- No GitHub Actions/CI tests are configured or needed. Run local tests with `gradlew.bat :app:testDebugUnitTest`. Current Java source has passed static lexical/balance checks, but the Android APK and tests have not been compiled/executed in this environment.

## Local regression coverage

`ShadowEngineTest.java` + `ShadowRulesTest.java` cover match duration, Power scoring, Inspired, targeted friendly buffs, canonical constants, illegal/occupied positions, payment atomics, opponent turns, faction-line target gating, deck sizes, rarity caps, and tokens. All must be run locally before making a release.
