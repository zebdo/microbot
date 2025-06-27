package net.runelite.client.plugins.microbot.moonsOfPeril.handlers;

import net.runelite.api.GameObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.moonsOfPeril.enums.GameObjects;
import net.runelite.client.plugins.microbot.moonsOfPeril.enums.Locations;
import net.runelite.client.plugins.microbot.moonsOfPeril.enums.State;
import net.runelite.client.plugins.microbot.moonsOfPeril.enums.Widgets;
import net.runelite.client.plugins.microbot.moonsOfPeril.moonsOfPerilConfig;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.plugins.microbot.moonsOfPeril.moonsOfPerilPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static net.runelite.client.plugins.microbot.util.Global.sleep;
import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

public class BloodMoonHandler implements BaseHandler {

    private static final String bossName = "Blood Moon";
    private static final int bossHealthBarWidgetID = Widgets.BOSS_HEALTH_BAR.getID();
    private static final int bossStatusWidgetID = Widgets.BLOOD_MOON_ID.getID();
    private static final int bossStatueObjectID = GameObjects.BLOOD_MOON_STATUE_ID.getID();
    private static final WorldPoint bossLobbyLocation = Locations.BLOOD_LOBBY.getWorldPoint();
    private static final WorldPoint bossArenaCenter = Locations.BLOOD_ARENA_CENTER.getWorldPoint();
    private static final WorldPoint[] ATTACK_TILES = Locations.bloodAttackTiles();
    private final int bossNpcID = NpcID.PMOON_BOSS_BLOOD_MOON_VIS;
    private final int sigilNpcID = GameObjects.SIGIL_NPC_ID.getID();
    private String weaponMain;
    private String shield;
    private static final WorldPoint afterRainTile = Locations.BLOOD_ATTACK_6.getWorldPoint();

    // Fields used for Blood Jaguar Sequence and event subcribe //
    private int poolTick  = -1;
    private boolean waitingForEvade = false;
    public boolean bloodJaguarActive = false;
    private int  evadeCount = 0;
    private Rs2NpcModel targetJaguar;
    public WorldPoint evadeTile;
    private WorldPoint attackTile;
    private WorldPoint spawnTile;
    public boolean arrived = false;

    public BloodMoonHandler(moonsOfPerilConfig cfg) {
        this.weaponMain = cfg.bloodWeaponMain();
        this.shield = cfg.bloodShield();
    }

    @Override
    public boolean validate() {
        // run while boss is alive
        return BossHandler.bossIsAlive(bossName, bossStatusWidgetID);
    }

    @Override
    public State execute() {
        if (!Rs2Widget.isWidgetVisible(bossHealthBarWidgetID)) {
            BossHandler.walkToBoss(bossName, bossLobbyLocation);
            BossHandler.fightPreparation(weaponMain, shield);
            BossHandler.enterBossArena(bossName, bossStatueObjectID, bossLobbyLocation);
            sleepUntil(() -> Rs2Widget.isWidgetVisible(bossHealthBarWidgetID), 5_000);
        }

        while (Rs2Widget.isWidgetVisible(bossHealthBarWidgetID) || Rs2Npc.getNpc(bossNpcID) != null) {
            if (isSpecialAttack1Sequence()) {
                specialAttack1Sequence();
            }
            else if (isSpecialAttack2Sequence()) {
                specialAttack2Sequence();
            }
            else if (BossHandler.isNormalAttackSequence(sigilNpcID)) {
                BossHandler.normalAttackSequence(sigilNpcID, bossNpcID, ATTACK_TILES, weaponMain, shield);
            }
            sleep(300); // half an in-game tick
        }
        Microbot.log("The " + bossName + "boss health bar widget is no longer visible, the fight must have ended.");
        Rs2Prayer.disableAllPrayers();
        sleep(2400);
        return State.IDLE;
    }


