package net.runelite.client.plugins.microbot.Acun.farmTreeRun;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Acun.farmTreeRun.enums.FarmTreeRunState;
import net.runelite.client.plugins.microbot.Acun.farmTreeRun.enums.FruitTreeEnum;
import net.runelite.client.plugins.microbot.Acun.farmTreeRun.enums.TreeEnums;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.antiban.enums.ActivityIntensity;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static net.runelite.client.plugins.microbot.Acun.farmTreeRun.enums.FarmTreeRunState.*;


/**
 * There are two tree types:
 * 1. 'Tree' refers to regular trees (e.g., maple, yew).
 * 2. 'Fruit Tree' refers to fruit trees (e.g., apple, banana).
 */
public class FarmTreeRunScript extends Script {
    public static FarmTreeRunState botStatus;
    public static boolean test = false;
    int treeSaplingsCount = 0;
    int fruitTreeSaplingsCount = 0;
    List<Supplier<Boolean>> selectedTreePatches;
    List<Supplier<Boolean>> selectedFruitPatches;

    private enum TreeKind {
        FRUIT_TREE,
        TREE
    }

    private enum PaymentKind {
        PROTECT,
        CLEAR
    }

    @Getter
    @RequiredArgsConstructor
    public enum Patch {
        GNOME_STRONGHOLD_FRUIT_TREE_PATCH(7962, new WorldPoint(2473, 3446, 0), TreeKind.FRUIT_TREE, 1, 0),
        GNOME_STRONGHOLD_TREE_PATCH(19147, new WorldPoint(2437, 3417, 0), TreeKind.TREE, 1, 0),
        TREE_GNOME_VILLAGE_FRUIT_TREE_PATCH(7963, new WorldPoint(2490, 3181, 0), TreeKind.FRUIT_TREE, 1, 0),
        FARMING_GUILD_TREE_PATCH(33732, new WorldPoint(1234, 3734, 0), TreeKind.TREE, 65, 0),
        FARMING_GUILD_FRUIT_TREE_PATCH(0000000, new WorldPoint(1244, 3757, 0), TreeKind.FRUIT_TREE, 85, 0),
        TAVERLEY_TREE_PATCH(8388, new WorldPoint(2936, 3440, 0), TreeKind.TREE, 1, 0),
        FALADOR_TREE_PATCH(8389, new WorldPoint(3001, 3374, 0), TreeKind.TREE, 1, 0),
        LUMBRIDGE_TREE_PATCH(8391, new WorldPoint(3195, 3228, 0), TreeKind.TREE, 1, 0),
        VARROCK_TREE_PATCH(8390, new WorldPoint(3226, 3458, 0), TreeKind.TREE, 1, 0),
        BRIMHAVEN_FRUIT_TREE_PATCH(7964, new WorldPoint(2765, 3213, 0), TreeKind.FRUIT_TREE, 1, 0),
        CATHERBY_FRUIT_TREE_PATCH(7965, new WorldPoint(2858, 3432, 0), TreeKind.FRUIT_TREE, 1, 0),
        LLETYA_FRUIT_TREE_PATCH(0000000, new WorldPoint(2345, 3163, 0), TreeKind.FRUIT_TREE, 1, 0);

        private final int id;
        private final WorldPoint location;
        private final TreeKind kind;
        private final int farmingLevel;
        private final int leprechaunId;

        public boolean hasRequiredLevel() {
            if (Rs2Player.getSkillRequirement(Skill.FARMING, this.farmingLevel))
                return true;
            Microbot.showMessage(this.name() + " requires level " + this.farmingLevel + " farming.");
            return false;
        }
    }

