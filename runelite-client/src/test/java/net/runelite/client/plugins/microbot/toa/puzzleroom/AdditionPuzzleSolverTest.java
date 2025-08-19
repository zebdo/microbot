package net.runelite.client.plugins.microbot.toa.puzzleroom;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.Point;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.client.plugins.microbot.Microbot;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Test framework: JUnit 4.12 with Mockito (mockito-core) and MockitoJUnitRunner.
 *
 * This test suite covers AdditionPuzzleSolver by validating:
 * - Resetting 'solved' on relevant GameObject spawn/despawn IDs
 * - Handling failure chat messages (reset targetNumber and solved)
 * - Parsing target numbers and computing flips when a start tile is present
 * - Graceful behavior when start tile cannot be located
 * - Accurate 5x5 tile-state detection and solution set difference
 * - Ignoring unrelated chat messages
 */
@RunWith(MockitoJUnitRunner.class)
public class AdditionPuzzleSolverTest {

    private AdditionPuzzleSolver solver;

    @Mock private Client client;
    @Mock private WorldView worldView;
    @Mock private Scene scene;

    @Before
    public void setUp() throws Exception {
        solver = new AdditionPuzzleSolver();

        // Reflectively inject mocked Client into Microbot (and fallback Global) since the project doesn't use static mocking.
        injectClientInto("net.runelite.client.plugins.microbot.Microbot", client);
        injectClientInto("net.runelite.client.plugins.microbot.util.Global", client); // fallback if Microbot delegates via Global

        when(client.getTopLevelWorldView()).thenReturn(worldView);
        when(worldView.getScene()).thenReturn(scene);
        when(worldView.getPlane()).thenReturn(0);
    }

    @After
    public void tearDown() throws Exception {
        // Best-effort cleanup to avoid shared state across tests
        nullClientOn("net.runelite.client.plugins.microbot.Microbot");
        nullClientOn("net.runelite.client.plugins.microbot.util.Global");
    }

    // ---------- Helpers ----------

    private void injectClientInto(String className, Client c) throws Exception {
        try {
            Class<?> clazz = Class.forName(className);

            // Try known field names
            for (String fname : new String[]{"client", "CLIENT"}) {
                try {
                    Field f = clazz.getDeclaredField(fname);
                    if (Modifier.isStatic(f.getModifiers())) {
                        f.setAccessible(true);
                        f.set(null, c);
                        return;
                    }
                } catch (NoSuchFieldException ignored) {}
            }

            // Try any static field typed as Client
            for (Field f : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())
                    && (f.getType() == Client.class || f.getType().getName().equals("net.runelite.api.Client"))) {
                    f.setAccessible(true);
                    f.set(null, c);
                    return;
                }
            }

            // Try a static setter method setClient(Client)
            try {
                Method m = clazz.getDeclaredMethod("setClient", Client.class);
                if (Modifier.isStatic(m.getModifiers())) {
                    m.setAccessible(true);
                    m.invoke(null, c);
                    return;
                }
            } catch (NoSuchMethodException ignored) {}

