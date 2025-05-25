package net.runelite.client.plugins.microbot.hunterKabbits;

import lombok.Getter;
import net.runelite.api.ItemID;
import net.runelite.api.NPC;
import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.antiban.enums.Activity;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Enum representing the state of the hunting bot.
 */
enum State {
    CATCHING, RETRIEVING, DROPPING
}

/**
 * Script for automated Kebbit hunting using a falcon in Old School RuneScape.
 * Handles catching, retrieving the falcon, and inventory management.
 */
public class HunterKabbitsScript extends Script {

    /**
     * Counter for how many kebbits have been successfully caught.
     */
    public static int KebbitCaught = 0;



    public boolean hasDied;

    @Getter
    private State currentState = State.CATCHING;

    private boolean droppingInProgress = false;

    /**
     * Main method to run the hunting script with the provided config and plugin context.
     *
     * @param config The configuration for the script.
     * @param plugin The plugin instance.
     */
    public void run(HunterKebbitsConfig config, HunterKebbitsPlugin plugin) {
        Rs2Antiban.resetAntibanSettings();
        applyAntiBanSettings();
        Rs2Antiban.setActivity(Activity.GENERAL_HUNTER);
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn() || !this.isRunning()) return;
                if (droppingInProgress) return;

                if (isHintArrowNpcActive() && currentState != State.RETRIEVING) {
                    currentState = State.RETRIEVING;
                    return;
                }

                if (currentState == State.CATCHING && !isFalconWithPlayer()) {
                    return;
                }

