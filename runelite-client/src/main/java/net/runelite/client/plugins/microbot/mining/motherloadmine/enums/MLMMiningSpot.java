package net.runelite.client.plugins.microbot.mining.motherloadmine.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.coords.WorldPoint;

import java.util.Arrays;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum MLMMiningSpot {
    IDLE(null),
    WEST_LOWER(Arrays.asList(new WorldPoint(3731, 5659, 0), new WorldPoint(3731, 5663, 0))),
    WEST_MID(Arrays.asList(new WorldPoint(3730, 5666, 0), new WorldPoint(3731, 5669, 0))),
    WEST_UPPER(Arrays.asList(new WorldPoint(3752, 5683, 0), new WorldPoint(3752, 5680, 0))),
    SOUTH_EAST(Arrays.asList(new WorldPoint(3745, 5647, 0), new WorldPoint(3756, 5653, 0))),
    SOUTH_WEST(Arrays.asList(new WorldPoint(3740, 5648, 0), new WorldPoint(3740, 5648, 0))),
    NORTH(null),
    EAST_UPPER(Arrays.asList(new WorldPoint(3760, 5673, 0), new WorldPoint(3759, 5673, 0))),
    ANY(null);

    private final List<WorldPoint> worldPoint;

}
