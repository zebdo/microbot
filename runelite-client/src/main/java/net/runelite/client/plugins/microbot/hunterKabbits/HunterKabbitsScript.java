package net.runelite.client.plugins.microbot.hunterKabbits;

import lombok.Getter;
import net.runelite.api.ItemID;
import net.runelite.api.NPC;
import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;

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
     * @param config  The configuration for the script.
     * @param plugin  The plugin instance.
     */
    public void run(HunterKebbitsConfig config, HunterKebbitsPlugin plugin) {
        super.run();

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

            } catch (Exception ignored) {
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
            if (Rs2Inventory.contains(ItemID.BONES)) Rs2Inventory.interact(ItemID.BONES, "Bury");
            sleep(300, 600);
        }

        currentState = State.CATCHING;
    }

    /**
     * Handles retrieving the falcon from the target NPC after an attempted catch.
     *
     * @param config The configuration for the script.
     */
    private void handleRetrievingState(HunterKebbitsConfig config) {
        final int GYR_FALCON_NPC_ID = 1342;
        NPC hintNpc = Microbot.getClient().getHintArrowNpc();

        if (hintNpc != null && hintNpc.getId() == GYR_FALCON_NPC_ID) {
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
            NPC falcon = Rs2Npc.getNpc(GYR_FALCON_NPC_ID);
            if (falcon == null) {
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
        String npcName = getKebbit(config).getNpcName();

        if (Rs2Npc.interact(npcName, "Catch")) {
            boolean falconActive = false;
            for (int i = 0; i < 10; i++) {
                NPC falcon = Rs2Npc.getNpc(1342);
                if (falcon != null) {
                    falconActive = true;
                    break;
                }
                sleep(200, 300);
            }

            if (falconActive && isHintArrowNpcActive()) {
                currentState = State.RETRIEVING;
            }
        }

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
