package net.runelite.client.plugins.microbot.toa.puzzleroom;

import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.plugins.microbot.Microbot;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.*;
import java.util.stream.IntStream;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Tests for LightPuzzleSolver.
 *
 * Testing framework and library:
 * - JUnit (assertions from org.junit.Assert)
 * - Mockito (including static mocking via Mockito.mockStatic)
 *
 * These tests mock the static Microbot.getClient() to supply a mocked client/scene/tiles graph.
 * We validate:
 *  - solve(): correctly identifies start tile, reads active light states, computes flip points.
 *  - solve(): gracefully handles missing start tile.
 *  - onChatMessage(): sets solved flag when message starts with "completed!".
 *  - Edge cases for all-lights-on (no flips) and mixed patterns (non-empty flips).
 */
public class LightPuzzleSolverTest
{
    // Constants from implementation (copied to ensure tests remain stable if refactoring occurs)
    private static final int GROUND_OBJECT_LIGHT_BACKGROUND = 45344;
    private static final int GAME_OBJECT_LIGHT_ENABLED = 45384;

    private MockedStatic<Microbot> microbotStatic;
    private Client client;
    private Scene scene;

    @Before
    public void setUp()
    {
        client = mock(Client.class, RETURNS_DEEP_STUBS);
        scene = mock(Scene.class, RETURNS_DEEP_STUBS);
        when(client.getScene()).thenReturn(scene);
        when(client.getPlane()).thenReturn(0);

        microbotStatic = Mockito.mockStatic(Microbot.class);
        microbotStatic.when(Microbot::getClient).thenReturn(client);

        // Prevent accidental NPEs if code logs; ignore Microbot.log calls
        microbotStatic.when(() -> Microbot.log(anyString())).then(inv -> null);
    }

    @After
    public void tearDown()
    {
        if (microbotStatic != null)
        {
            microbotStatic.close();
        }
    }

    /**
     * Utility: create a tiles grid sized [104][104], filled with a safe default Tile
     * that has no ground object and has an empty gameObjects array.
     * We then override certain coordinates as needed per test.
     */
    private Tile[][] newTilesGrid()
    {
        final int SIZE = 104; // RuneLite scenes typically 104x104
        Tile[][] grid = new Tile[SIZE][SIZE];

        Tile defaultTile = mock(Tile.class, RETURNS_DEEP_STUBS);
        when(defaultTile.getGroundObject()).thenReturn(null);
        when(defaultTile.getGameObjects()).thenReturn(new GameObject[6]);

        for (int x = 0; x < SIZE; x++)
        {
            Arrays.fill(grid[x], defaultTile);
        }
        return grid;
    }

    private GroundObject newGroundObject(int id)
    {
        GroundObject go = mock(GroundObject.class);
        when(go.getId()).thenReturn(id);
        return go;
    }

    private GameObject newGameObject(int id)
    {
        GameObject go = mock(GameObject.class);
        when(go.getId()).thenReturn(id);
        return go;
    }

    /**
     * Helper to set a start point with a GROUND_OBJECT_LIGHT_BACKGROUND so findStartTile finds it.
     */
    private void setStartTile(Tile[][] grid, Point start)
    {
        Tile startTile = mock(Tile.class, RETURNS_DEEP_STUBS);
        when(startTile.getGroundObject()).thenReturn(newGroundObject(GROUND_OBJECT_LIGHT_BACKGROUND));
        when(startTile.getGameObjects()).thenReturn(new GameObject[6]);
        grid[start.getX()][start.getY()] = startTile;
    }

