package net.runelite.client.plugins.microbot.moonsOfPeril.handlers;

import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.moonsOfPeril.enums.State;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.moonsOfPeril.moonsOfPerilConfig;

import static net.runelite.client.plugins.microbot.util.Global.sleep;
import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

public class ResupplyHandler implements BaseHandler {


    private final int minMoonlightPotions;
    private final int minCookedBream;
    private final int potionBatchSize;

    public ResupplyHandler(moonsOfPerilConfig cfg) {
        this.minMoonlightPotions = cfg.moonlightPotionsMinimum();
        this.minCookedBream      = cfg.cookedBreamMinimum();
        this.potionBatchSize     = cfg.moonlightPotionsQuantum();
    }

    /* ========== BaseHandler contract ================================== */

    @Override
    public boolean validate() {
        return checkSupplies(minMoonlightPotions, minCookedBream);
    }

    @Override
    public State execute() {
        walkToSupplies();
        makeMoonlightPotions(potionBatchSize);
        obtainBream();
        // add any “return to boss room” walking here if needed
        return State.IDLE;
    }

    private boolean checkSupplies(int moonlightPotionMinimum, int cookedBreamMinimum) {
        if ((countMoonlightPotions() < moonlightPotionMinimum)
                || (Rs2Inventory.count(ItemID.BREAM_FISH_COOKED) < cookedBreamMinimum)) {
            return true;           // triggers resupply
        } else {
            Microbot.log("No need to resupply right now.");
        }
        return false;
    }

    private void walkToSupplies() {
        Microbot.log("Attempting to walk to supply area 1");
        Rs2Walker.walkTo(1513, 9692, 0, 0);
        Microbot.log("Arrived at supply area 1");
    }

    private void makeMoonlightPotions(int moonlightPotionsQuantum) {
        Microbot.log("Need a total of " + moonlightPotionsQuantum + " Moonlight potions");
        int amountToCreate = checkPotionQuantum(moonlightPotionsQuantum);
        Microbot.log("Need to create " + amountToCreate + " Moonlight potions");

        if (amountToCreate > 0) {
            /* Take herblore supplies */
            while (Rs2Inventory.count(ItemID.VIAL_WATER) < amountToCreate) {
                Microbot.log("Take herblore supplies from supply crate");
                if (Rs2GameObject.interact(ObjectID.PMOON_SUPPLY_CRATE, "Take from")) {
                    Rs2Dialogue.sleepUntilHasDialogueOption("Take herblore supplies.");
                    Rs2Dialogue.clickOption("Take herblore supplies.");
                    Rs2Inventory.waitForInventoryChanges(1_500);
                }
            }

            /* Forage for grubs */
            while (Rs2Inventory.count(ItemID.MOONLIGHT_GRUB)
                    + Rs2Inventory.count(ItemID.MOONLIGHT_GRUB_PASTE) < amountToCreate) {
                Microbot.log("Collect Moonlight Grub");
                if (Rs2GameObject.interact(ObjectID.PMOON_GRUB_SAPLING, "Collect-from")) {
                    sleepUntil(() -> Rs2Inventory.count(ItemID.MOONLIGHT_GRUB)
                            + Rs2Inventory.count(ItemID.MOONLIGHT_GRUB_PASTE) >= amountToCreate, 8_000);
                }
            }

            /* Grind to paste */
            if (Rs2Inventory.contains(ItemID.PESTLE_AND_MORTAR)) {
                Rs2Inventory.combine(ItemID.PESTLE_AND_MORTAR, ItemID.MOONLIGHT_GRUB);
                sleepUntil(() -> !Rs2Inventory.contains(ItemID.MOONLIGHT_GRUB), 8_000);
            } else {
                Microbot.log("Need a pestle and mortar!");
            }

            /* Mix potion */
            if (Rs2Inventory.combine(ItemID.MOONLIGHT_GRUB_PASTE, ItemID.VIAL_WATER)) {
                sleepUntil(() -> !Rs2Player.isAnimating(), 10_000);
            }

            /* Drop leftovers */
            while (Rs2Inventory.contains(ItemID.PESTLE_AND_MORTAR))  Rs2Inventory.drop(ItemID.PESTLE_AND_MORTAR);
            sleep(600,800);
            while (Rs2Inventory.contains(ItemID.MOONLIGHT_GRUB))      Rs2Inventory.drop(ItemID.MOONLIGHT_GRUB);
            sleep(600,800);
            while (Rs2Inventory.contains(ItemID.MOONLIGHT_GRUB_PASTE))Rs2Inventory.drop(ItemID.MOONLIGHT_GRUB_PASTE);
            sleep(600,800);
            while (Rs2Inventory.contains(ItemID.VIAL_WATER))Rs2Inventory.drop(ItemID.VIAL_WATER);
            sleep(600,800);
        }
    }

