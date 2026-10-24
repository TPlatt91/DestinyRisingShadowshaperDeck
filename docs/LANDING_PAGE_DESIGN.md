# Shadow Shaper Duels — Portrait landing page redesign

## Intent

Give the Shadow Shaper Duels home screen a premium, atmospheric Destiny-inspired sci-fi/tactical-game look while retaining the existing navigation, card catalog, full save/continue workflow and strict portrait orientation. This is **original** interface artwork and ornamentation authored in Compose, not a claim that Bungie created or endorsed the app and not copied Bungie art.

## Final launcher hierarchy

The main menu contains **exactly four** primary destinations in the user's requested order:

1. **SKIRMISH** — same existing single-player battle Activity (renamed from SIM BATTLE / NEW BATTLE)
2. **DECK CONSTRUCTION** — existing deck manager, unchanged
3. **CARD COLLECTION** — existing card archive and count, unchanged
4. **RULES** — existing rules page, unchanged

**CONTINUE BATTLE** is a separate, compact icy-blue strip ABOVE the four primary actions **only when an unfinished match save exists**; the action passes the unchanged `resume_last_match=true` flag to the battle Activity. Normal Skirmish still handles the existing Continue vs Start New prompt if a saved match exists, so no player's active game gets silently overwritten.

## Visual design

- Original cinematic blue-black/graphite gradients in place of the previous flat brown list.
- Top DESTINY: RISING eyebrow, much stronger SHADOW SHAPER / DUELS title typography, subtle geometric orbit/survey device and etched border line.
- Deep navy menu panels with chamfered corners, subdued metallic gold accents, distinct vector glyphs (crossed blades, a deck stack, a card archive diamond, an open rulebook), numbered tactical captions, clear subtitles, and right-facing navigation chevrons.
- SKIRMISH gets a restrained brighter gold highlight; the other cards remain clean and subordinate.
- Pressed tiles compress slightly (under 3%), while each tile enters with a brief staggered reveal. A gently animated background glow adds motion without distracting from the navigation.
- Android system status/nav bars match the dark palette. Existing `safeDrawingPadding()` keeps content below the status bar and clear of gesture navigation.
- Compact portrait phones can scroll vertically and the larger headers adapt typography below 355dp width. All navigation entries have a minimum 94dp height and verbal semantics for accessibility.
- Footer displays the live number of cards loaded from the game's catalog, not a hardcoded count, and retains the card-verification disclosure.
- All visuals use local code and inexpensive Compose Canvas geometry. No data/network access, new remote asset, permissions, third-party SDK or external font package was added.

## Implementation / tests

- `ShadowLandingPage.kt`: all visual composition, canvas backdrop/sigil/glyphs, tile interactions, and optional Continue ribbon.
- `ShadowComposeActivity.kt`: home route delegated to this new page, old brown HomeScreen/HomeDestination implementation deleted; unchanged destination callbacks and saved-match availability refresh.
- `ShadowLandingPageGuardTest.kt`: pure-JVM structural checks for the four exact destinations, optional saved-match ribbon, unchanged page navigation, portrait lock, and original offline artwork.

**Verification:** Changes committed. Structural source audit and navigation-guard assertions pass; Android compilation and actual phone rendering have NOT been run in the repository environment. Pull and build locally with the normal Android Studio assembleDebug workflow (do not run GitHub Actions). Test both with and without a saved game, then test a narrow portrait device.
