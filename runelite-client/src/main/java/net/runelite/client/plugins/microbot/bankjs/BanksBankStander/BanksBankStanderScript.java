package net.runelite.client.plugins.microbot.bankjs.BanksBankStander;

import lombok.Setter;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.inventory.InteractOrder;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Item;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import javax.inject.Inject;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.runelite.client.plugins.microbot.util.Global.sleepUntilTrue;
import static net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory.calculateInteractOrder;
import static net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory.items;

// heaps of new features added by Storm
public class BanksBankStanderScript extends Script {
    @Inject
    private BanksBankStanderConfig config;
    public static double version = 2.0;

    public static long previousItemChange;

    public static CurrentStatus currentStatus = CurrentStatus.FETCH_SUPPLIES;

    public static int itemsProcessed;

    static Integer thirdItemId;
    static Integer fourthItemId;


    static Integer firstItemId;
    public static Integer secondItemId;
    static int firstItemSum;
    static int secondItemSum;
    static int thirdItemSum;
    static int fourthItemSum;

    private int sleepMin;
    private int sleepMax;
    private int sleepTarget;

    public static boolean isWaitingForPrompt = false;
    private static boolean sleep;
    // These just used for the overlay
    public static String firstIdentity;
    public static String secondIdentity;
    public static String thirdIdentity;
    public static String fourthIdentity;

    private long timeValue;
    private int randomNum;
    Iterator<Rs2Item> inventorySlots;

