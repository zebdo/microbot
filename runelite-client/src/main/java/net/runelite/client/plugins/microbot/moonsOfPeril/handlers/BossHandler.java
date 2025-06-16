package net.runelite.client.plugins.microbot.moonsOfPeril.handlers;

import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.moonsOfPeril.enums.GameObjects;
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
    //  1. check that all config weapons are in inventory or wielded
    //  2. check that Moons of Peril quest has been completed
    //  add check before equipping weapons that enough inventory space exists, otherwise don't equip

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
        if (!Rs2Equipment.isWearing(Arrays.asList(weaponMain, shield), false, Collections.emptyList())) {
            if (Rs2Inventory.wield(weaponMain)) {
                Microbot.log(weaponMain + " is now equipped");
            }
            if (Rs2Inventory.wield(shield)) {
                Microbot.log(weaponMain + " is now equipped");
            }
        }
        Microbot.log("Gear equipped: " + weaponMain + " + " + shield);
        sleep(2_000);

        // 2. Eats food if hitpoints below 80%
        Rs2Player.eatAt(80);
        sleep(600);

        // 3. Drinks potions if prayer points below 80%
        int minimumPrayerPoints = (Rs2Player.getRealSkillLevel(Skill.PRAYER) * 4) / 5;
        Rs2Player.drinkPrayerPotionAt(minimumPrayerPoints);
        sleep(600);

        // 4. Turns on Player's best offensive melee prayer
        Rs2Prayer.disableAllPrayers();
        sleep(600);
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

/*    *//**
     * Moves the player to stand on the highlighted sigil and attack the boss
     *
     * @return void
     *//*
    public static void normalAttackSequence(int sigilNpcID, int bossNpcID) {
        Microbot.log("Player is in a normal attack sequence");
        Rs2NpcModel sigilNpc = Rs2Npc.getNpc(sigilNpcID);
        WorldPoint attackTile = sigilNpc.getWorldLocation();
        Microbot.log("Walking to highlighted sigil");
        Rs2Walker.walkFastCanvas(attackTile);
        while (Rs2Player.distanceTo(sigilNpc.getWorldLocation()) <= 2) {
            if (Rs2Npc.getNpc(bossNpcID) != null) {
                Microbot.log("Attacking boss from highlighted sigil");
                if (Rs2Npc.attack(bossNpcID)) {
                    sleep(600);
                };
            }
        }

    }*/

    /**
     * Stand on the highlighted sigil tile and attack the boss.
     */
    public static void normalAttackSequence(int sigilNpcID, int bossNpcID)
    {
        while (true)
        {
            /* 1 ─ get current sigil NPC; leave if phase is over */
            Rs2NpcModel sigil = Rs2Npc.getNpc(sigilNpcID);
            if (sigil == null) {
                Microbot.log("Sigil NPC despawned – leaving attack loop");
                break;
            }

            WorldPoint sigilTile = sigil.getWorldLocation();

            /* 2 ─ step onto the sigil if we’re not already there */
            if (Rs2Player.distanceTo(sigilTile) > 2) {
                Microbot.log("Walking to highlighted sigil at " + sigilTile);
                if (Rs2Walker.walkFastCanvas(sigilTile)){
                    sleepUntil(() -> Rs2Player.distanceTo(sigilTile) <= 1, 3_000);
                }
            }

            /* 3 ─ once on the sigil, keep attacking the boss */
            Rs2NpcModel boss = Rs2Npc.getNpc(bossNpcID);
            if (boss != null && !Rs2Player.isAnimating()) {
                Microbot.log("Attacking boss from highlighted sigil");
                Rs2Npc.attack(bossNpcID);
            }

            sleep(600);   // ~2 game ticks between refreshes
        }
    }


    /** True while the “boss check mark” widget is showing on-screen. ie. the boss is defeated */
    public static boolean bossIsDefeated(String bossName, int defeatedWidgetId) {
        Microbot.log(bossName + " is defeated: " + Rs2Widget.isWidgetVisible(defeatedWidgetId));
        return Rs2Widget.isWidgetVisible(defeatedWidgetId);
    }
}
