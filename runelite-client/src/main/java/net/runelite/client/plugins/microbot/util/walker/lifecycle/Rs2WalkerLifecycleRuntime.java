package net.runelite.client.plugins.microbot.util.walker.lifecycle;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.shortestpath.ShortestPathPlugin;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.Pathfinder;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Slf4j
public final class Rs2WalkerLifecycleRuntime {

    private Rs2WalkerLifecycleRuntime() {
    }

    public static void applyWalkerDestination(WorldPoint target) {
        if (target == null) {
            return;
        }
        if (!Microbot.isLoggedIn()) {
            log.warn("Unable to apply walker destination: not logged in");
            return;
        }
        Client client = Microbot.getClient();
        if (client == null) {
            log.warn("Unable to apply walker destination: client unavailable");
            return;
        }
        Player localPlayer = Microbot.getClientThread().invoke(() -> client.getLocalPlayer());
        if (!ShortestPathPlugin.isStartPointSet() && localPlayer == null) {
            log.warn("Start point is not set and player is null");
            return;
        }

        WorldMapPointManager wmm = Microbot.getWorldMapPointManager();
        if (wmm == null) {
            Rs2Walker.clearWalkingRoute("walker:wmm-unavailable retry-setTarget dest=" + target);
            return;
        }
        wmm.removeIf(x -> x == ShortestPathPlugin.getMarker());
        ShortestPathPlugin.setMarker(new WorldMapPoint(target, ShortestPathPlugin.MARKER_IMAGE));
        ShortestPathPlugin.getMarker().setName("Target");
        ShortestPathPlugin.getMarker().setTarget(ShortestPathPlugin.getMarker().getWorldPoint());
        ShortestPathPlugin.getMarker().setJumpOnClick(true);
        wmm.add(ShortestPathPlugin.getMarker());

        WorldPoint start = Microbot.getClientThread().invoke(() -> {
            if (client.getTopLevelWorldView().isInstance()) {
                LocalPoint localLoc = Rs2Player.getLocalLocation();
                WorldPoint computed = localLoc != null ? WorldPoint.fromLocalInstance(client, localLoc) : null;
                if (computed == null) {
                    log.warn("[Walker] setTarget: instance localPoint conversion returned null (localLoc={} target={}) — falling back to raw world location",
                            localLoc, target);
                    computed = Rs2Player.getWorldLocation();
                }
                WorldPoint exitPortal = net.runelite.client.plugins.microbot.shortestpath.PohPanel.getExitPortalTile();
                if (exitPortal != null) {
                    Microbot.log("[Walker] In POH instance — remapping pathfinder start " + computed
                            + " -> exit portal " + exitPortal);
                    computed = exitPortal;
                }
                return computed;
            }
            return Rs2Player.getWorldLocation();
        });
        ShortestPathPlugin.setLastLocation(start);
        final Pathfinder pathfinder = ShortestPathPlugin.getPathfinder();
        if (ShortestPathPlugin.isStartPointSet() && pathfinder != null) {
            start = pathfinder.getStart();
        }
        final WorldPoint startPoint = start;
        Microbot.getClientThread().runOnSeperateThread(() -> restartPathfinding(startPoint, target));
    }

    public static boolean restartPathfinding(WorldPoint start, WorldPoint end) {
        return restartPathfinding(start, Set.of(end));
    }

    public static boolean restartPathfinding(WorldPoint start, Set<WorldPoint> ends) {
        Pathfinder pathfinder = ShortestPathPlugin.getPathfinder();
        if (pathfinder != null) {
            pathfinder.cancel();
            if (ShortestPathPlugin.getPathfinderFuture() != null) {
                ShortestPathPlugin.getPathfinderFuture().cancel(true);
            }
        }

        if (ShortestPathPlugin.getPathfindingExecutor() == null) {
            ThreadFactory shortestPathNaming = new ThreadFactoryBuilder().setNameFormat("shortest-path-%d").build();
            ShortestPathPlugin.setPathfindingExecutor(Executors.newSingleThreadExecutor(shortestPathNaming));
        }

        WorldPoint refreshTarget = ends != null && !ends.isEmpty() ? ends.iterator().next() : null;
        ShortestPathPlugin.getPathfinderConfig().refresh(refreshTarget);
        if (Rs2Player.isInCave()) {
            pathfinder = new Pathfinder(ShortestPathPlugin.getPathfinderConfig(), start, ends);
            pathfinder.run();
            try {
                ShortestPathPlugin.getPathfinderConfig().setIgnoreTeleportAndItems(true);
                Pathfinder pathfinderWithoutTeleports = new Pathfinder(ShortestPathPlugin.getPathfinderConfig(), start, ends);
                pathfinderWithoutTeleports.run();

                boolean noTeleportPathAvailable = !pathfinderWithoutTeleports.getPath().isEmpty();
                boolean basePathAvailable = pathfinder != null && !pathfinder.getPath().isEmpty();
                if (!noTeleportPathAvailable) {
                    ShortestPathPlugin.setPathfinder(basePathAvailable ? pathfinder : pathfinderWithoutTeleports);
                    return true;
                }

                WorldPoint lastPath = pathfinderWithoutTeleports.getPath().get(pathfinderWithoutTeleports.getPath().size() - 1);
                int reachedDistance = Rs2Walker.config != null ? Rs2Walker.config.reachedDistance() : 10;
                boolean pathWithoutTeleportsIsReachable = lastPath.distanceTo(ends.stream().findFirst().orElse(lastPath)) <= reachedDistance;
                if (pathWithoutTeleportsIsReachable
                        && basePathAvailable
                        && pathfinder.getPath().size() >= pathfinderWithoutTeleports.getPath().size()) {
                    ShortestPathPlugin.setPathfinder(pathfinderWithoutTeleports);
                } else {
                    ShortestPathPlugin.setPathfinder(basePathAvailable ? pathfinder : pathfinderWithoutTeleports);
                }
            } finally {
                ShortestPathPlugin.getPathfinderConfig().setIgnoreTeleportAndItems(false);
            }
        } else {
            ShortestPathPlugin.setPathfinder(new Pathfinder(ShortestPathPlugin.getPathfinderConfig(), start, ends));
            ShortestPathPlugin.setPathfinderFuture(ShortestPathPlugin.getPathfindingExecutor().submit(ShortestPathPlugin.getPathfinder()));
        }
        return true;
    }
}
