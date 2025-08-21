package net.runelite.client.plugins.microbot.aiofighter.combat;

import lombok.SneakyThrows;
import net.runelite.api.Actor;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.aiofighter.AIOFighterConfig;
import net.runelite.client.plugins.microbot.aiofighter.AIOFighterPlugin;
import net.runelite.client.plugins.microbot.aiofighter.enums.AttackStyle;
import net.runelite.client.plugins.microbot.aiofighter.enums.AttackStyleMapper;
import net.runelite.client.plugins.microbot.aiofighter.enums.State;
import net.runelite.client.plugins.microbot.shortestpath.ShortestPathPlugin;
import net.runelite.client.plugins.microbot.util.ActorModel;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.antiban.enums.ActivityIntensity;
import net.runelite.client.plugins.microbot.util.camera.Rs2Camera;
import net.runelite.client.plugins.microbot.util.coords.Rs2WorldArea;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcManager;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.plugins.microbot.util.slayer.Rs2Slayer;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import org.slf4j.event.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static net.runelite.api.gameval.VarbitID.*;

public class AttackNpcScript extends Script {

    public static Actor currentNpc = null;
    public static AtomicReference<List<Rs2NpcModel>> filteredAttackableNpcs = new AtomicReference<>(new ArrayList<>());
    public static Rs2WorldArea attackableArea = null;
    private boolean messageShown = false;
    private int noNpcCount = 0;

    public static void skipNpc() {
        currentNpc = null;
    }

