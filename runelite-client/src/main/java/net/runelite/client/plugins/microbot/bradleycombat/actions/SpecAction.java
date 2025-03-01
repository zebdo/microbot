package net.runelite.client.plugins.microbot.bradleycombat.actions;


import net.runelite.client.plugins.microbot.bradleycombat.BradleyCombatConfig;
import net.runelite.client.plugins.microbot.bradleycombat.enums.PrayerStyle;
import net.runelite.client.plugins.microbot.bradleycombat.enums.SpecType;
import net.runelite.client.plugins.microbot.bradleycombat.interfaces.CombatAction;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;

public class SpecAction implements CombatAction {
    private final BradleyCombatConfig config;
    private final int variant;

    public SpecAction(BradleyCombatConfig config, int variant) {
        this.config = config;
        this.variant = variant;
    }

    @Override
    public void execute() {
        int requiredEnergy;
        SpecType specType;
        PrayerStyle prayerStyle;
        String gearIDs;
        boolean attackTarget;
        boolean useVengeance;
        switch (variant) {
            case 1:
                requiredEnergy = config.specEnergyPrimary();
                specType = config.specTypePrimary();
                prayerStyle = config.specPrayerStylePrimary();
                gearIDs = config.gearIDsSpecialAttackPrimary();
                attackTarget = config.attackTargetSpecPrimary();
                useVengeance = config.useVengeanceSpecPrimary();
                break;
            case 2:
                requiredEnergy = config.specEnergySecondary();
                specType = config.specTypeSecondary();
                prayerStyle = config.specPrayerStyleSecondary();
                gearIDs = config.gearIDsSpecialAttackSecondary();
                attackTarget = config.attackTargetSpecSecondary();
                useVengeance = config.useVengeanceSpecSecondary();
                break;
            case 3:
                requiredEnergy = config.specEnergyTertiary();
                specType = config.specTypeTertiary();
                prayerStyle = config.specPrayerStyleTertiary();
                gearIDs = config.gearIDsSpecialAttackTertiary();
                attackTarget = config.attackTargetSpecTertiary();
                useVengeance = config.useVengeanceSpecTertiary();
                break;
            default:
                return;
        }
        if (specType == SpecType.DOUBLE) {
            requiredEnergy *= 2;
        }
        if (Rs2Combat.getSpecEnergy() < requiredEnergy) {
            return;
        }
        new PrayOffensiveAction(config, prayerStyle).execute();
        new EquipAction(gearIDs).execute();
        new EnableSpecAction(config, variant).execute();
        new VengeanceAction(useVengeance).execute();
        new AttackAction(attackTarget).execute();
    }
}