                switch (currentState) {
                    case DROPPING:
                        handleDroppingState(config);
                        break;
                    case RETRIEVING:
                        handleRetrievingState(config);
                        break;
                    case CATCHING:
                    default:
                        if (Rs2Inventory.isFull() || Rs2Inventory.getEmptySlots() <= 1) {
                            currentState = State.DROPPING;
                        } else {
                            handleCatchingState(config);
                        }
                        break;
                }

            } catch (Exception e) {
                e.printStackTrace();
                Microbot.status = "Error: " + e.getClass().getSimpleName() + " - " + e.getMessage();
            }
        }, 0, 600, TimeUnit.MILLISECONDS);
    }

    /**
     * Handles logic for dropping or burying items when the inventory is full.
     *
     * @param config The configuration for the script.
     */
    private void handleDroppingState(HunterKebbitsConfig config) {
        KebbitHunting currentKebbit = getKebbit(config);
        Integer furItemId = getSupportedFurItemId(currentKebbit);
        if (furItemId == null) {
            currentState = State.CATCHING;
            return;
        }

        while (Rs2Inventory.contains(furItemId) || Rs2Inventory.contains(ItemID.BONES)) {
            if (Rs2Inventory.contains(furItemId)) Rs2Inventory.drop(furItemId);

            if (Rs2Inventory.contains(ItemID.BONES)) {
                if (config.buryBones()) {
                    Rs2Inventory.interact(ItemID.BONES, "Bury");
                } else {
                    Rs2Inventory.drop(ItemID.BONES);
                }
                sleep(300, 600);
            }
        }

        currentState = State.CATCHING;
    }
    /**
     * Handles retrieving the falcon from the target NPC after an attempted catch.
     *
     * @param config The configuration for the script.
     */
    private void handleRetrievingState(HunterKebbitsConfig config) {
        // Unterstützte Falcon-NPC-IDs: Spotted, Dark, Dashing

        final Set<Integer> VALID_FALCON_NPC_IDS = Set.of(1342, 1343, 1344, 1345, 1346);

        NPC hintNpc = Microbot.getClient().getHintArrowNpc();

        if (hintNpc != null && VALID_FALCON_NPC_IDS.contains(hintNpc.getId())) {
            Rs2NpcModel model = new Rs2NpcModel(hintNpc);
            boolean retrieved = false;
            for (int i = 0; i < 5; i++) {
                if (Rs2Npc.interact(model, "Retrieve")) {
                    retrieved = true;
                    break;
                }
                sleep(200, 300);
            }

            if (retrieved) {
                sleep(config.minSleepAfterCatch(), config.maxSleepAfterCatch());
                KebbitCaught++;
                currentState = State.CATCHING;
            }
        } else {
            // Dynamisch prüfen, ob noch ein Falcon-NPC sichtbar ist
            boolean anyFalconStillActive = Rs2Npc.getNpcs()
                    .anyMatch(npc -> VALID_FALCON_NPC_IDS.contains(npc.getId()));

            if (!anyFalconStillActive) {
                currentState = State.CATCHING;
            }
        }
    }


    /**
     * Attempts to catch a kebbit by interacting with the corresponding NPC.
     *
     * @param config The configuration for the script.
     */
    private void handleCatchingState(HunterKebbitsConfig config) {
        String npcName = getKebbit(config).getNpcName(); // z. B. "Dark kebbit"

        if (Rs2Npc.interact(npcName, "Catch")) {
            Set<Integer> validFalconIds = Set.of(1342, 1343, 1344, 1345, 1346); // alle bekannten Falcon-IDs
            boolean falconActive = false;

            // Warte bis Falcon erscheint oder Hint Arrow aktiv wird (max. ~3 Sekunden)
            for (int i = 0; i < 10; i++) {
                boolean found = Rs2Npc.getNpcs().anyMatch(npc -> validFalconIds.contains(npc.getId()));
                if (found || isHintArrowNpcActive()) {
                    falconActive = true;
                    break;
                }
                sleep(200, 300);
            }

            if (falconActive) {
                currentState = State.RETRIEVING;
                return;
            }
        }

        // Falls kein Falcon aktiv oder Catch fehlgeschlagen → kleine Pause
        sleep(config.MinSleepAfterHuntingKebbit(), config.MaxSleepAfterHuntingKebbit());
    }

    /**
     * Determines the appropriate kebbit to hunt based on player level or configuration.
     *
     * @param config The configuration for the script.
     * @return The selected KebbitHunting enum value.
     */
    private KebbitHunting getKebbit(HunterKebbitsConfig config) {
        int level = Microbot.getClient().getRealSkillLevel(Skill.HUNTER);
        if (config.progressiveHunting()) {
            if (level >= 69) return KebbitHunting.DASHING;
            if (level >= 57) return KebbitHunting.DARK;
            return KebbitHunting.SPOTTED;
        }
        return config.kebbitType() != null ? config.kebbitType() : KebbitHunting.SPOTTED;
    }

    /**
     * Returns the corresponding fur item ID for a given Kebbit type.
     *
     * @param kebbit The kebbit type.
     * @return The item ID for the fur.
     */
    private Integer getSupportedFurItemId(KebbitHunting kebbit) {
        switch (kebbit) {
            case SPOTTED:
                return ItemID.SPOTTED_KEBBIT_FUR;
            case DASHING:
                return ItemID.DASHING_KEBBIT_FUR;
            case DARK:
                return ItemID.DARK_KEBBIT_FUR;
            default:
                return null;
        }
    }

    /**
     * Checks if a hint arrow is currently pointing to an NPC.
     *
     * @return true if an NPC is targeted by a hint arrow.
     */
    private boolean isHintArrowNpcActive() {
        return Microbot.getClient().getHintArrowNpc() != null;
    }

    /**
     * Determines if the falcon is currently with the player (i.e., not visible and no active hint arrow).
     *
     * @return true if the falcon is assumed to be with the player.
     */
    private boolean isFalconWithPlayer() {
        return Rs2Npc.getNpc(1342) == null && !isHintArrowNpcActive();
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

    /**
     * Called when the script is stopped. Resets relevant state.
     */
    @Override
    public void shutdown() {
        super.shutdown();
        KebbitCaught = 0;
        droppingInProgress = false;
        Microbot.status = "Script stopped.";
    }
}
