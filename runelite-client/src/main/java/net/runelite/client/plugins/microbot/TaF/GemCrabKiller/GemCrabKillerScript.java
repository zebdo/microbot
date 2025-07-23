package net.runelite.client.plugins.microbot.TaF.GemCrabKiller;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.Rs2InventorySetup;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.concurrent.TimeUnit;

public class GemCrabKillerScript extends Script {
    public static String version = "1.0";
    private final int CAVE_ENTRANCE_ID = 57631;
    private final int CRAB_NPC_ID = 14779;
    private final int CRAB_NPC_DEAD_ID = 14780;
    private final WorldPoint CLOSEST_CRAB_LOCATION_TO_BANK = new WorldPoint(1274, 3168, 0);
    public GemCrabKillerState gemCrabKillerState = GemCrabKillerState.WALKING;
    private Rs2InventorySetup inventorySetup = null;
    private boolean hasLooted = false;

    public boolean run(GemCrabKillerConfig config) {
        if (config.useInventorySetup() && config.inventorySetup() == null) {
            Microbot.showMessage("Please select an inventory setup in the plugin settings. If you've already done so, please reselect the inventory setup in the plugin settings.");
            shutdown();
            return false;
        }
        if (config.overrideState()) {
            gemCrabKillerState = config.startState();
        }
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;
                if (config.useInventorySetup()) {
                    inventorySetup = new Rs2InventorySetup(config.inventorySetup(), mainScheduledFuture);
                }
                switch (gemCrabKillerState) {
                    case WALKING:
                        if (handleWalking()) return;
                        break;
                    case FIGHTING:
                        if (handleFighting(config)) return;
                        break;
                    case BANKING:
                        handleBanking(config);
                        break;
                }

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
                Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        return true;
    }

    private void handleBanking(GemCrabKillerConfig config) {
        Rs2Bank.walkToBank(BankLocation.TAL_TEKLAN);
        Rs2Bank.openBank();
        if (Rs2Bank.isOpen()) {
            if (config.useInventorySetup()) {
                inventorySetup.loadEquipment();
                inventorySetup.loadInventory();
                gemCrabKillerState = GemCrabKillerState.WALKING;
            } else {
                Rs2Bank.depositAllExcept(false, " pickaxe");
                gemCrabKillerState = GemCrabKillerState.WALKING;
            }
        }
    }

    private boolean handleFighting(GemCrabKillerConfig config) {
        var npc = Rs2Npc.getNpc(CRAB_NPC_ID);
        var deadNpc = Rs2Npc.getNpc(CRAB_NPC_DEAD_ID);
        if (deadNpc != null) {
            if (config.lootCrab() && Rs2Inventory.hasItem(" pickaxe", false) && !hasLooted) {
                Rs2Npc.interact(deadNpc, "Mine");
                Rs2Inventory.waitForInventoryChanges(2400);
                hasLooted = true;
                if (Rs2Inventory.isFull()) {
                    gemCrabKillerState = GemCrabKillerState.BANKING;
                    return true;
                }
            }
            Rs2GameObject.interact(CAVE_ENTRANCE_ID, "Crawl-through");
            return true;
        } else {
            hasLooted = false;
        }
        if (npc == null) {
            gemCrabKillerState = GemCrabKillerState.WALKING;
            return true;
        }
        if (!Rs2Player.isInCombat()) {
            Rs2Npc.attack(npc);
        }
        return false;
    }

    private boolean handleWalking() {
        var npc = Rs2Npc.getNpc(CRAB_NPC_ID);
        if (Rs2Player.isNearArea(CLOSEST_CRAB_LOCATION_TO_BANK, 10) && npc != null) {
            gemCrabKillerState = GemCrabKillerState.FIGHTING;
            return true;
        }
        if (Rs2Player.isNearArea(CLOSEST_CRAB_LOCATION_TO_BANK, 10) && npc == null) {
            Rs2GameObject.interact(CAVE_ENTRANCE_ID, "Crawl-through");
            return true;
        }
        if (npc == null) {
            Rs2Walker.walkTo(CLOSEST_CRAB_LOCATION_TO_BANK);
        }
        if (npc != null) {
            gemCrabKillerState = GemCrabKillerState.FIGHTING;
        }
        return false;
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
}