package net.runelite.client.plugins.microbot.toa.puzzleroom;

import net.runelite.api.ObjectID;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.toa.puzzleroom.enums.Puzzle;
import net.runelite.client.plugins.microbot.toa.puzzleroom.enums.Room;
import net.runelite.client.plugins.microbot.toa.puzzleroom.models.PuzzleroomState;
import net.runelite.client.plugins.microbot.util.coords.Rs2WorldPoint;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.Global;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testing library and framework:
 * - JUnit 5 (Jupiter) for unit testing
 * - Mockito (including static mocking via MockedStatic) to isolate external/static dependencies
 *
 * This suite focuses on core logic inside PuzzleScript,
 * especially public methods and branch edges:
 *  - readTargetNumberFromChat
 *  - getCurrentPuzzle and room detection helpers
 *  - setNextPuzzleRoom
 *  - getCurrentPuzzleRoom (including EAST/null branches)
 *  - useShortcutToNextRoom and handleTravelToNextPuzzle (side-effect paths, guarded by mocks)
 *  - calculatePuzzleRooms (mapping by tile object IDs), with static mocks
 */
public class PuzzleScriptTest {

    private PuzzleScript script;

    private MockedStatic<Rs2Player> rs2PlayerMock;
    private MockedStatic<Rs2WorldPoint> rs2WorldPointMock;
    private MockedStatic<Rs2GameObject> rs2GameObjectMock;
    private MockedStatic<Rs2Walker> rs2WalkerMock;
    private MockedStatic<Global> globalMock;

    @BeforeEach
    void setUp() {
        script = new PuzzleScript();
        // Fresh state for each test
        script.puzzleroomState = new PuzzleroomState();

        rs2PlayerMock = Mockito.mockStatic(Rs2Player.class);
        rs2WorldPointMock = Mockito.mockStatic(Rs2WorldPoint.class);
        rs2GameObjectMock = Mockito.mockStatic(Rs2GameObject.class);
        rs2WalkerMock = Mockito.mockStatic(Rs2Walker.class);
        globalMock = Mockito.mockStatic(Global.class);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
        rs2WalkerMock.close();
        rs2GameObjectMock.close();
        rs2WorldPointMock.close();
        rs2PlayerMock.close();
        // Reset static field in PuzzleScript that tests rely on
        // (access via new instance to write for clarity)
        PuzzleScript.targetNumber = -1;
    }

    // -----------------------
    // readTargetNumberFromChat
    // -----------------------

    @Test
    void readTargetNumberFromChat_shouldParseValidMessage() {
        String msg = "The number 42 has been hastily chipped into the stone.";
        script.readTargetNumberFromChat(msg);
        assertEquals(42, PuzzleScript.targetNumber, "Target number should be parsed from a valid message");
    }

    @Test
    void readTargetNumberFromChat_shouldParseWhenTagsPresent() {
        String msg = "<col=ff0000>The number 69 has been hastily chipped into the stone.</col>";
        script.readTargetNumberFromChat(msg);
        assertEquals(69, PuzzleScript.targetNumber, "Target number should be parsed even when chat contains tags");
    }

    @Test
    void readTargetNumberFromChat_shouldIgnoreNonMatchingMessage() {
        PuzzleScript.targetNumber = -1;
        String msg = "Nothing useful here.";
        script.readTargetNumberFromChat(msg);
        assertEquals(-1, PuzzleScript.targetNumber, "Target number should remain unchanged if message does not match");
    }

    // -----------------------
    // Room boundary helpers
    // -----------------------

    @Test
    void isInSouthWestRoom_boundariesInclusiveAndYLeOrEq() {
        // lower bound x=3533, y <= 5278
        WorldPoint p1 = new WorldPoint(3533, 5278, 0);
        rs2PlayerMock.when(Rs2Player::getWorldLocation).thenReturn(p1);
        assertTrue(script.isInSouthWestRoom(), "SW lower boundary should be inside");

        // upper bound x=3548, y <= 5278
        WorldPoint p2 = new WorldPoint(3548, 5200, 0);
        rs2PlayerMock.when(Rs2Player::getWorldLocation).thenReturn(p2);
        assertTrue(script.isInSouthWestRoom(), "SW upper boundary should be inside");

        // outside by x
        WorldPoint p3 = new WorldPoint(3532, 5278, 0);
        rs2PlayerMock.when(Rs2Player::getWorldLocation).thenReturn(p3);
        assertFalse(script.isInSouthWestRoom(), "x < 3533 should be outside SW");

        // outside by y
        WorldPoint p4 = new WorldPoint(3540, 5279, 0);
        rs2PlayerMock.when(Rs2Player::getWorldLocation).thenReturn(p4);
        assertFalse(script.isInSouthWestRoom(), "y > 5278 should be outside SW");
    }

