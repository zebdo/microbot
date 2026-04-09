# Items — Entity Guide

Gotchas when interacting with items in inventory, bank, ground, equipment, shops, or deposit box.

Covers utilities under:
- `runelite-client/src/main/java/net/runelite/client/plugins/microbot/util/inventory/`
- `runelite-client/src/main/java/net/runelite/client/plugins/microbot/util/bank/`
- `runelite-client/src/main/java/net/runelite/client/plugins/microbot/util/equipment/`
- `runelite-client/src/main/java/net/runelite/client/plugins/microbot/util/grounditem/`
- `runelite-client/src/main/java/net/runelite/client/plugins/microbot/util/shop/`
- `runelite-client/src/main/java/net/runelite/client/plugins/microbot/util/depositbox/`

---

## 1. Always verify the menu action exists on the actual item before interacting

When implementing any code that interacts with an item via a hardcoded action string (`"Eat"`, `"Drink"`, `"Release"`, `"Use"`, `"Wield"`, `"Wear"`, `"Equip"`, etc.), **do not assume the action is universally present** across every item in a category. Read the item's actual menu options from `Rs2ItemModel.getInventoryActions()` and pick the first matching one.

**Why this matters:** Items that produce the same effect can use different menu verbs, and grouping them under one helper that hardcodes a single action will silently fail for the odd one out.

Real bug (fixed in `Rs2Player.java:1384`): `Rs2Potion.getPrayerPotionsVariants()` listed three items that all restore prayer:

| Item | Menu action |
|---|---|
| `Moonlight potion` | `Drink` |
| `Moonlight moth mix` | `Drink` |
| `Moonlight moth` (the bug itself) | `Release` |

`Rs2Player.usePotion(String...)` looked up the item by name and then unconditionally called `Rs2Inventory.interact(potion, "drink")`. When the player held a Moonlight moth, the helper found it, hovered it, and then dispatched a menu action that didn't exist on that item — so nothing happened. The script just stood there. The bug was invisible at the call site because `usePotion` *looks* like it can only receive potions; the type system doesn't enforce it.

**Pattern to follow:**

```java
// ❌ BAD - hardcoded action; silently fails for any item that doesn't expose it
return Rs2Inventory.interact(item, "drink");

// ✅ GOOD - derive the action from the item itself, with a prioritised fallback list
String action = Arrays.stream(item.getInventoryActions())
        .filter(Objects::nonNull)
        .filter(a -> a.equalsIgnoreCase("drink") || a.equalsIgnoreCase("release"))
        .findFirst()
        .orElse(null);
if (action == null) return false;
return Rs2Inventory.interact(item, action);
```

When you know the *intent* (consume, equip, drop) but not the verb the game uses, encode that intent as a small ordered list of acceptable verbs and pick the first one the item exposes. Never hardcode a single verb against a name list you didn't fully audit.

**Where this applies:**
- `Rs2Inventory.interact(item, action)` and all overloads
- `Rs2Bank.depositOne` / `withdrawX` action arguments
- `Rs2Equipment.interact()` / `Rs2Equipment.equip()`
- `Rs2GroundItem.interact()` / `Rs2GroundItem.loot()`
- `Rs2Shop.buyItem` action dispatch
- Any helper that accepts a list of item names (`Rs2Potion.getXxxVariants()`, `Rs2Food`, food/potion arrays) and applies a single action to whichever one is found

**Defensive check:** Before merging code that uses a hardcoded action against a curated list of item names, add a unit test that walks every name in the list and asserts the corresponding `ItemComposition` exposes the expected action. The test fails the moment a non-conforming item is added to the list — at PR time, not in production. The same pattern applies to any future `Rs2*.getXxxVariants()` list, food enum, or equipment set.

---

<!-- Add new gotchas here as numbered entries (## 2, ## 3, ...). -->
