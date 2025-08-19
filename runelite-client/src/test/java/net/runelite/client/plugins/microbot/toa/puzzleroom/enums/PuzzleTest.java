package net.runelite.client.plugins.microbot.toa.puzzleroom.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Puzzle enum tests (JUnit 5)")
public class PuzzleTest {

    @Test
    @DisplayName("values() contains all constants in declared order")
    void values_containsAllConstantsInOrder() {
        Puzzle[] values = Puzzle.values();
        assertEquals(6, values.length, "Expected exactly 6 enum constants");
        assertEquals(Puzzle.WAITING_ROOM, values[0]);
        assertEquals(Puzzle.LIGHT, values[1]);
        assertEquals(Puzzle.ADDITION, values[2]);
        assertEquals(Puzzle.SEQUENCE, values[3]);
        assertEquals(Puzzle.OBELISK, values[4]);
        assertEquals(Puzzle.MEMORY, values[5]);
    }

    @ParameterizedTest(name = "valueOf(\"{0}\") returns a valid Puzzle")
    @ValueSource(strings = { "WAITING_ROOM", "LIGHT", "ADDITION", "SEQUENCE", "OBELISK", "MEMORY" })
    @DisplayName("valueOf for all defined constants succeeds")
    void valueOf_validNames_succeeds(String name) {
        Puzzle p = Puzzle.valueOf(name);
        assertNotNull(p);
        assertEquals(name, p.name());
    }

    @Test
    @DisplayName("valueOf throws on invalid name")
    void valueOf_invalidName_throws() {
        assertThrows(IllegalArgumentException.class, () -> Puzzle.valueOf("NOT_A_PUZZLE"));
        assertThrows(NullPointerException.class, () -> Puzzle.valueOf(null));
        // Case sensitivity: valueOf should throw if case does not match
        assertThrows(IllegalArgumentException.class, () -> Puzzle.valueOf("light"));
    }

    @Test
    @DisplayName("Enum ordinals match declared order (documented for regression)")
    void ordinals_matchDeclaredOrder() {
        assertEquals(0, Puzzle.WAITING_ROOM.ordinal());
        assertEquals(1, Puzzle.LIGHT.ordinal());
        assertEquals(2, Puzzle.ADDITION.ordinal());
        assertEquals(3, Puzzle.SEQUENCE.ordinal());
        assertEquals(4, Puzzle.OBELISK.ordinal());
        assertEquals(5, Puzzle.MEMORY.ordinal());
    }

    @Test
    @DisplayName("Enum constants have stable names")
    void names_areStable() {
        for (Puzzle p : Puzzle.values()) {
            assertNotNull(p.name());
            assertFalse(p.name().isEmpty());
            // Verify name is uppercase with underscores (project convention)
            assertTrue(p.name().matches("[A-Z_]+"), "Enum name should be UPPER_SNAKE_CASE");
        }
    }
}