package net.runelite.client.plugins.microbot.bee.salamanders;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.coords.WorldPoint;

@Getter
@RequiredArgsConstructor
public enum SalamanderHunting {


    GREEN("Green salamander", 8990, new WorldPoint(2340, 3630, 0)),
    ORANGE("Orange salamander", 8990, new WorldPoint(2340, 3167, 0)),
    RED("Red salamander", 8990, new WorldPoint(2464, 3226, 0)),
    BLACK("Black salamander", 8990, new WorldPoint(3348, 3794, 0)),
    TECU("Tecu salamander", 8990, new WorldPoint(3725, 3200, 0));

    private final String name;
    private final int treeId;
    private final WorldPoint huntingPoint;

    @Override
    public String toString() {
        return name;
    }
}

/*
*
*     WorldArea AREA_LUMBRIDGE = new WorldArea(3205, 3200, 34, 37, 0);
    WorldArea AREA_VERDE_SALAMANDER = new WorldArea(3531, 3444, 10, 9, 0);
    WorldArea AREA_ROJA_SALAMANDER = new WorldArea(2443, 3217, 13, 12, 0);
    WorldArea AREA_NEGRA_SALAMANDER = new WorldArea(3290, 3663, 14, 16, 0);*/