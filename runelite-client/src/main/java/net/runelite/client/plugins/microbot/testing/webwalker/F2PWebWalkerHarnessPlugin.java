package net.runelite.client.plugins.microbot.testing.webwalker;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.testing.TestResult;
import net.runelite.client.plugins.microbot.testing.TestResultWriter;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.walker.WalkerState;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

@PluginDescriptor(
        name = "F2P Web Walker Harness",
        description = "Runs F2P-only in-game webwalker regression routes in test mode",
        tags = {"microbot", "test", "webwalker", "f2p"},
        hidden = true
)
@Slf4j
public class F2PWebWalkerHarnessPlugin extends Plugin {
    private static final String ROUTE_FILTER_PROPERTY = "microbot.webwalker.case";
    private static final String TEST_ROUTE_FILTER_PROPERTY = "microbot.test.webwalker.case";
    private static final String STOP_ON_FAILURE_PROPERTY = "microbot.webwalker.stopOnFailure";
    private static final String TEST_STOP_ON_FAILURE_PROPERTY = "microbot.test.webwalker.stopOnFailure";
    private static final String WALK_TIMEOUT_PROPERTY = "microbot.webwalker.walkTimeoutMs";
    private static final String TEST_WALK_TIMEOUT_PROPERTY = "microbot.test.webwalker.walkTimeoutMs";
    private static final String USE_TELEPORTATION_SPELLS_PROPERTY = "microbot.webwalker.useTeleportationSpells";
    private static final String TEST_USE_TELEPORTATION_SPELLS_PROPERTY = "microbot.test.webwalker.useTeleportationSpells";
    private static final String TEST_SCRIPT_PROPERTY = "microbot.test.script";
    private static final String SCRIPT_NAME = "F2P Web Walker Harness";
    private static final int DEFAULT_WALK_TIMEOUT_MS = 240000;

    @Inject
    private EventBus eventBus;

    private ExecutorService executor;
    private volatile WorldPoint lastLocation;

