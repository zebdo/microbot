package net.runelite.client.plugins.microbot.api.tileobject;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.IEntityQueryable;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import java.util.stream.Collectors;
import java.util.stream.Stream;


public final class Rs2TileObjectQueryable
        implements IEntityQueryable<Rs2TileObjectQueryable, Rs2TileObjectModel> {

    private Stream<Rs2TileObjectModel> source;

    public Rs2TileObjectQueryable() {
        this.source = Rs2TileObjectCache.getObjectsStream();
    }

    @Override
    public Rs2TileObjectQueryable where(java.util.function.Predicate<Rs2TileObjectModel> predicate) {
       source = source.filter(predicate);
       return this;
    }

    @Override
    public Rs2TileObjectModel first() {
        return source.findFirst().orElse(null);
    }

    @Override
    public Rs2TileObjectModel nearest() {
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
    public Rs2TileObjectModel nearest(int maxDistance) {
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
    public Rs2TileObjectModel withName(String name) {
        return source.filter(x -> x.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    @Override
    public Rs2TileObjectModel withNames(String... names) {
        return Microbot.getClientThread().invoke(() -> source.filter(x -> {
            for (String name : names) {
                if (x.getName().equalsIgnoreCase(name)) {
                    return true;
                }
            }
            return false;
        }).findFirst().orElse(null));
    }

    @Override
    public Rs2TileObjectModel withId(int id) {
        return source.filter(x -> x.getId() == id).findFirst().orElse(null);
    }

    @Override
    public Rs2TileObjectModel withIds(int... ids) {
        return source.filter(x -> {
            for (int id : ids) {
                if (x.getId() == id) {
                    return true;
                }
            }
            return false;
        }).findFirst().orElse(null);
    }

    @Override
    public java.util.List<Rs2TileObjectModel> toList() {
        return source.collect(Collectors.toList());
    }
}
