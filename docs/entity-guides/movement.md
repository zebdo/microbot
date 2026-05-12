# Movement Gotchas

## 1. Do not recurse on failed minimap clicks without changing the click target

`Rs2Walker.processWalk` holds the walker lock while processing a path. If a minimap click is rejected because the calculated point is outside the minimap clip, immediately recursing with the same target can spin forever while still holding the lock. Shrink the click target toward the player or otherwise change the condition before retrying.

**Why this matters:** Quest steps that walk to a nearby object can repeatedly calculate a valid path but never move, starving other walk requests because the walker lock is never released.

**Pattern to follow:**

```java
WorldPoint clickTarget = getPointWithWallDistance(targetWp);
boolean clicked = Rs2Walker.walkMiniMap(clickTarget);
if (!clicked)
{
	clicked = walkMiniMapToward(clickTarget, playerLoc, MINIMAP_REACH_EUCLIDEAN - 1);
}
```

**Where this applies:** `Rs2Walker`, `Rs2MiniMap`, and shortest-path walking loops.

**Defensive check:** When debugging stalls, compare pathfinder logs with `./microbot-cli state`. A repeating valid path with an unchanged player position usually means the click layer failed after pathing succeeded.

## 2. Probe raw path obstacles before declaring the walker stuck

Path smoothing can collapse many adjacent raw path tiles into one minimap waypoint. Some doors and gates are not represented as blocking collision in the pathfinder map, so the smoothed segment may legally cross them while hiding the exact tile the object handler needs to inspect. Run nearby raw-path door/object checks as soon as the raw path is longer than the smoothed path and the obstacle is in scene range; do not wait for `stuckCount` to increment first.

**Why this matters:** A walk from Varrock castle's upper floors toward Varrock fountain can descend correctly, then stall at the plane-1 castle door because the smoothed waypoint skips over the door tile and the normal per-segment door check never sees it.

**Pattern to follow:**

```java
if (rawPath != null && path != null && rawPath.size() > path.size()
        && handleNearbyRawPathSceneObjects(rawPath, HANDLER_RANGE)) {
    doorOrTransportResult = true;
}
```

**Where this applies:** `Rs2Walker`, `PathSmoother`, and shortest-path obstacle handling.

**Defensive check:** When a path stalls beside a visible door while the pathfinder reports a complete route, compare raw and smoothed path lengths; if the raw path is longer, verify nearby raw-path obstacle probing happens before stall recovery.

## 3. Match wall doors by crossed edge, not nearby tile

Wall-object doors block the edge between the wall object's tile and the neighboring tile indicated by its orientation. Raw-path segment probes must only treat a wall door as relevant when the path segment actually transitions across that edge. Do not match a wall door merely because the path starts on, ends on, or passes near one side of the door.

**Why this matters:** At Draynor Manor's east/back door, the player can stand on the south-side door tile and need to walk southwest into the room. A broad "door near segment" match repeatedly re-opens the back door instead of allowing the next minimap walk step to run.

**Pattern to follow:**

```java
WorldPoint doorTile = wall.getWorldLocation();
WorldPoint blockedNeighbor = getWallDoorNeighborPoint(wall.getOrientationA(), doorTile);
return isDoorEdgeTransition(previousPathTile, nextPathTile, doorTile, blockedNeighbor);
```

**Where this applies:** `Rs2Walker.handleNearbyRawPathSceneObjects`, `Rs2Walker.findDoorNearSegment`, and any wall-door probe that uses `WallObject.getOrientationA()`.

**Defensive check:** Add a unit test for a path starting on the door's blocked-neighbor tile and moving away from the door; it must return false.

## 4. Do not raw-probe doors while the player is already moving

Raw-path scene-object probing is a recovery aid for smoothed paths that hide nearby obstacles. Once a door interaction has started movement, let that movement settle or reach the door edge before probing again. Re-running raw probes while the player is still moving can repeatedly interact with the same door and prevent the normal minimap/path step from taking over.

**Why this matters:** When leaving Draynor Manor through the east/back door, the walker can click the door, start moving toward it, then immediately re-enter raw-path probing and click the same door again instead of continuing through the path outside.

**Pattern to follow:**

```java
if (Rs2Player.isMoving()) {
    return false;
}
waitForDoorInteractionProgress(fromWp, toWp);
```

**Where this applies:** `Rs2Walker.handleNearbyRawPathSceneObjects`, door handlers that call `Rs2GameObject.interact`, and any recovery logic that recurses into `processWalk`.

**Defensive check:** In live testing, a door should produce one interaction followed by movement/path progress, not repeated `Raw path door handler resolved obstacle` messages every tick while the player is moving.

## 5. Suppress the inverse adjacent transport after crossing a same-plane door

Some doors are represented in `transports.tsv` as two adjacent same-plane transports, one for each direction. After the walker clicks one side and arrives on the other, immediately accepting the inverse transport can bounce the player back through the same door instead of letting the next minimap step continue away from it. Mark both tiles of a successful adjacent same-plane transport as recently handled for a short window.

**Why this matters:** Leaving Draynor Manor through the east/back door can alternate between `3123,3360,0` and `3123,3361,0`, repeatedly logging raw-path/current-tile transport handling and burning the route timeout before walking back to Draynor.

