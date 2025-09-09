package net.runelite.client.plugins.microbot.pluginscheduler.tasks.requirements.requirement;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.pluginscheduler.tasks.requirements.enums.RequirementPriority;
import net.runelite.client.plugins.microbot.pluginscheduler.tasks.requirements.enums.RequirementType;
import net.runelite.client.plugins.microbot.pluginscheduler.tasks.requirements.enums.TaskContext;
import net.runelite.client.plugins.microbot.util.Rs2InventorySetup;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;
/**
 * Inventory setup requirement that loads a specific inventory setup using Rs2InventorySetup.
 * This allows plugins to use predefined inventory configurations instead of progressive equipment management.
 */
@Slf4j
public class InventorySetupRequirement extends Requirement {
    
    private final String inventorySetupName;
    private final boolean bankItemsNotInSetup;
    /**
     * Creates an inventory setup requirement.
     * 
     * @param inventorySetupName Name of the inventory setup to load
     * @param taskContext When to apply this requirement (PRE_SCHEDULE, POST_SCHEDULE, BOTH)
     * @param priority Priority level (MANDATORY, RECOMMENDED, OPTIONAL)
     * @param rating Effectiveness rating 1-10
     * @param description Human-readable description
     */
    public InventorySetupRequirement(String inventorySetupName, TaskContext taskContext, 
                                   RequirementPriority priority, int rating, String description, boolean bankItemsNotInSetup) {
        super(RequirementType.CUSTOM, priority, rating, description, Collections.emptyList(), taskContext);
        this.inventorySetupName = inventorySetupName;
        this.bankItemsNotInSetup = bankItemsNotInSetup;

    }
    
    @Override
    public String getName() {
        return "Inventory Setup: " + inventorySetupName;
    }
    
    @Override
    public boolean fulfillRequirement(CompletableFuture<Boolean> scheduledFuture) {
        try {
            if (inventorySetupName == null || inventorySetupName.trim().isEmpty()) {
                log.warn("Inventory setup name is empty, skipping requirement");
                return !isMandatory(); // fail only if mandatory
            }
            
            log.info("Loading inventory setup: {}", inventorySetupName);                                    
            // check if setup was successful by waiting for completion
            // rs2inventorysetup handles its own validation and timeout
            return execute(scheduledFuture);
            
        } catch (Exception e) {
            log.error("Failed to load inventory setup '{}': {}", inventorySetupName, e.getMessage());
            return !isMandatory(); // only fail if mandatory
        }
    }
    private boolean execute(CompletableFuture<Boolean> scheduledFuture){
        try {
            log.info("\n\t-Executing plan using Rs2InventorySetup approach: {}", inventorySetupName);
            
            // Convert CompletableFuture to ScheduledFuture (simplified conversion)
            ScheduledFuture<?> mainScheduler = new ScheduledFuture<Object>() {
                @Override
                public long getDelay(TimeUnit unit) { return 0; }
                @Override
                public int compareTo(Delayed o) { return 0; }
                @Override
                public boolean cancel(boolean mayInterruptIfRunning) { return scheduledFuture.cancel(mayInterruptIfRunning); }
                @Override
                public boolean isCancelled() { return scheduledFuture.isCancelled(); }
                @Override
                public boolean isDone() { return scheduledFuture.isDone(); }
                @Override
                public Object get() throws InterruptedException, ExecutionException { return scheduledFuture.get(); }
                @Override
                public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException { return scheduledFuture.get(timeout, unit); }
            };
            if (!Rs2InventorySetup.isInventorySetup(inventorySetupName)) {
                log.error("Failed to create Rs2InventorySetup");
                return false;
            }
            Rs2InventorySetup rs2Setup = new Rs2InventorySetup(inventorySetupName, mainScheduler);
         
          
            if(rs2Setup.doesEquipmentMatch() && rs2Setup.doesInventoryMatch()){
                log.info("Plan already matches current inventory and equipment setup, skipping execution");
                return true; // No need to execute if already matches
            }
            if (!Rs2Bank.isOpen()) {
                if (!Rs2Bank.walkToBankAndUseBank() && !Rs2Player.isInteracting() && !Rs2Player.isMoving()) {
                    log.error("\n\tFailed to open bank for comprehensive item management");                    
                }
                boolean openBank= sleepUntil(() -> Rs2Bank.isOpen(), 5000);
                if (!openBank) {
                    log.error("\n\tFailed to open bank within timeout period,for invntory setup execution \"{}\"", inventorySetupName);
                    return false;
                }
            }
            
            // Bank items not in setup first if requested (excludes teleport items)
            if (bankItemsNotInSetup) {
                log.info("Banking items not in setup (excluding teleport items) before setting up: {}", inventorySetupName);
                if (!rs2Setup.bankAllItemsNotInSetup(true)) {
                    log.warn("Failed to bank all items not in setup, continuing with setup anyway");
                }
            }
            // Use existing Rs2InventorySetup methods to fulfill the requirements
            boolean equipmentSuccess = rs2Setup.loadEquipment();
            if (!equipmentSuccess) {
                log.error("Failed to load equipment using Rs2InventorySetup");
                return false;
            }
            
            boolean inventorySuccess = rs2Setup.loadInventory();
            if (!inventorySuccess) {
                log.error("Failed to load inventory using Rs2InventorySetup");
                return false;
            }
            
            // Verify the setup matches
            boolean equipmentMatches = rs2Setup.doesEquipmentMatch();
            boolean inventoryMatches = rs2Setup.doesInventoryMatch();
            
            if (equipmentMatches && inventoryMatches) {
                log.info("Successfully executed plan using Rs2InventorySetup: {}", inventorySetupName);
                return true;
            } else {
                log.warn("Plan execution completed but setup verification failed. Equipment matches: {}, Inventory matches: {}", 
                        equipmentMatches, inventoryMatches);
                return false;
            }
            
        } catch (Exception e) {
            log.error("Failed to execute plan using Rs2InventorySetup: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean isFulfilled() {
        // inventory setup is more of an action than a state to check
        // we consider it fulfilled if the setup name is valid
        return inventorySetupName != null && !inventorySetupName.trim().isEmpty();
    }
    
    @Override
    public String getUniqueIdentifier() {
        return String.format("%s:INVENTORY_SETUP:%s", 
                requirementType.name(), 
                inventorySetupName != null ? inventorySetupName : "null");
    }
    
    /**
     * Gets the inventory setup name for this requirement.
     * 
     * @return The inventory setup name
     */
    public String getInventorySetupName() {
        return inventorySetupName;
    }
}