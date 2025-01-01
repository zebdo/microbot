package net.runelite.client.plugins.microbot.magic.aiomagic.scripts;

import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.magic.aiomagic.AIOMagicPlugin;
import net.runelite.client.plugins.microbot.magic.aiomagic.enums.MagicState;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.antiban.enums.Activity;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.microbot.util.magic.Runes;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TeleportScript extends Script {
    
    private MagicState state;
    private final AIOMagicPlugin plugin;

    @Inject
    public TeleportScript(AIOMagicPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean run() {
        Microbot.enableAutoRunOn = false;
        Rs2Antiban.resetAntibanSettings();
        Rs2Antiban.antibanSetupTemplates.applyGeneralBasicSetup();
        Rs2AntibanSettings.simulateAttentionSpan = true;
        Rs2AntibanSettings.nonLinearIntervals = true;
        Rs2AntibanSettings.contextualVariability = true;
        Rs2AntibanSettings.usePlayStyle = true;
        Rs2Antiban.setActivity(Activity.TELEPORT_TRAINING);
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;
                long startTime = System.currentTimeMillis();
                
                if (hasStateChanged()) {
                    state = updateState();
                }
                
                switch (state) {
                    case BANKING:
                        boolean isBankOpen = Rs2Bank.isNearBank(15) ? Rs2Bank.useBank() : Rs2Bank.walkToBankAndUseBank();
                        if (!isBankOpen || !Rs2Bank.isOpen()) return;
                        
                        if (!Rs2Equipment.hasEquipped(plugin.getStaff().getItemID())) {
                            if (!Rs2Bank.hasItem(plugin.getStaff().getItemID())) {
                                Microbot.showMessage("Configured Staff not found!");
                                shutdown();
                                return;
                            }
                            
                            Rs2Bank.withdrawAndEquip(plugin.getStaff().getItemID());
                        }
                        
                        Map<Runes, Integer> requiredRunes = getRequiredRunes(plugin.getTotalCasts());
                        
                        requiredRunes.forEach((rune, quantity) -> {
                            if (!isRunning()) return;
                            int itemID = rune.getItemId();
                            
                            if (!Rs2Bank.withdrawX(itemID, quantity)) {
                                Microbot.log("Failed to withdraw " + quantity + " of " + rune.name());
                            }
                            
                            Rs2Inventory.waitForInventoryChanges(1200);
                        });
                        
                        Rs2Bank.closeBank();
                        sleepUntil(() -> !Rs2Bank.isOpen());
                        break;
                    case CASTING:
                        Rs2Magic.cast(plugin.getTeleportSpell().getRs2Spell().getAction());
                        Rs2Player.waitForAnimation(1200);
                        break;
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
        Rs2Antiban.resetAntibanSettings();
        super.shutdown();
    }
    
    private boolean hasStateChanged() {
        if (!getRequiredRunes(1).isEmpty()) return true;
        if (state == MagicState.BANKING && getRequiredRunes(plugin.getTotalCasts()).isEmpty()) return true;
        return false;
    }
    
    private MagicState updateState() {
        if (!getRequiredRunes(1).isEmpty()) return MagicState.BANKING;
        if (state == MagicState.BANKING && getRequiredRunes(plugin.getTotalCasts()).isEmpty()) return MagicState.CASTING;
        return null;
    }
    
    private Map<Runes, Integer> getRequiredRunes(int casts) {
        return Rs2Magic.getRequiredRunes(plugin.getTeleportSpell().getRs2Spell(), plugin.getStaff(), casts, false);
    }
}
