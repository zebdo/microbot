package net.runelite.client.plugins.microbot.moonsOfPeril.handlers;

import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.moonsOfPeril.enums.GameObjects;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;


import java.util.Objects;
import java.util.Arrays;
import java.util.Collections;

import static net.runelite.client.plugins.microbot.util.Global.sleep;
import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

/** Pure helper methods – no instances. */
public final class BossHandler {

    private BossHandler() {}

    private Rs2PrayerEnum bestMeleePrayer = Objects.requireNonNull(Rs2Prayer.getBestMeleePrayer());

    // TODO:
    //  add checks at script initialisation:
    //  a. check that all config weapons are in inventory or wielded
    //  b. check that Moons of Peril quest has been completed
    //  2. add check before equipping weapons that enough inventory space exists, otherwise don't equip
    //  3. The while loops on the specific boss handlers should check if boss health widget is visible (instead of player distance to tile)

    /** Walks to a boss lobby and logs progress. */
    public static void walkToBoss(String bossName, WorldPoint bossWorldPoint) {
        Microbot.log("Walking to " + bossName + " lobby");
        Rs2Walker.walkWithState(bossWorldPoint, 0);
        sleep(600);
        if (!Rs2Player.getWorldLocation().equals(bossWorldPoint)) {
            Rs2Walker.walkFastCanvas(bossWorldPoint);
            sleepUntil(() -> Rs2Player.getWorldLocation().equals(bossWorldPoint));
        }
        Microbot.log("Arrived at " + bossName + " lobby");
    }


    /** True while the “boss check mark” widget is hidden on-screen. i.e. the boss is alive*/
    public static boolean bossIsAlive(String bossName, int aliveWidgetId) {
        Microbot.log(bossName + " is alive: " + Rs2Widget.isHidden(aliveWidgetId));
        return Rs2Widget.isHidden(aliveWidgetId);
    }

    /** Interacts with the Boss statue and waits for the player to be teleported into Boss Arena*/
    public static void enterBossArena(String bossName, int bossStatueID, WorldPoint bossWorldPoint) {
        // Check if player is standing on the exact tile
        if (!Rs2Player.getWorldLocation().equals(bossWorldPoint)) {
            if (Rs2Walker.walkFastCanvas(bossWorldPoint)) {
                Microbot.log("Walking to statue tile");
            }
            sleepUntil(() -> Rs2Player.getWorldLocation().equals(bossWorldPoint));
        }

        if (Rs2GameObject.interact(bossStatueID, "Use")) {
            Microbot.log("Entering " + bossName + " arena");
            sleepUntil(() -> !Rs2Player.getWorldLocation().equals(bossWorldPoint));
            Microbot.log("Teleport into " + bossName + " arena has occurred");
        }
    }

    /** 1. Equips the player's chosen weapon + offhand
     * 2. Eats food if required
     * 3. Drinks potions if required
     * 4. Turns on Player's best offensive melee prayer*/
    public static void fightPreparation(String weaponMain, String shield) {
        // 1. Equip the player's chosen weapon + offhand
        equipWeapons(weaponMain, shield);
        sleep(600);

        // 2. Eats food if hitpoints below 70%
        eatIfNeeded(70);
        sleep(600);

        // 3. Drinks potions if prayer points below 70%
        drinkIfNeeded(70);
        sleep(600);

    }

    /**
     * Equip the main-hand weapon, and the shield only if one is supplied.
     * Pass null (or "") for shield when you don’t want to equip anything there.
     */
    public static void equipWeapons(String weaponMain, String shield)
    {
        /* --- figure out what we actually need to wear ------------------- */
        boolean hasShield = shield != null && !shield.isEmpty();
        String[] needed   = hasShield
                ? new String[] { weaponMain, shield }
                : new String[] { weaponMain };

        /* --- skip if already wearing everything ------------------------- */
        if (Rs2Equipment.isWearing(
                java.util.Arrays.asList(needed),          // no import needed
                false,
                java.util.Collections.emptyList()))
        {
            return;
        }

        /* --- wield main hand -------------------------------------------- */
        if (Rs2Inventory.wield(weaponMain)) {
            Microbot.log(weaponMain + " is now equipped");
        }

        /* --- wield shield if requested ---------------------------------- */
        if (hasShield && Rs2Inventory.wield(shield)) {
            Microbot.log(shield + " is now equipped");
        }
    }


    /**
     * Eats food if hitpoints below percentage threshold
     *
     * @return
     */
    public static void eatIfNeeded(int percentage) {
        Rs2Player.eatAt(percentage);
    }

