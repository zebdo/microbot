package net.runelite.client.plugins.microbot.SulphurNaguaAIO;

import net.runelite.api.NPC;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.Rs2InventorySetup;

import java.util.concurrent.TimeUnit;

public class SulphurNaguaScript extends Script {

    public static String version = "1.2"; // Version erhöht

    public enum SulphurNaguaState {
        BANKING,
        WALKING,
        PREPARATION,
        FIGHTING
    }

    public SulphurNaguaState currentState = SulphurNaguaState.WALKING;
    public int totalNaguaKills = 0;

    private final String NAGUA_NAME = "Sulphur Nagua";
    private final String MOONLIGHT_POTION_NAME = "Moonlight potion";
    private final String PRAYER_POTION_NAME = "Prayer potion"; // Exakter Name ist wichtig
    private final String FOOD_NAME = "Shark"; // Beispiel-Essen, passe es an deins an

    private final WorldPoint NAGUA_AREA = new WorldPoint(1452, 9568, 1);
    private Rs2InventorySetup inventorySetup = null;

    public boolean run(SulphurNaguaConfig config) {
        if (config.useInventorySetup()) {
            inventorySetup = new Rs2InventorySetup(config.inventorySetup(), mainScheduledFuture);
        }

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn() || !super.run()) return;

                determineState(config);

                switch (currentState) {
                    case BANKING:
                        handleBanking(config);
                        break;
                    case WALKING:
                        handleWalking();
                        break;
                    case PREPARATION:
                        handlePreparation(config);
                        break;
                    case FIGHTING:
                        handleFighting(config);
                        break;
                }
            } catch (Exception ex) {
                Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
            }
        }, 0, 600, TimeUnit.MILLISECONDS);
        return true;
    }

    private void determineState(SulphurNaguaConfig config) {
        if (shouldBank(config)) {
            currentState = SulphurNaguaState.BANKING;
        } else if (!isInNaguaArea()) {
            currentState = SulphurNaguaState.WALKING;
        } else if (needsPreparation()) {
            currentState = SulphurNaguaState.PREPARATION;
        } else {
            currentState = SulphurNaguaState.FIGHTING;
        }
    }

    // --- HANDLER-METHODEN (ÜBERARBEITET) ---

    private void handleBanking(SulphurNaguaConfig config) {
        if (!Rs2Bank.isOpen()) {
            Rs2Bank.openBank();
            sleepUntil(Rs2Bank::isOpen);
            return;
        }

        if (config.useInventorySetup() && inventorySetup != null) {
            if (!inventorySetup.loadInventory() || !inventorySetup.loadEquipment()) {
                Microbot.showMessage("Inventar-Setup konnte nicht geladen werden. Stoppe Skript.");
                shutdown();
            }
        } else {
            // ----- HIER IST DIE KORREKTUR -----
            // Wenn kein Setup verwendet wird, führe diese Standard-Bank-Logik aus.
            Rs2Bank.depositAll();
            sleep(300, 600);

            // Prüfe, ob die Items in der Bank sind, bevor du sie abhebst.
            if (!Rs2Bank.hasItem(MOONLIGHT_POTION_NAME) || !Rs2Bank.hasItem(PRAYER_POTION_NAME) || !Rs2Bank.hasItem(FOOD_NAME)) {
                Microbot.showMessage("Benötigte Items nicht in der Bank gefunden. Stoppe Skript.");
                shutdown();
                return;
            }

        }

        Rs2Bank.closeBank();
        sleep(600, 1000);
    }

    private void handleWalking() {
        if (Rs2Bank.isOpen()) {
            Rs2Bank.closeBank();
        }
        Rs2Walker.walkTo(NAGUA_AREA);
    }

    private void handlePreparation(SulphurNaguaConfig config) {
        if (Rs2Player.getPrayerPercentage() < 30) {
            Rs2Player.drinkPrayerPotion();
        }
        Rs2Player.eatAt(60);

        if (!isMoonlightPotionActive()) {
            Rs2Inventory.interact(MOONLIGHT_POTION_NAME, "Drink");
            sleep(1200);
        }

        if (Rs2Inventory.hasItem("Pestle and mortar") && Rs2Inventory.hasItem("Volcanic sulphur")) {
            // Hier Logik zum Brauen einfügen
        }
    }

    private void handleFighting(SulphurNaguaConfig config) {
        if (!Rs2Prayer.isPrayerActive(Rs2PrayerEnum.PROTECT_MELEE)) {
            Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MELEE, true);
        }

        if (!Rs2Player.isInCombat()) {
//            NPC naguaToAttack = Rs2Npc.getNpc(NAGUA_NAME);
//            if (naguaToAttack != null) {
//                Rs2Npc.attack(naguaToAttack);
//                // Warte, bis der Kampf beginnt, bevor du den Kill zählst
//                sleepUntil(Rs2Player::isInCombat, 5000);
//                // Warte, bis der NPC tot ist, dann zähle den Kill
//                if (sleepUntil(() -> Rs2Npc.getNpc(naguaToAttack.getIndex()) == null || Rs2Npc.getNpc(naguaToAttack.getIndex()).isDead(), 60000)) {
//                    totalNaguaKills++;
//                }
//            }
        }
    }

    // --- HILFSMETHODEN (VERBESSERT) ---

    private boolean shouldBank(SulphurNaguaConfig config) {
        // Banke, wenn dir eine der grundlegenden Ressourcen ausgeht.
        boolean outOfPotions = !Rs2Inventory.hasItem(MOONLIGHT_POTION_NAME) && !canMakePotion();
        boolean outOfPrayer = !Rs2Inventory.hasItem(PRAYER_POTION_NAME);
        boolean outOfFood = Rs2Inventory.getInventoryFood().isEmpty();

        return outOfPotions || outOfPrayer || outOfFood;
    }

    private boolean canMakePotion() {
        return Rs2Inventory.hasItem("Pestle and mortar") && Rs2Inventory.hasItem("Volcanic sulphur");
    }

    private boolean isInNaguaArea() {
        return Rs2Player.getWorldLocation().distanceTo(NAGUA_AREA) < 15;
    }

    private boolean needsPreparation() {
        return Rs2Player.getHealthPercentage() < 60 || Rs2Player.getPrayerPercentage() < 30 || !isMoonlightPotionActive();
    }

    private boolean isMoonlightPotionActive() {
        // PLATZHALTER
        return false;
    }
}

