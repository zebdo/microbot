package net.runelite.client.plugins.microbot.SulphurNaguaAIO;

import net.runelite.api.Skill;
import java.util.List;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.inventorysetups.InventorySetup;
import net.runelite.client.plugins.microbot.inventorysetups.InventorySetupsItem;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.antiban.enums.Activity;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.models.RS2Item;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.floor;
import static java.lang.Math.max;

public class SulphurNaguaScript extends Script {

    public static String version = "1.0";

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
    private int droppedPotionCount = 0;
    private boolean isBankingInProgress = false;

    private long lastPotionDrinkTime = 0;

    private final String NAGUA_NAME = "Sulphur Nagua";
    private final int PESTLE_AND_MORTAR_ID = 233;
    private final int VIAL_OF_WATER_ID = 227;
    private final int MOONLIGHT_GRUB_ID = 29078;
    private final int MOONLIGHT_GRUB_PASTE_ID = 29079;

    private final Set<Integer> MOONLIGHT_POTION_IDS = Set.of(
            29080, 29081, 29082, 29083
    );

    private final int SUPPLY_CRATE_ID = 51371;
    private final int GRUB_SAPLING_ID = 51365;

    private final WorldPoint BANK_AREA = new WorldPoint(1452, 9568, 1);
    private final WorldPoint PREPARATION_AREA = new WorldPoint(1376, 9712, 0);
    private final WorldPoint FIGHT_AREA = new WorldPoint(1356, 9565, 0);


    public boolean run(SulphurNaguaConfig config) {
        applyAntiBanSettings();
        Rs2Antiban.setActivity(Activity.GENERAL_COMBAT);

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn() || !super.run()) return;

                Rs2Antiban.takeMicroBreakByChance();

