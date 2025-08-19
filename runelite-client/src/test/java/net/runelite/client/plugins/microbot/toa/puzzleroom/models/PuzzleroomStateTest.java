package net.runelite.client.plugins.microbot.toa.puzzleroom.models;

import net.runelite.client.plugins.microbot.toa.puzzleroom.enums.Puzzle;
import net.runelite.client.plugins.microbot.toa.puzzleroom.enums.Room;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for PuzzleroomState (data container).
 *
 * Testing framework: JUnit 4.
 *
 * Focus:
 * - Default values and invariants
 * - Getter/setter round-trip behavior for all fields
 * - Null handling where applicable
 * - Array mutability semantics (no defensive copies)
 */
public class PuzzleroomStateTest
{
	@Test
	public void defaultState_shouldBeInitializedProperly()
	{
		PuzzleroomState state = new PuzzleroomState();

		// Boolean default
		assertFalse("useShortcut should default to false", state.isUseShortcut());

		// Enum defaults
		assertNull("currentPuzzle should default to null", state.getCurrentPuzzle());
		assertNull("currentRoom should default to null", state.getCurrentRoom());
		assertNull("nextRoom should default to null", state.getNextRoom());

		// puzzleLayout default
		assertNotNull("puzzleLayout should be initialized", state.getPuzzleLayout());
		assertEquals("puzzleLayout should have default length 5", 5, state.getPuzzleLayout().length);

		for (int i = 0; i < state.getPuzzleLayout().length; i++)
		{
			assertNull("Each default puzzleLayout entry should be null", state.getPuzzleLayout()[i]);
		}
	}

	@Test
	public void settersRoundTrip_shouldPersistValues()
	{
		PuzzleroomState state = new PuzzleroomState();

		Puzzle samplePuzzle = getAnyPuzzleEnum();
		Room sampleCurrentRoom = getAnyRoomEnum();
		Room sampleNextRoom = getAnotherRoomEnumDistinctFrom(sampleCurrentRoom);

		state.setCurrentPuzzle(samplePuzzle);
		state.setCurrentRoom(sampleCurrentRoom);
		state.setNextRoom(sampleNextRoom);
		state.setUseShortcut(true);

		assertEquals("currentPuzzle should round-trip", samplePuzzle, state.getCurrentPuzzle());
		assertEquals("currentRoom should round-trip", sampleCurrentRoom, state.getCurrentRoom());
		assertEquals("nextRoom should round-trip", sampleNextRoom, state.getNextRoom());
		assertTrue("useShortcut should be set to true", state.isUseShortcut());

		// puzzleLayout round trip + reference equality (no defensive copy expected)
		Puzzle[] layout = new Puzzle[]{
			samplePuzzle,
			null,
			samplePuzzle,
			getDifferentPuzzleEnumFrom(samplePuzzle),
			null
		};
		state.setPuzzleLayout(layout);
		assertSame("Setter should assign exact reference (no defensive copy expected)", layout, state.getPuzzleLayout());
		assertArrayEquals("Array contents should match", layout, state.getPuzzleLayout());
	}

	@Test
	public void puzzleLayout_nullAssignment_shouldBeAccepted()
	{
		PuzzleroomState state = new PuzzleroomState();
		assertNotNull("Precondition: default puzzleLayout is non-null", state.getPuzzleLayout());

		state.setPuzzleLayout((Puzzle[]) null);
		assertNull("puzzleLayout should allow null assignment", state.getPuzzleLayout());
	}

	@Test
	public void puzzleLayout_externalMutation_shouldReflectInState()
	{
		PuzzleroomState state = new PuzzleroomState();

		Puzzle p1 = getAnyPuzzleEnum();
		Puzzle p2 = getDifferentPuzzleEnumFrom(p1);

		Puzzle[] layout = new Puzzle[]{p1, p1, p2, null, p2};
		state.setPuzzleLayout(layout);

		// Mutate external array after setting
		layout[1] = p2;

		// Expect change to reflect in internal state (no defensive copy)
		assertNotNull("puzzleLayout should not be null after set", state.getPuzzleLayout());
		assertEquals("External mutation should reflect internally (no defensive copy)", p2, state.getPuzzleLayout()[1]);
	}

	@Test
	public void togglingUseShortcut_shouldToggle()
	{
		PuzzleroomState state = new PuzzleroomState();
		assertFalse("Default should be false", state.isUseShortcut());

		state.setUseShortcut(true);
		assertTrue("Should be true after setting to true", state.isUseShortcut());

		state.setUseShortcut(false);
		assertFalse("Should be false after setting to false", state.isUseShortcut());
	}

	@Test
	public void emptyPuzzleLayout_shouldBeAccepted()
	{
		PuzzleroomState state = new PuzzleroomState();
		Puzzle[] empty = new Puzzle[0];

		state.setPuzzleLayout(empty);

		assertSame("Expected same array reference assigned", empty, state.getPuzzleLayout());
		assertEquals("Empty array length should be 0", 0, state.getPuzzleLayout().length);
	}

	@Test
	public void setters_allowNullEnums()
	{
		PuzzleroomState state = new PuzzleroomState();

		// Set to non-null first
		state.setCurrentPuzzle(getAnyPuzzleEnum());
		state.setCurrentRoom(getAnyRoomEnum());
		state.setNextRoom(getAnyRoomEnum());

		// Now set to null
		state.setCurrentPuzzle(null);
		state.setCurrentRoom(null);
		state.setNextRoom(null);

		assertNull("currentPuzzle should be settable to null", state.getCurrentPuzzle());
		assertNull("currentRoom should be settable to null", state.getCurrentRoom());
		assertNull("nextRoom should be settable to null", state.getNextRoom());
	}

	// Helper methods to obtain enum constants without relying on specific names.
	private static Puzzle getAnyPuzzleEnum()
	{
		Puzzle[] values = Puzzle.values();
		assertTrue("Puzzle enum should have at least one value", values.length > 0);
		return values[0];
	}

	private static Puzzle getDifferentPuzzleEnumFrom(Puzzle base)
	{
		for (Puzzle p : Puzzle.values())
		{
			if (p != base)
			{
				return p;
			}
		}
		// If only one constant exists, return the same (documents limitation)
		return base;
	}

	private static Room getAnyRoomEnum()
	{
		Room[] values = Room.values();
		assertTrue("Room enum should have at least one value", values.length > 0);
		return values[0];
	}

	private static Room getAnotherRoomEnumDistinctFrom(Room base)
	{
		for (Room r : Room.values())
		{
			if (r != base)
			{
				return r;
			}
		}
		// If only one constant exists, return the same (documents limitation)
		return base;
	}
}