package net.runelite.client.plugins.microbot.driftnet;

import net.runelite.api.ItemID;
import net.runelite.api.ObjectID;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.KeyEvent;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static net.runelite.client.plugins.microbot.Microbot.log;
import static net.runelite.client.plugins.microbot.util.Global.sleepGaussian;

public class DriftNetScript extends Script {

    // Script version - Update if needed
    public static final double VERSION = 1.21; // Reflects major chasing logic overhaul

    private static final int MAX_WITHDRAW_ATTEMPTS = 5;
    private static final Logger scriptLog = LoggerFactory.getLogger(DriftNetScript.class);

    // Annette Storage Widget IDs
    private static final int ANNETTE_STORAGE_ITEM_SLOT_ID = 20250629;

    // Drift Net Reward Widget IDs
    private static final int DRIFT_NET_REWARD_GROUP_ID = 39780352;
    private static final int DRIFT_NET_REWARD_BANK_BUTTON_ID = 39780359;
    private static final int DRIFT_NET_REWARD_LOOT_BUTTON_ID = 39780358;
    private static final int DRIFT_NET_REWARD_BANK_ALL_BUTTON_ID = 39780365;

    // Stamina Potions Item IDs
    private static final int STAMINA_POTION4_ID = 12625;
    private static final int STAMINA_POTION3_ID = 12627;
    private static final int STAMINA_POTION2_ID = 12629;
    private static final int STAMINA_POTION1_ID = 12631;

    // Current number of attempts to withdraw nets
    private int netWithdrawAttempts = 0;

    // Priority Zones & Net Centers
    private static final WorldArea PRIORITY_ZONE_1 = new WorldArea(3743, 10297, 7, 5, 1);
    private static final WorldArea PRIORITY_ZONE_2 = new WorldArea(3739, 10288, 4, 5, 1);
    private static final WorldPoint NET_1_CENTER = new WorldPoint(3748, 10297, 1);
    private static final WorldPoint NET_2_CENTER = new WorldPoint(3742, 10290, 1);

    // Scoring Weights (Tune these!)
    private static final double W_PLAYER_DIST = 6.0; // Weight for distance from player to fish (Increased)
    private static final double W_NET_DIST = 1.0;    // Weight for distance from fish to target net
    private static final double W_NET_NEED = 7.5;    // Weight for how much the target net needs fish
    private static final double PRIORITY_ZONE_BONUS = 75.0; // Flat bonus for being in a priority zone
    private static final double W_TRAVEL_PENALTY = 0.5; // Weight for player distance penalty (Increased)

    // Main Loop Timer
    private static final int MAIN_LOOP_DELAY_MS = 150; // Faster main loop checks