                determineState(config);
                switch (currentState) {
                    case BANKING:
                        handleBanking(config);
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
                        handleFighting(config);
                        break;
                }
            } catch (Exception ex) {
                Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
            }
            // *** ÄNDERUNG: Die Schleife läuft jetzt schneller (alle 300ms statt 600ms), was das Skript reaktionsschneller macht.
        }, 0, 300, TimeUnit.MILLISECONDS);
        return true;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        Rs2Antiban.resetAntibanSettings();
    }

    private void applyAntiBanSettings() {
        Rs2AntibanSettings.antibanEnabled = true;
        Rs2AntibanSettings.usePlayStyle = true;
        Rs2AntibanSettings.simulateFatigue = true;
        Rs2AntibanSettings.simulateAttentionSpan = true;
        Rs2AntibanSettings.behavioralVariability = true;
        Rs2AntibanSettings.nonLinearIntervals = true;
        Rs2AntibanSettings.naturalMouse = true;
        Rs2AntibanSettings.simulateMistakes = true;
        Rs2AntibanSettings.moveMouseOffScreen = true;
        Rs2AntibanSettings.contextualVariability = true;
        Rs2AntibanSettings.devDebug = false;
        Rs2AntibanSettings.playSchedule = true;
        Rs2AntibanSettings.actionCooldownChance = 0.1;
    }

    private void determineState(SulphurNaguaConfig config) {
        if (isBankingInProgress) {
            return;
        }

        boolean hasPestle = Rs2Inventory.hasItem(PESTLE_AND_MORTAR_ID);
        int minPotions = config.moonlightPotionsMinimum();
        if (minPotions == 0) {
            minPotions = 27;
        }

        boolean needsMorePotions = (countMoonlightPotions() + droppedPotionCount) < minPotions;

        if (!needsMorePotions && droppedPotionCount > 0) {
            pickupPending = true;
        }

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

    private void handleBanking(SulphurNaguaConfig config) {
        isBankingInProgress = true;
        try {
            if (!Rs2Bank.isOpen()) {
                Rs2Bank.openBank();
                if (!sleepUntil(Rs2Bank::isOpen, 5000)) {
                    isBankingInProgress = false;
                    return;
                }
            }

            InventorySetup setup = config.useInventorySetup() ? config.inventorySetup() : null;
            if (setup != null) {
                Microbot.log("Using Inventory Setup: " + setup.getName());
                Rs2Bank.depositAll();
                sleepUntil(Rs2Inventory::isEmpty, 2000);
                Rs2Bank.depositEquipment();
                Rs2Random.wait(300, 600);

                if (setup.getEquipment() != null) {
                    for (InventorySetupsItem item : setup.getEquipment()) {
                        if (Rs2Bank.hasItem(item.getId())) {
                            Rs2Bank.withdrawItem(item.getId());
                            sleepUntil(() -> Rs2Inventory.hasItem(item.getId()), 2000);
                        }
                    }
                }

                if (setup.getInventory() != null) {
                    for (InventorySetupsItem item : setup.getInventory()) {
                        final int currentAmount = Rs2Inventory.count(item.getId());
                        final int requiredAmount = item.getQuantity();
                        if (currentAmount < requiredAmount && Rs2Bank.hasItem(item.getId())) {
                            Rs2Bank.withdrawX(item.getId(), requiredAmount - currentAmount);
                            sleepUntil(() -> Rs2Inventory.count(item.getId()) >= requiredAmount, 2000);
                        }
                    }
                }

                if (!Rs2Inventory.hasItem(PESTLE_AND_MORTAR_ID)) {
                    if (Rs2Bank.hasItem(PESTLE_AND_MORTAR_ID)) {
                        Rs2Bank.withdrawItem(PESTLE_AND_MORTAR_ID);
                        sleepUntil(() -> Rs2Inventory.hasItem(PESTLE_AND_MORTAR_ID), 2000);
                    } else {
                        Microbot.showMessage("Pestle and mortar not in bank. Stopping script.");
                        shutdown();
                        return;
                    }
                }
            } else {
                Rs2Bank.depositAll();
                sleep(300, 600);
                if (Rs2Bank.hasItem(PESTLE_AND_MORTAR_ID)) {
                    Rs2Bank.withdrawItem(PESTLE_AND_MORTAR_ID);
                    sleepUntil(() -> Rs2Inventory.hasItem(PESTLE_AND_MORTAR_ID));
                } else {
                    Microbot.showMessage("Pestle and mortar not in bank. Stopping script.");
                    shutdown();
                    return;
                }
            }

            if (Rs2Bank.isOpen()) {
                Rs2Bank.closeBank();
                sleepUntil(() -> !Rs2Bank.isOpen(), 2000);
            }

            if (setup != null && setup.getEquipment() != null) {
                for (InventorySetupsItem item : setup.getEquipment()) {
                    Rs2Inventory.wear(item.getId());
                    sleepUntil(() -> Rs2Equipment.isWearing(item.getId()), 2000);
                    Rs2Random.wait(200, 400);
                }
            }
        } finally {
            if (Rs2Bank.isOpen()) {
                Rs2Bank.closeBank();
            }
            isBankingInProgress = false;
        }
    }

    private void handlePreparation(SulphurNaguaConfig config) {
        if (pickupPending) {
            while (!Rs2Inventory.isFull() && anyPotionOnGround()) {
                if (dropLocation != null && Rs2Player.getWorldLocation().distanceTo(dropLocation) > 5) {
                    Rs2Walker.walkTo(dropLocation);
                    sleep(600, 1000);
                    continue;
                }
                RS2Item potionOnGround = Arrays.stream(Rs2GroundItem.getAll(15))
                        .filter(item -> MOONLIGHT_POTION_IDS.contains(item.getTileItem().getId()))
                        .min(Comparator.comparingInt(item -> Rs2Player.getWorldLocation().distanceTo(item.getTile().getWorldLocation())))
                        .orElse(null);
                if (potionOnGround != null) {
                    int invCountBefore = Rs2Inventory.count();
                    if (Rs2GroundItem.interact(potionOnGround)) {
                        sleepUntil(() -> Rs2Inventory.count() > invCountBefore || Rs2Inventory.isFull(), 3000);
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
            Microbot.log("Aufsammeln abgeschlossen.");
            pickupPending = false;
            dropLocation = null;
            droppedPotionCount = 0;
            return;
        }

        int minPotions = config.moonlightPotionsMinimum() == 0 ? 27 : config.moonlightPotionsMinimum();
        int potionsStillNeeded = minPotions - (countMoonlightPotions() + droppedPotionCount);

        boolean needsToMakeSpace = potionsStillNeeded > 0 && Rs2Inventory.getEmptySlots() < 2 && countMoonlightPotions() > 0;

        if (needsToMakeSpace && dropLocation == null) {
            Microbot.log("Zu wenig Platz für neue Zutaten. Droppe Tränke, um Platz zu schaffen.");
            droppedPotionCount += countMoonlightPotions();
            dropLocation = Rs2Player.getWorldLocation();
            for (int potionId : MOONLIGHT_POTION_IDS) {
                Rs2Inventory.dropAll(potionId);
            }
            sleep(1200);
            return;
        }

        makePotionBatch(config);
    }

    private void handleFighting(SulphurNaguaConfig config) {
        boolean hasPotions = countMoonlightPotions() > 0;

        int basePrayerLevel = Microbot.getClient().getRealSkillLevel(Skill.PRAYER);
        int currentHerbloreLevel = Microbot.getClient().getBoostedSkillLevel(Skill.HERBLORE);
        int currentPrayerPoints = Microbot.getClient().getBoostedSkillLevel(Skill.PRAYER);

        int prayerBasedRestore = (int) floor(basePrayerLevel * 0.25) + 7;
        int herbloreBasedRestore = (int) floor(currentHerbloreLevel * 0.3) + 7;
        int dynamicThreshold = max(prayerBasedRestore, herbloreBasedRestore);

        boolean shouldDrink = currentPrayerPoints < dynamicThreshold || !isMoonlightPotionActive();

        if (hasPotions && shouldDrink) {
            drinkMoonlightPotion();
            sleep(1200);
        }

        if (!Rs2Prayer.isPrayerActive(Rs2PrayerEnum.PROTECT_MELEE)) {
            Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MELEE, true);
        }
        if (config.usePiety() && !Rs2Prayer.isPrayerActive(Rs2PrayerEnum.PIETY)) {
            Rs2Prayer.toggle(Rs2PrayerEnum.PIETY, true);
        }

        if (!Rs2Player.isInCombat()) {
            if (Rs2Npc.interact(NAGUA_NAME, "Attack")) {
                sleepUntil(Rs2Player::isInCombat, 5000);
                if (sleepUntil(() -> !Rs2Player.isInCombat(), 60000)) {
                    totalNaguaKills++;
                    // *** ÄNDERUNG: Die künstliche Antiban-Pause nach einem Kill wurde entfernt.
                    // Rs2Antiban.actionCooldown();
                }
            }
        }
    }

    private void makePotionBatch(SulphurNaguaConfig config) {
        if (Rs2Inventory.hasItem(MOONLIGHT_GRUB_ID)) {
            Rs2Inventory.combine(PESTLE_AND_MORTAR_ID, MOONLIGHT_GRUB_ID);
            // *** ÄNDERUNG: Die lange Wartezeit von 20 Sekunden wurde durch eine kurze, feste Pause ersetzt.
            sleep(600);
            return;
        }
        if (Rs2Inventory.hasItem(MOONLIGHT_GRUB_PASTE_ID) && Rs2Inventory.hasItem(VIAL_OF_WATER_ID)) {
            Rs2Inventory.combine(MOONLIGHT_GRUB_PASTE_ID, VIAL_OF_WATER_ID);
            // *** ÄNDERUNG: Auch hier wurde die lange Wartezeit von 20 Sekunden entfernt.
            sleep(600);
            return;
        }

        int minPotions = config.moonlightPotionsMinimum() == 0 ? 27 : config.moonlightPotionsMinimum();
        int totalPotionsSoFar = countMoonlightPotions() + droppedPotionCount;
        int potionsStillNeeded = minPotions - totalPotionsSoFar;

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
        return Arrays.stream(Rs2GroundItem.getAll(15))
                .anyMatch(item -> MOONLIGHT_POTION_IDS.contains(item.getTileItem().getId()));
    }

    private void drinkMoonlightPotion() {
        // 1. Get a list of all inventory items. The new API uses 'Rs2ItemModel'.
        List<Rs2ItemModel> inventoryItems = Rs2Inventory.all();

        // 2. Loop through the items in the order of the inventory slots.
        for (net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel item : inventoryItems) {
            if (item != null) {
                // 3. Check if the item is a moonlight potion using 'item.id' (the new way).
                if (MOONLIGHT_POTION_IDS.contains(item.getId())) {

                    // 4. Drink the first one found and then stop.
                    if (Rs2Inventory.interact(item.getId(), "Drink")) {
                        lastPotionDrinkTime = System.currentTimeMillis();
                        break; // This stops the loop so you only drink one.
                    }
                }
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
        return (System.currentTimeMillis() - lastPotionDrinkTime) < 300000;
    }
}