**Pattern to follow:**

```java
boolean reachedDestination = sleepUntil(() -> atTransportDestination(transport), 5000);
if (reachedDestination && isAdjacentSamePlaneTransport(transport)) {
    markStationaryDoorOpened(transport.getOrigin());
    markStationaryDoorOpened(transport.getDestination());
}
```

**Where this applies:** `Rs2Walker.handleTransports`, current-tile transport recovery, raw-path transport probing, and bidirectional same-plane door/gate transports.

**Defensive check:** A successful adjacent same-plane transport should be followed by a minimap/path step away from the doorway, not by alternating `Raw path transport handler` and `Current-tile transport handler` logs for the same two tiles.

## 6. Recalculate after long-distance object transports

Not every large map transition changes plane or uses a teleport type. Some object transports, such as the Varrock Sewers ladder, remain on plane 0 while jumping between coordinate bands. After a successful object interaction reaches one of these destinations, run the normal transport finalizer so the shortest path is rebuilt from the new location.

**Why this matters:** A route from Varrock Sewers back to a surface origin can climb the ladder successfully, then continue using a path that was calculated from the underground coordinate band. The walker may drift off path or exit during setup even though the transport itself worked.

**Pattern to follow:**

```java
if (reachedDestination) {
    markAdjacentSamePlaneTransportHandled(transport, object);
    return finishHandledTransport(transport);
}
```

**Where this applies:** `Rs2Walker.handleTransports` object interactions and any object-transport handler that waits for the destination tile directly.

**Defensive check:** Same-plane object transports with a large `distanceTo2D` delta should produce a fresh pathfinder start near the post-transport player location before the next minimap step.

## 7. Model missing collision edges before tuning walker retries

Some static collision gaps are specific edges, not whole tiles. If the pathfinder repeatedly routes through a visible fence/wall and the live client keeps clicking fallback tiles near that boundary, add an explicit blocked edge to pathfinding and smoothing instead of trying to solve it with longer timeouts or broader minimap fallback.

**Why this matters:** The Varrock Palace garden south fence can be missing from the bundled collision map near `3229..3241,3472 -> 3471`. A no-agility F2P route to the Varrock Sewers manhole can walk around the trellis correctly, then stall against that garden boundary because the path says the south edge is traversable.

**Pattern to follow:**

```java
if (config.isBlockedTransportEdge(node.packedPosition, neighborPacked)) {
    continue;
}
```

**Where this applies:** `CollisionMap.getNeighbors`, `PathSmoother.lineOfSight`, and any path data correction where only one edge between adjacent tiles is invalid.

**Defensive check:** Add a core pathfinder regression from the observed stuck tile; assert neither the raw path nor smoothed path crosses the blocked edge, and that the route still reaches the original destination.

## 8. Do not click a visible endpoint before honoring pending route interactions

An endpoint being visible on the minimap does not mean it is the next correct click. If the computed shortest path reaches that endpoint through an intermediate door, gate, transport, shortcut, ladder, or other route object, the walker must process the first route interaction before issuing a direct endpoint click.

**Why this matters:** From Varrock Palace, a destination such as `3229,3473,0` can be visible on the minimap while the shorter route requires opening the palace doors first. Clicking the endpoint lets the game choose a longer collision-valid detour and bypasses the webwalker's route.

**Pattern to follow:**

```java
if (handleNearbyRawPathSceneObjects(rawPath, HANDLER_RANGE)) {
    return true;
}
if (!hasPendingExplicitTransportStepBeforeArrival(rawPath, target, distance)
        && !localRouteDetoursFromComputedRoute(rawPath, end, DIRECT_CLICK_MAX_DISTANCE)) {
    walkMiniMap(end);
}
```

**Where this applies:** `Rs2Walker.walkWithStateInternal`, short local walk kick-starts, final/minimap endpoint clicks, and any future fast-path that bypasses normal path iteration.

**Defensive check:** Reproduce with closed Varrock Palace doors toward `3229,3473,0`; the first action should target the door or route waypoint, not the final endpoint tile.

## 9. Preserve interrupts so walker cancellation stops waits immediately

Ctrl+X and script shutdown cancel the active walk task with `Future.cancel(true)` and clear the walker target. Shared sleep/poll helpers must preserve the interrupted flag and stop polling when interruption is observed; otherwise the walker can continue through several timeout cycles before noticing the cleared target.

**Why this matters:** A user pressing Ctrl+X expects the webwalker to stop issuing route actions immediately. If `InterruptedException` is swallowed, long waits in object, transport, dialogue, or animation handling can keep cycling until their normal timeout elapses.

**Pattern to follow:**

```java
try {
    Thread.sleep(delayMs);
} catch (InterruptedException ignored) {
    Thread.currentThread().interrupt();
}
while (!Thread.currentThread().isInterrupted() && !condition.getAsBoolean()) {
    sleep(pollMs);
}
```

**Where this applies:** `Global.sleep*`, `Global.sleepUntil*`, `Rs2Walker.setTarget(null)`, and any walker helper that waits after clicking a door, shortcut, transport, or minimap tile.

**Defensive check:** Start a long webwalk, press Ctrl+X during movement or a route-object wait, and verify no additional path recalculations or route-object interactions occur after the cancel log.
