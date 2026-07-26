# Headless shortest-path transport testing

The headless transport harness validates catalog requirements, route selection,
the pinned collision map, and the walker interaction intent without starting or
logging in a RuneLite client.

Run the focused suite:

```bash
./gradlew :client:runUnitTests \
  --tests 'net.runelite.client.plugins.microbot.shortestpath.HeadlessTransportHarnessTest'
```

`HeadlessTransportState` supplies synthetic membership, boosted skills, quest
states, varbits, varplayers, inventory/equipment quantities, currency, feature
toggles, and the global teleport toggle. `HeadlessTransportHarness` loads the
packaged transport catalog and collision map, filters transports through the
same pure requirement policies used by `PathfinderConfig`, and runs the real
`Pathfinder`.

Each selected transport produces a side-effect-free
`TransportExecutionRegistry.ExecutionIntent`. The intent identifies the walker
executor and records its object/NPC/widget-facing metadata: origin, destination,
object ID, action, target, and display option.

The harness intentionally cannot prove that a live scene contains the expected
object or NPC, that a widget layout has not changed, or that dialogue/animation
timing succeeds. Keep a small logged-in smoke test for those runtime contracts.