            // If we reach here, injection into this class failed; not fatal if another class holds the reference.
        } catch (ClassNotFoundException ignored) {
            // Class may not exist (e.g., Global fallback not present). Ignore.
        }
    }

    private void nullClientOn(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            for (String fname : new String[]{"client", "CLIENT"}) {
                try {
                    Field f = clazz.getDeclaredField(fname);
                    if (Modifier.isStatic(f.getModifiers())) {
                        f.setAccessible(true);
                        f.set(null, null);
                    }
                } catch (NoSuchFieldException ignored) {}
            }
            for (Field f : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())
                    && (f.getType() == Client.class || f.getType().getName().equals("net.runelite.api.Client"))) {
                    f.setAccessible(true);
                    f.set(null, null);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private Tile[][][] makeTiles3D(Tile[][] planeTiles) {
        Tile[][][] tiles = new Tile[1][][];
        tiles[0] = planeTiles;
        return tiles;
    }

    private Tile mockTileWith(GroundObject ground, GameObject... gameObjects) {
        Tile t = mock(Tile.class);
        when(t.getGroundObject()).thenReturn(ground);
        when(t.getGameObjects()).thenReturn(gameObjects);
        return t;
    }

    private GameObject go(int id) {
        GameObject g = mock(GameObject.class);
        when(g.getId()).thenReturn(id);
        return g;
    }

    private GroundObject gro(int id) {
        GroundObject g = mock(GroundObject.class);
        when(g.getId()).thenReturn(id);
        return g;
    }

    private ChatMessage chat(String msg) {
        ChatMessage cm = mock(ChatMessage.class);
        when(cm.getMessage()).thenReturn(msg);
        return cm;
    }

    private boolean solvedFlag() {
        try {
            Field f = AdditionPuzzleSolver.class.getDeclaredField("solved");
            f.setAccessible(true);
            return (boolean) f.get(solver);
        } catch (Exception e) {
            fail("Reflection access failed for 'solved': " + e.getMessage());
            return false;
        }
    }

    private int targetNumber() {
        try {
            Field f = AdditionPuzzleSolver.class.getDeclaredField("targetNumber");
            f.setAccessible(true);
            return (int) f.get(solver);
        } catch (Exception e) {
            fail("Reflection access failed for 'targetNumber': " + e.getMessage());
            return -1;
        }
    }

    // ---------- Tests ----------

    @Test
    public void resetsSolvedOnRelevantGameObjectSpawnAndDespawn() {
        // Known AdditionTile game object IDs: e.g., FOOT gameObjectId=45395, TRIANGLE=45390
        GameObjectSpawned spawned = mock(GameObjectSpawned.class);
        when(spawned.getGameObject()).thenReturn(go(45395)); // relevant

        GameObjectDespawned despawned = mock(GameObjectDespawned.class);
        when(despawned.getGameObject()).thenReturn(go(45390)); // relevant

        // Precondition: mark solved true (solve() sets solved=true at start)
        solver.solve();
        assertTrue("Precondition: solved should be true after calling solve()", solvedFlag());

        solver.onGameObjectSpawned(spawned);
        assertFalse("Solved should reset to false on relevant spawn", solvedFlag());

        solver.solve();
        assertTrue(solvedFlag());

        solver.onGameObjectDespawned(despawned);
        assertFalse("Solved should reset to false on relevant despawn", solvedFlag());
    }

    @Test
    public void handlesFailureChatMessage() {
        solver.solve();
        assertTrue(solvedFlag());

        solver.onChatMessage(chat("Your party failed to complete the challenge"));
        assertEquals("targetNumber should reset to 0", 0, targetNumber());
        assertFalse("Solved should reset to false", solvedFlag());
    }

    @Test
    public void parsesTargetNumberAndSolves_whenStartTilePresent() {
        int size = 60;
        Tile[][] planeTiles = new Tile[size][size];

        // Place FOOT ground object at a valid start: (36,56) with groundObjectId=45353
        planeTiles[36][56] = mockTileWith(gro(45353));

        // Fill remaining tiles
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if (planeTiles[x][y] == null) {
                    planeTiles[x][y] = mockTileWith(null);
                }
            }
        }

        when(scene.getTiles()).thenReturn(makeTiles3D(planeTiles));

        // With tags; the solver strips tags before matching
        solver.onChatMessage(chat("<col=ffff00>The number 30 has been hastily chipped into the stone.</col>"));

        Set<LocalPoint> flips = solver.getFlips();
        assertNotNull(flips);
        assertFalse(flips.isEmpty());

        // For target 30, OPTIMAL includes index 10 -> (36 + 10%5, 56 - 10/5) = (36, 54)
        LocalPoint expected = LocalPoint.fromScene(36, 54);
        assertTrue(flips.stream().anyMatch(lp -> lp.getSceneX() == expected.getSceneX() && lp.getSceneY() == expected.getSceneY()));
    }

    @Test
    public void solveReturnsWhenNoStartTileFound() {
        int size = 60;
        Tile[][] planeTiles = new Tile[size][size];
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                planeTiles[x][y] = mockTileWith(null);
            }
        }
        when(scene.getTiles()).thenReturn(makeTiles3D(planeTiles));

        solver.onChatMessage(chat("The number 32 has been hastily chipped into the stone."));
        Set<LocalPoint> flips = solver.getFlips();
        assertTrue(flips == null || flips.isEmpty());
    }

    @Test
    public void readTileStatesAndSolutionDifferenceAreCorrect() {
        int size = 60;
        Tile[][] planeTiles = new Tile[size][size];

        // Start tile
        planeTiles[36][56] = mockTileWith(gro(45353));

        // Fill defaults
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if (planeTiles[x][y] == null) {
                    planeTiles[x][y] = mockTileWith(null);
                }
            }
        }

        // Activate tiles for indices 0, 6, 12, 24 using valid game object IDs
        int[] indices = {0, 6, 12, 24};
        int[] ids =     {45388, 45390, 45394, 45395};
        for (int k = 0; k < indices.length; k++) {
            int i = indices[k];
            int x = 36 + (i % 5);
            int y = 56 - (i / 5);
            planeTiles[x][y] = mockTileWith(null, go(ids[k]));
        }

        when(scene.getTiles()).thenReturn(makeTiles3D(planeTiles));

        // For target 31, optimal {0,6,12,13,14}; active {0,6,12,24}; flips should be {13,14}
        solver.onChatMessage(chat("The number 31 has been hastily chipped into the stone."));
        Set<LocalPoint> flips = solver.getFlips();
        assertNotNull(flips);

        LocalPoint p13 = LocalPoint.fromScene(36 + (13 % 5), 56 - (13 / 5));
        LocalPoint p14 = LocalPoint.fromScene(36 + (14 % 5), 56 - (14 / 5));
        LocalPoint p24 = LocalPoint.fromScene(36 + (24 % 5), 56 - (24 / 5));

        assertTrue(flips.stream().anyMatch(lp -> lp.getSceneX() == p13.getSceneX() && lp.getSceneY() == p13.getSceneY()));
        assertTrue(flips.stream().anyMatch(lp -> lp.getSceneX() == p14.getSceneX() && lp.getSceneY() == p14.getSceneY()));
        assertFalse(flips.stream().anyMatch(lp -> lp.getSceneX() == p24.getSceneX() && lp.getSceneY() == p24.getSceneY()));
    }

    @Test
    public void ignoresUnrelatedChatMessages() {
        Set<LocalPoint> before = solver.getFlips();
        solver.onChatMessage(chat("Hello world"));
        solver.onChatMessage(chat("The number is wrongfully formatted"));
        solver.onChatMessage(chat("<col=00ff00>Random tag</col>"));

        Set<LocalPoint> after = solver.getFlips();
        if (before == null) {
            assertTrue(after == null || after.isEmpty());
        } else {
            assertEquals(before, after);
        }
    }

    @Test
    public void detectsAllStartPositions() {
        // SCENE_COORD_STARTS: (36,56), (36,44), (53,56), (53,44)
        Point[] starts = new Point[] {
                new Point(36,56),
                new Point(36,44),
                new Point(53,56),
                new Point(53,44)
        };

        for (Point start : starts) {
            int size = 60;
            Tile[][] planeTiles = new Tile[size][size];
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    planeTiles[x][y] = mockTileWith(null);
                }
            }
            // FOOT ground object id = 45353
            planeTiles[start.getX()][start.getY()] = mockTileWith(gro(45353));

            when(scene.getTiles()).thenReturn(makeTiles3D(planeTiles));
            solver.onChatMessage(chat("The number 20 has been hastily chipped into the stone."));

            Set<LocalPoint> flips = solver.getFlips();
            assertNotNull("Flips should not be null for start " + start, flips);
            assertFalse("Flips should not be empty for start " + start, flips.isEmpty());
        }
    }
}