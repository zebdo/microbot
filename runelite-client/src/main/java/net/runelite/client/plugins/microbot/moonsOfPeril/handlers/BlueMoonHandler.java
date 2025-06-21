package net.runelite.client.plugins.microbot.moonsOfPeril.handlers;

import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.moonsOfPeril.enums.GameObjects;
import net.runelite.client.plugins.microbot.moonsOfPeril.enums.Locations;
import net.runelite.client.plugins.microbot.moonsOfPeril.enums.State;
import net.runelite.client.plugins.microbot.moonsOfPeril.enums.Widgets;
import net.runelite.client.plugins.microbot.moonsOfPeril.moonsOfPerilConfig;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.tile.Rs2Tile;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static net.runelite.client.plugins.microbot.util.Global.sleep;
import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

public class BlueMoonHandler implements BaseHandler {

    private static final String bossName = "Blue Moon";
    private static final int bossStatusWidgetID = Widgets.BLUE_MOON_ID.getID();
    private static final int bossStatueObjectID = GameObjects.BLUE_MOON_STATUE_ID.getID();
    private static final WorldPoint bossLobbyLocation = Locations.BLUE_LOBBY.getWorldPoint();
    private static final WorldPoint bossArenaCenter = Locations.BLUE_ARENA_CENTER.getWorldPoint();
    private static final WorldPoint[] ATTACK_TILES = Locations.blueAttackTiles();
    private static final WorldPoint AFTER_TORNADO = Locations.BLUE_ATTACK_1.getWorldPoint();
    private final int sigilNpcID = GameObjects.SIGIL_NPC_ID.getID();
    private final int bossNpcID = NpcID.PMOON_BOSS_BLUE_MOON_VIS;
    private String weaponMain;
    private String shield;

    public BlueMoonHandler(moonsOfPerilConfig cfg) {
        this.weaponMain = cfg.blueWeaponMain();
        this.shield = cfg.blueShield();
    }


    @Override
    public boolean validate() {
        // run while boss is alive
        sleep(1_000);
        return BossHandler.bossIsAlive(bossName, bossStatusWidgetID);
    }

    @Override
    public State execute() {
        BossHandler.walkToBoss(bossName, bossLobbyLocation);
        BossHandler.fightPreparation(weaponMain, shield);
        BossHandler.enterBossArena(bossName, bossStatueObjectID, bossLobbyLocation);
        while (Rs2Player.distanceTo(bossArenaCenter) <= 25) {
            // PLAYER SHOULD NOW BE INSIDE THE ARENA
            // TODO: check boss widget health bar for ID
            // TODO: check for Brazier interaction string, currently "relight"


            if (isSpecialAttack1Sequence()) {
                specialAttack1Sequence();
            }
            else if (isSpecialAttack2Sequence()) {
/*                sleepUntil(() -> !isSpecialAttack2Sequence());*/
                specialAttack2Sequence();
            }
            else if (BossHandler.isNormalAttackSequence(sigilNpcID)) {
                BossHandler.normalAttackSequence(sigilNpcID, bossNpcID, ATTACK_TILES, weaponMain, shield);
            }
            sleep(300); // half an in-game tick
        }
        Rs2Prayer.disableAllPrayers();
        return State.IDLE;
    }

    /* --------  phase gate --------------------------------------------------- */
    public boolean isSpecialAttack1Sequence() {
        return (Rs2Npc.getNpc(NpcID.PMOON_BOSS_WINTER_STORM) != null && Rs2Npc.getNpc(sigilNpcID) == null);
    }

    /* --------  executor ----------------------------------------------------- */
    public void specialAttack1Sequence()
    {
        Rs2Prayer.disableAllPrayers();
        Microbot.log("Running to safe tile and waiting out the sequence");
        Rs2Walker.walkFastCanvas(AFTER_TORNADO, true);
        sleepUntil(() -> !isSpecialAttack1Sequence());
    }


    /**
     * Returns True if the icicle NPC is found.
     */
    public boolean isSpecialAttack2Sequence() {
        Rs2NpcModel icicle = Rs2Npc.getNpc(NpcID.PMOON_BOSS_ICICLE_UNCRACKED);
        if (icicle != null) {
            Microbot.log("An icicle has spawned – We've entered Special Attack 2 Sequence");
            return true;
        }
        return false;
    }

