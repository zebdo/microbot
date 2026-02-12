# runelite-client

What it does: Main RuneLite client fork that embeds the hidden, always-on Microbot plugin, overlays, and config panels. Builds shaded artifacts for end users.

Public entry points
- `net.runelite.client.RuneLite` (application main class)
- `net.runelite.client.plugins.microbot.MicrobotPlugin` (plugin descriptor/boot)
- Gradle tasks: `:client:runDebug`, `:client:assemble` (`shadowJar` + `microbotReleaseJar`)

How to run/test
- Fast compile: `./gradlew :client:compileJava`
- Run with JDWP: `./gradlew :client:runDebug`
- Assemble shaded jars: `./gradlew :client:assemble`
- Tests are disabled by default; if enabled, use `./gradlew :client:runTests` (or `runDebugTests` for debugger).

Key invariants/constraints
- Never block/sleep on the client thread; use script/executor threads and `ClientThread.runOnClientThreadOptional` for client access.
- Use Queryable caches only via `Microbot.getRs2XxxCache().query()`/`.getStream()`; do not instantiate caches/queryables manually.
- Preserve hidden/always-on plugin descriptor and Microbot config panel wiring.
- Version and commit values are injected during `processResources`; keep `gradle.properties` values in sync when packaging.

Links
- Architecture: `../docs/ARCHITECTURE.md`
- ADRs: `../docs/decisions/`
- API guide: `src/main/java/net/runelite/client/plugins/microbot/api/QUERYABLE_API.md`
