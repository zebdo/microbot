package net.runelite.client.plugins.microbot.runecrafting.arceuus;

import lombok.Getter;
import net.runelite.api.GameObject;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import org.apache.commons.lang3.NotImplementedException;

import java.util.concurrent.TimeUnit;

public class ArceuusRcScript extends Script {
    public static String version = "1.0.1";
    public static int darkAltarTripCount = 0;

    private static ArceuusRcConfig config;

    // TODO: Use to determine state
    private static final int MAX_DARK_ESSENCE_FRAGMENTS = 108;
    private static final int DARK_ESSENCE_FRAGS_PER_BLOCK = 4;

    private static final int BLOOD_ESSENCE_ACTIVE = ItemID.BLOOD_ESSENCE_ACTIVE;
    private static final int BLOOD_ESSENCE = ItemID.BLOOD_ESSENCE_INACTIVE;
    private static final int DARK_ESSENCE_FRAGMENTS = ItemID.BIGBLANKRUNE;
    private static final int DENSE_ESSENCE_BLOCK = ItemID.ARCEUUS_ESSENCE_BLOCK;
    private static final int DARK_ESSENCE_BLOCK = ItemID.ARCEUUS_ESSENCE_BLOCK_DARK;

    private static final String DARK_ALTAR = "Dark altar";
    private static final String STR_DENSE_RUNESTONE = "Dense runestone";

    public static final WorldPoint ARCEUUS_BLOOD_ALTAR = new WorldPoint(1720, 3828, 0);
    public static final WorldPoint ARCEUUS_SOUL_ALTAR = new WorldPoint(1815, 3856, 0);
    public static final WorldPoint ARCEUUS_DARK_ALTAR = new WorldPoint(1718, 3880, 0);
    public static final WorldPoint DENSE_RUNESTONE = new WorldPoint(1760, 3853, 0);

    @Getter
    private String state = "Unknown";

    public boolean run(ArceuusRcConfig config) {
        ArceuusRcScript.config = config;
        Rs2Antiban.antibanSetupTemplates.applyUniversalAntibanSetup();
        if(Microbot.isLoggedIn()) {
            if(Rs2Inventory.hasItem(DARK_ESSENCE_FRAGMENTS)) {
                darkAltarTripCount++;
                if(Rs2Inventory.hasItem(DARK_ESSENCE_BLOCK)) {
                    darkAltarTripCount++;
                }
            }
        }
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(this::executeTask, 0, 600, TimeUnit.MILLISECONDS);
        return true;
    }

    private void executeTask() {
        try {
            if (!super.run() || !Microbot.isLoggedIn()) {
                state = "Disabled";
                return;
            }
            if (shouldGoToBloodAltar()) {
                state = "Go to " + getAltarName();
                goToAltar();
            } else if (shouldUseAltar()) {
                state = "Use " + getAltarName();
                useAltar();
            } else if (shouldGoToDarkAltar()) {
                state = "Go to Dark Altar";
                goToDarkAltar();
            } else if (shouldUseDarkAltar()) {
                state = "Use Dark Altar";
                useDarkAltar();
            } else if (shouldGoToRunestone()) {
                state = "Go to Runestone";
                goToRunestone();
            } else if (shouldChipEssence()) {
                state = "Chip Essence" + (config.getChipEssenceFast() ? " Fast" : "");
                chipEssence(config.getChipEssenceFast());
            } else if (shouldMineEssence()) {
                state = "Mine Essence";
                mineEssence();
            } else {
                state = "Unknown";
            }
        } catch (Exception e) {
            Microbot.log("Error in Arceuus Runecrafter: " + e.getMessage());
        }
    }

    public Altar getAltar() {
        if (config.getAltar() != Altar.AUTO) return config.getAltar();
        final int level = Microbot.getClient().getRealSkillLevel(Skill.RUNECRAFT);
        if (level >= 90) return Altar.SOUL;
        if (level >= 77) return Altar.BLOOD;
        // TODO: handle this better
        throw new IllegalStateException("Runecraft Level " + level + " to low");
    }

    public WorldPoint getAltarWorldPoint() {
        if (getAltar() == Altar.BLOOD) return ARCEUUS_BLOOD_ALTAR;
        return ARCEUUS_SOUL_ALTAR;
    }

    public String getAltarName() {
        if (getAltar() == Altar.BLOOD) return "Blood Altar";
        return "Soul Altar";
    }

    public boolean shouldGoToBloodAltar() {
        return darkAltarTripCount >= 2
                && Rs2Inventory.hasItem(DARK_ESSENCE_FRAGMENTS)
                && Rs2Player.getWorldLocation().distanceTo(getAltarWorldPoint()) > 10;
    }

    public void goToAltar() {
        Rs2Walker.walkTo(getAltarWorldPoint());
    }

    public boolean shouldGoToDarkAltar() {
        return Rs2Inventory.isFull()
                && Rs2Inventory.hasItem(DENSE_ESSENCE_BLOCK)
                && Rs2Player.getWorldLocation().distanceTo(ARCEUUS_DARK_ALTAR) > 10
                && darkAltarTripCount < 2;
    }

    public void goToDarkAltar() {
        Rs2Walker.walkTo(ARCEUUS_DARK_ALTAR);
    }

