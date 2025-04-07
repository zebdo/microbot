package net.runelite.client.plugins.microbot.toa.puzzleroom;

import com.google.common.collect.EvictingQueue;
import lombok.Getter;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.client.plugins.microbot.Microbot;

public class SequencePuzzleSolver {
    private static final int GROUND_OBJECT_ID = 45340;
    private static final int DISPLAY_GAME_OBJECT_ID = 45341;
    private static final int STEPPED_GAME_OBJECT_ID = 45342;
    private static final int GRAPHICS_OBJECT_RESET = 302;


    @Getter
    private final EvictingQueue<LocalPoint> points = EvictingQueue.create(5);

    @Getter
    private int completedTiles = 0;

    private boolean puzzleFinished = false;
    private int lastDisplayTick = 0;

    public void onGameObjectSpawned(GameObjectSpawned e)
    {
        if (puzzleFinished)
        {
            return;
        }

        if (e == null || e.getGameObject() == null) return;

        switch (e.getGameObject().getId())
        {
            case DISPLAY_GAME_OBJECT_ID:
                if (lastDisplayTick == (lastDisplayTick = Microbot.getClient().getTickCount()))
                {
                    reset();
                    puzzleFinished = true;
                    return;
                }
                points.add(e.getTile().getLocalLocation());
                completedTiles = 0;
                break;

            case STEPPED_GAME_OBJECT_ID:
                completedTiles++;
                break;
        }
    }

    public void onGraphicsObjectCreated(GraphicsObjectCreated e)
    {
        if (e.getGraphicsObject().getId() == GRAPHICS_OBJECT_RESET)
        {
            LocalPoint gLoc = e.getGraphicsObject().getLocation();
            Tile gTile = Microbot.getClient().getTopLevelWorldView().getScene().getTiles()[Microbot.getClient().getTopLevelWorldView().getPlane()][gLoc.getSceneX()][gLoc.getSceneY()];
            if (gTile.getGroundObject() != null && gTile.getGroundObject().getId() == GROUND_OBJECT_ID)
            {
                reset();
            }
        }
    }

    private void reset()
    {
        puzzleFinished = false;
        points.clear();
        completedTiles = 0;
        lastDisplayTick = 0;
    }
}
