# Development

Guidelines for setting up Microbot locally, building it, and creating scripts.

## Prerequisites
- Java 17+ (same as RuneLite); install a JDK, not just a JRE.
- Git and the included Gradle wrapper (`./gradlew`); no system Gradle needed.
- IntelliJ IDEA is recommended. Follow the RuneLite wiki for IntelliJ setup basics: https://github.com/runelite/runelite/wiki/Building-with-IntelliJ-IDEA

## Project Layout
- Core plugin: `runelite-client/src/main/java/net/runelite/client/plugins/microbot`
- Helpers/utilities: `microbot/util` within the plugin tree
- Queryable API docs: `runelite-client/src/main/java/net/runelite/client/plugins/microbot/api/QUERYABLE_API.md` (quick overview: `api/README.md`)
- Config UI for microbot plugins: `runelite-client/src/main/java/net/runelite/client/plugins/microbot/ui/MicrobotConfigPanel`
- Included builds: `settings.gradle.kts` composes `cache`, `runelite-api`, `runelite-client`, and `runelite-jshell`

## Build & Run
- Quick compile (client only): `./gradlew :runelite-client:compileJava`
- Full build (all included builds): `./gradlew build`
- Clean everything: `./gradlew cleanAll`
- Tests (client): `./gradlew :runelite-client:test`
- Shaded jars for end users are published in releases; for local testing, run from IntelliJ or Gradle.

## IDE Setup (IntelliJ)
1) Open the root `build.gradle.kts` as a Gradle project.  
2) Set Project SDK and Gradle JVM to Java 17.  
3) Let IntelliJ import included builds; the main sources live under `runelite-client`.  
4) Create a Run Configuration (Gradle) for `:runelite-client:run` if you want to launch the client directly.

## Developing Scripts
- Place new scripts inside the microbot plugin folder: `runelite-client/src/main/java/net/runelite/client/plugins/microbot`.
- Reusable helpers belong in `microbot/util`.
- Config UI goes in `microbot/ui/MicrobotConfigPanel`.
- Prefer the Queryable API over legacy util calls:
  - Never instantiate caches or queryables directly.
  - Use `Microbot.getRs2XxxCache().query()` (NPCs, players, tile items, tile objects) or cache streams.
  - When filtering by name (`withName` / `withNames`), resolve on the client thread using the `*OnClientThread` terminal helpers: `firstOnClientThread()`, `nearestOnClientThread(...)`, or `toListOnClientThread()`.
  - For boat world views, call `.fromWorldView()` on the query.
  - Examples live under `api/*/*ApiExample.java`.

### Minimal Queryable Example
```java
var banker = Microbot.getRs2NpcCache().query()
    .withName("Banker")
    .nearestOnClientThread();

if (banker != null) {
    banker.interact("Bank");
}
```

### Script Loop Template
```java
public class ExampleScript extends Script {
    public static double version = 1.0;

    public boolean run(ExampleConfig config) {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            if (!super.run()) return;
            try {
                if (Microbot.isLoggedIn() && !Rs2Player.isAnimating()) {
                    var npc = Microbot.getRs2NpcCache().query()
                        .withName("Man")
                        .nearestOnClientThread();
                    if (npc != null) {
                        npc.interact("Attack");
                    }
                }
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }, 0, 800, TimeUnit.MILLISECONDS);
        return true;
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
}
```

## Additional References
- Installation steps and launcher notes: `docs/installation.md`
- API guide and examples: `runelite-client/src/main/java/net/runelite/client/plugins/microbot/api`
- Example scripts: `runelite-client/src/main/java/net/runelite/client/plugins/microbot/example/`

Are you stuck? Join our [Discord](https://discord.gg/zaGrfqFEWE) server.
