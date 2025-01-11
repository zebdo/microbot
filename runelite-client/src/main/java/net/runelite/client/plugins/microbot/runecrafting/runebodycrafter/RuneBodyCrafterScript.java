package net.runelite.client.plugins.microbot.runecrafting.runebodycrafter;

import net.runelite.api.ItemID;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.concurrent.TimeUnit;

import static net.runelite.api.ItemID.BODY_TIARA;
import static net.runelite.api.ItemID.PURE_ESSENCE;


public class RuneBodyCrafterScript extends Script {

    private WorldArea EdgevilleBankArea = new WorldArea(3088, 3486, 13, 16, 0);
    private WorldPoint BodyAltarPoint = new WorldPoint(3055,3446,0);
    private boolean isInAltarRoom() {
        return Rs2Player.getWorldLocation().getRegionID() == 10059;
    }


    public static boolean test = false;
    public boolean run(RuneBodyCrafterConfig config) {
        Microbot.enableAutoRunOn = false;

        Rs2Antiban.resetAntibanSettings();
        Rs2Antiban.antibanSetupTemplates.applySmithingSetup();
        Rs2AntibanSettings.dynamicActivity = false;
        Rs2AntibanSettings.dynamicIntensity = true;
        Rs2AntibanSettings.actionCooldownChance = 0.1;
        Rs2AntibanSettings.microBreakChance = 0.1;
        Rs2AntibanSettings.microBreakDurationLow = 0;
        Rs2AntibanSettings.microBreakDurationHigh = 3;

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;
                long startTime = System.currentTimeMillis();

                if (EdgevilleBankArea.contains(Rs2Player.getWorldLocation()))
                {   Rs2Bank.walkToBankAndUseBank(BankLocation.EDGEVILLE);
                    if (!Rs2Equipment.isWearing(BODY_TIARA))
                    {
                        Rs2Bank.withdrawAndEquip(BODY_TIARA);
                    }
                    Rs2Bank.withdrawAll(PURE_ESSENCE);
                    sleepUntil(() -> Rs2Inventory.contains(PURE_ESSENCE) && Rs2Equipment.isWearing(BODY_TIARA), 10000);
                    Rs2Walker.walkTo(BodyAltarPoint);
                    Rs2GameObject.interact(34818,"Enter");
                    sleep(3000,5000);
                    Rs2GameObject.interact(34765,"Craft-rune");
                    sleepUntil(() -> Rs2Inventory.contains(ItemID.MIND_RUNE),10000);
                    Rs2GameObject.interact(34753,"Use");
                    sleepUntil(() -> !isInAltarRoom(), 10000);
                    Rs2Bank.walkToBank(BankLocation.EDGEVILLE);
                    Rs2Bank.useBank();
                    Rs2Bank.depositAll();
                }
                else if (Rs2Player.getWorldLocation().getRegionID() == 10059) {
                    Rs2GameObject.interact(34753, "Use");
                    sleepUntil(() -> !isInAltarRoom(), 10000);
                    Rs2Bank.walkToBank(BankLocation.EDGEVILLE);
                    Rs2Bank.useBank();
                    Rs2Bank.depositAll();
                    Rs2Bank.closeBank();

                }
                else {Rs2Bank.walkToBank(BankLocation.EDGEVILLE);
                    Rs2Bank.useBank();
                    Rs2Bank.depositAll();
                    Rs2Bank.closeBank();

                }

                long endTime = System.currentTimeMillis();
                long totalTime = endTime - startTime;
                System.out.println("Total time for loop " + totalTime);

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        return true;
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
}