    /**
     * Returns True if the jaguar NPC is found.
     */
    public boolean isSpecialAttack1Sequence() {
        Rs2NpcModel jaguar = Rs2Npc.getNpc(NpcID.PMOON_BOSS_JAGUAR);
        return (jaguar != null && Rs2Widget.isWidgetVisible(bossHealthBarWidgetID));
    }

    /**
     * Returns True if the Moonfire gameobject is found and the boss is not attackable.
     */
    public boolean isSpecialAttack2Sequence() {
        return Rs2GameObject.exists(ObjectID.PMOON_BOSS_BLOOD_FIRE) && Rs2Npc.getNpc(sigilNpcID) == null && Rs2Widget.isWidgetVisible(bossHealthBarWidgetID);
    }


    /**
     * Handles the Blood Rain Sequence.
     */
    public void specialAttack2Sequence() {
        Microbot.log("The moonfire has spawned – We've entered Special Attack 2, the Blood Rain Sequence");
        sleepUntil(() -> !Rs2Player.isAnimating(),5000);
        Rs2Prayer.disableAllPrayers();
        Rs2Walker.walkFastCanvas(afterRainTile,true);
        sleepUntil(() -> Rs2Player.getWorldLocation().equals(afterRainTile));
        while (isSpecialAttack2Sequence()) {
            WorldPoint playerTile = Rs2Player.getWorldLocation();
            GameObject bloodPool = Rs2GameObject.getGameObject(o -> o.getId() == ObjectID.PMOON_BOSS_BLOOD_POOL && o.getWorldLocation().equals(playerTile));
            if (bloodPool != null) {
                Microbot.log("Standing on dangerous tile: " + playerTile);
                WorldPoint safeTile = getRandomSafeTile(ObjectID.PMOON_BOSS_BLOOD_POOL, 1);
                Microbot.log("Safe tile calculated to be: " + safeTile);
                if (safeTile != null) {
                    Rs2Walker.walkFastCanvas(safeTile, true);
                    Microbot.log("Now standing on safe tile: " + safeTile);
                    sleepUntil(() -> Rs2Player.getWorldLocation().equals(safeTile), 600);
                }
            }
            BossHandler.eatIfNeeded(70);
            BossHandler.drinkIfNeeded(70);
            sleep(600); // sleep one game tick
        }
    }

