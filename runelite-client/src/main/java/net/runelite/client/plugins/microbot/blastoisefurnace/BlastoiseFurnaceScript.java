package net.runelite.client.plugins.microbot.blastoisefurnace;

import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.blastoisefurnace.enums.Bars;
import net.runelite.client.plugins.microbot.blastoisefurnace.enums.State;
import net.runelite.client.plugins.microbot.breakhandler.BreakHandlerScript;
import net.runelite.client.plugins.microbot.pluginscheduler.SchedulerPlugin;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.awt.event.KeyEvent;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static net.runelite.api.gameval.ItemID.*;
import static net.runelite.api.gameval.ObjectID.*;
import static net.runelite.api.gameval.VarbitID.*;

public class BlastoiseFurnaceScript extends Script {
    static final int coalBag = 12019;
    private static final int MAX_ORE_PER_INTERACTION = 27;
    private static final int MAX_ORE_PER_HYBRID_INTERACTION = 26;
    public static double version = 1.0;
    public static State state;
    static int staminaTimer;
    static boolean coalBagEmpty;
    static boolean primaryOreEmpty;
    static boolean secondaryOreEmpty;

    static {
        state = State.BANKING;
    }

    private BlastoiseFurnaceConfig config;
    boolean init = false;

    private boolean hasRequiredOresForSmithing() {
        int primaryOre = this.config.getBars().getPrimaryOre();
        int secondaryOre = this.config.getBars().getSecondaryOre() == null ? -1 : this.config.getBars().getSecondaryOre();
        boolean hasPrimaryOre = Rs2Bank.hasItem(primaryOre);
        boolean hasSecondaryOre = secondaryOre != -1 && Rs2Bank.hasItem(secondaryOre);
        return hasPrimaryOre && hasSecondaryOre;
    }

    public boolean run(BlastoiseFurnaceConfig config) {
        staminaTimer = 0;
        this.config = config;
        Microbot.enableAutoRunOn = false;
        state = State.BANKING;
        primaryOreEmpty = !Rs2Inventory.hasItem(config.getBars().getPrimaryOre());
        secondaryOreEmpty = !Rs2Inventory.hasItem(config.getBars().getSecondaryOre());
        Rs2Antiban.resetAntibanSettings();
        applyAntiBanSettings();

        this.mainScheduledFuture = this.scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) {
                    return;
                }

                if (!super.run()) {
                    return;
                }

                if (!init) {
                    checkAndTopOffCoffer();
                    init = true;
                    return;
                }


