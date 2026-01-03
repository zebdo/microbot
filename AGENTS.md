# Microbot API Guide (for Claude)

## Scope & Paths
- Primary plugin code: `runelite-client/src/main/java/net/runelite/client/plugins/microbot`.
- Queryable API docs: `.../microbot/api/QUERYABLE_API.md`; quick read: `api/README.md`.
- Keep new scripts inside the microbot plugin; share helpers under `microbot/util`.
- Config UI for microbot plugins is rendered via `plugins/microbot/ui/MicrobotConfigPanel` (not the default RuneLite config panel); put config UI changes there.

## Paths & Builds
- Plugin sources live in `runelite-client/src/main/java/net/runelite/client/plugins/microbot`.
- The queryable API lives in `.../microbot/api`; full guide: `.../microbot/api/QUERYABLE_API.md`.
- Quick builds: `mvn -pl runelite-client -am package`; tests: `mvn -pl runelite-client test`.

## Queryable API Quick Reference
Prefer the queryable API over legacy util calls.

## Interaction & Timing Tips
- Never sleep on the RuneLite client thread; use the script thread with `sleep(...)` / `sleepUntil(...)`.
- After interactions, wait for state changes (e.g., `Rs2Bank.isOpen()`, `Rs2Player.isAnimating()`).
- Limit search radius with `.within(...)` to reduce overhead, and cache query results when reusing in a loop.

## Helpful References
- Example templates: `runelite-client/src/main/java/net/runelite/client/plugins/microbot/example/`.
- API examples: `api/*/` directories contain `*ApiExample.java` files for NPCs, tile items, players, and objects.
- Core utilities (legacy but still useful): `microbot/util` (e.g., `Rs2Inventory`, `Rs2Bank`, `Rs2Walker`).

## QuestScript Loop (Quest Helper)
- `QuestScript.run(config, plugin)` sets a 400–1000ms fixed-delay loop; exits early if quest helper is toggled off, not logged in, paused (`super.run()`), or no quest is selected, and waits out player animations.
- Captures the active `QuestStep`, marks when dialogue starts, auto-chooses matching dialogue options, and clicks highlighted widgets (special shop buy for Pirate's Treasure).
- Runs quest-specific logic via `QuestRegistry.getQuest(...).executeCustomLogic()` (Pirate's Treasure gets the plugin injected).
- While incomplete: handles dialogue quirks (Cook's Assistant/Pirate's Treasure), exits cutscenes, clears walk targets when talking, and manages reachability flags.
- Requirement phase: equips required items, warns on missing items (rate-limited), and attempts to acquire them by looting nearby or walking toward the defined point; prioritizes item-on-item detailed steps before other step types.
- Dispatch order: `ConditionalStep` → `NpcStep` → `ObjectStep` → `DigStep` → `PuzzleStep`; per-type handlers choose the correct menu action, manage line-of-sight and walkable tiles, and call `sleepUntil` to wait for movement/animation/interactions before looping.