    @Test
    void isInNorthWestRoom_boundariesInclusiveAndYGreater() {
        WorldPoint inside = new WorldPoint(3540, 5279, 0);
        rs2PlayerMock.when(Rs2Player::getWorldLocation).thenReturn(inside);
        assertTrue(script.isInNorthWestRoom(), "NorthWest requires x in [3533,3548] and y > 5278");

        WorldPoint onYBoundary = new WorldPoint(3535, 5278, 0);
        rs2PlayerMock.when(Rs2Player::getWorldLocation).thenReturn(onYBoundary);
        assertFalse(script.isInNorthWestRoom(), "y == 5278 is not NorthWest");

        WorldPoint xOut = new WorldPoint(3532, 5280, 0);
        rs2PlayerMock.when(Rs2Player::getWorldLocation).thenReturn(xOut);
        assertFalse(script.isInNorthWestRoom(), "x < 3533 should be outside NW");
    }

    @Test
    void isInSouthMiddleRoom_boundariesInclusiveAndYLeOrEq() {
        WorldPoint inside = new WorldPoint(3550, 5278, 0);
        rs2PlayerMock.when(Rs2Player::getWorldLocation).thenReturn(inside);
        assertTrue(script.isInSouthMiddleRoom(), "SouthMiddle requires x in [3549,3563] and y <= 5278");

        WorldPoint yOut = new WorldPoint(3555, 5279, 0);
        rs2PlayerMock.when(Rs2Player::getWorldLocation).thenReturn(yOut);
        assertFalse(script.isInSouthMiddleRoom(), "y > 5278 should be outside SouthMiddle");
    }

    @Test
    void isInNorthMiddleRoom_boundariesInclusiveAndYGreater() {
        WorldPoint inside = new WorldPoint(3563, 5285, 0);
        rs2PlayerMock.when(Rs2Player::getWorldLocation).thenReturn(inside);
        assertTrue(script.isInNorthMiddleRoom(), "NorthMiddle requires x in [3549,3563] and y > 5278");

        WorldPoint yBoundary = new WorldPoint(3550, 5278, 0);
        rs2PlayerMock.when(Rs2Player::getWorldLocation).thenReturn(yBoundary);
        assertFalse(script.isInNorthMiddleRoom(), "y == 5278 is not NorthMiddle");
    }

    // -----------------------
    // getCurrentPuzzle
    // -----------------------

    @Test
    void getCurrentPuzzle_returnsWaitingRoomWhenXLessThan3533() {
        rs2PlayerMock.when(Rs2Player::getWorldLocation).thenReturn(new WorldPoint(3532, 5300, 0));
        Puzzle[] layout = new Puzzle[]{Puzzle.ADDITION, Puzzle.LIGHT, Puzzle.SEQUENCE, Puzzle.OBELISK, Puzzle.MEMORY};
        assertEquals(Puzzle.WAITING_ROOM, script.getCurrentPuzzle(layout));
    }

    @Test
    void getCurrentPuzzle_mapsToCorrectIndexByRoom() {
        Puzzle[] layout = new Puzzle[]{Puzzle.ADDITION, Puzzle.LIGHT, Puzzle.SEQUENCE, Puzzle.OBELISK, Puzzle.MEMORY};

        rs2PlayerMock.when(Rs2Player::getWorldLocation).thenReturn(new WorldPoint(3533, 5278, 0));
        assertEquals(Puzzle.ADDITION, script.getCurrentPuzzle(layout), "SouthWest -> index 0");

        rs2PlayerMock.when(Rs2Player::getWorldLocation).thenReturn(new WorldPoint(3540, 5280, 0));
        assertEquals(Puzzle.LIGHT, script.getCurrentPuzzle(layout), "NorthWest -> index 1");

        rs2PlayerMock.when(Rs2Player::getWorldLocation).thenReturn(new WorldPoint(3550, 5278, 0));
        assertEquals(Puzzle.SEQUENCE, script.getCurrentPuzzle(layout), "SouthMiddle -> index 2");

        rs2PlayerMock.when(Rs2Player::getWorldLocation).thenReturn(new WorldPoint(3560, 5285, 0));
        assertEquals(Puzzle.OBELISK, script.getCurrentPuzzle(layout), "NorthMiddle -> index 3");

        // Else case -> index 4
        rs2PlayerMock.when(Rs2Player::getWorldLocation).thenReturn(new WorldPoint(3568, 5274, 0));
        assertEquals(Puzzle.MEMORY, script.getCurrentPuzzle(layout), "Else/East -> index 4");
    }

