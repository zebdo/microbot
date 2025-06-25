package net.runelite.client.plugins.microbot.util.inventory;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemID;
import net.runelite.api.ParamID;
import net.runelite.client.plugins.microbot.Microbot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Rs2ItemModel {
    @Getter
    private int id;
    @Getter
    @Setter
	private int quantity;
    @Getter
	private int slot = -1;
	private String name;
	private String[] inventoryActions;
    @Getter
	private List<String> equipmentActions = new ArrayList<>();
	private boolean isStackable;
	private boolean isNoted;
	private boolean isTradeable;
	private ItemComposition itemComposition;
	private int[] wearableActionIndexes = new int[]{
            ParamID.OC_ITEM_OP1,
            ParamID.OC_ITEM_OP2,
            ParamID.OC_ITEM_OP3,
            ParamID.OC_ITEM_OP4,
            ParamID.OC_ITEM_OP5,
            ParamID.OC_ITEM_OP6,
            ParamID.OC_ITEM_OP7,
            ParamID.OC_ITEM_OP8
	};


    public Rs2ItemModel(Item item, ItemComposition itemComposition, int slot) {
        this.id = item.getId();
        this.quantity = item.getQuantity();
        this.name = itemComposition.getName();
        this.slot = slot;
        this.isStackable = itemComposition.isStackable();
        this.isNoted = itemComposition.getNote() == 799;
        if (this.isNoted) {
            Microbot.getClientThread().runOnClientThreadOptional(() ->
                    Microbot.getClient().getItemDefinition(this.id - 1)).ifPresent(itemDefinition -> this.isTradeable = itemDefinition.isTradeable());
        } else {
            this.isTradeable = itemComposition.isTradeable();
        }
        this.inventoryActions = itemComposition.getInventoryActions();
        this.itemComposition = itemComposition;
        addEquipmentActions(itemComposition);
    }

    /**
     * Creates an Rs2ItemModel from cached data (ID, quantity, slot).
     * This is used when loading bank data from config where we don't have the full ItemComposition.
     * ItemComposition data will be loaded lazily when needed.
     * 
     * @param id Item ID
     * @param quantity Item quantity
     * @param slot Item slot position
     * @return Rs2ItemModel with basic data, ItemComposition loaded lazily
     */
    public static Rs2ItemModel createFromCache(int id, int quantity, int slot) {
        return new Rs2ItemModel(id, quantity, slot);
    }

    /**
     * Private constructor for creating Rs2ItemModel from cached data.
     * ItemComposition will be loaded lazily when needed.
     */
    private Rs2ItemModel(int id, int quantity, int slot) {
        this.id = id;
        this.quantity = quantity;
        this.slot = slot;
        
        // Initialize with defaults - will be loaded lazily
        this.name = null;
        this.isStackable = false;
        this.isNoted = false;
        this.isTradeable = false;
        this.inventoryActions = new String[0];
        this.itemComposition = null;
        this.equipmentActions = new ArrayList<>();
    }

    /**
     * Lazy loads the ItemComposition if not already loaded.
     * This ensures we can work with cached items while minimizing performance impact.
     */
    private void ensureCompositionLoaded() {
        
        if (itemComposition == null && id > 0) {
            itemComposition = Microbot.getClientThread().runOnClientThreadOptional(()->Microbot.getItemManager().getItemComposition(id)).orElse(null);
            if (itemComposition != null) {
                this.name = itemComposition.getName();
                this.isStackable = itemComposition.isStackable();
                this.isNoted = itemComposition.getNote() == 799;
                if (this.isNoted) {
                    Microbot.getClientThread().runOnClientThreadOptional(() ->
                            Microbot.getClient().getItemDefinition(this.id - 1)).ifPresent(itemDefinition -> this.isTradeable = itemDefinition.isTradeable());
                } else {
                    this.isTradeable = itemComposition.isTradeable();
                }
                this.inventoryActions = itemComposition.getInventoryActions();
                addEquipmentActions(itemComposition);
            }
        }
    }

    /**
     * Gets the item name, loading composition if needed.
     */
    public String getName() {
        if (name == null) {
            ensureCompositionLoaded();
        }
        return name != null ? name : "Unknown Item";
    }

    /**
     * Gets whether the item is stackable, loading composition if needed.
     */
    public boolean isStackable() {
        if (itemComposition == null) {
            ensureCompositionLoaded();
        }
        return isStackable;
    }

    /**
     * Gets whether the item is noted, loading composition if needed.
     */
    public boolean isNoted() {
        if (itemComposition == null) {
            ensureCompositionLoaded();
        }
        return isNoted;
    }

    /**
     * Gets whether the item is tradeable, loading composition if needed.
     */
    public boolean isTradeable() {
        if (itemComposition == null) {
            ensureCompositionLoaded();
        }
        return isTradeable;
    }

    /**
     * Gets the inventory actions, loading composition if needed.
     */
    public String[] getInventoryActions() {
        if (itemComposition == null) {
            ensureCompositionLoaded();
        }
        return inventoryActions;
    }

    /**
     * Gets the item composition, loading it if needed.
     */
    public ItemComposition getItemComposition() {
        if (itemComposition == null) {
            ensureCompositionLoaded();
        }
        return itemComposition;
    }

	public boolean isFood() {
		if (isNoted()) return false;

		String lowerName = getName().toLowerCase();

		boolean isEdible = Arrays.stream(getInventoryActions()).anyMatch(action -> action != null && action.equalsIgnoreCase("eat"));

		return (isEdible || lowerName.contains("jug of wine")) && !lowerName.contains("rock cake");
	}

    private void addEquipmentActions(ItemComposition itemComposition) {
        for (int i = 0; i < wearableActionIndexes.length; i++) {
            try {
                String value = itemComposition.getStringValue(wearableActionIndexes[i]);
                this.equipmentActions.add(value);
            } catch (Exception ex) {
                this.equipmentActions.add("");
            }
        }
    }

    public int getPrice() {
        return Microbot.getClientThread().runOnClientThreadOptional(() ->
                Microbot.getItemManager().getItemPrice(id) * quantity).orElse(0);
    }

    public int getHaPrice() {
        return itemComposition.getHaPrice();
    }

    public boolean isHaProfitable() {
        int natureRunePrice = Microbot.getClientThread().runOnClientThreadOptional(() ->
                Microbot.getItemManager().getItemPrice(ItemID.NATURE_RUNE)).orElse(0);
        return (getHaPrice() - natureRunePrice) > (getPrice()/quantity) && isTradeable;

    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Rs2ItemModel other = (Rs2ItemModel) obj;
        return id == other.id;
    }
}
