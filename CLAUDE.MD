# Microbot API Guide (for Claude)

Short notes for writing automation scripts with the Microbot plugin inside RuneLite.

## Paths & Builds
- Plugin sources live in `runelite-client/src/main/java/net/runelite/client/plugins/microbot`.
- The queryable API lives in `.../microbot/api`; full guide: `.../microbot/api/QUERYABLE_API.md`.
- Quick builds: `mvn -pl runelite-client -am package`; tests: `mvn -pl runelite-client test`.

## Script Skeleton
Use a Plugin + Script pair. Keep sleeps off the client thread and always check login state.

```java
@PluginDescriptor(name = "Goblin Demo", description = "Queryable API example")
public class GoblinDemoPlugin extends Plugin {
	@Inject private GoblinDemoScript script;

	@Override protected void startUp() { script.run(); }
	@Override protected void shutDown() { script.shutdown(); }
}

@Slf4j
public class GoblinDemoScript extends Script {
	@Override
	public boolean run() {
		mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
			try {
				if (!Microbot.isLoggedIn() || !super.run()) return;

				Rs2NpcModel target = new Rs2NpcQueryable()
					.withName("Goblin")
					.where(npc -> !npc.isInteracting())
					.nearest(10);

				if (target != null && !Rs2Player.isInCombat()) {
					target.click("Attack");
					sleepUntil(() -> Rs2Player.isInCombat(), 2000);
				}
			} catch (Exception e) {
				log.error("Loop error", e);
			}
		}, 0, 600, TimeUnit.MILLISECONDS); // ~1 game tick
		return true;
	}
}
```

## Queryable API Quick Reference
Prefer the queryable API over legacy util calls.

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
		.where(item -> item.getTotalGeValue() >= 5000)
		.nearest(10);
	if (loot != null) loot.pickup();
	```

- **Tile objects**
	```java
	Rs2TileObjectModel tree = new Rs2TileObjectQueryable()
		.where(obj -> obj.getName() != null && obj.getName().contains("Tree"))
		.nearest();
	if (tree != null && !Rs2Player.isAnimating()) {
		tree.click("Chop down");
		sleepUntil(() -> Rs2Player.isAnimating(), 3000);
	}
	```

- **Players**
	```java
	Rs2PlayerModel teammate = new Rs2PlayerQueryable()
		.where(Rs2PlayerModel::isFriend)
		.within(20)
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