    private void obtainBream() {
        fishBream();
        cookBream();
    }

    private void fishBream() {
        if (!Rs2Inventory.contains(ItemID.BIG_NET)) {
            if (Rs2GameObject.interact(ObjectID.PMOON_SUPPLY_CRATE, "Take from")) {
                Rs2Dialogue.sleepUntilHasDialogueOption("Take fishing supplies.");
                Rs2Dialogue.clickOption("Take fishing supplies.");
                Rs2Inventory.waitForInventoryChanges(4_000);
            }
        }
        Rs2Walker.walkFastCanvas(new WorldPoint(1520,9689,0));
        while (!Rs2Inventory.isFull()) {
            if (!Rs2Player.isAnimating()) {
                Rs2GameObject.interact(51367, "Fish"); // restart if animation stopped
                sleep(3000, 4000);
            }
            sleep(300, 500);
        }
        Microbot.log("Inventory should now be full of fish");
        sleep(600, 900);
        // Drop the Big Fishing Net
        Rs2Inventory.drop(ItemID.BIG_NET);
    }

    private void cookBream() {
        Microbot.log("Walking to cooking stove");
        Rs2Walker.walkFastCanvas(new WorldPoint(1512, 9693, 0));

        while (Rs2Inventory.contains(ItemID.BREAM_FISH_RAW)) {
            if (!Rs2Player.isAnimating()) {
                if (Rs2GameObject.interact(ObjectID.PMOON_RANGE, "Cook")) {
                    sleep(600, 900);
                }
            }
            sleep(900, 1200);
        }
        Microbot.log("Finished cooking bream.");
    }

    /* Helpers ---------------------------------------------------------- */

    private int countMoonlightPotions() {
        return Rs2Inventory.count(ItemID._4DOSEMOONLIGHTPOTION)
                + Rs2Inventory.count(ItemID._3DOSEMOONLIGHTPOTION)
                + Rs2Inventory.count(ItemID._2DOSEMOONLIGHTPOTION)
                + Rs2Inventory.count(ItemID._1DOSEMOONLIGHTPOTION);
    }

    /**
     * How many new Moonlight potions should we make?
     *
     * • desired = target – current (unchanged)
     * • Each potion needs 2 free slots  (vial + grub/paste)  +1 for the pestle & mortar
     * • Drop cooked bream first, then raw, until we have enough free slots.
     * • If we can’t free enough space, scale desired down to what fits.
     */
    private int checkPotionQuantum(int target)
    {
        int desired = target - countMoonlightPotions();
        if (desired <= 0) return 0;

        int requiredSlots = desired * 2 + 1;                // 2 per potion + 1 mortar
        Microbot.log("Required free inventory slots: " + requiredSlots);
        int freeSlots     = Rs2Inventory.getEmptySlots();
        Microbot.log("Current free inventory slots: " + freeSlots);

        /* --------- Free inventory space by dropping fish ---------------- */
        while (freeSlots < requiredSlots &&
                (Rs2Inventory.contains(ItemID.BREAM_FISH_COOKED) ||
                        Rs2Inventory.contains(ItemID.BREAM_FISH_RAW)))
        {
            if (Rs2Inventory.contains(ItemID.BREAM_FISH_RAW)) {
                Rs2Inventory.drop(ItemID.BREAM_FISH_RAW);
            } else {
                Rs2Inventory.drop(ItemID.BREAM_FISH_COOKED);
            }
            sleep(400, 600);
            freeSlots = Rs2Inventory.emptySlotCount();
        }

        /* --------- Final sanity: if space is *still* tight, scale down desired potions-- */
        if (freeSlots < requiredSlots) {
            desired = Math.max((freeSlots - 1) / 2, 0); // how many potions fit
        }

        return desired;
    }
}