    // -----------------------
    // getCurrentPuzzleRoom
    // -----------------------

    @Test
    void getCurrentPuzzleRoom_returnsEachExplicitRoom() {
        // SOUTHWEST
        rs2PlayerMock.when(Rs2Player::getWorldLocation).thenReturn(new WorldPoint(3533, 5278, 0));
        assertEquals(Room.SOUTHWEST, script.getCurrentPuzzleRoom());

        // NORTHMIDDLE
        rs2PlayerMock.when(Rs2Player::getWorldLocation).thenReturn(new WorldPoint(3555, 5285, 0));
        assertEquals(Room.NORTHMIDDLE, script.getCurrentPuzzleRoom());

        // SOUTHMIDDLE
        rs2PlayerMock.when(Rs2Player::getWorldLocation).thenReturn(new WorldPoint(3555, 5278, 0));
        assertEquals(Room.SOUTHMIDDLE, script.getCurrentPuzzleRoom());

        // NORTHWEST
        rs2PlayerMock.when(Rs2Player::getWorldLocation).thenReturn(new WorldPoint(3540, 5285, 0));
        assertEquals(Room.NORTHWEST, script.getCurrentPuzzleRoom());
    }

    @Test
    void getCurrentPuzzleRoom_returnsEastWhenCurrentPuzzleNotWaiting() {
        // Place at any coordinate not matching the four rooms, e.g., x > 3563 ensures not in those ranges
        rs2PlayerMock.when(Rs2Player::getWorldLocation).thenReturn(new WorldPoint(3568, 5274, 0));
        // puzzleLayout with non-WAITING puzzle for fallback check
        script.puzzleroomState.setPuzzleLayout(new Puzzle[]{Puzzle.ADDITION, Puzzle.LIGHT, Puzzle.SEQUENCE, Puzzle.OBELISK, Puzzle.MEMORY});
        assertEquals(Room.EAST, script.getCurrentPuzzleRoom(), "When not in known rooms and current puzzle != WAITING_ROOM, return EAST");
    }

    @Test
    void getCurrentPuzzleRoom_returnsNullWhenWaitingRoom() {
        // Not in the four rooms, and getCurrentPuzzle returns WAITING_ROOM
        rs2PlayerMock.when(Rs2Player::getWorldLocation).thenReturn(new WorldPoint(3568, 5274, 0));
        script.puzzleroomState.setPuzzleLayout(new Puzzle[]{Puzzle.WAITING_ROOM, Puzzle.WAITING_ROOM, Puzzle.WAITING_ROOM, Puzzle.WAITING_ROOM, Puzzle.WAITING_ROOM});
        assertNull(script.getCurrentPuzzleRoom(), "When not in known rooms and current puzzle == WAITING_ROOM, return null");
    }

    // -----------------------
    // setNextPuzzleRoom
    // -----------------------

    @Test
    void setNextPuzzleRoom_fromSouthWest_setsNorthMiddleWithShortcut() {
        rs2PlayerMock.when(Rs2Player::getWorldLocation).thenReturn(new WorldPoint(3533, 5278, 0));
        script.setNextPuzzleRoom();
        assertTrue(script.puzzleroomState.isUseShortcut(), "Should enable shortcut when moving from SW");
        assertEquals(Room.NORTHMIDDLE, script.puzzleroomState.getNextRoom(), "Next room from SW should be NorthMiddle");
    }

    @Test
    void setNextPuzzleRoom_fromNorthWest_setsSouthMiddleWithShortcut() {
        rs2PlayerMock.when(Rs2Player::getWorldLocation).thenReturn(new WorldPoint(3540, 5285, 0));
        script.setNextPuzzleRoom();
        assertTrue(script.puzzleroomState.isUseShortcut(), "Should enable shortcut when moving from NW");
        assertEquals(Room.SOUTHMIDDLE, script.puzzleroomState.getNextRoom(), "Next room from NW should be SouthMiddle");
    }

