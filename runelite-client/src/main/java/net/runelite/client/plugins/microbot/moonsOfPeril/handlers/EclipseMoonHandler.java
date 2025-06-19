package net.runelite.client.plugins.microbot.moonsOfPeril.handlers;

import net.runelite.api.TileObject;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.moonsOfPeril.enums.GameObjects;
import net.runelite.client.plugins.microbot.moonsOfPeril.enums.Locations;
import net.runelite.client.plugins.microbot.moonsOfPeril.enums.State;
import net.runelite.client.plugins.microbot.moonsOfPeril.enums.Widgets;
import net.runelite.client.plugins.microbot.moonsOfPeril.moonsOfPerilConfig;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;

import static net.runelite.client.plugins.microbot.util.Global.sleep;
import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class EclipseMoonHandler implements BaseHandler {

    private static final String bossName = "Eclipse Moon";
    private static final int bossStatusWidgetID = Widgets.ECLIPSE_MOON_ID.getID();
    private static final int bossStatueObjectID = GameObjects.ECLIPSE_MOON_STATUE_ID.getID();
    private static final WorldPoint bossLobbyLocation = Locations.ECLIPSE_LOBBY.getWorldPoint();
    private static final WorldPoint bossArenaCenter = Locations.ECLIPSE_ARENA_CENTER.getWorldPoint();
    private static final WorldPoint exitTile = Locations.ECLIPSE_EXIT_TILE.getWorldPoint();
    private static final WorldPoint shieldSpawnTile = Locations.ECLIPSE_SHIELD_SPAWN_TILE.getWorldPoint();
    private static final WorldPoint cloneAttackTile = bossArenaCenter;
    private static final WorldPoint[] ATTACK_TILES = Locations.eclipseAttackTiles();
    private final int sigilNpcID = GameObjects.SIGIL_NPC_ID.getID();
    private final int bossNpcID = NpcID.PMOON_BOSS_ECLIPSE_MOON_VIS;
    private final String weaponMain;
    private final String shield;
    private final String weaponClones;

    public EclipseMoonHandler(moonsOfPerilConfig cfg) {
        this.weaponMain = cfg.eclipseWeaponMain();
        this.shield = cfg.eclipseShield();
        this.weaponClones = cfg.eclipseWeaponClones();
    }

    @Override
    public boolean validate() {
        // run while boss is alive
        return (BossHandler.bossIsAlive(bossName, bossStatusWidgetID));
    }

    @Override
    public State execute() {
        BossHandler.walkToBoss(bossName, bossLobbyLocation);
        BossHandler.fightPreparation(weaponMain, shield);
        BossHandler.enterBossArena(bossName, bossStatueObjectID, bossLobbyLocation);
        sleepUntil(() -> Rs2Widget.isWidgetVisible(19857413),5_000);
        while (Rs2Widget.isWidgetVisible(19857413) || Rs2Npc.getNpc(bossNpcID) != null) {
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
        Microbot.log("The boss health bar widget is no longer visible, the fight must have ended.");
        Rs2Prayer.disableAllPrayers();
        sleep(1200);
        return State.IDLE;
    }

    /**
     * Returns True if the eclipseMoonShield NPC is found.
     */
    public boolean isSpecialAttack1Sequence() {
        Rs2NpcModel eclipseMoonShield = Rs2Npc.getNpc(NpcID.PMOON_BOSS_ECLIPSE_MOON_SHIELD);
        if (eclipseMoonShield != null && Rs2Npc.getNpc(sigilNpcID) == null) {
            Microbot.log("The Eclipse Moon Shield has spawned – We've entered Special Attack 1 Sequence");
            return true;
        }
        return false;
    }

    /**  Eclipse – Special-Attack 1  (“moon-shield lap”)  */
    public void specialAttack1Sequence()
    {
        Rs2Prayer.disableAllPrayers();
        Rs2NpcModel eclipseMoonShield = Rs2Npc.getNpc(NpcID.PMOON_BOSS_ECLIPSE_MOON_SHIELD);

        /*if we enter arena mid attack phase, bail out*/
        WorldPoint spawn = eclipseMoonShield.getWorldLocation();
        Microbot.log("Exact Moonshield location = " + spawn);
        if (!spawn.equals(shieldSpawnTile)) {
            Microbot.log("Moonshield spawn tile: " + shieldSpawnTile);
            Microbot.log("Moonshield current tile: " + spawn);
            Microbot.log("We have spawned in mid special attack sequence. Need to exit boss arena.");
            BossHandler.bossBailOut(exitTile);
            return;
        }

        /* 1 ─ wait until shield starts sliding */

        Microbot.log("Waiting for Moonshield to begin movement...");
        sleepUntil(() -> {
            Rs2NpcModel currentShield = Rs2Npc.getNpc(NpcID.PMOON_BOSS_ECLIPSE_MOON_SHIELD);
            return currentShield != null && !currentShield.getWorldLocation().equals(spawn);
        }, 5_000);
        Microbot.log("Moonshield has commenced movement...");

        /* ───── 2. Four anchor tiles around the boss (SW → NW → NE → SE) ───── */
        WorldPoint[] lap = {
            new WorldPoint(1483, 9627, 0),  // SW
            new WorldPoint(1483, 9637, 0),  // NW
            new WorldPoint(1493, 9637, 0),  // NE
            new WorldPoint(1493, 9627, 0)   // SE
        };

        for (WorldPoint p : lap) {
            Microbot.log("Walking to WorldPoint: " + p);
            Rs2Walker.walkFastCanvas(p, false);
            sleepUntil(() -> Rs2Player.getWorldLocation().equals(p));
        }
        Microbot.log("Shield lap has been completed");

        /* 3 ─ run to post-phase attack tile */
        WorldPoint fin = Locations.ECLIPSE_ATTACK_6.getWorldPoint();
        Microbot.log("Running to the normal attack sequence tile");
        Rs2Walker.walkFastCanvas(fin, true);
        Microbot.log("Sleeping until the Sigil tile spawns");
        sleepUntil(() -> Rs2Npc.getNpc(sigilNpcID) != null, 4_000);
        Microbot.log("Searing Rays phase finished");

    }



/*    public void specialAttack1Sequence() {
        Rs2NpcModel shield = Rs2Npc.getNpc(NpcID.PMOON_BOSS_ECLIPSE_MOON_SHIELD);
        if (shield == null) {
            Microbot.log("Moonshield NPC not found. Aborting Eclipse specialAttack1Sequence.");
            return;
        }

        final WorldPoint CENTER = bossArenaCenter;
        final int REGION = CENTER.getRegionID();
        final int PLANE = CENTER.getPlane();

        Microbot.log("Generating safe course around arena center...");
        Set<WorldPoint> safeCourse = new HashSet<>();
        for (int dx = -5; dx <= 5; dx++) {
            for (int dy = -5; dy <= 5; dy++) {
                safeCourse.add(new WorldPoint(CENTER.getX() + dx, CENTER.getY() + dy, PLANE));
            }
        }

        WorldPoint spawn = shield.getWorldLocation();
        Microbot.log("Waiting for Moonshield to begin movement...");
        sleepUntil(() -> {
            Rs2NpcModel currentShield = Rs2Npc.getNpc(NpcID.PMOON_BOSS_ECLIPSE_MOON_SHIELD);
            return currentShield != null && !currentShield.getWorldLocation().equals(spawn);
        }, 5_000);

        final WorldPoint LAP_END_TILE = WorldPoint.fromRegion(6038, 21, 29, 0);
        Microbot.log("Beginning shield-follow sequence...");

        while (true) {
            shield = Rs2Npc.getNpc(NpcID.PMOON_BOSS_ECLIPSE_MOON_SHIELD);
            if (shield == null) {
                Microbot.log("Moonshield NPC despawned mid-phase.");
                break;
            }

            WorldPoint shieldTile = shield.getWorldLocation();
            WorldPoint playerTile = Rs2Player.getWorldLocation();

            if (shieldTile.distanceTo(LAP_END_TILE) <= 1) {
                Microbot.log("Moonshield has reached end tile: lap complete.");
                break;
            }

            if (playerTile.distanceTo(shieldTile) > 4) {
                WorldPoint target = safeCourse.stream()
                        .min(Comparator.comparingInt(t -> t.distanceTo(shieldTile)))
                        .orElse(CENTER);
                Microbot.log("Player >4 tiles from shield. Moving to nearest safe tile near shield.");
                Rs2Walker.walkFastCanvas(target, true);
            } else if (!safeCourse.contains(playerTile)) {
                WorldPoint fallback = safeCourse.stream()
                        .min(Comparator.comparingInt(t -> t.distanceTo(playerTile)))
                        .orElse(CENTER);
                Microbot.log("Player outside safe course. Redirecting to fallback safe tile.");
                Rs2Walker.walkFastCanvas(fallback, true);
            }

            sleep(600); // 1 game tick
        }

        WorldPoint postPhase = Locations.ECLIPSE_ATTACK_6.getWorldPoint();
        Microbot.log("Phase ended. Running to post-phase attack tile.");
        Rs2Walker.walkFastCanvas(postPhase, true);
        sleepUntil(() -> Rs2Player.distanceTo(postPhase) <= 1, 3_000);

        Microbot.log("Shield lap complete – Searing Rays phase finished");
    }*/



    /**  Eclipse – Special-Attack 1  (“moon-shield lap”)  *//*
    public void specialAttack1Sequence()
    {
        *//* 1 ─ wait until shield starts sliding *//*
        Rs2NpcModel shield = Rs2Npc.getNpc(NpcID.PMOON_BOSS_ECLIPSE_MOON_SHIELD);
        if (shield == null) return;

        WorldPoint spawn = shield.getWorldLocation();
        sleepUntil(() -> !shield.getWorldLocation().equals(spawn), 2_000);

        *//* 2 ─ config: center of arena and anchor tiles *//*
        int region = bossArenaCenter.getRegionID();
        WorldPoint[] ANCHOR = {
                WorldPoint.fromRegion(region, 11, 27, 0),  // SW
                WorldPoint.fromRegion(region, 11, 37, 0),  // NW
                WorldPoint.fromRegion(region, 21, 37, 0),  // NE
                WorldPoint.fromRegion(region, 21, 27, 0)   // SE
        };

        int anchorIdx = 0, corners = 0;

        *//* 3 ─ escort lap *//*
        while (corners < 4 && Rs2Npc.getNpc(NpcID.PMOON_BOSS_ECLIPSE_MOON_SHIELD) != null)
        {
            WorldPoint shieldTile = shield.getWorldLocation();

            *//* Tile on the 5-radius lap, directly “behind” the moving shield *//*
            int dx = Integer.signum(shieldTile.getX() - bossArenaCenter.getX());  // −1, 0, or 1
            int dy = Integer.signum(shieldTile.getY() - bossArenaCenter.getY());

            WorldPoint target = new WorldPoint(
                    bossArenaCenter.getX() + dx * 5,     // ±5 tiles from center in X
                    bossArenaCenter.getY() + dy * 5,     // ±5 tiles from center in Y
                    bossArenaCenter.getPlane());

            *//* Click target if we’re not already on/adjacent *//*
            if (Rs2Player.distanceTo(target) > 0) {
                Rs2Walker.walkFastCanvas(target, true);            // always run
            }

            *//* Corner reached? *//*
            if (shieldTile.distanceTo(ANCHOR[anchorIdx]) <= 1) {
                corners++;
                anchorIdx = (anchorIdx + 1) & 3;
            }

            BossHandler.eatIfNeeded(75);
            BossHandler.drinkIfNeeded(75);
            sleep(150);
        }

        *//* 4 ─ sprint to post-phase attack tile *//*
        WorldPoint fin = Locations.ECLIPSE_ATTACK_6.getWorldPoint();
        Rs2Walker.walkFastCanvas(fin, true);
        sleepUntil(() -> Rs2Player.distanceTo(fin) <= 1, 3_000);

        Microbot.log("Shield lap complete – Searing Rays phase finished");
    }
*/
    /**
     * Returns true while the clone‐burst (Special-2) phase is active.
     * ­– Player must be standing on the center knock-back tile
     * ­– Boss (or its shield) must NOT be on that centre tile
     *   (so we don’t trigger during normal melee phases)
     */
    public boolean isSpecialAttack2Sequence()
    {
        WorldPoint center = cloneAttackTile;               // the forced-push tile
        WorldPoint playerTile = Rs2Player.getWorldLocation();
        Rs2NpcModel bossNPC = Rs2Npc.getNpc(NpcID.PMOON_BOSS_ECLIPSE_MOON_VIS);

        // 1. Captures the conditions required for the start of the special attack sequence.
        if (playerTile.equals(center) && Rs2Player.getAnimation() == AnimationID.HUMAN_TROLL_FLYBACK_MERGE) {
            Microbot.log("Player located on center tile and knock back animation – entering Special Attack 2");
            sleepUntil(() -> Rs2Player.getAnimation() != AnimationID.HUMAN_TROLL_FLYBACK_MERGE);
            Microbot.log("Knockback animation stopped. Clones are about to spawn");
            BossHandler.equipWeapons(weaponClones, null);
            return true;
        }

        // 2. Captures the conditions required if we spawn into the arena midway through the special attack phase.
        if (playerTile.equals(center) && bossNPC != null && Rs2Npc.getNpc(sigilNpcID) == null) {
            Microbot.log("Player located on center tile and no sigil exists");
            Microbot.log("We have entered the arena mid special attack phase");
            BossHandler.equipWeapons(weaponClones, null);
            return true;
        }

        return false;
    }

/*    *//** Eclipse – Special Attack 2 (five waves × 3 clones) *//*
    public void specialAttack2Sequence()
    {
        *//* ---------- helper so we don’t repeat System.currentTimeMillis() ------- *//*
        java.util.function.Supplier < String > ts = () -> "[" + System.currentTimeMillis() + "] ";

        *//* ---------- 1. ensure clone weapon is wielded (≤3 s) -------------- *//*
        long until = System.currentTimeMillis() + 3_000;
        while (!Rs2Equipment.isWearing(weaponClones) && System.currentTimeMillis() < until) {
            BossHandler.equipWeapons(weaponClones, null);
            sleep(600);
        }
        if (!Rs2Equipment.isWearing(weaponClones)) {
            Microbot.log("WARNING: weapon not equipped – continuing anyway");
        }

        sleep(1_800); // This is an important sleep timing to get the sequence correct. It takes 2.32 seconds between main boss despawn & 1st clone spawn

        *//* ---------- 2. parry up to 15 clones ------------------------------ *//*
        final int MAX_CLONES = 15;
        int cloneCount       = 0;
        WorldPoint lastTile  = null;
        long phaseTimeout    = System.currentTimeMillis() + 10_000;   // safety

        while (cloneCount < MAX_CLONES &&
                System.currentTimeMillis() < phaseTimeout &&
                Rs2Player.getWorldLocation().equals(cloneAttackTile))
        {
            Rs2NpcModel boss = Rs2Npc.getNpc(bossNpcID);
            if (boss == null) break;                         // phase ended

            WorldPoint spawnTile = boss.getWorldLocation();

            *//* new spawn? *//*
            if (!spawnTile.equals(lastTile)) {
                lastTile = spawnTile;
                cloneCount++;

                Microbot.log(ts.get() + "Attacking clone " + cloneCount
                        + "  playerHP=" + Rs2Player.getHealthPercentage());
                Rs2Walker.walkFastCanvas(spawnTile, true);
                Microbot.log(ts.get() + "walkFastCanvas() issued for " + spawnTile);

                Microbot.log("[" + System.currentTimeMillis() +
                        "] Attack click sent");

                phaseTimeout = System.currentTimeMillis() + 10_000; // refresh watchdog
            }

            *//* tiny sleep so we don’t busy-spin; clones spawn ≥1 tick apart *//*
            sleep(120);
        }

        Microbot.log("Clone phase finished after " + cloneCount + " parries");
    }*/

    public void specialAttack2Sequence() {
        final int CLONE_SPAWN_ANIM = 11019;
        final int CLONE_NPC_ID = NpcID.PMOON_BOSS_ECLIPSE_MOON_VIS;
        final long PHASE_TIMEOUT_MS = 35_000;
        final long CLONE_IDLE_TIMEOUT_MS = 4_000;

        Microbot.log("Starting Eclipse Special Attack 2 sequence");

        int parried = 0;
        WorldPoint spawn = null;
        long phaseStart = System.currentTimeMillis();
        long lastCloneSeenAt = phaseStart;

        while (System.currentTimeMillis() - phaseStart < PHASE_TIMEOUT_MS &&
                System.currentTimeMillis() - lastCloneSeenAt < CLONE_IDLE_TIMEOUT_MS &&
                Rs2Player.getWorldLocation().equals(cloneAttackTile)) {


            // 1. Look at ALL Eclipse-Moon NPCs this tick
            List<Rs2NpcModel> spawningClones = Rs2Npc
                    .getNpcs(n -> n.getId() == CLONE_NPC_ID          // same ID
                            && n.getAnimation() == CLONE_SPAWN_ANIM)
                    .collect(Collectors.toList());

            // 2. Find the first clone on a *new* tile
            for (Rs2NpcModel clone : spawningClones) {
                WorldPoint location = clone.getWorldLocation();
                if (!location.equals(spawn)) {               // new spawn tile
                    spawn = location;
                    lastCloneSeenAt = System.currentTimeMillis();  // reset idle timer

                    WorldPoint parryTile = calculateParryTile(spawn);
                    Microbot.log("Clone #" + (parried + 1) + " spawned at " + spawn
                            + " → Parrying via " + parryTile);

                    sleep(300);                               // one-tick delay
                    Rs2Walker.walkCanvas(parryTile);

                    parried++;
                    break;                                    // handle one spawn per tick
                }
            }

            /* small pause before next poll */
            sleep(50); // frequent polling

            if (Rs2Npc.getNpc(sigilNpcID) != null) {
                Microbot.log("DEBUG — sigil detected, breaking out");
                break;
            }
        }

        // DEBUG: why did we exit?
        Microbot.log("DEBUG exit — elapsed=" + (System.currentTimeMillis() - phaseStart)
                + " ms  | sinceLastClone=" + (System.currentTimeMillis() - lastCloneSeenAt) + " ms");
        // END DEBUG
        Microbot.log("Clone phase ended – total clones parried: " + parried);
    }

    private WorldPoint calculateParryTile(WorldPoint cloneLoc) {
        WorldPoint center = Rs2Player.getWorldLocation();
        int dx = Integer.signum(cloneLoc.getX() - center.getX());
        int dy = Integer.signum(cloneLoc.getY() - center.getY());
        return new WorldPoint(center.getX() + dx * 3, center.getY() + dy * 3, center.getPlane());
    }

    public static WorldPoint[] eclipseShieldLapTiles() {
        WorldPoint center = bossArenaCenter;
        int region = center.getRegionID();
        int centerX = center.getRegionX();
        int centerY = center.getRegionY();

        int minX = centerX - 5;
        int maxX = centerX + 5;
        int minY = centerY - 5;
        int maxY = centerY + 5;

        List<WorldPoint> tiles = new ArrayList<>();
        // Bottom row (west → east)
        for (int x = minX; x <= maxX; x++) {
            tiles.add(WorldPoint.fromRegion(region, x, minY, 0));
        }
        // Right column (bottom → top)
        for (int y = minY + 1; y <= maxY; y++) {
            tiles.add(WorldPoint.fromRegion(region, maxX, y, 0));
        }
        // Top row (east → west)
        for (int x = maxX - 1; x >= minX; x--) {
            tiles.add(WorldPoint.fromRegion(region, x, maxY, 0));
        }
        // Left column (top → bottom)
        for (int y = maxY - 1; y > minY; y--) {
            tiles.add(WorldPoint.fromRegion(region, minX, y, 0));
        }

        return tiles.toArray(new WorldPoint[0]);
    }


}