    @SneakyThrows
    public void run(AIOFighterConfig config) {
        try {
            Rs2NpcManager.loadJson();
            Rs2Antiban.resetAntibanSettings();
            Rs2Antiban.antibanSetupTemplates.applyCombatSetup();
            Rs2Antiban.setActivityIntensity(ActivityIntensity.EXTREME);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn() || !super.run() || !config.toggleCombat())
                    return;

                if (config.centerLocation().distanceTo(Rs2Player.getWorldLocation()) < config.attackRadius() &&
                        !config.centerLocation().equals(new WorldPoint(0, 0, 0)) && AIOFighterPlugin.getState() != State.BANKING) {
                    if (ShortestPathPlugin.getPathfinder() != null)
                        Rs2Walker.setTarget(null);
                    AIOFighterPlugin.setState(State.IDLE);
                }

                attackableArea = new Rs2WorldArea(config.centerLocation().toWorldArea());
                attackableArea = attackableArea.offset(config.attackRadius());
                List<String> npcsToAttack = Arrays.stream(config.attackableNpcs().split(","))
                        .map(x -> x.trim().toLowerCase())
                        .collect(Collectors.toList());
                filteredAttackableNpcs.set(
                        Rs2Npc.getAttackableNpcs(config.attackReachableNpcs())
                                .filter(npc -> npc.getWorldLocation().distanceTo(config.centerLocation()) <= config.attackRadius())
                                .filter(npc -> npc.getName() != null && !npcsToAttack.isEmpty() && npcsToAttack.stream().anyMatch(npc.getName()::equalsIgnoreCase))
                                .sorted(Comparator.comparingInt((Rs2NpcModel npc) -> npc.getInteracting() == Microbot.getClient().getLocalPlayer() ? 0 : 1)
                                        .thenComparingInt(npc -> Rs2Player.getRs2WorldPoint().distanceToPath(npc.getWorldLocation())))
                                .collect(Collectors.toList())
                );
                final List<Rs2NpcModel> attackableNpcs = new ArrayList<>();

                for (var attackableNpc : filteredAttackableNpcs.get()) {
                    if (attackableNpc == null || attackableNpc.getName() == null) continue;
                    for (var npcToAttack : npcsToAttack) {
                        if (npcToAttack.equalsIgnoreCase(attackableNpc.getName())) {
                            attackableNpcs.add(attackableNpc);
                        }
                    }
                }
                filteredAttackableNpcs.set(attackableNpcs);

                if (config.state().equals(State.BANKING) || config.state().equals(State.WALKING))
                    return;

                // Check if we should pause while looting is happening
                if (Microbot.pauseAllScripts.get()) {
                    return; // Don't attack while looting
                }

                // Check if our current target just died and we should wait for loot
                if (config.toggleWaitForLoot() && !AIOFighterPlugin.isWaitingForLoot()) {
                    // Check if we were recently in combat but no longer interacting (NPC just died)
                    Actor currentInteracting = Rs2Player.getInteracting();

                    // If we're not interacting but were recently, the NPC probably just died
                    if (currentInteracting == null && Rs2Player.isInCombat()) {
                        // We were in combat but lost our target - NPC likely died
                        AIOFighterPlugin.setWaitingForLoot(true);
                        AIOFighterPlugin.setLastNpcKilledTime(System.currentTimeMillis());
                        Microbot.log("Lost target while in combat, waiting for loot...");
                        return;
                    }

                    if (currentInteracting instanceof net.runelite.api.NPC) {
                        net.runelite.api.NPC npc = (net.runelite.api.NPC) currentInteracting;
                        if (npc.isDead() || (npc.getHealthRatio() == 0 && npc.getHealthScale() > 0)) {
                            AIOFighterPlugin.setWaitingForLoot(true);
                            AIOFighterPlugin.setLastNpcKilledTime(System.currentTimeMillis());
                            Microbot.log("NPC died, waiting for loot...");
                            return;
                        }
                    }
                }

                // Check if we're waiting for loot
                if (config.toggleWaitForLoot() && AIOFighterPlugin.isWaitingForLoot()) {
                    long timeSinceKill = System.currentTimeMillis() - AIOFighterPlugin.getLastNpcKilledTime();
                    if (timeSinceKill >= AIOFighterPlugin.LOOT_WAIT_TIMEOUT) {
                        // Timeout reached, resume combat
                        AIOFighterPlugin.setWaitingForLoot(false);
                        AIOFighterPlugin.setLastNpcKilledTime(0);
                        Microbot.log("Loot wait timeout reached, resuming combat");
                    } else {
                        // Still waiting for loot, don't attack
                        int secondsLeft = (int)((AIOFighterPlugin.LOOT_WAIT_TIMEOUT - timeSinceKill) / 1000);
                        Microbot.status = "Waiting for loot... " + secondsLeft + "s";
                        return;
                    }
                }

                if (config.toggleCenterTile() && config.centerLocation().getX() == 0
                        && config.centerLocation().getY() == 0) {
                    if (!messageShown) {
                        Microbot.showMessage("Please set a center location");
                        messageShown = true;
                    }
                    return;
                }
                messageShown = false;


                if (Rs2AntibanSettings.actionCooldownActive) {
                    AIOFighterPlugin.setState(State.COMBAT);
                    handleItemOnNpcToKill(config);
                    return;
                }

                if (!attackableNpcs.isEmpty()) {
                    noNpcCount = 0;

                    Rs2NpcModel npc = attackableNpcs.stream().findFirst().orElse(null);

                    if (!Rs2Camera.isTileOnScreen(npc.getLocalLocation()))
                        Rs2Camera.turnTo(npc);

                    Rs2Npc.interact(npc, "attack");
                    Microbot.status = "Attacking " + npc.getName();
                    Rs2Antiban.actionCooldown();
                    //sleepUntil(Rs2Player::isInteracting, 1000);

                    if (config.togglePrayer()) {
                        if (!config.toggleQuickPray()) {
                            AttackStyle attackStyle = AttackStyleMapper
                                    .mapToAttackStyle(Rs2NpcManager.getAttackStyle(npc.getId()));
                            if (attackStyle != null) {
                                switch (attackStyle) {
                                    case MAGE:
                                        Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MAGIC, true);
                                        break;
                                    case MELEE:
                                        Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MELEE, true);
                                        break;
                                    case RANGED:
                                        Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_RANGE, true);
                                        break;
                                }
                            }
                        } else {
                            Rs2Prayer.toggleQuickPrayer(true);
                        }
                    }


                } else {
                    if (Rs2Player.getWorldLocation().isInArea(attackableArea)) {
                        Microbot.log(Level.INFO, "No attackable NPC found");
                        noNpcCount++;
                        if (noNpcCount > 60 && config.slayerMode()) {
                            Microbot.log(Level.INFO, "No attackable NPC found for 60 ticks, resetting slayer task");
                            AIOFighterPlugin.addBlacklistedSlayerNpcs(Rs2Slayer.slayerTaskMonsterTarget);
                            noNpcCount = 0;
                            SlayerScript.reset();
                        }
                    } else {
                        Rs2Walker.walkTo(config.centerLocation(), 0);
                        AIOFighterPlugin.setState(State.WALKING);
                    }

                }
            } catch (Exception ex) {
                Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
            }
        }, 0, 600, TimeUnit.MILLISECONDS);
    }


    /**
     * item on npcs that need to kill like rockslug
     */
    private void handleItemOnNpcToKill(AIOFighterConfig config) {
        Rs2NpcModel npc = Rs2Npc.getNpcsForPlayer(ActorModel::isDead).findFirst().orElse(null);
        List<String> lizardVariants = new ArrayList<>(Arrays.asList("Lizard", "Desert Lizard", "Small Lizard"));
        if (npc == null) return;
        if (Microbot.getVarbitValue(SLAYER_AUTOKILL_DESERTLIZARDS) == 0 && lizardVariants.contains(npc.getName()) && npc.getHealthRatio() < 5) {
            Rs2Inventory.useItemOnNpc(ItemID.SLAYER_ICY_WATER, npc);
            Rs2Player.waitForAnimation();
        } else if (Microbot.getVarbitValue(SLAYER_AUTOKILL_ROCKSLUGS) == 0 && npc.getName().equalsIgnoreCase("rockslug") && npc.getHealthRatio() < 5) {
            Rs2Inventory.useItemOnNpc(ItemID.SLAYER_BAG_OF_SALT, npc);
            Rs2Player.waitForAnimation();
        } else if (Microbot.getVarbitValue(SLAYER_AUTOKILL_GARGOYLES) == 0 && npc.getName().equalsIgnoreCase("gargoyle") && npc.getHealthRatio() < 3) {
            Rs2Inventory.useItemOnNpc(ItemID.SLAYER_ROCK_HAMMER, npc);
            Rs2Player.waitForAnimation();
        }
    }


    @Override
    public void shutdown() {
        super.shutdown();
    }
}
