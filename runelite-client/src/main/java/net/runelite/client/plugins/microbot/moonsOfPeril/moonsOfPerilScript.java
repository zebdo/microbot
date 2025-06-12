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
/*                if (retrieveTravelItems()) {
                    Microbot.log("Successfully retrieved travel items.");
                }
                else {
                    Microbot.log("Could not retrieve travel items...shutting down.");
                    shutdown();
                }
                if (retrieveDeathItems()) {
                    Microbot.log("Successfully retrieved death items.");
                }
                else {
                    Microbot.log("Could not retrieve death items...shutting down.");
                    shutdown();
                }*/
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
            /* ---------- Grab vials & pestle and mortar ---------- */
            for (int tries = 0; Rs2Inventory.count(ItemID.VIAL_WATER) < amountToCreate && tries < 4; tries++) {
                Microbot.log("Taking vials (" + (tries + 1) + "/4)");
                if (Rs2GameObject.interact(ObjectID.PMOON_SUPPLY_CRATE, "Take from")) {
                    Rs2Dialogue.sleepUntilHasDialogueOption("Take herblore supplies.");
                    Rs2Dialogue.clickOption("Take herblore supplies.");
                    Rs2Inventory.waitForInventoryChanges(1500);
                }
            }
            /* ---------- Pick grubs ---------- */
            for (int tries = 0; Rs2Inventory.count(ItemID.MOONLIGHT_GRUB)
                    + Rs2Inventory.count(ItemID.MOONLIGHT_GRUB_PASTE) < amountToCreate
                    && tries < 6; tries++) {
                Microbot.log("Collecting grubs (" + (tries + 1) + "/6)");
                if (Rs2GameObject.interact(ObjectID.PMOON_GRUB_SAPLING, "Collect-from")) {
                    sleepUntil(() -> Rs2Inventory.count(ItemID.MOONLIGHT_GRUB)
                                    + Rs2Inventory.count(ItemID.MOONLIGHT_GRUB_PASTE) >= amountToCreate,
                            8_000);
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
            for (int id : new int[]{
                    ItemID.PESTLE_AND_MORTAR,
                    ItemID.MOONLIGHT_GRUB,
                    ItemID.MOONLIGHT_GRUB_PASTE}) {

                while (Rs2Inventory.contains(id)) {
                    Rs2Inventory.drop(id);
                    sleep(200, 400);
                }
            }
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
        /* One click starts fishing; wait up to 3 min for the bag to fill.
        If animation stops early, give it up to four more nudges.        */
        for (int nudges = 0; !Rs2Inventory.isFull() && nudges < 5; nudges++) {
            if (!Rs2Player.isAnimating()) {
                Rs2GameObject.interact(51367, "Fish");
                sleep(600, 900);
            }
            sleepUntil(Rs2Inventory::isFull, 5_000);  // poll every 10 s
        }
        // Drop the Big Fishing Net
        Rs2Inventory.drop(ItemID.BIG_NET);
    }

    private void cookBream() {
        Microbot.log("Walking to cooking spot");
        Rs2Walker.walkFastCanvas(new WorldPoint(1512, 9693, 0));
        for (int i = 0; i < 3 && Rs2Inventory.contains(ItemID.BREAM_FISH_RAW); i++) {
            Microbot.log("Attempting to cook raw bream");
            if (Rs2GameObject.interact(ObjectID.PMOON_RANGE, "Cook")) {
                Microbot.log("Waiting for no more raw bream in inventory");
                sleepUntil(() -> !Rs2Inventory.contains(ItemID.BREAM_FISH_RAW), 15_000);
            }
        }
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

    private boolean retrieveTravelItems() {
        if (Rs2Bank.walkToBank()) {
            if (Rs2Bank.openBank()) {
                sleep(600,800);
                Rs2Bank.withdrawOne(ItemID.FIRERUNE);
                Rs2Inventory.waitForInventoryChanges(600);
                Rs2Bank.withdrawOne(ItemID.EARTHRUNE);
                Rs2Inventory.waitForInventoryChanges(600);
                Rs2Bank.withdrawX(ItemID.LAWRUNE, 2);
                Rs2Inventory.waitForInventoryChanges(600);
                Rs2Bank.closeBank();
            }
        }
        else {
            return false;
        }
        return true;
    }

    private boolean retrieveDeathItems() {
        Microbot.log("Attempting to walk back to grave site");
        Rs2Walker.walkTo(1440, 9626, 0, 0);
        // interact with NPC grave site "loot"
        Rs2Npc.interact(NpcID.GRAVESTONE_DEFAULT, "Loot");
        return Rs2Inventory.waitForInventoryChanges(2000);
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
}

// [{"regionId":5782,"regionX":4,"regionY":32,"z":0,"color":"#FFFF0043","label":"Slash Boss"},{"regionId":5526,"regionX":47,"regionY":29,"z":0,"color":"#FFF2E836","label":"After jaguars"},{"regionId":5526,"regionX":45,"regionY":31,"z":0,"color":"#FFF2E836","label":"1"},{"regionId":5526,"regionX":45,"regionY":33,"z":0,"color":"#FFF2E836","label":"2"},{"regionId":5526,"regionX":51,"regionY":33,"z":0,"color":"#FFF2E836","label":"1"},{"regionId":5526,"regionX":51,"regionY":31,"z":0,"color":"#FFF2E836","label":"2"},{"regionId":5526,"regionX":49,"regionY":35,"z":0,"color":"#FFF2E836","label":"After Rain"},{"regionId":5782,"regionX":32,"regionY":59,"z":0,"color":"#FF00B8D4","label":"crush Boss"},{"regionId":5783,"regionX":31,"regionY":13,"z":0,"color":"#FFF2E836","label":"After tornado"},{"regionId":5783,"regionX":29,"regionY":15,"z":0,"color":"#FFF2E836","label":"1"},{"regionId":5783,"regionX":29,"regionY":17,"z":0,"color":"#FFF2E836","label":"2"},{"regionId":5783,"regionX":35,"regionY":17,"z":0,"color":"#FFF2E836","label":"1"},{"regionId":5783,"regionX":35,"regionY":15,"z":0,"color":"#FFF2E836","label":"2"},{"regionId":5783,"regionX":33,"regionY":19,"z":0,"color":"#FFF2E836","label":"After Frozen Weapons"},{"regionId":5782,"regionX":59,"regionY":32,"z":0,"color":"#FFFF9A00","label":"Stab Boss"},{"regionId":6038,"regionX":15,"regionY":29,"z":0,"color":"#FFF2E836","label":"After Orbit"},{"regionId":6038,"regionX":21,"regionY":37,"z":0,"color":"#FFF2E836"},{"regionId":6038,"regionX":11,"regionY":27,"z":0,"color":"#FFF2E836"},{"regionId":6038,"regionX":11,"regionY":37,"z":0,"color":"#FFF2E836"},{"regionId":6038,"regionX":21,"regionY":27,"z":0,"color":"#FFF2E836"},{"regionId":6038,"regionX":19,"regionY":27,"z":0,"color":"#FFF2E836","label":"walk"},{"regionId":6038,"regionX":17,"regionY":35,"z":0,"color":"#FFF2E836","label":"After Whack a Mole"},{"regionId":6038,"regionX":19,"regionY":33,"z":0,"color":"#FFF2E836","label":"1"},{"regionId":6038,"regionX":19,"regionY":31,"z":0,"color":"#FFF2E836","label":"2"},{"regionId":6038,"regionX":13,"regionY":31,"z":0,"color":"#FFF2E836","label":"1"},{"regionId":6038,"regionX":13,"regionY":33,"z":0,"color":"#FFF2E836","label":"2"},{"regionId":6038,"regionX":16,"regionY":32,"z":0,"color":"#FFF2E836","label":"Whack a Mole"}]