    public boolean run(BanksBankStanderConfig config) {
        this.config = config; // Initialize the config object before accessing its parameters
        itemsProcessed = 0;
        firstItemSum = 0;
        secondItemSum = 0;
        thirdItemSum = 0;
        fourthItemSum = 0;
        inventorySlots = null;

        sleepMin = config.sleepMin();
        sleepMax = config.sleepMax();
        if (config.sleepMax() > config.sleepMin() + 120) {
            sleepMax = config.sleepMax();
            sleepTarget = config.sleepTarget();
        } else {
            sleepMax = config.sleepMax() + Rs2Random.between(120 - (config.sleepMax() - config.sleepMin()), 151);
            sleepTarget = sleepMin + ((sleepMax - sleepMin) / 2);
        }
        // Determine whether the first & second item is the ID or Name.
        firstItemId = TryParseInt(config.firstItemIdentifier());
        secondItemId = TryParseInt(config.secondItemIdentifier());
        thirdItemId = TryParseInt(config.thirdItemIdentifier());
        fourthItemId = TryParseInt(config.fourthItemIdentifier());
        firstIdentity = firstItemId != null ? "identified by ID" : "identified by name";
        secondIdentity = secondItemId != null ? "identified by ID" : "identified by name";
        thirdIdentity = thirdItemId != null ? "identified by ID" : "identified by name";
        fourthIdentity = fourthItemId != null ? "identified by ID" : "identified by name";

        inventorySlots = calculateInteractOrder(new ArrayList<>(Rs2Inventory.items()), config.interactOrder()).iterator();

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            if (!Microbot.isLoggedIn()) return;
            if (!super.run()) return;
            try {
                //start
                combineItems();

            } catch (Exception ex) {
                ex.printStackTrace();
                Microbot.log(ex.getMessage());
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
        return true;
    }

    private boolean hasItems() {
        // Check if the player has the required quantity of both items using the configuration
        if (config.firstItemQuantity() > 0 && config.secondItemQuantity() > 0 && config.thirdItemQuantity() > 0 && config.fourthItemQuantity() > 0) {
            System.out.println("Checking all items.");
            return firstItemId == null ? Rs2Inventory.hasItem(config.firstItemIdentifier()) : Rs2Inventory.hasItem(firstItemId) &&
                    secondItemId == null ? Rs2Inventory.hasItem(config.secondItemIdentifier()) : Rs2Inventory.hasItem(secondItemId) &&
                    thirdItemId == null ? Rs2Inventory.hasItem(config.thirdItemIdentifier()) : Rs2Inventory.hasItem(thirdItemId) &&
                    fourthItemId == null ? Rs2Inventory.hasItem(config.fourthItemIdentifier()) : Rs2Inventory.hasItem(fourthItemId);
        } else if (config.firstItemQuantity() > 0 && config.secondItemQuantity() > 0 && config.thirdItemQuantity() > 0) {
            System.out.println("Checking first, second, and third item.");
            return firstItemId == null ? Rs2Inventory.hasItem(config.firstItemIdentifier()) : Rs2Inventory.hasItem(firstItemId) &&
                    secondItemId == null ? Rs2Inventory.hasItem(config.secondItemIdentifier()) : Rs2Inventory.hasItem(secondItemId) &&
                    thirdItemId == null ? Rs2Inventory.hasItem(config.thirdItemIdentifier()) : Rs2Inventory.hasItem(thirdItemId);
        } else if (config.firstItemQuantity() > 0 && config.secondItemQuantity() > 0) {
            System.out.println("Checking first, and second item.");
            return firstItemId == null ? Rs2Inventory.hasItem(config.firstItemIdentifier()) : Rs2Inventory.hasItem(firstItemId) &&
                    secondItemId == null ? Rs2Inventory.hasItem(config.secondItemIdentifier()) : Rs2Inventory.hasItem(secondItemId);
        } else if (config.firstItemQuantity() > 0) {
            System.out.println("Everything else empty, checking first item.");
            return firstItemId == null ? Rs2Inventory.hasItem(config.firstItemIdentifier()) : Rs2Inventory.hasItem(firstItemId);
        }  else {
            System.out.println("Something may have gone wrong.");
            return false;
        }
    }


    private String fetchItems() {
        if (config.pause()) {
            while (this.isRunning() && config.pause()) {
                if (!config.pause() || !this.isRunning()) { break; }
                sleep(100, 1000);
            }
        }
        if (currentStatus != CurrentStatus.FETCH_SUPPLIES) { currentStatus = CurrentStatus.FETCH_SUPPLIES; }
        sleep(calculateSleepDuration(1));
        if (!hasItems()) {
            if (!Rs2Bank.isOpen()) {
                Rs2Bank.openBank();
            }
            sleep = sleepUntilTrue(() -> Rs2Bank.isOpen(), Rs2Random.between(67, 97), 18000);
            sleep(calculateSleepDuration(1));
            inventorySlots = null;
            depositUnwantedItems(config.firstItemIdentifier(), config.firstItemQuantity());
            depositUnwantedItems(config.secondItemIdentifier(), config.secondItemQuantity());
            depositUnwantedItems(config.thirdItemIdentifier(), config.thirdItemQuantity());
            depositUnwantedItems(config.fourthItemIdentifier(), config.fourthItemQuantity());

            // Checking that we have enough items in the bank
            String missingItem = checkItemSums();
            if (!missingItem.isEmpty()) {
                return missingItem;
            }
            getXItem(config.firstItemIdentifier(), config.firstItemQuantity());
            getXItem(config.secondItemIdentifier(), config.secondItemQuantity());
            getXItem(config.thirdItemIdentifier(), config.thirdItemQuantity());
            getXItem(config.fourthItemIdentifier(), config.fourthItemQuantity());

            // Checking that we have our items, and tallying a summary for the overlay.
            if (hasItems()) {
                previousItemChange = (System.currentTimeMillis() - 2500);
                if (firstItemSum == 0) {
                    firstItemSum = firstItemId != null ? (Rs2Bank.bankItems.stream().filter(item -> item.id == firstItemId).mapToInt(item -> item.quantity).sum() + Rs2Inventory.count(firstItemId)) : (Rs2Bank.count(config.firstItemIdentifier()) + Rs2Inventory.count(config.firstItemIdentifier()));
                }
                if (config.secondItemQuantity() > 0 && secondItemSum == 0) {
                    secondItemSum = secondItemId != null ? (Rs2Bank.bankItems.stream().filter(item -> item.id == secondItemId).mapToInt(item -> item.quantity).sum() + Rs2Inventory.count(secondItemId)) : (Rs2Bank.count(config.secondItemIdentifier()) + Rs2Inventory.count(config.secondItemIdentifier()));
                }
                if (config.thirdItemQuantity() > 0 && thirdItemSum == 0) {
                    thirdItemSum = thirdItemId != null ? (Rs2Bank.bankItems.stream().filter(item -> item.id == thirdItemId).mapToInt(item -> item.quantity).sum() + Rs2Inventory.count(thirdItemId)) : (Rs2Bank.count(config.thirdItemIdentifier()) + Rs2Inventory.count(config.thirdItemIdentifier()));
                }
                if (config.fourthItemQuantity() > 0 && fourthItemSum == 0) {
                    fourthItemSum = fourthItemId != null ? (Rs2Bank.bankItems.stream().filter(item -> item.id == fourthItemId).mapToInt(item -> item.quantity).sum() + Rs2Inventory.count(fourthItemId)) : (Rs2Bank.count(config.fourthItemIdentifier()) + Rs2Inventory.count(config.fourthItemIdentifier()));
                }

                // added this code because I noticed high latency worlds could cause the script to miss the bank closing, causing it to get stuck in a loop.
                long bankCloseTime = System.currentTimeMillis();
                sleep = false;
                while (this.isRunning() && !sleep
                        && (System.currentTimeMillis() - bankCloseTime < 1800)) {
                    Rs2Bank.closeBank();
                    timeValue = System.currentTimeMillis();
                    sleep = sleepUntilTrue(() -> !Rs2Bank.isOpen(), Rs2Random.between(60, 97), 600);
                }
                randomNum = calculateSleepDuration(1)-10;
                if (System.currentTimeMillis()-timeValue<randomNum) { sleep((int) (randomNum-(System.currentTimeMillis()-timeValue))); } else { sleep(Rs2Random.between(14, 28)); }

                inventorySlots = calculateInteractOrder(new ArrayList<>(Rs2Inventory.items()), config.interactOrder()).iterator();
                //this is a fail-safe for in case anything whatsoever prevents the script from closing the bank, that it will log out to be safe.
                if (!sleep) {
                    sleep(calculateSleepDuration(1));
                    if (this.isRunning()) { Rs2Player.logout(); }
                    sleep(calculateSleepDuration(1));
                }
                currentStatus = CurrentStatus.COMBINE_ITEMS;
                return "";
            }
        }
        return "";
    }
    private boolean combineItems() {
        if (!hasItems()) {
            String missingItem = fetchItems();
            if (!missingItem.isEmpty()) {
                Microbot.showMessage("Insufficient " + missingItem);
                while (this.isRunning()) {
                    if (hasItems()) {
                        break;
                    }
                    sleep(300, 3000);
                }
            }
            return false;
        }
        // this is to prevent unintended behaviour when the script is started with the bank open.
        if (Rs2Bank.isOpen()) {
            Rs2Bank.closeBank();
            sleep = sleepUntilTrue(() -> !Rs2Bank.isOpen(), Rs2Random.between(60, 97), 5000);
            sleep(calculateSleepDuration(1));
            return false;
        }
        // We loop through executing this method "combineItems()", so we want to force return to do nothing while we wait for processing.
        if (config.waitForAnimation()) {
            if (Rs2Player.isAnimating() || (System.currentTimeMillis() - previousItemChange) < 2400) { return false; }
        }

        if (currentStatus != CurrentStatus.COMBINE_ITEMS) { currentStatus = CurrentStatus.COMBINE_ITEMS; }

        // This just allows us to pause the script so that we don't lose our overlay.
        if (config.pause()) {
            while (this.isRunning() && config.pause()) {
                if (!config.pause()){ break; }
                sleep(100,1000);
            }
        }

        // using our items from the config string and the selected interaction order.
        timeValue = System.currentTimeMillis();
        interactOrder(config.firstItemIdentifier());
        randomNum = calculateSleepDuration(0.5);
        if (System.currentTimeMillis()-timeValue<randomNum) { sleep((int) (randomNum-(System.currentTimeMillis()-timeValue))); } else { sleep(Rs2Random.between(14, 28)); }
        if (config.secondItemQuantity() > 0) {
            timeValue = System.currentTimeMillis();
            interactOrder(config.secondItemIdentifier());
            randomNum = calculateSleepDuration(0.5);
            if (System.currentTimeMillis()-timeValue<randomNum) { sleep((int) (randomNum-(System.currentTimeMillis()-timeValue))); } else { sleep(Rs2Random.between(14, 28)); }
        }

        // When the config option is enabled, we interact with the popup when processing items.
        if (config.needPromptEntry()) {
            sleep(calculateSleepDuration(1));
            isWaitingForPrompt = true;
            sleep = sleepUntilTrue(() -> !isWaitingForPrompt, Rs2Random.between(7, 31), Rs2Random.between(800, 1200));
            Rs2Keyboard.keyPress(KeyEvent.VK_SPACE);
            previousItemChange = System.currentTimeMillis();
            if (secondItemId != null) {
                sleep = sleepUntilTrue(() -> !Rs2Inventory.hasItem(secondItemId), 30, 40000);
            } else {
                sleep = sleepUntilTrue(() -> !Rs2Inventory.hasItem(config.secondItemIdentifier()), 30, 40000);
            }
            sleep(calculateSleepDuration(1));
        }
        return true;
    }
    //TODO sonofabech I forgot to look this over to see how it works
    // If this gets added to Rs2Inventory, it's going to need String menu at the very least, and TryParseInt would also be nice.
    // calculateInteractOrder() returns a list, we need to calculate this list when we close the bank. and then iterate through it each time this method gets called.
    public void interactOrder(String item) {
        Integer itemID = TryParseInt(item);
        Rs2Item nextItem;
        if (inventorySlots.hasNext()) {
            nextItem = inventorySlots.next();
        } else {
            inventorySlots = calculateInteractOrder(new ArrayList<>(Rs2Inventory.items()), config.interactOrder()).iterator();
            nextItem = inventorySlots.next();
        }
        long start = System.currentTimeMillis();
        if (itemID == null) {
            if(!Objects.equals(nextItem.name, item)) {
                while (!Objects.equals(nextItem.name, item) && (System.currentTimeMillis() - start < 800)) {
                    nextItem = inventorySlots.next();
                    if (!inventorySlots.hasNext()) {
                        inventorySlots = calculateInteractOrder(new ArrayList<>(Rs2Inventory.items()), config.interactOrder()).iterator();
                    }
                }
            }
            if (Objects.equals(Rs2Inventory.getNameForSlot(nextItem.getSlot()), item)) {
                Rs2Inventory.interact(nextItem, config.menu());
            } else {
                while (!Objects.equals(nextItem.name, item) && (System.currentTimeMillis() - start < 800)) {
                    nextItem = inventorySlots.next();
                    if (!inventorySlots.hasNext()) {
                        inventorySlots = calculateInteractOrder(new ArrayList<>(Rs2Inventory.items()), config.interactOrder()).iterator();
                        nextItem = inventorySlots.next();
                    }
                }
                Rs2Inventory.interact(nextItem, config.menu());
            }
        } else {
            if ((nextItem.id != itemID)) {
                while ((nextItem.id != itemID) && (System.currentTimeMillis() - start < 800)) {
                    nextItem = inventorySlots.next();
                    if (!inventorySlots.hasNext()) {
                        inventorySlots = calculateInteractOrder(new ArrayList<>(Rs2Inventory.items()), config.interactOrder()).iterator();
                    }
                }
            }
            if (Rs2Inventory.getIdForSlot(nextItem.getSlot()) != itemID) {
                Rs2Inventory.interact(nextItem, config.menu());
            } else {
                while (nextItem.id != itemID && (System.currentTimeMillis() - start < 800)) {
                    nextItem = inventorySlots.next();
                    if (!inventorySlots.hasNext()) {
                        inventorySlots = calculateInteractOrder(new ArrayList<>(Rs2Inventory.items()), config.interactOrder()).iterator();
                        nextItem = inventorySlots.next();
                    }
                }
                Rs2Inventory.interact(nextItem, config.menu());
            }
        }
    }

    private int calculateSleepDuration(double multiplier) {
        // Create a Random object
        Random random = new Random();

        // Calculate the mean (average) of sleepMin and sleepMax, adjusted by sleepTarget
        double mean = (sleepMin + sleepMax + sleepTarget) / 3.0;

        // Calculate the standard deviation with added noise
        double noiseFactor = 0.2; // Adjust the noise factor as needed (0.0 to 1.0)
        double stdDeviation = Math.abs(sleepTarget - mean) / 3.0 * (1 + noiseFactor * (random.nextDouble() - 0.5) * 2);

        // Generate a random number following a normal distribution
        int sleepDuration;
        do {
            // Generate a random number using nextGaussian method, scaled by standard deviation
            sleepDuration = (int) Math.round(mean + random.nextGaussian() * stdDeviation);
        } while (sleepDuration < sleepMin || sleepDuration > sleepMax); // Ensure the duration is within the specified range
        if ((int) Math.round(sleepDuration * multiplier) < 60) sleepDuration += ((60-sleepDuration)+Rs2Random.between(11,44));
        return sleepDuration;
    }
    public String checkItemSums(){
        if(!Rs2Bank.isOpen()){
            Rs2Bank.openBank();
            sleep = sleepUntilTrue(Rs2Bank::isOpen, Rs2Random.between(67, 97), 18000);
            sleep(200, 600);
        }

        if (firstItemId != null && ((Rs2Bank.bankItems.stream().filter(item -> item.id == firstItemId).mapToInt(item -> item.quantity).sum() + Rs2Inventory.count(firstItemId))) < config.firstItemQuantity()) {
            return firstItemId.toString();
        } else if (firstItemId == null && (Rs2Bank.count(config.firstItemIdentifier()) + Rs2Inventory.count(config.firstItemIdentifier())) < config.firstItemQuantity()) {
            return config.firstItemIdentifier();
        }

        if (config.secondItemQuantity() > 0 && !config.secondItemIdentifier().isEmpty()) {
            if (secondItemId != null && ((Rs2Bank.bankItems.stream().filter(item -> item.id == secondItemId).mapToInt(item -> item.quantity).sum() + Rs2Inventory.count(secondItemId))) < config.secondItemQuantity()) {
                return secondItemId.toString();
            } else if (secondItemId == null && (Rs2Bank.count(config.secondItemIdentifier()) + Rs2Inventory.count(config.secondItemIdentifier())) < config.secondItemQuantity()) {
                return config.secondItemIdentifier();
            }
        }
        if (config.thirdItemQuantity() > 0 && !config.thirdItemIdentifier().isEmpty()) {

            if (thirdItemId != null && ((Rs2Bank.bankItems.stream().filter(item -> item.id == thirdItemId).mapToInt(item -> item.quantity).sum() + Rs2Inventory.count(thirdItemId))) < config.thirdItemQuantity()) {
                return thirdItemId.toString();
            } else if (thirdItemId == null && (Rs2Bank.count(config.thirdItemIdentifier()) + Rs2Inventory.count(config.thirdItemIdentifier())) < config.thirdItemQuantity()) {
                return config.thirdItemIdentifier();
            }
        }
        if (config.fourthItemQuantity() > 0 && !config.fourthItemIdentifier().isEmpty()) {

            if (fourthItemId != null && ((Rs2Bank.bankItems.stream().filter(item -> item.id == fourthItemId).mapToInt(item -> item.quantity).sum() + Rs2Inventory.count(fourthItemId))) < config.fourthItemQuantity()) {
                return fourthItemId.toString();
            } else if (fourthItemId == null && (Rs2Bank.count(config.fourthItemIdentifier()) + Rs2Inventory.count(config.fourthItemIdentifier())) < config.fourthItemQuantity()) {
                return config.fourthItemIdentifier();
            }
        }
        return "";
    }
    private void getXItem(String item, int amount) {
        if (amount > 0) {
            Integer id = TryParseInt(item);

            // calculates the quantity we need to withdraw in case of any bugs, so we don't get stuck in a loop from any bugs.
            int missingQuantity = (id != null) ? ((Rs2Inventory.count(id) < amount)
                    ? amount - Rs2Inventory.count(id)
                    : 0)
                    : ((Rs2Inventory.count(item) < amount)
                    ? amount - Rs2Inventory.count(item)
                    : 0);
            // just for efficiency, there's no point running everything else if we already have this item.
            if (missingQuantity > 0) {
                // watching our time immediately before attempting to withdraw an item, so we can keep our sleep timer within an expected range when not our last item.
                timeValue = System.currentTimeMillis();
                if (!config.withdrawAll()) {
                    if (id != null) {
                        Rs2Bank.withdrawX(true, id, missingQuantity);
                    } else {
                        Rs2Bank.withdrawX(true, item, missingQuantity);
                    }
                } else {
                    if (id != null) {
                        Rs2Bank.withdrawAll(id);
                    } else {
                        Rs2Bank.withdrawAll(item);
                    }
                }
                // code here is checking that we've withdrawn our last item from the bank before we wait for it to be in our inventory before we attempt to close the bank.
                int lastItem = (config.fourthItemQuantity() > 0) ? 4 : (config.thirdItemQuantity() > 0) ? 3 : (config.secondItemQuantity() > 0) ? 2 : 1;
                if (lastItem == 4 && Objects.equals(item, config.fourthItemIdentifier()) ||
                        lastItem == 3 && Objects.equals(item, config.thirdItemIdentifier()) ||
                        lastItem == 2 && Objects.equals(item, config.secondItemIdentifier()) ||
                        lastItem == 1) {
                    if (id != null) {
                        sleepUntilTrue(() -> Rs2Inventory.hasItemAmount(id, amount), 40, 1800);
                    } else {
                        sleepUntilTrue(() -> Rs2Inventory.hasItemAmount(item, amount), 40, 1800);
                    }
                }

                // setting and executing our calculated action delay.
                randomNum = calculateSleepDuration(1);
                if (System.currentTimeMillis() - timeValue < randomNum) {
                    sleep((int) (randomNum - (System.currentTimeMillis() - timeValue)));
                } else {
                    sleep(Rs2Random.between(14, 48));
                }
            }
        }
    }

    private void depositUnwantedItems(String item, int quantityMax){
        Integer id = TryParseInt(item);
        if (config.depositAll() && Rs2Inventory.getEmptySlots() < 28) {
            timeValue = System.currentTimeMillis();
            Rs2Bank.depositAll();
            sleepUntilTrue(() -> Rs2Inventory.getEmptySlots() == 28,40, 1800);
            randomNum = calculateSleepDuration(1);
            if (System.currentTimeMillis() - timeValue < randomNum) {
                sleep((int) (randomNum - (System.currentTimeMillis() - timeValue)));
            } else {
                sleep(Rs2Random.between(14, 48));
            }
            // we check that the inventory changes in case the player's bank is full so that we don't cause an unintentional loop.
            // also since we're using the method "logout()", we'll also use "this.isRunning()" so people can avoid the logout by turning the plugin off.
            if (this.isRunning() && Rs2Inventory.getEmptySlots() < 28) {
                Microbot.showMessage("Bank is full, unable to deposit items.");
                long start = System.currentTimeMillis();
                while (this.isRunning() && ((System.currentTimeMillis()-start) < 120000) && Rs2Inventory.getEmptySlots() < 28) {
                    sleepUntilTrue(() -> Rs2Inventory.getEmptySlots() == 28,1800, 3600);
                    if (Rs2Inventory.getEmptySlots() == 28) sleep(10000);
                }
                if (this.isRunning() && Rs2Inventory.getEmptySlots() < 28) {
                    sleep(calculateSleepDuration(1));
                    Rs2Player.logout();
                    sleep(calculateSleepDuration(1));
                }
            }
        }

        // here is checking for excess items in the inventory
        boolean excessItems = false;
        timeValue = System.currentTimeMillis();
        if (id != null && quantityMax < Rs2Inventory.count(id)) {
            excessItems = true;
            Rs2Bank.depositAll(id);
            sleepUntilTrue(() -> Rs2Inventory.hasItem(id), 40, 1800);
        } else if (id == null && quantityMax < Rs2Inventory.count(item)) {
            excessItems = true;
            Rs2Bank.depositAll(item);
            sleepUntilTrue(() -> Rs2Inventory.hasItem(item), 40, 1800);
        }
        if (excessItems) {
            randomNum = calculateSleepDuration(1);
            if (System.currentTimeMillis() - timeValue < randomNum) {
                sleep((int) (randomNum - (System.currentTimeMillis() - timeValue)));
            } else {
                sleep(Rs2Random.between(14, 48));
            }
        }

        // and here we just deposit anything that doesn't match our item entries
        List<Integer> bankExcept = new ArrayList<>();
        if (config.fourthItemQuantity() > 0) { if (fourthItemId != null) { if (Rs2Inventory.hasItem(fourthItemId)) { bankExcept.add(fourthItemId); } } else { if (Rs2Inventory.hasItem(config.fourthItemIdentifier())) { bankExcept.add(Rs2Inventory.get(config.fourthItemIdentifier()).id); } } }
        if (config.thirdItemQuantity() > 0) { if (thirdItemId != null) { if (Rs2Inventory.hasItem(thirdItemId)) { bankExcept.add(thirdItemId); } } else { if (Rs2Inventory.hasItem(config.thirdItemIdentifier())) { bankExcept.add(Rs2Inventory.get(config.thirdItemIdentifier()).getId()); } } }
        if (config.secondItemQuantity() > 0) { if (secondItemId != null) { if (Rs2Inventory.hasItem(secondItemId)) { bankExcept.add(secondItemId); } } else { if (Rs2Inventory.hasItem(config.secondItemIdentifier())) { bankExcept.add(Rs2Inventory.get(config.secondItemIdentifier()).getId()); } } }
        if (config.firstItemQuantity() > 0) { if (firstItemId != null) { if (Rs2Inventory.hasItem(firstItemId)) { bankExcept.add(firstItemId); } } else { if (Rs2Inventory.hasItem(config.firstItemIdentifier())) { bankExcept.add(Rs2Inventory.get(config.firstItemIdentifier()).getId()); } } }
        if (Rs2Inventory.getEmptySlots() < 28) {
            if (!bankExcept.isEmpty()) {
                Rs2Bank.depositAllExcept(bankExcept.toArray(new Integer[0]));
            } else {
                Rs2Bank.depositAll();
            }
            sleep(calculateSleepDuration(1));
        }
    }
    // method to parse string to integer, returns null if parsing fails
    public static Integer TryParseInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            System.out.println("Could not Parse Int from Item, using Name Instead");
            return null;
        }
    }
}
