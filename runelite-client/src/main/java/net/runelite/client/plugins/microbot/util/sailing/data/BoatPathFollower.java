package net.runelite.client.plugins.microbot.util.sailing.data;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.sailing.Rs2Sailing;

import java.util.List;

import static net.runelite.client.plugins.microbot.util.sailing.Rs2Sailing.sailTo;

public class BoatPathFollower {
    private final List<WorldPoint> path;
    private int currentIndex = 0;
    private static final int WAYPOINT_TOLERANCE = 2; // tiles

    public BoatPathFollower(List<WorldPoint> fullPath) {
        this.path = fullPath;
        // Optional: skip waypoints behind us
        this.currentIndex = findStartingIndex();
    }

    public boolean loop() {
        if (!Rs2Sailing.isOnBoat()) {
            return false;
        }

        if (currentIndex >= path.size()) {
            // Done, we reached the final destination
            stopFollowing();
            return true;
        }

        WorldPoint boat = Rs2Sailing.getPlayerBoatLocation();
        if (boat == null) {
            return false;
        }

        WorldPoint target = path.get(currentIndex);

        // If we're close enough to this waypoint, go to the next one
        if (boat.distanceTo(target) <= WAYPOINT_TOLERANCE) {
            currentIndex++;
            return false;
        }

        // Actively sail towards the current waypoint
        sailTo(target);
        return false;
    }

    private int findStartingIndex() {
        WorldPoint boat = Rs2Sailing.getPlayerBoatLocation();
        if (boat == null) return 0;

        int bestIndex = 0;
        int bestDist = Integer.MAX_VALUE;

        for (int i = 0; i < path.size(); i++) {
            int dist = boat.distanceTo(path.get(i));
            if (dist < bestDist) {
                bestDist = dist;
                bestIndex = i;
            }
        }

        return bestIndex;
    }

    private void stopFollowing() {
        Rs2Sailing.unsetSails();
        // e.g. clear some flag, stop the script, whatever your framework uses
    }

    /**
     * Returns the current waypoint index in the path.
     */
    public int getCurrentWaypointIndex() {
        return currentIndex;
    }
}
