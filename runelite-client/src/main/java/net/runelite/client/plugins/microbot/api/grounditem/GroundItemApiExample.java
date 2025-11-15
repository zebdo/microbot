package net.runelite.client.plugins.microbot.api.grounditem;

import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItemModel;

import java.util.List;

/**
 * Example usage of the Ground Item API
 * This demonstrates how to query ground items using the new API structure:
 * - Rs2GroundItemCache: Caches ground items for efficient querying
 * - Rs2GroundItemQueryable: Provides a fluent interface for filtering and querying ground items
 */
public class GroundItemApiExample {

    public static void examples() {
        // Create a new cache instance
        Rs2GroundItemCache cache = new Rs2GroundItemCache();

        // Example 1: Get the nearest ground item
        Rs2GroundItemModel nearestItem = cache.query().nearest();

        // Example 2: Get the nearest ground item within 10 tiles
        Rs2GroundItemModel nearestItemWithinRange = cache.query().nearest(10);

        // Example 3: Find a ground item by name
        Rs2GroundItemModel coins = cache.query().withName("Coins");

        // Example 4: Find a ground item by multiple names
        Rs2GroundItemModel loot = cache.query().withNames("Dragon bones", "Dragon scale", "Dragon dagger");

        // Example 5: Find a ground item by ID
        Rs2GroundItemModel itemById = cache.query().withId(995); // Coins

        // Example 6: Find a ground item by multiple IDs
        Rs2GroundItemModel itemByIds = cache.query().withIds(995, 526, 537); // Coins, Bones, Dragon bones

        // Example 7: Get all ground items worth more than 1000 gp
        List<Rs2GroundItemModel> valuableItems = cache.query()
                .where(item -> item.getTotalValue() >= 1000)
                .toList();

        // Example 8: Find nearest lootable item
        Rs2GroundItemModel lootableItem = cache.query()
                .where(Rs2GroundItemModel::isLootAble)
                .nearest();

        // Example 9: Find items owned by player
        List<Rs2GroundItemModel> ownedItems = cache.query()
                .where(Rs2GroundItemModel::isOwned)
                .toList();

        // Example 10: Find stackable items
        List<Rs2GroundItemModel> stackableItems = cache.query()
                .where(Rs2GroundItemModel::isStackable)
                .toList();

        // Example 11: Find noted items
        List<Rs2GroundItemModel> notedItems = cache.query()
                .where(Rs2GroundItemModel::isNoted)
                .toList();

        // Example 12: Find items worth high alching
        List<Rs2GroundItemModel> alchableItems = cache.query()
                .where(item -> item.isProfitableToHighAlch(100))
                .toList();

        // Example 13: Find items about to despawn
        List<Rs2GroundItemModel> despawningItems = cache.query()
                .where(item -> item.willDespawnWithin(30))
                .toList();

        // Example 14: Find common loot items
        List<Rs2GroundItemModel> commonLoot = cache.query()
                .where(Rs2GroundItemModel::isCommonLoot)
                .toList();

        // Example 15: Find priority items (high value or about to despawn)
        List<Rs2GroundItemModel> priorityItems = cache.query()
                .where(Rs2GroundItemModel::shouldPrioritize)
                .toList();

        // Example 16: Complex query - Find nearest valuable lootable item within 15 tiles
        Rs2GroundItemModel target = cache.query()
                .where(Rs2GroundItemModel::isLootAble)
                .where(item -> item.getTotalGeValue() >= 5000)
                .where(item -> !item.isDespawned())
                .nearest(15);

        // Example 17: Find items by quantity
        List<Rs2GroundItemModel> largeStacks = cache.query()
                .where(item -> item.getQuantity() >= 100)
                .toList();

        // Example 18: Find tradeable items
        List<Rs2GroundItemModel> tradeableItems = cache.query()
                .where(Rs2GroundItemModel::isTradeable)
                .toList();

        // Example 19: Find members items
        List<Rs2GroundItemModel> membersItems = cache.query()
                .where(Rs2GroundItemModel::isMembers)
                .toList();

        // Example 20: Static method to get stream directly
        Rs2GroundItemModel firstItem = Rs2GroundItemCache.getGroundItemsStream()
                .filter(item -> item.getName() != null)
                .findFirst()
                .orElse(null);

        // Example 21: Find items by partial name match
        List<Rs2GroundItemModel> herbItems = cache.query()
                .where(item -> item.getName() != null &&
                       item.getName().toLowerCase().contains("herb"))
                .toList();

        // Example 22: Find items by distance from specific point
        List<Rs2GroundItemModel> itemsNearBank = cache.query()
                .where(item -> item.isWithinDistanceFromPlayer(5))
                .toList();

        // Example 23: Find items that are clickable (visible in viewport)
        List<Rs2GroundItemModel> clickableItems = cache.query()
                .where(Rs2GroundItemModel::isClickable)
                .toList();

        // Example 24: Find best value item to loot
        Rs2GroundItemModel bestValue = cache.query()
                .where(Rs2GroundItemModel::isLootAble)
                .where(item -> !item.isDespawned())
                .where(item -> item.isWithinDistanceFromPlayer(10))
                .toList()
                .stream()
                .max((a, b) -> Integer.compare(a.getTotalGeValue(), b.getTotalGeValue()))
                .orElse(null);

        // Example 25: Find items worth looting based on minimum value
        List<Rs2GroundItemModel> worthLooting = cache.query()
                .where(item -> item.isWorthLootingGe(10000))
                .toList();
    }
}