    /**
     * Helper to define active/inactive states for the 8 lights in the 3x3 (without center),
     * using the indexing scheme from LightPuzzleSolver:
     *   index i from 0..7
     *   tileIx = i > 3 ? i + 1 : i;
     *   x = tileIx % 3; y = tileIx / 3;
     *   coord = (topLeft.x + x*2, topLeft.y - y*2)
     */
    private void setLightStates(Tile[][] grid, Point topLeft, boolean[] activeStates)
    {
        for (int i = 0; i < 8; i++)
        {
            int tileIx = i > 3 ? i + 1 : i;
            int x = tileIx % 3;
            int y = tileIx / 3;
            int sx = topLeft.getX() + (x * 2);
            int sy = topLeft.getY() - (y * 2);

            Tile tile = mock(Tile.class, RETURNS_DEEP_STUBS);
            when(tile.getGroundObject()).thenReturn(null);

            if (activeStates[i])
            {
                when(tile.getGameObjects()).thenReturn(new GameObject[]{ newGameObject(GAME_OBJECT_LIGHT_ENABLED) });
            }
            else
            {
                when(tile.getGameObjects()).thenReturn(new GameObject[0]);
            }

            grid[sx][sy] = tile;
        }
    }

    /**
     * Compute expected flip points based on the LIGHTS_PUZZLE_XOR_ARRAY logic:
     * flips are those indices where xor bit = 1, with xor initialized as 0 and
     * XOR-ing the pattern for each OFF light (active=false).
     *
     * The array content is read from the SUT via reflection to avoid duplication.
     */
    private Set<LocalPoint> expectedFlips(Point topLeft, boolean[] activeStates)
    {
        // Reflect LIGHTS_PUZZLE_XOR_ARRAY from SUT to keep test synced
        int[] xorArray;
        try
        {
            java.lang.reflect.Field field = LightPuzzleSolver.class.getDeclaredField("LIGHTS_PUZZLE_XOR_ARRAY");
            field.setAccessible(true);
            xorArray = (int[]) field.get(null);
        }
        catch (Exception e)
        {
            throw new AssertionError("Unable to access LIGHTS_PUZZLE_XOR_ARRAY via reflection", e);
        }

        int xor = 0;
        for (int i = 0; i < 8; i++)
        {
            if (!activeStates[i])
            {
                xor ^= xorArray[i];
            }
        }

        Set<LocalPoint> points = new HashSet<>();
        for (int i = 0; i < 8; i++)
        {
            int mask = 1 << i;
            if ((xor & mask) == mask)
            {
                int tileIx = i > 3 ? i + 1 : i;
                int x = tileIx % 3;
                int y = tileIx / 3;
                points.add(LocalPoint.fromScene(topLeft.getX() + (x * 2), topLeft.getY() - (y * 2)));
            }
        }
        return points;
    }

    @Test
    public void solve_whenAllLightsActive_producesNoFlips()
    {
        // Arrange: choose one of the known starts from the SUT; use (36,56)
        Point start = new Point(36, 56);
        Tile[][] tiles = newTilesGrid();
        setStartTile(tiles, start);

        // all lights active
        boolean[] active = new boolean[8];
        Arrays.fill(active, true);
        setLightStates(tiles, start, active);

        when(scene.getTiles()).thenReturn(tiles);

        LightPuzzleSolver solver = new LightPuzzleSolver();

        // Act
        solver.solve();

        // Assert
        Set<LocalPoint> flips = solver.getFlips();
        assertNotNull("Flips set should not be null", flips);
        assertTrue("No flips expected when all lights are active", flips.isEmpty());
        assertFalse("Solved should initially be false", solver.isSolved());
    }

    @Test
    public void solve_whenMixedStates_producesExpectedFlips()
    {
        // Arrange: choose a different start to ensure start detection loop works; use (53,44)
        Point start = new Point(53, 44);
        Tile[][] tiles = newTilesGrid();
        setStartTile(tiles, start);

        // Define a mixed pattern of active lights
        boolean[] active = new boolean[]{
            true,  // i=0
            false, // i=1
            true,  // i=2
            false, // i=3
            false, // i=4
            true,  // i=5
            false, // i=6
            true   // i=7
        };
        setLightStates(tiles, start, active);

        when(scene.getTiles()).thenReturn(tiles);

        LightPuzzleSolver solver = new LightPuzzleSolver();

        // Act
        solver.solve();

        // Assert: compute expected flips using the same XOR logic
        Set<LocalPoint> expected = expectedFlips(start, active);
        Set<LocalPoint> actual = solver.getFlips();

        assertNotNull(actual);
        assertEquals("Flip set should match expected mapping from XOR result", expected, actual);

        // Some sanity assertions
        assertFalse("A mixed state should generally produce some flips", actual.isEmpty());
        // Verify the generated points are from the valid 3x3 grid (skip center)
        for (LocalPoint lp : actual)
        {
            int dx = lp.getSceneX() - start.getX();
            int dy = start.getY() - lp.getSceneY(); // inverse because y decreases downward in solver
            assertEquals(0, dx % 2);
            assertEquals(0, dy % 2);
            int gx = dx / 2;
            int gy = dy / 2;
            assertTrue(gx >= 0 && gx <= 2);
            assertTrue(gy >= 0 && gy <= 2);
            // (1,1) is center and should not be present
            assertFalse("Center tile should be skipped", gx == 1 && gy == 1);
        }
    }

