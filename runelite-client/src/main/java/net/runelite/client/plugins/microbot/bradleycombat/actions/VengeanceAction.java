package net.runelite.client.plugins.microbot.bradleycombat.actions;

import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.bradleycombat.interfaces.CombatAction;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.skillcalculator.skills.MagicAction;

public class VengeanceAction implements CombatAction {
    private final boolean useVengeance;

    public VengeanceAction(boolean useVengeance) {
        this.useVengeance = useVengeance;
    }

    @Override
    public void execute() {
        if (!useVengeance) return;
        if (Microbot.getClient().getBoostedSkillLevel(Skill.MAGIC) <= 93) return;
        Rs2Magic.cast(MagicAction.VENGEANCE);
    }
}