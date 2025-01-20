package net.runelite.client.plugins.microbot.util.depositbox;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameObject;
import net.runelite.api.SpriteID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Item;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.tile.Rs2Tile;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static net.runelite.api.widgets.ComponentID.DEPOSIT_BOX_INVENTORY_ITEM_CONTAINER;
import static net.runelite.client.plugins.microbot.util.Global.*;

@Slf4j
public class Rs2DepositBox {

    private static final int DEPOSIT_ALL_BUTTON_ID = 1920;
    private static final int DEPOSIT_INVENTORY_ID = 1921;
    private static final int DEPOSIT_EQUIPMENT_ID = 1922;
    private static final int CLOSE_BUTTON_PARENT_ID = DEPOSIT_BOX_INVENTORY_ITEM_CONTAINER - 1;

    /**
     * Checks if the deposit box interface is open.
     *
     * @return true if the deposit box interface is open, false otherwise.
     */
    public static boolean isOpen() {
        return Rs2Widget.isDepositBoxWidgetOpen();
    }

    /**
     * Closes the deposit box interface.
     *
     * @return true if the deposit box interface was successfully closed, false otherwise.
     */
    public static boolean closeDepositBox() {
        if (!isOpen()) return false;
        Rs2Widget.clickChildWidget(CLOSE_BUTTON_PARENT_ID, 11); // Assuming close button ID is 11
        sleepUntilOnClientThread(() -> !isOpen());
        return true;
    }

    /**
     * Opens the deposit box interface by interacting with a nearby deposit box.
     *
     * @return true if the deposit box was successfully opened, false otherwise.
     */
    public static boolean openDepositBox() {
        Microbot.status = "Opening deposit box";
        try {
            if (Microbot.getClient().isWidgetSelected())
                Microbot.getMouse().click();
            // Assuming interaction logic with a nearby deposit box
            if (isOpen()) return true;
            GameObject depositBox = Rs2GameObject.findDepositBox();
            boolean action = false;
            if (depositBox != null) {
                action = Rs2GameObject.interact(depositBox, "Deposit");
            }
            if (action) {
                sleepUntil(Rs2DepositBox::isOpen, 2500);
            }
            return action;
        } catch (Exception e) {
            Microbot.log("Error opening deposit box: " + e.getMessage());
        }
        return false;
    }

    /**
     * Deposits all items in the inventory into the deposit box.
     */
    public static void depositAll() {
        Microbot.status = "Depositing all items";
        if (Rs2Inventory.isEmpty()) return;
        if (!isOpen()) return;
        Widget depositAllWidget = Rs2Widget.findWidget(SpriteID.BANK_DEPOSIT_INVENTORY, null);
        if (depositAllWidget == null) return;

        Microbot.getMouse().click(depositAllWidget.getBounds());
        sleepUntil(Rs2Inventory::isEmpty);
    }

    public static boolean depositAll(Predicate<Rs2Item> predicate, boolean fastDeposit) {
        boolean result = false;
        List<Rs2Item> items = Rs2Inventory.items().stream().filter(predicate).distinct().collect(Collectors.toList());
        for (Rs2Item item : items) {
            if (item == null) continue;
            depositItem(item);
            if (!fastDeposit)
                sleep(100, 300);
            result = true;
        }
        return result;
    }

    public static boolean depositAll(Predicate<Rs2Item> predicate) {
        return depositAll(predicate, false);
    }

    /**
     * Deposits all items in the player's inventory into the bank, except for the items with the specified IDs.
     * This method uses a lambda function to filter out the items with the specified IDs from the deposit operation.
     *
     * @param ids The IDs of the items to be excluded from the deposit.
     *
     * @return true if any items were deposited, false otherwise.
     */
    public static boolean depositAllExcept(Integer... ids) {
        return depositAll(x -> Arrays.stream(ids).noneMatch(id -> id == x.id));
    }

    public static boolean depositAllExcept(boolean fastDeposit, Integer... ids) {
        return depositAll(x -> Arrays.stream(ids).noneMatch(id -> id == x.id), fastDeposit);
    }

    /**
     * Deposits all items in the player's inventory into the bank, except for the items with the specified names.
     * This method uses a lambda function to filter out the items with the specified names from the deposit operation.
     *
     * @param names The names of the items to be excluded from the deposit.
     *
     * @return true if any items were deposited, false otherwise.
     */
    public static boolean depositAllExcept(String... names) {
        return depositAll(x -> Arrays.stream(names).noneMatch(name -> name.equalsIgnoreCase(x.name)));
    }

    public static boolean depositAllExcept(boolean fastDeposit, String... names) {
        return depositAll(x -> Arrays.stream(names).noneMatch(name -> name.equalsIgnoreCase(x.name)), fastDeposit);
    }

