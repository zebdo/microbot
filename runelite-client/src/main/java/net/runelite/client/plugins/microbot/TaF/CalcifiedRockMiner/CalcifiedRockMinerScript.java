package net.runelite.client.plugins.microbot.TaF.CalcifiedRockMiner;

import net.runelite.api.KeyCode;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.Rs2InventorySetup;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.awt.event.KeyEvent;
import java.util.concurrent.TimeUnit;

import static net.runelite.client.plugins.microbot.util.antiban.enums.ActivityIntensity.VERY_LOW;

public class CalcifiedRockMinerScript extends Script {
    public static final String VERSION = "1.0";
    public static CalcifiedRockMinerState BOT_STATUS = CalcifiedRockMinerState.BANKING;
    private final WorldPoint CALCIFIED_ROCK_LOCATION = new WorldPoint(1516, 9545, 1);
    private final WorldPoint ANVIL = new WorldPoint(1447, 9584, 1);

    {
        Microbot.enableAutoRunOn = false;
        Rs2Antiban.resetAntibanSettings();
        Rs2AntibanSettings.usePlayStyle = true;
        Rs2AntibanSettings.simulateFatigue = false;
        Rs2AntibanSettings.simulateAttentionSpan = true;
        Rs2AntibanSettings.behavioralVariability = true;
        Rs2AntibanSettings.nonLinearIntervals = true;
        Rs2AntibanSettings.dynamicActivity = true;
        Rs2AntibanSettings.profileSwitching = true;
        Rs2AntibanSettings.naturalMouse = true;
        Rs2AntibanSettings.simulateMistakes = true;
        Rs2AntibanSettings.moveMouseOffScreen = true;
        Rs2AntibanSettings.moveMouseRandomly = true;
        Rs2AntibanSettings.moveMouseRandomlyChance = 0.04;
        Rs2Antiban.setActivityIntensity(VERY_LOW);
    }

    public boolean run(CalcifiedRockMinerConfig config) {
        BOT_STATUS = CalcifiedRockMinerState.MINING;
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!super.run()) return;
                if (!Microbot.isLoggedIn()) return;
                if (Rs2AntibanSettings.actionCooldownActive) return;
                switch (BOT_STATUS) {
                    case BANKING:
                        handleBanking(config);
                        break;
                    case CRUSHING:
                        handleCrushing(config);
                        break;
                    case MINING:
                        handleMining(config);
                        break;
                }
            } catch (Exception ex) {
                System.out.println("Exception message: " + ex.getMessage());
                ex.printStackTrace();
            }
        }, 0, 600, TimeUnit.MILLISECONDS);
        return true;
    }

    private void handleMining(CalcifiedRockMinerConfig config) {
        Rs2GameObject.interact()
    }

    private void handleCrushing(CalcifiedRockMinerConfig config) {
        if (config.crushDeposits()) {
            if (Rs2Walker.isNear(ANVIL)) {
                Rs2Inventory.interact("Calcified deposit", "use");
                Rs2GameObject.interact("Anvil");
                Rs2Widget.sleepUntilHasWidget("test");
                var randomNum = Rs2Random.between(0,1);
                if (randomNum == 0) {
                    Rs2Widget.clickWidget("");
                } else {
                    Rs2Keyboard.keyPress(KeyEvent.VK_SPACE);
                }
            }
            else {
                Rs2Walker.walkTo(ANVIL);
            }
        }

    }

    private void handleBanking(CalcifiedRockMinerConfig config) {
        if (!config.dropDeposits() && !config.crushDeposits()) {
            Rs2Bank.walkToBank(BankLocation.CAM_TORUM);
            Rs2Bank.openBank();
            Rs2Bank.depositAll("Calcified deposit");
        }

    }

    private void setNextTask(CalcifiedRockMinerConfig config) {

    }

    public enum CalcifiedRockMinerState {BANKING, CRUSHING, MINING}

}
