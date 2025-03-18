package net.runelite.client.plugins.microbot.zerozero.bluedragons;

import lombok.Getter;
import net.runelite.api.ItemID;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.grounditem.LootingParameters;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.RunePouch;
import net.runelite.client.plugins.microbot.util.misc.Rs2Food;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BlueDragonsScript extends Script {

    public static BlueDragonState currentState;
    String lastChatMessage = "";
    private BlueDragonsConfig config;
    public static final WorldPoint SAFE_SPOT = new WorldPoint(2918, 9781, 0);
    @Getter
    private Integer currentTargetId = null;
    
    @Inject
    private BlueDragonsOverlay overlay;
    
    // Add a timestamp field to track when we last logged a loot message
    private long lastLootMessageTime = 0;


    private boolean isInventoryFull() {
        boolean simpleFull = Rs2Inventory.isFull();
        if (!simpleFull) {
            return Rs2Inventory.getEmptySlots() <= 0;
        }
        
        return true;
    }

    public boolean run(BlueDragonsConfig config) {
        this.config = config;
        currentState = BlueDragonState.STARTING;
        
        // Reset overlay stats when starting
        if (overlay != null) {
            overlay.resetStats();
            overlay.setScript(this);
            overlay.setConfig(config);
        }
        
        // Track consecutive errors for safety shutdown
        final int[] consecutiveErrors = {0};
        final int MAX_CONSECUTIVE_ERRORS = 5;
        
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!super.run() || !Microbot.isLoggedIn()) return;

                // Global safety check - if inventory is full and we're not already banking, switch to banking state
                if (isInventoryFull() && currentState != BlueDragonState.BANKING && currentState != BlueDragonState.STARTING) {
                    logOnceToChat("Global safety: Inventory is full. Switching to BANKING state.", false, config);
                    currentState = BlueDragonState.BANKING;
                }

                switch (currentState) {
                    case STARTING:
                        determineStartingState(config);
                        break;

                    case BANKING:
                        handleBanking(config);
                        break;

                    case TRAVEL_TO_DRAGONS:
                        handleTravelToDragons();
                        break;

                    case FIGHTING:
                        handleFighting(config);
                        break;
                        
                    case LOOTING:
                        handleLooting(config);
                        break;
                }
                
                // Reset consecutive errors counter on successful execution
                consecutiveErrors[0] = 0;
                
            } catch (Exception ex) {
                consecutiveErrors[0]++;
                logOnceToChat("Error in Blue Dragons script: " + ex.getMessage(), false, config);
                
                // Log stack trace to debug output
                if (config.debugLogs()) {
                    StringBuilder stackTrace = new StringBuilder();
                    for (StackTraceElement element : ex.getStackTrace()) {
                        stackTrace.append(element.toString()).append("\n");
                    }
                    logOnceToChat("Stack trace: " + stackTrace.toString(), true, config);
                }
                
                // Safety measure - if too many consecutive errors, stop the script
                if (consecutiveErrors[0] >= MAX_CONSECUTIVE_ERRORS) {
                    logOnceToChat("Too many consecutive errors. Stopping script for safety.", false, config);
                    shutdown();
                }
                
                // Recovery logic based on current state
                if (Rs2Player.isInCombat()) {
                    logOnceToChat("In combat during error - attempting to eat food", true, config);
                    Rs2Player.eatAt(config.eatAtHealthPercent());
                }
                
                // Wait longer between retries if errors are occurring
                sleep(1000 * Math.min(consecutiveErrors[0], 5));
            }
        }, 0, 100, TimeUnit.MILLISECONDS);

        return true;
    }

    private void handleBanking(BlueDragonsConfig config) {
        logOnceToChat("Traveling to Falador West bank for depositing looted items.", true, config);
        logOnceToChat("Current location: " + Microbot.getClient().getLocalPlayer().getWorldLocation(), true, config);

        if (Rs2Bank.walkToBankAndUseBank(BankLocation.FALADOR_WEST)) {
            logOnceToChat("Opened bank. Depositing loot.", true, config);
            Rs2Bank.depositAll("Dragon bones");
            Rs2Bank.depositAll("Dragon spear");
            Rs2Bank.depositAll("Shield left half");
            Rs2Bank.depositAll("Scaly blue dragonhide");

            if (config.lootEnsouledHead()) {
                Rs2Bank.depositAll("Ensouled dragon head");
            }

            if (config.lootDragonhide()) {
                Rs2Bank.depositAll("Blue dragonhide");
            }
            logOnceToChat("Withdrawing food for combat.", true, config);
            withdrawFood(config);
            Rs2Bank.closeBank();
            logOnceToChat("Banking complete. Transitioning to travel state.", true, config);
            currentState = BlueDragonState.TRAVEL_TO_DRAGONS;
        }
        else {
            logOnceToChat("Failed to reach the bank.", true, config);

        }
    }

    private void determineStartingState(BlueDragonsConfig config) {
        boolean hasTeleport = hasTeleportToFalador();
        boolean hasAgilityOrKey = Microbot.getClient().getRealSkillLevel(Skill.AGILITY) >= 70 || hasDustyKey();

        if (!hasTeleport) {
            logOnceToChat("Missing teleport to Falador or required runes.", false, config);
        }

        if (!hasAgilityOrKey) {
            logOnceToChat("Requires Agility level 70 or a Dusty Key.", false, config);
        }

        // Check if all requirements are met
        if (hasTeleport && hasAgilityOrKey) {
            currentState = BlueDragonState.BANKING;
        } else {
            logOnceToChat("Starting conditions not met. Stopping the plugin.", false, config);
            shutdown();
        }
    }

    private boolean hasTeleportToFalador() {
        logOnceToChat("Checking for Falador teleport or required runes.", true, config);

        if (Rs2Inventory.contains("Falador teleport")) {
            logOnceToChat("Found Falador teleport in inventory.", true, config);
            return true;
        }

        int lawRuneId = ItemID.LAW_RUNE;
        int waterRuneId = ItemID.WATER_RUNE;
        int dustRuneId = ItemID.DUST_RUNE;
        int airRuneId = ItemID.AIR_RUNE;

        int requiredLawRunes = 1;
        int requiredAirRunes = 3;
        int requiredWaterRunes = 1;

        boolean runePouchInInventory = Rs2Inventory.contains("Rune pouch") || Rs2Inventory.contains("Divine rune pouch");
        boolean hasLawRunes = checkRuneAvailability(lawRuneId, requiredLawRunes, runePouchInInventory);
        boolean hasWaterRunes = checkRuneAvailability(waterRuneId, requiredWaterRunes, runePouchInInventory);
        boolean hasAirOrDustRunes = checkRuneAvailability(dustRuneId, requiredAirRunes, runePouchInInventory) ||
                checkRuneAvailability(airRuneId, requiredAirRunes, runePouchInInventory);

        return hasLawRunes && hasWaterRunes && hasAirOrDustRunes;
    }

    private boolean checkRuneAvailability(int runeId, int requiredAmount, boolean checkRunePouch) {
        boolean inInventory = Rs2Inventory.hasItemAmount(runeId, requiredAmount);
        boolean inRunePouch = checkRunePouch && RunePouch.contains(runeId, requiredAmount);
        return inInventory || inRunePouch;
    }

    private boolean hasDustyKey() {
        return Rs2Inventory.contains("Dusty key");
    }

    private void handleTravelToDragons() {
        logOnceToChat("Traveling to dragons.", false, config);
        logOnceToChat("Player location before travel: " + Microbot.getClient().getLocalPlayer().getWorldLocation(), true, config);

        // Check if we're already at the safe spot
        if (isPlayerAtSafeSpot()) {
            logOnceToChat("Already at safe spot. Transitioning to FIGHTING state.", true, config);
            currentState = BlueDragonState.FIGHTING;
            return;
        }

        // Try to walk to the safe spot
        boolean walkAttemptSuccessful = Rs2Walker.walkTo(SAFE_SPOT);
        
        if (!walkAttemptSuccessful) {
            logOnceToChat("Failed to start walking to safe spot. Will retry next tick.", true, config);
            return;
        }
        
        // Wait for the player to get close to the safe spot
        boolean reachedNearSafeSpot = sleepUntil(() -> Rs2Player.distanceTo(SAFE_SPOT) <= 5, 60000); // 1 minute timeout
        
        // If we're close but not exactly at the safe spot, use moveToSafeSpot for final approach
        if (reachedNearSafeSpot || Rs2Player.distanceTo(SAFE_SPOT) <= 20) {
            logOnceToChat("Close to safe spot. Using precise movement for final approach.", true, config);
            moveToSafeSpot();
        } else {
            logOnceToChat("Failed to get close to safe spot within timeout.", true, config);
            
            // Only continue if we're somewhere reasonably close
            int distance = Rs2Player.distanceTo(SAFE_SPOT);
            logOnceToChat("Current distance to safe spot: " + distance, true, config);
            
            if (distance < 50) {
                moveToSafeSpot();
            } else {
                logOnceToChat("Too far from safe spot. Returning to banking state to try again.", true, config);
                currentState = BlueDragonState.BANKING;
                return;
            }
        }

        if (hopIfPlayerAtSafeSpot()) {
            logOnceToChat("Hopped worlds due to player detection at safe spot.", true, config);
            return;
        }
        
        // Double-check we actually reached the safe spot
        if (isPlayerAtSafeSpot()) {
            logOnceToChat("Reached safe spot. Transitioning to FIGHTING state.", true, config);
            currentState = BlueDragonState.FIGHTING;
        } else {
            logOnceToChat("Still not at safe spot after multiple attempts. Will continue from current position.", true, config);
            currentState = BlueDragonState.FIGHTING;
        }
    }

    private void handleFighting(BlueDragonsConfig config) {
        if (currentState != BlueDragonState.FIGHTING) {
            logOnceToChat("Not in FIGHTING state but handleFighting was called. Current state: " + currentState, true, config);
            return;
        }
        
        // Check if inventory is full first - switch to banking if needed
        if (isInventoryFull()) {
            logOnceToChat("Inventory is full. Switching to BANKING state.", false, config);
            currentState = BlueDragonState.BANKING;
            return;
        }
        
        // Check if there are items to loot first - looting takes priority
        if (checkForLoot()) {
            // Only log the loot message at most once every 10 seconds
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastLootMessageTime > 10000) {
                logOnceToChat("Found loot on the ground. Switching to LOOTING state.", false, config);
                lastLootMessageTime = currentTime;
            }
            currentState = BlueDragonState.LOOTING;
            return;
        }
        
        // Make sure player is at the safe spot before attacking
        if (!isPlayerAtSafeSpot()) {
            logOnceToChat("Not at safe spot. Moving back before continuing to fight.", true, config);
            moveToSafeSpot();
            return;
        }

        Rs2Player.eatAt(config.eatAtHealthPercent());

        // Try to attack a dragon if not in combat
        if (!underAttack()) {
            // If we're not in combat, look for a new dragon to attack
            NPC dragon = getAvailableDragon();
            if (dragon != null) {
                logOnceToChat("Found available dragon. Attacking.", true, config);
                if (attackDragon(dragon)) {
                    currentTargetId = dragon.getId();
                }
            } else {
                logOnceToChat("No dragons available to attack.", true, config);
            }
        } else {
            // We're in combat, just make sure we're at the safe spot
            if (!isPlayerAtSafeSpot()) {
                moveToSafeSpot();
            }
        }
    }
    
    private boolean checkForLoot() {
        // Check for any valuable loot items nearby that belong to the player
        String[] lootItems = {
            "Dragon bones", "Blue dragonhide", "Ensouled dragon head",
            "Dragon spear", "Shield left half", "Scaly blue dragonhide"
        };
        
        // Create parameters that will check only for player-owned items
        LootingParameters params = new LootingParameters(15, 1, 1, 0, false, true, lootItems);
        
        // Check if there are any items matching our criteria
        return Rs2GroundItem.lootItemsBasedOnNames(params);
    }
    
    private void handleLooting(BlueDragonsConfig config) {
        if (currentState != BlueDragonState.LOOTING) {
            return;
        }
        
        // Check if inventory is full before attempting to loot
        if (isInventoryFull()) {
            logOnceToChat("Inventory is full, switching to BANKING state.", false, config);
            currentState = BlueDragonState.BANKING;
            return;
        }
        
        // Try to loot all valuable items
        boolean lootedAnything = false;
        
        // Check if we're full again before attempting each loot
        // This covers cases where we became full during the looting process
        if (!isInventoryFull()) {
            lootedAnything |= lootItem("Dragon bones");
        } 
        if (!isInventoryFull()) {
            lootedAnything |= lootItem("Dragon spear");
        }
        if (!isInventoryFull()) {
            lootedAnything |= lootItem("Shield left half");
        }
        if (!isInventoryFull()) {
            lootedAnything |= lootItem("Scaly blue dragonhide");
        }
        
        if (config.lootDragonhide() && !isInventoryFull()) {
            lootedAnything |= lootItem("Blue dragonhide");
        }
        
        if (config.lootEnsouledHead() && !isInventoryFull()) {
            lootedAnything |= lootItem("Ensouled dragon head");
        }
        
        // Wait a bit to ensure we've completely finished looting
        sleep(300, 500);
        
        // Final inventory check - if we're full after looting, go to banking
        if (isInventoryFull()) {
            logOnceToChat("Inventory is full after looting, switching to BANKING state.", false, config);
            currentState = BlueDragonState.BANKING;
            return;
        }
        
        // Check if there's still loot to pick up
        if (!checkForLoot() || !lootedAnything) {
            // Return to fighting regardless of combat state
            // We've already established we're in the right area
            logOnceToChat("Finished looting. Returning to combat.", true, config);
            currentState = BlueDragonState.FIGHTING;
            
            // Reset currentTargetId as we're no longer targeting any dragon
            currentTargetId = null;
        }
    }
    
    private boolean lootItem(String itemName) {
        if (!isInventoryFull()) {
            // Use antiLureProtection=true to only loot items that belong to the player
            LootingParameters params = new LootingParameters(15, 1, 1, 0, false, true, itemName);
            boolean looted = Rs2GroundItem.lootItemsBasedOnNames(params);
            if (looted) {
                logOnceToChat("Looted: " + itemName, true, config);
                
                // Update static counters directly
                if (itemName.equalsIgnoreCase("Dragon bones")) {
                    BlueDragonsOverlay.bonesCollected++;
                    Microbot.log("Bones looted: " + BlueDragonsOverlay.bonesCollected);
                } else if (itemName.equalsIgnoreCase("Blue dragonhide")) {
                    BlueDragonsOverlay.hidesCollected++;
                }
                
                return true;
            }
        }
        return false;
    }

    private void withdrawFood(BlueDragonsConfig config) {
        Rs2Food food = config.foodType();
        int requiredAmount = config.foodAmount();

        if (food == null || requiredAmount <= 0) {
            logOnceToChat("Invalid food type or amount in configuration.", true, config);
            return;
        }

        int currentFoodInInventory = Rs2Inventory.count(food.getName());
        int deficit = requiredAmount - currentFoodInInventory;

        if (deficit <= 0) {
            logOnceToChat("Inventory already contains the required amount of " + food.getName() + ".", true, config);
            return;
        }

        if (!Rs2Bank.isOpen()) {
            logOnceToChat("Bank is not open. Cannot withdraw food.", true, config);
            return;
        }

        boolean bankLoaded = sleepUntil(() -> !Rs2Bank.bankItems().isEmpty(), 5000);
        if (!bankLoaded) {
            logOnceToChat("Bank items did not load in time.", true, config);
            return;
        }

        if (!Rs2Bank.hasItem(food.getName())) {
            logOnceToChat(food.getName() + " not found in the bank. Stopping script.", true, config);
            shutdown();
            return;
        }

        logOnceToChat("Attempting to withdraw " + deficit + "x " + food.getName(), false, config);

        int retryCount = 0;
        final int maxRetries = 3;
        boolean success = false;

        while (retryCount < maxRetries && !success) {
            success = Rs2Bank.withdrawX(false, food.getName(), deficit, true);
            if (!success) {
                retryCount++;
                sleep(500);
                logOnceToChat("Retrying withdrawal of " + food.getName() + " (" + retryCount + ")", true, config);
            }
        }

        if (!success) {
            logOnceToChat("Unable to withdraw " + food.getName() + " after multiple attempts. Stopping script.", true, config);
            shutdown();
        } else {
            logOnceToChat("Successfully withdrew " + deficit + "x " + food.getName(), false, config);
        }
    }

    private NPC getAvailableDragon() {
        NPC dragon = Rs2Npc.getNpc("Blue dragon");
        logOnceToChat("Found dragon: " + (dragon != null ? "Yes (ID: " + dragon.getId() + ")" : "No"), true, config);
        
        if (dragon != null) {
            boolean correctId = (dragon.getId() == 265 || dragon.getId() == 266);
            logOnceToChat("Dragon has correct ID (265 or 266): " + correctId, true, config);
            
            boolean hasLineOfSight = Rs2Npc.hasLineOfSight(new Rs2NpcModel(dragon));
            logOnceToChat("Has line of sight to dragon: " + hasLineOfSight, true, config);
            
            // Don't check if dragon is interacting - we want to attack regardless
            if (correctId && hasLineOfSight) {
                return dragon;
            }
        }
        return null;
    }

    private boolean attackDragon(NPC dragon) {
        final int dragonId = dragon.getId();
        
        // Only check for actual combat, not targeting
        if (Rs2Combat.inCombat() && dragon.getInteracting() != Microbot.getClient().getLocalPlayer()) {
            logOnceToChat("Cannot attack dragon - player is in combat with different target.", true, config);
            return false;
        }
        
        if (Rs2Npc.attack(dragon)) {
            // Wait for dragon to die
            boolean dragonKilled = sleepUntil(() -> Rs2Npc.getNpc(dragonId) == null, 60000);
            
            if (dragonKilled) {
                logOnceToChat("Dragon killed. Transitioning to looting state.", true, config);
                // Update kill count directly
                BlueDragonsOverlay.dragonKillCount++;
                
                // Give a slight delay for loot to appear
                sleep(600, 900);
                currentState = BlueDragonState.LOOTING;
            }
            
            return true;
        }
        return false;
    }

    private boolean isPlayerAtSafeSpot() {
        return SAFE_SPOT.equals(Microbot.getClient().getLocalPlayer().getWorldLocation());
    }

    private void moveToSafeSpot() {
        Microbot.pauseAllScripts = true;
        
        // Check if player is close enough to use walkFastCanvas
        int distance = Rs2Player.distanceTo(SAFE_SPOT);
        
        logOnceToChat("Moving to safe spot. Distance: " + distance, true, config);
        
        // Always try walkTo first for longer distances
        if (distance > 15) {
            logOnceToChat("Using walkTo to approach safe spot", true, config);
            Rs2Walker.walkTo(SAFE_SPOT);
            
            // Wait for the player to get closer to the safe spot
            sleepUntil(() -> Rs2Player.distanceTo(SAFE_SPOT) <= 5, 30000);
        }
        
        // For final approach or if already close, use walkFastCanvas
        if (!isPlayerAtSafeSpot()) {
            logOnceToChat("Using walkFastCanvas for final approach to safe spot", true, config);
            Rs2Walker.walkFastCanvas(SAFE_SPOT);
            sleepUntil(this::isPlayerAtSafeSpot, 15000);
        }

        if (hopIfPlayerAtSafeSpot()) {
            return;
        }

        // Double check if we're at the safe spot
        if (!isPlayerAtSafeSpot()) {
            logOnceToChat("Failed to reach exact safe spot. Will continue with current position.", true, config);
        } else {
            logOnceToChat("Successfully reached safe spot.", true, config);
        }

        Microbot.pauseAllScripts = false;
    }

    private boolean hopIfPlayerAtSafeSpot() {
        // Check if any other players are near our safe spot
        boolean otherPlayersAtSafeSpot = false;
        List<Player> players = Rs2Player.getPlayers();
        
        for (Player player : players) {
            if (player != null && 
                !player.equals(Microbot.getClient().getLocalPlayer()) && 
                player.getWorldLocation().distanceTo(SAFE_SPOT) <= 1) {
                otherPlayersAtSafeSpot = true;
                break;
            }
        }
                
        // If there are other players directly on our safe spot
        if (otherPlayersAtSafeSpot) {
            logOnceToChat("Player detected at safe spot. Pausing script and hopping worlds.", false, config);
            Microbot.pauseAllScripts = true;
            
            // Try to hop to a new world
            boolean hopSuccess = Microbot.hopToWorld(findRandomWorld());
            sleep(5000); // Give time to process the hop
            
            Microbot.pauseAllScripts = false;
            return hopSuccess;
        }
        
        return false;
    }
    
    private int findRandomWorld() {
        int currentWorld = Microbot.getClient().getWorld();
        // Standard member worlds range
        int minWorld = 302;
        int maxWorld = 580;
        
        int targetWorld;
        do {
            targetWorld = minWorld + new java.util.Random().nextInt(maxWorld - minWorld);
        } while (targetWorld == currentWorld);
        
        return targetWorld;
    }

    void logOnceToChat(String message, boolean isDebug, BlueDragonsConfig config) {
        if (message == null || message.trim().isEmpty()) {
            message = "Unknown log message (null or empty)...";
        }
        if (isDebug && (config == null || !config.debugLogs())) {
            return;
        }
        if (!message.equals(lastChatMessage)) {
            Microbot.log(message);
            lastChatMessage = message;
        }
    }

    public void updateConfig(BlueDragonsConfig config) {
        logOnceToChat("Applying new configuration to Blue Dragons script.", true, config);
        this.config = config;
        
        // Update overlay config
        if (overlay != null) {
            overlay.setConfig(config);
        }
        
        withdrawFood(config);
    }

    public void shutdown() {
        super.shutdown();
        Rs2Walker.disableTeleports = false;
        if (overlay != null) {
            overlay.resetStats();
        }
        currentState = BlueDragonState.STARTING;
        currentTargetId = null;
    }

    private boolean underAttack() {
        // Only consider ourselves under attack if we're actually in combat
        // Ignore dragons targeting us but not in combat yet
        return Rs2Combat.inCombat();
    }
}