    /**
     * Deposits all items in the player's inventory into the bank, except for the items with the specified names.
     * This method uses a lambda function to filter out the items with the specified names from the deposit operation.
     *
     * @param names The names of the items to be excluded from the deposit.
     *
     * @return true if any items were deposited, false otherwise.
     */
    public static boolean depositAllExcept(List<String> names) {
        return depositAll(x -> names.stream().noneMatch(name -> name.equalsIgnoreCase(x.name)));
    }

    public static boolean depositAllExcept(boolean fastDeposit, List<String> names) {
        return depositAll(x -> names.stream().noneMatch(name -> name.equalsIgnoreCase(x.name)), fastDeposit);
    }

    /**
     * Deposits all items in the player's inventory into the bank, except for the items with the specified names.
     * This method uses a lambda function to filter out the items with the specified names from the deposit operation.
     * It also allows for a delay between deposit operations.
     *
     * @param names The names of the items to be excluded from the deposit.
     *
     * @return true if any items were deposited, false otherwise.
     */
    public static boolean depositAllExcept(boolean exact, boolean fastDeposit, String... names) {
        if (!exact)
            return depositAll(x -> Arrays.stream(names).noneMatch(name -> x.name.contains(name.toLowerCase())), fastDeposit);
        else
            return depositAll(x -> Arrays.stream(names).noneMatch(name -> name.equalsIgnoreCase(x.name)), fastDeposit);
    }


    /**
     * Deposits a specific item by its name.
     *
     * @param itemName the name of the item to deposit.
     */
    public static void depositItem(String itemName) {
        Rs2Item item = Rs2Inventory.get(itemName);
        if (item == null) return;
        depositItem(item);
    }

    /**
     * Deposits a specific item by its ID.
     *
     * @param itemId the ID of the item to deposit.
     */
    public static void depositItem(int itemId) {
        Rs2Item item = Rs2Inventory.get(itemId);
        if (item == null) return;
        depositItem(item);
    }

    /**
     * Deposits a item quickly by its name with a partial or exact name match.
     * Name and a boolean to determine if the name should be an exact match.
     *
     * @param itemName   the name of the item to deposit.
     * @param exactMatch true if the name should be an exact match, false otherwise.
     */
    public static void depositItem(String itemName, boolean exactMatch) {
        Rs2Item item = Rs2Inventory.get(itemName, exactMatch);
        if (item == null) return;
        depositItem(item);
    }


    /**
     * Deposits a specific item by its Rs2Item reference.
     *
     * @param rs2Item the Rs2Item to deposit.
     */
    public static void depositItem(Rs2Item rs2Item) {
        if (rs2Item == null || !isOpen()) return;
        if (!Rs2Inventory.hasItem(rs2Item.id)) return;
        Rs2Inventory.interact(rs2Item, "Deposit-All");
    }

    /**
     * Deposits all equipment into the deposit box.
     */
    public static void depositEquipment() {
        Widget widget = Rs2Widget.findWidget(SpriteID.BANK_DEPOSIT_EQUIPMENT, null);
        if (widget == null) return;

        Microbot.getMouse().click(widget.getBounds());
    }

    /**
     * Get the nearest despoi tbox
     *
     * @return DepositBoxLocation
     */
    public static DepositBoxLocation getNearestDepositBox() {
        return getNearestDepositBox(Microbot.getClient().getLocalPlayer().getWorldLocation());
    }
    
    /**
     * Get the nearest deposit box to world point
     *
     * @param worldPoint 
     * @return DepositBoxLocation
     */
    public static DepositBoxLocation getNearestDepositBox(WorldPoint worldPoint) {
        Microbot.log("Calculating nearest bank path...");

        DepositBoxLocation despoitBoxLocation = Arrays.stream(DepositBoxLocation.values())
                .parallel()
                .filter(DepositBoxLocation::hasRequirements)
                .min(Comparator.comparingDouble(db ->
                        Rs2Walker.getTotalTiles(worldPoint, db.getWorldPoint())))
                .orElse(null);

        if (despoitBoxLocation != null) {
            Microbot.log("Found nearest deposit box: " + despoitBoxLocation.name());
            return despoitBoxLocation;
        } else {
            Microbot.log("Unable to find nearest deposit box");
            return null;
        }
    }

    /**
     * Walk to deposit box location
     *
     * @param depositBoxLocation
     * @return true if player location is less than 4 tiles away from the bank location
     */
    public static boolean walkToDepositBox(DepositBoxLocation depositBoxLocation) {
        if (Rs2Bank.isOpen()) return true;
        Rs2Player.toggleRunEnergy(true);
        Microbot.status = "Walking to nearest deposit box " + depositBoxLocation.name();
        Rs2Walker.walkTo(depositBoxLocation.getWorldPoint(), 4);
        return depositBoxLocation.getWorldPoint().distanceTo2D(Microbot.getClient().getLocalPlayer().getWorldLocation()) <= 4;
    }
    
    /**
     * Walk to the nearest deposit box location
     *
     * @return true if player location is less than 4 tiles away from the bank location
     */
    public static boolean walkToDepositBox() {
        return walkToDepositBox(getNearestDepositBox());
    }

