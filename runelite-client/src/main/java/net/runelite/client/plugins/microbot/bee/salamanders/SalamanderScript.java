package net.runelite.client.plugins.microbot.bee.salamanders;

import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.hunter.HunterTrap;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SalamanderScript extends Script {
    public static final int SMALL_FISHING_NET = 303;
    public static final int ROPE = 954;
    public boolean run(SalamanderConfig config, SalamanderPlugin plugin) {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!super.run()) return;
                if (!Microbot.isLoggedIn()) return;
                if (!this.isRunning()) return;

                // Get selected salamander type from config
                SalamanderHunting salamanderType = config.salamanderHunting();
                if (salamanderType == null) {
                    Microbot.log("Please select a salamander type in the config");
                    return;
                }

                // Walk to area if not nearby
                if (!isNearSalamanderArea(salamanderType)) {
                    Microbot.log("Walking to " + salamanderType.getName() + " hunting area");
                    Rs2Walker.walkTo(salamanderType.getHuntingPoint());
                    return;
                }

                // Check if we have enough supplies
                if (Rs2Inventory.count(ROPE) < 1 || Rs2Inventory.count(SMALL_FISHING_NET) < 1) {
                    Microbot.log("Not enough supplies, need ropes and fishing nets");
                    return;
                }

                // Count existing traps from plugin's trap map
                int activeTrapCount = plugin.getTraps().size();
                int maxTraps = getMaxTrapsForHunterLevel();

                // Tend to active traps
                boolean handledTrap = handleExistingTraps(plugin, config);
                if (handledTrap) return;

                // Set new traps if we have space and supplies
                if (activeTrapCount < maxTraps && !IsRopeOnTheGround()) {
                    setNewTrap(salamanderType, config);
                }

            } catch (Exception ex) {
                System.out.println("Salamander Script Error: " + ex.getMessage());
                ex.printStackTrace();
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        return true;
    }

    private boolean isNearSalamanderArea(SalamanderHunting salamanderType) {
        // Check if within ~20 tiles of the hunting point
        WorldPoint currentLocation = Rs2Player.getWorldLocation();
        return currentLocation.distanceTo(salamanderType.getHuntingPoint()) <= 20;
    }

    private int getMaxTrapsForHunterLevel() {
        int hunterLevel = Microbot.getClient().getRealSkillLevel(Skill.HUNTER);
        if (hunterLevel >= 80) return 5;
        if (hunterLevel >= 60) return 4;
        if (hunterLevel >= 40) return 3;
        if (hunterLevel >= 20) return 2;
        return 1;
    }

    private boolean handleExistingTraps(SalamanderPlugin plugin, SalamanderConfig config) {
        // Filter for FULL traps and sort by time (traps about to collapse first) and then pick the first one
        var trapToHandle = plugin.getTraps().entrySet().stream()
                .filter(entry -> entry.getValue().getState() == HunterTrap.State.FULL)
                .sorted((a, b) -> Double.compare(b.getValue().getTrapTimeRelative(), a.getValue().getTrapTimeRelative())).collect(Collectors.toList()).stream().findFirst().orElse(null);
        if (trapToHandle == null) return false;
        WorldPoint location = trapToHandle.getKey();
        if (!Rs2Player.isAnimating() && !Rs2Player.isMoving()) {
            Microbot.log("Checking full trap");
            Rs2Walker.walkTo(location);
            var gameObject = Rs2GameObject.getGameObject(location);
            if (gameObject != null) {
                Rs2GameObject.interact(gameObject, "Reset");
                sleep(config.minSleepAfterCatch(), config.maxSleepAfterCatch());
                return true;
            }
        }
        return false;
    }

    private void setNewTrap(SalamanderHunting salamanderType, SalamanderConfig config) {
        Microbot.log("Looking for tree to set trap");
        if (Rs2GameObject.exists(salamanderType.getTreeId())) {
            Microbot.log("Setting trap at " + salamanderType.getName() + " tree");
            Rs2GameObject.interact(salamanderType.getTreeId(), "Set-trap");
            sleep(config.minSleepAfterLay(), config.maxSleepAfterLay());
        }
    }

    public boolean IsRopeOnTheGround() {
        return Rs2GroundItem.exists(ROPE, 7) || Rs2GroundItem.exists(303, 7);
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
}
