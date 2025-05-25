package net.runelite.client.plugins.microbot.TaF.EnsouledHeadSlayer;

import net.runelite.api.Skill;
import net.runelite.api.Varbits;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.util.Rs2InventorySetup;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.misc.Rs2Food;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.skillcalculator.skills.MagicAction;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.runelite.client.plugins.microbot.util.antiban.enums.ActivityIntensity.MODERATE;

public class EnsouledHeadSlayerScript extends Script {
    public static final String VERSION = "1.0";
    public static final int ENSOULED_GROUND_GRAPHICS = 1290;
    public EnsouledHeadSlayerStatus BOT_STATE = EnsouledHeadSlayerStatus.BANKING;
    private final WorldPoint ALTAR_LOCATION = new WorldPoint(1711, 3882, 0);
    private Rs2InventorySetup inventorySetup = null;
    private final WorldArea SUMMON_AREA = new WorldArea(ALTAR_LOCATION, 25,25);
    private boolean firstRun = true;

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
        Rs2Antiban.setActivityIntensity(MODERATE);
    }

    public boolean run(EnsouledHeadSlayerConfig config) {
        BOT_STATE = config.startingState();
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!super.run()) return;
                if (!Microbot.isLoggedIn()) return;
                if (config.useInventorySetup()) {
                    if (config.inventorySetup() == null) {
                        Microbot.log("Inventory setup is null, please select an inventory setup. If one is already selected, please reselect it.");
                        return;
                    } else {
                        inventorySetup = new Rs2InventorySetup(config.inventorySetup(), mainScheduledFuture);
                    }
                }
                switch (BOT_STATE) {
                    case BANKING:
                        if (handleBanking(config)) {
                            BOT_STATE = EnsouledHeadSlayerStatus.WALKING_TO_ALTAR;
                        }
                    case WALKING_TO_ALTAR:
                        if (Rs2Walker.walkTo(ALTAR_LOCATION)) {
                            BOT_STATE = EnsouledHeadSlayerStatus.REANIMATING;
                        }
                        break;
                    case REANIMATING:
                        handleReanimatingAndKilling(config);
                        break;
                }
            } catch (Exception ex) {
                System.out.println("Exception message: " + ex.getMessage());
                ex.printStackTrace();
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        return true;
    }

    private void handleReanimatingAndKilling(EnsouledHeadSlayerConfig config) {
        if (Rs2Player.getHealthPercentage() < 50) {
            Rs2Player.eatAt(50);
        }
//        if (!Rs2Player.getWorldLocation().isInArea(SUMMON_AREA) && !Rs2Player.isInCombat()) {
//            Microbot.log("Not in the summoning area, walking to altar...");
//            BOT_STATE = EnsouledHeadSlayerStatus.WALKING_TO_ALTAR;
//            return;
//        }
    /*
		0 = Standard
		1 = Ancient
		2 = Lunar
		3 = Arceuus
		4 = NONE
	*/
        if (Microbot.getVarbitValue(Varbits.SPELLBOOK) != 3) {
            Microbot.log("On wrong spellbook, switching to Arceuus...");
            Rs2Npc.interact("Tyss", "Spellbook");
        }
        Rs2Combat.enableAutoRetialiate();
        var ensouledHead = Rs2Inventory.count("ensouled");
        if (ensouledHead == 0 && !Rs2Combat.inCombat()) {
            Microbot.log("No ensouled heads found in inventory, banking...");
            BOT_STATE = EnsouledHeadSlayerStatus.BANKING;
            return;
        }
        if (Rs2Inventory.count(config.food().getName()) < 1 && Microbot.getClient().getBoostedSkillLevel(Skill.HITPOINTS) < Microbot.getClient().getRealSkillLevel(Skill.HITPOINTS) / 2) {;
            Microbot.log("Not enough food & below half health, banking...");
            BOT_STATE = EnsouledHeadSlayerStatus.BANKING;
            return;
        }
        if (Rs2Player.getInteracting() != null) {
            return;
        }
        var enemy = Rs2Npc.getAttackableNpcs("Reanimated").collect(Collectors.toList());
        if (enemy.isEmpty() && Rs2Player.getInteracting() == null && Rs2GameObject.getGroundObject(ENSOULED_GROUND_GRAPHICS) == null) {
            var ensouledHeadItem = Rs2Inventory.get("ensouled");
            var spell = Arrays.stream(EnsouledHeads.values())
                    .filter(x -> x.hasRequirements() && Objects.equals(x.getName(), ensouledHeadItem.getName()))
                    .findFirst()
                    .orElse(null);
            if (spell != null) {
                Rs2Magic.cast(spell.getMagicSpell());
                sleepUntil(() -> Rs2Tab.getCurrentTab() == InterfaceTab.INVENTORY, 1000);
                Rs2Inventory.interact(ensouledHeadItem);
                // NPC animation getting cast
                sleepUntil(() -> Rs2GameObject.getGroundObject(ENSOULED_GROUND_GRAPHICS) != null, 4000);
                // NPC animation finished - NPC should be spawned
                sleepUntil(() -> Rs2GameObject.getGroundObject(ENSOULED_GROUND_GRAPHICS) == null, 4000);
                sleep(4000,4000);
            }
        } else {
            Microbot.log("Found NPC");
            enemy.stream().findFirst().ifPresent(Rs2Npc::attack);
        }
    }

    private int attempts = 0;
    private boolean handleBanking(EnsouledHeadSlayerConfig config) {
        if (firstRun) {
            Rs2Bank.walkToBank();
            firstRun = false;
        } else {
            Rs2Bank.walkToBank(BankLocation.WINTERTODT);
        }
        if (!Rs2Bank.openBank()) {
            return false;
        }
        if (!useInventorySetup(config)) return false;
        if (!config.useInventorySetup()) {
            if (!getRunesAndEnsouledHeads(config)) {
                Microbot.log("Failed to withdraw runes or ensouled heads");
            }
            var foodAmount = config.foodAmount() - Rs2Inventory.count(config.food().getId());
            if (foodAmount > 0) {
                Microbot.log("Withdrawing " + foodAmount + " " + config.food().getName() + " from bank");
                Rs2Bank.withdrawX(config.food().getId(), foodAmount);
            }

            Rs2Bank.closeBank();
        }

        return true;
    }

    private boolean getRunesAndEnsouledHeads(EnsouledHeadSlayerConfig config) {
        if (config.ensouledHeads().getName().equals("All")) {
            // Group ensouled heads by ID and count them
            Map<Integer, Integer> headCounts = new HashMap<>();

            for (var item : Rs2Bank.bankItems()) {
                if (item != null && item.name != null && item.name.toLowerCase().contains("ensouled")) {
                    headCounts.put(item.id, headCounts.getOrDefault(item.id, 0) + 1);
                }
            }

            if (headCounts.isEmpty()) {
                Microbot.log("No ensouled heads found in bank");
                return false;
            }

            // Find the head with the highest count
            int highestId = -1;
            int highestCount = 0;

            for (Map.Entry<Integer, Integer> entry : headCounts.entrySet()) {
                if (entry.getValue() > highestCount) {
                    highestId = entry.getKey();
                    highestCount = entry.getValue();
                }
            }

            // Find corresponding enum
            EnsouledHeads primaryHead = null;
            for (EnsouledHeads head : EnsouledHeads.values()) {
                if (head.getItemId() == highestId) {
                    primaryHead = head;
                    break;
                }
            }

            if (primaryHead == null) {
                Microbot.log("Could not identify ensouled head type");
                return false;
            }

            // Calculate available slots
            int availableSlots = Rs2Inventory.getEmptySlots() - config.foodAmount() - 3;

            if (availableSlots <= 0) {
                Microbot.log("Not enough inventory space");
                return false;
            }

            // Withdraw the primary head type
            int toWithdraw = Math.min(highestCount, availableSlots);
            Rs2Bank.withdrawX(primaryHead.getItemId(), toWithdraw);

            // If we still have space and not enough of primary head
            if (toWithdraw < availableSlots) {
                int remainingSlots = availableSlots - toWithdraw;
                MagicAction requiredSpell = primaryHead.getMagicSpell();

                // Find other heads using the same spell
                for (EnsouledHeads otherHead : EnsouledHeads.values()) {
                    if (otherHead != primaryHead && otherHead.getMagicSpell() == requiredSpell) {
                        int count = headCounts.getOrDefault(otherHead.getItemId(), 0);
                        if (count > 0) {
                            int otherWithdraw = Math.min(count, remainingSlots);
                            Rs2Bank.withdrawX(otherHead.getItemId(), otherWithdraw);
                            remainingSlots -= otherWithdraw;

                            if (remainingSlots == 0) break;
                        }
                    }
                }
            }

            // Withdraw runes for the spell
            withdrawRunesForSpell(primaryHead.getMagicSpell());
        } else {
            EnsouledHeads selectedHead = config.ensouledHeads();
            withdrawRunesForSpell(selectedHead.getMagicSpell());
            if (config.useGamesNecklaceForBanking() && Rs2Inventory.count("Games necklace") < 1) {
                Rs2Bank.withdrawX("Games necklace", 1);
                sleep(600, 800);
            }
            if (config.useArcheusLibraryTeleport() && Rs2Inventory.count("earth rune") < 2 && Rs2Inventory.count("law rune") < 1) {
                Rs2Bank.withdrawX("earth rune", Rs2Random.between(4,10));
                sleep(600, 800);
                Rs2Bank.withdrawX("law rune", Rs2Random.between(4,10));
                sleep(600, 800);
            }

            int headCount = Rs2Bank.count(selectedHead.getItemId());
            if (headCount == 0) {
                Microbot.log("No " + selectedHead.getName() + " found in bank");
                return false;
            }
            var headsToWithdraw = Rs2Inventory.getEmptySlots() - config.foodAmount();
            Microbot.log("Withdrawing " + headsToWithdraw + " " + selectedHead.getName() + " from bank");
            Rs2Bank.withdrawX(selectedHead.getName(), headsToWithdraw);
        }
        return true;
    }

    private void withdrawRunesForSpell(MagicAction spell) {
        if (spell == null) return;

        switch (spell) {
            case BASIC_REANIMATION:
                Rs2Bank.withdrawX("Nature rune", Rs2Random.between(2 * 28, 4 * 28));
                Rs2Bank.withdrawX("Body rune", Rs2Random.between(4 * 28, 6 * 28));
                break;
            case ADEPT_REANIMATION:
                Rs2Bank.withdrawX("Nature rune", Rs2Random.between(3 * 28, 6 * 28));
                Rs2Bank.withdrawX("Body rune", Rs2Random.between(4 * 28, 6 * 28));
                Rs2Bank.withdrawX("Soul rune", Rs2Random.between(28, 4 * 28));
                break;
            case EXPERT_REANIMATION:
                Rs2Bank.withdrawX("Nature rune", Rs2Random.between(3 * 28, 5 * 28));
                Rs2Bank.withdrawX("Soul rune", Rs2Random.between(2 * 28, 4 * 28));
                Rs2Bank.withdrawX("blood rune", Rs2Random.between(28, 2 * 28));
                break;
            case MASTER_REANIMATION:
                Rs2Bank.withdrawX( "Nature rune", Rs2Random.between(4 * 28, 6 * 28));
                Rs2Bank.withdrawX( "Soul rune", Rs2Random.between(4 * 28, 6 * 28));
                Rs2Bank.withdrawX( "Blood rune", Rs2Random.between(2 * 28, 4 * 28));
                break;
        }
    }

    private boolean useInventorySetup(EnsouledHeadSlayerConfig config) {
        if (config.useInventorySetup()) {
            var successEq = inventorySetup.loadEquipment();
            var successInv = inventorySetup.loadInventory();
            if (!successEq || !successInv) {
                Microbot.log("Failed to load inventory setup, trying again...");
                attempts++;
                // Stupid failsafe to handle inventory setups failing the first time
                if (attempts > 2) {
                    Microbot.log("Failed to load inventory setup after 2 attempts, stopping script.");
                    shutdown();
                    return false;
                }
                return false;
            } else {
                attempts = 0;
                Microbot.log("Loaded inventory setup successfully.");
            }
        }
        return true;
    }

    @Override
    public void shutdown() {
        BOT_STATE = EnsouledHeadSlayerStatus.BANKING;
        firstRun = true;
        super.shutdown();
    }
}
