package net.runelite.client.plugins.microbot.moonsOfPeril.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GameObjects {
    ECLIPSE_MOON_STATUE_ID(51374),
    BLOOD_MOON_STATUE_ID(51372),
    BLUE_MOON_STATUE_ID(51373);

    public final int ID;
}