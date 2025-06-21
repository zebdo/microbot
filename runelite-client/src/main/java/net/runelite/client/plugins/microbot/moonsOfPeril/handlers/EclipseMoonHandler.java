package net.runelite.client.plugins.microbot.moonsOfPeril.handlers;

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

import static net.runelite.client.plugins.microbot.util.Global.sleep;
import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

import java.util.List;
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
            return true;
        }
        return false;
    }

    /**  Eclipse – Special-Attack 1  (“moon-shield lap”)  */
    public void specialAttack1Sequence()
    {
        Rs2Prayer.disableAllPrayers();

        /*if we enter arena mid attack phase, bail out*/
        WorldPoint spawn = Rs2Npc.getNpc(NpcID.PMOON_BOSS_ECLIPSE_MOON_SHIELD).getWorldLocation();
        Microbot.log("Exact Moonshield location = " + spawn);
        if (!spawn.equals(shieldSpawnTile)) {
            automatedWalk(false);
        }

/*      1 ─ wait until shield starts sliding */

        Microbot.log("Waiting for Moonshield to begin movement...");
        sleepUntil(() -> {
            Rs2NpcModel currentShield = Rs2Npc.getNpc(NpcID.PMOON_BOSS_ECLIPSE_MOON_SHIELD);
            return currentShield != null && !currentShield.getWorldLocation().equals(spawn);
        }, 5_000);
        Microbot.log("Moonshield has commenced movement...");

/*         ───── 2. Four anchor tiles around the boss (SW → NW → NE → SE) ───── */
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

/*         3 ─ run to post-phase attack tile */
        WorldPoint fin = Locations.ECLIPSE_ATTACK_6.getWorldPoint();
        Microbot.log("Running to the normal attack sequence tile");
        Rs2Walker.walkFastCanvas(fin, true);
        Microbot.log("Sleeping until the Sigil tile spawns");
        sleepUntil(() -> Rs2Npc.getNpc(sigilNpcID) != null, 4_000);
        Microbot.log("Searing Rays phase finished");

    }

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
            BossHandler.equipWeapons(weaponClones, null);
            return true;
        }

        return false;
    }

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


            // 1. Look at ALL Eclipse-Moon NPCs this tick that have the specific spawn animation. Game mechanics mean this list SHOULD only return 1 NPC
            List<Rs2NpcModel> spawningClones = Rs2Npc
                    .getNpcs(n -> n.getId() == CLONE_NPC_ID          // same ID
                            && n.getAnimation() == CLONE_SPAWN_ANIM)
                    .collect(Collectors.toList());
            Microbot.log("Collected all NPCs that match NPC ID & NPC Animation. Total = " + spawningClones.size());

            // 2. Find the first clone within the list that matches the filter
            if (!spawningClones.isEmpty()) {
                Rs2NpcModel clone = spawningClones.get(0);
                WorldPoint cloneTrueLocation = clone.getWorldLocation();
                Microbot.log("Spawn true location: " + cloneTrueLocation);
                WorldPoint cloneLocalLocation = WorldPoint.fromLocal(Microbot.getClient(), clone.getLocalLocation());
                Microbot.log("Spawn local location: " + cloneLocalLocation);
                sleep(300); // half-tick delay

                // 3. Parry / Attack the clone
                /*WorldPoint parryTile = calculateParryTile(cloneTrueLocation);*/
                WorldPoint parryTile = cloneLocalLocation;
                Microbot.log("Clone #" + (parried + 1) + " spawned at " + cloneTrueLocation + " → Parrying via " + parryTile);
                Rs2Walker.walkCanvas(parryTile);
                parried++;
            }
