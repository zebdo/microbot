package net.runelite.client.plugins.microbot.toa.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ToaState enum.
 * These tests validate the enum's public interface and standard enum behaviors.
 *
 * Scenarios covered:
 * - values() returns only the expected constants in the expected order
 * - valueOf with valid and invalid names (including null)
 * - ordinal values for constants
 * - toString contract (no custom override present)
 * - Identity/uniqueness guarantees of enum constants
 */
class ToaStateTest {

    @Test
    @DisplayName("values() should contain exactly one constant: PuzzleRoom")
    void values_shouldContainOnlyPuzzleRoom() {
        ToaState[] values = ToaState.values();
        assertNotNull(values, "values() should not return null");
        assertEquals(1, values.length, "There should be exactly one ToaState constant");
        assertSame(ToaState.PuzzleRoom, values[0], "First and only value should be PuzzleRoom");
    }

    @Test
    @DisplayName("valueOf should return PuzzleRoom for exact name")
    void valueOf_validName_returnsConstant() {
        ToaState state = ToaState.valueOf("PuzzleRoom");
        assertSame(ToaState.PuzzleRoom, state, "valueOf(\"PuzzleRoom\") should return ToaState.PuzzleRoom");
    }

    @Test
    @DisplayName("valueOf should throw for invalid name")
    void valueOf_invalidName_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ToaState.valueOf("NotARealState"),
                "valueOf should throw IllegalArgumentException for unknown names");
        assertTrue(ex.getMessage() == null || ex.getMessage().contains("NotARealState"),
                "Exception message should reference the invalid name when available");
    }

    @Test
    @DisplayName("valueOf should throw NullPointerException for null name")
    void valueOf_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> ToaState.valueOf(null),
                "valueOf should throw NullPointerException for null name");
    }

    @Test
    @DisplayName("Enum ordinal should be zero for the first and only constant")
    void ordinal_shouldBeZero() {
        assertEquals(0, ToaState.PuzzleRoom.ordinal(),
                "The ordinal of the first and only enum constant should be 0");
    }

    @Test
    @DisplayName("toString should match the enum name by default")
    void toString_shouldMatchName() {
        assertEquals("PuzzleRoom", ToaState.PuzzleRoom.toString(),
                "Enum toString should default to its name when not overridden");
    }

    @Test
    @DisplayName("Enum constants are singletons and identity-equal across accesses")
    void constants_shouldBeIdentityEqual() {
        ToaState fromField = ToaState.PuzzleRoom;
        ToaState fromValues = ToaState.values()[0];
        ToaState fromValueOf = ToaState.valueOf("PuzzleRoom");

        assertSame(fromField, fromValues, "Access via values()[0] should return the same instance");
        assertSame(fromField, fromValueOf, "Access via valueOf should return the same instance");
    }

    @Test
    @DisplayName("values() returns a new array copy (mutations to returned array don't affect enum)")
    void values_returnsDefensiveCopy() {
        ToaState[] original = ToaState.values();
        assertEquals(1, original.length, "Sanity check: single value expected");
        original[0] = null; // mutate the returned array

        ToaState[] afterMutation = ToaState.values();
        assertEquals(1, afterMutation.length, "Returned arrays should be independent copies");
        assertSame(ToaState.PuzzleRoom, afterMutation[0], "Enum constants should remain intact");
    }
}