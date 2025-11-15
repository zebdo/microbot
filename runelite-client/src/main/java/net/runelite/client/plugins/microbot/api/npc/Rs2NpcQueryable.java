package net.runelite.client.plugins.microbot.api.npc;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.api.IEntityQueryable;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public final class Rs2NpcQueryable
        implements IEntityQueryable<Rs2NpcQueryable, Rs2NpcModel> {

    private Stream<Rs2NpcModel> source;

    public Rs2NpcQueryable() {
        this.source = Rs2NpcCache.getNpcsStream();
    }

    @Override
    public Rs2NpcQueryable where(java.util.function.Predicate<Rs2NpcModel> predicate) {
       source = source.filter(predicate);
       return this;
    }

    @Override
    public Rs2NpcModel first() {
        return source.findFirst().orElse(null);
    }

    @Override
    public Rs2NpcModel nearest() {
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
    public Rs2NpcModel nearest(int maxDistance) {
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
    public Rs2NpcModel withName(String name) {
        return source.filter(x -> x.getName() != null && x.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Rs2NpcModel withNames(String... names) {
        return source.filter(x -> {
            if (x.getName() == null) return false;
            return Arrays.stream(names)
                    .anyMatch(name -> x.getName().equalsIgnoreCase(name));
        }).findFirst().orElse(null);
    }

    @Override
    public Rs2NpcModel withId(int id) {
        return source.filter(x -> x.getId() == id)
                .findFirst()
                .orElse(null);
    }

    @Override
    public Rs2NpcModel withIds(int... ids) {
        return source.filter(x -> {
            for (int id : ids) {
                if (x.getId() == id) return true;
            }
            return false;
        }).findFirst().orElse(null);
    }

    @Override
    public java.util.List<Rs2NpcModel> toList() {
        return source.collect(Collectors.toList());
    }
}
