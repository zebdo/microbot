package net.runelite.client.plugins.microbot.bradleycombat.actions;

import net.runelite.client.plugins.microbot.bradleycombat.BradleyCombatConfig;
import net.runelite.client.plugins.microbot.bradleycombat.enums.SpecType;
import net.runelite.client.plugins.microbot.bradleycombat.interfaces.CombatAction;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;

import static net.runelite.client.plugins.microbot.util.Global.sleep;

public class EnableSpecAction implements CombatAction {
    private final BradleyCombatConfig config;
    private final int variant;

    public EnableSpecAction(BradleyCombatConfig config, int variant) {
        this.config = config;
        this.variant = variant;
    }

    @Override
    public void execute() {
        int energy;
        SpecType specType;
        switch (variant) {
            case 1:
                energy = config.specEnergyPrimary();
                specType = config.specTypePrimary();
                break;
            case 2:
                energy = config.specEnergySecondary();
                specType = config.specTypeSecondary();
                break;
            case 3:
                energy = config.specEnergyTertiary();
                specType = config.specTypeTertiary();
                break;
            default:
                energy = 0;
                specType = SpecType.SINGLE;
        }
        if (specType == SpecType.DOUBLE) {
            energy *= 2;
        }
        if (!Rs2Combat.getSpecState() && Rs2Combat.getSpecEnergy() >= energy) {
            if (specType == SpecType.SINGLE) {
                Rs2Combat.setSpecState(true, energy);
            } else {
                Rs2Combat.setSpecState(true, energy);
                sleep(20);
                Rs2Combat.setSpecState(true, energy);
            }
        }
    }
}