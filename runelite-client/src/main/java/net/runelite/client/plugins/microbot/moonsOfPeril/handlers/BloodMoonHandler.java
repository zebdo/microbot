package net.runelite.client.plugins.microbot.moonsOfPeril.handlers;

import net.runelite.api.GameObject;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.moonsOfPeril.enums.GameObjects;
import net.runelite.client.plugins.microbot.moonsOfPeril.enums.Locations;
import net.runelite.client.plugins.microbot.moonsOfPeril.enums.State;
import net.runelite.client.plugins.microbot.moonsOfPeril.enums.Widgets;
import net.runelite.client.plugins.microbot.moonsOfPeril.moonsOfPerilConfig;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.tile.Rs2Tile;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.util.Collections;
import java.util.List;
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

    public BloodMoonHandler(moonsOfPerilConfig cfg) {
        this.weaponMain = cfg.bloodWeaponMain();
        this.shield = cfg.bloodShield();
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
        BossHandler.fightPreparation(weaponMain, shield);
        BossHandler.enterBossArena(bossName, bossStatueObjectID, bossLobbyLocation);
        sleepUntil(() -> Rs2Widget.isWidgetVisible(bossHealthBarWidgetID),5_000);
        while (Rs2Widget.isWidgetVisible(bossHealthBarWidgetID) || Rs2Npc.getNpc(bossNpcID) != null) {
            // PLAYER SHOULD NOW BE INSIDE THE ARENA
            // TODO: check boss widget health bar for ID

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
        return State.IDLE;
    }


    /**
     * Returns True if the jaguar NPC is found.
     */
    public boolean isSpecialAttack1Sequence() {
        Rs2NpcModel jaguar = Rs2Npc.getNpc(NpcID.PMOON_BOSS_JAGUAR);
        return jaguar != null;
    }

    /**
     * Returns True if the Moonfire gameobject is found and the boss is not attackable.
     */
    public boolean isSpecialAttack2Sequence() {
        return Rs2GameObject.exists(ObjectID.PMOON_BOSS_BLOOD_FIRE) && Rs2Npc.getNpc(sigilNpcID) == null;
    }


    /**
     * Handles the Blood Rain Sequence.
     */
    public void specialAttack2Sequence() {
        Microbot.log("The moonfire has spawned – We've entered Special Attack 2, the Blood Rain Sequence");
        sleepUntil(() -> !Rs2Player.isAnimating());
        Rs2Prayer.disableAllPrayers();
        Rs2Walker.walkFastCanvas(afterRainTile,true);
        while (isSpecialAttack2Sequence()) {
            WorldPoint playerTile = Rs2Player.getWorldLocation();
            if (BossHandler.inDanger(playerTile)) {
                Microbot.log("Standing on dangerous tile: " + playerTile);
                WorldPoint safeTile = Rs2Tile.getSafeTiles(1).get(0);
                Microbot.log("Safe tile calculated to be: " + safeTile);
                if (safeTile != null) {
                    Rs2Walker.walkFastCanvas(safeTile, true);
                    Microbot.log("Now standing on safe tile: " + safeTile);
                    sleepUntil(() -> !isSpecialAttack2Sequence(), 600);
                }
            }
            sleep(600); // sleep one game tick
        }
    }

    /** Blood-Moon – Special-Attack 1 (“jaguar swipe / floor-sigil”) */
    public void specialAttack1Sequence()
    {
        /* ───── constants & cached tiles ───── */
        final int    SIGIL_ID          = ObjectID.PMOON_BOSS_OVERLAY_FULL;
        final int    JAGUAR_IDLE_ANIM  = -1; // idle
        final long   TIMEOUT_MS        = 30_000;
        final long   startMs           = System.currentTimeMillis();

        Microbot.log("ENTER – starting Blood-Jaguar sequence");

        /* ───── 1. locate lit sigil & target jaguar ───── */
        GameObject sigil = Rs2GameObject.getGameObject(g -> g.getId() == SIGIL_ID);
        if (sigil == null) {
            Microbot.log("No sigil in view – walking to arena centre");
            Rs2Walker.walkFastCanvas(bossArenaCenter, true);
            sleep(600);                                   // one tick
            sigil = Rs2GameObject.getGameObject(g -> g.getId() == SIGIL_ID);
            if (sigil == null) {
                Microbot.log("Sigil still not found – aborting");
                return;
            }
        }
        Microbot.log("Highlighted sigil found @ " + sigil.getWorldLocation());
        WorldPoint sigilLocation = sigil.getWorldLocation();   // must be effectively-final for the lambda
        List<Rs2NpcModel> matches = Rs2Npc.getNpcs(
                n -> n.getId() == NpcID.PMOON_BOSS_JAGUAR &&
                        n.getWorldLocation().equals(sigilLocation)
        ).collect(Collectors.toList());
        Microbot.log("Polling jaguars in the arena --> matches=" + matches.size());
        if (matches.isEmpty()) {
            Microbot.log("Did not find any matches for the target jaguar");
            return;
        }


        Rs2NpcModel targetJaguar = matches.get(0);
        Microbot.log("Target jaguar chosen @ " + targetJaguar.getWorldLocation());

        WorldPoint swTile = targetJaguar.getWorldLocation();

        WorldPoint attackTile = Locations.bloodJaguarAttackTile(swTile);
        WorldPoint evadeTile  = Locations.bloodJaguarEvadeTile(swTile);

        if (attackTile == null) {
            Microbot.log("Unknown jaguar spawn at " + swTile + " – aborting");
            return;
        }

/*         ───── 2. run to attack tile ───── */
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


/*         ───── 3. main 600-ms tick loop ───── */
        int tickCounter = -1;                // unknown until we see first idle
        int prevAnim    = Integer.MIN_VALUE; // impossible value

        while (isSpecialAttack1Sequence() &&
                System.currentTimeMillis() - startMs < TIMEOUT_MS)
        {
            int curAnim = targetJaguar.getAnimation();

/*             ─ track idle ticks ─ */
            if (curAnim == JAGUAR_IDLE_ANIM) {
                tickCounter = (prevAnim == JAGUAR_IDLE_ANIM) ? tickCounter + 1 : 0;
            } else {
                if (prevAnim == JAGUAR_IDLE_ANIM)
                    Microbot.log("Jaguar left idle (anim " + curAnim + ")");
                tickCounter = -1;            // reset
            }
            prevAnim = curAnim;

            Microbot.log(String.format(
                    "· [BJ-S1] tick=%2d  anim=%d", tickCounter, curAnim));

/*             ─ timing gates ─ */
            if (tickCounter == 2) {
                Microbot.log("tick 2 – EVADE to " + evadeTile);
                Rs2Walker.walkFastCanvas(evadeTile, true);
            } else if (tickCounter == 3) {
                Microbot.log("tick 3 – ATTACK jaguar");
                Rs2Npc.attack(targetJaguar);
            }

            sleep(600);   // one guaranteed game-tick
        }
    }

/*    public void specialAttack1Sequence() {

        long start = System.currentTimeMillis();
        long timeout = 30_000;

        Microbot.log("A jaguar has spawned – We've entered Special Attack 1, the Blood Jaguar Sequence");

        Microbot.log("Debug Logging sequence started");

        while (isSpecialAttack1Sequence() && System.currentTimeMillis() - start < timeout) {
            WorldPoint playerLoc = Rs2Player.getWorldLocation();
            if (playerLoc == null) {
                Microbot.log("Player location is null. Skipping this cycle.");
                sleep(3000);
                continue;
            }

            // ── GameObjects within 3 tiles ──
            List<GameObject> gameObjects = Rs2GameObject.getGameObjects(
                    go -> go != null && go.getWorldLocation() != null, playerLoc, 3
            );

            Microbot.log("---- GameObjects within 3 tiles ----");
            for (GameObject obj : gameObjects) {
                Microbot.log(String.format("GameObject -> ID: %d, Loc: %s",
                        obj.getId(),
                        obj.getWorldLocation()
                ));
            }

            // ── TileObjects within 3 tiles ──
            List<TileObject> tileObjects = Rs2GameObject.getTileObjects(
                    go -> go != null && go.getWorldLocation() != null, playerLoc, 3
            );

            Microbot.log("---- TileObjects within 3 tiles ----");
            for (TileObject obj : tileObjects) {
                Microbot.log(String.format("TileObject -> ID: %d, Loc: %s",
                        obj.getId(),
                        obj.getWorldLocation()
                ));
            }

            // ── All NPCs ──
            Microbot.log("---- All NPCs ----");
            Rs2Npc.getNpcs().forEach(npc -> {
                Microbot.log(String.format("NPC -> ID: %d, Name: %s, Loc: %s",
                        npc.getId(),
                        npc.getName(),
                        npc.getWorldLocation()
                ));
            });

            sleep(5000);
        }

        Microbot.log("Stopped logging — timeout reached");
    }*/

}