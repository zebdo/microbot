package net.runelite.client.plugins.microbot.SulphurNaguaAIO;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.Set;
import java.util.concurrent.TimeUnit;

public class SulphurNaguaScript extends Script {

    public static String version = "5.2"; // Count Potions (not doses)

    public enum SulphurNaguaState {
        BANKING,
        WALKING_TO_BANK,
        WALKING_TO_PREP,
        PREPARATION,
        WALKING_TO_FIGHT,
        FIGHTING
    }

    public SulphurNaguaState currentState = SulphurNaguaState.BANKING;
    public int totalNaguaKills = 0;

    private WorldPoint dropLocation = null;
    private boolean pickupPending = false;
    private int droppedPotionCount = 0; // jetzt Anzahl Items, nicht Dosen

    // --- Item- & Objekt-IDs ---
    private final String NAGUA_NAME = "Sulphur Nagua";
    private final int PESTLE_AND_MORTAR_ID = 233;
    private final int VIAL_OF_WATER_ID = 227;
    private final int VIAL_ID = 229;
    private final int MOONLIGHT_GRUB_ID = 29078;
    private final int MOONLIGHT_GRUB_PASTE_ID = 29079;

    // Potion-IDs (alle Varianten zählen als 1 Potion)
    private final Set<Integer> MOONLIGHT_POTION_IDS = Set.of(
            29080, // Potion(4)
            29081, // Potion(3)
            29082, // Potion(2)
            29083  // Potion(1)
    );

    private final int SUPPLY_CRATE_ID = 51371;
    private final int GRUB_SAPLING_ID = 51365;

    private final WorldPoint BANK_AREA = new WorldPoint(1452, 9568, 1);
    private final WorldPoint PREPARATION_AREA = new WorldPoint(1376, 9712, 0);
    private final WorldPoint FIGHT_AREA = new WorldPoint(1356, 9565, 0);


