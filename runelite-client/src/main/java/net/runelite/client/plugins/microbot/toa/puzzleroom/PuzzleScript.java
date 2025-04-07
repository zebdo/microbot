package net.runelite.client.plugins.microbot.toa.puzzleroom;

import net.runelite.api.ObjectID;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.toa.ToaConfig;
import net.runelite.client.plugins.microbot.toa.puzzleroom.enums.Puzzle;
import net.runelite.client.plugins.microbot.toa.puzzleroom.enums.Room;
import net.runelite.client.plugins.microbot.toa.puzzleroom.models.PuzzleroomState;
import net.runelite.client.plugins.microbot.util.coords.Rs2WorldPoint;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.util.Text;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.runelite.client.plugins.microbot.util.Global.sleepUntilTrue;


public class PuzzleScript extends Script {

    static int targetNumber = -1;

    private static final Pattern TARGET_NUMBER_PATTERN = Pattern.compile("The number (\\d+) has been hastily chipped into the stone.");

    public LightPuzzleSolver lightPuzzleSolver = new LightPuzzleSolver();
    public SequencePuzzleSolver sequencePuzzleSolver = new SequencePuzzleSolver();

    public AdditionPuzzleSolver additionPuzzleSolver = new AdditionPuzzleSolver();

    public PuzzleroomState puzzleroomState = new PuzzleroomState();

    public boolean run(ToaConfig config) {
        Microbot.enableAutoRunOn = false;
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;
                long startTime = System.currentTimeMillis();

                if (Rs2Player.getWorldLocation().getRegionID() != 14162) {
                    return;
                }

                puzzleroomState.setCurrentPuzzle(getCurrentPuzzle(puzzleroomState.getPuzzleLayout()));
                puzzleroomState.setCurrentRoom(getCurrentPuzzleRoom());

                if (puzzleroomState.getCurrentPuzzle() == Puzzle.WAITING_ROOM && puzzleroomState.getNextRoom() != null) {
                    puzzleroomState.setNextRoom(null);
                }

             /*   if (Arrays.stream(puzzleroomState.getPuzzleLayout()).allMatch(Objects::isNull)) {
                }*/

                puzzleroomState.setPuzzleLayout(calculatePuzzleRooms());


                if (puzzleroomState.getNextRoom() != null) {
                    if (puzzleroomState.getCurrentRoom() != puzzleroomState.getNextRoom()) {
                        Microbot.log("Walk to next room");
                        useShortcutToNextRoom();
                        handleTravelToNextPuzzle();
                    } else {
                        puzzleroomState.setNextRoom(null);
                    }
                    //TODO: have a variable to make sure that you can check when you reached the room you want to be in
                    return;
                }


/*                switch(currentPuzzle) {
                    case WAITING_ROOM:
                        if (Arrays.stream(puzzleLayout).allMatch(Objects::isNull)) {
                            puzzleLayout = calculatePuzzleRooms();
                        }
                        WorldPoint gateInstancedWorldPoint = new WorldPoint(3532, 5274, 0);
                        WorldPoint gateGlobalWorldPoint = Rs2WorldPoint.convertInstancedWorldPoint(gateInstancedWorldPoint);
                        Rs2GameObject.interact(gateGlobalWorldPoint, "quick-pass");
                        Rs2Player.waitForWalking();
                        break;
                    case ADDITION:
                        Rs2GameObject.interact(ObjectID.ANCIENT_TABLET);
                        Rs2Player.waitForWalking();

                        if (additionPuzzleSolver.getFlips().isEmpty()) {
                            additionPuzzleSolver.solve();
                        } else {
                            for (LocalPoint localPoint:additionPuzzleSolver.getFlips()) {
                                WorldPoint w = WorldPoint.fromLocalInstance(Microbot.getClient(), localPoint);
                                Rs2Walker.walkFastCanvas(w);
                                Rs2Player.waitForWalking();
                            }
                        }
                        break;
                    case LIGHT:
                        if (additionPuzzleSolver.getFlips().isEmpty()) {
                            additionPuzzleSolver.solve();
                        } else {
                            for (LocalPoint localPoint:additionPuzzleSolver.getFlips()) {
                                WorldPoint w = WorldPoint.fromLocalInstance(Microbot.getClient(), localPoint);
                                Rs2Walker.walkFastCanvas(w);
                                Rs2Player.waitForWalking();
                            }
                        }
                        break;
                    case SEQUENCE:
                        Rs2GameObject.interact(ObjectID.ANCIENT_BUTTON);
                        sleepUntil(() -> sequencePuzzleSolver.getPoints().stream().allMatch(Objects::nonNull));

                        if (sequencePuzzleSolver.getPoints().isEmpty()) return;

                        int index = 0;
                        for (LocalPoint localPoint: sequencePuzzleSolver.getPoints()) {
                            if (index <= sequencePuzzleSolver.getCompletedTiles()) continue;

                            WorldPoint w = WorldPoint.fromLocalInstance(Microbot.getClient(), localPoint);
                            if (w != null) {
                                Rs2Walker.setTarget(w);
                            }
                            if (ShortestPathPlugin.getPathfinder() != null) {
                                for (WorldPoint path : ShortestPathPlugin.getPathfinder().getPath()) {
                                    Rs2Walker.walkFastCanvas(path);
                                    sleepUntil(() -> Rs2Player.getWorldLocation().equals(path));
                                }
                            }
                            index++;
                        }

                        break;
                }*/


                long endTime = System.currentTimeMillis();
                long totalTime = endTime - startTime;
                System.out.println("Total time for loop " + totalTime);

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        return true;
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }


