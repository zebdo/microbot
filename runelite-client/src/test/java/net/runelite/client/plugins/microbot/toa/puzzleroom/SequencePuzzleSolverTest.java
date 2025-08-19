/*
 Note on test stack:
 - Using JUnit 5 (org.junit.jupiter) and Mockito (including inline static mocking via MockedStatic).
 - If the repository uses JUnit 4, adjust imports/annotations accordingly.
*/
package net.runelite.client.plugins.microbot.toa.puzzleroom;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GraphicsObject;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.scene.Scene;
import net.runelite.api.WorldView;
import net.runelite.api.World;
import net.runelite.client.plugins.microbot.Microbot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SequencePuzzleSolver focusing on public event handlers and private state via observation.
 * We validate happy paths, edge cases, and failure conditions derived from the original diff.
 */
public class SequencePuzzleSolverTest
{
    // Constants from SequencePuzzleSolver (mirrored for clarity in tests)
    // private static final int GROUND_OBJECT_ID = 45340;
    // private static final int DISPLAY_GAME_OBJECT_ID = 45341;
    // private static final int STEPPED_GAME_OBJECT_ID = 45342;
    // private static final int GRAPHICS_OBJECT_RESET = 302;

    private static final int GROUND_OBJECT_ID = 45340;
    private static final int DISPLAY_GAME_OBJECT_ID = 45341;
    private static final int STEPPED_GAME_OBJECT_ID = 45342;
    private static final int GRAPHICS_OBJECT_RESET = 302;

    private SequencePuzzleSolver solver;
    private MockedStatic<Microbot> microbotStatic;

    @BeforeEach
    void setUp()
    {
        solver = new SequencePuzzleSolver();
        // Mock static Microbot by default; individual tests can override behavior as needed.
        microbotStatic = mockStatic(Microbot.class, withSettings().lenient());
    }

    @AfterEach
    void tearDown()
    {
        if (microbotStatic != null)
        {
            microbotStatic.close();
        }
    }

    // Utility to set private boolean puzzleFinished
    private void setPuzzleFinished(boolean value)
    {
        try
        {
            Field f = SequencePuzzleSolver.class.getDeclaredField("puzzleFinished");
            f.setAccessible(true);
            f.set(solver, value);
        }
        catch (Exception e)
        {
            fail("Failed to set puzzleFinished via reflection: " + e.getMessage());
        }
    }

    // Utility to read private int lastDisplayTick
    private int getLastDisplayTick()
    {
        try
        {
            Field f = SequencePuzzleSolver.class.getDeclaredField("lastDisplayTick");
            f.setAccessible(true);
            return (int) f.get(solver);
        }
        catch (Exception e)
        {
            fail("Failed to get lastDisplayTick via reflection: " + e.getMessage());
            return -1;
        }
    }

    // Utility to call private reset() to verify state resets for tests that can't reach it via events
    private void invokeReset()
    {
        try
        {
            var m = SequencePuzzleSolver.class.getDeclaredMethod("reset");
            m.setAccessible(true);
            m.invoke(solver);
        }
        catch (Exception e)
        {
            fail("Failed to invoke reset via reflection: " + e.getMessage());
        }
    }

    private GameObjectSpawned mockGameObjectSpawned(int gameObjectId, LocalPoint localPoint)
    {
        GameObjectSpawned evt = mock(GameObjectSpawned.class);
        GameObject go = mock(GameObject.class);
        Tile tile = mock(Tile.class);

        when(evt.getGameObject()).thenReturn(go);
        when(go.getId()).thenReturn(gameObjectId);
        when(evt.getTile()).thenReturn(tile);
        when(tile.getLocalLocation()).thenReturn(localPoint);

        return evt;
    }

    private GraphicsObjectCreated mockGraphicsObjectCreated(int graphicsId, LocalPoint loc)
    {
        GraphicsObjectCreated evt = mock(GraphicsObjectCreated.class);
        GraphicsObject go = mock(GraphicsObject.class);
        when(evt.getGraphicsObject()).thenReturn(go);
        when(go.getId()).thenReturn(graphicsId);
        when(go.getLocation()).thenReturn(loc);
        return evt;
    }

    private Client setupClientForTick(int tick)
    {
        Client client = mock(Client.class);
        when(client.getTickCount()).thenReturn(tick);

        // For graphics reset tests (top-level world view and scene)
        WorldView worldView = mock(WorldView.class);
        when(client.getTopLevelWorldView()).thenReturn(worldView);
        when(worldView.getPlane()).thenReturn(0);

        // Fallbacks to avoid NPE in tests that don't reach deeper calls
        Scene scene = mock(Scene.class);
        when(worldView.getScene()).thenReturn(scene);

        // We create a small 2x2 scene grid by default. Tests can stub more precisely if needed.
        Tile[][][] tiles3d = new Tile[1][104][104]; // RuneLite scene typical size is 104x104; safe default
        when(scene.getTiles()).thenReturn(tiles3d);

        return client;
    }

