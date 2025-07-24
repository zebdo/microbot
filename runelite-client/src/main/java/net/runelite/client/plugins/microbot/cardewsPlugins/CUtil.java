package net.runelite.client.plugins.microbot.cardewsPlugins;

import lombok.Getter;
import net.runelite.api.Point;
import net.runelite.api.World;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.slayer.enums.SlayerTaskMonster;

import java.awt.*;

public class CUtil {
    public static Point GetRandomPointInRectangle(Rectangle rect)
    {
        int randX = (int)(Math.random() * rect.width);
        int randY = (int)(Math.random() * rect.height);
        return new Point(randX, randY);
    }

    public static void SetMyAntiban(double _microbreakChance)
    {
        Rs2Antiban.resetAntibanSettings();

        Rs2AntibanSettings.antibanEnabled = true;
        Rs2AntibanSettings.usePlayStyle = true;
        Rs2AntibanSettings.randomIntervals = true;
        Rs2AntibanSettings.simulateFatigue = true;
        Rs2AntibanSettings.simulateAttentionSpan = true;
        Rs2AntibanSettings.behavioralVariability = true;
        Rs2AntibanSettings.nonLinearIntervals = true;
        Rs2AntibanSettings.dynamicIntensity = false;
        Rs2AntibanSettings.dynamicActivity = false;
        Rs2AntibanSettings.naturalMouse = true;
        Rs2AntibanSettings.simulateMistakes = true;
        Rs2AntibanSettings.moveMouseOffScreen = true;
        Rs2AntibanSettings.moveMouseRandomly = true;
        Rs2AntibanSettings.moveMouseRandomlyChance = 0.1;
        Rs2AntibanSettings.takeMicroBreaks = true;
        Rs2AntibanSettings.microBreakDurationLow = 2;
        Rs2AntibanSettings.microBreakDurationHigh = 15;
        Rs2AntibanSettings.microBreakChance = _microbreakChance;

        Rs2AntibanSettings.contextualVariability = true;
        Rs2AntibanSettings.actionCooldownChance = 0.6;

        Rs2AntibanSettings.devDebug = false;
    }

    @Getter
    public enum SlayerTarget {
        NONE(null, new WorldPoint(0,0,0)),
        BANSHEE(SlayerTaskMonster.BANSHEE, new WorldPoint(3436, 3558, 0)),
        BAT(SlayerTaskMonster.BAT, new WorldPoint(3370, 3485, 0)),
        BEAR(SlayerTaskMonster.BEAR, new WorldPoint(2705, 3340, 0)),
        BIRD(SlayerTaskMonster.BIRD, new WorldPoint(0, 0, 0)),  // BIRD uses alternative locations. Location is populated in script based on config.AlternativeBirdTask
        CAVE_CRAWLER(SlayerTaskMonster.CAVE_CRAWLER, new WorldPoint(2791, 9996, 0)),
        COW(SlayerTaskMonster.COW, new WorldPoint(2667, 3348, 0)),
        CRAWLING_HAND(SlayerTaskMonster.CRAWLING_HAND, new WorldPoint(3411, 3538, 0)),
        DOG(SlayerTaskMonster.DOG, new WorldPoint(2669, 3495, 0)),   // Goes to McGrubors wood Guard Dogs. Can extend Alternative tasks if wanted, similar to BIRD.
        DWARF(SlayerTaskMonster.DWARF, new WorldPoint(0, 0, 0)),    // Alt dwarf locations
        GHOST(SlayerTaskMonster.GHOST, new WorldPoint(1690, 10062, 0)),
        GOBLIN(SlayerTaskMonster.GOBLIN, new WorldPoint(3259, 3228, 0)),
        ICEFIEND(SlayerTaskMonster.ICEFIEND, new WorldPoint(3007, 3474, 0)),
        KALPHITE(SlayerTaskMonster.KALPHITE, new WorldPoint(0, 0, 0)),  // Alt kalphite locations
        LIZARD(SlayerTaskMonster.LIZARD, new WorldPoint(3441, 3065, 0)),
        MINOTAUR(SlayerTaskMonster.MINOTAUR, new WorldPoint(1881, 5217, 0)),
        MONKEY(SlayerTaskMonster.MONKEY, new WorldPoint(2877, 3154, 0)),
        RAT(SlayerTaskMonster.RAT, new WorldPoint(3199, 3209, 0)),
        SCORPION(SlayerTaskMonster.SCORPION, new WorldPoint(3043, 9789, 0)),
        SKELETON(SlayerTaskMonster.SKELETON, new WorldPoint(1641, 10047, 0)),
        SPIDER(SlayerTaskMonster.SPIDER, new WorldPoint(3167, 3245, 0)),
        WOLF(SlayerTaskMonster.WOLF, new WorldPoint(0, 0, 0)),  // Alt wolf locations
        ZOMBIE(SlayerTaskMonster.ZOMBIE, new WorldPoint(3146, 9900, 0));

