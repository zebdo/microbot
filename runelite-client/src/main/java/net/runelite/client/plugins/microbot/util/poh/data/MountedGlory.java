package net.runelite.client.plugins.microbot.util.poh.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.worldmap.TeleportLocationData;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum MountedGlory implements PohTransportable {
    EDGEVILLE("Edgeville", TeleportLocationData.EDGEVILLE.getLocation()),
    KARAMJA("Karamja", TeleportLocationData.KARAMJA.getLocation()),
    DRAYNOR_VILLAGE("Draynor Village", TeleportLocationData.DRAYNOR_VILLAGE.getLocation()),
    AL_KHARID("Al Kharid", TeleportLocationData.AL_KHARID.getLocation()),
    ;

    private final String destinationName;
    private final WorldPoint destination;

    private final int time = 4;


    @Override
    public boolean transport() {
        return Rs2GameObject.interact(getObject(), destinationName);
    }

    public static TileObject getObject() {
        return Rs2GameObject.getTileObject(ObjectID.POH_TROPHY_AMULETOFGLORY_4);
    }

    public static List<PohTransport> getTransports() {
        return Arrays.stream(values()).map(PohTransport::new).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "MountedGlory -> " + destinationName;
    }
}
