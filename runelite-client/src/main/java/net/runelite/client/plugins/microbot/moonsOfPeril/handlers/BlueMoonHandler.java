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
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

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
        sleep(2_000);
        return BossHandler.bossIsAlive(bossName, bossStatusWidgetID);
    }

    @Override
    public State execute() {
        BossHandler.walkToBoss(bossName, bossLobbyLocation);
        sleep(1_000);
        BossHandler.fightPreparation(weaponMain, shield);
        sleep(1_000);
        BossHandler.enterBossArena(bossName, bossStatueObjectID, bossLobbyLocation);
        while (Rs2Player.distanceTo(bossArenaCenter) <= 25) {
            if (isSpecialAttack1Sequence()) {
                specialAttack1Sequence();
            }
            else if (isSpecialAttack2Sequence()) {
                specialAttack2Sequence();
            }
            else if (BossHandler.isNormalAttackSequence(sigilNpcID)) {
                BossHandler.normalAttackSequence(sigilNpcID, bossNpcID, ATTACK_TILES, weaponMain, shield);
            }
            sleep(600); // ~1 in-game ticks
        }
        Rs2Prayer.disableAllPrayers();
        return State.IDLE;
        // PLAYER SHOULD NOW BE INSIDE THE ARENA
        // TODO: add specific combat logic
    }

    /* --------  helper : returns true while an unlit brazier still exists ---- */
    private boolean brazierUnlit() {
        return Rs2GameObject.exists(ObjectID.PMOON_BOSS_BRAZIER_UNLIT);
    }

    /* --------  phase gate --------------------------------------------------- */
    public boolean isSpecialAttack1Sequence() {
        return Rs2Npc.getNpc(NpcID.PMOON_BOSS_WINTER_STORM) != null;
    }

    /* --------  executor ----------------------------------------------------- */
    public void specialAttack1Sequence()
    {
        // WorldPoints use this instance’s region so tiles are always in-scene
        int region = Rs2Player.getWorldLocation().getRegionID();
        WorldPoint BRAZIER_E = WorldPoint.fromRegion(region, 44, 16, 0);   // east
        WorldPoint BRAZIER_W = WorldPoint.fromRegion(region, 20, 16, 0);   // west

        WorldPoint[] lap = { BRAZIER_E, bossArenaCenter, BRAZIER_W, AFTER_TORNADO };

        int step = 0;
        while (brazierUnlit() && step < lap.length)
        {
            WorldPoint target = lap[step];

            /* 1 ─ run to target (10-s timeout) */
            if (Rs2Walker.walkFastCanvas(target, true)) {
                long tEnd = System.currentTimeMillis() + 10_000;
                sleepUntil(() -> Rs2Player.distanceTo(target) <= 1
                        || System.currentTimeMillis() > tEnd, 300);
            }

            /* 2 ─ if at brazier and it’s still unlit → relight */
            if (target != bossArenaCenter &&                          // we’re on a brazier tile
                    Rs2Player.distanceTo(target) <= 1 &&
                    Rs2GameObject.interact(ObjectID.PMOON_BOSS_BRAZIER_UNLIT, "Relight"))
            {
                Microbot.log("Relit brazier at " + target);
                sleep(600);                                 // one tick for animation
            }

            step++;                                         // next leg
            sleep(150);                                     // small breathing gap
        }

        Microbot.log("Frost Storm phase complete");
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

    /**
     * Attacks the frozen icicle
     */
    public void specialAttack2Sequence() {
        /* 1. Run to the */
    }

}
