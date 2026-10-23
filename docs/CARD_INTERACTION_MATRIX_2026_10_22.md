# Shadow Shaper Duels — Per-card interaction matrix (all 121 cards)

Audit: **2026-10-22**, Android Kotlin `main` sources plus packaged card TSVs. This is a **static reference and test-priority inventory**, not successful gameplay QA.

- **121 unique names** (not alternate artwork); **14 BASIC**, **107 effect-bearing** cards.
- Literal name references in main engine/effect source: **107/107** non-basic cards. This does **not** prove trigger activation or effect correctness.
- Video-effect overlays: **85/121**. Detailed metadata rows: **119/121**; incomplete details: **Gwynn**, **Tenacious Shielded Knight**.
- **Priority A: 28; B: 64; C: 29.** These are audit order, not confirmed defects. All rows remain *runtime not verified*.
- Mechanic tags are approximate classifications from printed effect/trigger; they are not confirmed original-game keywords.
- `Video` means a record in `verified_video_cards.tsv`, not the only possible original footage evidence (some non-overlaid cards have notes from videos).
- `Handler ref` indicates only that the **literal card name appears in source**; conditional branches may still be inactive.

| Card | Faction | Mechanic categories | Video overlay | Detailed metadata | Handler ref | Priority | Targeted interaction / open question |
|---|---|---|:---:|:---:|:---:|:---:|---|
| Acolyte | Hive | BASIC | — | Y | Y | B | Deck/board Power only |
| Amu-Atu | Humans | PLAY, DAMAGE | Y | Y | Y | C | When active; row/target/sustained guard and nested effects |
| Attal | Humans | PLAY, POWER | Y | Y | Y | B | When active; row/target/sustained guard and nested effects |
| Augmented Shank | Fallen | PLAY, DAMAGE | Y | Y | Y | B | Target+adjacent splash death wave |
| Ayuela | Humans | SUSTAINED, DAMAGE, SUMMON, TIMED | — | Y | Y | A | Two-turn Scorched stacking and caster ownership |
| Bazinet | Humans | PLAY, DAMAGE, POWER | Y | Y | Y | B | Damage after bonuses; eligible targets |
| Blinder Shank | Fallen | PLAY, STATUS | Y | Y | Y | C | When active; row/target/sustained guard and nested effects |
| Buhr | Cabal | PLAY, POWER, COLUMN | Y | Y | Y | C | When active; row/target/sustained guard and nested effects |
| Carrier | Fallen | PLAY, SUMMON | — | Y | Y | B | Dreg summons, arrival watchers |
| Centurion | Cabal | SUSTAINED, POWER, LINE | Y | Y | Y | B | 4-unit Line aura |
| Chinnawat | Humans | PLAY, POWER | Y | Y | Y | B | When active; row/target/sustained guard and nested effects |
| Clone | Humans | SUSTAINED, DAMAGE, POWER | — | Y | Y | A | Simultaneous death wave vs sequential splash |
| Colossus | Cabal | SUSTAINED, POWER, COLUMN | Y | Y | Y | C | When active; row/target/sustained guard and nested effects |
| Common Cover | Humans | PLAY, SUMMON, RANDOM | Y | Y | Y | B | Two Rocks, blocked slots |
| Cursed Thrall | Hive | PLAY, DAMAGE, RANDOM, END TURN, TIMED, MOVE | — | Y | Y | A | Full opposing Front Row; fuse timing and splash |
| Decurion | Cabal | SUSTAINED, POWER, COLUMN | Y | Y | Y | C | When active; row/target/sustained guard and nested effects |
| Denzin | Humans | SUSTAINED, POWER, SUMMON | Y | Y | Y | A | Buff->Guerrilla->arrival bonus |
| Devil Sentry | Fallen | PLAY, DAMAGE, RANDOM | Y | Y | Y | C | When active; row/target/sustained guard and nested effects |
| Donovan-17 | Humans | BASIC | — | Y | Y | B | Deck/board Power only |
| Dreg | Fallen | BASIC | — | Y | Y | B | Deck/board Power only |
| Drop Pod | Cabal | PLAY, SUMMON, LINE, RANDOM | Y | Y | Y | A | Printed-Line summon pool and left-slot requirement |
| Elite Sniper Vandal | Fallen | SUSTAINED, DAMAGE, POWER | Y | Y | Y | A | Two-turn lifespan; once per target per turn |
| Elite Vandal | Fallen | PLAY, POWER | Y | Y | Y | C | When active; row/target/sustained guard and nested effects |
| Elite Walker | Fallen | SUSTAINED, DAMAGE, RANDOM | Y | Y | Y | A | End-turn damage order, Back Row |
| Estela | Humans | PLAY, POWER | Y | Y | Y | C | When active; row/target/sustained guard and nested effects |
| Exploder Shank | Fallen | PLAY, DAMAGE, RANDOM, END TURN, TIMED, MOVE | — | Y | Y | A | Full opposing Front Row; fuse timing and splash |
| Finnala | Humans | PLAY, POWER | Y | Y | Y | B | When active; row/target/sustained guard and nested effects |
| Gladiator | Cabal | PLAY, DAMAGE | Y | Y | Y | B | When active; row/target/sustained guard and nested effects |
| Goliath Tank | Cabal | PLAY, DAMAGE, LINE | Y | Y | Y | B | Line-qualified whole-row damage |
| Grenade Dreg | Fallen | SUSTAINED, POWER | — | Y | Y | B | When active; row/target/sustained guard and nested effects |
| Guarding Servitor | Fallen | PLAY, STATUS | Y | Y | Y | B | When active; row/target/sustained guard and nested effects |
| Guerrilla | Humans | BASIC | — | Y | Y | B | Deck/board Power only |
| Guerrilla Captain | Humans | SUSTAINED, DAMAGE, POWER, RANDOM | Y | Y | Y | A | Buff->damage->counter-effects |
| Gwynn | Humans | PLAY, DAMAGE, POWER | — | — | Y | A | Detail metadata absent; verify target splash, self penalty |
| Harrow | Humans | PLAY, POWER, RANDOM | Y | Y | Y | C | When active; row/target/sustained guard and nested effects |
| Helhest | Humans | SUSTAINED, DAMAGE, POWER, RANDOM | Y | Y | Y | A | Damage-to-Power ratio and overkill |
| Hexguard | Hive | PLAY, STATUS | — | Y | Y | B | When active; row/target/sustained guard and nested effects |
| High Servitor | Fallen | PLAY, DAMAGE | Y | Y | Y | C | When active; row/target/sustained guard and nested effects |
| Hyssop | Humans | PLAY, POWER | Y | Y | Y | B | Printed video: requires an active Power bonus; Column-aura, injury, suppression and CPU target regressions authored (runtime not run) |
| Ikora | Humans | PLAY, DAMAGE | Y | Y | Y | B | Whole-row damage and deaths |
| Incendior | Cabal | PLAY, DAMAGE | Y | Y | Y | C | When active; row/target/sustained guard and nested effects |
| Inquisitor | Hive | PLAY, DAMAGE | Y | Y | Y | C | When active; row/target/sustained guard and nested effects |
| Jaren | Humans | PLAY, DAMAGE, RANDOM | Y | Y | Y | B | Six individual random damage allocations |
| Jolder | Humans | SUSTAINED, DAMAGE | Y | Y | Y | A | Next damage only; Overshield and reduction |
| Juggernaut | Cabal | SUSTAINED, DAMAGE, COLUMN | Y | Y | Y | B | Column + adjacency outgoing damage |
| Kabr | Humans | PLAY, POWER | Y | Y | Y | C | When active; row/target/sustained guard and nested effects |
| Ketch | Fallen | PLAY, SUMMON | Y | Y | Y | B | Dreg summons, Captain bonuses |
| Kevarrh | Fallen | PLAY, DRAW | Y | Y | Y | C | When active; row/target/sustained guard and nested effects |
| Knight | Hive | PLAY, POWER, STATUS | Y | Y | Y | B | When active; row/target/sustained guard and nested effects |
| Lady Skorri | Humans | PLAY, POWER | Y | Y | Y | C | When active; row/target/sustained guard and nested effects |
| Legionary | Cabal | SUSTAINED, POWER, COLUMN | — | Y | Y | B | When active; row/target/sustained guard and nested effects |
| Luksis-3 | Fallen | PLAY, POWER | Y | Y | Y | C | When active; row/target/sustained guard and nested effects |
| Marauder | Fallen | BASIC | — | Y | Y | B | Deck/board Power only |
| Maru | Humans | PLAY, POWER | Y | Y | Y | B | When active; row/target/sustained guard and nested effects |
| Mind Disruptor | Fallen | SUSTAINED, STATUS | Y | Y | Y | B | Suppression immunity on left |
| Mishra | Humans | PLAY, POWER | — | Y | Y | A | Text discrepancy: in-app +3, TodayInDestiny +2 |
| Molkrul | Fallen | PLAY, DAMAGE, POWER, RANDOM | Y | Y | Y | B | Coin toss, target cap, Overshield |
| Morgran | Humans | PLAY, POWER | — | Y | Y | B | When active; row/target/sustained guard and nested effects |
| Mu Ying | Humans | PLAY, SUMMON | Y | Y | Y | B | When active; row/target/sustained guard and nested effects |
| Ning Fei | Humans | PLAY, SUMMON, RANDOM | — | Y | Y | B | Random Clone, death splash |
| Novice Marauder | Fallen | BASIC | — | Y | Y | B | Deck/board Power only |
| Ogre | Hive | SUSTAINED, DAMAGE, POWER, RANDOM | Y | Y | Y | A | Sequential hits; slain Clone removes later target |
| Pandora-7 | Humans | SUSTAINED, POWER | Y | Y | Y | B | When active; row/target/sustained guard and nested effects |
| Paradoxical Neophyte | Hive | PLAY, SUMMON | Y | Y | Y | B | Shade left summon and blocked slot |
| Paradoxical Neophyte's Shade | Hive | SUSTAINED, POWER, STATUS | Y | Y | Y | B | Inspired Hive aura; summon-only |
| Phalanx | Cabal | SUSTAINED, DAMAGE, LINE | Y | Y | Y | B | 3-unit Line damage reduction |
| Psion | Cabal | PLAY, DAMAGE | Y | Y | Y | C | When active; row/target/sustained guard and nested effects |
| Radegast | Humans | PLAY, POWER | Y | Y | Y | C | When active; row/target/sustained guard and nested effects |
| Resilient Captain | Fallen | SUSTAINED, DAMAGE, SUMMON | — | Y | Y | A | Six summons after triggered Fallen hits |
| Resilient Dreg | Fallen | BASIC | — | Y | Y | B | Deck/board Power only |
| Resilient Grenade Dreg | Fallen | SUSTAINED, DAMAGE, SUMMON, TIMED | Y | Y | Y | A | Multiple Scorched sources and expiry |
| Resilient Marauder | Fallen | PLAY, DAMAGE | Y | Y | Y | C | When active; row/target/sustained guard and nested effects |
| Rock | Humans | BASIC | — | Y | Y | B | Deck/board Power only |
| Rock Pile | Humans | BASIC | — | Y | Y | B | Deck/board Power only |
| Rossi-11 | Humans | PLAY, STATUS | Y | Y | Y | B | Overshield prevention and consumption |
| Sabikis | Fallen | PLAY, POWER | Y | Y | Y | C | When active; row/target/sustained guard and nested effects |
| Saladin | Humans | SUSTAINED, DAMAGE, POWER | Y | Y | Y | A | Multiple sources, lethal hits and retaliation |
| Scorpius | Cabal | SUSTAINED, DAMAGE, RANDOM, END TURN | Y | Y | Y | B | Current one-hit text vs historic version |
| Servitor | Fallen | SUSTAINED, DAMAGE | — | Y | Y | B | When active; row/target/sustained guard and nested effects |
| Shank | Fallen | BASIC | — | Y | Y | B | Deck/board Power only |
| Shinka Centurion | Cabal | SUSTAINED, POWER, COLUMN | Y | Y | Y | B | Column partner and left/right buff |
| Shinka Decurion | Cabal | SUSTAINED, DAMAGE, LINE | — | Y | Y | B | 3-unit Line outgoing aura |
| Shinka Gladiator | Cabal | PLAY, DAMAGE, LINE | Y | Y | Y | B | 5-unit Line + outgoing damage |
| Shinka Incendior | Cabal | PLAY, DAMAGE, LINE | Y | Y | Y | B | When active; row/target/sustained guard and nested effects |
| Shinka Legionary | Cabal | SUSTAINED, POWER, COLUMN | Y | Y | Y | C | When active; row/target/sustained guard and nested effects |
| Shinka Phalanx | Cabal | SUSTAINED, DAMAGE, COLUMN | Y | Y | Y | B | Column damage reduction |
| Shinka Psion | Cabal | PLAY, DAMAGE, LINE | Y | Y | Y | B | When active; row/target/sustained guard and nested effects |
| Shinka Scorpius | Cabal | SUSTAINED, DAMAGE, LINE, RANDOM, END TURN | Y | Y | Y | B | End turn; Line formation and suppression |
| Shinka Vigile | Cabal | PLAY, DAMAGE, LINE | Y | Y | Y | B | When active; row/target/sustained guard and nested effects |
| Shrieker | Hive | SUSTAINED, DAMAGE, POWER, RANDOM | Y | Y | Y | A | Back Row bonus-triggered damage |
| Sparrow | Humans | PLAY, POWER | — | Y | Y | B | When active; row/target/sustained guard and nested effects |
| Tan-2 | Humans | SUSTAINED, POWER | — | Y | Y | A | Next bonus only, nested triggered bonuses |
| Tenacious Hexguard | Hive | PLAY, STATUS | Y | Y | Y | C | When active; row/target/sustained guard and nested effects |
| Tenacious Knight | Hive | PLAY, POWER, STATUS, RANDOM | Y | Y | Y | C | When active; row/target/sustained guard and nested effects |
| Tenacious Ogre | Hive | SUSTAINED, DAMAGE, POWER, RANDOM | Y | Y | Y | A | Four hits; slain Clone removes later target |
| Tenacious Shielded Knight | Hive | PLAY, STATUS | — | — | Y | A | Detail metadata absent; verify adjacency/Inspired |
| Tenacious Thrall | Hive | BASIC | Y | Y | Y | C | Deck/board Power only |
| Tenacious Wizard | Hive | PLAY, STATUS, RANDOM | Y | Y | Y | C | When active; row/target/sustained guard and nested effects |
| Thagrius | Cabal | PLAY, DAMAGE, LINE | Y | Y | Y | B | When active; row/target/sustained guard and nested effects |
| The Disfavored | Hive | SUSTAINED, POWER, RANDOM | Y | Y | Y | A | Feedback-chain limit per action vs turn |
| The Speaker | Humans | SUSTAINED, POWER | Y | Y | Y | A | Buff-to-buff chain and recursion |
| Thinktank | Humans | PLAY, DAMAGE, POWER | Y | Y | Y | B | Highest active Power bonus |
| Thrall | Hive | BASIC | — | Y | Y | B | Deck/board Power only |
| Tomb Ship | Hive | PLAY, SUMMON, RANDOM | Y | Y | Y | A | Random Hive pool, Shade odds, blocked neighbors |
| Tracer Shank | Fallen | PLAY, DAMAGE | — | Y | Y | B | When active; row/target/sustained guard and nested effects |
| Tracker | Fallen | BASIC | Y | Y | Y | C | Deck/board Power only |
| Umbliks Prime | Fallen | PLAY, POWER, RANDOM | Y | Y | Y | C | When active; row/target/sustained guard and nested effects |
| Umeko | Humans | SUSTAINED, DAMAGE, RANDOM | Y | Y | Y | B | Random 1-3 damage; multiple hits |
| Unrelenting Captain | Fallen | SUSTAINED, POWER | — | Y | Y | A | Separate Dreg arrival bonuses |
| Vandal | Fallen | PLAY, DAMAGE | — | Y | Y | B | When active; row/target/sustained guard and nested effects |
| Vanok | Fallen | PLAY, POWER | Y | Y | Y | B | When active; row/target/sustained guard and nested effects |
| Vigile | Cabal | PLAY, POWER | Y | Y | Y | B | When active; row/target/sustained guard and nested effects |
| War Beast | Cabal | BASIC | — | Y | Y | B | Deck/board Power only |
| Wizard | Hive | PLAY, STATUS | Y | Y | Y | B | When active; row/target/sustained guard and nested effects |
| Wolf | Humans | PLAY, DAMAGE, POWER | — | Y | Y | B | 5+ allies, valid 5-Power target |
| Wretch | Fallen | PLAY, DAMAGE | Y | Y | Y | C | When active; row/target/sustained guard and nested effects |
| Wu Ming | Humans | PLAY, STATUS | Y | Y | Y | C | When active; row/target/sustained guard and nested effects |
| Xuan Wei | Humans | PLAY, DAMAGE, MOVE | Y | Y | Y | A | Full-board relocation and legal destination |
| Xur | Humans | SUSTAINED, POWER, SUMMON, RANDOM | Y | Y | Y | A | Buff->Rock->enemy arrival effects |
| Zhiyao | Humans | SUSTAINED, POWER, SUMMON, TIMED | — | Y | Y | A | Separate 3-turn bonuses; suppression + Tan-2 |
| Zhong Wenbin | Humans | PLAY, POWER | — | Y | Y | B | When active; row/target/sustained guard and nested effects |

## Verification rules

- A general play-smoke test can play conditional cards when their prerequisite is **not met**, so passing placement is not passing the printed ability.
- A named card in the engine is not an exhaustive clause implementation, and a card mentioned in a JUnit fixture is not necessarily covered by assertions.
- Verify each priority A mechanic using a legal active-state fixture and at least one suppressed/protected/blocked or counter-effect fixture.
- See `FULL_CARD_INTERACTION_AUDIT_2026_10_22.md` for grouped interaction findings and next action order.
