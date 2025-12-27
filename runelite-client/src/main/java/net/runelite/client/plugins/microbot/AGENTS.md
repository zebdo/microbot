# Repository Guidelines

## Project Structure & Module Organization
The Maven root (`pom.xml`) orchestrates `cache`, `runelite-api`, `runelite-client`, `runelite-jshell`, and `runelite-maven-plugin`. Gameplay automation belongs under `runelite-client/src/main/java/net/runelite/client/plugins/microbot`, with runnable examples in `.../microbot/example` and shared helpers in `.../microbot/util`. Mirror sources with tests in `runelite-client/src/test/java`, keep docs in `docs/`, CI utilities in `ci/`, and locally built jars in `runelite-client/target/`.

## Build, Test, and Development Commands
- `mvn -pl runelite-client -am package`: builds dependent modules and emits `runelite-client/target/microbot-<version>.jar`.
- `mvn -pl runelite-client test`: runs the client-side JUnit suite; omit `-DskipTests` except for distribution builds.
- `./ci/build.sh`: reproduces CI locally, fetching `glslangValidator` and running `mvn verify --settings ci/settings.xml`.
- `java -jar runelite-client/target/microbot-<version>.jar`: launches the built client for manual validation.

## Coding Style & Naming Conventions
Target Java 11 via `maven-compiler-plugin` (`<release>11</release>`). Use tabs, keep braces consistent with `MicrobotPlugin.java`, and prefer lines under 120 characters. Name types with UpperCamelCase, members with lowerCamelCase, and prefix plugin configs with the plugin name (e.g., `ExampleConfig`). Centralize reusable behavior in `microbot/util` packages and inject dependencies through RuneLite’s DI where appropriate.

## Testing Guidelines
Write JUnit 4 tests (`junit:4.12`) mirrored under `runelite-client/src/test/java`. Name test classes with `*Test`, keep assertions focused, and use Mockito (`mockito-core:3.1.0`) or `guice-testlib` for service wiring. Run `mvn -pl runelite-client test` before committing and `mvn verify` ahead of releases; attach logs if failures require review.

## Commit & Pull Request Guidelines
Follow conventional commits (`type(scope): summary`, ≤72 characters) and squash WIP history. PRs should describe the gameplay scenario, affected plugins, linked issues, and include screenshots or clips for UI changes. State which builds/tests ran and flag any manual setup reviewers must perform.

## Agent-Specific Instructions
Register new automation under `net.runelite.client.plugins.microbot`, leaning on the scheduler pattern shown in `ExampleScript`. Update the Microbot navigation panel when introducing controls or overlays, provide sensible defaults in the config class, and document new APIs in `docs/api/` while cross-linking from `docs/development.md` so contributors can discover capabilities quickly.
