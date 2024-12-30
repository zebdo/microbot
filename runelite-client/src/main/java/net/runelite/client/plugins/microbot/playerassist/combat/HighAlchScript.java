package net.runelite.client.plugins.microbot.playerassist.combat;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.playerassist.PlayerAssistConfig;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Item;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.skillcalculator.skills.MagicAction;

import java.util.concurrent.TimeUnit;

public class HighAlchScript extends Script {
public boolean run(PlayerAssistConfig config) {
    mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
        try {
            if (!Microbot.isLoggedIn() || !super.run() || !config.toggleHighAlchProfitable()) return;
            Rs2Item item = Rs2Inventory.get(Rs2Item::isHaProfitable);
            if (item != null && Rs2Magic.canCast(MagicAction.HIGH_LEVEL_ALCHEMY)) {
                Rs2Magic.alch(item);
            }

        } catch(Exception ex) {
            System.out.println(ex.getMessage());
        }
    }, 0, 600, TimeUnit.MILLISECONDS);
    return true;
}


    public void shutdown() {
        super.shutdown();
    }
}
