# Rock token — exclusive Xur summoning rule

User-requested restriction (21 October 2026): **Rock is summon-only and can only enter gameplay through Xur**.

- Rock remains a distinct **Humans / Rare / 1 Power / 1 Light** card with the printed effect **"No special effects."**
- Rock is visible in **Card Collection** and keeps its `card_art/rock.webp` static image (standard 500 × 754 format).
- Rock is **excluded from Deck Construction suggestions** and cannot be added by tapping, imported from a text list, included in a legal deck, directly played from hand, or generated into the CPU deck.
- The battle engine authorizes Rock summons only from an Xur origin, including at the low-level spawning function. Xur's Sustained ability still places it into a random empty opposing Front Row slot after Xur receives a Power bonus.
- **Conflict:** Batch 09's in-game video for **Common Cover** explicitly says “Randomly places 2 Rocks in the opponent's front row.” The video-authoritative card text is intentionally retained in `verified_video_cards.tsv` and `card_details.tsv`, but the Rock creation is blocked by the user's newer Xur-only requirement. **This makes Common Cover's Rock-generation effect inactive until the conflicting requirements are resolved.** It has not been silently changed to summon a different token.
- Local regression tests include Xur summoning, rejected direct/manual Rock play, rejected deck insertion, generic non-Xur summoning, and Common Cover's blocked Rock generation.

No GitHub Actions jobs or device testing were run in this change.
