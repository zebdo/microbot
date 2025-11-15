package net.runelite.client.plugins.microbot.api.player;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.api.IEntityQueryable;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.player.Rs2PlayerModel;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public final class Rs2PlayerQueryable
        implements IEntityQueryable<Rs2PlayerQueryable, Rs2PlayerModel> {

    private Stream<Rs2PlayerModel> source;
    private boolean includeLocalPlayer = false;

    public Rs2PlayerQueryable() {
        this.source = Rs2PlayerCache.getPlayersStream();
    }

    /**
     * Include the local player in the query results
     * @return this queryable for chaining
     */
    public Rs2PlayerQueryable includeLocalPlayer() {
        this.includeLocalPlayer = true;
        this.source = Rs2PlayerCache.getPlayersStream(true);
        return this;
    }

    @Override
    public Rs2PlayerQueryable where(java.util.function.Predicate<Rs2PlayerModel> predicate) {
       source = source.filter(predicate);
       return this;
    }

    @Override
    public Rs2PlayerModel first() {
        return source.findFirst().orElse(null);
    }

    @Override
    public Rs2PlayerModel nearest() {
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
    public Rs2PlayerModel nearest(int maxDistance) {
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
    public Rs2PlayerModel withName(String name) {
        return source.filter(x -> x.getName() != null && x.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Rs2PlayerModel withNames(String... names) {
        return source.filter(x -> {
            if (x.getName() == null) return false;
            return Arrays.stream(names)
                    .anyMatch(name -> x.getName().equalsIgnoreCase(name));
        }).findFirst().orElse(null);
    }

    @Override
    public Rs2PlayerModel withId(int id) {
        return source.filter(x -> x.getId() == id)
                .findFirst()
                .orElse(null);
    }

    @Override
    public Rs2PlayerModel withIds(int... ids) {
        return source.filter(x -> {
            for (int id : ids) {
                if (x.getId() == id) return true;
            }
            return false;
        }).findFirst().orElse(null);
    }

    @Override
    public java.util.List<Rs2PlayerModel> toList() {
        return source.collect(Collectors.toList());
    }
}