    /**
     * This is used for the light room, we read the number from the ancient tablet.
     * The number corresponds to a path that we have to take to complete the room
     * Possible steps: https://oldschool.runescape.wiki/w/Tombs_of_Amascut/Strategies#/media/File:Path_of_Scabaras_-_additions_puzzle_key.png
     */
    public void readTargetNumberFromChat(String message) {
        Matcher matcher = TARGET_NUMBER_PATTERN.matcher(Text.removeTags(message));
        if (!matcher.matches()) {
            return;
        }

        targetNumber = Integer.parseInt(matcher.group(1));
    }


    /**
     * @return
     */
    public Puzzle[] calculatePuzzleRooms() {

        Puzzle[] toaLayout = new Puzzle[5];
        var southWestRoomTile = new WorldPoint(3540, 5274, 0);
        var northWestRoomTile = new WorldPoint(3540, 5286, 0);

        var southMiddleRoomTile = new WorldPoint(3557, 5274, 0);
        var northMiddleRoomTile = new WorldPoint(3557, 5286, 0);

        WorldPoint[] rooms = new WorldPoint[]{southWestRoomTile, northWestRoomTile, southMiddleRoomTile, northMiddleRoomTile};

        for (int i = 0; i < rooms.length; i++) {
            WorldPoint globalWorldPoint = Rs2WorldPoint.convertInstancedWorldPoint(rooms[i]);
            TileObject tileObject = Rs2GameObject.findGroundObjectByLocation(globalWorldPoint);

            if (tileObject != null && tileObject.getId() == 45380) {
                toaLayout[i] = Puzzle.OBELISK;
            } else if (tileObject != null && tileObject.getId() == ObjectID.PRESSURE_PLATE_45352) {
                toaLayout[i] = Puzzle.ADDITION;
            } else if (tileObject != null && tileObject.getId() == ObjectID.PRESSURE_PLATE_45344) {
                toaLayout[i] = Puzzle.LIGHT;
            } else if (tileObject != null && tileObject.getId() == ObjectID.PRESSURE_PLATE) {
                toaLayout[i] = Puzzle.SEQUENCE;
            }
        }

        toaLayout[4] = Puzzle.MEMORY;


        return toaLayout;

    }

    public boolean isInSouthWestRoom() {
        return Rs2Player.getWorldLocation().getX() >= 3533 && Rs2Player.getWorldLocation().getX() <= 3548 &&
                Rs2Player.getWorldLocation().getY() <= 5278;
    }

    public boolean isInNorthWestRoom() {
        return Rs2Player.getWorldLocation().getX() >= 3533 && Rs2Player.getWorldLocation().getX() <= 3548 &&
                Rs2Player.getWorldLocation().getY() > 5278;
    }

