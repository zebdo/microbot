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
import net.runelite.client.plugins.microbot.util.inventory.Rs2Item;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.microbot.util.magic.Rs2Spells;
import net.runelite.client.plugins.microbot.util.magic.Runes;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TeleAlchScript extends Script {

    private MagicState state;
    private final AIOMagicPlugin plugin;

    @Inject
    public TeleAlchScript(AIOMagicPlugin plugin) {
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

                if (state == null) {
                    Microbot.showMessage("Unable to evaluate state");
                    shutdown();
                    return;
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

                        Map<Runes, Integer> requiredTeleportRunes = getRequiredTeleportRunes(plugin.getTotalCasts());

                        requiredTeleportRunes.forEach((rune, quantity) -> {
                            if (!isRunning()) return;
                            int itemID = rune.getItemId();

                            if (!Rs2Bank.withdrawX(itemID, quantity)) {
                                Microbot.log("Failed to withdraw " + quantity + " of " + rune.name());
                            }
                            Rs2Inventory.waitForInventoryChanges(1200);
                        });

                        Rs2Bank.setWithdrawAsNote();
                        plugin.getAlchItemNames().forEach((itemName) -> {
                            if (!isRunning()) return;
                            Rs2Bank.withdrawAll(itemName);
                            
                            Rs2Inventory.waitForInventoryChanges(1200);
                        });
                        Rs2Bank.setWithdrawAsItem();

                        int totalAlchCasts = getAlchCastAmount();

                        Map<Runes, Integer> requiredAlchRunes = getRequiredAlchRunes(totalAlchCasts);
                        requiredAlchRunes.forEach((rune, quantity) -> {
                            if (!isRunning()) return;
                            int itemID = rune.getItemId();
                            if (!Rs2Bank.withdrawX(itemID, quantity)) {
                                Microbot.log("Failed to withdraw " + quantity + " of " + rune.name());
                            }
                        });

                        Rs2Bank.closeBank();
                        sleepUntil(() -> !Rs2Bank.isOpen());
                        break;
                    case CASTING:
                        if (!Rs2Inventory.hasItem(plugin.getAlchItemNames().get(0))) {
                            plugin.getAlchItemNames().remove(0);
                            return;
                        }

                        Rs2Item alchItem = Rs2Inventory.get(plugin.getAlchItemNames().get(0));
                        int inventorySlot = Rs2Player.getRealSkillLevel(Skill.MAGIC) >= 55 ? 11 : 4;
                        if (alchItem.getSlot() != inventorySlot) {
                            Rs2Inventory.moveItemToSlot(alchItem, inventorySlot);
                            return;
                        }

                        Rs2Magic.cast(plugin.getTeleportSpell().getRs2Spell().getAction());
                        Rs2Player.waitForAnimation(1200);

                        Rs2Magic.alch(alchItem);
                        Rs2Player.waitForXpDrop(Skill.MAGIC, false);
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
        if (state == null) return true;
        if (!getRequiredTeleportRunes(1).isEmpty() || !getRequiredAlchRunes(1).isEmpty()) return true;
        return state == MagicState.BANKING && (getRequiredTeleportRunes(plugin.getTotalCasts()).isEmpty() && getRequiredAlchRunes(getAlchCastAmount()).isEmpty());
    }

    private MagicState updateState() {
        if (state == null) {
            if (!getRequiredTeleportRunes(1).isEmpty() || !getRequiredAlchRunes(getAlchCastAmount()).isEmpty()) {
                return MagicState.BANKING;
            } else {
                return MagicState.CASTING;
            }
        }
        if (!getRequiredTeleportRunes(1).isEmpty() || !getRequiredAlchRunes(1).isEmpty()) return MagicState.BANKING;
        if (state == MagicState.BANKING && (getRequiredTeleportRunes(plugin.getTotalCasts()).isEmpty() && getRequiredAlchRunes(getAlchCastAmount()).isEmpty()))
            return MagicState.CASTING;
        return null;
    }

    private Map<Runes, Integer> getRequiredTeleportRunes(int casts) {
        return Rs2Magic.getRequiredRunes(plugin.getTeleportSpell().getRs2Spell(), plugin.getStaff(), casts, false);
    }

    private Map<Runes, Integer> getRequiredAlchRunes(int casts) {
        Rs2Spells alchSpell = Rs2Player.getRealSkillLevel(Skill.MAGIC) >= 55 ? Rs2Spells.HIGH_LEVEL_ALCHEMY : Rs2Spells.LOW_LEVEL_ALCHEMY;
        return Rs2Magic.getRequiredRunes(alchSpell, plugin.getStaff(), casts, false);
    }

    private int getAlchCastAmount() {
        return Rs2Inventory.items().stream()
                .filter(item -> plugin.getAlchItemNames().contains(item.getName().toLowerCase()))
                .mapToInt(Rs2Item::getQuantity)
                .sum();
    }
}
