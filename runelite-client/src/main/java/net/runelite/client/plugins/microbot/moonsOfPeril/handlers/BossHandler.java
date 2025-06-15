package net.runelite.client.plugins.microbot.moonsOfPeril.handlers;

import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;


import java.util.Objects;

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
/*        if (Rs2Equipment.isWearing(weaponMain, false)) {
            if (!Rs2Inventory.wield(weaponMain)) {
                Microbot.log("Could not find " + weaponMain + " in player's inventory");
            }
        }
        sleep(10_000);
        if (!Rs2Equipment.isWearing(shield, false)) {
            if (!Rs2Inventory.wield(shield)) {
                Microbot.log("Could not find " + shield + " in player's inventory");
            }
        }
        sleep(10_000);*/

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


    /** True while the “boss check mark” widget is showing on-screen. ie. the boss is defeated */
    public static boolean bossIsDefeated(String bossName, int defeatedWidgetId) {
        Microbot.log(bossName + " is defeated: " + Rs2Widget.isWidgetVisible(defeatedWidgetId));
        return Rs2Widget.isWidgetVisible(defeatedWidgetId);
    }
}