    @Test
    void setNextPuzzleRoom_fromMiddleSetsEastWithShortcut() {
        // SouthMiddle case
        rs2PlayerMock.when(Rs2Player::getWorldLocation).thenReturn(new WorldPoint(3550, 5270, 0));
        script.setNextPuzzleRoom();
        assertTrue(script.puzzleroomState.isUseShortcut());
        assertEquals(Room.EAST, script.puzzleroomState.getNextRoom());

        // Reset state and verify NorthMiddle case
        script.puzzleroomState = new PuzzleroomState();
        rs2PlayerMock.when(Rs2Player::getWorldLocation).thenReturn(new WorldPoint(3550, 5285, 0));
        script.setNextPuzzleRoom();
        assertTrue(script.puzzleroomState.isUseShortcut());
        assertEquals(Room.EAST, script.puzzleroomState.getNextRoom());
    }

    // -----------------------
    // useShortcutToNextRoom (side effects)
    // -----------------------

    @Test
    void useShortcutToNextRoom_fromWestRooms_uses45343AndDisablesShortcutWhenArrived() {
        // Setup current room WEST (SOUTHWEST)
        rs2PlayerMock.when(Rs2Player::getWorldLocation).thenReturn(new WorldPoint(3535, 5278, 0));
        script.puzzleroomState.setUseShortcut(true);
        script.puzzleroomState.setCurrentRoom(Room.SOUTHWEST);

        // Make sleepUntilTrue return true, simulating room change without invoking predicate
        globalMock.when(() -> Global.sleepUntilTrue(Mockito.any())).thenReturn(true);

        // No need to rely on actual getCurrentPuzzleRoom evaluation since we short-circuit sleepUntilTrue
        rs2GameObjectMock.when(() -> Rs2GameObject.interact(45343)).thenReturn(true);
        rs2PlayerMock.when(Rs2Player::waitForAnimation).then(invocation -> null);

        script.useShortcutToNextRoom();

        rs2GameObjectMock.verify(() -> Rs2GameObject.interact(45343));
        assertFalse(script.puzzleroomState.isUseShortcut(), "Shortcut should be disabled after successful travel");
    }

    @Test
    void useShortcutToNextRoom_fromMiddleRooms_uses45396AndDisablesShortcutWhenArrived() {
        // Setup current room MIDDLE (SOUTHMIDDLE)
        rs2PlayerMock.when(Rs2Player::getWorldLocation).thenReturn(new WorldPoint(3555, 5270, 0));
        script.puzzleroomState.setUseShortcut(true);
        script.puzzleroomState.setCurrentRoom(Room.SOUTHMIDDLE);

        globalMock.when(() -> Global.sleepUntilTrue(Mockito.any())).thenReturn(true);
        rs2GameObjectMock.when(() -> Rs2GameObject.interact(45396)).thenReturn(true);
        rs2PlayerMock.when(Rs2Player::waitForAnimation).then(invocation -> null);

        script.useShortcutToNextRoom();

        rs2GameObjectMock.verify(() -> Rs2GameObject.interact(45396));
        assertFalse(script.puzzleroomState.isUseShortcut(), "Shortcut should be disabled after successful travel from middle rooms");
    }

    @Test
    void useShortcutToNextRoom_doesNothingWhenUseShortcutFalse() {
        script.puzzleroomState.setUseShortcut(false);
        script.puzzleroomState.setCurrentRoom(Room.SOUTHWEST);

        script.useShortcutToNextRoom();
        // verify no static interactions
        rs2GameObjectMock.verifyNoInteractions();
    }

    // -----------------------
    // handleTravelToNextPuzzle (side effects)
    // -----------------------

    @Test
    void handleTravelToNextPuzzle_skipsWhenUseShortcutTrue() {
        script.puzzleroomState.setUseShortcut(true);
        script.puzzleroomState.setNextRoom(Room.NORTHMIDDLE);

        script.handleTravelToNextPuzzle();
        rs2WalkerMock.verifyNoInteractions();
    }