    /**  Blue Moon – Special Attack 2  (“weapon-freeze / icicle smash”)  */
    public void specialAttack2Sequence()
    {
        /* ---------- constants --------------------------------------------- */
        final int  ICICLE_NPC_ID    = NpcID.PMOON_BOSS_ICICLE_UNCRACKED;            // 13025
        final int  ICICLE_ANIM_ID   = AnimationID.VFX_DJINN_BLUE_ICE_BLOCK_IDLE_02; // 11031
        final long POLL_TIMEOUT_MS  = 5_000;                                        // ≤   5 s
        final long PHASE_TIMEOUT_MS = 25_000;                                       // ≈ 40 t
        final WorldPoint SAFE_SPOT  = new WorldPoint(1440, 9681, 0);

        java.util.function.Supplier<String> ts =
                () -> "[" + System.currentTimeMillis() + "] ";

        Microbot.log(ts.get() + "specialAttack2Sequence() START");

        /* ---------- 1. Identify the animated icicle ----------------------- */
        List<Rs2NpcModel> matches = Collections.emptyList();
        long pollStart = System.currentTimeMillis();
        Microbot.log(ts.get() + "Polling for animated icicle… (timeout " + POLL_TIMEOUT_MS + " ms)");

        while (matches.isEmpty() &&
                System.currentTimeMillis() - pollStart < POLL_TIMEOUT_MS)
        {
            matches = Rs2Npc.getNpcs(n ->
                            n.getId() == ICICLE_NPC_ID &&
                                    n.getAnimation() == ICICLE_ANIM_ID)
                    .collect(Collectors.toList());

            Microbot.log(ts.get() + "Polling tick --> matches=" + matches.size());
            if (matches.isEmpty()) sleep(600);
        }

        if (matches.isEmpty()) {
            Microbot.log(ts.get() + "Timed-out waiting for animated icicle — aborting");
            return;
        }

        Rs2NpcModel icicle = matches.get(0);
        Microbot.log(ts.get() + "Icicle found at " + icicle.getWorldLocation());
        Microbot.log(ts.get() + "Walking to icicle…");
        Rs2Walker.walkFastCanvas(icicle.getWorldLocation(), true);

        /* ---------- 2. Attack + dodge loop ------------------------------- */

        long phaseStart = System.currentTimeMillis();

        Microbot.log(ts.get() + "Entering attack-and-dodge loop (timeout " + PHASE_TIMEOUT_MS + " ms)");

        while (!Rs2Equipment.isWearing(weaponMain) &&
                System.currentTimeMillis() - phaseStart < PHASE_TIMEOUT_MS)
        {
            Rs2Npc.attack(icicle);
            WorldPoint attackTile = Rs2Player.getWorldLocation();
            Microbot.log(ts.get() + "Attack location calculated as: " + attackTile);
            sleepUntil(() -> BossHandler.inDanger(attackTile),3_000);
            if (BossHandler.inDanger(attackTile)) {
                Microbot.log(ts.get() + "Standing on dangerous tile: " + attackTile);
                WorldPoint safeTile = Rs2Tile.getSafeTiles(1).get(0);
                Microbot.log(ts.get() + "Safe tile calculated to be: " + safeTile);

                if (safeTile != null) {
                    Rs2Walker.walkFastCanvas(safeTile, true);
                    Microbot.log(ts.get() + "Now standing on safe tile: " + safeTile);
                    sleepUntil(() -> !BossHandler.inDanger(attackTile));
                    Microbot.log(ts.get() + "Attack tile now calculated as safe: " + attackTile);
                }
            }
        }

        /* ---------- 3. Retreat to safespot ------------------------------- */
        Microbot.log(ts.get() + "Retreating to SAFE_SPOT " + SAFE_SPOT);
        Rs2Walker.walkFastCanvas(SAFE_SPOT, true);

        Microbot.log(ts.get() + "Waiting for all icicles to despawn…");
        sleepUntil(() -> Rs2Npc.getNpc(ICICLE_NPC_ID) == null);

        Microbot.log(ts.get() + "specialAttack2Sequence() COMPLETE");
    }

    /**
     * Path-finds to {@code target} with run ON and checks every 600 ms
     * that the run-orb is still enabled (e.g. a tornado hit can turn it off).
     * Returns {@code true} once the player is ≤ 1 tile from the target,
     * or {@code false} if the path fails or times out (15 s safety net).
     */
    public static boolean forceRun(WorldPoint target)
    {
        // 0 ─ kick off path-finding in run mode
        Rs2Player.toggleRunEnergy(true);
        boolean started = Rs2Walker.walkFastCanvas(target, true);
        Microbot.log("[forceWalk] started=" + started + "  target=" + target);

        if (!started) return false;

        long tEnd = System.currentTimeMillis() + 15_000;           // safety timeout
        while (System.currentTimeMillis() < tEnd &&
                Rs2Player.distanceTo(target) > 1)
        {
            /* every 300 ms: make sure run is ON --------------------------------*/
            sleep(300);

            if (Microbot.getVarbitPlayerValue(173) == 0) {         // run OFF
                boolean toggled = Rs2Player.toggleRunEnergy(true);
                Microbot.log("[forceWalk] run re-enabled: " + toggled);
            }
        }
        boolean arrived = Rs2Player.distanceTo(target) <= 1;
        Microbot.log("[forceWalk] arrived=" + arrived
                + "  distance=" + Rs2Player.distanceTo(target));
        return arrived;
    }

}
