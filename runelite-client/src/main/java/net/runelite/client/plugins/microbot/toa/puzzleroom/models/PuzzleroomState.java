package net.runelite.client.plugins.microbot.toa.puzzleroom.models;

import lombok.Getter;
import lombok.Setter;
import net.runelite.client.plugins.microbot.toa.puzzleroom.enums.Puzzle;
import net.runelite.client.plugins.microbot.toa.puzzleroom.enums.Room;

public class PuzzleroomState {
    @Getter
    @Setter
    Puzzle currentPuzzle;

    @Getter
    @Setter
    Room currentRoom;

    @Getter
    @Setter
    Room nextRoom;

    @Getter
    @Setter
    Puzzle[] puzzleLayout = new Puzzle[5];

    @Getter
    @Setter
    boolean useShortcut = false;

}
