package net.runelite.client.plugins.microbot.api.tileitem;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.api.IEntityQueryable;
import net.runelite.client.plugins.microbot.api.tileitem.models.Rs2TileItemModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public final class Rs2TileItemQueryable
        implements IEntityQueryable<Rs2TileItemQueryable, Rs2TileItemModel> {

    private Stream<Rs2TileItemModel> source;

    public Rs2TileItemQueryable() {
        this.source = Rs2TileItemCache.getGroundItemsStream();
    }

    @Override
    public Rs2TileItemQueryable where(java.util.function.Predicate<Rs2TileItemModel> predicate) {
       source = source.filter(predicate);
       return this;
    }

    @Override
    public Rs2TileItemModel first() {
        return source.findFirst().orElse(null);
    }

    @Override
    public Rs2TileItemModel nearest() {
        WorldPoint playerLoc = Rs2Player.getWorldLocation();
        if (playerLoc == null) {
            return null;
        }

        return source
                .min(java.util.Comparator.comparingInt(
                        o -> o.getWorldLocation().distanceTo(playerLoc)))
                .orElse(null);
    }

    @Override
    public Rs2TileItemModel nearest(int maxDistance) {
        WorldPoint playerLoc = Rs2Player.getWorldLocation();
        if (playerLoc == null) {
            return null;
        }

        return source
                .filter(x -> x.getWorldLocation().distanceTo(playerLoc) <= maxDistance)
                .min(java.util.Comparator.comparingInt(
                        o -> o.getWorldLocation().distanceTo(playerLoc)))
                .orElse(null);
    }

    @Override
    public Rs2TileItemModel withName(String name) {
        return source.filter(x -> x.getName() != null && x.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Rs2TileItemModel withNames(String... names) {
        return source.filter(x -> {
            if (x.getName() == null) return false;
            return Arrays.stream(names)
                    .anyMatch(name -> x.getName().equalsIgnoreCase(name));
        }).findFirst().orElse(null);
    }

    @Override
    public Rs2TileItemModel withId(int id) {
        return source.filter(x -> x.getId() == id)
                .findFirst()
                .orElse(null);
    }

    @Override
    public Rs2TileItemModel withIds(int... ids) {
        return source.filter(x -> {
            for (int id : ids) {
                if (x.getId() == id) return true;
            }
            return false;
        }).findFirst().orElse(null);
    }

    @Override
    public java.util.List<Rs2TileItemModel> toList() {
        return source.collect(Collectors.toList());
    }
}
