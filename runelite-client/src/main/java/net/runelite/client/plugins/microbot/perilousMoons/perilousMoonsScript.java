package net.runelite.client.plugins.microbot.perilousMoons;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.util.concurrent.TimeUnit;

public class perilousMoonsScript extends Script {

    public static boolean test = false;

    public boolean run(perilousMoonsConfig config) {
        Microbot.enableAutoRunOn = false;
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;
                long startTime = System.currentTimeMillis();

                // CODE STARTS HERE
                walktToSupplies();
                int moonlightPotionsQuantum = config.moonlightPotionsQuantum();
                makeMoonlightPotions(moonlightPotionsQuantum);
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

    private void walktToSupplies() {
        Microbot.log("Attempting to walk to supply area 1");
        Rs2Walker.walkTo(1513, 9692, 0, 0);
        Microbot.log("Arrived at supply area 1");
    }

    private void makeMoonlightPotions(int moonlightPotionsQuantum) {
        Microbot.log("Need to a total of " + moonlightPotionsQuantum + " Moonlight potions");
        int amountToCreate = checkPotionQuantum(moonlightPotionsQuantum);
        Microbot.log("Need to a create " + amountToCreate + " Moonlight potions");
        // Take the herblore supplies from supply crate
        while (Rs2Inventory.count(ItemID.VIAL_WATER) < 2 * amountToCreate) {
            Microbot.log("Attempt to obtain herblore supplies from supply crate");
            Rs2GameObject.interact(ObjectID.PMOON_SUPPLY_CRATE,"Take from");
            sleep(2000);
            Rs2Widget.clickWidget("Take herblore supplies.");
            sleepUntil(() -> (Rs2Inventory.hasItem(ItemID.PESTLE_AND_MORTAR) && (Rs2Inventory.count(ItemID.VIAL_WATER) >= 2)), 8000);
            Microbot.log("Herblore Supplies Obtained.");
            sleep(2000);
        }
        // Forage for herblore supplies
        while (Rs2Inventory.count(ItemID.MOONLIGHT_GRUB) + Rs2Inventory.count(ItemID.MOONLIGHT_GRUB_PASTE) < 2 * amountToCreate) {
            Microbot.log("Attempting to collect Moonlight Grub");
            Rs2GameObject.interact(ObjectID.PMOON_GRUB_SAPLING,"Collect-from");
            sleepUntil(() -> (Rs2Inventory.count(ItemID.MOONLIGHT_GRUB) + Rs2Inventory.count(ItemID.MOONLIGHT_GRUB_PASTE) > 2 * amountToCreate), 8000);
            Microbot.log("Moonlight Grubs obtained.");
        }
        // Process the supplies into Moonlight paste
        Microbot.log("Attempting to create moonlight grub paste.");
        Rs2Inventory.combine(ItemID.PESTLE_AND_MORTAR, ItemID.MOONLIGHT_GRUB);
        sleepUntil(() -> !(Rs2Inventory.hasItem(ItemID.MOONLIGHT_GRUB)), 8000);
        Microbot.log("Moonlight grub paste created");
        // Mix the Moonlight paste with the vial of water
        Microbot.log("Attempting to mix moonlight potion.");
        Rs2Inventory.combine(ItemID.MOONLIGHT_GRUB_PASTE, ItemID.VIAL_WATER);
        sleepUntil(() -> !Rs2Player.isAnimating());
        Microbot.log("Moonlight potions created");

    }

    private int checkPotionQuantum(int moonlightPotionsQuantum) {
        int startingAmount = Rs2Inventory.count(ItemID._4DOSEMOONLIGHTPOTION);
        int amountToCreate = moonlightPotionsQuantum - startingAmount;
        return amountToCreate;
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
}