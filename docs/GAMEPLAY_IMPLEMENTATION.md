# Shadowshaper Duel Simulator — Implementation Status

This project is a fan-made **single-player Android simulator against an AI opponent**. It does not connect to Destiny: Rising accounts or reproduce online matchmaking.

## Gameplay references

- Full gameplay: https://www.youtube.com/watch?v=Ts4uJSQEA5Q (DanS118, six recorded matches)
- Mechanics: https://game8.co/games/Destiny-Rising/archives/546603
- Card wording and glossary: https://www.todayindestiny.com/rising/shadowshaper_duels/cards and https://www.todayindestiny.com/rising/shadowshaper_duels/keywords
- Card effect demonstrations, strategy: https://www.shacknews.com/article/146121/otk-triumph-destiny-rising

The Android battle screen uses an **original recreation** of a dark bronze/blue Shadowshaper-themed landscape tabletop, not extracted proprietary UI art. This is a first-pass approximation of the video client's proportions; exact UI matching and board layout require frame-by-frame comparison against reference screenshots and a device test.

## Implemented foundations

- Battle screen switches to landscape with a compact four-row two-sided table, CPU side above, player side below.
- Round progress, offensive/defensive indicator, Power scoreboard, Light cost and remaining Light.
- Original card-face artwork where available; fallback for missing images, visibly marked individual cost/power and name.
- Compact bottom hand with tap-to-select, drag-to-empty-slot, and long-press for card details.
- Choose any saved legal 15-card deck or a built-in beginner practice deck.
- AI opponents by faction: Mixed, Fallen, Hive, Cabal, Humans. The deck generator observes 2-copy/3 Exotic/5 Mythic limits and favors affordable cards.
- Coin toss, 5-card / 4-card opening hands (first/second), six alternating rounds, one draw per new round.
- Round N grants N play points; no carryover; cards can be played into available front/back-row slots.
- Tracks Power, buffs, cumulative score, unit damage and destruction.
- Implements recurring and triggered effect families in `ShadowEngine.java`: front/back-row on-play conditions, selected friendly/enemy targets, random target resolution for CPU, Inspired, Suppressed, Scorched, adjacency, same-faction lines, column bonuses, triggered buffs, selected summons, death splashes, end-turn effects, row-wide damage, and protective/damaging auras.
- End-of-sixth-round result dialog and event log; restart and switch decks.
- UI state and card effects are separated. **BattleActivity must not implement rules directly**; new effects belong in `ShadowEngine`.

## Still in progress / not yet exact

**Do not claim all cards are fully playable.** Script coverage is not equivalent to faithful reproduction of each card's detailed rules:

1. Several summoned units have unknown source Power (Guerrilla, Clone, Rock). The engine records a missing effect rather than assigning invented statistics.
2. Some random faction summons (Tomb Ship, Drop Pod), movement and placement choices (Xuan Wei), and opponent-field effects (Cursed Thrall) still require exact definitions and implementation.
3. Some conditional buff/damage interactions are first-pass interpretations of community ability summaries, including exact "Line" and "Column" geometry, damage modifiers, and persistent status timing. Validate against original game footage and card faces.
4. A fully verified original visual skin, animations, exact board dimensions, and every artwork image are not included.
5. The engine currently uses an AI heuristic and fixed faction-built decks, not the game's NPC scripts.
6. Match pause/resume persistence across app termination is not yet implemented.
7. Individual card coverage must be audited with correct text before declaring completion.

## Local verification (no GitHub Actions)

On a PC with Android Studio SDK and JDK 17:

```bat
gradlew.bat :app:assembleDebug
gradlew.bat :app:testDebugUnitTest
```

These are **local commands**, not CI workflows and do not require GitHub usage credits. Four source-level JUnit smoke tests were added for match length, playing a card, Inspired, and selected target buffs. They have **not** been run in this environment. Gradle sync, compilation, actual Android UI behavior, source image resolution, and on-device gameplay remain to be verified before treating the build as release-ready.
