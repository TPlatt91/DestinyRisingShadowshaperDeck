# Original-game video is the source of truth — implementation status

Date: 2026-10-22. Repository: `TPlatt91/DestinyRisingShadowshaperDeck`, Kotlin Android game. **The user's original Destiny: Rising recordings outrank TodayInDestiny and community listings whenever they conflict.**

## Evidence inventory

| Evidence tier | Distinct cards | In-app treatment |
|---|---:|---|
| Direct original-video card-detail overlay (`verified_video_cards.tsv`) | **85** | Authoritative faction, rarity, cost, Power, printed effect; older detail metadata cannot overwrite these |
| Additional original video-verified *effect notes* (`card_details.tsv`) | **19** | Original-game recording credited as effect evidence; detailed metadata still supplies stats, since a complete captured row is missing |
| Neither direct overlay nor documented video-effect note | **17** | Community/TodayInDestiny/other existing data remain provisional, **not** rebranded as user-video verified |
| **Total** | **121** | Unique gameplay identities, not alternate-art variants |

Cards still lacking a documented original-video **effect** source: **Carrier, Donovan-17, Dreg, Guerrilla, Gwynn, Mishra, Morgran, Novice Marauder, Resilient Dreg, Rock, Rock Pile, Shank, Sparrow, Tenacious Shielded Knight, Tracer Shank, Zhiyao, Zhong Wenbin**.

Among these, **Gwynn and Tenacious Shielded Knight** also lack detailed metadata rows. We will not invent missing attributes from community text and call them in-game verified. **Mishra** is specifically unresolved: a historical TodayInDestiny listing described +2 to adjacent units while the app's legacy card entry implements +3. No authoritative submitted Mishra footage was verified here, so no ungrounded change was made.

## Changes implemented

- `ShadowData.applyDetailedMetadata()` preserves all four full-video-confirmed fields (rarity, faction, cost, Power) and the full recorded effect when applying older supplemental metadata. It still imports nonconflicting classification, acquisition, trigger, notes and card number.
- `ShadowData.VIDEO_NOTES_SOURCE` separately marks effect-evidence notes for 19 other cards, without falsely elevating them to complete numeric-stat video captures.
- `ShadowData.videoRequiredRow()` and `ShadowEngine.activeInRow()` use the actual printed effect header when the card is video-backed: `When Played (Front Row)` must be Front Row; `Sustained (Back Row)` must be Back Row; `When Played:` **without** a row label must not inherit an older web-only row restriction. `3-Unit Line` must not be mistaken for Front/Back Row.
- `tools/audit_runtime_parity.py` and `tools/audit_effect_coverage.py` now model the same **video-first** precedence and print the distinct direct-video / effect-noted counts.
- `ShadowVideoSourcePriorityTest.kt`: **12** test cases for video precedence, video-note-only provenance, contradictory historical triggers, no invented row restriction, verified combined ability headers and non-video fallback.
- `ShadowOriginalVideoActivationTest.kt`: **8** more targeted **battle engine** scenarios for captured Resilient Marauder damage, Shinka Incendior 3-Unit Line, Shinka Decurion Line aura and suppression, Tenacious Wizard Inspired eligibility, Ayuela Scorched placement, and Wolf's original-video no-row requirement. These are authored regression tests, not proofs they have passed.

## Direct footage inspection

The user's uploaded `batch 14.mp4` was sampled as a contact sheet. Recorded card faces corroborate:

- **Buhr:** When Played (Column): Grants +3 Power to all units in a Column.
- **Thagrius:** When Played (6-Unit Line): Damage equal to double the number of Line units.
- **Sabikis:** When Played (Back Row): Grants a unit Power equal to the Power of the unit to its left.

These already agree with the project's active handlers, so their wording/effects were preserved.

The Kotlin printed-row regex was independently typechecked against six representative header examples with command-line `kotlinc` (**six examples passed**). This is **not** an Android/Gradle build or the JUnit suite. The additional source and tests were pushed, but have not been run. No GitHub Actions were invoked.

## Remaining verification protocol

For each card requiring further correction, use the user-recorded card detail first, then a real-game match showing active and inactive placement, Power/damage arithmetic, and any chained effects. Update the exact card text and mechanic together, add a deterministic regression, and run `run_local_engine_checks.bat` locally once the Android workstation is available. Never substitute a TodayInDestiny description for contradictory original footage.
