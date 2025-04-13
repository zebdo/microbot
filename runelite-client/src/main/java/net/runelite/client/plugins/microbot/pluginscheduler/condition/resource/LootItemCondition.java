package net.runelite.client.plugins.microbot.pluginscheduler.condition.resource;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.TileItem;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;

import net.runelite.client.plugins.grounditems.GroundItem;
import net.runelite.client.plugins.grounditems.GroundItemsPlugin;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.logical.AndCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.logical.LogicalCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.logical.OrCondition;


import net.runelite.client.plugins.microbot.util.math.Rs2Random;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.runelite.api.TileItem.OWNERSHIP_SELF;

import static net.runelite.api.TileItem.OWNERSHIP_GROUP;
import static net.runelite.api.TileItem.OWNERSHIP_OTHER;
import static net.runelite.api.TileItem.OWNERSHIP_NONE;

/**
 * Condition that tracks a specific looted item quantity.
 * Focuses on tracking a single item for clarity and precision.
 */
@Getter
@Slf4j
public class LootItemCondition extends ResourceCondition {
    private final String itemName;
    private final Pattern itemPattern;    
    private final int targetAmountMin;
    private final int targetAmountMax;
    private final boolean includeNoted;
    private final boolean includeNoneOwner;
    
    private int currentTargetAmount;
    private int currentTrackedCount;
    private int lastInventoryCount;
    
    // Track ground items that belong to the player
    private final Set<WorldPoint> trackedItemLocations = new HashSet<>();
    private final Map<WorldPoint, Integer> trackedItemQuantities = new HashMap<>();
    
    // Keep track of recently looted items to avoid double counting
    private final Map<WorldPoint, Long> recentlyLootedItems = new HashMap<>();

    public LootItemCondition(String itemName, int targetAmount, boolean includeNoted) {
        this.itemName = itemName;
        this.targetAmountMin = targetAmount;
        this.targetAmountMax = targetAmount;        
        this.includeNoted = includeNoted;
        this.itemPattern = createItemPattern(itemName);
        this.currentTargetAmount = Rs2Random.between(targetAmountMin, targetAmountMax);
        this.currentTrackedCount = 0;
        this.lastInventoryCount = 0;
        this.includeNoneOwner = false;
        this.isIncludeNoneOwner();
    }
    
    @Builder
    public LootItemCondition(String itemName, int targetAmountMin, 
                            int targetAmountMax, 
                            boolean includeNoted, 
                            boolean includeNoneOwner) {
        this.itemName = itemName;
        this.targetAmountMin = targetAmountMin;
        this.targetAmountMax = targetAmountMax;        
        this.itemPattern = createItemPattern(itemName);
        this.currentTargetAmount = Rs2Random.between(targetAmountMin, targetAmountMax);
        this.currentTrackedCount = 0;
        this.lastInventoryCount = 0;
        this.includeNoted = includeNoted;
        this.includeNoneOwner = includeNoneOwner;
    }
    
    /**
     * Creates a condition with randomized target between min and max
     */
    public static LootItemCondition createRandomized(String itemName, int minAmount, int maxAmount, boolean includeNoted, boolean includeNoneOwner) {   
        return LootItemCondition.builder()
                .itemName(itemName)
                .targetAmountMin(minAmount)
                .targetAmountMax(maxAmount)
                .includeNoted(includeNoted)
                .includeNoneOwner(includeNoneOwner)
                .build();
    }