    /**
     * Walk to the nearest deposit box location
     *
     * @return true if bank interface is open
     */
    public static boolean walkToAndUseDepositBox() {
        return walkToAndUseDepositBox(getNearestDepositBox());
    }

    /**
     * Walk to deposit box location & use bank
     *
     * @param depositBoxLocation
     * @return true if bank interface is open
     */
    public static boolean walkToAndUseDepositBox(DepositBoxLocation depositBoxLocation) {
        if (Rs2Bank.isOpen()) return true;
        Rs2Player.toggleRunEnergy(true);
        Microbot.status = "Walking to nearest deposit box " + depositBoxLocation.name();
        boolean result = depositBoxLocation.getWorldPoint().distanceTo(Microbot.getClient().getLocalPlayer().getWorldLocation()) <= 8;
        if (result) {
            return openDepositBox();
        } else {
            Rs2Walker.walkTo(depositBoxLocation.getWorldPoint());
        }
        return false;
    }

    /**
     * Banks items if your inventory does not have enough empty slots (0 empty slots being full).
     * Will walk back to the initial player location passed as a parameter.
     *
     * @param itemNames           List of item names to deposit.
     * @param initialPlayerLocation The initial location of the player to return to after banking.
     * @param emptySlotCount      The minimum number of empty slots required in the inventory.
     * @return true if items were successfully banked and the player returned to the initial location, false otherwise.
     */
    public static boolean bankItemsAndWalkBackToOriginalPosition(List<String> itemNames, WorldPoint initialPlayerLocation, int emptySlotCount) {
        return bankItemsAndWalkBackToOriginalPosition(itemNames, false, initialPlayerLocation, emptySlotCount, 4);
    }

    /**
     * Banks items if the inventory is full and returns to the initial player location.
     *
     * @param itemNames           List of item names to deposit.
     * @param initialPlayerLocation The initial location of the player to return to after banking.
     * @return true if items were successfully banked and the player returned to the initial location, false otherwise.
     */
    public static boolean bankItemsAndWalkBackToOriginalPosition(List<String> itemNames, WorldPoint initialPlayerLocation) {
        return bankItemsAndWalkBackToOriginalPosition(itemNames, false, initialPlayerLocation, 0, 4);
    }

    /**
     * Banks items at a deposit box if the inventory does not have enough empty slots (0 empty slots being full).
     * Will walk back to the initial player location passed as a parameter.
     *
     * @param itemNames           List of item names to deposit.
     * @param exactItemNames      Whether to match item names exactly or partially.
     * @param initialPlayerLocation The initial location of the player to return to after banking.
     * @param emptySlotCount      The minimum number of empty slots required in the inventory.
     * @param distance            Maximum distance to allow from the initial location when returning.
     * @return true if items were successfully banked and the player returned to the initial location, false otherwise.
     */
    public static boolean bankItemsAndWalkBackToOriginalPosition(List<String> itemNames, boolean exactItemNames, WorldPoint initialPlayerLocation, int emptySlotCount, int distance) {
        if (Rs2Inventory.getEmptySlots() <= emptySlotCount) {
            boolean isDepositBoxOpen = Rs2DepositBox.walkToAndUseDepositBox();
            if (isDepositBoxOpen) {
                for (String itemName : itemNames) {
                    if (exactItemNames) {
                        Rs2DepositBox.depositItem(itemName, true);
                    } else {
                        Rs2DepositBox.depositAll(x -> x.name.toLowerCase().contains(itemName.toLowerCase()));
                    }
                }
            }
            return false;
        }

        if (distance > 10) distance = 10;

        if (initialPlayerLocation.distanceTo(Rs2Player.getWorldLocation()) > distance || !Rs2Tile.isTileReachable(initialPlayerLocation)) {
            Rs2Walker.walkTo(initialPlayerLocation, distance);
        } else {
            Rs2Walker.walkFastCanvas(initialPlayerLocation);
        }

        return !(Rs2Inventory.getEmptySlots() <= emptySlotCount) && initialPlayerLocation.distanceTo(Rs2Player.getWorldLocation()) <= distance;
    }

    /**
     * Banks items if the inventory does not have enough empty slots (0 empty slots being full).
     * Will walk back to the initial player location passed as a parameter.
     *
     * @param itemNames           List of item names to deposit.
     * @param initialPlayerLocation The initial location of the player to return to after banking.
     * @param emptySlotCount      The minimum number of empty slots required in the inventory.
     * @param distance            Maximum distance to allow from the initial location when returning.
     * @return true if items were successfully banked and the player returned to the initial location, false otherwise.
     */
    public static boolean bankItemsAndWalkBackToOriginalPosition(List<String> itemNames, WorldPoint initialPlayerLocation, int emptySlotCount, int distance) {
        return bankItemsAndWalkBackToOriginalPosition(itemNames, false, initialPlayerLocation, emptySlotCount, distance);
    }
}
