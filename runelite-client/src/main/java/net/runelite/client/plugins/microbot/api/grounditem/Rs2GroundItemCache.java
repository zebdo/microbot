package net.runelite.client.plugins.microbot.api.grounditem;

import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItemModel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class Rs2GroundItemCache {

    private static int lastUpdateGroundItems = 0;
    private static List<Rs2GroundItemModel> groundItems = new ArrayList<>();

    public Rs2GroundItemQueryable query() {
        return new Rs2GroundItemQueryable();
    }

    public static void registerEventBus() {
        Microbot.getEventBus().register(Rs2GroundItemCache.class);
    }

    /**
     * Get all ground items in the current scene
     * Uses the existing ground item cache from util.cache package
     * @return Stream of Rs2GroundItemModel
     */
    public static Stream<Rs2GroundItemModel> getGroundItemsStream() {

        if (lastUpdateGroundItems >= Microbot.getClient().getTickCount()) {
            return groundItems.stream();
        }

        // Use the existing ground item cache
        List<Rs2GroundItemModel> result =  new ArrayList<>();

        groundItems = result;
        lastUpdateGroundItems = Microbot.getClient().getTickCount();
        return result.stream();
    }

    @Subscribe
    public void onItemSpawned(ItemSpawned event)
    {
        /*groundItems.add(
                new TileItemEx(
                        event.getItem(),
                        WorldPoint.fromLocal(Static.getClient(), event.getTile().getLocalLocation()),
                        event.getTile().getLocalLocation()
                )
        );*/
    }

    @Subscribe
    public void onItemDespawned(ItemDespawned event)
    {
        /*groundItems.removeIf(ex -> ex.getItem().equals(event.getItem()) &&
                ex.getWorldPoint().equals(WorldPoint.fromLocal(Static.getClient(), event.getTile().getLocalLocation())) &&
                ex.getLocalPoint().equals(event.getTile().getLocalLocation())
        );*/
    }
}
