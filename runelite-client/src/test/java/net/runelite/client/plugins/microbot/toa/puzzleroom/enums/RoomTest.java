package net.runelite.client.plugins.microbot.toa.puzzleroom.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for the Room enum.
 *
 * Framework: JUnit 5 (Jupiter). If this project uses JUnit 4, replace imports with:
 *   import org.junit.Test;
 *   import static org.junit.Assert.*;
 * and remove @DisplayName/@Nested as appropriate.
 */
public class RoomTest
{
    @Test
    @DisplayName("values() should contain all expected constants in declaration order")
    void valuesContainAllConstantsInOrder()
    {
        Room[] values = Room.values();
        assertNotNull(values, "values() should not be null");
        assertEquals(5, values.length, "There should be exactly 5 Room constants");

        assertEquals(Room.SOUTHWEST, values[0], "Ordinal 0 should be SOUTHWEST");
        assertEquals(Room.NORTHWEST, values[1], "Ordinal 1 should be NORTHWEST");
        assertEquals(Room.SOUTHMIDDLE, values[2], "Ordinal 2 should be SOUTHMIDDLE");
        assertEquals(Room.NORTHMIDDLE, values[3], "Ordinal 3 should be NORTHMIDDLE");
        assertEquals(Room.EAST, values[4], "Ordinal 4 should be EAST");
    }

    @Test
    @DisplayName("Each constant should have the expected ordinal")
    void ordinalsAreStable()
    {
        assertEquals(0, Room.SOUTHWEST.ordinal(), "SOUTHWEST ordinal");
        assertEquals(1, Room.NORTHWEST.ordinal(), "NORTHWEST ordinal");
        assertEquals(2, Room.SOUTHMIDDLE.ordinal(), "SOUTHMIDDLE ordinal");
        assertEquals(3, Room.NORTHMIDDLE.ordinal(), "NORTHMIDDLE ordinal");
        assertEquals(4, Room.EAST.ordinal(), "EAST ordinal");
    }

    @Test
    @DisplayName("name() and toString() should match for plain enums (no custom toString)")
    void toStringMatchesName()
    {
        for (Room r : Room.values())
        {
            assertEquals(r.name(), r.toString(), "Default toString should equal name()");
            assertNotNull(r.name(), "name() should not be null");
            assertFalse(r.name().isEmpty(), "name() should not be empty");
        }
    }

    @Nested
    @DisplayName("valueOf(String) behavior")
    class ValueOfBehavior
    {
        @Test
        @DisplayName("valueOf should resolve each constant by exact name")
        void resolvesByExactName()
        {
            assertEquals(Room.SOUTHWEST, Room.valueOf("SOUTHWEST"));
            assertEquals(Room.NORTHWEST, Room.valueOf("NORTHWEST"));
            assertEquals(Room.SOUTHMIDDLE, Room.valueOf("SOUTHMIDDLE"));
            assertEquals(Room.NORTHMIDDLE, Room.valueOf("NORTHMIDDLE"));
            assertEquals(Room.EAST, Room.valueOf("EAST"));
        }

        @Test
        @DisplayName("valueOf with invalid name should throw IllegalArgumentException")
        void invalidNameThrows()
        {
            assertThrows(IllegalArgumentException.class, () -> Room.valueOf("SOUTH_WEST"));
            assertThrows(IllegalArgumentException.class, () -> Room.valueOf("southwest"));
            assertThrows(IllegalArgumentException.class, () -> Room.valueOf("NORTH"));
            assertThrows(IllegalArgumentException.class, () -> Room.valueOf("WEST"));
            assertThrows(IllegalArgumentException.class, () -> Room.valueOf("")); // empty
            assertThrows(IllegalArgumentException.class, () -> Room.valueOf("   ")); // whitespace
            assertThrows(IllegalArgumentException.class, () -> Room.valueOf("UNKNOWN"));
        }

        @Test
        @DisplayName("valueOf with null should throw NullPointerException")
        void nullThrowsNpe()
        {
            assertThrows(NullPointerException.class, () -> Room.valueOf(null));
        }
    }

    @Test
    @DisplayName("Enum constants should be unique singletons")
    void constantsAreSingletons()
    {
        // Same constant fetched multiple times should be same reference
        assertSame(Room.SOUTHWEST, Room.valueOf("SOUTHWEST"));
        assertSame(Room.NORTHWEST, Room.valueOf("NORTHWEST"));
        assertSame(Room.SOUTHMIDDLE, Room.valueOf("SOUTHMIDDLE"));
        assertSame(Room.NORTHMIDDLE, Room.valueOf("NORTHMIDDLE"));
        assertSame(Room.EAST, Room.valueOf("EAST"));
    }

    @Test
    @DisplayName("values() returns a copy (mutations to returned array should not affect enum)")
    void valuesReturnsDefensiveCopy()
    {
        Room[] a = Room.values();
        Room[] b = Room.values();
        assertNotSame(a, b, "Each call to values() should return a new array instance");
        assertArrayEquals(a, b, "Returned arrays should have identical content");

        // Mutate 'a' locally and ensure it doesn't affect a fresh call
        a[0] = Room.EAST;
        assertNotEquals(a[0], Room.values()[0], "Mutating returned array should not affect enum constants");
    }
}