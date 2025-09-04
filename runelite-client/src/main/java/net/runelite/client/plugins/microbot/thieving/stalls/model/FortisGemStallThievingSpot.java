package net.runelite.client.plugins.microbot.thieving.stalls.model;

import net.runelite.api.GameObject;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.Global;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import javax.inject.Inject;

public class FortisGemStallThievingSpot implements IStallThievingSpot {

    private static final WorldPoint SAFESPOT = new WorldPoint(1671, 3101, 0);
    private static final int STALL_ID = 51935;

    @Inject
    public FortisGemStallThievingSpot() {}

    @Override
    public void thieve() {
        boolean atSafespot = SAFESPOT.equals(Rs2Player.getWorldLocation());
        if (!atSafespot) {
            Rs2Walker.walkTo(SAFESPOT);
            Global.sleepUntil(() -> SAFESPOT.equals(Rs2Player.getWorldLocation()), 3000);
            if (!SAFESPOT.equals(Rs2Player.getWorldLocation())) {
                return;
            }
        }

        final GameObject stall = Rs2GameObject.getGameObject(STALL_ID, SAFESPOT, 2);
        if (stall == null) {
            return;
        }

        Rs2GameObject.interact(stall, "Steal-from");
        Rs2Player.waitForXpDrop(Skill.THIEVING);
    }

    @Override
    public void bank() {
        Rs2Bank.walkToBankAndUseBank();
        Rs2Bank.depositAll();
        Rs2Bank.closeBank();
    }

    @Override
    public Integer[] getItemIdsToDrop() {
        return new Integer[0];
    }
}
