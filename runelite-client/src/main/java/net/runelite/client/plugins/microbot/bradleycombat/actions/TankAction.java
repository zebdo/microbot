package net.runelite.client.plugins.microbot.bradleycombat.actions;

import net.runelite.client.plugins.microbot.bradleycombat.BradleyCombatConfig;
import net.runelite.client.plugins.microbot.bradleycombat.interfaces.CombatAction;

public class TankAction implements CombatAction {
    private final BradleyCombatConfig config;

    public TankAction(BradleyCombatConfig config) {
        this.config = config;
    }

    @Override
    public void execute() {
        new EquipAction(config.gearIDsTank()).execute();
    }
}