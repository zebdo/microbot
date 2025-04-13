package net.runelite.client.plugins.microbot.pluginscheduler.condition.skill;

import lombok.Getter;
import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.ConditionType;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;

/**
 * Skill level-based condition for script execution.
 */
@Getter 
public class SkillLevelCondition extends SkillCondition {
    private int currentTargetLevel;
    private final int targetLevelMin;
    private final int targetLevelMax;
    private int startLevel;
    private int[] startLevelsBySkill; // Used for total level tracking
    
    public SkillLevelCondition(Skill skill, int targetLevel) {
        super(skill); // Call parent constructor with skill
        this.currentTargetLevel = targetLevel;
        this.targetLevelMin = targetLevel;
        this.targetLevelMax = targetLevel;
        initializeLevelTracking();
    }
    
    public SkillLevelCondition(Skill skill, int targetMinLevel, int targetMaxLevel) {
        super(skill); // Call parent constructor with skill
        targetMinLevel = Math.max(1, targetMinLevel);
        targetMaxLevel = Math.min(99, targetMaxLevel);
        this.currentTargetLevel = Rs2Random.between(targetMinLevel, targetMaxLevel);
        this.targetLevelMin = targetMinLevel;
        this.targetLevelMax = targetMaxLevel;
        initializeLevelTracking();
    }

    /**
     * Initialize level tracking for individual skill or all skills if total
     */
    private void initializeLevelTracking() {
        if (isTotal()) {
            Skill[] skills = getAllTrackableSkills();
            startLevelsBySkill = new int[skills.length];
            int totalLevel = 0;
            totalLevel =           
            startLevel = totalLevel;
        } else {
            startLevel = getCurrentLevel();
        }
    }

    @Override
    public void reset(boolean randomize) {
        if (randomize) {
            currentTargetLevel = Rs2Random.between(targetLevelMin, targetLevelMax);
        }
        initializeLevelTracking();
    }

    /**
     * Create a skill level condition with random target between min and max
     */
    public static SkillLevelCondition createRandomized(Skill skill, int minLevel, int maxLevel) {
        if (minLevel == maxLevel) {
            return new SkillLevelCondition(skill, minLevel);
        }
        
        return new SkillLevelCondition(skill, minLevel, maxLevel);
    }
    
    @Override
    public boolean isSatisfied() {
        return getCurrentLevel() >= currentTargetLevel;
    }
    
    /**
     * Gets the number of levels gained since condition was created
     */
    public int getLevelsGained() {
        return getCurrentLevel() - startLevel;
    }
    
    /**
     * Gets the number of levels remaining to reach target
     */
    public int getLevelsRemaining() {
        int currentLevel = getCurrentLevel();
        return Math.max(0, currentTargetLevel - currentLevel);
    }
    
    /**
     * Gets the current skill level or total level if this is a total skill condition
     */
    public int getCurrentLevel() {
        if (isTotal()) {
            return getTotalLevel();
        }
        return Microbot.getClientThread().runOnClientThreadOptional(
            () -> Microbot.getClient().getRealSkillLevel(skill)).orElse(0);
    }
    
    /**
     * Gets the total level across all skills
     */
    private int getTotalLevel() {
        int total = 0;
        Skill[] skills = getAllTrackableSkills();
        for (Skill s : skills) {
            if (s == null || s == Skill.OVERALL) continue;
            total += Microbot.getClientThread().runOnClientThreadOptional(
                () -> Microbot.getClient().getRealSkillLevel(s)).orElse(0);
        }
        return  Microbot.getClientThread().runOnClientThreadOptional(
            () -> Microbot.getClient().getTotalLevel()).orElse(0); 
    }
    
    /**
     * Gets the starting skill level
     */
    public int getStartingLevel() {
        return startLevel;
    }
    
    @Override
    public String getDescription() {
        int currentLevel = getCurrentLevel();
        int levelsNeeded = Math.max(0, currentTargetLevel - currentLevel);
        String randomRangeInfo = "";
        String skillName = isTotal() ? "Total" : skill.getName();
        
        if (targetLevelMin != targetLevelMax) {
            randomRangeInfo = String.format(" (randomized from %d-%d)", targetLevelMin, targetLevelMax);
        }
        
        if (levelsNeeded <= 0) {
            return String.format("%s level %d or higher%s (currently %d, goal reached)", 
                    skillName, currentTargetLevel, randomRangeInfo, currentLevel);
        } else {
            return String.format("%s level %d or higher%s (currently %d, need %d more)", 
                    skillName, currentTargetLevel, randomRangeInfo, currentLevel, levelsNeeded);
        }
    }
    
    @Override
    public ConditionType getType() {
        return ConditionType.SKILL_LEVEL;
    }

    @Override
    public double getProgressPercentage() {
        int currentLevel = getCurrentLevel();
        int startingLevel = getStartingLevel();
        int targetLevel = getCurrentTargetLevel();
        
        if (currentLevel >= targetLevel) {
            return 100.0;
        }
        
        int levelsGained = currentLevel - startingLevel;
        int levelsNeeded = targetLevel - startingLevel;
        
        if (levelsNeeded <= 0) {
            return 100.0;
        }
        
        return (100.0 * levelsGained) / levelsNeeded;
    }
}