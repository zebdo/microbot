package net.runelite.client.plugins.microbot.bradleycombat.actions;

import net.runelite.client.plugins.microbot.bradleycombat.BradleyCombatConfig;
import net.runelite.client.plugins.microbot.bradleycombat.enums.PrayerStyle;
import net.runelite.client.plugins.microbot.bradleycombat.interfaces.CombatAction;

public class RangeAction implements CombatAction {
    private final BradleyCombatConfig config;
    private final int variant;

    public RangeAction(BradleyCombatConfig config, int variant) {
        this.config = config;
        this.variant = variant;
    }

    @Override
    public void execute() {
        new PrayOffensiveAction(config, PrayerStyle.RANGE).execute();
        String gearIDs;
        boolean attackTarget;
        boolean useVengeance;
        switch (variant) {
            case 1:
                gearIDs = config.gearIDsRangePrimary();
                attackTarget = config.attackTargetRangePrimary();
                useVengeance = config.useVengeanceRangePrimary();
                break;
            case 2:
                gearIDs = config.gearIDsRangeSecondary();
                attackTarget = config.attackTargetRangeSecondary();
                useVengeance = config.useVengeanceRangeSecondary();
                break;
            case 3:
                gearIDs = config.gearIDsRangeTertiary();
                attackTarget = config.attackTargetRangeTertiary();
                useVengeance = config.useVengeanceRangeTertiary();
                break;
            default:
                gearIDs = "";
                attackTarget = false;
                useVengeance = false;
        }
        new EquipAction(gearIDs).execute();
        new VengeanceAction(useVengeance).execute();
        new AttackAction(attackTarget).execute();
    }
}