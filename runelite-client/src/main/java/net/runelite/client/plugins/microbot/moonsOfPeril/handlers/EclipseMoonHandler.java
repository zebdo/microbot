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
import net.runelite.client.plugins.microbot.util.coords.Rs2LocalPoint;
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
    private static final int bossHealthBarWidgetID = Widgets.BOSS_HEALTH_BAR.getID();
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
    private final boolean enableBoss;

    public EclipseMoonHandler(moonsOfPerilConfig cfg) {
        this.weaponMain = cfg.eclipseWeaponMain();
        this.shield = cfg.eclipseShield();
        this.weaponClones = cfg.eclipseWeaponClones();
        this.enableBoss = cfg.enableEclipse();
    }

    @Override
    public boolean validate() {
        return (enableBoss && BossHandler.bossIsAlive(bossName, bossStatusWidgetID));
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
            sleep(300);
        }
        Microbot.log("The " + bossName + "boss health bar widget is no longer visible, the fight must have ended.");
        Rs2Prayer.disableAllPrayers();
        sleep(2400);
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

    /**  Eclipse – Moon Shield Special-Attack Handler */
    public void specialAttack1Sequence()
    {
        Rs2Prayer.disableAllPrayers();
        WorldPoint spawn = Rs2Npc.getNpc(NpcID.PMOON_BOSS_ECLIPSE_MOON_SHIELD).getWorldLocation();
        Microbot.log("Exact Moonshield location = " + spawn);
        /*if we enter arena mid attack phase, bail out*/
        if (!spawn.equals(shieldSpawnTile)) {
            Microbot.log("Player has spawned into the arena in the middle of the sequence. Need to escape.");
            BossHandler.bossBailOut(exitTile);
            return;
        }

/*      1 ─ wait until shield starts sliding */

        Microbot.log("Sleeping until knockback animation finishes...");
        sleepUntil(() ->
                        Rs2Player.getAnimation() != AnimationID.HUMAN_TROLL_FLYBACK_MERGE &&
                                Rs2Player.getWorldLocation().equals(new WorldPoint(1491, 9627, 0)),
                5_000);

        Microbot.log("Now sleeping 3.5 ticks to perfectly time our walk");
        sleep(2_100);
        Microbot.log("Commencing our walk around the lap");

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
            BossHandler.eatIfNeeded();
            BossHandler.drinkIfNeeded();
            if (!isSpecialAttack1Sequence()) {
                return;
            }
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
     * – Player must be standing on the center knock-back tile
     * – Boss must NOT be on that center tile
     * – Sigil NPC must not be present
     *
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
            BossHandler.eatIfNeeded();
            BossHandler.drinkIfNeeded();
            BossHandler.meleePrayerOn();
            return true;
        }

        // 2. Captures the conditions required if we spawn into the arena midway through the special attack phase.
        if (playerTile.equals(center) && bossNPC != null && Rs2Npc.getNpc(sigilNpcID) == null && !bossNPC.getLocalLocation().equals(Rs2LocalPoint.fromWorldInstance(center))) {
            BossHandler.equipWeapons(weaponClones, null);
            BossHandler.meleePrayerOn();
            return true;
        }

        return false;
    }

    public void specialAttack2Sequence() {
        final int CLONE_SPAWN_ANIM = 11019;
        final int CLONE_NPC_ID = NpcID.PMOON_BOSS_ECLIPSE_MOON_VIS;
        final long PHASE_TIMEOUT_MS = 35_000;

        Microbot.log("Starting Eclipse Special Attack 2 sequence");

        int parried = 0;
        long phaseStart = System.currentTimeMillis();

        while (isSpecialAttack2Sequence() && System.currentTimeMillis() - phaseStart < PHASE_TIMEOUT_MS) {


            // 1. Look at ALL Eclipse-Moon NPCs this tick that have the specific spawn animation. Game mechanics mean this list SHOULD only return 1 NPC
            List<Rs2NpcModel> spawningClones = Rs2Npc
                    .getNpcs(n -> n.getId() == CLONE_NPC_ID
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

                // 3. Parry / Attack the clone
                WorldPoint parryTile = cloneLocalLocation;
                Microbot.log("Clone #" + (parried + 1) + " spawned at " + cloneTrueLocation + " → Parrying via " + parryTile);
                Rs2Walker.walkCanvas(parryTile);
                parried++;
            }
            if (!isSpecialAttack2Sequence()) {
                Microbot.log("Special attack 2 sequence has ended, breaking out");
                break;
            }
            sleep(600);
        }
        Microbot.log("Clone phase ended – total clones parried: " + parried);
    }
}