        private final SlayerTaskMonster monsterData;
        private WorldPoint location;

        SlayerTarget(SlayerTaskMonster _monsterData, WorldPoint _location) {
            this.monsterData = _monsterData;
            this.location = _location;
        }

        public void SetLocation(WorldPoint _newLocation)
        {
            location = _newLocation;
        }
    }

    @Getter
    public enum AlternativeBirdTask {
        SEAGULL("Seagull", new WorldPoint(3027, 3204, 0)),
        CHICKEN("Chicken", new WorldPoint(3177, 3299, 0)),
        TERRORBIRD("Terrorbird", new WorldPoint(2378, 3434, 0));

        private final String monsterName;
        private final WorldPoint location;

        AlternativeBirdTask(String _monsterName, WorldPoint _location)
        {
            this.monsterName = _monsterName;
            this.location = _location;
        }
    }

    @Getter
    public enum AlternativeDwarfTask {
        DWARF_LVL7_11("Dwarf", new WorldPoint(3016, 3450, 0)),
        DWARF_LVL20("Dwarf", new WorldPoint(2864, 9877, 0)),
        CHAOS_DWARF_LVL49("Chaos dwarf", new WorldPoint(2923, 9760, 0));

        private final String monsterName;
        private final WorldPoint location;

        AlternativeDwarfTask(String _monsterName, WorldPoint _location)
        {
            this.monsterName = _monsterName;
            this.location = _location;
        }
    }

    @Getter
    public enum AlternativeKalphiteTask {
        KALPHITE_WORKER_LVL28_EAST("Kalphite Worker", new WorldPoint(3322, 9502, 0)),
        KALPHITE_WORKER_LVL28_NORTHWEST("Kalphite Worker", new WorldPoint(3279, 9519, 0)),
        KALPHITE_SOLDIER_LVL85_NORTH("Kalphite Soldier", new WorldPoint(3309, 9522, 0)),
        KALPHITE_SOLDIER_LVL85_SOUTH("Kalphite Soldier", new WorldPoint(3314, 9481, 0)),
        KALPHITE_GUARDIAN_LVL141("Kalphite Guardian", new WorldPoint(3280, 9498, 0));

        private final String monsterName;
        private final WorldPoint location;

        AlternativeKalphiteTask(String _monsterName, WorldPoint _location)
        {
            this.monsterName = _monsterName;
            this.location = _location;
        }
    }

    @Getter
    public enum AlternativeWolfTask {
        WOLF_LVL11_14("Wolf", new WorldPoint(2746, 3478, 0)),
        WHITE_WOLF_LVL25("White wolf", new WorldPoint(2848, 3481, 0));

        private final String monsterName;
        private final WorldPoint location;

        AlternativeWolfTask(String _monsterName, WorldPoint _location)
        {
            this.monsterName = _monsterName;
            this.location = _location;
        }
    }
}
