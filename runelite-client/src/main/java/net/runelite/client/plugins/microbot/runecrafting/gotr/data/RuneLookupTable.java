package net.runelite.client.plugins.microbot.runecrafting.gotr.data;

import net.runelite.api.gameval.ItemID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RuneLookupTable {
    private final Map<Integer, List<LevelMultiplier>> lookupTable;

    public RuneLookupTable() {
        this.lookupTable = new HashMap<>();
        initializeLookupTable();
    }

    private void initializeLookupTable() {
        lookupTable.put(ItemID.AIRRUNE, createLevelMultipliers(
            new LevelMultiplier(11, 2),
            new LevelMultiplier(22, 3),
            new LevelMultiplier(33, 4),
            new LevelMultiplier(44, 5),
            new LevelMultiplier(55, 6),
            new LevelMultiplier(66, 7),
            new LevelMultiplier(77, 8),
            new LevelMultiplier(88, 9),
            new LevelMultiplier(99, 10)
        ));
        
        lookupTable.put(ItemID.MINDRUNE, createLevelMultipliers(
            new LevelMultiplier(14, 2),
            new LevelMultiplier(28, 3),
            new LevelMultiplier(42, 4),
            new LevelMultiplier(56, 5),
            new LevelMultiplier(70, 6),
            new LevelMultiplier(84, 7),
            new LevelMultiplier(98, 8)
        ));
        
        lookupTable.put(ItemID.WATERRUNE, createLevelMultipliers(
            new LevelMultiplier(19, 2),
            new LevelMultiplier(38, 3),
            new LevelMultiplier(57, 4),
            new LevelMultiplier(76, 5),
            new LevelMultiplier(95, 6)
        ));
        
        lookupTable.put(ItemID.EARTHRUNE, createLevelMultipliers(
            new LevelMultiplier(26, 2),
            new LevelMultiplier(52, 3),
            new LevelMultiplier(78, 4),
            new LevelMultiplier(104, 5)
        ));
        
        lookupTable.put(ItemID.FIRERUNE, createLevelMultipliers(
            new LevelMultiplier(35, 2),
            new LevelMultiplier(70, 3)
        ));
        
        lookupTable.put(ItemID.BODYRUNE, createLevelMultipliers(
            new LevelMultiplier(46, 2),
            new LevelMultiplier(92, 3)
        ));
        
        lookupTable.put(ItemID.COSMICRUNE, createLevelMultipliers(
            new LevelMultiplier(59, 2)
        ));
        
        lookupTable.put(ItemID.CHAOSRUNE, createLevelMultipliers(
            new LevelMultiplier(74, 2)
        ));
        
        lookupTable.put(ItemID.NATURERUNE, createLevelMultipliers(
            new LevelMultiplier(91, 2)
        ));
        
        lookupTable.put(ItemID.LAWRUNE, createLevelMultipliers(
            new LevelMultiplier(95, 2)
        ));
        
        lookupTable.put(ItemID.DEATHRUNE, createLevelMultipliers(
            new LevelMultiplier(99, 2)
        ));
    }

    private List<LevelMultiplier> createLevelMultipliers(LevelMultiplier... multipliers) {
        List<LevelMultiplier> list = new ArrayList<>();
        for (LevelMultiplier multiplier : multipliers) {
            list.add(multiplier);
        }
        return list;
    }

    public int getHighestMultiplier(int runeId, int level) {
        List<LevelMultiplier> runeMultipliers = lookupTable.get(runeId);
        if (runeMultipliers == null) {
            // Rune ID not found in the lookup table
            return 1;
        }

        int highestMultiplier = 1;
        for (LevelMultiplier levelMultiplier : runeMultipliers) {
            if (levelMultiplier.getLevel() <= level && levelMultiplier.getMultiplier() > highestMultiplier) {
                highestMultiplier = levelMultiplier.getMultiplier();
            }
        }

        return highestMultiplier;
    }

    private static class LevelMultiplier {
        private final int level;
        private final int multiplier;

        public LevelMultiplier(int level, int multiplier) {
            this.level = level;
            this.multiplier = multiplier;
        }

        public int getLevel() {
            return level;
        }

        public int getMultiplier() {
            return multiplier;
        }
    }
}