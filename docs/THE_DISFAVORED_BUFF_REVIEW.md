# The Disfavored — Power-bonus reaction review

## Printed card text

Video-verified catalog record (in `verified_video_cards.tsv`):

> Sustained (Front Row): When acquiring a buff, grants +2 Power to one of your different Hive units at random.

The Disfavored must be on its owner's Front Row, unsuppressed and alive. It needs at least one **different** surviving allied Hive unit on either row. The +2 recipient is randomly chosen, never The Disfavored itself, and the ability cannot target enemy Hive units.

## Issue and implementation

The core `ShadowEngine.buff()` pathway already called `ShadowCardEffects.onBuff()` after The Disfavored gained a direct Power bonus. The older random recipient list, however, included on-board Hive units whose current Power was zero or lower but had not yet been removed by the end of the active effect chain. In such a chain the +2 could target an effectively defeated unit and vanish when cleanup finished. This was not covered by the existing happy-path test.

`ShadowCardEffects.onBuff()` now filters to different, present, **living** allied Hive recipients before choosing randomly, while retaining the existing one-activation-per-causal-bonus-chain guard for two Disfavored sources. It logs the actual recipient (`The Disfavored ▶ +2 Power to <unit>`) so the effect can be traced through the battle log. If there is no eligible ally, a no-target message explains why the bonus was not granted.

The Front Row requirement remains explicit for old records with incomplete trigger metadata; video-backed descriptions continue to take precedence.

## Regression coverage

`ShadowBuffChainResolutionTest.kt` adds:
- A genuine `engine.play()` of Mishra beside Front Row Disfavored, confirming Mishra's +3 to it creates +2 on a different Hive ally.
- Mishra's same buff applied to Back Row Disfavored, confirming no extra Hive buff.
- Real `engine.endTurn()` Inspired +1 Power granted to Front Row Disfavored and its resulting +2 on a Hive ally.
- A defeated but not-yet-swept Hive unit next to a living Hive ally, confirming only the living ally can be selected.
- No surviving other Hive ally, confirming that no phantom +2 is generated.

Preexisting tests cover the two-copy anti-loop guard, separate chains within one turn, multiple Inspired copies, self-exclusion and suppression.

## Local confirmation

After pulling `main`, run `run_local_engine_checks.bat` (runs local unit tests and `:app:assembleDebug` without GitHub Actions). In Skirmish, place Disfavored in **your Front Row**, put another allied Hive unit anywhere on your board, then apply a Power bonus to Disfavored. The battle log should identify the Hive recipient and the +2 increase. Giving it Inspired produces the +1 at the end of its owner's turn and should trigger its ability then, not immediately when Inspired status is applied.

Actual Kotlin build/test execution and on-phone rendering have not been performed in this session.