    @Test
    public void solve_whenStartTileNotFound_logsAndProducesEmptyFlips()
    {
        // Arrange: No start tile set with background id
        Tile[][] tiles = newTilesGrid();
        when(scene.getTiles()).thenReturn(tiles);

        LightPuzzleSolver solver = new LightPuzzleSolver();

        // Capture whether log was called (best-effort)
        final boolean[] logCalled = { false };
        microbotStatic.when(() -> Microbot.log(anyString())).then(inv -> { logCalled[0] = true; return null; });

        // Act
        solver.solve();

        // Assert
        assertTrue("Should log when start tile is not found", logCalled[0]);
        assertNotNull(solver.getFlips());
        assertTrue("Flips should remain empty when no start tile", solver.getFlips().isEmpty());
        assertFalse("Solved should remain false", solver.isSolved());
    }

    @Test
    public void onChatMessage_setsSolvedTrueWhenCompletedPrefix()
    {
        LightPuzzleSolver solver = new LightPuzzleSolver();

        ChatMessage msg = mock(ChatMessage.class);
        when(msg.getMessage()).thenReturn("completed! You solved the puzzle.");

        solver.onChatMessage(msg);

        assertTrue("Solved should be set to true when message starts with 'completed!'", solver.isSolved());
    }

    @Test
    public void onChatMessage_doesNotSetSolvedForOtherMessages()
    {
        LightPuzzleSolver solver = new LightPuzzleSolver();

        ChatMessage msg1 = mock(ChatMessage.class);
        when(msg1.getMessage()).thenReturn("not completed!");
        solver.onChatMessage(msg1);
        assertFalse("Solved should remain false for unrelated messages", solver.isSolved());

        ChatMessage msg2 = mock(ChatMessage.class);
        when(msg2.getMessage()).thenReturn("Completed! (wrong case)"); // case sensitive check
        solver.onChatMessage(msg2);
        assertFalse("Solved remains false due to case-sensitive startsWith", solver.isSolved());
    }

    @Test
    public void solve_usesCorrectStartAmongMultipleCandidates()
    {
        // Arrange: Provide background id on multiple possible starts but ensure the first in order is chosen
        // Order in SUT: (36,56), (36,44), (53,56), (53,44)
        Tile[][] tiles = newTilesGrid();

        Point first = new Point(36, 56);
        Point second = new Point(36, 44);
        setStartTile(tiles, first);
        setStartTile(tiles, second);

        // Light states for 'first' start: all off --> should yield some flips
        boolean[] activeFirst = new boolean[8];
        Arrays.fill(activeFirst, false);
        setLightStates(tiles, first, activeFirst);

        // For 'second' start: set a different pattern (should be ignored if first is chosen)
        boolean[] activeSecond = new boolean[]{ true, true, true, true, true, true, true, true };
        setLightStates(tiles, second, activeSecond);

        when(scene.getTiles()).thenReturn(tiles);

        LightPuzzleSolver solver = new LightPuzzleSolver();

        // Act
        solver.solve();

        // Assert: ensure flips correspond to the 'first' pattern (all off), not the second
        Set<LocalPoint> expectedFirst = expectedFlips(first, activeFirst);
        assertEquals("Solver should prefer the first matching start tile", expectedFirst, solver.getFlips());
        assertFalse("All-off should produce non-empty flips", solver.getFlips().isEmpty());
    }
}