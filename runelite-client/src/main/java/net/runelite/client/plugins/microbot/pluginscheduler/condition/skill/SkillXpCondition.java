package net.runelite.client.plugins.microbot.pluginscheduler.condition.skill;

import lombok.Getter;
import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.ConditionType;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;

/**
 * Skill XP-based condition for script execution.
 */
@Getter 
public class SkillXpCondition extends SkillCondition {
    private long currentTargetXp;
    private final long targetXpMin;
    private final long targetXpMax;
    private long startXp;
    private long[] startXpBySkill; // Used for total XP tracking
    
    public SkillXpCondition(Skill skill, long targetXp) {
        super(skill); // Call parent constructor with skill
        this.currentTargetXp = targetXp;
        this.targetXpMin = targetXp;
        this.targetXpMax = targetXp;
        initializeXpTracking();
    }
    
    public SkillXpCondition(Skill skill, long targetXpMin, long targetXpMax) {
        super(skill); // Call parent constructor with skill
        targetXpMin = Math.max(0, targetXpMin);
        targetXpMax = Math.min(Long.MAX_VALUE, targetXpMax);
        
        this.currentTargetXp = Rs2Random.between((int)targetXpMin, (int)targetXpMax);
        this.targetXpMin = targetXpMin;
        this.targetXpMax = targetXpMax;
        initializeXpTracking();
    }
    
    /**
     * Initialize XP tracking for individual skill or all skills if total
     */
    private void initializeXpTracking() {
        if (isTotal()) {
            Skill[] skills = getAllTrackableSkills();
            startXpBySkill = new long[skills.length];
            long totalXp = 0;
            totalXp = Microbot.getClientThread().runOnClientThreadOptional(  () -> {
                return Microbot.getClient().getOverallExperience();                               
            }).orElse(0L);
            startXp = (long)totalXp;
        } else {
            startXp = getCurrentXp();
        }
    }
    
    @Override
    public void reset(boolean randomize) {
        if (randomize) {
            currentTargetXp = (long)Rs2Random.between((int)targetXpMin, (int)targetXpMax);
        }
        initializeXpTracking();
    }
    
    /**
     * Create a skill XP condition with random target between min and max
     */
    public static SkillXpCondition createRandomized(Skill skill, long minXp, long maxXp) {
        if (minXp == maxXp) {
            return new SkillXpCondition(skill, minXp);
        }
        
        return new SkillXpCondition(skill, minXp, maxXp);
    }
    
    @Override
    public boolean isSatisfied() {
        return getXpGained() >= currentTargetXp;
    }
    
    /**
     * Gets the amount of XP gained since condition was created
     */
    public long getXpGained() {
        if (isTotal()) {
            return getTotalXp() - startXp;
        } else {
            long currentXp = Microbot.getClient().getSkillExperience(skill);
            return currentXp - startXp;
        }
    }
    
    /**
     * Gets total XP across all skills
     */
    private long getTotalXp() {
        if (!isTotal()) {
            return getCurrentXp();
        }
        
        long total = 0;
        total = Microbot.getClientThread().runOnClientThreadOptional(  () -> {
            return Microbot.getClient().getOverallExperience();                               
        }).orElse(0L);
        return total;
    }
    
    /**
     * Gets the amount of XP remaining to reach target
     */
    public long getXpRemaining() {
        return Math.max(0, currentTargetXp - getXpGained());
    }
    
    /**
     * Gets the current XP
     */
    public long getCurrentXp() {
        if (isTotal()) {
            return getTotalXp();
        }
        return (long)Microbot.getClientThread().runOnClientThreadOptional(
            () -> Microbot.getClient().getSkillExperience(skill)).orElse(0);
    }
    
    /**
     * Gets the starting XP
     */
    public long getStartingXp() {
        return startXp;
    }
    
    /**
     * Gets progress percentage towards target
     */
    @Override
    public double getProgressPercentage() {
        long xpGained = getXpGained();
        long targetXp = currentTargetXp;
        
        if (xpGained >= targetXp) {
            return 100.0;
        }
        
        if (targetXp <= 0) {
            return 100.0;
        }
        
        return (100.0 * xpGained) / targetXp;
    }
    
    @Override
    public String getDescription() {
        String skillName = isTotal() ? "Total" : skill.getName();
        return String.format("Gain %d %s XP (Current: %d/%d - %.1f%%)", 
            currentTargetXp, 
            skillName,
            getXpGained(),
            currentTargetXp,
            getProgressPercentage());
    }
    
    @Override
    public ConditionType getType() {
        return ConditionType.SKILL_XP;
    }
}