/*            // 2. Find the first clone on a *new* tile
            for (Rs2NpcModel clone : spawningClones) {
                WorldPoint location = clone.getWorldLocation();
                if (!location.equals(spawn)) {               // new spawn tile
                    spawn = location;
                    lastCloneSeenAt = System.currentTimeMillis();  // reset idle timer

                    WorldPoint parryTile = calculateParryTile(spawn);
                    Microbot.log("Clone #" + (parried + 1) + " spawned at " + spawn
                            + " → Parrying via " + parryTile);

                    sleep(300);                               // half-tick delay
                    Rs2Walker.walkCanvas(parryTile);

                    parried++;
                    break;                                    // handle one spawn per tick
                }
            }*/
            /* small pause before next poll */
            sleep(50); // frequent polling

            if (!isSpecialAttack2Sequence()) {
                Microbot.log("Special attack 2 sequence has ended, breaking out");
                break;
            }
        }
        Microbot.log("Clone phase ended – total clones parried: " + parried);
    }

    /**
     * Return a tile three squares away from the player, in line with the clone.
     *  ─ If clone is within 1 tile of the cardinal axis, treat it as cardinal.
     *  ─ Otherwise, keep both axes (true diagonal).
     */
    private WorldPoint calculateParryTile(WorldPoint cloneLoc)
    {
        WorldPoint center = Rs2Player.getWorldLocation();
        int dx = cloneLoc.getX() - center.getX();
        int dy = cloneLoc.getY() - center.getY();

        // ─── snap minor offsets to 0 ───
        if (Math.abs(dx) <= 1) dx = 0;
        if (Math.abs(dy) <= 1) dy = 0;

        return new WorldPoint(
                center.getX() + Integer.signum(dx) * 3,
                center.getY() + Integer.signum(dy) * 3,
                center.getPlane());
    }

    /**
     * Returns the next corner tile the shield will arrive at, moving clockwise around a square.
     * The square must have an odd side length and be centred on the given centre tile.
     *
     * @param currentShieldLocation Current location of the shield (WorldPoint)
     * @param centreTile            Centre of the square (WorldPoint)
     * @param sideLength            Must be an odd integer ≥ 3
     * @return The next corner tile in clockwise direction, or null if not on expected square edge
     */
    public static WorldPoint nextCornerTile(WorldPoint currentShieldLocation, WorldPoint centreTile, int sideLength) {
        if (sideLength % 2 == 0 || sideLength < 3) {
            throw new IllegalArgumentException("Side length must be an odd integer ≥ 3.");
        }

        int half = (sideLength - 1) / 2;

        int minX = centreTile.getX() - half;
        int maxX = centreTile.getX() + half;
        int minY = centreTile.getY() - half;
        int maxY = centreTile.getY() + half;
        int plane = centreTile.getPlane();

        WorldPoint topLeft     = new WorldPoint(minX, maxY, plane);
        WorldPoint topRight    = new WorldPoint(maxX, maxY, plane);
        WorldPoint bottomRight = new WorldPoint(maxX, minY, plane);
        WorldPoint bottomLeft  = new WorldPoint(minX, minY, plane);

        int x = currentShieldLocation.getX();
        int y = currentShieldLocation.getY();

        if (y == maxY && x < maxX) {
            return topRight;
        } else if (x == maxX && y > minY) {
            return bottomRight;
        } else if (y == minY && x > minX) {
            return bottomLeft;
        } else if (x == minX && y < maxY) {
            return topLeft;
        }

        return null; // not on expected square edge
    }

    /**
     * Returns the “safe tile” one step beyond the given corner, still heading
     * diagonally away from the centre.
     */
    public static WorldPoint safeTileOneStepOut(WorldPoint tile, WorldPoint centre) {
        int stepX = Integer.signum(tile.getX() - centre.getX());  // −1, 0, or +1
        int stepY = Integer.signum(tile.getY() - centre.getY());  // −1, 0, or +1
        return new WorldPoint(
                tile.getX() + stepX,
                tile.getY() + stepY,
                tile.getPlane());
    }

    public void automatedWalk(boolean playerRun) {
        Rs2NpcModel eclipseMoonShield = Rs2Npc.getNpc(NpcID.PMOON_BOSS_ECLIPSE_MOON_SHIELD);
        Microbot.log("TrueTile Moonshield location = " + eclipseMoonShield.getWorldLocation());

        while (isSpecialAttack1Sequence()) {

            /* -------------- QUICK-RECOVER if player is >2 tiles away from shield -------------- */
            WorldPoint currentTile   = Rs2Npc.getNpc(NpcID.PMOON_BOSS_ECLIPSE_MOON_SHIELD).getWorldLocation();
            WorldPoint playerTile    = Rs2Player.getWorldLocation();
            if (playerTile.distanceTo(currentTile) > 2) {
                WorldPoint recovery = safeTileOneStepOut(currentTile, bossArenaCenter);
                Microbot.log("Out of position (>2). Sprinting to " + recovery);
                Rs2Walker.walkFastCanvas(recovery, true);   // force run
                playerRun = false;                          // revert to walking afterwards
            }
            /* --------------------------------------------------------------------- */

            Microbot.log("Sleeping until the moon shield moves from: " + currentTile);
            sleepUntil(
                    () -> Rs2Npc.getNpc(NpcID.PMOON_BOSS_ECLIPSE_MOON_SHIELD)
                            .getWorldLocation()
                            .equals(currentTile)
                            || !isSpecialAttack1Sequence(),
                    5_000);
            if (!isSpecialAttack1Sequence()) {
                Microbot.log("We've detected the special attack sequence is over: breaking out now");
                break;
            }

            WorldPoint nextCornerTileShield = nextCornerTile(currentTile, bossArenaCenter, 9);
            Microbot.log("The next corner tile the shield will visit is: " + nextCornerTileShield);

            if (nextCornerTileShield != null) {
                WorldPoint safeCornerTile = safeTileOneStepOut(nextCornerTileShield, bossArenaCenter);
                Microbot.log("Moving to: " + safeCornerTile + ". Run switched on: " + playerRun);
                Rs2Walker.walkFastCanvas(safeCornerTile, playerRun);
                playerRun = false; // from now on, we walk
                sleepUntil(() -> Rs2Npc.getNpc(NpcID.PMOON_BOSS_ECLIPSE_MOON_SHIELD).getWorldLocation().distanceTo(nextCornerTileShield) <= 1,10_000);
            }

            /* -------------- Run to next attack cycle tile if the moonshield reaches its final tile  -------------- */
            if (Rs2Npc.getNpc(NpcID.PMOON_BOSS_ECLIPSE_MOON_SHIELD).getWorldLocation().equals(shieldSpawnTile)) {
                WorldPoint nextAttackTile = Locations.ECLIPSE_ATTACK_6.getWorldPoint();
                Microbot.log("Running to the normal attack sequence tile");
                Rs2Walker.walkFastCanvas(nextAttackTile, true);
                Microbot.log("Sleeping until the Sigil tile spawns");
                sleepUntil(() -> Rs2Npc.getNpc(sigilNpcID) != null, 4_000);
                Microbot.log("Searing Rays phase finished");
            }
        }
    }


}