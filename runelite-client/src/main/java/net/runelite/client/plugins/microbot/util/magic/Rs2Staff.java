package net.runelite.client.plugins.microbot.util.magic;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.ItemID;

import java.util.Collections;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum Rs2Staff {

    NONE(0, Collections.emptyList()),
    STAFF_OF_AIR(ItemID.STAFF_OF_AIR, List.of(Runes.AIR)),
    STAFF_OF_WATER(ItemID.STAFF_OF_WATER, List.of(Runes.WATER)),
    STAFF_OF_EARTH(ItemID.STAFF_OF_EARTH, List.of(Runes.EARTH)),
    STAFF_OF_FIRE(ItemID.STAFF_OF_FIRE, List.of(Runes.FIRE)),
    AIR_BATTLESTAFF(ItemID.AIR_BATTLESTAFF, List.of(Runes.AIR)),
    WATER_BATTLESTAFF(ItemID.WATER_BATTLESTAFF, List.of(Runes.WATER)),
    EARTH_BATTLESTAFF(ItemID.EARTH_BATTLESTAFF, List.of(Runes.EARTH)),
    FIRE_BATTLESTAFF(ItemID.FIRE_BATTLESTAFF, List.of(Runes.FIRE)),
    MUD_BATTLESTAFF(ItemID.MUD_BATTLESTAFF, List.of(Runes.WATER, Runes.EARTH)),
    LAVA_BATTLESTAFF(ItemID.LAVA_BATTLESTAFF, List.of(Runes.FIRE, Runes.EARTH)),
    MYSTIC_AIR_STAFF(ItemID.MYSTIC_AIR_STAFF, List.of(Runes.AIR)),
    MYSTIC_WATER_STAFF(ItemID.MYSTIC_WATER_STAFF, List.of(Runes.WATER)),
    MYSTIC_EARTH_STAFF(ItemID.MYSTIC_EARTH_STAFF, List.of(Runes.EARTH)),
    MYSTIC_FIRE_STAFF(ItemID.MYSTIC_FIRE_STAFF, List.of(Runes.FIRE));

    private final int itemID;
    private final List<Runes> runes;
}
