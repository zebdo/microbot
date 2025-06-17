package net.runelite.client.plugins.microbot.moonsOfPeril.enums;

import net.runelite.api.coords.WorldPoint;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum Locations {
    ECLIPSE_LOBBY(new WorldPoint(1466, 9632, 0)),
    ECLIPSE_ARENA_CENTER(WorldPoint.fromRegion(6038, 13, 33, 0)),
    ECLIPSE_ATTACK_1(WorldPoint.fromRegion(6038, 17, 35, 0)),
    ECLIPSE_ATTACK_2(WorldPoint.fromRegion(6038, 19, 33, 0)),
    ECLIPSE_ATTACK_3(WorldPoint.fromRegion(6038, 19, 31, 0)),
    ECLIPSE_ATTACK_4(WorldPoint.fromRegion(6038, 13, 31, 0)),
    ECLIPSE_ATTACK_5(WorldPoint.fromRegion(6038, 13, 33, 0)),
    ECLIPSE_ATTACK_6(WorldPoint.fromRegion(6038, 15, 29, 0)),
    ECLIPSE_CLONE_TILE(WorldPoint.fromRegion(6038, 16, 32, 0)),
    BLOOD_LOBBY(new WorldPoint(1413, 9632, 0)),
    BLOOD_ARENA_CENTER(WorldPoint.fromRegion(5526, 48, 32, 0)),
    BLOOD_ATTACK_1(WorldPoint.fromRegion(5526, 47, 29, 0)),
    BLOOD_ATTACK_2(WorldPoint.fromRegion(5526, 45, 31, 0)),
    BLOOD_ATTACK_3(WorldPoint.fromRegion(5526, 45, 33, 0)),
    BLOOD_ATTACK_4(WorldPoint.fromRegion(5526, 51, 33, 0)),
    BLOOD_ATTACK_5(WorldPoint.fromRegion(5526, 51, 31, 0)),
    BLOOD_ATTACK_6(WorldPoint.fromRegion(5526, 49, 35, 0)),
    BLUE_LOBBY(new WorldPoint(1440, 9658, 0)),
    BLUE_ARENA_CENTER(WorldPoint.fromRegion(5783, 32, 16, 0)),
    BLUE_ATTACK_1(WorldPoint.fromRegion(5783, 31, 13, 0)),
    BLUE_ATTACK_2(WorldPoint.fromRegion(5783, 29, 15, 0)),
    BLUE_ATTACK_3(WorldPoint.fromRegion(5783, 29, 17, 0)),
    BLUE_ATTACK_4(WorldPoint.fromRegion(5783, 35, 17, 0)),
    BLUE_ATTACK_5(WorldPoint.fromRegion(5783, 35, 15, 0)),
    BLUE_ATTACK_6(WorldPoint.fromRegion(5783, 33, 19, 0)),
    BLUE_ICESHARD_SAFEPOT(WorldPoint.fromRegion(5783, 32, 17, 0)),
    REWARDS_CHEST_LOBBY(new WorldPoint(1513, 9578, 0));

    public final WorldPoint worldPoint;

    /** All Eclipse Moon attack tiles as a WorldPoint[] array. */
    public static WorldPoint[] eclipseAttackTiles() {
        return Arrays.stream(values())
                .filter(l -> l.name().startsWith("ECLIPSE_ATTACK_"))
                .map(l -> l.worldPoint)
                .toArray(WorldPoint[]::new);      // ← returns a WorldPoint[]
    }

    /** All Blue Moon attack tiles as a WorldPoint[] array. */
    public static WorldPoint[] blueAttackTiles() {
        return Arrays.stream(values())
                .filter(l -> l.name().startsWith("BLUE_ATTACK_"))
                .map(l -> l.worldPoint)
                .toArray(WorldPoint[]::new);      // ← returns a WorldPoint[]
    }

    /** All Blood Moon attack tiles as a WorldPoint[] array. */
    public static WorldPoint[] bloodAttackTiles() {
        return Arrays.stream(values())
                .filter(l -> l.name().startsWith("BLOOD_ATTACK_"))
                .map(l -> l.worldPoint)
                .toArray(WorldPoint[]::new);      // ← returns a WorldPoint[]
    }

}

