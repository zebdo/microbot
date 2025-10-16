# Repository Guidelines

## Project Structure & Module Organization
- The root `pom.xml` controls the multi-module Maven build for `cache`, `runelite-api`, `runelite-client`, `runelite-jshell`, and `runelite-maven-plugin`.
- Gameplay automation lives in `runelite-client/src/main/java/net/runelite/client/plugins/microbot`; keep new scripts and utilities inside this plugin.
- Shared helpers sit under `.../microbot/util`, while runnable examples live in `.../microbot/example`.
- Tests mirror sources in `runelite-client/src/test/java`, and project documentation and walkthroughs are kept in `docs/`.
- CI helpers and custom Maven settings are in `ci/`, and distributable jars land in `runelite-client/target/`.

## Build, Test, and Development Commands
- `mvn -pl runelite-client -am package` builds the client and produces `target/microbot-<version>.jar`.
- `./ci/build.sh` recreates the CI pipeline, fetching `glslangValidator` and running `mvn verify --settings ci/settings.xml`.
- `mvn -pl runelite-client test` runs the unit suite; add `-DskipTests` only when packaging binaries for distribution.
- `java -jar runelite-client/target/microbot-<version>.jar` launches a locally built client for manual validation.

## Coding Style & Naming Conventions
- Target Java 11 (`maven-compiler-plugin` uses `<release>11</release>`); rely on Lombok for boilerplate where already adopted.
- Keep indentation with tabs, follow the brace placement already in `MicrobotPlugin.java`, and prefer lines under 120 characters.
- Use `UpperCamelCase` for types, `lowerCamelCase` for members, and prefix configuration interfaces with the plugin name (e.g., `ExampleConfig`).
- Centralize shared logic in util classes rather than duplicating inside scripts; inject dependencies through RuneLite’s DI when needed.

## Testing Guidelines
- Write JUnit 4 tests (`junit:4.12`) under matching package paths in `runelite-client/src/test/java`.
- Name test classes with the `*Test` suffix and break scenarios into focused `@Test` methods that assert observable client state.
- Use Mockito (`mockito-core:3.1.0`) for client services; rely on `guice-testlib` when event bus wiring is involved.
- Run `mvn test` (or `mvn verify` before release) locally before opening a pull request and attach logs when failures require review.

## Commit & Pull Request Guidelines
- Follow the existing conventional commit style: `type(scope): summary` (e.g., `refactor(Rs2Walker): expand teleport keywords`).
- Squash noisy work-in-progress commits before pushing and keep summaries under 72 characters.
- PRs should explain the gameplay scenario, note affected plugins, link related issues or scripts, and include screenshots or clips when UI overlays change.
- Confirm tests/builds in the PR description and mention any follow-up tasks or config changes reviewers must perform.

## Agent-Specific Instructions
- Register new automation under `net.runelite.client.plugins.microbot` and reuse the scheduler pattern shown in `ExampleScript`.
- Expose reusable behaviour through `microbot/util` packages so scripts stay thin and composable.
- When adding panel controls or overlays, update the Microbot navigation panel setup in `MicrobotPlugin` and provide default config values.
- Document new APIs in `docs/api/` and cross-link from `docs/development.md` so contributors can discover capabilities quickly.
