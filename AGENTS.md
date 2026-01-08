# Microbot API Guide (for Claude)

## Scope & Paths
- Primary plugin code: `runelite-client/src/main/java/net/runelite/client/plugins/microbot`.
- Queryable API docs: `.../microbot/api/QUERYABLE_API.md`; quick read: `api/README.md`.
- Keep new scripts inside the microbot plugin; share helpers under `microbot/util`.
- Config UI for microbot plugins is rendered via `plugins/microbot/ui/MicrobotConfigPanel` (not the default RuneLite config panel); put config UI changes there.

## Paths & Builds
- Plugin sources live in `runelite-client/src/main/java/net/runelite/client/plugins/microbot`.
- The queryable API lives in `.../microbot/api`; full guide: `.../microbot/api/QUERYABLE_API.md`.
- Quick builds: `./gradlew :runelite-client:compileJava`; full build: `./gradlew build`.

## Cache & Queryable API

### Singleton Pattern
All caches are `@Singleton` classes injected via Guice and exposed through `Microbot` static getters:

```java
@Singleton
public final class Rs2NpcCache {
    @Inject
    public Rs2NpcCache(Client client, ClientThread clientThread) { ... }

    public Stream<Rs2NpcModel> getStream() { ... }
    public Rs2NpcQueryable query() { return new Rs2NpcQueryable(); }
}
```

### Usage Rules

**CRITICAL:** Never instantiate caches or queryables directly. Always access via `Microbot.getRs2XxxCache()`:

```java
// ❌ WRONG - Don't instantiate caches
Rs2NpcCache cache = new Rs2NpcCache();

// ❌ WRONG - Don't instantiate queryables directly
Rs2NpcModel npc = new Rs2NpcQueryable().withName("Banker").nearest();

// ✅ CORRECT - Use Microbot.getRs2XxxCache().query()
Rs2NpcModel npc = Microbot.getRs2NpcCache().query()
    .withName("Banker")
    .nearest();

// ✅ CORRECT - Other caches
Rs2TileObjectModel tree = Microbot.getRs2TileObjectCache().query().withName("Tree").nearest();
Rs2TileItemModel item = Microbot.getRs2TileItemCache().query().withName("Bones").nearest();
Rs2PlayerModel player = Microbot.getRs2PlayerCache().query().withName("PlayerName").first();

// ✅ CORRECT - Direct stream access
Rs2NpcModel firstNpc = Microbot.getRs2NpcCache().getStream()
    .filter(npc -> npc.getName() != null)
    .findFirst()
    .orElse(null);

// ✅ CORRECT - Boat cache (no queryable, direct methods)
Rs2BoatModel boat = Microbot.getRs2BoatCache().getLocalBoat();
```

### Available Caches

| Cache | Accessor | Methods |
|-------|----------|---------|
| `Rs2NpcCache` | `Microbot.getRs2NpcCache()` | `.query()`, `.getStream()` |
| `Rs2PlayerCache` | `Microbot.getRs2PlayerCache()` | `.query()`, `.getStream()` |
| `Rs2TileItemCache` | `Microbot.getRs2TileItemCache()` | `.query()`, `.getStream()` |
| `Rs2TileObjectCache` | `Microbot.getRs2TileObjectCache()` | `.query()`, `.getStream()` |
| `Rs2BoatCache` | `Microbot.getRs2BoatCache()` | `.getLocalBoat()`, `.getBoat(player)` |

### Boat World View Support
When querying objects on a boat, use `.fromWorldView()` to search the player's current world view:

```java
// Query objects in the boat's world view
var sails = Microbot.getRs2TileObjectCache().query()
    .fromWorldView()
    .withName("Sails")
    .nearest();
```

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
