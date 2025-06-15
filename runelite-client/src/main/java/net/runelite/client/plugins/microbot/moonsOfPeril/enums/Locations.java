package net.runelite.client.plugins.microbot.moonsOfPeril.enums;

import net.runelite.api.coords.WorldPoint;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Locations {
    ECLIPSE_MOON_LOBBY(new WorldPoint(1466, 9632, 0)),
    BLOOD_MOON_LOBBY(new WorldPoint(1413, 9632, 0)),
    BLUE_MOON_LOBBY(new WorldPoint(1440, 9658, 0)),
    REWARDS_CHEST_LOBBY(new WorldPoint(1513, 9578, 0));

    public final WorldPoint worldPoint;
}
