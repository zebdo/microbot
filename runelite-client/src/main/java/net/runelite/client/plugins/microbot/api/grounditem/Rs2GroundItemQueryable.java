package net.runelite.client.plugins.microbot.api.grounditem;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.api.IEntityQueryable;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItemModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public final class Rs2GroundItemQueryable
        implements IEntityQueryable<Rs2GroundItemQueryable, Rs2GroundItemModel> {

    private Stream<Rs2GroundItemModel> source;

    public Rs2GroundItemQueryable() {
        this.source = Rs2GroundItemCache.getGroundItemsStream();
    }

    @Override
    public Rs2GroundItemQueryable where(java.util.function.Predicate<Rs2GroundItemModel> predicate) {
       source = source.filter(predicate);
       return this;
    }

    @Override
    public Rs2GroundItemModel first() {
        return source.findFirst().orElse(null);
    }

    @Override
    public Rs2GroundItemModel nearest() {
        WorldPoint playerLoc = Rs2Player.getWorldLocation();
        if (playerLoc == null) {
            return null;
        }

        return source
                .min(java.util.Comparator.comparingInt(
                        o -> o.getLocation().distanceTo(playerLoc)))
                .orElse(null);
    }

    @Override
    public Rs2GroundItemModel nearest(int maxDistance) {
        WorldPoint playerLoc = Rs2Player.getWorldLocation();
        if (playerLoc == null) {
            return null;
        }

        return source
                .filter(x -> x.getLocation().distanceTo(playerLoc) <= maxDistance)
                .min(java.util.Comparator.comparingInt(
                        o -> o.getLocation().distanceTo(playerLoc)))
                .orElse(null);
    }

    @Override
    public Rs2GroundItemModel withName(String name) {
        return source.filter(x -> x.getName() != null && x.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Rs2GroundItemModel withNames(String... names) {
        return source.filter(x -> {
            if (x.getName() == null) return false;
            return Arrays.stream(names)
                    .anyMatch(name -> x.getName().equalsIgnoreCase(name));
        }).findFirst().orElse(null);
    }

    @Override
    public Rs2GroundItemModel withId(int id) {
        return source.filter(x -> x.getId() == id)
                .findFirst()
                .orElse(null);
    }

    @Override
    public Rs2GroundItemModel withIds(int... ids) {
        return source.filter(x -> {
            for (int id : ids) {
                if (x.getId() == id) return true;
            }
            return false;
        }).findFirst().orElse(null);
    }

    @Override
    public java.util.List<Rs2GroundItemModel> toList() {
        return source.collect(Collectors.toList());
    }
}
