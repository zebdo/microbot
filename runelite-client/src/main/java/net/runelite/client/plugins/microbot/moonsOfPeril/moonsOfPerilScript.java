package net.runelite.client.plugins.microbot.moonsOfPeril;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;

import java.util.concurrent.TimeUnit;

public class moonsOfPerilScript extends Script {

    public static boolean test = false;

    public boolean run(moonsOfPerilConfig config) {
        Microbot.enableAutoRunOn = false;
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;
                long startTime = System.currentTimeMillis();

                // CODE STARTS HERE
                retrieveTravelItems();
                retrieveDeathItems();
                if (checkSupplies(config.moonlightPotionsMinimum(), config.cookedBreamMinimum())) {
                    walkToSupplies();
                    makeMoonlightPotions(config.moonlightPotionsQuantum());
                    obtainBream();
                    rechargeRunEnergy(new WorldPoint(1520,9689,0));
                }
                shutdown();
                // CODE ENDS HERE

                long endTime = System.currentTimeMillis();
                long totalTime = endTime - startTime;
                System.out.println("Total time for loop " + totalTime);

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        return true;
    }

    private boolean checkSupplies(int moonlightPotionMinimum, int cookedBreamMinimum) {
        if ((countMoonlightPotions() < moonlightPotionMinimum) || (Rs2Inventory.count(ItemID.BREAM_FISH_COOKED) < cookedBreamMinimum)) {
            // change state to resupply;
            return true;
        } else {
            Microbot.log("No need to resupply right now.");
            // change state
        }
        return false;
    }

    private void walkToSupplies() {
        Microbot.log("Attempting to walk to supply area 1");
        Rs2Walker.walkTo(1513, 9692, 0, 0);
        Microbot.log("Arrived at supply area 1");
    }

    private void makeMoonlightPotions(int moonlightPotionsQuantum) {
        Microbot.log("Need to a total of " + moonlightPotionsQuantum + " Moonlight potions");
        int amountToCreate = checkPotionQuantum(moonlightPotionsQuantum);
        Microbot.log("Need to a create " + amountToCreate + " Moonlight potions");

        if (amountToCreate > 0) {
            // Take the herblore supplies from supply crate
            while (Rs2Inventory.count(ItemID.VIAL_WATER) < amountToCreate) {
                Microbot.log("Attempt to obtain herblore supplies from supply crate");
                if (Rs2GameObject.interact(ObjectID.PMOON_SUPPLY_CRATE,"Take from")) {
                    Rs2Dialogue.sleepUntilHasDialogueOption("Take herblore supplies.");
                    Rs2Dialogue.clickOption("Take herblore supplies.");
                    if (Rs2Inventory.waitForInventoryChanges(1500)) {
                        Microbot.log("Herblore Supplies Obtained.");
                    }
                }
            }
            // Forage for herblore supplies
            while (Rs2Inventory.count(ItemID.MOONLIGHT_GRUB) + Rs2Inventory.count(ItemID.MOONLIGHT_GRUB_PASTE) < amountToCreate) {
                Microbot.log("Attempting to collect Moonlight Grub");
                if (Rs2GameObject.interact(ObjectID.PMOON_GRUB_SAPLING,"Collect-from")) {
                    sleepUntil(() -> (Rs2Inventory.count(ItemID.MOONLIGHT_GRUB) + Rs2Inventory.count(ItemID.MOONLIGHT_GRUB_PASTE) > amountToCreate), 8000);
                    Microbot.log("Moonlight Grubs obtained.");
                }
            }
            // Process the supplies into Moonlight paste
            Microbot.log("Attempting to create moonlight grub paste.");
            if (Rs2Inventory.contains(ItemID.PESTLE_AND_MORTAR)) {
                Rs2Inventory.combine(ItemID.PESTLE_AND_MORTAR, ItemID.MOONLIGHT_GRUB);
                sleepUntil(() -> !(Rs2Inventory.hasItem(ItemID.MOONLIGHT_GRUB)), 8000);
                Microbot.log("Moonlight grub paste created");
            } else {
                Microbot.log("You need a pestle and mortar to make this!");
            }

            // Mix the Moonlight paste with the vial of water
            Microbot.log("Attempting to mix moonlight potion.");
            if (Rs2Inventory.combine(ItemID.MOONLIGHT_GRUB_PASTE, ItemID.VIAL_WATER)) {
                sleepUntil(() -> !Rs2Player.isAnimating());
                Microbot.log("Moonlight potions created");
            }

            // Drop excess herblore supplies
            Microbot.log("Attempting to drop excess herblore supplies");
            while (Rs2Inventory.contains(ItemID.PESTLE_AND_MORTAR)) {
                Rs2Inventory.drop(ItemID.PESTLE_AND_MORTAR);
                sleep(200,400);
            }
            while (Rs2Inventory.contains(ItemID.MOONLIGHT_GRUB)) {
                Rs2Inventory.drop(ItemID.MOONLIGHT_GRUB);
                sleep(200,400);
            }
            while (Rs2Inventory.contains(ItemID.MOONLIGHT_GRUB_PASTE)) {
                Rs2Inventory.drop(ItemID.MOONLIGHT_GRUB_PASTE);
                sleep(200,400);
            }
            Microbot.log("All excess herblore supplies should now be dropped");
        }

    }