    @Test
    void incrementsCompletedTilesOnSteppedGameObject()
    {
        // Arrange
        int initial = solver.getCompletedTiles();
        LocalPoint lp = new LocalPoint(32, 32);
        GameObjectSpawned stepped = mockGameObjectSpawned(STEPPED_GAME_OBJECT_ID, lp);

        // Act
        solver.onGameObjectSpawned(stepped);

        // Assert
        assertEquals(initial + 1, solver.getCompletedTiles(), "completedTiles should increment on STEPPED_GAME_OBJECT_ID");
    }

    @Test
    void ignoresNullEventOrNullGameObjectGracefully()
    {
        // Null event
        assertDoesNotThrow(() -> solver.onGameObjectSpawned(null), "Null event should be ignored without exception");

        // Event with null game object
        GameObjectSpawned evt = mock(GameObjectSpawned.class);
        when(evt.getGameObject()).thenReturn(null);
        assertDoesNotThrow(() -> solver.onGameObjectSpawned(evt), "Event with null game object should be ignored");
    }

    @Test
    void earlyReturnWhenPuzzleFinished()
    {
        // Arrange
        setPuzzleFinished(true);
        LocalPoint lp = new LocalPoint(64, 64);
        GameObjectSpawned stepped = mockGameObjectSpawned(STEPPED_GAME_OBJECT_ID, lp);
        int before = solver.getCompletedTiles();

        // Act
        solver.onGameObjectSpawned(stepped);

        // Assert
        assertEquals(before, solver.getCompletedTiles(), "When puzzle is finished, event should be ignored");
    }

    @Test
    void recordsDisplayTileAndResetsCompletedTilesOnDisplaySpawnDifferentTicks()
    {
        // Arrange
        LocalPoint displayPoint = new LocalPoint(100, 100);
        GameObjectSpawned displayEvt = mockGameObjectSpawned(DISPLAY_GAME_OBJECT_ID, displayPoint);

        // Client tick 1
        Client clientTick1 = setupClientForTick(1);
        microbotStatic.when(Microbot::getClient).thenReturn(clientTick1);

        // First display should add point and reset completed tiles
        solver.onGameObjectSpawned(displayEvt);
        assertEquals(0, solver.getCompletedTiles(), "completedTiles should be reset to 0 on display object");
        // We can't inspect points directly (private), but we can infer no exceptions and track lastDisplayTick changed
        int last1 = getLastDisplayTick();
        assertEquals(1, last1, "lastDisplayTick should be updated to current client tick");

        // Now simulate stepping on a tile to increment completedTiles
        LocalPoint lp = new LocalPoint(120, 120);
        GameObjectSpawned stepped = mockGameObjectSpawned(STEPPED_GAME_OBJECT_ID, lp);
        solver.onGameObjectSpawned(stepped);
        assertEquals(1, solver.getCompletedTiles(), "completedTiles should be incremented after a step");

        // Client tick 2, another display spawn should clear completed tiles again and record new tick
        Client clientTick2 = setupClientForTick(2);
        microbotStatic.when(Microbot::getClient).thenReturn(clientTick2);

        solver.onGameObjectSpawned(displayEvt);
        assertEquals(0, solver.getCompletedTiles(), "completedTiles should be reset again on subsequent display");
        int last2 = getLastDisplayTick();
        assertEquals(2, last2, "lastDisplayTick should reflect latest tick");
    }

    @Test
    void doubleDisplaySameTickTriggersResetAndFinish()
    {
        // Arrange
        LocalPoint displayPoint = new LocalPoint(200, 200);
        GameObjectSpawned displayEvt = mockGameObjectSpawned(DISPLAY_GAME_OBJECT_ID, displayPoint);

        Client clientTick = setupClientForTick(5);
        microbotStatic.when(Microbot::getClient).thenReturn(clientTick);

        // First display at tick 5: should add point, reset completed tiles, and set lastDisplayTick
        solver.onGameObjectSpawned(displayEvt);
        assertEquals(0, solver.getCompletedTiles());

        int beforeTick = getLastDisplayTick();
        assertEquals(5, beforeTick);

        // Second display in the same tick (tick 5 again) should call reset() and mark puzzleFinished true, then return
        // We verify by attempting to increment tiles after this and seeing it ignored.
        solver.onGameObjectSpawned(displayEvt);

        // Attempt to step after puzzle marked finished: should be ignored
        LocalPoint lp = new LocalPoint(210, 210);
        solver.onGameObjectSpawned(mockGameObjectSpawned(STEPPED_GAME_OBJECT_ID, lp));
        assertEquals(0, solver.getCompletedTiles(), "Steps after finish should not increment completedTiles");

        // Also verify lastDisplayTick was reset to 0 by reset()
        assertEquals(0, getLastDisplayTick(), "reset should zero lastDisplayTick");
    }