    /**
     * Creates an AND logical condition requiring multiple looted items with individual targets
     */
    public static LogicalCondition createAndCondition(List<String> itemNames, List<Integer> targetAmountsMins, List<Integer> targetAmountsMaxs,boolean includeNoted, boolean includeNoneOwner) {
        // Validate input
        if (itemNames == null || itemNames.isEmpty()) {
            throw new IllegalArgumentException("Item name list cannot be null or empty");
        }
        
        // Determine the smallest list size for safe iteration
        int minSize = Math.min(itemNames.size(), 
                      Math.min(targetAmountsMins != null ? targetAmountsMins.size() : 0,
                               targetAmountsMaxs != null ? targetAmountsMaxs.size() : 0));
        
        // If target amounts not provided or empty, default to single item each
        if (targetAmountsMins == null || targetAmountsMins.isEmpty()) {
            targetAmountsMins = new ArrayList<>(itemNames.size());
            for (int i = 0; i < itemNames.size(); i++) {
                targetAmountsMins.add(1);
            }
        }
        
        if (targetAmountsMaxs == null || targetAmountsMaxs.isEmpty()) {
            targetAmountsMaxs = new ArrayList<>(targetAmountsMins);
        }
        
        // Create the logical condition
        AndCondition andCondition = new AndCondition();
        
        // Add a condition for each item
        for (int i = 0; i < minSize; i++) {
            LootItemCondition itemCondition = LootItemCondition.builder()
                    .itemName(itemNames.get(i))
                    .targetAmountMin(targetAmountsMins.get(i))
                    .targetAmountMax(targetAmountsMaxs.get(i))
                    .includeNoted(includeNoted)
                    .includeNoneOwner(includeNoneOwner)
                    .build();
                    
            andCondition.addCondition(itemCondition);
        }
        
        return andCondition;
    }

    /**
     * Creates an OR logical condition requiring multiple looted items with individual targets
     */
    public static LogicalCondition createOrCondition(List<String> itemNames, 
                        List<Integer> targetAmountsMins, List<Integer> targetAmountsMaxs,
                         boolean includeNoted, boolean includeNoneOwner) {
        if (itemNames == null || itemNames.isEmpty()) {
            throw new IllegalArgumentException("Item name list cannot be null or empty");
        }
        
        // Determine the smallest list size for safe iteration
        int minSize = Math.min(itemNames.size(), 
                      Math.min(targetAmountsMins != null ? targetAmountsMins.size() : 0,
                               targetAmountsMaxs != null ? targetAmountsMaxs.size() : 0));
        
        // If target amounts not provided or empty, default to single item each
        if (targetAmountsMins == null || targetAmountsMins.isEmpty()) {
            targetAmountsMins = new ArrayList<>(itemNames.size());
            for (int i = 0; i < itemNames.size(); i++) {
                targetAmountsMins.add(1);
            }
        }
        
        if (targetAmountsMaxs == null || targetAmountsMaxs.isEmpty()) {
            targetAmountsMaxs = new ArrayList<>(targetAmountsMins);
        }
        
        // Create the logical condition
        OrCondition orCondition = new OrCondition();
        
        // Add a condition for each item
        for (int i = 0; i < minSize; i++) {
            LootItemCondition itemCondition = LootItemCondition.builder()
                    .itemName(itemNames.get(i))
                    .targetAmountMin(targetAmountsMins.get(i))
                    .targetAmountMax(targetAmountsMaxs.get(i))
                    .includeNoted(includeNoted)
                    .includeNoneOwner(includeNoneOwner)
                    .build();
                    
            orCondition.addCondition(itemCondition);
        }
        
        return orCondition;
    }

    /**
     * Creates an AND logical condition requiring multiple looted items with the same target for all
     */
    public static LogicalCondition createAndCondition(List<String> itemNames, int targetAmountMin, int targetAmountMax,boolean includeNoted, boolean includeNoneOwner) {
        if (itemNames == null || itemNames.isEmpty()) {
            throw new IllegalArgumentException("Item name list cannot be null or empty");
        }
        
        // Create the logical condition
        AndCondition andCondition = new AndCondition();
        
        // Add a condition for each item with the same targets
        for (String itemName : itemNames) {
            LootItemCondition itemCondition = LootItemCondition.builder()
                    .itemName(itemName)
                    .targetAmountMin(targetAmountMin)
                    .targetAmountMax(targetAmountMax)
                    .includeNoted(includeNoted)
                    .includeNoneOwner(includeNoneOwner)
                    .build();
                    
            andCondition.addCondition(itemCondition);
        }
        
        return andCondition;
    }

