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
                    Microbot.log("Clone #" + (parried + 1) + " spawned at " + spawn
                            + " → Parrying via " + spawn);

                    sleep(300);                               // one-tick delay
                    Rs2Walker.walkCanvas(spawn);

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

/*    private WorldPoint calculateParryTile(WorldPoint cloneLoc) {
        WorldPoint center = Rs2Player.getWorldLocation();
        int dx = Integer.signum(cloneLoc.getX() - center.getX());
        int dy = Integer.signum(cloneLoc.getY() - center.getY());
        return new WorldPoint(center.getX() + dx * 3, center.getY() + dy * 3, center.getPlane());
    }*/

}