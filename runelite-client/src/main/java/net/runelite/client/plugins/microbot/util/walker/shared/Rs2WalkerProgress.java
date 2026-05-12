package net.runelite.client.plugins.microbot.util.walker.shared;

import net.runelite.api.coords.WorldPoint;

public final class Rs2WalkerProgress {
    private Rs2WalkerProgress() {
    }

    public static boolean hasMovementOrProgress(WorldPoint before, WorldPoint now, WorldPoint expectedDestination, WorldPoint target) {
        if (before == null || now == null) {
            return false;
        }
        if (!now.equals(before)) {
            return true;
        }
        if (isWithinChebyshev(now, expectedDestination, 1)) {
            return true;
        }
        return madeProgressToward(before, now, target);
    }

    public static boolean madeProgressToward(WorldPoint before, WorldPoint now, WorldPoint target) {
        if (before == null || now == null || target == null) {
            return false;
        }
        return now.distanceTo2D(target) < before.distanceTo2D(target);
    }

    public static boolean isWithinChebyshev(WorldPoint from, WorldPoint to, int maxInclusive) {
        if (from == null || to == null) {
            return false;
        }
        if (from.getPlane() != to.getPlane()) {
            return false;
        }
        return from.distanceTo2D(to) <= maxInclusive;
    }
}