    /**
     * Drinks prayer potion if prayer points below percentage threshold
     *
     * @return
     */
    public static void drinkIfNeeded(int percentage) {
        int maxPrayer          = Rs2Player.getRealSkillLevel(Skill.PRAYER);
        int minimumPrayerPoint = (maxPrayer * percentage) / 100;
        Rs2Player.drinkPrayerPotionAt(minimumPrayerPoint);
    }


    /**
     * Turns on Player's best offensive melee prayer and returns true
     *
     * @return
     */
    public static void meleePrayerOn() {
        Rs2Prayer.toggle(Objects.requireNonNull(Rs2Prayer.getBestMeleePrayer()), true);
    }

    /**
     * Returns true if the sigil NPC (the highlighted attack tile) is present
     *
     * @return
     */
    public static boolean isNormalAttackSequence(int sigilNpcID) {
        return Rs2Npc.getNpc(sigilNpcID) != null;
    }

    /**
     * Eclipse “normal” phase: follow ≤ 3 sigil squares, stand on the
     * matching attack tile, and keep attacking the boss.
     *
     * @param sigilNpcID  NPC ID of the 2×2 sigil marker
     * @param bossNpcID   NPC ID of the Eclipse boss
     * @param attackTiles WorldPoints from Locations enum
     */
    public static void normalAttackSequence(int sigilNpcID,
                                            int bossNpcID,
                                            WorldPoint[] attackTiles, String Weapon, String Shield)
    {
        WorldPoint lastSigilSW = null;   // south-west anchor of current sigil
        int sigilMoves = 0;              // how many times the sigil has shifted
        equipWeapons(Weapon, Shield);
        sleep(150);
        meleePrayerOn();
        sleep(150);

        while (sigilMoves < 3)           // a normal phase has ≤3 sigil positions
        {
            /* 1 ─ current sigil NPC; exit if phase is over */
            Rs2NpcModel sigil = Rs2Npc.getNpc(sigilNpcID);
            if (sigil == null) {
                Microbot.log("Sigil despawned – leaving attack loop");
                break;
            }

            /* 2 ─ detect a new sigil square */
            WorldPoint sigilSW = sigil.getWorldLocation();
            if (!sigilSW.equals(lastSigilSW)) {
                sigilMoves++;
                lastSigilSW = sigilSW;
                Microbot.log("Sigil moved → #" + sigilMoves);
            }

            /* 3 ─ choose the nearest predefined attack tile ≤1 tiles away */
            WorldPoint target = sigilSW;   // fallback = sigil itself
            for (WorldPoint atk : attackTiles) {
                if (atk.distanceTo(sigilSW) <= 1) { target = atk; break; }
            }

            /* 4 ─ walk onto the target tile if not already there */
            if (Rs2Player.distanceTo(target) > 1) {
                if (Rs2Walker.walkFastCanvas(target, true)) {
                    final WorldPoint destinationTile = target;
                    sleepUntil(() -> Rs2Player.getWorldLocation().equals(destinationTile), 2_000);
                }
            }

            /* 5 ─ attack the boss whenever idle */
            Rs2NpcModel boss = Rs2Npc.getNpc(bossNpcID);
            if (boss != null && !Rs2Combat.inCombat()) {
                Rs2Npc.attack(bossNpcID);
            }

            sleep(300);   // half a game tick
        }
    }

    /** True while the “boss check mark” widget is showing on-screen. ie. the boss is defeated */
    public static boolean bossIsDefeated(String bossName, int defeatedWidgetId) {
        Microbot.log(bossName + " is defeated: " + Rs2Widget.isWidgetVisible(defeatedWidgetId));
        return Rs2Widget.isWidgetVisible(defeatedWidgetId);
    }

    /** Runs the player out of the arena */
    public static void bossBailOut(WorldPoint bailOutLocation) {
        int exitStairsGroundObjectID = 53003;
        long endTime = System.currentTimeMillis() + 10_000;

        while (System.currentTimeMillis() < endTime) {
            if (Rs2GameObject.interact(exitStairsGroundObjectID)) {
                sleepUntil(() -> Rs2Widget.isWidgetVisible(19857413),5_000);
                Microbot.log("Successfully bailed out of the boss arena");
                return;
            }

            Microbot.log("Couldn't interact with the door. Attempting to move closer");
            Rs2Walker.walkFastCanvas(bailOutLocation, true);
            sleep(600);
        }
        Microbot.log("Timeout: Failed to bail out of the boss arena after 10 seconds.");
    }
}
