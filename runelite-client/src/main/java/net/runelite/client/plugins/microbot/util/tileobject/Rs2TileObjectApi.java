package net.runelite.client.plugins.microbot.util.tileobject;

import net.runelite.api.GameObject;
import net.runelite.api.Player;
import net.runelite.api.Tile;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * API for interacting with tile objects in the game world.
 */
public class Rs2TileObjectApi {

    private static int lastUpdateObjects = 0;
    private static List<Rs2TileObjectModel> tileObjects = new ArrayList<>();

    /**
     * Get all tile objects in the current scene
     * @return Stream of Rs2TileObjectModel
     */
    public static Stream<Rs2TileObjectModel> getObjectsStream() {

        if (lastUpdateObjects >= Microbot.getClient().getTickCount()) {
            return tileObjects.stream();
        }

        Player player = Microbot.getClient().getLocalPlayer();
        if (player == null) return Stream.empty();

        List<Rs2TileObjectModel> result = new ArrayList<>();

        var tileValues = Microbot.getClient().getTopLevelWorldView().getScene().getTiles()[Microbot.getClient().getTopLevelWorldView().getPlane()];

        for (Tile[] tileValue : tileValues) {
            for (Tile tile : tileValue) {
                if (tile == null) continue;

                if (tile.getGameObjects() != null) {
                    for (GameObject gameObject : tile.getGameObjects()) {
                        if (gameObject == null) continue;
                        if (gameObject.getSceneMinLocation().equals(tile.getSceneLocation())) {
                            result.add(new Rs2TileObjectModel(gameObject));
                        }
                    }
                }
                if (tile.getGroundObject() != null) {
                    result.add(new Rs2TileObjectModel(tile.getGroundObject()));
                }
                if (tile.getWallObject() != null) {
                    result.add(new Rs2TileObjectModel(tile.getWallObject()));
                }
                if (tile.getDecorativeObject() != null)  {
                    result.add(new Rs2TileObjectModel(tile.getDecorativeObject()));
                }
            }
        }

        tileObjects = result;
        lastUpdateObjects = Microbot.getClient().getTickCount();
        return result.stream();
    }

    /**
     * Returns the nearest tile object matching the supplied predicate, or null if none match.
     * Distance is based on straight-line world tile distance from the local player.
     *
     * @return nearest matching Rs2TileObjectModel or null
     */
    public static Rs2TileObjectModel getNearest() {
        return getNearest(null);
    }

    /**
     * Returns the nearest tile object matching the supplied predicate, or null if none match.
     * Distance is based on straight-line world tile distance from the local player.
     *
     * @param filter predicate to test objects
     * @return nearest matching Rs2TileObjectModel or null
     */
    public static Rs2TileObjectModel getNearest(Predicate<Rs2TileObjectModel> filter) {
        Player player = Microbot.getClient().getLocalPlayer();
        if (player == null) return null;

        WorldPoint playerLoc = player.getWorldLocation();

        Stream<Rs2TileObjectModel> stream = getObjectsStream();
        if (filter != null) {
            stream = stream.filter(filter);
        }

        return stream
                .min(Comparator.comparingInt(o -> o.getWorldLocation().distanceTo(playerLoc)))
                .orElse(null);
    }
}
