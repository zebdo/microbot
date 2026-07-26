# ADR-0005: Integrate shortest-path as a pinned catalog

## Status

Accepted

## Context

Microbot's web walker originated from
[Skretzo/shortest-path](https://github.com/Skretzo/shortest-path), but the two
implementations now have different responsibilities:

- upstream owns a frequently updated collision map, transport catalog, requirement
  model, and route-planning features;
- Microbot owns automated transport execution, runtime recovery, local collision
  corrections, dangerous-tile avoidance, route smoothing, A* heuristics, and
  banking/POH integration.

Upstream transport TSVs cannot be copied over the existing Microbot resources.
The current schemas use different column names and object-action encodings, and
upstream intentionally does not execute transports.

## Decision

Microbot will:

1. vendor an explicitly pinned upstream commit, including its license and source
   metadata;
2. treat upstream collision and transport data as a read-only base catalog;
3. apply Microbot-owned transport, restriction, dangerous-tile, and blocked-edge
   overlays after the base catalog is loaded;
4. retain Microbot's pathfinding algorithms and walker recovery behavior;
5. carry the exact selected transport identity in the computed route;
6. make a transport eligible for automated planning only when an execution
   capability is registered for it;
7. update the pin through reviewed source changes, never by downloading mutable
   data at client runtime.

The initial pin is `e3dc7c5a621ca9cdd4c404ca4da5654b603286e7`
(`shortest-path` version `1.20.6`).

## Consequences

- Collision and transport updates become auditable and reproducible.
- Microbot-specific behavior is not overwritten by upstream updates.
- New catalog entries fail closed until their executor is implemented and tested.
- Synchronization needs a lint/differential test suite and a small amount of
  adapter maintenance when the upstream schema changes.
- Catalog requirements, collision-backed routing, and execution intents can be
  validated without a login using the
  [headless transport harness](../SHORTEST_PATH_TESTING.md).