    /**
     * Creates an OR logical condition requiring multiple looted items with the same target for all
     */
    public static LogicalCondition createOrCondition(List<String> itemNames, int targetAmountMin, int targetAmountMax,boolean includeNoted, boolean includeNoneOwner) {
        if (itemNames == null || itemNames.isEmpty()) {
            throw new IllegalArgumentException("Item name list cannot be null or empty");
        }
        
        // Create the logical condition
        OrCondition orCondition = new OrCondition();
        
        // Add a condition for each item with the same targets
        for (String itemName : itemNames) {
            LootItemCondition itemCondition = LootItemCondition.builder()
                    .itemName(itemName)
                    .targetAmountMin(targetAmountMin)
                    .targetAmountMax(targetAmountMax)
                    .includeNoted(includeNoted)
                    .includeNoneOwner(includeNoneOwner)
                    .build();
                    
            orCondition.addCondition(itemCondition);
        }
        
        return orCondition;
    }
   
    @Override
    public boolean isSatisfied() {        
        return currentTrackedCount >= currentTargetAmount;
    }

    @Override
    public String getDescription() {
        String randomRangeInfo = "";
        
        if (targetAmountMin != targetAmountMax) {
            randomRangeInfo = String.format(" (randomized from %d-%d)", targetAmountMin, targetAmountMax);
        }
        
        return String.format("Loot %d %s%s (%d/%d, %.1f%%)", 
                currentTargetAmount, 
                itemName,
                randomRangeInfo,
                currentTrackedCount,
                currentTargetAmount,
                getProgressPercentage());
    }

    @Override
    public void reset() {
        reset(false);
    }

    @Override
    public void reset(boolean randomize) {
        if (randomize) {
            this.currentTargetAmount = Rs2Random.between(targetAmountMin, targetAmountMax);
        }
        this.currentTrackedCount = 0;
        this.lastInventoryCount = getCurrentInventoryCount();
        this.trackedItemLocations.clear();
        this.trackedItemQuantities.clear();
        this.recentlyLootedItems.clear();
    }
    
    @Override
    public double getProgressPercentage() {
        if (currentTargetAmount <= 0) {
            return 100.0;
        }
        
        double ratio = (double) currentTrackedCount / currentTargetAmount;
        return Math.min(100.0, ratio * 100.0);
    }
    
    /**
     * Called when an item spawns on the ground - we check if it matches our target item
     */
    @Override
    public void onItemSpawned(ItemSpawned event) {
        TileItem tileItem = event.getItem();
        WorldPoint location = event.getTile().getWorldLocation();
        
        // Check for ownership and item name match
        Client client = Microbot.getClient();
        if (client == null) return;
        
        // Only track items that belong to the player 
        boolean isPlayerOwned = tileItem.getOwnership() == OWNERSHIP_SELF || tileItem.getOwnership() == OWNERSHIP_GROUP;
        log.info("\nDetected own item spawn: {} at {}", tileItem.getModel(), location);
        log.info("\n"+itemName + "  - ownership: " + tileItem.getOwnership()); 
        if (isPlayerOwned) {
            // Get the item name and check if it matches our pattern
            String spawnedItemName = Microbot.getClientThread().runOnClientThreadOptional(() -> 
                Microbot.getItemManager().getItemComposition(tileItem.getId()).getName()
            ).orElse("");
            
            if (itemPattern.matcher(spawnedItemName).matches()) {                
                if(Microbot.isDebug()){
                    log.info("\nDetected own item spawn: {} at {}", spawnedItemName, location);
                    log.info("\n"+itemName + "  - ownership: " + tileItem.getOwnership());    
                }
                trackedItemLocations.add(location);
                trackedItemQuantities.put(location, tileItem.getQuantity());
            }
        }
    }
    