    @Test
    void graphicsResetClearsStateWhenGroundObjectMatches()
    {
        // Arrange client and scene with a ground object at graphics location
        Client client = setupClientForTick(10);

        // Prepare a tile with a ground object ID == GROUND_OBJECT_ID
        Tile[][][] tiles = client.getTopLevelWorldView().getScene().getTiles();
        int plane = client.getTopLevelWorldView().getPlane();

        // Create a Tile mock with ground object id = GROUND_OBJECT_ID at scene coordinates [10][15]
        int sceneX = 10, sceneY = 15;
        Tile targetTile = mock(Tile.class, RETURNS_DEEP_STUBS);
        GameObject groundObject = mock(GameObject.class);
        when(groundObject.getId()).thenReturn(GROUND_OBJECT_ID);
        when(targetTile.getGroundObject()).thenReturn(groundObject);

        // Ensure tiles array is non-null and place our tile
        // Provide a larger array if null to avoid NPE
        if (tiles == null)
        {
            Scene scene = client.getTopLevelWorldView().getScene();
            Tile[][][] newTiles = new Tile[1][104][104];
            when(scene.getTiles()).thenReturn(newTiles);
            tiles = newTiles;
        }
        tiles[plane][sceneX][sceneY] = targetTile;

        microbotStatic.when(Microbot::getClient).thenReturn(client);

        // Increment some state first
        solver.onGameObjectSpawned(mockGameObjectSpawned(STEPPED_GAME_OBJECT_ID, new LocalPoint(1, 1)));
        assertEquals(1, solver.getCompletedTiles());

        // Act: send graphics reset event at same scene coordinates with matching graphics id
        LocalPoint graphicsLoc = new LocalPoint(sceneX << 7, sceneY << 7); // LocalPoint sceneX/sceneY mapping
        solver.onGraphicsObjectCreated(mockGraphicsObjectCreated(GRAPHICS_OBJECT_RESET, graphicsLoc));

        // Assert: state should be reset by reset()
        assertEquals(0, solver.getCompletedTiles(), "Graphics reset should clear completedTiles");
        assertEquals(0, getLastDisplayTick(), "Graphics reset should clear lastDisplayTick");
    }

    @Test
    void graphicsNonResetOrMismatchedGroundObjectDoesNothing()
    {
        // Arrange baseline state
        solver.onGameObjectSpawned(mockGameObjectSpawned(STEPPED_GAME_OBJECT_ID, new LocalPoint(1, 1)));
        assertEquals(1, solver.getCompletedTiles());

        // Case 1: Non-reset graphics id
        assertDoesNotThrow(() -> solver.onGraphicsObjectCreated(
            mockGraphicsObjectCreated(GRAPHICS_OBJECT_RESET + 1, new LocalPoint(0, 0))
        ));
        assertEquals(1, solver.getCompletedTiles(), "Non-reset graphics id should not change state");

        // Case 2: Reset graphics id but scene has no matching ground object
        Client client = setupClientForTick(0);
        microbotStatic.when(Microbot::getClient).thenReturn(client);

        // tiles array remains null entries, so getGroundObject() will be null -> no reset
        LocalPoint graphicsLoc = new LocalPoint(5 << 7, 6 << 7);
        solver.onGraphicsObjectCreated(mockGraphicsObjectCreated(GRAPHICS_OBJECT_RESET, graphicsLoc));
        assertEquals(1, solver.getCompletedTiles(), "Reset graphics with no matching ground object should not change state");
    }

    @Test
    void resetViaReflectionClearsAllInternalState()
    {
        // Arrange: simulate some changes
        solver.onGameObjectSpawned(mockGameObjectSpawned(STEPPED_GAME_OBJECT_ID, new LocalPoint(7, 7)));
        assertEquals(1, solver.getCompletedTiles());

        // Directly invoke reset() via reflection as a sanity test of method behavior
        invokeReset();

        // Assert: state cleared
        assertEquals(0, solver.getCompletedTiles(), "completedTiles should be cleared");
        assertEquals(0, getLastDisplayTick(), "lastDisplayTick should be cleared");
        // points queue is private; we cannot assert directly, but no exceptions indicates it's safe
    }
}