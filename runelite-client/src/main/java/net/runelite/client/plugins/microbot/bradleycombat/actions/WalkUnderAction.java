package net.runelite.client.plugins.microbot.bradleycombat.actions;


import net.runelite.client.plugins.microbot.bradleycombat.BradleyCombatPlugin;
import net.runelite.client.plugins.microbot.bradleycombat.interfaces.CombatAction;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import static net.runelite.client.plugins.microbot.util.player.Rs2Player.getPlayer;

public class WalkUnderAction implements CombatAction {
    @Override
    public void execute() {
        if (BradleyCombatPlugin.validTarget() && BradleyCombatPlugin.getTarget().getLocalLocation().isInScene())
            Rs2Player.walkUnder(getPlayer(BradleyCombatPlugin.getTarget().getName()));
    }
}