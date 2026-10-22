# Rock summon-only gameplay rules (Batch 06 / Batch 09 videos)

Rock is a **Humans / Rare / 1 Power / 1 Light** card with **No special effects**. It is **summon-only**, visible in Card Collection, and is **not available for constructed decks, deck import, deck validation, the player/AI draw pile, or ordinary hand play**. `card_art/rock.webp` is the existing static card image.

Two card abilities may summon Rock, both confirmed as **printed effects** by the user's original card-detail recordings:

1. **Xur** — Sustained: on acquiring a Power bonus, summons **one Rock** into a random unoccupied space in the opponent's **Front Row**. If full, no token is created.
2. **Common Cover** — When Played (**Front Row**): creates **two Rocks**, each in a random unoccupied opposing **Front Row** space. Each token occupies its cell before the next is rolled; if only one free space remains, summons one; if none remains, summons none.

The rule is enforced in `ShadowEngine.spawnRandom()` and `ShadowEngine.spawnAt()` via an explicit source allowlist, not simply by naming the card "Rock". Xur and Common Cover handlers are in `ShadowCardEffects.kt`. `ShadowData.Card.isPlayable` and `ShadowDeckRules` exclude Rock from constructed decks and ordinary hand play.

Video accuracy: Batch 09 explicitly displays Common Cover's **two-Rock** ability; the recording verifies printed text rather than an actual in-battle demonstration.

Local JVM regression tests (not executed here) check Xur's Rock summon, Common Cover's 2 Rock placements and limited-space behavior, and prevention of unauthorized deck inclusion and direct summoning. No GitHub Actions minutes were used.