    public boolean isInSouthMiddleRoom() {
        return Rs2Player.getWorldLocation().getX() >= 3549 && Rs2Player.getWorldLocation().getX() <= 3563 &&
                Rs2Player.getWorldLocation().getY() <= 5278;
    }

    public boolean isInNorthMiddleRoom() {
        return Rs2Player.getWorldLocation().getX() >= 3549 && Rs2Player.getWorldLocation().getX() <= 3563 &&
                Rs2Player.getWorldLocation().getY() > 5278;
    }

    public void useShortcutToNextRoom() {
        if (!puzzleroomState.isUseShortcut()) return;
        if (puzzleroomState.getCurrentRoom() == Room.SOUTHWEST || puzzleroomState.getCurrentRoom() == Room.NORTHWEST) {
            Room _cachedRoom = puzzleroomState.getCurrentRoom();
            Rs2GameObject.interact(45343);
            Rs2Player.waitForAnimation();
            boolean isInNextRoom = sleepUntilTrue(() -> _cachedRoom != getCurrentPuzzleRoom());
            if (isInNextRoom) {
                puzzleroomState.setUseShortcut(false);
            }
        } else if (puzzleroomState.getCurrentRoom() == Room.SOUTHMIDDLE || puzzleroomState.getCurrentRoom() == Room.NORTHMIDDLE) {
            Room _cachedRoom = puzzleroomState.getCurrentRoom();
            Rs2GameObject.interact(45396);
            Rs2Player.waitForAnimation();
            boolean isInNextRoom = sleepUntilTrue(() -> _cachedRoom != getCurrentPuzzleRoom());
            if (isInNextRoom) {
                puzzleroomState.setUseShortcut(false);
            }
        }
    }

    public void handleTravelToNextPuzzle() {
        if (puzzleroomState.isUseShortcut()) return;
        if (puzzleroomState.getNextRoom() == Room.NORTHMIDDLE) {
            Rs2Walker.walkTo(3554, 5286, 0, 2);
        } else if (puzzleroomState.getNextRoom() == Room.SOUTHMIDDLE) {
            Rs2Walker.walkTo(3554, 5274, 0, 2);
        } else if (puzzleroomState.getNextRoom() == Room.EAST) {
            Rs2Walker.walkTo(3567, 5274, 0, 2);
        }
    }

    /**
     * calculates the next room we have to go to
     */
    public Room getCurrentPuzzleRoom() {
        if (isInSouthWestRoom()) {
            return Room.SOUTHWEST;
        } else if (isInNorthMiddleRoom()) {
            return Room.NORTHMIDDLE;
        } else if (isInSouthMiddleRoom()) {
            return Room.SOUTHMIDDLE;
        } else if (isInNorthWestRoom()) {
            return Room.NORTHWEST;
        } else if (getCurrentPuzzle(puzzleroomState.getPuzzleLayout()) != Puzzle.WAITING_ROOM) {
            return Room.EAST;
        } else {
            return null;
        }
    }

    /**
     * calculates the next room we have to go to
     */
    public void setNextPuzzleRoom() {
        if (isInSouthWestRoom()) {
            puzzleroomState.setUseShortcut(true);
            puzzleroomState.setNextRoom(Room.NORTHMIDDLE);
        } else if (isInNorthWestRoom()) {
            puzzleroomState.setUseShortcut(true);
            puzzleroomState.setNextRoom(Room.SOUTHMIDDLE);
        } else if (isInSouthMiddleRoom() || isInNorthMiddleRoom()) {
            puzzleroomState.setUseShortcut(true);
            puzzleroomState.setNextRoom(Room.EAST);
        }
    }

    /**
     * Get current puzzle the player is in based on the dynamic toaLayout
     *
     * @param puzzleLayout
     * @return
     */
    public Puzzle getCurrentPuzzle(Puzzle[] puzzleLayout) {
        if (Rs2Player.getWorldLocation().getX() < 3533) {
            return Puzzle.WAITING_ROOM;
        } else if (isInSouthWestRoom()) {
            return puzzleLayout[0];
        } else if (isInNorthWestRoom()) {
            return puzzleLayout[1];
        } else if (isInSouthMiddleRoom()) {
            return puzzleLayout[2];
        } else if (isInNorthMiddleRoom()) {
            return puzzleLayout[3];
        } else {
            return puzzleLayout[4];
        }
    }
}