    public boolean run(SulphurNaguaConfig config) {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn() || !super.run()) return;
                determineState(config);
                switch (currentState) {
                    case BANKING:
                        handleBanking();
                        break;
                    case WALKING_TO_BANK:
                        Rs2Walker.walkTo(BANK_AREA);
                        break;
                    case WALKING_TO_PREP:
                        Rs2Walker.walkTo(PREPARATION_AREA);
                        break;
                    case PREPARATION:
                        handlePreparation(config);
                        break;
                    case WALKING_TO_FIGHT:
                        Rs2Walker.walkTo(FIGHT_AREA);
                        break;
                    case FIGHTING:
                        handleFighting();
                        break;
                }
            } catch (Exception ex) {
                Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
            }
        }, 0, 600, TimeUnit.MILLISECONDS);
        return true;
    }

    private void determineState(SulphurNaguaConfig config) {
        boolean hasPestle = Rs2Inventory.hasItem(PESTLE_AND_MORTAR_ID);
        boolean needsMorePotions = (countMoonlightPotions() + droppedPotionCount) < config.moonlightPotionsMinimum();

        // Diagnose-Logging
        Microbot.log("Status: " + currentState +
                " | Benötige mehr Tränke: " + needsMorePotions +
                " | Aufheben ausstehend: " + pickupPending +
                " | Potions am Boden: " + droppedPotionCount +
                " | Potions im Inventar: " + countMoonlightPotions());

        if (!hasPestle) {
            dropLocation = null;
            pickupPending = false;
            droppedPotionCount = 0;
            if (at(BANK_AREA)) currentState = SulphurNaguaState.BANKING;
            else currentState = SulphurNaguaState.WALKING_TO_BANK;
            return;
        }

        if (needsMorePotions || pickupPending) {
            if (at(PREPARATION_AREA)) currentState = SulphurNaguaState.PREPARATION;
            else currentState = SulphurNaguaState.WALKING_TO_PREP;
            return;
        }

        if (at(FIGHT_AREA)) currentState = SulphurNaguaState.FIGHTING;
        else currentState = SulphurNaguaState.WALKING_TO_FIGHT;
    }

    private void handleBanking() {
        if (!Rs2Bank.isOpen()) {
            Rs2Bank.openBank();
            sleepUntil(Rs2Bank::isOpen);
            return;
        }
        Rs2Bank.depositAll();
        sleep(300, 600);
        if (Rs2Bank.hasItem(PESTLE_AND_MORTAR_ID)) {
            Rs2Bank.withdrawItem(PESTLE_AND_MORTAR_ID);
            sleepUntil(() -> Rs2Inventory.hasItem(PESTLE_AND_MORTAR_ID));
        } else {
            Microbot.showMessage("Pestle and mortar nicht in der Bank. Stoppe Skript.");
            shutdown();
        }
        Rs2Bank.closeBank();
    }


    private void handlePreparation(SulphurNaguaConfig config) {
        if (pickupPending) {
            if (Rs2Inventory.hasItem(VIAL_ID) || Rs2Inventory.hasItem(VIAL_OF_WATER_ID) || Rs2Inventory.hasItem(MOONLIGHT_GRUB_ID) || Rs2Inventory.hasItem(MOONLIGHT_GRUB_PASTE_ID)) {
                Rs2Inventory.dropAll(VIAL_ID, VIAL_OF_WATER_ID, MOONLIGHT_GRUB_ID, MOONLIGHT_GRUB_PASTE_ID);
                sleep(1000);
                return;
            }
            if (Rs2Player.getWorldLocation().distanceTo(dropLocation) > 2) {
                Rs2Walker.walkTo(dropLocation);
                return;
            }
            boolean pickedUpSomething = false;
            for (int potionId : MOONLIGHT_POTION_IDS) {
                if (Rs2GroundItem.pickup(potionId)) {
                    sleepUntil(() -> !Rs2Player.isAnimating(), 2000);
                    pickedUpSomething = true;
                    break;
                }
            }
            if (!pickedUpSomething) {
                if (!anyPotionOnGround()) {
                    pickupPending = false;
                    dropLocation = null;
                    droppedPotionCount = 0;
                }
            }
            return;
        }

        if (Rs2Inventory.isFull() && countMoonlightPotions() > 0 && dropLocation == null) {
            Microbot.log("Inventar ist voll. Droppe erste Ladung Tränke.");
            droppedPotionCount = countMoonlightPotions();
            dropLocation = Rs2Player.getWorldLocation();
            for (int potionId : MOONLIGHT_POTION_IDS) {
                Rs2Inventory.dropAll(potionId);
            }
            sleep(1200);

            if ((countMoonlightPotions() + droppedPotionCount) >= config.moonlightPotionsMinimum()) {
                pickupPending = true;
            }
            return;
        }

        makePotionBatch(config);
    }

    private void handleFighting() {
        if (countMoonlightPotions() > 0 && !isMoonlightPotionActive()) {
            drinkMoonlightPotion();
            sleep(1200);
            return;
        }
        if (!Rs2Prayer.isPrayerActive(Rs2PrayerEnum.PROTECT_MELEE)) {
            Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MELEE, true);
        }
        if (!Rs2Player.isInCombat()) {
            if (Rs2Npc.interact(NAGUA_NAME, "Attack")) {
                sleepUntil(Rs2Player::isInCombat, 5000);
                if (sleepUntil(() -> !Rs2Player.isInCombat(), 60000)) {
                    totalNaguaKills++;
                }
            }
        }
    }

    private void makePotionBatch(SulphurNaguaConfig config) {
        if (Rs2Inventory.hasItem(MOONLIGHT_GRUB_ID)) {
            Rs2Inventory.combine(PESTLE_AND_MORTAR_ID, MOONLIGHT_GRUB_ID);
            sleepUntil(() -> !Rs2Inventory.hasItem(MOONLIGHT_GRUB_ID), 20000);
            return;
        }
        if (Rs2Inventory.hasItem(MOONLIGHT_GRUB_PASTE_ID) && Rs2Inventory.hasItem(VIAL_OF_WATER_ID)) {
            Rs2Inventory.combine(MOONLIGHT_GRUB_PASTE_ID, VIAL_OF_WATER_ID);
            sleepUntil(() -> !Rs2Inventory.hasItem(MOONLIGHT_GRUB_PASTE_ID), 20000);
            return;
        }

        int totalPotionsSoFar = countMoonlightPotions() + droppedPotionCount;
        int potionsStillNeeded = config.moonlightPotionsMinimum() - totalPotionsSoFar;

        if (potionsStillNeeded <= 0) {
            if (dropLocation != null) {
                pickupPending = true;
            }
            return;
        }

        int targetIngredients = Math.min(potionsStillNeeded, Rs2Inventory.getEmptySlots() / 2);
        if (targetIngredients > 0 && Rs2Inventory.count(VIAL_OF_WATER_ID) < targetIngredients) {
            if (Rs2GameObject.interact(SUPPLY_CRATE_ID, "Take from")) {
                sleepUntil(() -> Rs2Dialogue.hasDialogueOption("Take herblore supplies."));
                Rs2Dialogue.clickOption("Take herblore supplies.");
                sleepUntil(() -> Rs2Inventory.count(VIAL_OF_WATER_ID) >= targetIngredients, 3000);
            }
            return;
        }

        int vialsWeHave = Rs2Inventory.count(VIAL_OF_WATER_ID);
        if (vialsWeHave > 0 && Rs2Inventory.count(MOONLIGHT_GRUB_ID) < vialsWeHave) {
            if (Rs2GameObject.interact(GRUB_SAPLING_ID, "Collect-from")) {
                sleepUntil(() -> Rs2Inventory.count(MOONLIGHT_GRUB_ID) >= vialsWeHave || Rs2Inventory.isFull(), 8000);
            }
        }
    }

    private boolean anyPotionOnGround() {
        if (dropLocation != null && Rs2Player.getWorldLocation().distanceTo(dropLocation) > 3) {
            return true;
        }
        for (int potionId : MOONLIGHT_POTION_IDS) {
            if (Rs2GroundItem.exists(potionId, 2)) {
                return true;
            }
        }
        return false;
    }

    private void drinkMoonlightPotion() {
        for (int potionId : MOONLIGHT_POTION_IDS) {
            if (Rs2Inventory.hasItem(potionId)) {
                Rs2Inventory.interact(potionId, "Drink");
                break;
            }
        }
    }

    private boolean at(WorldPoint worldPoint) {
        return Rs2Player.getWorldLocation().distanceTo(worldPoint) < 10;
    }

    private int countMoonlightPotions() {
        int count = 0;
        for (int potionId : MOONLIGHT_POTION_IDS) {
            count += Rs2Inventory.count(potionId);
        }
        return count;
    }

    private boolean isMoonlightPotionActive() {
        return false; // TODO: Status-Check einbauen, wenn Buff erkennbar
    }
}
