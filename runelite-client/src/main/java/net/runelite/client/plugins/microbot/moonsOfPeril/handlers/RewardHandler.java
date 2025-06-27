package net.runelite.client.plugins.microbot.moonsOfPeril.handlers;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.moonsOfPeril.enums.Locations;
import net.runelite.client.plugins.microbot.moonsOfPeril.enums.State;
import net.runelite.client.plugins.microbot.moonsOfPeril.enums.Widgets;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import static net.runelite.client.plugins.microbot.util.Global.sleep;

public class RewardHandler implements BaseHandler {

    private static final WorldPoint rewardChestLocation = Locations.REWARDS_CHEST_LOBBY.getWorldPoint();
    private static final int lunarChestGameObjectID = 51346;
    private static final int lunarChestBankAllWidgetID = 56885268;

    @Override
    public boolean validate() {
        return !BossHandler.bossIsAlive("Eclipse Moon", Widgets.ECLIPSE_MOON_ID.getID()) &&
                !BossHandler.bossIsAlive("Blue Moon", Widgets.BLUE_MOON_ID.getID()) &&
                !BossHandler.bossIsAlive("Blood Moon", Widgets.BLOOD_MOON_ID.getID());
    }

    @Override
    public State execute() {
        BossHandler.walkToBoss("Rewards Chest", rewardChestLocation);
        if (Rs2GameObject.interact(lunarChestGameObjectID, "Claim")) {
            Microbot.log("Successfully claimed rewards from Lunar Chest");
            sleep(2_400);
        }
        if (Rs2Widget.clickWidget(lunarChestBankAllWidgetID)) {
            Microbot.log("Successfully banked all rewards");
            sleep(1_200);
        }
        return State.IDLE;
    }
}