    @Test
    void handleTravelToNextPuzzle_walksToExpectedCoordinates() {
        script.puzzleroomState.setUseShortcut(false);

        // NORTHMIDDLE -> (3554, 5286, 0, radius 2)
        script.puzzleroomState.setNextRoom(Room.NORTHMIDDLE);
        script.handleTravelToNextPuzzle();
        rs2WalkerMock.verify(() -> Rs2Walker.walkTo(3554, 5286, 0, 2));

        // SOUTHMIDDLE -> (3554, 5274, 0, radius 2)
        script.puzzleroomState.setNextRoom(Room.SOUTHMIDDLE);
        script.handleTravelToNextPuzzle();
        rs2WalkerMock.verify(() -> Rs2Walker.walkTo(3554, 5274, 0, 2));

        // EAST -> (3567, 5274, 0, radius 2)
        script.puzzleroomState.setNextRoom(Room.EAST);
        script.handleTravelToNextPuzzle();
        rs2WalkerMock.verify(() -> Rs2Walker.walkTo(3567, 5274, 0, 2));
    }

    // -----------------------
    // calculatePuzzleRooms
    // -----------------------

    @Test
    void calculatePuzzleRooms_mapsTileObjectIdsToPuzzles() {
        // Prepare convertInstancedWorldPoint as identity (return same world point)
        rs2WorldPointMock.when(() -> Rs2WorldPoint.convertInstancedWorldPoint(Mockito.any(WorldPoint.class)))
                .thenAnswer(inv -> (WorldPoint) inv.getArgument(0));

        // Capture calls to findGroundObjectByLocation and return tile objects with specific IDs by position
        rs2GameObjectMock.when(() -> Rs2GameObject.findGroundObjectByLocation(Mockito.any(WorldPoint.class)))
                .thenAnswer(inv -> {
                    WorldPoint w = inv.getArgument(0);
                    // match expected four rooms in order in method:
                    // southWest (3540, 5274), northWest (3540, 5286), southMiddle (3557, 5274), northMiddle (3557, 5286)
                    int id;
                    if (w.equals(new WorldPoint(3540, 5274, 0))) {
                        id = 45380; // OBELISK
                    } else if (w.equals(new WorldPoint(3540, 5286, 0))) {
                        id = ObjectID.PRESSURE_PLATE_45352; // ADDITION
                    } else if (w.equals(new WorldPoint(3557, 5274, 0))) {
                        id = ObjectID.PRESSURE_PLATE_45344; // LIGHT
                    } else if (w.equals(new WorldPoint(3557, 5286, 0))) {
                        id = ObjectID.PRESSURE_PLATE; // SEQUENCE (generic pressure plate)
                    } else {
                        return null;
                    }
                    TileObject t = Mockito.mock(TileObject.class);
                    Mockito.when(t.getId()).thenReturn(id);
                    return t;
                });

        Puzzle[] layout = script.calculatePuzzleRooms();

        assertNotNull(layout, "Layout should not be null");
        assertEquals(5, layout.length, "Layout must contain 5 entries");

        assertEquals(Puzzle.OBELISK, layout[0], "SW room id 45380 -> OBELISK");
        assertEquals(Puzzle.ADDITION, layout[1], "NW PRESSURE_PLATE_45352 -> ADDITION");
        assertEquals(Puzzle.LIGHT, layout[2], "SouthMiddle PRESSURE_PLATE_45344 -> LIGHT");
        assertEquals(Puzzle.SEQUENCE, layout[3], "NorthMiddle PRESSURE_PLATE -> SEQUENCE");
        assertEquals(Puzzle.MEMORY, layout[4], "Fifth slot should be MEMORY as per implementation");
    }

    @Test
    void calculatePuzzleRooms_handlesNullTileObjects() {
        // If all rooms return null tile objects, all four first entries remain null, last is MEMORY
        rs2WorldPointMock.when(() -> Rs2WorldPoint.convertInstancedWorldPoint(Mockito.any(WorldPoint.class)))
                .thenAnswer(inv -> (WorldPoint) inv.getArgument(0));

        rs2GameObjectMock.when(() -> Rs2GameObject.findGroundObjectByLocation(Mockito.any(WorldPoint.class)))
                .thenReturn(null);

        Puzzle[] layout = script.calculatePuzzleRooms();
        assertEquals(5, layout.length);
        assertNull(layout[0], "No tile object -> null");
        assertNull(layout[1], "No tile object -> null");
        assertNull(layout[2], "No tile object -> null");
        assertNull(layout[3], "No tile object -> null");
        assertEquals(Puzzle.MEMORY, layout[4], "Fifth is always MEMORY");
    }
}