    /**
     * Called when an item despawns from the ground - check if it was one of our tracked items
     */
    @Override
    public void onItemDespawned(ItemDespawned event) {
        WorldPoint location = event.getTile().getWorldLocation();
        
        // If we were tracking this item and it despawned, it was likely picked up
        if (trackedItemLocations.contains(location)) {
            TileItem tileItem = event.getItem();
            
            // Get the item name and check if it matches our pattern
            String despawnedItemName = Microbot.getClientThread().runOnClientThreadOptional(() -> 
                Microbot.getItemManager().getItemComposition(tileItem.getId()).getName()
            ).orElse("");
            
            if (itemPattern.matcher(despawnedItemName).matches()) {
                int quantity = trackedItemQuantities.getOrDefault(location, 1);
                log.info("Item despawned that we were tracking: {} x{} at {}", 
                        despawnedItemName, quantity, location);
                
                // Record that we just looted from this location
                recentlyLootedItems.put(location, System.currentTimeMillis());
                
                // Clean up tracking
                trackedItemLocations.remove(location);
                trackedItemQuantities.remove(location);
            }
        }
    }
    
    @Override
    public void onGroundObjectSpawned(GroundObjectSpawned event) {
        // Not used for this implementation
    }
    
    @Override
    public void onGroundObjectDespawned(GroundObjectDespawned event) {
        // Not used for this implementation
    }
    
    /**
     * Called when item containers change (inventory, bank, etc.)
     * We need to check if we gained any of our target items.
     */
    @Override
    public void onItemContainerChanged(ItemContainerChanged event) {
        // Only interested in inventory changes
        if (event.getContainerId() != InventoryID.INVENTORY.getId()) {
            return;
        }
        
        int currentCount = getCurrentInventoryCount();
        
        // If inventory count increased, we might have looted the item
        if (currentCount > lastInventoryCount) {
            int gained = currentCount - lastInventoryCount;
            
            // Only count as looted if we have recently seen an item despawn
            // or if it's been a while since we last checked (to catch edge cases)
            boolean recentlyDespawned = !recentlyLootedItems.isEmpty();
            
            // Clean up old entries from recently looted map (older than 5 seconds)
            long now = System.currentTimeMillis();
            recentlyLootedItems.entrySet().removeIf(entry -> now - entry.getValue() > 5000);
            
            if (recentlyDespawned) {
                log.debug("Gained {} {} in inventory after ground item despawned - counting as looted",
                        gained, itemName);
                currentTrackedCount += gained;
            }
        }
        
        lastInventoryCount = currentCount;
    }
    
    /**
     * Check inventory every game tick to catch changes that might not trigger ItemContainerChanged
     */
    @Override
    public void onGameTick(GameTick gameTick) {
        int currentCount = getCurrentInventoryCount();
        
        // If current count is different from last count, update
        if (currentCount != lastInventoryCount) {
            if (currentCount > lastInventoryCount) {
                int gained = currentCount - lastInventoryCount;
                log.debug("Game tick detected inventory increase of {} {}", gained, itemName);
                
                // Check if we're likely to have looted the item by looking at available ground items
                boolean potentialLoot = false;
                for (WorldPoint location : trackedItemLocations) {
                    GroundItem groundItem = GroundItemsPlugin.getCollectedGroundItems()
                            .values()
                            .stream()
                            .filter(item -> item.getLocation().equals(location))
                            .findFirst()
                            .orElse(null);
                    
                    if (groundItem != null) {
                        potentialLoot = true;
                        break;
                    }
                }
                
                if (potentialLoot || !trackedItemLocations.isEmpty()) {
                    log.debug("Counting {} {} as looted items", gained, itemName);
                    currentTrackedCount += gained;
                }
            }
            
            lastInventoryCount = currentCount;
        }
    }
    
    /**
     * Gets the current count of this item in the inventory
     */
    private int getCurrentInventoryCount() {
        int currentItemCountNoted = getNotedItems().stream().filter(item -> {
            if (item == null) {
                return false;
            }
            return itemPattern.matcher(item.getName()).matches();             
        }).collect(Collectors.toList()).size();
        
        int currentItemCountUnNoted = getUnNotedItems().stream().filter(item -> {
            if (item == null) {
                return false;
            }            
            return itemPattern.matcher(item.getName()).matches();             
        }).collect(Collectors.toList()).size();
        
        log.debug("getCurrentInventoryCount: noted {} - unnoted {}", 
                currentItemCountNoted, currentItemCountUnNoted);
        
        if (includeNoted) {
            return currentItemCountNoted + currentItemCountUnNoted;
        }
        return currentItemCountUnNoted;
    }

}