    public void specialAttack1Sequence() {
        Microbot.log("Entering Special Attack Sequence: Blood Jaguar");
        Rs2Prayer.disableAllPrayers(); // prayer not required for this special attack
        final long startMs = System.currentTimeMillis();

        /* 1  find the sigil NPC (2×2, SW tile = sigilLoc) */
        Rs2NpcModel sigilNpc = Rs2Npc.getNpcs(n -> n.getId() == sigilNpcID).findFirst().orElse(null);
        if (sigilNpc == null) {
            Microbot.log("no sigil NPC – bail");
            return;
        }
        WorldPoint sigilLocation = sigilNpc.getWorldLocation();
        /* derive the trio of tiles */
        Locations.Rotation rot = Locations.bloodJaguarRotation(sigilLocation);
        if (rot == null) {
            Microbot.log("Unknown sigil tile: " + sigilLocation);
            return;
        }
        WorldPoint attackTile = rot.attack;
        WorldPoint evadeTile = rot.evade;
        WorldPoint spawnTile = rot.spawn;

        Microbot.log("Resolved rotation. attackTile=" + attackTile + "  evadeTile=" + evadeTile + "  spawnTile=" + spawnTile);

        if (attackTile == null || spawnTile == null || evadeTile == null) {
            Microbot.log("Unknown sigilLocation tile: " + sigilLocation);
            return;
        }

        /* 2 ─ move onto Attack tile ---------------------------------------- */
        Microbot.log("Moving to attackTile " + attackTile);
        Rs2Walker.walkFastCanvas(attackTile, true);
        sleep(600);
        if (!Rs2Player.getWorldLocation().equals(attackTile)) {
            Rs2Walker.walkFastCanvas(attackTile, true);
            sleepUntil(() -> Rs2Player.getWorldLocation().equals(attackTile));
        }
        boolean arrived = sleepUntil(() -> Rs2Player.getWorldLocation().equals(attackTile), 5_000);
        Microbot.log(arrived
                ? "Arrived on attackTile"
                : "Failed to reach attackTile – aborting");
        if (!arrived) return;

        /* 3 ─ lock the target jaguar (SW tile == spawnTile) ---------------- */
        Rs2NpcModel targetJaguar = Rs2Npc.getNpcs(n ->
                        n.getId() == NpcID.PMOON_BOSS_JAGUAR &&
                                n.getWorldLocation().equals(spawnTile))
                .findFirst().orElse(null);
        if (targetJaguar == null) {
            Microbot.log("jaguar not on expected spawn");
            return;
        }

        /* 4 ─ blood-pool-tick watcher (3 ticks idle) ----------------------------- */
        final long TIMEOUT_MS = 30_000;
        final int POOL_ID = ObjectID.PMOON_BOSS_BLOOD_POOL;
        int evadeCount = 0;
        boolean evadedThisCycle = false;

        while (isSpecialAttack1Sequence() && evadeCount <= 5 && System.currentTimeMillis() - startMs < TIMEOUT_MS) {
            /*  detect first tick when the blood pool exists  */
            poolTick = moonsOfPerilPlugin.bloodPoolTick;
            Microbot.log("Current tick counter: " + poolTick);
/*            if (poolTick == -1) {
                GameObject pool = Rs2GameObject.getGameObject(g -> g.getId() == POOL_ID);
                if (pool != null && pool.getWorldLocation().equals(evadeTile)) {
                    poolTick = 1;                                    // T0
                    Microbot.log("Pool spawned – starting pool-tick counter");
                }
            } else {
                poolTick++; // advance each 600 ms
            }*/

            /* step back exactly on the 3rd tick after pool spawn */
            if (poolTick == 3) {
                Microbot.log("EVADE to " + evadeTile);
                Rs2Walker.walkFastCanvas(evadeTile, true);
/*                evadedThisCycle = true;*/
                evadeCount++;
                Microbot.log("Evade count = " + evadeCount);
            } else if (poolTick == 5) {
                Microbot.log("ATTACK jaguar");
                Rs2Npc.attack(targetJaguar);
            }
            else if (poolTick == 6) {
                Microbot.log("Clicking on ground to stop attacking");
                Rs2Walker.walkFastCanvas(attackTile, true);
            }

            /* reset for next cycle */
/*            if (evadedThisCycle) {
                evadedThisCycle = false;
            }*/
            sleep(100);   // short polling. Let OnGameTick method in MoonsOfPerilPlugin.java handle the ticks
            poolTick++;
        }
    }




/*    *//**
     * Handles the Blood Rain Sequence.
     *//*
     public void moveToJaguar() {
         if (!arrived) {
             Microbot.log("Handler hashCode=" + System.identityHashCode(this));
             Microbot.log("Entering Special Attack Sequence: Blood Jaguar");
             Rs2Prayer.disableAllPrayers(); // prayer not required for this special attack
             *//*1 find the sigil NPC (2×2, SW tile = sigilLoc)*//*
             Rs2NpcModel sigilNpc = Rs2Npc.getNpcs(n -> n.getId() == sigilNpcID).findFirst().orElse(null);
             if (sigilNpc == null) {
                 Microbot.log("no sigil NPC – bail");
                 return;
             }

             WorldPoint sigilLocation = sigilNpc.getWorldLocation();
             *//*derive the trio of tiles*//*
             Locations.Rotation rot = Locations.bloodJaguarRotation(sigilLocation);
             if (rot == null) {
                 Microbot.log("Unknown sigil tile: " + sigilLocation);
                 return;
             }
             attackTile = rot.attack;
             evadeTile = rot.evade;
             spawnTile = rot.spawn;
             targetJaguar = Rs2Npc.getNpcs(n -> n.getId() == NpcID.PMOON_BOSS_JAGUAR && n.getWorldLocation().equals(spawnTile)).findFirst().orElse(null);

             Microbot.log("moveToJaguar Resolved rotation. attackTile=" + attackTile + "  evadeTile=" + evadeTile + "  spawnTile=" + spawnTile);

             if (attackTile == null || spawnTile == null || evadeTile == null) {
                 Microbot.log("Unknown sigilLocation tile: " + sigilLocation);
                 return;
             }

             *//* 2 ─ move onto Attack tile ---------------------------------------- *//*
             Microbot.log("Moving to attackTile " + attackTile);
             Rs2Walker.walkFastCanvas(attackTile, true);
             if (!Rs2Player.getWorldLocation().equals(attackTile)) {
                 Rs2Walker.walkFastCanvas(attackTile, true);
                 sleepUntil(() -> Rs2Player.getWorldLocation().equals(attackTile));
             }
             arrived = sleepUntil(() -> Rs2Player.getWorldLocation().equals(attackTile), 5_000);
             Microbot.log(arrived
                     ? "Arrived on attackTile"
                     : "Failed to reach attackTile – aborting");
         }
     }

    public void handleJaguars(GameObject poolObj)
    {
        Microbot.log("Blood pool spawned: poolTick = 1");
        poolTick = 1;
        Microbot.log("handleJaguars rotation. attackTile=" + attackTile + "  evadeTile=" + evadeTile + "  spawnTile=" + spawnTile);
        Microbot.log("Game object: " + poolObj + ". Location: " + poolObj.getWorldLocation());
        Microbot.log("Evade tile Location: " + evadeTile);
        waitingForEvade = true;
    }

    public void onGameTick()
    {
        Microbot.log("Starting onGameTick: poolTick = " + poolTick);
        if (!bloodJaguarActive || poolTick == -1) return;

        if (poolTick == 3 && waitingForEvade) {
            Rs2Walker.walkFastCanvas(evadeTile, true);
            waitingForEvade = false;
            evadeCount++;
        }
        else if (poolTick == 5) {
            Rs2Npc.attack(targetJaguar);
        }
        else if (poolTick == 6) {
            Rs2Walker.walkFastCanvas(attackTile, true);
            poolTick      = -1;          // wait for next pool
            waitingForEvade = true;      // re-arm
            if (evadeCount >= 5 || !isSpecialAttack1Sequence()) {
                return;
            }
        }
        poolTick++;
    }*/

    /**
     * Returns a random safe WorldPoint within {@code distance} tiles of the player.
     * A tile is unsafe if it contains a GameObject with {@code dangerousId}.
     * Returns {@code null} when no safe tile exists.
     */
    public static WorldPoint getRandomSafeTile(int dangerousId, int distance)
    {
        // ── 1. player location ──
        WorldPoint centre = Rs2Player.getWorldLocation();
        if (centre == null) return null;

        // ── 2. collect all dangerous tiles within radius ──
        Set<WorldPoint> dangerTiles = Rs2GameObject
                .getGameObjects(o -> o.getId() == dangerousId, distance)
                .stream()
                .map(GameObject::getWorldLocation)
                .collect(Collectors.toSet());

        // ── 3. enumerate every tile in the square centred on the player ──
        List<WorldPoint> candidates = new ArrayList<>();
        for (int dx = -distance; dx <= distance; dx++) {
            for (int dy = -distance; dy <= distance; dy++) {
                WorldPoint wp = new WorldPoint(
                        centre.getX() + dx,
                        centre.getY() + dy,
                        centre.getPlane());
                if (!dangerTiles.contains(wp)) {
                    candidates.add(wp);
                }
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        // ── 4. pick one at random ──
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }
}