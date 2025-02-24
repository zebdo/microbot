package net.runelite.client.plugins.microbot.bradleycombat.actions;

import net.runelite.client.plugins.microbot.bradleycombat.interfaces.CombatAction;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;

public class EquipAction implements CombatAction {
    private final String gearCfg;

    public EquipAction(String gearCfg) {
        this.gearCfg = gearCfg;
    }

    @Override
    public void execute() {
        if (gearCfg == null || gearCfg.trim().isEmpty())
            return;
        String[] split = gearCfg.split("\\s*,\\s*");
        for (String s : split) {
            s = s.trim();
            if (s.isEmpty())
                continue;
            try {
                int itemId = Integer.parseInt(s);
                if (Rs2Inventory.contains(itemId))
                    Rs2Inventory.equip(itemId);
            } catch (NumberFormatException ignored) { }
        }
    }
}