    public boolean shouldGoToRunestone() {
        return !Rs2Inventory.isFull()
                && Rs2Player.getWorldLocation().distanceTo(DENSE_RUNESTONE) > 8
                && darkAltarTripCount < 2
                && !(Rs2Player.getWorldLocation().distanceTo(getAltarWorldPoint()) < 5
                    && Rs2Inventory.hasItem(DARK_ESSENCE_BLOCK,DARK_ESSENCE_FRAGMENTS));
    }

    public void goToRunestone() {
        Rs2Walker.walkTo(DENSE_RUNESTONE);
    }

    public boolean shouldUseAltar() {
        if (!Rs2Inventory.hasItem(DARK_ESSENCE_FRAGMENTS)) return false;
        final GameObject altar = Rs2GameObject.getGameObject(getAltarName(), true, 11);
        return altar != null;
    }

    public void useAltar() {
        final GameObject altar = Rs2GameObject.getGameObject(getAltarName(), true, 11);
        if (altar != null) {
            if (Rs2GameObject.interact(altar,"Bind"))
                Rs2Inventory.waitForInventoryChanges(6000);
            darkAltarTripCount = 0;

        }
    }

    public boolean shouldChipEssence() {
        final GameObject altar = Rs2GameObject.getGameObject(getAltarName(), true, 11);
        return Rs2Inventory.isFull() && !Rs2Inventory.hasItem(DARK_ESSENCE_FRAGMENTS) && Rs2Inventory.hasItem(DARK_ESSENCE_BLOCK)
                || (!Rs2Inventory.hasItem(DARK_ESSENCE_FRAGMENTS)
                    && Rs2Inventory.hasItem(DARK_ESSENCE_BLOCK)
                    && altar != null);
    }

    public boolean moveChisel() {
        if (Rs2Inventory.slotContains(27, ItemID.CHISEL)) return true;
        final Rs2ItemModel chisel = Rs2Inventory.get(ItemID.CHISEL);
        if (chisel == null) {
            Microbot.log("No chisel found in inventory");
            return false;
        }
        if (Rs2Inventory.moveItemToSlot(chisel,27)) {
            if (!sleepUntil(() -> Rs2Inventory.slotContains(27, ItemID.CHISEL),6_000)) {
                Microbot.log("Failed to move chisel to slot 27");
                return false;
            }
        }
        return true;
    }

    public boolean chipEssence(boolean fast) {
        if (!moveChisel()) return false;
        return fast ? chipEssenceFast() : chipEssenceSlow();
    }

    public boolean chipEssenceSlow() {
        if(Rs2Inventory.combineClosest(DARK_ESSENCE_BLOCK,ItemID.CHISEL)) {
            return sleepUntil(() -> !Rs2Inventory.hasItem(DARK_ESSENCE_BLOCK), 60_000);
        }
        Microbot.log("Failed to await no dark essence block in inventory");
        return false;
    }

    private void reverse(int[] ints) {
        if (ints.length != 2) throw new NotImplementedException("reverse does not support length != 2");
        final int tmp = ints[0];
        ints[0] = ints[1];
        ints[1] = tmp;
    }

    public boolean chipEssenceFast() {
        final int[] ids = {ItemID.CHISEL, DARK_ESSENCE_BLOCK};
        while (Rs2Inventory.containsAll(ids)) {
            if (!Rs2Inventory.combineClosest(ids[0], ids[1])) {
                Microbot.log("Failed to combine closest chisel & dark essence block");
                return false;
            }
            reverse(ids);
        }
        return true;
    }

    public boolean shouldUseDarkAltar() {
        if (!Rs2Inventory.isFull()) return false;
        if (!Rs2Inventory.hasItem(DENSE_ESSENCE_BLOCK)) return false;
        final GameObject darkAltar = Rs2GameObject.getGameObject(DARK_ALTAR, true, 11);
        return darkAltar != null;
    }

    public void useDarkAltar() {
        final GameObject darkAltar = Rs2GameObject.getGameObject(DARK_ALTAR, true, 11);
        if (darkAltar == null) return;

        Rs2GameObject.interact(darkAltar,"Venerate");
        sleepUntil(()->!Rs2Inventory.hasItem(DENSE_ESSENCE_BLOCK),6_000);
        darkAltarTripCount++;
    }

    public void mineEssence() {
        if(getAltar() == Altar.BLOOD && !Rs2Inventory.hasItem(BLOOD_ESSENCE_ACTIVE)){
            Rs2Inventory.interact(BLOOD_ESSENCE, "Activate");
        }
        final GameObject runeStone = Rs2GameObject.getGameObject(STR_DENSE_RUNESTONE, true, 11);
        if (runeStone == null) { // should never happen bc shouldMineEssence checks for the runestone
            Microbot.log("Cannot find runestone");
            return;
        }
        Rs2GameObject.interact(runeStone,"Chip");
        Rs2Player.waitForAnimation(10000);
    }
    public static boolean shouldMineEssence() {
        if (Rs2Inventory.hasItem(DARK_ESSENCE_BLOCK)) return false;
        if (Rs2Inventory.isFull()) return false;
        if (Rs2Player.isAnimating()) return false;
        final GameObject runeStone = Rs2GameObject.getGameObject(STR_DENSE_RUNESTONE, true, 11);
        return runeStone != null;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        darkAltarTripCount = 0;
    }
}
