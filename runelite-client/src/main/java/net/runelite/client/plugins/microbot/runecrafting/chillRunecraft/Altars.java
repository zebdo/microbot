package net.runelite.client.plugins.microbot.runecrafting.chillRunecraft;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.ItemID;
import net.runelite.api.ObjectID;
import net.runelite.api.coords.WorldPoint;

@Getter
@RequiredArgsConstructor
public enum Altars
{
    AIR_ALTAR("Air Altar", ObjectID.ALTAR_34760, 34813, 34748 ,new WorldPoint(2990,3291,0), new WorldPoint(3012,3356,0), "Air Talisman", "Air Rune", ItemID.AIR_TALISMAN),
    EARTH_ALTAR("Earth Altar", ObjectID.ALTAR_34763, 34816, 34751 ,new WorldPoint(3302,3470,0), new WorldPoint(3253,3421,0), "Earth Talisman", "Earth Rune", ItemID.EARTH_TALISMAN),
    WATER_ALTAR("Water Altar", ObjectID.ALTAR_34762, 34815, 34750 ,new WorldPoint(3190,3162,0), new WorldPoint(3209,3219,2), "Water Talisman", "Water Rune", ItemID.WATER_TALISMAN),
    FIRE_ALTAR("Fire Altar", ObjectID.ALTAR_34764, 34817, 34752 ,new WorldPoint(3309,3252,0), new WorldPoint(3270,3167,0), "Fire Talisman", "Fire Rune", ItemID.FIRE_TALISMAN);

    private final String altarName;
    private final int altarID;
    private final int altarRuinsID;
    private final int portalID;
    private final WorldPoint altarWorldPoint;
    private final WorldPoint closestBankWorldPoint;
    private final String talismanName;
    private final String runeName;
    private final int talismanID;

}
