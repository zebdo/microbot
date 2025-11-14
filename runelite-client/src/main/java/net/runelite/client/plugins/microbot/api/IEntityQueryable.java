package net.runelite.client.plugins.microbot.api;

public interface IEntityQueryable<Q extends IEntityQueryable<Q, E>, E> {
    Q where(java.util.function.Predicate<E> predicate);
    E first();
    E nearest();
    E nearest(int maxDistance);
    E withName(String name);
    E withNames(String...names);
    E withId(int id);
    E withIds(int...ids);
    java.util.List<E> toList();
}
