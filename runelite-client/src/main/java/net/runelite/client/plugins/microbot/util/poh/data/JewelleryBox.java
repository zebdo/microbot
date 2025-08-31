package net.runelite.client.plugins.microbot.util.poh.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.GameObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.plugins.microbot.util.equipment.JewelleryLocationEnum;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.poh.PohTeleports;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum JewelleryBox {
    NONE(-1),
    BASIC(ObjectID.POH_JEWELLERY_BOX_1,
            // Games Necklace teleports
            JewelleryLocationEnum.BARBARIAN_ASSAULT,
            JewelleryLocationEnum.BURTHORPE_GAMES_ROOM,
            JewelleryLocationEnum.TEARS_OF_GUTHIX,
            JewelleryLocationEnum.CORPOREAL_BEAST,
            JewelleryLocationEnum.WINTERTODT_CAMP,
            // Ring of Dueling teleports
            JewelleryLocationEnum.PVP_ARENA,
            JewelleryLocationEnum.FEROX_ENCLAVE,
            JewelleryLocationEnum.CASTLE_WARS,
            JewelleryLocationEnum.FORTIS_COLOSSEUM
    ),
    FANCY(ObjectID.POH_JEWELLERY_BOX_2,
            // All Basic teleports
            JewelleryLocationEnum.BARBARIAN_ASSAULT,
            JewelleryLocationEnum.BURTHORPE_GAMES_ROOM,
            JewelleryLocationEnum.TEARS_OF_GUTHIX,
            JewelleryLocationEnum.CORPOREAL_BEAST,
            JewelleryLocationEnum.WINTERTODT_CAMP,
            JewelleryLocationEnum.PVP_ARENA,
            JewelleryLocationEnum.FEROX_ENCLAVE,
            JewelleryLocationEnum.CASTLE_WARS,
            JewelleryLocationEnum.FORTIS_COLOSSEUM,
            // Combat Bracelet teleports
            JewelleryLocationEnum.WARRIORS_GUILD,
            JewelleryLocationEnum.CHAMPIONS_GUILD,
            JewelleryLocationEnum.EDGEVILLE_MONASTERY,
            JewelleryLocationEnum.RANGING_GUILD,
            // Skills Necklace teleports
            JewelleryLocationEnum.FISHING_GUILD_NECK,
            JewelleryLocationEnum.MINING_GUILD,
            JewelleryLocationEnum.CRAFTING_GUILD,
            JewelleryLocationEnum.COOKING_GUILD,
            JewelleryLocationEnum.WOODCUTTING_GUILD,
            JewelleryLocationEnum.FARMING_GUILD
    ),
    ORNATE(ObjectID.POH_JEWELLERY_BOX_3,
            // All Fancy teleports
            JewelleryLocationEnum.BARBARIAN_ASSAULT,
            JewelleryLocationEnum.BURTHORPE_GAMES_ROOM,
            JewelleryLocationEnum.TEARS_OF_GUTHIX,
            JewelleryLocationEnum.CORPOREAL_BEAST,
            JewelleryLocationEnum.WINTERTODT_CAMP,
            JewelleryLocationEnum.PVP_ARENA,
            JewelleryLocationEnum.FEROX_ENCLAVE,
            JewelleryLocationEnum.CASTLE_WARS,
            JewelleryLocationEnum.FORTIS_COLOSSEUM,
            JewelleryLocationEnum.WARRIORS_GUILD,
            JewelleryLocationEnum.CHAMPIONS_GUILD,
            JewelleryLocationEnum.EDGEVILLE_MONASTERY,
            JewelleryLocationEnum.RANGING_GUILD,
            JewelleryLocationEnum.FISHING_GUILD_NECK,
            JewelleryLocationEnum.MINING_GUILD,
            JewelleryLocationEnum.CRAFTING_GUILD,
            JewelleryLocationEnum.COOKING_GUILD,
            JewelleryLocationEnum.WOODCUTTING_GUILD,
            JewelleryLocationEnum.FARMING_GUILD,
            // Amulet of Glory teleports
            JewelleryLocationEnum.EDGEVILLE,
            JewelleryLocationEnum.KARAMJA,
            JewelleryLocationEnum.DRAYNOR_VILLAGE,
            JewelleryLocationEnum.AL_KHARID,
            // Ring of Wealth teleports
            JewelleryLocationEnum.MISCELLANIA,
            JewelleryLocationEnum.GRAND_EXCHANGE,
            JewelleryLocationEnum.FALADOR_PARK,
            JewelleryLocationEnum.DONDAKAN
    );

    private final int objectId;
    private final List<JewelleryLocationEnum> jewelleryLocations;

    JewelleryBox(int objectId, JewelleryLocationEnum... locations) {
        this.objectId = objectId;
        this.jewelleryLocations = Arrays.asList(locations);
    }

    public List<PohTransport> getTransports() {
        return jewelleryLocations.stream()
                .map(location -> new PohTransport(new PohTransportable() {
                            @Override
                            public String toString() {
                                return name() + " -> " + location.getDestination();
                            }

                            @Override
                            public WorldPoint getDestination() {
                                return location.getLocation();
                            }

                            @Override
                            public boolean transport() {
                                return PohTeleports.useJewelleryBox(location);
                            }

                            @Override
                            public int getTime() {
                                return 6;
                            }
                        })
                ).collect(Collectors.toList());
    }

    public static Integer[] getJewelleryBoxIds() {
        return Arrays.stream(JewelleryBox.values()).map(JewelleryBox::getObjectId).toArray(Integer[]::new);
    }

    public static GameObject getObject() {
        return Rs2GameObject.getGameObject(getJewelleryBoxIds());
    }

    public static JewelleryBox getJewelleryBox() {
        GameObject go = getObject();
        if (go == null) {
            return NONE;
        }
        for (JewelleryBox box : values()) {
            if (box.objectId == go.getId()) {
                return box;
            }
        }
        return NONE;
    }
}