package net.runelite.client.plugins.microbot.Acun.farmTreeRun;

import lombok.Getter;

@Getter
class FarmingItem {
    private final int itemId;
    private final int quantity;
    private final boolean noted;

    public FarmingItem(int itemId, int quantity) {
        this(itemId, quantity, false); // Default `noted` to false
    }

    public FarmingItem(int itemId, int quantity, boolean noted) {
        this.itemId = itemId;
        this.quantity = quantity;
        this.noted = noted;
    }
}