    public boolean run(FarmTreeRunConfig config) {
        Microbot.enableAutoRunOn = false;
        Rs2Antiban.resetAntibanSettings();
        Rs2AntibanSettings.naturalMouse = true;
        Rs2Antiban.setActivityIntensity(ActivityIntensity.LOW);

        botStatus = FarmTreeRunState.BANKING;

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;
                calculatePatches(config);
                long startTime = System.currentTimeMillis();
                if (Rs2AntibanSettings.actionCooldownActive) return;

                dropEmptyPlantPots();
                Patch patch = null;

                switch (botStatus) {
                    case BANKING:
                        if (config.banking()) {
                            bank(config);
                        } else {
                            botStatus = HANDLE_GNOME_STRONGHOLD_FRUIT_PATCH;
                        }
                        break;
                    case HANDLE_GNOME_STRONGHOLD_FRUIT_PATCH:
                        patch = Patch.GNOME_STRONGHOLD_FRUIT_TREE_PATCH;
                        if (config.gnomeStrongholdFruitTreePatch() && walkToLocation(patch.getLocation())) {
                            boolean handledPatch = handlePatch(config, patch);
                            if (!handledPatch)
                                return;
                        }
                        botStatus = HANDLE_GNOME_STRONGHOLD_TREE_PATCH;
                        break;
                    case HANDLE_GNOME_STRONGHOLD_TREE_PATCH:
                        patch = Patch.GNOME_STRONGHOLD_TREE_PATCH;
                        if (config.gnomeStrongholdTreePatch() && walkToLocation(patch.getLocation())) {
                            boolean handledPatch = handlePatch(config, patch);
                            if (!handledPatch)
                                return;
                        }
                        botStatus = HANDLE_TREE_GNOME_VILLAGE_FRUIT_TREE_PATCH;
                        break;
                    case HANDLE_TREE_GNOME_VILLAGE_FRUIT_TREE_PATCH:
                        patch = Patch.TREE_GNOME_VILLAGE_FRUIT_TREE_PATCH;
                        if (config.treeGnomeVillageFruitTreePatch() && walkToLocation(patch.getLocation())) {
                            boolean handledPatch = handlePatch(config, patch);
                            if (!handledPatch)
                                return;
                        }
                        botStatus = HANDLE_FARMING_GUILD_TREE_PATCH;
                        break;
                    case HANDLE_FARMING_GUILD_TREE_PATCH:
                        patch = Patch.FARMING_GUILD_TREE_PATCH;
                        if (config.farmingGuildTreePatch() && walkToLocation(patch.getLocation()) && patch.hasRequiredLevel()) {
                            boolean handledPatch = handlePatch(config, patch);
                            if (!handledPatch)
                                return;
                        }
                        botStatus = HANDLE_FARMING_GUILD_FRUIT_PATCH;
                        break;
                    case HANDLE_FARMING_GUILD_FRUIT_PATCH:
                        patch = Patch.FARMING_GUILD_FRUIT_TREE_PATCH;
                        if (config.farmingGuildFruitTreePatch() && walkToLocation(patch.getLocation()) && patch.hasRequiredLevel()) {
                            boolean handledPatch = handlePatch(config, patch);
                            if (!handledPatch)
                                return;
                        }
                        botStatus = HANDLE_TAVERLEY_TREE_PATCH;
                        break;
                    case HANDLE_TAVERLEY_TREE_PATCH:
                        patch = Patch.TAVERLEY_TREE_PATCH;
                        if (config.taverleyTreePatch() && walkToLocation(patch.getLocation())) {
                            boolean handledPatch = handlePatch(config, patch);
                            if (!handledPatch)
                                return;
                        }
                        botStatus = HANDLE_FALADOR_TREE_PATCH;
                        break;
                    case HANDLE_FALADOR_TREE_PATCH:
                        patch = Patch.FALADOR_TREE_PATCH;
                        if (config.faladorTreePatch() && walkToLocation(patch.getLocation())) {
                            boolean handledPatch = handlePatch(config, patch);
                            if (!handledPatch)
                                return;
                        }
                        botStatus = HANDLE_LUMBRIDGE_TREE_PATCH;
                        break;
                    case HANDLE_LUMBRIDGE_TREE_PATCH:
                        patch = Patch.LUMBRIDGE_TREE_PATCH;
                        if (config.lumbridgeTreePatch() && walkToLocation(patch.getLocation())) {
                            boolean handledPatch = handlePatch(config, patch);
                            if (!handledPatch)
                                return;
                        }
                        botStatus = HANDLE_VARROCK_TREE_PATCH;
                        break;
                    case HANDLE_VARROCK_TREE_PATCH:
                        patch = Patch.VARROCK_TREE_PATCH;
                        if (config.varrockTreePatch() && walkToLocation(patch.getLocation())) {
                            boolean handledPatch = handlePatch(config, patch);
                            if (!handledPatch)
                                return;
                        }
                        botStatus = HANDLE_BRIMHAVEN_FRUIT_TREE_PATCH;
                    case HANDLE_BRIMHAVEN_FRUIT_TREE_PATCH:
                        patch = Patch.BRIMHAVEN_FRUIT_TREE_PATCH;
                        if (config.brimhavenFruitTreePatch() && walkToLocation(patch.getLocation())) {
                            boolean handledPatch = handlePatch(config, patch);
                            if (!handledPatch)
                                return;
                        }
                        botStatus = HANDLE_CATHERBY_FRUIT_TREE_PATCH;
                        break;
                    case HANDLE_CATHERBY_FRUIT_TREE_PATCH:
                        patch = Patch.CATHERBY_FRUIT_TREE_PATCH;
                        if (config.catherbyFruitTreePatch() && walkToLocation(patch.getLocation())) {
                            boolean handledPatch = handlePatch(config, patch);
                            if (!handledPatch)
                                return;
                        }
                        botStatus = HANDLE_LLETYA_FRUIT_TREE_PATCH;
                        break;
                    case HANDLE_LLETYA_FRUIT_TREE_PATCH:
                        patch = Patch.LLETYA_FRUIT_TREE_PATCH;
                        if (config.lletyaFruitTreePatch() && walkToLocation(patch.getLocation())) {
                            boolean handledPatch = handlePatch(config, patch);
                            if (!handledPatch)
                                return;
                        }
                        botStatus = FINISHED;
                        break;
                    case FINISHED:
                        Microbot.getClientThread().runOnClientThread(() ->
                                Microbot.getClient().addChatMessage(ChatMessageType.ENGINE, "", "Made with love by Acun.", "Acun", false)
                        );

                        Microbot.log("Finished tree run.");
                        shutdown();
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

    private void calculatePatches(FarmTreeRunConfig config) {
        selectedTreePatches = List.of(
                config::faladorTreePatch,
                config::gnomeStrongholdTreePatch,
                config::lumbridgeTreePatch,
                config::taverleyTreePatch,
                config::varrockTreePatch,
                config::farmingGuildTreePatch
        );

        selectedFruitPatches = List.of(
                config::brimhavenFruitTreePatch,
                config::catherbyFruitTreePatch,
                config::farmingGuildFruitTreePatch,
                config::lletyaFruitTreePatch,
                config::gnomeStrongholdFruitTreePatch,
                config::treeGnomeVillageFruitTreePatch
        );

        treeSaplingsCount = (int) selectedTreePatches.stream()
                .filter(Supplier::get) // Call the method and check if it returns true
                .count();

        fruitTreeSaplingsCount = (int) selectedFruitPatches.stream()
                .filter(Supplier::get) // Call the method and check if it returns true
                .count();

        int total = treeSaplingsCount + fruitTreeSaplingsCount;

        if (total <= 0) {
            Microbot.log("You must select at least one patch. Shut down.");
            Microbot.showMessage("You must select at least one patch. Shut down.");
            shutdown();
        }

        if (treeSaplingsCount > 0)
            config.selectedTree().hasRequiredLevel();

        if (fruitTreeSaplingsCount > 0)
            config.selectedFruitTree().hasRequiredLevel();
    }

    private void dropEmptyPlantPots() {
        int emptyPlantPot = ItemID.EMPTY_PLANT_POT;
        if (!Rs2Player.isAnimating() && !Rs2Player.isMoving() && !Rs2Player.isInteracting()) {
            if (Rs2Inventory.hasItem(emptyPlantPot)) {
                Rs2Inventory.dropAll(emptyPlantPot);
                sleepUntil(() -> !Rs2Inventory.hasItem(emptyPlantPot), 8000);
            }
        }
    }

    private boolean walkToLocation(WorldPoint location) {
        if (!Rs2Player.isAnimating()) {
            Rs2Walker.walkTo(location);
            sleepUntil(() -> Rs2Player.distanceTo(location) < 10);
            return Rs2Player.distanceTo(location) < 10;
        }
        return false;
    }

    private void bank(FarmTreeRunConfig config) {
        if (Rs2Bank.openBank() || Rs2Bank.walkToBank()) {
            sleepUntil(() -> !Rs2Player.isAnimating());
            if (!Rs2Bank.isOpen())
                return;
            sleep(600, 2200);

            if (!Rs2Inventory.isEmpty()) {
                Rs2Bank.depositAll();
                Rs2Inventory.waitForInventoryChanges(5000);
            }

            if (!Rs2Equipment.isNaked() && !alreadyWearingGraceful()) {
                Rs2Bank.depositEquipment();
                sleepUntil(Rs2Equipment::isNaked);
                sleep(500, 2200);
            }

            // TODO: I don't have graceful boots yet, alternative is boots of lightness
            equipGraceful();

            List<FarmingItem> items = new ArrayList<>();

            // Add initial items
            items.add(new FarmingItem(ItemID.COINS_995, 3000));
            items.add(new FarmingItem(ItemID.SPADE, 1));
            items.add(new FarmingItem(ItemID.RAKE, 1));
            items.add(new FarmingItem(ItemID.SEED_DIBBER, 1));
            items.add(new FarmingItem(ItemID.BOTTOMLESS_COMPOST_BUCKET_22997, 1));

            if (config.farmingGuildTreePatch() || config.farmingGuildFruitTreePatch()) {
                if (Rs2Bank.hasItem(ItemID.SKILLS_NECKLACE2)) {
                    items.add(new FarmingItem(ItemID.SKILLS_NECKLACE2, 1));
                } else if (Rs2Bank.hasItem(ItemID.SKILLS_NECKLACE3)) {
                    items.add(new FarmingItem(ItemID.SKILLS_NECKLACE3, 1));
                } else if (Rs2Bank.hasItem(ItemID.SKILLS_NECKLACE4)) {
                    items.add(new FarmingItem(ItemID.SKILLS_NECKLACE4, 1));
                } else if (Rs2Bank.hasItem(ItemID.SKILLS_NECKLACE5)) {
                    items.add(new FarmingItem(ItemID.SKILLS_NECKLACE5, 1));
                } else {
                    items.add(new FarmingItem(ItemID.SKILLS_NECKLACE6, 1));
                }
            }

            TreeEnums selectedTree = config.selectedTree();
            FruitTreeEnum selectedFruitTree = config.selectedFruitTree();

            if (treeSaplingsCount > 0)
                items.add(new FarmingItem(selectedTree.getSaplingId(), treeSaplingsCount));

            if (fruitTreeSaplingsCount > 0)
                items.add(new FarmingItem(selectedFruitTree.getSaplingId(), fruitTreeSaplingsCount));

            if (config.protectTrees())
                items.add(new FarmingItem(selectedTree.getPaymentId(), selectedTree.getPaymentAmount() * treeSaplingsCount, true));

            if (config.protectFruitTrees())
                items.add(new FarmingItem(selectedFruitTree.getPaymentId(), selectedFruitTree.getPaymentAmount() * fruitTreeSaplingsCount, true));

            if (config.taverleyTreePatch())
                items.add(new FarmingItem(ItemID.TAVERLEY_TELEPORT, 1));

            if (config.lletyaFruitTreePatch()) {
                if (Rs2Bank.hasItem(ItemID.TELEPORT_CRYSTAL_1)) {
                    items.add(new FarmingItem(ItemID.TELEPORT_CRYSTAL_1, 1));
                } else if (Rs2Bank.hasItem(ItemID.TELEPORT_CRYSTAL_2)) {
                    items.add(new FarmingItem(ItemID.TELEPORT_CRYSTAL_2, 1));
                } else if (Rs2Bank.hasItem(ItemID.TELEPORT_CRYSTAL_3)) {
                    items.add(new FarmingItem(ItemID.TELEPORT_CRYSTAL_3, 1));
                } else if (Rs2Bank.hasItem(ItemID.TELEPORT_CRYSTAL_4)) {
                    items.add(new FarmingItem(ItemID.TELEPORT_CRYSTAL_4, 1));
                } else {
                    items.add(new FarmingItem(ItemID.TELEPORT_CRYSTAL_5, 1));
                }
            }

            items.add(new FarmingItem(ItemID.LAW_RUNE, 10));
            items.add(new FarmingItem(ItemID.FIRE_RUNE, 30));
            items.add(new FarmingItem(ItemID.AIR_RUNE, 30));
            items.add(new FarmingItem(ItemID.EARTH_RUNE, 30));
            items.add(new FarmingItem(ItemID.WATER_RUNE, 30));


//              TODO: Need to handle what happens if a required item does not exist

            // Loop through the items and perform withdrawals
            for (FarmingItem item : items) {
                if (!super.run() || !Microbot.isLoggedIn() || Microbot.pauseAllScripts)
                    shutdown();
                int itemId = item.getItemId();
                int quantity = item.getQuantity();
                boolean noted = item.isNoted();

                if (!Rs2Bank.hasItem(itemId)) {
                    Microbot.log("Item not found: " + Microbot.getClient().getItemDefinition(itemId).getName() + ". Plugin shut down.");
                    Microbot.showMessage("Item not found: " + Microbot.getClient().getItemDefinition(itemId).getName() + ". Plugin shut down.");
                    shutdown();
                }

//              Handle items which require to be noted
                if (noted && !Rs2Bank.hasWithdrawAsNote()) {
                    Rs2Bank.setWithdrawAsNote();
                    sleep(600, 2000);
                } else if (Rs2Bank.hasWithdrawAsNote()) { // Disables 'Note' toggle
                    Rs2Bank.setWithdrawAsItem();
                }

                if (quantity == 1) {
                    Rs2Bank.withdrawOne(itemId);
                } else {
                    Rs2Bank.withdrawX(itemId, quantity);
                }

                sleep(250, 2400);
            }

            Rs2Bank.closeBank();
            botStatus = HANDLE_GNOME_STRONGHOLD_FRUIT_PATCH;
        }
    }

    private boolean handlePatch(FarmTreeRunConfig config, Patch patch) {
        String[] possibleActions = {"Check-health", "Chop-down", "Chop down", "Pick-fruit", "Rake", "Clear", "Inspect"};
        GameObject treePatch = null;
        String foundAction = null;

        // Loop through the possible actions and try to find the tree patch with any valid action
        for (String action : possibleActions) {
            treePatch = Rs2GameObject.findObjectByImposter(patch.getId(), action);  // Find object by patchId and action
            if (treePatch != null) {
                foundAction = action;
                break;  // Exit the loop once we find the patch with a valid action
            }
        }

        // If no tree patch is, print an error and return
        if (treePatch == null) {
            System.out.println("Tree patch not found with any of the possible actions!");
            return false;
        }

        boolean done = false;
        boolean treePlanted = false;
        boolean protectionHandled = false;

        // Handle the patch based on the action found
        switch (foundAction) {
            case "Check-health":
                handleCheckHealth(treePatch);
                break;
            case "Chop-down":
            case "Chop down":
                handlePayment(config, patch, PaymentKind.CLEAR);
                break;
            case "Pick-fruit":
                handlePickingFruit(treePatch, config, patch);
                break;
            case "Rake":
                handleRakeAction(treePatch);
                break;
            case "Clear":
                handleClearAction(treePatch);
                break;
            case "Inspect":
                if (handlePlantingTree(treePatch, patch, config))
                    treePlanted = true;
                if (treePlanted && handlePayment(config, patch, PaymentKind.PROTECT))
                    protectionHandled = true;
                if (treePlanted && protectionHandled)
                    done = true;
                break;
            default:
                System.out.println("Unexpected action found on tree patch: " + foundAction);
                break;
        }

        if (patch.kind == TreeKind.FRUIT_TREE) {
            boolean notedFruit = handleNotingFruit(patch);
            return notedFruit && done;
        }

        return done;
    }

    private boolean handleNotingFruit(Patch patch) {
        // Array of fruit item IDs
        int[] fruitIds = {
                ItemID.COOKING_APPLE,
                ItemID.BANANA,
                ItemID.ORANGE,
                ItemID.CURRY_LEAF,
                ItemID.PINEAPPLE,
                ItemID.PAPAYA_FRUIT,
                ItemID.COCONUT,
                ItemID.DRAGONFRUIT
        };

        if (!Rs2Inventory.hasItem(fruitIds)) return true;

        // Iterate through the fruit IDs
        for (int fruitId : fruitIds) {
            if (Rs2Inventory.hasItem(fruitId)) {
                // Interact with the specific fruit found
                Rs2Inventory.useItemOnNpc(fruitId, patch.getLeprechaunId());
                return false; // Return false if any fruit is found and interacted with
            }
        }
        return true;
    }

    /**
     * Handles both 'Chop-down' and 'Guard' payment
     *
     * @param config Configurations for this plugin. It should not be {@code null}.
     * @return {@code true} if payment was successful, else {@code false}
     */
    private boolean handlePayment(FarmTreeRunConfig config, Patch patch, PaymentKind action) {
        if (!isPatchEmpty(patch) && !shouldProtectTree(config, patch))
            return true;

        if (!isPatchEmpty(patch) && !shouldProtectFruitTree(config, patch))
            return true;

        Rs2NpcModel treeGardener = null;
        treeGardener = Rs2Npc.getNearestNpcWithAction("Pay");
        Rs2Npc.interact(treeGardener, "Pay");

//      Farming guild tree patch action is called
        if (treeGardener == null) {
            treeGardener = Rs2Npc.getNpc("Rosie");
            Rs2Npc.interact(treeGardener, "Pay (tree patch)");
        }

        sleepUntil(Rs2Dialogue::isInDialogue, 5000);
        if (!Rs2Dialogue.hasSelectAnOption()) {
            return Rs2Dialogue.hasDialogueText("Leave it with me") || Rs2Dialogue.hasDialogueText("already looking after that patch");
        }
        Rs2Dialogue.clickContinue();
        sleep(500, 850);

        if (Rs2Dialogue.hasSelectAnOption()) {
            Rs2Dialogue.clickOption("Yes");
            sleepUntil(() -> isPatchEmpty(patch), 6000);
            if (isPatchEmpty(patch)) {
                return true;
            }
            shutdown();
            System.out.println("Failed gardener money payment.");
            return false;
        } else {
            System.out.println("Failed gardener payment.");
        }

        return false;
    }

    private boolean handlePlantingTree(GameObject treePatch, Patch patch, FarmTreeRunConfig config) {

        System.out.println("Planting tree");

//      Not fully grown
        if (Rs2GameObject.findObjectByImposter(patch.getId(), "Chop down") != null)
            return false;

        // Skip if patch is not empty
        if (!isPatchEmpty(patch))
            return true;

        // Select which sapling to use
        int saplingToUse = patch.kind == TreeKind.TREE ? config.selectedTree().getSaplingId() : config.selectedFruitTree().getSaplingId();

        // Check if protect tree is on, else use compost
        if (shouldUseCompost(config, patch)) {
            Rs2Inventory.useItemOnObject(ItemID.BOTTOMLESS_COMPOST_BUCKET_22997, treePatch.getId());
            Rs2Player.waitForXpDrop(Skill.FARMING, 2000);
            sleep(750, 3200);
        }

        sleep(250, 1000);
        if (!Rs2GameObject.hasAction(Rs2GameObject.findObjectComposition(patch.id), "Rake")) {
            Rs2Inventory.useItemOnObject(saplingToUse, treePatch.getId());
            Rs2Inventory.waitForInventoryChanges(3000);
            sleep(750, 2400);
            return !isPatchEmpty(patch);
        }
        Rs2Inventory.deselect();
        return false;
    }

    private void handlePickingFruit(GameObject fruitTreePatch, FarmTreeRunConfig config, Patch patch) {
        System.out.println("Checking health...");
        handleNotingFruit(patch);
        Rs2GameObject.interact(fruitTreePatch, "Pick-fruit");
        // Wait for the picking to complete (player stops animating and patch no longer has the "Pick" action)
        sleepUntil(() -> !Rs2GameObject.hasAction(Rs2GameObject.findObjectComposition(fruitTreePatch.getId()), "Pick-fruit"), 12000);
        sleep(400, 1500);
        handleNotingFruit(patch);
    }

    private void handleCheckHealth(GameObject treePatch) {
        System.out.println("Checking health...");

        // Rake the patch
        Rs2GameObject.interact(treePatch, "Check-health");
        Rs2Player.waitForXpDrop(Skill.FARMING);
        sleep(250, 3500);
    }

    private void handleRakeAction(GameObject treePatch) {
        System.out.println("Raking the patch...");

        // Rake the patch
        Rs2GameObject.interact(treePatch, "rake");

        Rs2Player.waitForAnimation();
        sleepUntil(() -> !Rs2Player.isAnimating() && !Rs2Player.isInteracting());

        // Drop the weeds (assuming weeds are added to the inventory)
        if (!Rs2Player.isMoving() &&
                !Rs2Player.isAnimating() &&
                !Rs2Player.isInteracting() && !Rs2Player.isMoving()) {
            System.out.println("Dropping weeds...");
            Rs2Inventory.dropAll(ItemID.WEEDS);
            Rs2Player.waitForAnimation();
            sleepUntil(() -> !Rs2Player.isAnimating() && !Rs2Player.isInteracting());
        }
    }

    private void handleClearAction(GameObject treePatch) {
        System.out.println("Clearing the tree patch...");

        // Try to interact with the patch using the "clear" action
        boolean interactionSuccess = Rs2GameObject.interact(treePatch, "clear");
        Rs2Player.waitForAnimation();
        sleepUntil(() -> !Rs2Player.isAnimating());

        if (!interactionSuccess) {
            System.out.println("Failed to interact with the tree patch to clear it.");
            return;
        }

        // Wait for the clearing animation to finish
        Rs2Player.waitForAnimation();
        sleepUntil(() -> !Rs2Player.isAnimating() && Rs2Player.isInteracting() && Rs2Player.isMoving());
    }

    private void equipGraceful() {
        checkBeforeWithdrawAndEquip("GRACEFUL GLOVES");
        checkBeforeWithdrawAndEquip("GRACEFUL LEGS");
        checkBeforeWithdrawAndEquip("GRACEFUL CAPE");
        checkBeforeWithdrawAndEquip("Boots of lightness");
        checkBeforeWithdrawAndEquip("GRACEFUL HOOD");
        checkBeforeWithdrawAndEquip("GRACEFUL TOP");
    }

    private void checkBeforeWithdrawAndEquip(String itemName) {
        if (!Rs2Equipment.isWearing(itemName)) {
            Rs2Bank.withdrawAndEquip(itemName);
            sleep(500, 1400);
        }
    }

    private boolean alreadyWearingGraceful() {
        return Rs2Equipment.isWearing("GRACEFUL LEGS")
                && Rs2Equipment.isWearing("GRACEFUL TOP")
                && Rs2Equipment.isWearing("GRACEFUL HOOD")
                && Rs2Equipment.isWearing("GRACEFUL CAPE");
    }

    private boolean shouldProtectTree(FarmTreeRunConfig config, Patch patch) {
        return patch.kind == TreeKind.TREE && config.protectTrees();
    }

    private boolean shouldProtectFruitTree(FarmTreeRunConfig config, Patch patch) {
        return patch.kind == TreeKind.FRUIT_TREE && config.protectFruitTrees();
    }

    private boolean shouldUseCompost(FarmTreeRunConfig config, Patch patch) {
        if (!config.protectTrees() && patch.kind == TreeKind.TREE)
            return true;

        return !config.protectFruitTrees() && patch.kind == TreeKind.FRUIT_TREE;
    }

    private boolean isPatchEmpty(Patch patch) {
        String name = Rs2GameObject.getObjectComposition(patch.getId()).getName().toLowerCase();
        return name.endsWith("patch");
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
}

/**
 * Tracks the farming patches from TimeTrackingPlugin.
 * Checks if specified patches are fully grown before going for run.
 * specified in the {@code ikiFarmConfig} object.
 *
 * @param config Configurations for this plugin. It should not be {@code null}.
 * @return {@code true} if all specified farming patches are fully grown and to check health,
 * {@code false} otherwise.
 */
//    private boolean trackFarmingPatches(ikiFarmConfig config) {
/// /        If fruit tree patch -> use compost (Can we use compost on weeded patch???)
/// /        If !weed on patch -> Use sapling on tree patch
/// /        Check if patch contains the planted tree. Can we use Runelite time tracking plugin?
//
//
//        TimeTrackingPlugin timeTrackingPlugin = (TimeTrackingPlugin) Microbot.getPlugin(TimeTrackingPlugin.class.getName());
//
//        FarmingWorld farmingWorld = timeTrackingPlugin.farmingTracker.farmingWorld;
//        WorldPoint location = client.getLocalPlayer().getWorldLocation();
//        Collection<FarmingRegion> newRegions = farmingWorld.getRegionsForLocation(location);
//        for (FarmingRegion region : newRegions) {
//            for (FarmingPatch patch : region.getPatches()) {
//                PatchPrediction prediction = timeTrackingPlugin.farmingTracker.predictPatch(patch);
//                Microbot.log(String.valueOf(prediction.getProduce()));
//                Microbot.log(String.valueOf(prediction.getCropState()));
//                Microbot.log(prediction.getStage() + "/" + prediction.getStages());
//            }
//        }
//
//        return true;
//    }