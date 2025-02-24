package net.runelite.client.plugins.microbot.bradleycombat.actions;

import net.runelite.api.Player;
import net.runelite.client.plugins.microbot.bradleycombat.interfaces.CombatAction;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.target.Rs2Target;

public class AttackAction implements CombatAction {
    private final boolean shouldAttack;

    public AttackAction(boolean shouldAttack) {
        this.shouldAttack = shouldAttack;
    }

    @Override
    public void execute() {
        if (!shouldAttack) return;
        if (Rs2Target.validTarget()) Rs2Player.attack((Player) Rs2Target.getTarget());
    }
}