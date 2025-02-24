package net.runelite.client.plugins.microbot.bradleycombat.actions;

import net.runelite.client.plugins.microbot.bradleycombat.BradleyCombatConfig;
import net.runelite.client.plugins.microbot.bradleycombat.BradleyCombatPlugin;
import net.runelite.client.plugins.microbot.bradleycombat.enums.PrayerStyle;
import net.runelite.client.plugins.microbot.bradleycombat.interfaces.CombatAction;
import net.runelite.client.plugins.microbot.util.magic.Rs2CombatSpells;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;

public class MageAction implements CombatAction {
    private final BradleyCombatConfig config;
    private final int variant;

    public MageAction(BradleyCombatConfig config, int variant) {
        this.config = config;
        this.variant = variant;
    }

    @Override
    public void execute() {
        String gearIDs;
        Rs2CombatSpells spell;
        switch (variant) {
            case 1:
                gearIDs = config.gearIDsMagePrimary();
                spell = config.selectedCombatSpellPrimary();
                break;
            case 2:
                gearIDs = config.gearIDsMageSecondary();
                spell = config.selectedCombatSpellSecondary();
                break;
            case 3:
                gearIDs = config.gearIDsMageTertiary();
                spell = config.selectedCombatSpellTertiary();
                break;
            default:
                gearIDs = "";
                spell = Rs2CombatSpells.WIND_STRIKE;
        }
        new PrayOffensiveAction(config, PrayerStyle.MAGE).execute();
        new EquipAction(gearIDs).execute();
        if (BradleyCombatPlugin.validTarget()) {
            Rs2Magic.castOn(spell.getMagicAction(), BradleyCombatPlugin.getTarget());
        }
    }
}