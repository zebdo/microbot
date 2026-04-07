# Microbot Project Notes (for Codex)

## Microbot at a Glance
Microbot is a RuneLite-based Old School RuneScape client fork with an always-on hidden plugin that hosts automation scripts. It packages a shaded client for end users and keeps developer ergonomics via Queryable caches and script helpers under `microbot/util`. The build mirrors upstream RuneLite as a composite Gradle setup.

## Tech Stack
- Java target 11 (develop/run with JDK 17+), Gradle wrapper, composite builds (`cache`, `runelite-api`, `runelite-client`, `runelite-gradle-plugin`, `runelite-jshell`).
- RuneLite client + plugin APIs, LWJGL, Guice, Lombok, OkHttp, Gson, Logback/SLF4J.
- CI helper `ci/build.sh` bootstraps `glslang` then runs `./gradlew :buildAll`.

## Repo Map
- `runelite-client/` – Main client (`:client`) containing the Microbot plugin and shaded jar assembly.
- `runelite-api/` – RuneLite API included build consumed by the client.
- `runelite-gradle-plugin/` – Gradle plugins for assemble/index/jarsign tasks.
- `runelite-jshell/` – JShell support artifacts.
- `cache/` – Cache tools/build used by RuneLite.
- `docs/` – User/dev docs and static site assets.
- `config/` – Shared Checkstyle configuration.
- `ci/` – CI build helper script.

## How to Validate Changes
- Fast sanity: `./gradlew :client:compileJava`
- Full build (all included builds): `./gradlew buildAll`
- Assemble shaded client: `./gradlew :client:assemble` (creates `runelite-client/build/libs/*-shaded.jar` and `microbot-<version>.jar`)
- Tests are disabled by default via Gradle config; enable/selectively run if you add tests (`./gradlew :client:runTests` or `runDebugTests`).

## Non-Negotiable Rules
- Never instantiate caches or queryables directly; always use `Microbot.getRs2XxxCache().query()` or `.getStream()` (see `runelite-client/src/main/java/net/runelite/client/plugins/microbot/api/QUERYABLE_API.md`).
- Do not block or sleep on the RuneLite client thread; long work belongs on script/executor threads.
- Never use static sleeps like `sleep(12000)` to wait for game state. Always use conditional dynamic sleeps: `sleepUntil(BooleanSupplier awaitedCondition)` (optionally with a timeout as a safety net). Static delays are race-prone; conditional waits self-document the awaited state and are robust to latency/animation variation.
- Keep logging minimal; avoid PII/session identifiers and respect existing log levels/patterns.
- Preserve the hidden/always-on nature of `MicrobotPlugin` and its config panel wiring.
- Follow Checkstyle/Lombok patterns already in the codebase; do not downgrade security (e.g., telemetry token handling, HTTP clients) without discussion.

## Review Guidelines
- P0: Anything that can crash the client, block the client thread, break world hopping/login, corrupt cache/queryable invariants, or expose credentials/telemetry tokens.
- P1: Regressions to script loop timing, overlay correctness, plugin discovery/config panels, packaging (shaded jar/version props), or build reproducibility.
- Check threading (client vs script), cache access patterns, Gradle task wiring, and error handling around network calls.

## Agentic Testing Loop
- Architecture and protocol for autonomous script testing: `docs/AGENTIC_TESTING_LOOP.md`.
- The inner harness writes structured JSON results + screenshots to `~/.runelite/test-results/`.
- The outer loop (`claude -p` via OAuth session) reads results, fixes code, rebuilds, and re-launches.
- Test mode activated via `-Dmicrobot.test.mode=true -Dmicrobot.test.script=<PluginName>`.

## Microbot CLI (Runtime Agent Control)
- CLI for real-time game interaction during a running session: `docs/MICROBOT_CLI.md`.
- Full HTTP API reference and handler architecture: `docs/AGENT_SERVER.md`.
- The **Agent Server** plugin is enabled by default.
- Exposes: inventory, NPCs, objects, ground items, walking, banking, dialogues, widgets, skills, game state, login, and script lifecycle.
- Login control (`/login`): blocks until login succeeds or fails, detects errors (non-member on members world, bans, auth failures) via `loginIndex`/`loginError`. Auto-dismisses error dialogs on retry — no manual intervention needed. Use `./microbot-cli login now --world 381 --timeout 60` to login from the CLI (blocks until complete).
- Keyboard input (`/keyboard`): type text or press keys (enter, escape, backspace) via `./microbot-cli keyboard type "text"` or `./microbot-cli keyboard enter`.
- Script lifecycle (`/scripts`): start/stop plugins by class name, poll status, submit/retrieve test results. Designed for Microbot-Hub automated testing.
- The CLI (`./microbot-cli`) is a bash wrapper at the repo root; all commands output JSON.

## Working with In-Game Settings
- **Always use the settings search bar** instead of navigating tabs. Tab indices shift on game updates and the "Interfaces" tab was removed — hardcoded tab navigation will break.
- Flow: open Settings tab (548:52) → click "All Settings" → click Search (134:11) → type the setting name via keyboard → find the widget → click it.
- CLI example: `./microbot-cli widgets click 548 52 && sleep 1 && ./microbot-cli widgets click --text "All Settings" && sleep 1 && ./microbot-cli widgets click 134 11 && sleep 1 && ./microbot-cli keyboard type "level-up" && sleep 1 && ./microbot-cli widgets click --text "Show level only"`
- For dropdowns: click the dropdown to open it (`widgets invoke` with `Select` action), then `widgets click --text "Option Text"` to select an option.
- Always verify changes via varbit: `./microbot-cli varbit <id>`.
- See `runelite-client/src/main/java/net/runelite/client/plugins/microbot/util/settings/CLAUDE.md` for detailed widget debugging docs.

## When Unsure
- Start with `docs/ARCHITECTURE.md` and `docs/decisions/` for background.
- Agentic testing: `docs/AGENTIC_TESTING_LOOP.md`.
- Runtime agent CLI: `docs/MICROBOT_CLI.md`; HTTP API: `docs/AGENT_SERVER.md`.
- API and script usage: `runelite-client/src/main/java/net/runelite/client/plugins/microbot/api/README.md` and `QUERYABLE_API.md`.
- Development/setup: `docs/development.md`; installation: `docs/installation.md`.
- Still unclear? Ask in Discord (link in `README.md`) or leave TODO with assumption noted.

