package net.runelite.client.plugins.microbot.bradleycombat.actions;


import net.runelite.client.plugins.microbot.bradleycombat.interfaces.CombatAction;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.target.Rs2Target;

import static net.runelite.client.plugins.microbot.util.player.Rs2Player.getPlayer;

public class WalkUnderAction implements CombatAction {
    @Override
    public void execute() {
        if (Rs2Target.validTarget() && Rs2Target.getTarget().getLocalLocation().isInScene())
            Rs2Player.walkUnder(getPlayer(Rs2Target.getTarget().getName()));
    }
}