    public boolean run(DriftNetConfig config) {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                // --- Initial Checks (Stamina, Login, Max Withdraw) ---
                if (!Microbot.isLoggedIn() || !super.run()) return;
                handleStaminaPotion();
                if (netWithdrawAttempts > MAX_WITHDRAW_ATTEMPTS) {
                    log("Script shutdown: Failed to withdraw nets from Annette after " + MAX_WITHDRAW_ATTEMPTS + " attempts (Maybe empty?).");
                    Microbot.showMessage("Drift Net script stopping: Max withdrawal attempts reached. Check Annette's stock.");
                    shutdown();
                    return;
                }

                // --- Check/Withdraw Nets ---
                if (!Rs2Inventory.hasItem(ItemID.DRIFT_NET)) {
                    withdrawNetsFromAnnette();
                    return; // Return after starting withdrawal
                }
                if (netWithdrawAttempts > 0) { // Reset attempts if we have nets
                    log("Successfully obtained nets, resetting withdrawal attempts.");
                    netWithdrawAttempts = 0;
                }

                // --- Action Priority Logic ---
                boolean tookPriorityAction = false;
                List<DriftNet> currentNets = DriftNetPlugin.getNETS(); // Get nets once per cycle

                // Check if net data is available
                if (currentNets == null || currentNets.isEmpty()) {
                    log("Waiting for DriftNetPlugin to provide net data...");
                    sleep(500, 1000); // Wait a bit if plugin data isn't ready
                    return;
                }

                // 1. Handle FULL Nets
                for (DriftNet net : currentNets) {
                    if (net.getStatus() == DriftNetStatus.FULL) {
                        log("Main loop: Handling FULL net..."); // Optional log
                        handleFullNet(net, config);
                        tookPriorityAction = true;
                        // Don't break, check other net too
                    }
                }

                // 2. Handle UNSET Nets (Only if no FULL nets were handled)
                if (!tookPriorityAction) {
                    for (DriftNet net : currentNets) {
                        if (net.getStatus() == DriftNetStatus.UNSET) {
                            log("Main loop: Handling UNSET net..."); // Optional log
                            handleUnsetNet(net);
                            tookPriorityAction = true;
                            break; // Only one unset per cycle
                        }
                    }
                }

                // 3. Manage Chasing State (Lowest Priority)
                if (!tookPriorityAction) {
                    // log("Main loop: Entering chasing state..."); // Optional log
                    manageChasingState(); // Call the looping chase function
                    // log("Main loop: Exited chasing state."); // Optional log
                }

            } catch (Exception ex) {
                scriptLog.error("Error in DriftNetScript loop: {}", ex.getMessage(), ex);
                // Print stack trace for better debugging
                ex.printStackTrace();
                sleep(1000, 2000); // Pause after error
            }
            // Use the reduced delay for the main scheduler
        }, 0, MAIN_LOOP_DELAY_MS, TimeUnit.MILLISECONDS);

        return true;
    }

    /**
     * Checks run energy and drinks a stamina potion if needed.
     */
    private void handleStaminaPotion() {
        int currentEnergy = Rs2Player.getRunEnergy();
        if (currentEnergy < 60) {
            if (Rs2Inventory.hasItem(STAMINA_POTION4_ID) || Rs2Inventory.hasItem(STAMINA_POTION3_ID) ||
                    Rs2Inventory.hasItem(STAMINA_POTION2_ID) || Rs2Inventory.hasItem(STAMINA_POTION1_ID)) {
                log("Run energy low ("+currentEnergy+"), drinking stamina.");
                boolean drank = Rs2Inventory.interact(STAMINA_POTION4_ID, "Drink") ||
                        Rs2Inventory.interact(STAMINA_POTION3_ID, "Drink") ||
                        Rs2Inventory.interact(STAMINA_POTION2_ID, "Drink") ||
                        Rs2Inventory.interact(STAMINA_POTION1_ID, "Drink");
                if (drank) {
                    sleepGaussian(200, 200); // Wait after drinking
                } else {
                    log("ERROR: Failed to interact with Stamina Potion in inventory.");
                }
            }
        }
    }

    /**
     * Attempts to withdraw drift nets from Annette's underwater storage.
     */
    private void withdrawNetsFromAnnette() {
        // ... (Withdraw logic remains the same as provided before) ...
        int freeSlots = Rs2Inventory.getEmptySlots();
        if (freeSlots < 1) {
            log("Inventory full, cannot withdraw nets.");
            netWithdrawAttempts++;
            return;
        }

        final String ANNETTE_INTERACTION_ACTION = "Nets";
        boolean interacted = Rs2GameObject.interact(ObjectID.ANNETTE, ANNETTE_INTERACTION_ACTION);
        if (!interacted) {
            log("Failed to interact with Annette (" + ANNETTE_INTERACTION_ACTION + ").");
            netWithdrawAttempts++;
            return;
        }

        sleep(700, 1100); // User preferred timing

        final int WITHDRAW_ALL_IDENTIFIER = 4;
        int netsBefore = Rs2Inventory.count(ItemID.DRIFT_NET);

        try {
            if (Rs2Widget.getWidget(ANNETTE_STORAGE_ITEM_SLOT_ID) != null && !Rs2Widget.isHidden(ANNETTE_STORAGE_ITEM_SLOT_ID)) {
                Rs2Widget.clickWidgetFast(ANNETTE_STORAGE_ITEM_SLOT_ID, WITHDRAW_ALL_IDENTIFIER);

                sleep(300, 400);
                boolean interfaceClosedOrGotNets = sleepUntil(() -> Rs2Widget.getWidget(ANNETTE_STORAGE_ITEM_SLOT_ID) == null || Rs2Inventory.hasItem(ItemID.DRIFT_NET), 4500);

                int netsAfter = Rs2Inventory.count(ItemID.DRIFT_NET);

                if (interfaceClosedOrGotNets && netsAfter > netsBefore) {
                    // Success
                } else if (interfaceClosedOrGotNets && netsAfter <= netsBefore) {
                    log("WARN: Withdrawal attempt finished, but net count did not increase. Annette might be empty.");
                    netWithdrawAttempts++;
                } else {
                    log("Timeout waiting for withdraw confirmation after clickWidgetFast.");
                    netWithdrawAttempts++;
                    if (Rs2Widget.getWidget(ANNETTE_STORAGE_ITEM_SLOT_ID) != null && !Rs2Widget.isHidden(ANNETTE_STORAGE_ITEM_SLOT_ID)) {
                        log("Interface still open after timeout, attempting to close with ESC.");
                        Rs2Keyboard.keyPress(KeyEvent.VK_ESCAPE);
                        sleepGaussian(300, 100);
                    }
                }
            } else {
                log("ERROR: Widget " + ANNETTE_STORAGE_ITEM_SLOT_ID + " not found or hidden after fixed delay!");
                netWithdrawAttempts++;
            }
        } catch (Exception e) {
            log("ERROR during clickWidgetFast: " + e.getMessage());
            scriptLog.error("Exception in clickWidgetFast", e);
            netWithdrawAttempts++;
            if (Rs2Widget.getWidget(ANNETTE_STORAGE_ITEM_SLOT_ID) != null && !Rs2Widget.isHidden(ANNETTE_STORAGE_ITEM_SLOT_ID)) {
                Rs2Keyboard.keyPress(KeyEvent.VK_ESCAPE);
                sleepGaussian(300, 100);
            }
        }
    }


    /**
     * Handles collecting rewards from a FULL net and potentially setting it again.
     */
    private void handleFullNet(DriftNet net, DriftNetConfig config) {
        // ... (handleFullNet logic remains the same with immediate Escape fix) ...
        if (net.getNet() == null) {
            log("Attempted to handle FULL net, but GameObject was null.");
            return;
        }
        boolean interacted = Rs2GameObject.interact(net.getNet());
        if (!interacted) {
            log("Failed to interact with FULL net.");
            return;
        }

        boolean rewardInterfaceVisible = sleepUntil(
                () -> Rs2Widget.getWidget(DRIFT_NET_REWARD_GROUP_ID) != null && !Rs2Widget.isHidden(DRIFT_NET_REWARD_GROUP_ID),
                5000
        );

        if (!rewardInterfaceVisible) {
            log("Reward interface did not appear or was hidden after checking FULL net.");
            return;
        }

        boolean actionTaken = false;
        if (config.bankFish()) {
            boolean clickedBank = Rs2Widget.clickWidget(DRIFT_NET_REWARD_BANK_BUTTON_ID);
            if (clickedBank) {
                boolean bankAllVisible = sleepUntil(() -> {
                    net.runelite.api.widgets.Widget bankAllButton = Rs2Widget.getWidget(DRIFT_NET_REWARD_BANK_ALL_BUTTON_ID);
                    return bankAllButton != null && !bankAllButton.isHidden();
                }, 1500);

                if (bankAllVisible) {
                    boolean clickedBankAll = Rs2Widget.clickWidget(DRIFT_NET_REWARD_BANK_ALL_BUTTON_ID);
                    if (!clickedBankAll) log("Failed to click Bank All button.");
                    else actionTaken = true;
                } else {
                    log("Bank All button did not become visible/available.");
                }
            } else {
                log("Failed to click initial Bank button.");
            }
        } else { // Looting
            boolean clickedLoot = Rs2Widget.clickWidget(DRIFT_NET_REWARD_LOOT_BUTTON_ID);
            if (!clickedLoot) log("Failed to click Loot button.");
            else actionTaken = true;
        }

        log("Pressing Escape after Bank/Loot action.");
        Rs2Keyboard.keyPress(KeyEvent.VK_ESCAPE);
        sleep(100, 200);

        boolean closed = sleepUntil(() -> Rs2Widget.getWidget(DRIFT_NET_REWARD_GROUP_ID) == null, 2000);

        if (!closed) {
            log("WARN: Reward interface still open after pressing Escape and waiting.");
        }
    }

    /**
     * Handles setting up an UNSET net.
     */
    private void handleUnsetNet(DriftNet net) {
        // ... (handleUnsetNet logic remains the same, consider testing animation wait removal later) ...
        if (net.getNet() == null) {
            log("Attempted to handle UNSET net, but GameObject was null.");
            return;
        }
        boolean interacted = Rs2GameObject.interact(net.getNet());
        if (interacted) {
            boolean startedAnimating = sleepUntil(Rs2Player::isAnimating, 2000);
            if (startedAnimating) {
                sleepUntil(() -> !Rs2Player.isAnimating(), 5000);
            } else {
                log("Player did not start animating after interacting with UNSET net.");
            }
        } else {
            log("Failed to interact with UNSET net.");
        }
    }

    /**
     * Estimates how many currently tagged fish are likely heading towards each net.
     */
    private Map<DriftNet, Integer> calculatePendingFishCounts() {
        // ... (calculatePendingFishCounts logic remains the same with HashSet copy fix) ...
        Map<DriftNet, Integer> pendingCounts = new HashMap<>();
        List<DriftNet> nets = DriftNetPlugin.getNETS();
        if (nets == null || nets.isEmpty()) {
            log("ERROR: DriftNetPlugin.NETS is null or empty, cannot calculate pending counts.");
            return pendingCounts;
        }

        if (nets.size() >= 1) pendingCounts.put(nets.get(0), 0);
        if (nets.size() >= 2) pendingCounts.put(nets.get(1), 0);

        DriftNet net1 = (nets.size() >= 1) ? nets.get(0) : null;
        DriftNet net2 = (nets.size() >= 2) ? nets.get(1) : null;

        Set<Integer> taggedFishIndices = new HashSet<>(DriftNetPlugin.getTaggedFish().keySet()); // Use copy

        for (int fishIndex : taggedFishIndices) {
            Rs2NpcModel fishNpc = Rs2Npc.getNpcByIndex(fishIndex);
            if (fishNpc == null || fishNpc.getRuneliteNpc() == null) continue;
            LocalPoint fishLp = fishNpc.getRuneliteNpc().getLocalLocation();
            if (fishLp == null) continue;
            WorldPoint fishInstancedWp = WorldPoint.fromLocalInstance(Microbot.getClient(), fishLp);
            if (fishInstancedWp == null) continue;

            DriftNet targetNet = null;
            if (net1 != null && PRIORITY_ZONE_1.contains(fishInstancedWp)) targetNet = net1;
            else if (net2 != null && PRIORITY_ZONE_2.contains(fishInstancedWp)) targetNet = net2;
            else if (net1 != null && net2 != null) {
                int distToNet1 = fishInstancedWp.distanceTo(NET_1_CENTER);
                int distToNet2 = fishInstancedWp.distanceTo(NET_2_CENTER);
                targetNet = (distToNet1 <= distToNet2) ? net1 : net2;
            } else targetNet = (net1 != null) ? net1 : net2;

            if (targetNet != null) {
                pendingCounts.put(targetNet, pendingCounts.getOrDefault(targetNet, 0) + 1);
            }
        }
        // log("Pending Counts: Net1=" + pendingCounts.getOrDefault(net1, 0) + ", Net2=" + pendingCounts.getOrDefault(net2, 0));
        return pendingCounts;
    }

    /**
     * Helper function to calculate scores for available fish.
     * Can enforce strict net validity or use relaxed rules for fallback.
     */
    private List<Map.Entry<Rs2NpcModel, Double>> calculateFishScores(
            Set<Integer> availableIndices, Map<Integer, Integer> taggedMap, WorldPoint playerWp,
            List<DriftNet> nets, Map<DriftNet, Integer> pendingCounts, boolean enforceStrictValidity)
    {
        List<Map.Entry<Rs2NpcModel, Double>> scores = new ArrayList<>();
        if (nets == null || nets.isEmpty() || nets.size() < 2) return scores; // Safety check
        DriftNet net1 = nets.get(0);
        DriftNet net2 = nets.get(1);

        for (int fishIndex : availableIndices) {
            if (taggedMap.containsKey(fishIndex)) continue;
            Rs2NpcModel fishNpc = Rs2Npc.getNpcByIndex(fishIndex);
            if (fishNpc == null || fishNpc.getRuneliteNpc() == null) continue;
            LocalPoint fishLp = fishNpc.getRuneliteNpc().getLocalLocation();
            if (fishLp == null) continue;
            WorldPoint fishInstancedWp = WorldPoint.fromLocalInstance(Microbot.getClient(), fishLp);
            if (fishInstancedWp == null) continue;

            DriftNet targetNet; WorldPoint targetNetCenter; boolean inPriorityZone = false;
            if (PRIORITY_ZONE_1.contains(fishInstancedWp)) { targetNet = net1; targetNetCenter = NET_1_CENTER; inPriorityZone = true; }
            else if (PRIORITY_ZONE_2.contains(fishInstancedWp)) { targetNet = net2; targetNetCenter = NET_2_CENTER; inPriorityZone = true; }
            else { int d1=fishInstancedWp.distanceTo(NET_1_CENTER); int d2=fishInstancedWp.distanceTo(NET_2_CENTER); if(d1<=d2){targetNet=net1;targetNetCenter=NET_1_CENTER;}else{targetNet=net2;targetNetCenter=NET_2_CENTER;} }

            DriftNetStatus currentStatus = targetNet.getStatus();
            int currentCount = targetNet.getCount();
            int pendingCount = pendingCounts.getOrDefault(targetNet, 0);

            if (currentStatus == DriftNetStatus.FULL || currentStatus == DriftNetStatus.UNSET) continue;
            if (enforceStrictValidity && (currentCount + pendingCount > 10)) continue; // Relaxed check is > 10

            int playerDist = playerWp.distanceTo(fishInstancedWp);
            double playerDistanceScore = 1.0 / (playerDist + 1.0);
            int netDist = fishInstancedWp.distanceTo(targetNetCenter);
            double netDistanceScore = 1.0 / (netDist + 1.0);
            double netNeedScore = Math.max(0, (double)(10 - (currentCount + pendingCount)));
            double bonus = inPriorityZone ? PRIORITY_ZONE_BONUS : 0.0;
            double travelPenalty = W_TRAVEL_PENALTY * playerDist;

            double score = (W_PLAYER_DIST * playerDistanceScore * 1000) + (W_NET_DIST * netDistanceScore * 1000) + (W_NET_NEED * netNeedScore) + bonus - travelPenalty;

            scores.add(new AbstractMap.SimpleEntry<>(fishNpc, score));
        }
        return scores;
    }

    /**
     * Main state loop for actively chasing fish. Uses primary/fallback scoring.
     */
    private void manageChasingState() {
        int consecutiveFailedScores = 0;

        while (true) {
            // --- 1. Interrupt Checks ---
            List<DriftNet> nets = DriftNetPlugin.getNETS();
            if (nets == null) { log("Nets null in chase state, exiting."); return; } // Null check
            boolean needsNetAction = false;
            for (DriftNet net : nets) {
                if (net.getStatus() == DriftNetStatus.FULL || net.getStatus() == DriftNetStatus.UNSET) {
                    log("Interrupting chase state: Net is " + net.getStatus());
                    needsNetAction = true;
                    break;
                }
            }
            if (needsNetAction || !Rs2Inventory.hasItem(ItemID.DRIFT_NET)) {
                return;
            }

            // --- Get State & Copies ---
            final Set<Integer> availableFishIndices_copy = new HashSet<>(DriftNetPlugin.getFish());
            final Map<Integer, Integer> taggedFishMap_copy = new HashMap<>(DriftNetPlugin.getTaggedFish());

            LocalPoint playerLp = Microbot.getClient().getLocalPlayer().getLocalLocation();
            if (playerLp == null) { log("Player LP null, exiting chase state."); return; }

            WorldPoint playerInstancedWp = WorldPoint.fromLocalInstance(Microbot.getClient(), playerLp);
            if (playerInstancedWp == null) { log("Player WP null, exiting chase state."); return; }

            Map<DriftNet, Integer> pendingCounts = calculatePendingFishCounts(); // Uses internal copy

            // --- 2. Primary Target Search (Strict Validity) ---
            List<Map.Entry<Rs2NpcModel, Double>> fishScoresPrimary = calculateFishScores(
                    availableFishIndices_copy, taggedFishMap_copy, playerInstancedWp, nets, pendingCounts, true // enforceStrictValidity = true
            );

            Rs2NpcModel targetFish = null;

            if (!fishScoresPrimary.isEmpty()) {
                consecutiveFailedScores = 0;
                fishScoresPrimary.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));
                targetFish = fishScoresPrimary.get(0).getKey();
            } else {
                // --- No Valid Targets Found - Initiate Fallback ---
                consecutiveFailedScores++;
                // log("No strictly valid targets found (Cycle: " + consecutiveFailedScores + "). Performing fallback search..."); // Can be spammy

                List<Map.Entry<Rs2NpcModel, Double>> fishScoresFallback = calculateFishScores(
                        availableFishIndices_copy, taggedFishMap_copy, playerInstancedWp, nets, pendingCounts, false // enforceStrictValidity = false
                );

                if (!fishScoresFallback.isEmpty()) {
                    fishScoresFallback.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));
                    targetFish = fishScoresFallback.get(0).getKey();
                    // log("Fallback target found: Fish " + targetFish.getIndex()); // Can be spammy
                } else {
                    // log("Fallback search also found no targets. Waiting briefly."); // Can be spammy
                    sleep(200, 350); // Wait only if truly nothing available
                    continue; // Skip interaction phase and restart loop
                }
            }

            // --- 3. Interact ---
            if (targetFish != null) {
                final int targetFishIndex = targetFish.getRuneliteNpc().getIndex();
                boolean interacted = Rs2Npc.interact(targetFish, "Chase");

                // --- 4. Handle Confirmation (No Sleeps) ---
                if (interacted) {
                    boolean tagged = sleepUntil(() -> DriftNetPlugin.getTaggedFish().containsKey(targetFishIndex), 2000);
                    if (!tagged) {
                        log("WARN: Fish " + targetFishIndex + " tag confirmation timeout.");
                    }
                    continue; // Immediate continue
                } else {
                    log("Failed to interact (Chase): Index " + targetFishIndex);
                    continue; // Immediate continue
                }
            }

        } // End while(true)
    }

}