package net.runelite.client.plugins.microbot.api;

import net.runelite.api.coords.WorldPoint;
import java.util.List;
import java.util.function.Predicate;

public interface IEntityQueryable<Q extends IEntityQueryable<Q, E>, E> {
    Q where(Predicate<E> predicate);
    Q within(int distance);
    Q within(WorldPoint anchor, int distance);
    Q withName(String name);
    Q withNames(String... names);
    Q withId(int id);
    Q withIds(int... ids);
    E first();
    E nearest();
    E nearest(int maxDistance);
    E nearest(WorldPoint anchor, int maxDistance);
    List<E> toList();
    E firstOnClientThread();
    E nearestOnClientThread();
    E nearestOnClientThread(int maxDistance);
    E nearestOnClientThread(WorldPoint anchor, int maxDistance);
    List<E> toListOnClientThread();
}
