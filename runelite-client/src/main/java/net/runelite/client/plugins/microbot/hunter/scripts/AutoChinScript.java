package net.runelite.client.plugins.microbot.hunter.scripts;

import net.runelite.api.Item;
import net.runelite.api.ItemID;
import net.runelite.api.ObjectID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.hunter.AutoHunterConfig;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;

import java.util.concurrent.TimeUnit;

enum State {
    IDLE,
    CATCHING,
    DROPPING,
    LAYING
}


public class AutoChinScript extends Script {

    public static boolean test = false;
    public static String version = "1.1.0";
    State currentState = State.IDLE;
    public boolean run(AutoHunterConfig config) {
        Microbot.enableAutoRunOn = false;
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;
                long startTime = System.currentTimeMillis();

                switch(currentState) {
                    case IDLE:
                        handleIdleState();
                        break;
                    case DROPPING:
                        handleDroppingState(config);
                        break;
                    case CATCHING:
                        handleCatchingState(config);
                        break;
                    case LAYING:
                        handleLayingState(config);
                        break;
                }

                long endTime = System.currentTimeMillis();
                long totalTime = endTime - startTime;
                System.out.println("Total time for loop " + totalTime);

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }, 0, 600, TimeUnit.MILLISECONDS);
        return true;
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

    private void handleIdleState() {
        try {
            // If there are box traps on the floor, interact with them first
            if (Rs2GroundItem.interact(ItemID.BOX_TRAP, "lay", 4)) {
                currentState = State.LAYING;
                return;
            }

            // If our inventory is full of ferrets
            if(Rs2Inventory.getEmptySlots() <= 1 && Rs2Inventory.contains(ItemID.FERRET)){
                // ferrets have the option release and not drop
                while(Rs2Inventory.contains(ItemID.FERRET)){
                    Rs2Inventory.interact(ItemID.FERRET, "Release");
                    sleep(0,750);
                    if(!Rs2Inventory.contains(ItemID.FERRET)){
                        break;
                    }
                }
                currentState = State.DROPPING;
                return;
            }

            // If there are shaking boxes, interact with them
            if (Rs2GameObject.interact(ObjectID.SHAKING_BOX_9384, "reset", 4)) {
                currentState = State.CATCHING;
                return;
            }
            // If there are shaking boxes, interact with them
            if (Rs2GameObject.interact(ObjectID.SHAKING_BOX_9383, "reset", 4)) {
                currentState = State.CATCHING;
                return;
            }
            // If there are shaking boxes, interact with them
            if (Rs2GameObject.interact(ObjectID.SHAKING_BOX_9382, "reset", 4)) {
                currentState = State.CATCHING;
                return;
            }

            // Interact with traps that have not caught anything
            if (Rs2GameObject.interact(ObjectID.BOX_TRAP_9385, "reset", 4)) {
                currentState = State.CATCHING;
            }
        } catch (Exception ex) {
            Microbot.log(ex.getMessage());
            ex.printStackTrace();
            currentState = State.CATCHING;
        }
    }

    private void handleDroppingState(AutoHunterConfig config) {
        sleep(config.minSleepAfterLay(), config.maxSleepAfterLay());
        currentState = State.IDLE;
    }

    private void handleCatchingState(AutoHunterConfig config) {
        sleep(config.minSleepAfterCatch(), config.maxSleepAfterCatch());
        currentState = State.IDLE;
    }

    private void handleLayingState(AutoHunterConfig config) {
        sleep(config.minSleepAfterLay(), config.maxSleepAfterLay());
        currentState = State.IDLE;
    }
}