    @Override
    protected void startUp() {
        if (!isHarnessTarget()) {
            return;
        }

        executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                .setNameFormat("F2PWebWalkerHarness-%d")
                .build());
        executor.submit(this::runHarness);
    }

    @Override
    protected void shutDown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (!isHarnessTarget()) {
            return;
        }

        Player player = Microbot.getClient().getLocalPlayer();
        if (player != null) {
            lastLocation = player.getWorldLocation();
        }
    }

    private void runHarness() {
        WebWalkerTestResult result = new WebWalkerTestResult(SCRIPT_NAME);
        result.routeFilter = property(TEST_ROUTE_FILTER_PROPERTY, ROUTE_FILTER_PROPERTY, "all");
        result.stopOnFailure = Boolean.parseBoolean(property(TEST_STOP_ON_FAILURE_PROPERTY, STOP_ON_FAILURE_PROPERTY, "true"));
        result.walkTimeoutMs = intProperty(TEST_WALK_TIMEOUT_PROPERTY, WALK_TIMEOUT_PROPERTY, DEFAULT_WALK_TIMEOUT_MS);

        int exitCode = 0;
        try {
            List<F2PWebWalkerRoute> routes = F2PWebWalkerRoute.selected(result.routeFilter);
            result.selectedRoutes = routes.stream().map(route -> route.id).collect(Collectors.toList());

            if (!sleepUntil(() -> safeLocation() != null, 60000)) {
                result.addError("Timed out waiting for local player location before starting webwalker routes");
                result.complete("login_failure");
                writeAndExit(result, result.exitCode);
                return;
            }

            applyShortestPathOverrides();

            log.info("[F2PWebWalkerHarness] Starting {} route(s): {}", routes.size(), result.selectedRoutes);
            for (F2PWebWalkerRoute route : routes) {
                if (Thread.currentThread().isInterrupted()) {
                    result.addError("Harness interrupted before route " + route.id);
                    exitCode = 1;
                    break;
                }

                RouteOutcome outcome = runRoute(route, result.walkTimeoutMs);
                result.routes.add(outcome);

                result.addCheck(route.id + " setup", outcome.setupPassed, outcome.setupError);
                result.addCheck(route.id + " route", outcome.passed, outcome.error);

                if (!outcome.passed) {
                    exitCode = 1;
                    if (result.stopOnFailure) {
                        log.warn("[F2PWebWalkerHarness] Stopping on first failed route: {}", route.id);
                        break;
                    }
                }
            }

            result.complete("completed");
            writeAndExit(result, exitCode == 0 ? result.exitCode : exitCode);
        } catch (Throwable t) {
            log.error("[F2PWebWalkerHarness] Harness crashed", t);
            result.addError(t.getClass().getSimpleName() + ": " + t.getMessage());
            result.complete("crash");
            writeAndExit(result, result.exitCode);
        }
    }

    private RouteOutcome runRoute(F2PWebWalkerRoute route, int walkTimeoutMs) {
        RouteOutcome outcome = new RouteOutcome();
        outcome.id = route.id;
        outcome.name = route.name;
        outcome.start = format(route.start);
        outcome.destination = format(route.destination);
        outcome.startTolerance = route.startTolerance;
        outcome.destinationTolerance = route.destinationTolerance;
        outcome.startedAt = Instant.now().toString();

        log.info("[F2PWebWalkerHarness] Running {}: {} -> {}", route.id, route.start, route.destination);

        WorldPoint current = safeLocation();
        outcome.initialLocation = format(current);
        if (current == null) {
            outcome.setupError = "Player location unavailable before setup";
            outcome.error = outcome.setupError;
            return outcome;
        }

        int initialDistance = distance(current, route.start);
        outcome.initialDistanceToStart = initialDistance;
        if (initialDistance > route.startTolerance) {
            long setupStart = System.currentTimeMillis();
            outcome.setupState = walk(route.start, route.startTolerance, walkTimeoutMs);
            outcome.setupDurationMs = System.currentTimeMillis() - setupStart;
        } else {
            outcome.setupState = WalkerState.ARRIVED.name();
            outcome.setupDurationMs = 0;
        }

        WorldPoint beforeRoute = safeLocation();
        outcome.routeStartLocation = format(beforeRoute);
        outcome.distanceToStartAfterSetup = distance(beforeRoute, route.start);
        outcome.setupPassed = WalkerState.ARRIVED.name().equals(outcome.setupState)
                && outcome.distanceToStartAfterSetup <= route.startTolerance;
        if (!outcome.setupPassed) {
            outcome.setupError = "Setup webwalk failed for " + route.id
                    + ": state=" + outcome.setupState
                    + ", location=" + outcome.routeStartLocation
                    + ", distanceToStart=" + outcome.distanceToStartAfterSetup;
            outcome.error = outcome.setupError;
            outcome.finishedAt = Instant.now().toString();
            return outcome;
        }

        long walkStart = System.currentTimeMillis();
        outcome.walkerState = walk(route.destination, route.destinationTolerance, walkTimeoutMs);
        outcome.walkDurationMs = System.currentTimeMillis() - walkStart;

        WorldPoint end = safeLocation();
        outcome.endLocation = format(end);
        outcome.distanceToDestination = distance(end, route.destination);
        outcome.passed = WalkerState.ARRIVED.name().equals(outcome.walkerState)
                && outcome.distanceToDestination <= route.destinationTolerance;
        if (!outcome.passed) {
            outcome.error = "Route webwalk failed for " + route.id
                    + ": state=" + outcome.walkerState
                    + ", location=" + outcome.endLocation
                    + ", distanceToDestination=" + outcome.distanceToDestination;
        }

        outcome.finishedAt = Instant.now().toString();
        log.info("[F2PWebWalkerHarness] {} finished: state={}, end={}, distance={}, duration={}ms",
                route.id, outcome.walkerState, outcome.endLocation, outcome.distanceToDestination, outcome.walkDurationMs);
        return outcome;
    }

    private String walk(WorldPoint destination, int tolerance, int timeoutMs) {
        ExecutorService walkExecutor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                .setNameFormat("F2PWebWalkerLeg-%d")
                .build());
        Future<WalkerState> future = walkExecutor.submit(() -> Rs2Walker.walkWithState(destination, tolerance));

        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS).name();
        } catch (TimeoutException e) {
            future.cancel(true);
            return "TIMEOUT";
        } catch (Exception e) {
            log.warn("[F2PWebWalkerHarness] Webwalker leg failed for destination {}", destination, e);
            return "ERROR:" + e.getClass().getSimpleName();
        } finally {
            walkExecutor.shutdownNow();
        }
    }

    private WorldPoint safeLocation() {
        return lastLocation;
    }

    private void applyShortestPathOverrides() {
        String value = property(TEST_USE_TELEPORTATION_SPELLS_PROPERTY, USE_TELEPORTATION_SPELLS_PROPERTY, "");
        if (value.isBlank()) {
            return;
        }

        boolean useTeleportationSpells = Boolean.parseBoolean(value);
        Map<String, Object> config = new HashMap<>();
        config.put("useTeleportationSpells", useTeleportationSpells);

        Map<String, Object> data = new HashMap<>();
        data.put("config", config);

        eventBus.post(new PluginMessage("shortestpath", "path", data));
        log.info("[F2PWebWalkerHarness] Applied shortest path override: useTeleportationSpells={}", useTeleportationSpells);
    }

    private static boolean isHarnessTarget() {
        return "true".equals(System.getProperty("microbot.test.mode"))
                && System.getProperty(TEST_SCRIPT_PROPERTY, "").contains(SCRIPT_NAME);
    }

    private static String property(String preferred, String legacy, String defaultValue) {
        String value = System.getProperty(preferred);
        if (value != null && !value.isBlank()) {
            return value;
        }

        value = System.getProperty(legacy);
        if (value != null && !value.isBlank()) {
            return value;
        }

        return defaultValue;
    }

    private static int intProperty(String preferred, String legacy, int defaultValue) {
        String value = property(preferred, legacy, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static int distance(WorldPoint from, WorldPoint to) {
        if (from == null || to == null) {
            return Integer.MAX_VALUE;
        }
        return from.distanceTo(to);
    }

    private static String format(WorldPoint point) {
        if (point == null) {
            return null;
        }
        return point.getX() + "," + point.getY() + "," + point.getPlane();
    }

    private static void writeAndExit(WebWalkerTestResult result, int exitCode) {
        TestResultWriter.write(result);
        System.exit(exitCode);
    }

    public static class WebWalkerTestResult extends TestResult {
        public String routeFilter;
        public boolean stopOnFailure;
        public int walkTimeoutMs;
        public List<String> selectedRoutes = new ArrayList<>();
        public List<RouteOutcome> routes = new ArrayList<>();

        public WebWalkerTestResult(String script) {
            super(script);
        }
    }

    public static class RouteOutcome {
        public String id;
        public String name;
        public String start;
        public String destination;
        public int startTolerance;
        public int destinationTolerance;
        public String startedAt;
        public String finishedAt;
        public String initialLocation;
        public int initialDistanceToStart;
        public String setupState;
        public boolean setupPassed;
        public String setupError;
        public long setupDurationMs;
        public String routeStartLocation;
        public int distanceToStartAfterSetup;
        public String walkerState;
        public boolean passed;
        public String error;
        public long walkDurationMs;
        public String endLocation;
        public int distanceToDestination;
    }
}