                boolean hasGauntlets;
                switch (state) {
                    case BANKING:
                        Microbot.status = "Banking";
                        if (!Rs2Bank.isOpen()) {
                            Microbot.log("Opening bank");
                            Rs2Bank.openBank();
                            sleepUntil(Rs2Bank::isOpen, 20000);
                        }

                        if (config.getBars().isRequiresCoalBag() && !Rs2Inventory.contains(coalBag)) {
                            if (!Rs2Bank.hasItem(coalBag)) {
                                Microbot.showMessage("No coal bag found in inventory and bank.");
                                this.shutdown();
                                return;
                            }

                            Rs2Bank.withdrawItem(coalBag);
                        }

                        if (config.getBars().isRequiresGoldsmithGloves()) {
                            hasGauntlets = Rs2Inventory.contains(GAUNTLETS_OF_GOLDSMITHING) || Rs2Equipment.isWearing(GAUNTLETS_OF_GOLDSMITHING);
                            if (!hasGauntlets) {
                                if (!Rs2Bank.hasItem(GAUNTLETS_OF_GOLDSMITHING)) {
                                    Microbot.showMessage("No goldsmith gauntlets found.");
                                    this.shutdown();
                                    return;
                                }

                                Rs2Bank.withdrawItem(GAUNTLETS_OF_GOLDSMITHING);
                            }
                        } else Rs2Bank.depositAll(GAUNTLETS_OF_GOLDSMITHING);

                        if (Rs2Inventory.hasItem("bar")) {
                            Rs2Bank.depositAllExcept(coalBag, GAUNTLETS_OF_GOLDSMITHING, ICE_GLOVES, SMITHING_UNIFORM_GLOVES_ICE);
                        }

                        if (!this.hasRequiredOresForSmithing()) {
                            Microbot.log("Out of ores. Walking you out for coffer safety");
                            Rs2Walker.walkTo(new WorldPoint(2930, 10196, 0));                            Rs2Player.logout();
                            this.shutdown();

                        }

                        if (!Rs2Player.hasStaminaBuffActive() && Microbot.getClient().getEnergy() < 8100) {
                            this.useStaminaPotions();
                        }

                        // Check here if dispenser contains bars. If so we need to clean-up
                        if (dispenserContainsBars()) {
                            Rs2Bank.depositAllExcept(coalBag, GAUNTLETS_OF_GOLDSMITHING, ICE_GLOVES, SMITHING_UNIFORM_GLOVES_ICE);
                            handleDispenserLooting();
                            return;
                        }else {
                            this.retrieveItemsForCurrentFurnaceInteraction();
                            state = State.SMITHING;
                        }
                        break;
                    case SMITHING:
                        System.out.println("clicking conveyor");

                        if (barsInDispenser(config.getBars()) > 0) {
                            handleDispenserLooting();
                        }

                        state = State.BANKING;
                        break;
                }
            } catch (Exception ex) {

                Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
            }

        }, 0, 200, TimeUnit.MILLISECONDS);
        return true;
    }

    private void handleTax() {
        Microbot.log("Paying noob smithing tax");
        if (!Rs2Bank.isOpen()) {
            Microbot.log("Opening bank");
            Rs2Bank.openBank();
            sleepUntil(Rs2Bank::isOpen, 20000);
        }
        Rs2Bank.depositOne(config.getBars().getPrimaryOre());
        sleep(500, 1200);
        Rs2Bank.depositOne(COAL);
        sleep(500, 1200);
        Rs2Bank.withdrawX(COINS,2500);
        sleep(500, 1200);
        Rs2Bank.closeBank();
        sleep(500, 1200);
        Rs2NpcModel blastie = Rs2Npc.getNpc("Blast Furnace Foreman");
        Rs2Npc.interact(blastie, "Pay");
        sleepUntil(Rs2Dialogue::isInDialogue,10000);
        if (Rs2Dialogue.hasSelectAnOption()) {
            Rs2Dialogue.clickOption("Yes");
            sleep(1000, 1850);
            Rs2Dialogue.clickContinue();
            sleep(500, 1300);

        }
    }
    private void handleDispenserLooting() {

        // Check if the inventory is full before interacting with the dispenser
        if (!Rs2Inventory.isFull()) {

            if(!dispenserContainsBars()){
                sleepUntil(this::dispenserContainsBars, Rs2Random.between(3000,5000));
            }

            Rs2GameObject.interact(BLAST_FURNACE_DISPENSER, "Take");

            sleepUntil(() ->
                    Rs2Widget.hasWidget("What would you like to take?") ||
                            Rs2Widget.hasWidget("How many would you like") ||
                            Rs2Widget.hasWidget("The bars are still molten!"), 5000);

            boolean noIceGlovesEquipped = Rs2Widget.hasWidget("The bars are still molten!");

            if (noIceGlovesEquipped){
                if (!Rs2Inventory.interact(ICE_GLOVES, "Wear") && !Rs2Inventory.interact(SMITHING_UNIFORM_GLOVES, "Wear") && !Rs2Inventory.interact(SMITHING_UNIFORM_GLOVES_ICE, "Wear")) {
                    Microbot.showMessage("Ice gloves or smith gloves required to loot the hot bars.");
                    Rs2Player.logout();
                    this.shutdown();
                    return;
                }
                Rs2GameObject.interact(BLAST_FURNACE_DISPENSER, "Take");
            }

            sleepUntil(() -> Rs2Widget.hasWidget("What would you like to take?") || Rs2Widget.hasWidget("How many would you like"), 3000);

            // If somehow multiple type of bars are created we need to clean up the dispenser.
            boolean multipleBarTypes = Rs2Widget.hasWidget("What would you like to take?");
            boolean canLootBar = Rs2Widget.hasWidget("How many would you like");

            if (super.run()) {
                if (canLootBar) {
                    Rs2Keyboard.keyPress(KeyEvent.VK_SPACE);
                } else if (multipleBarTypes) {
                    Rs2Keyboard.keyPress(KeyEvent.VK_SPACE);
                }
                Rs2Inventory.waitForInventoryChanges(5000);
                equipGoldSmithGauntlets(); // Optionally
            }
        }

        state = State.BANKING;
    }

    private void retrievePrimary() {
        int ore = config.getBars().getPrimaryOre();
        if (!Rs2Inventory.hasItem(ore)) {
            Rs2Bank.withdrawAll(ore);
            return;
        }
        doOreRun(false, false);
    }

    private void retrieveDoubleCoal() {
        if (!Rs2Inventory.hasItem(COAL)) {
            Rs2Bank.withdrawAll(COAL);
            return;
        }
        if (!Rs2Inventory.interact(coalBag, "Fill"))
            return;
        depositOre();
    }

    private void retrieveCoalAndPrimary() {
        int ore = config.getBars().getPrimaryOre();
        if (!Rs2Inventory.hasItem(ore)) {
            Rs2Bank.withdrawAll(ore);
            sleep(500, 1200);
            return;
        }
        if (!Rs2Inventory.interact(coalBag, "Fill"))
            return;

        sleep(500, 1200);
        Rs2Bank.closeBank();
        sleep(500, 1200);
        depositOre();
        doOreRun(false, false);
    }

    private void retrieveCoalAndGold() {
        if (!Rs2Inventory.hasItem(GOLD_ORE)) {
            Rs2Bank.withdrawAll(GOLD_ORE);
            return;
        }
        if (!Rs2Inventory.interact(coalBag, "Fill"))
            return;

        sleep(500, 1200);
        Rs2Bank.closeBank();
        sleep(500, 1200);
        depositOre();
        doOreRun(true, true);
    }

    private void retrieveGold() {
        if (!Rs2Inventory.hasItem(GOLD_ORE)) {
            Rs2Bank.withdrawAll(GOLD_ORE);
            return;
        }
        depositOre();
        doOreRun(true, true);
    }

    private void doOreRun(boolean useIceGloves, boolean waitInventoryChange) {
        Rs2Walker.walkFastCanvas(new WorldPoint(1940, 4962, 0));
        sleep(3400);
        sleepUntil(() -> barsInDispenser(config.getBars()) > 0, 10000);

        if (useIceGloves) {
            Rs2Inventory.interact(ICE_GLOVES, "wear");
            if (waitInventoryChange)
                Rs2Inventory.waitForInventoryChanges(2000);
        } else {
            sleep(400, 700);
        }
    }

    private void retrieveItemsForCurrentFurnaceInteraction() {
        final Bars bar = config.getBars();
        final int coal = Microbot.getVarbitValue(
                bar == Bars.GOLD_BAR ? BLAST_FURNACE_GOLD_ORE : BLAST_FURNACE_COAL
        );
        final int divisor = isHybrid(bar) ? MAX_ORE_PER_HYBRID_INTERACTION : MAX_ORE_PER_INTERACTION;
        final int batch = coal / divisor;

        switch (bar) {
            case GOLD_BAR:
                retrieveGold();
                break;

            case STEEL_BAR:
            case MITHRIL_BAR:
                dispatchStandard(batch, 0, retrieveDoubleCoal, retrieveCoalAndPrimary, retrievePrimary);
                break;

            case ADAMANTITE_BAR:
            case RUNITE_BAR:
                dispatchStandard(batch, 2, retrieveDoubleCoal, retrieveCoalAndPrimary, retrievePrimary);
                break;

            case HYBRID_MITHRIL_BAR:
                dispatchHybrid(batch, 0, retrieveCoalAndGold, retrieveCoalAndPrimary);
                break;

            case HYBRID_ADAMANTITE_BAR:
                dispatchHybrid(batch, 2, retrieveCoalAndGold, retrieveCoalAndPrimary);
                break;

            case HYBRID_RUNITE_BAR:
                dispatchHybrid(batch, 3, retrieveCoalAndGold, retrieveCoalAndPrimary);
                break;

            default:
                assert false : "unhandled bar type: " + bar;
        }
    }

    private boolean isHybrid(Bars bar) {
        return bar.name().startsWith("HYBRID");
    }

    private final Runnable retrievePrimary = this::retrievePrimary;
    private final Runnable retrieveCoalAndPrimary = this::retrieveCoalAndPrimary;
    private final Runnable retrieveCoalAndGold = this::retrieveCoalAndGold;
    private final Runnable retrieveDoubleCoal = this::retrieveDoubleCoal;

    private void dispatchStandard(int batch, int doubleCoalMax, Runnable doubleCoal, Runnable coalAndPrimary, Runnable primary) {
        if (batch <= doubleCoalMax) {
            doubleCoal.run();
        } else if (batch <= 6) {
            coalAndPrimary.run();
        } else {
            primary.run();
        }
    }

    private void dispatchHybrid(int batch, int goldThreshold, Runnable coalAndGold, Runnable coalAndPrimary) {
        if (batch <= goldThreshold) {
            coalAndGold.run();
        } else {
            coalAndPrimary.run();
        }
    }

    private void useStaminaPotions() {

        boolean usedPotion = false;

        // Step 1: Keep using Energy potions until energy is above 71%
        while (Microbot.getClient().getEnergy() < 6900) {
            usedPotion = usePotionIfNeeded("Energy potion", 6900);
            if (!usedPotion) {
                break; // Exit if no Energy potion is available
            }
        }

        // Step 2: If energy is above 71% but below 81%, use Stamina potion if no stamina buff is active
        if (Microbot.getClient().getEnergy() < 8100 && !Rs2Player.hasStaminaBuffActive()) {
            usedPotion = usePotionIfNeeded("Stamina potion", 8100);
        }

        // Sleep after using a potion
        if (usedPotion) {
            sleep(161, 197);
        }
    }

    private boolean usePotionIfNeeded(String potionName, int energyThreshold) {
        if (Microbot.getClient().getEnergy() < energyThreshold) {
            if (withdrawPotion(potionName)) {
                if (drinkPotion(potionName)) {
                    depositItems(potionName);
                    return true; // Potion was successfully used
                }
            }
        }
        return false; // Potion was not used
    }

    private boolean withdrawPotion(String potionName) {
        Rs2Bank.withdrawOne(potionName);
        sleep(900);
        return true;
    }

    private boolean drinkPotion(String potionName) {
        Rs2Inventory.interact(potionName, "Drink");
        sleep(900);
        return true;
    }

    private void depositItems(String potionName) {
        if (Rs2Inventory.hasItem(potionName)) {
            Rs2Bank.depositOne(potionName);
        }
        if (Rs2Inventory.hasItem(VIAL_EMPTY)) {
            Rs2Bank.depositOne(VIAL_EMPTY);
        }
    }

    private void depositOre() {
        Rs2GameObject.interact(BLAST_FURNACE_CONVEYER_BELT_CLICKABLE, "Put-ore-on");
        Rs2Inventory.waitForInventoryChanges(10000);
        if(Rs2Dialogue.hasDialogueText("You must ask the foreman's")){
            Microbot.log("Need to pay the noob tax");
            handleTax();
            Rs2GameObject.interact(BLAST_FURNACE_CONVEYER_BELT_CLICKABLE, "Put-ore-on");
            Rs2Inventory.waitForInventoryChanges(10000);
        }
        if(Rs2Dialogue.hasDialogueText("You must put money in the coffer to pay the workers.")){
            Microbot.log("Need to refill Coffer");
            checkAndTopOffCoffer();
            Rs2GameObject.interact(BLAST_FURNACE_CONVEYER_BELT_CLICKABLE, "Put-ore-on");
            Rs2Inventory.waitForInventoryChanges(10000);
        }
        if (this.config.getBars().isRequiresCoalBag()) {

            Rs2Inventory.interact(coalBag, "Empty");
            Rs2Inventory.waitForInventoryChanges(3000);

            Rs2GameObject.interact(BLAST_FURNACE_CONVEYER_BELT_CLICKABLE, "Put-ore-on");
            Rs2Inventory.waitForInventoryChanges(3000);
        }
        if (this.config.getBars().isRequiresCoalBag() && (Rs2Inventory.hasItem(SMITHING_UNIFORM_GLOVES_ICE) || Rs2Inventory.hasItem(GAUNTLETS_OF_GOLDSMITHING) || Rs2Inventory.hasItem(ICE_GLOVES))) {

            Rs2Inventory.interact(coalBag, "Empty");
            Rs2Inventory.waitForInventoryChanges(3000);

            Rs2GameObject.interact(BLAST_FURNACE_CONVEYER_BELT_CLICKABLE, "Put-ore-on");
            Rs2Inventory.waitForInventoryChanges(3000);
        }
    }

    public int barsInDispenser(Bars bar) {
        switch (bar) {
            case GOLD_BAR:
                return getBars(BLAST_FURNACE_GOLD_BARS);
            case STEEL_BAR:
                return getBars(BLAST_FURNACE_STEEL_BARS);
            case MITHRIL_BAR:
                return getBars(BLAST_FURNACE_MITHRIL_BARS);
            case ADAMANTITE_BAR:
                return getBars(BLAST_FURNACE_ADAMANTITE_BARS);
            case RUNITE_BAR:
                return getBars(BLAST_FURNACE_RUNITE_BARS);
            case HYBRID_MITHRIL_BAR:
                return fallbackBar(
                        BLAST_FURNACE_MITHRIL_BARS
                );
            case HYBRID_ADAMANTITE_BAR:
                return fallbackBar(
                        BLAST_FURNACE_ADAMANTITE_BARS
                );
            case HYBRID_RUNITE_BAR:
                return fallbackBar(
                        BLAST_FURNACE_RUNITE_BARS
                );
            default:
                return -1;
        }
    }

    private int fallbackBar(int primary) {
        int value = Microbot.getVarbitValue(primary);
        return value > 0 ? value : Microbot.getVarbitValue(net.runelite.api.gameval.VarbitID.BLAST_FURNACE_GOLD_BARS);
    }

    private int getBars(int varbitId) {
        return Microbot.getVarbitValue(varbitId);
    }

    public boolean dispenserContainsBars() {
        return Arrays.stream(new int[] {
                BLAST_FURNACE_IRON_BARS,
                BLAST_FURNACE_STEEL_BARS,
                BLAST_FURNACE_GOLD_BARS,
                BLAST_FURNACE_MITHRIL_BARS,
                BLAST_FURNACE_ADAMANTITE_BARS,
                BLAST_FURNACE_RUNITE_BARS
        }).anyMatch(id -> Microbot.getVarbitValue(id) > 0);
    }

    private void equipGoldSmithGauntlets() {
        if (config.getBars().isRequiresGoldsmithGloves()) {
            Rs2Inventory.interact(GAUNTLETS_OF_GOLDSMITHING, "Wear");
        }
    }

    private void applyAntiBanSettings() {
        Rs2AntibanSettings.antibanEnabled = true;
        Rs2AntibanSettings.naturalMouse = true;
        Rs2AntibanSettings.devDebug = true;
    }

    public void shutdown() {
        state = State.BANKING;
        primaryOreEmpty = false;
        secondaryOreEmpty = false;
        super.shutdown();
    }

    public int evaluateCofferDeposit()
    {
        assert SchedulerPlugin.class != null : "SchedulerPlugin missing";
        if (!Microbot.isPluginEnabled(SchedulerPlugin.class))
        {
            return 72000;
        }

        Plugin plugin = Microbot.getPlugin(SchedulerPlugin.class.getName());
        assert plugin instanceof SchedulerPlugin : "Invalid scheduler plugin";
        SchedulerPlugin scheduler = (SchedulerPlugin) plugin;

        if (scheduler.getNextScheduledPlugin() != null)
        {
            Duration untilNext = scheduler.getTimeBeforeNextScheduledPlugin();
            if (untilNext == null || untilNext.isZero() || untilNext.isNegative())
            {
                untilNext = scheduler.getTimeUntilNextBreak();
            }
            if (untilNext != null && !untilNext.isZero() && !untilNext.isNegative())
            {
                long millis = untilNext.toMillis();
                assert millis >= 0 : "Negative duration";
                return (int) Math.ceil(millis * 0.2); // 1 ms = 0.2 gold
            }
        }

        if (BreakHandlerScript.breakIn > 0)
        {
            assert BreakHandlerScript.breakIn >= 0 : "Negative breakIn";
            return BreakHandlerScript.breakIn * 20;   // 1 s = 20 gold
        }

        return 72000;
    }

    public void checkAndTopOffCoffer()
    {
        int lvl = Microbot.getClient().getRealSkillLevel(Skill.SMITHING);
        if (lvl < 60)
        {
            return;
        }

        int inCoffer = Microbot.getVarbitValue(BLAST_FURNACE_COFFER);
        int req      = evaluateCofferDeposit();
        if (inCoffer >= req)
        {
            return;
        }

        if (!Rs2Bank.isOpen())
        {
            Rs2Bank.openBank();
            sleepUntil(Rs2Bank::isOpen, 5000);
        }

        // — Check bank for at least 72 000 coins
        if (!Rs2Bank.hasBankItem(COINS, 72000))
        {
            shutdown();
            return;
        }

        Rs2Bank.withdrawX(COINS, req);
        sleep(600, 900);
        Rs2Bank.closeBank();
        sleep(400, 800);

        Rs2GameObject.interact(BLAST_FURNACE_AUTOMATA_COFFER, "use");
        Rs2Player.waitForWalking(2400);
        if (Rs2Dialogue.hasDialogueOption("deposit", false))
        {
            Rs2Widget.clickWidget("deposit");
            sleepUntil(() -> Rs2Dialogue.hasQuestion("Deposit how much?"), 5000);
            Rs2Keyboard.typeString(String.valueOf(req));
            Rs2Keyboard.enter();
            Rs2Inventory.waitForInventoryChanges(1200);
        }
    }

}