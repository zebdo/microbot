package net.runelite.client.plugins.microbot.util.poh;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.plugins.microbot.util.poh.data.JewelleryBox;

import static net.runelite.client.plugins.microbot.util.poh.PohConfig.CONFIG_GROUP;

@ConfigGroup(CONFIG_GROUP)
public interface PohConfig extends Config {
    String CONFIG_GROUP = "pohTeleports";
    String PORTALS = "portals";
    String NEXUS = "nexus";
    String MOUNTED_GLORY = "mountedGlory";
    String MOUNTED_DIGSITE = "mountedDigsite";
    String MOUNTED_XERICS = "mountedXerics";
    String MOUNTED_MYTHS = "mountedMyths";
    String JEWELLERY_BOX = "jewelleryBox";
    String FAIRY_RING = "fairyRing";
    String WILDY_OBELISK = "wildyObelisk";
    String SPIRIT_TREE = "spiritTree";

    // list of available portals in the POH's Portal Chamber
    // Should be stored as 'Name,Name'
    @ConfigItem(
            keyName = PORTALS,
            name = "",
            description = "",
            hidden = true
    )
    default String portals() {
        return "";
    }


    // Achievement Gallery mounts and utility
    @ConfigItem(keyName = MOUNTED_GLORY, name = "", description = "", hidden = true)
    default boolean hasMountedGlory() {
        return false;
    }

    @ConfigItem(keyName = MOUNTED_DIGSITE, name = "", description = "", hidden = true)
    default boolean hasMountedDigsitePendant() {
        return false;
    }

    @ConfigItem(keyName = MOUNTED_XERICS, name = "", description = "", hidden = true)
    default boolean hasMountedXericsTalisman() {
        return false;
    }

    @ConfigItem(keyName = MOUNTED_MYTHS, name = "", description = "", hidden = true)
    default boolean hasMountedMythicalCape() {
        return false;
    }


    @ConfigItem(keyName = JEWELLERY_BOX, name = "", description = "", hidden = true)
    default JewelleryBox jewelleryBoxType() {
        return JewelleryBox.NONE;
    }

    @ConfigItem(keyName = NEXUS, name = "", description = "", hidden = true)
    default String nexusTeleports() {
        return "";
    }

    // Garden teleports
    @ConfigItem(keyName = FAIRY_RING, name = "", description = "", hidden = true)
    default boolean hasFairyRing() {
        return false;
    }

    @ConfigItem(keyName = SPIRIT_TREE, name = "", description = "", hidden = true)
    default boolean hasSpiritTree() {
        return false;
    }

    @ConfigItem(keyName = WILDY_OBELISK, name = "", description = "", hidden = true)
    default boolean hasWildernessObelisk() {
        return false;
    }
}