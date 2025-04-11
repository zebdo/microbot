package net.runelite.client.plugins.microbot.pluginscheduler.condition.logical;

import java.time.ZonedDateTime;
import java.util.Optional;

import net.runelite.client.plugins.microbot.pluginscheduler.condition.Condition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.TimeCondition;

/**
 * Logical OR combination of conditions - any can be met.
 */
public class OrCondition extends LogicalCondition {
    public OrCondition(Condition... conditions) {
        super(conditions);

    }
    @Override
    public boolean isSatisfied() {
        if (conditions.isEmpty()) return true;
        return conditions.stream().anyMatch(Condition::isSatisfied);
    }
    
    @Override
    public String getDescription() {
        if (conditions.isEmpty()) {
            return "No conditions";
        }
        
        StringBuilder sb = new StringBuilder("ANY of: (");
        for (int i = 0; i < conditions.size(); i++) {
            if (i > 0) sb.append(" OR ");
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
     * Gets the next time this OR condition will be satisfied.
     * If any condition is satisfied, returns the trigger time of the first satisfied condition.
     * If no condition is satisfied, returns the earliest next trigger time among TimeConditions.
     * 
     * @return Optional containing the next trigger time, or empty if none available
     */
    @Override
    public Optional<ZonedDateTime> getCurrentTriggerTime() {
        if (conditions.isEmpty()) {
            return Optional.empty();
        }
      
        
        // If none satisfied, find earliest trigger time among TimeConditions
        ZonedDateTime earliestTimeSatisfied = null;
        ZonedDateTime earliestTimeUnSatisfied = null;
        int satisfiedCount = 0;
        for (Condition condition : conditions) {
            if (condition instanceof TimeCondition) {
                Optional<ZonedDateTime> nextTrigger = condition.getCurrentTriggerTime();
                if (condition.isSatisfied()) {
                    satisfiedCount++;
                    if (earliestTimeSatisfied == null || nextTrigger.get().isBefore(earliestTimeSatisfied)) {
                        earliestTimeSatisfied = nextTrigger.get();
                    }
                }else{
                    if (nextTrigger.isPresent()) {
                        ZonedDateTime triggerTime = nextTrigger.get();
                        if (earliestTimeUnSatisfied == null || triggerTime.isBefore(earliestTimeUnSatisfied)) {
                            earliestTimeUnSatisfied = triggerTime;
                        }
                    }
                }
            }
        }
        if (satisfiedCount > 0) {
            return earliestTimeSatisfied != null ? Optional.of(earliestTimeSatisfied) : Optional.empty();
        }else if (earliestTimeUnSatisfied != null) {
            return Optional.of(earliestTimeUnSatisfied);
        }else{
            return Optional.empty();
        }        
        
    }
}