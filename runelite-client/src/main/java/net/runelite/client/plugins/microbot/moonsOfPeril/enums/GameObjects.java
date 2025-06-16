package net.runelite.client.plugins.microbot.moonsOfPeril.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GameObjects {
    ECLIPSE_MOON_STATUE_ID(51374),
    ECLIPSE_MOON_NPC_ID(13012),
    BLOOD_MOON_STATUE_ID(51372),
    BLOOD_MOON_NPC_ID(13011),
    BLUE_MOON_STATUE_ID(51373),
    BLUE_MOON_NPC_ID(13013),
    SIGIL_NPC_ID(13015),;

    public final int ID;
}