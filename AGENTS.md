# Microbot Agent Guide

Guidance for AI agents building Microbot scripts with the new `microbot/api` queryable layer.

## Scope & Paths
- Primary plugin code: `runelite-client/src/main/java/net/runelite/client/plugins/microbot`.
- Queryable API docs: `.../microbot/api/QUERYABLE_API.md`; quick read: `api/README.md`.
- Keep new scripts inside the microbot plugin; share helpers under `microbot/util`.

## Build & Test
- Fast build: `mvn -pl runelite-client -am package` (jar in `runelite-client/target/`).
- Unit tests: `mvn -pl runelite-client test`.
- CI parity: `./ci/build.sh` (runs `mvn verify --settings ci/settings.xml`).

## Style Rules
- Java 11 target, tabs for indentation, braces match `MicrobotPlugin.java`, prefer <120 chars/line.
- Name types in UpperCamelCase, members in lowerCamelCase; configs prefixed with plugin name (e.g., `ExampleConfig`).

## Script Pattern
Pair a RuneLite `Plugin` with a `Script` that runs on a background thread; never sleep on the client thread.

```java
@PluginDescriptor(name = "Gathering Demo")
public class GatheringPlugin extends Plugin {
	@Inject private GatheringScript script;
	@Override protected void startUp() { script.run(); }
	@Override protected void shutDown() { script.shutdown(); }
}

@Slf4j
public class GatheringScript extends Script {
	@Override
	public boolean run() {
		mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
			try {
				if (!Microbot.isLoggedIn() || !super.run()) return;

				Rs2TileObjectModel tree = new Rs2TileObjectQueryable()
					.withName("Tree")
					.where(obj -> !Rs2Player.isAnimating())
					.nearest();

				if (tree != null) {
					tree.click("Chop down");
					sleepUntil(() -> Rs2Player.isAnimating(), 3000);
				}
			} catch (Exception e) {
				log.error("Loop error", e);
			}
		}, 0, 600, TimeUnit.MILLISECONDS); // ~1 tick
		return true;
	}
}
```

## Queryable API Cheatsheet
- **NPCs**
	```java
	Rs2NpcModel banker = new Rs2NpcQueryable()
		.withNames("Banker", "Bank clerk")
		.where(npc -> !npc.isInteracting())
		.nearest(15);
	if (banker != null) banker.click("Bank");
	```
- **Ground items**
	```java
	Rs2TileItemModel loot = new Rs2TileItemQueryable()
		.where(Rs2TileItemModel::isLootAble)
		.where(item -> item.getTotalGeValue() >= 3000)
		.nearest(10);
	if (loot != null) loot.pickup();
	```
- **Tile objects**
	```java
	Rs2TileObjectModel bankChest = new Rs2TileObjectQueryable()
		.withNames("Bank chest", "Bank booth")
		.nearest(20);
	if (bankChest != null && !Rs2Bank.isOpen()) {
		bankChest.click("Bank");
		sleepUntil(Rs2Bank::isOpen, 5000);
	}
	```
- **Players**
	```java
	Rs2PlayerModel ally = new Rs2PlayerQueryable()
		.where(Rs2PlayerModel::isFriend)
		.within(20)
		.nearest();
	```

## Safety & Timing
- Always guard logic with `Microbot.isLoggedIn()` and `super.run()`; bail early when paused.
- Use `sleep`/`sleepUntil` only on script threads; wrap client access with `Microbot.getClientThread().runOnClientThread(...)` when needed.
- Wait for state changes after interactions (`Rs2Bank.isOpen()`, `Rs2Player.isAnimating()`, inventory/bank counts).
- Limit query radius with `.within(...)` to reduce overhead and cache results inside a loop when reused.