    private void obtainBream() {
        fishBream();
        cookBream();
    }

    private void fishBream() {
        if (!Rs2Inventory.contains(ItemID.BIG_NET)) {
            Microbot.log("Attempting to obtain fishing supplies from supply crate");
            if (Rs2GameObject.interact(ObjectID.PMOON_SUPPLY_CRATE,"Take from")) {
                Rs2Dialogue.sleepUntilHasDialogueOption("Take fishing supplies.");
                Rs2Dialogue.clickOption("Take fishing supplies.");
                if (Rs2Inventory.waitForInventoryChanges(4000)) {
                    Microbot.log("Big Fishing Net Obtained.");
                }
            }
        }
        Microbot.log("Walking to fishing spot");
        Rs2Walker.walkFastCanvas(new WorldPoint(1520,9689,0));
        while (!Rs2Inventory.isFull()) {
            if (!Rs2Player.isAnimating()) {
                Rs2GameObject.interact(51367, "Fish"); // restart if animation stopped
                sleep(3000, 4000);
            }
            sleep(300, 500);
        }
        Microbot.log("Inventory should now be full of fish");
        // Drop the Big Fishing Net
        Rs2Inventory.drop(ItemID.BIG_NET);
    }

    private void cookBream() {
        Microbot.log("Walking to cooking stove");
        Rs2Walker.walkFastCanvas(new WorldPoint(1512, 9693, 0));

        while (Rs2Inventory.contains(ItemID.BREAM_FISH_RAW)) {

            if (!Rs2Player.isAnimating()) {
                // Not cooking right now – try to restart
                if (Rs2GameObject.interact(ObjectID.PMOON_RANGE, "Cook")) {
                    sleep(600, 900); // human-like pause to start animation
                } else {
                    Microbot.log("Range interact failed; will retry.");
                }
            }

            sleep(300, 500); // small poll interval
        }
        Microbot.log("Finished cooking bream.");
    }

    private void rechargeRunEnergy(WorldPoint WorldPoint) {
        Microbot.log("Walking to cooking stove");
        Rs2Walker.walkFastCanvas(WorldPoint);
        Rs2GameObject.interact(ObjectID.PMOON_RANGE, "Make-cuppa");
    }

    private int countMoonlightPotions() {
        return Rs2Inventory.count(ItemID._4DOSEMOONLIGHTPOTION) + Rs2Inventory.count(ItemID._3DOSEMOONLIGHTPOTION) + Rs2Inventory.count(ItemID._2DOSEMOONLIGHTPOTION) + Rs2Inventory.count(ItemID._1DOSEMOONLIGHTPOTION);
    }

    private int checkPotionQuantum(int moonlightPotionsQuantum) {
        return moonlightPotionsQuantum - countMoonlightPotions();
    }

    private void retrieveTravelItems() {
        if (Rs2Bank.openBank()) {
            Rs2Bank.withdrawOne(ItemID.FIRERUNE);
            Rs2Bank.withdrawOne(ItemID.EARTHRUNE);
            Rs2Bank.withdrawX(ItemID.EARTHRUNE, 2);
            Rs2Bank.closeBank();
        }
    }

    private void retrieveDeathItems() {
        Microbot.log("Attempting to walk back to grave site");
        Rs2Walker.walkTo(1440, 9626, 0, 0);
        Rs2Npc.interact(NpcID.GRAVESTONE_DEFAULT, "Loot");
        // interact with NPC grave site "loot"
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
}