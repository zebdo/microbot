package net.runelite.client.plugins.microbot.api.tileitem;

import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.tileitem.models.Rs2TileItemModel;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Singleton
public class Rs2TileItemCache {

    private static int lastUpdateGroundItems = 0;
    private static List<Rs2TileItemModel> groundItems = new ArrayList<>();

    @Inject
    public Rs2TileItemCache(EventBus eventBus) {
        eventBus.register(this);
    }

    public Rs2TileItemQueryable query() {
        return new Rs2TileItemQueryable();
    }

    /**
     * Get all ground items in the current scene
     * Uses the existing ground item cache from util.cache package
     *
     * @return Stream of Rs2GroundItemModel
     */
    public static Stream<Rs2TileItemModel> getGroundItemsStream() {

        if (lastUpdateGroundItems >= Microbot.getClient().getTickCount()) {
            return groundItems.stream();
        }

        // Use the existing ground item cache
        List<Rs2TileItemModel> result = new ArrayList<>();

        groundItems = result;
        lastUpdateGroundItems = Microbot.getClient().getTickCount();
        return result.stream();
    }

    @Subscribe
    public void onItemSpawned(ItemSpawned event) {
        groundItems.add(new Rs2TileItemModel(event.getTile(), event.getItem()));
    }

    @Subscribe
    public void onItemDespawned(ItemDespawned event) {
        groundItems.removeIf(groundItem -> groundItem.getId() == event.getItem().getId() &&
                groundItem.getWorldLocation().equals(event.getTile().getWorldLocation())
                 && groundItem.getLocalLocation().equals(event.getTile().getLocalLocation())
        );
    }
}
