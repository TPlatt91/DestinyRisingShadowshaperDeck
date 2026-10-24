# Skirmish — card-status visual effects (October 2026)

## Implemented in `main`

All effects are **presentation only**. The engine still immediately commits legal moves, damage, status durations and Power gains; animation does not alter game outcomes, RNG, card legality or autosaves. Compose does not parse battle-journal text to invent effect triggers.

| Effect | On activation | While active | On resolution |
|---|---|---|---|
| **Scorched** | Orange edge burst | Low-cost drifting edge embers; center art clear | Orange damage ring on actual damaging status tick; embers stop on expiry or Suppression |
| **Suppressed** | Violet electrical border flash | Thin corner static arcs | Remains until underlying status is cleared |
| **Inspired** | Mint/gold-green border pulse | Light under-card strip/spark marks | Actual +1 Power at owner's end-of-turn uses the existing Power number animation |
| **Overshield** | Cyan flare | Delicate animated beveled/hex shield edge | Cyan break flash when the shield actually blocks an effect |
| **Power transfers** | Target gets a Power pulse | — | 630 ms pale-gold line travels from real source card to real recipient slot; Disfavored, Speaker, Mishra, Maru, Knight, Morgran, Hyssop and Zhong Wenbin have explicit source references |

These drawings reuse the existing **portrait**, 7-column board, original card assets, and miniature Power/status indicators. FX do not cover the card face center or add hitboxes.

## Typed event architecture

- `DuelEffectEvents.kt`: `DuelEffectKind`, identity-based source/recipient records, monotonically increasing event IDs, bounded history (96 entries).
- `ShadowEngine.kt`: typed events at the actual `inspire`, `buffAndReport`, `addScorch`, `suppress`, `shieldAbsorbs` code paths. Zero-amount/no-op actions do not emit Power animation events.
- `ShadowCardEffects.kt`: emit Overshield grants and Scorched end-turn ticks only when damage occurred; hand-to-target buff sources are attached to their recipient's Power event.
- `ShadowBattleCoordinator.kt`: snapshot includes `visualRevision`, allowing a status-only change to redraw even when a card's Power, turn and placement revision did not change.
- `DuelStatusEffects.kt`: localized edge effects and source/target beam. Power beams do not draw if the source/recipient left the reported board slot. Event sequences are consumed once per unit.
- `DuelBoardAnimations.kt`, `ShadowBattleActivity.kt`: render effects on the existing board art without changing touch coordinates.
- Effects are NOT serialized in `ShadowMatchState`. An old match resumes with accurate statuses but with no replay of obsolete one-time effects.

## Battle Effects setting

Skirmish > MENU > SETTINGS > **BATTLE EFFECTS** cycles:

- `FULL` (default): animated persistent status auras, quick status bursts, and Power-transfer beam.
- `REDUCED`: restrained static status edges and shorter activation pulses, no continuous shimmer or transfer beam.
- `OFF`: no additional FX, leaving the existing small status icons and Power values untouched.

Choice is saved locally in Android SharedPreferences, independent of the match save. It takes effect without reopening the battle.

## Local verification

After pulling latest `main` run `run_local_engine_checks.bat` on the Windows PC. The new `DuelEffectEventsTest.kt` exercises Scorched application/ticks, Inspired, Suppression, Overshield grant/consumption, The Disfavored's +2 Power transfer, 96-event history limit, visual-revision notifications and settings modes. Existing ability and save/restore tests should also run.

In a physical match, try these:

1. Scorch a unit with Ayuela or Resilient Grenade Dreg: orange edges appear, tick animates at the status's actual damage timing, then ends as the status clears.
2. Buff Front Row The Disfavored with another friendly Hive on board: recipient glows and a gold beam travels from Disfavored to that Hive unit.
3. Give Hexguard Inspired: mint activation, persistent status until removed, +1 Power on the owner's end turn.
4. Grant Overshield with Rossi-11, then attack or Suppress that unit: blue perimeter then one break flash.
5. Suppress an Inspired/Scorched unit: status visuals vanish, violet disruption remains.
6. Toggle Full / Reduced / Off while viewing the board and restart the app to confirm the setting is retained.

The code is pushed and source-level integration checks have passed. **No Android SDK compile, Gradle unit-test execution or physical-device rendering is claimed from this environment.** No GitHub Actions have been run.
