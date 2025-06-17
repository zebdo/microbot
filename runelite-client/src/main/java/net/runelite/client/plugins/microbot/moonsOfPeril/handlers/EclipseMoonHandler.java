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
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import static net.runelite.client.plugins.microbot.util.Global.sleep;
import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

public class EclipseMoonHandler implements BaseHandler {

    private static final String bossName = "Eclipse Moon";
    private static final int bossStatusWidgetID = Widgets.ECLIPSE_MOON_ID.getID();
    private static final int bossStatueObjectID = GameObjects.ECLIPSE_MOON_STATUE_ID.getID();
    private static final WorldPoint bossLobbyLocation = Locations.ECLIPSE_LOBBY.getWorldPoint();
    private static final WorldPoint bossArenaCenter = Locations.ECLIPSE_ARENA_CENTER.getWorldPoint();
    private static final WorldPoint cloneAttackTile = Locations.ECLIPSE_CLONE_TILE.getWorldPoint();
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
        return (BossHandler.bossIsAlive(bossName, bossStatusWidgetID) || Rs2Npc.getNpc(bossNpcID) != null);
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
    }

    /**
     * Returns True if the eclipseMoonShield NPC is found.
     */
    public boolean isSpecialAttack1Sequence() {
        Rs2NpcModel eclipseMoonShield = Rs2Npc.getNpc(NpcID.PMOON_BOSS_ECLIPSE_MOON_SHIELD);
        if (eclipseMoonShield != null) {
            Microbot.log("The Eclipse Moon Shield has spawned – We've entered Special Attack 1 Sequence");
            return true;
        }
        return false;
    }


    /**
     * 1. Wait for knockback animation to finish
     * 2. Walks to tile X1
     * 3. Walks to tile X2
     * 4. Walks to tile X3
     * 5. Walks to tile X4
     */
    public void specialAttack1Sequence() {

        /* 1. Wait for knockback animation to finish*/
        var player = Rs2Player.getLocalPlayer();
        Rs2Prayer.disableAllPrayers();
        sleepUntil(() -> (player.getAnimation() != AnimationID.HUMAN_TROLL_FLYBACK_MERGE));
        Microbot.log("The Eclipse Moon Shield has spawned – We've entered Special Attack 1 Sequence");

        /* ───── 2. Four anchor tiles around the boss (SW → NW → NE → SE) ───── */
        WorldPoint[] lap = {
            WorldPoint.fromRegion(6038, 11, 27, 0),
            WorldPoint.fromRegion(6038, 11, 37, 0),
            WorldPoint.fromRegion(6038, 21, 37, 0),
            WorldPoint.fromRegion(6038, 21, 27, 0)
        };

        for (WorldPoint p : lap) {
            Microbot.log("Walking to: " + p);
            Rs2Walker.walkFastCanvas(p, false);
            BossHandler.eatIfNeeded(75);
            BossHandler.drinkIfNeeded(75);
        }
    }

    /**
     * Returns True if the player knockback animation && the location tile is found.
     */
    public boolean isSpecialAttack2Sequence() {
        var player = Rs2Player.getLocalPlayer();
        if (player.getAnimation() == AnimationID.HUMAN_TROLL_FLYBACK_MERGE && Rs2Player.getWorldLocation().equals(cloneAttackTile)) {
            Microbot.log("The throwback animation has occurred – We've entered Special Attack 2 Sequence");
            return true;
        }
        return false;
    }

    /**
     * Eclipse – Special Attack 2 (“clone burst”)
     * • Equip the clone weapon
     * • Parry/attack up to 15 clones (five waves of three)
     */
    public void specialAttack2Sequence()
    {
        /* ---------- 1. Weapon swap (3 s timeout) ------------------------ */
        long swapDeadline = System.currentTimeMillis() + 3_000;
        while (!Rs2Equipment.isWearing(weaponClones) && System.currentTimeMillis() < swapDeadline) {
            BossHandler.equipWeapons(weaponClones, null);
            sleep(300);
        }
        if (!Rs2Equipment.isWearing(weaponClones)) {
            Microbot.log("WARNING: couldn’t equip clone weapon in time");
        }

        /* ---------- 2. Parry 15 clones  -------------------------------- */
        final int MAX_CLONES = 15;
        int cloneNumber = 0;
        boolean waitingForNextSpawn = true;

        long phaseTimeout = System.currentTimeMillis() + 5_000;   // refresh on each spawn

        while (cloneNumber < MAX_CLONES &&
                System.currentTimeMillis() < phaseTimeout &&
                Rs2Player.getWorldLocation().equals(cloneAttackTile))
        {
            Rs2NpcModel boss = Rs2Npc.getNpc(bossNpcID);  // may be null between spawns
            int anim = boss != null ? boss.getAnimation() : -1;

            if (waitingForNextSpawn && anim == AnimationID.NPC_DJINN_02_SPAWN) {
                cloneNumber++;
                phaseTimeout = System.currentTimeMillis() + 12_000; // reset timer
                Microbot.log("Attacking clone " + cloneNumber);
                Rs2Npc.attack(bossNpcID);                 // click the clone to face it
                waitingForNextSpawn = false;              // debounce until anim leaves
            }
            else if (anim != AnimationID.NPC_DJINN_02_SPAWN) {
                waitingForNextSpawn = true;               // ready for next clone
            }

            sleep(300);                                   // half-tick poll
        }

        Microbot.log("Clone phase finished after " + cloneNumber + " parries");
    }

}