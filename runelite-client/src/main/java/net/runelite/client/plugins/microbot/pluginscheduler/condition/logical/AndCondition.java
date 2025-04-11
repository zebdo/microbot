package net.runelite.client.plugins.microbot.pluginscheduler.condition.logical;

import java.time.ZonedDateTime;
import java.util.Optional;

import net.runelite.client.plugins.microbot.pluginscheduler.condition.Condition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.TimeCondition;

/**
 * Logical AND combination of conditions - all must be met.
 */
public class AndCondition extends LogicalCondition {
    @Override
    public boolean isSatisfied() {
        if (conditions.isEmpty()) return true;
        return conditions.stream().allMatch(Condition::isSatisfied);
    }
    
    @Override
    public String getDescription() {
        if (conditions.isEmpty()) {
            return "No conditions";
        }
        
        StringBuilder sb = new StringBuilder("ALL of: (");
        for (int i = 0; i < conditions.size(); i++) {
            if (i > 0) sb.append(" AND ");
            sb.append(conditions.get(i).getDescription());
        }
        sb.append(")");
        return sb.toString();
    }
    @Override
    public void reset() {
        for (Condition condition : conditions) {
            condition.reset();
        }
    }
    @Override
    public void reset(boolean randomize) {
        for (Condition condition : conditions) {
            condition.reset(randomize);
        }
        
    }

    /**
     * Gets the next time this AND condition will be satisfied.
     * If all conditions are satisfied, returns the minimum trigger time among all conditions.
     * If any condition is not satisfied, returns the maximum trigger time among unsatisfied TimeConditions.
     * 
     * @return Optional containing the next trigger time, or empty if none available
     */
    @Override
    public Optional<ZonedDateTime> getCurrentTriggerTime() {
        if (conditions.isEmpty()) {
            return Optional.empty();
        }
        
        boolean allSatisfied = true;
        ZonedDateTime minTriggerTime = null;
        ZonedDateTime maxUnsatisfiedTimeConditionTriggerTime = null;
        
        // Check if all conditions are satisfied and track min trigger time
        for (Condition condition : conditions) {
            // Check if this condition is satisfied
            if (!condition.isSatisfied()) {
                allSatisfied = false;
                
                // For unsatisfied TimeConditions, track the maximum trigger time
                if (condition instanceof TimeCondition) {
                    Optional<ZonedDateTime> nextTrigger = condition.getCurrentTriggerTime();
                    if (nextTrigger.isPresent()) {
                        ZonedDateTime triggerTime = nextTrigger.get();
                        if (maxUnsatisfiedTimeConditionTriggerTime == null || 
                            triggerTime.isAfter(maxUnsatisfiedTimeConditionTriggerTime)) {
                            maxUnsatisfiedTimeConditionTriggerTime = triggerTime;
                        }
                    }
                }
            } 
            // If satisfied, track the minimum trigger time
            else {
                Optional<ZonedDateTime> triggerTime = condition.getCurrentTriggerTime();
                if (triggerTime.isPresent()) {
                    if (minTriggerTime == null || triggerTime.get().isBefore(minTriggerTime)) {
                        minTriggerTime = triggerTime.get();
                    }
                }
            }
        }
        
        // If all conditions are satisfied, return the minimum trigger time
        if (allSatisfied) {
            return minTriggerTime != null ? Optional.of(minTriggerTime) : Optional.empty();
        }
        
        // If at least one condition is not satisfied, return the maximum trigger time
        // of unsatisfied TimeConditions
        return maxUnsatisfiedTimeConditionTriggerTime != null ? 
            Optional.of(maxUnsatisfiedTimeConditionTriggerTime) : Optional.empty();
    }
}