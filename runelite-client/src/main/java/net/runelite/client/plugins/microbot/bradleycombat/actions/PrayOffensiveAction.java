package net.runelite.client.plugins.microbot.bradleycombat.actions;

import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.bradleycombat.BradleyCombatConfig;
import net.runelite.client.plugins.microbot.bradleycombat.enums.PrayerStyle;
import net.runelite.client.plugins.microbot.bradleycombat.interfaces.CombatAction;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;

import static net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer.isPrayerActive;
import static net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer.toggle;


public class PrayOffensiveAction implements CombatAction {
    private final BradleyCombatConfig config;
    private final PrayerStyle prayerStyle;

    public PrayOffensiveAction(BradleyCombatConfig config, PrayerStyle prayerStyle) {
        this.config = config;
        this.prayerStyle = prayerStyle;
    }

    @Override
    public void execute() {
        if (!config.enableOffensiveSpells()) return;
        int prayerLevel = Microbot.getClient().getRealSkillLevel(Skill.PRAYER);
        switch (prayerStyle) {
            case MELEE:
                Microbot.getClientThread().runOnSeperateThread(() -> {
                    toggleBestMeleePrayers(prayerLevel, config.hasPiety());
                    return null;
                });
                break;
            case RANGE:
                Microbot.getClientThread().runOnSeperateThread(() -> {
                    toggleBestRangePrayers(prayerLevel, config.hasRigour());
                    return null;
                });
                break;
            case MAGE:
                Microbot.getClientThread().runOnSeperateThread(() -> {
                    toggleBestMagePrayers(prayerLevel, config.hasAugury());
                    return null;
                });
                break;
        }
    }


    public static void toggleBestMeleePrayers(int lvl, boolean hasPiety) {
        if (hasPiety && lvl >= Rs2PrayerEnum.PIETY.getLevel()) {
            toggle(Rs2PrayerEnum.PIETY, true);
            return;
        }
        boolean steel = lvl >= Rs2PrayerEnum.STEEL_SKIN.getLevel();
        boolean rock = lvl >= Rs2PrayerEnum.ROCK_SKIN.getLevel();
        boolean thick = lvl >= Rs2PrayerEnum.THICK_SKIN.getLevel();
        if (steel) toggle(Rs2PrayerEnum.STEEL_SKIN, true);
        else if (rock) toggle(Rs2PrayerEnum.ROCK_SKIN, true);
        else if (thick) toggle(Rs2PrayerEnum.THICK_SKIN, true);

        boolean ultimate = lvl >= Rs2PrayerEnum.ULTIMATE_STRENGTH.getLevel();
        boolean superhuman = lvl >= Rs2PrayerEnum.SUPERHUMAN_STRENGTH.getLevel();
        boolean burst = lvl >= Rs2PrayerEnum.BURST_STRENGTH.getLevel();
        if (ultimate) toggle(Rs2PrayerEnum.ULTIMATE_STRENGTH, true);
        else if (superhuman) toggle(Rs2PrayerEnum.SUPERHUMAN_STRENGTH, true);
        else if (burst) toggle(Rs2PrayerEnum.BURST_STRENGTH, true);

        boolean incredible = lvl >= Rs2PrayerEnum.INCREDIBLE_REFLEXES.getLevel();
        boolean improved = lvl >= Rs2PrayerEnum.IMPROVED_REFLEXES.getLevel();
        boolean clarity = lvl >= Rs2PrayerEnum.CLARITY_THOUGHT.getLevel();
        if (incredible) toggle(Rs2PrayerEnum.INCREDIBLE_REFLEXES, true);
        else if (improved) toggle(Rs2PrayerEnum.IMPROVED_REFLEXES, true);
        else if (clarity) toggle(Rs2PrayerEnum.CLARITY_THOUGHT, true);
    }

    public static void toggleBestRangePrayers(int lvl, boolean hasRigour) {
        if (hasRigour && lvl >= Rs2PrayerEnum.RIGOUR.getLevel()) {
            toggle(Rs2PrayerEnum.RIGOUR, true);
            return;
        }
        if (lvl >= Rs2PrayerEnum.EAGLE_EYE.getLevel()) {
            toggle(Rs2PrayerEnum.EAGLE_EYE, true);
            if (isPrayerActive(Rs2PrayerEnum.EAGLE_EYE)) return;
        }
        if (lvl >= Rs2PrayerEnum.HAWK_EYE.getLevel()) {
            toggle(Rs2PrayerEnum.HAWK_EYE, true);
            if (isPrayerActive(Rs2PrayerEnum.HAWK_EYE)) return;
        }
        if (lvl >= Rs2PrayerEnum.SHARP_EYE.getLevel()) {
            toggle(Rs2PrayerEnum.SHARP_EYE, true);
        }
    }

    public static void toggleBestMagePrayers(int lvl, boolean hasAugury) {
        if (hasAugury && lvl >= Rs2PrayerEnum.AUGURY.getLevel()) {
            toggle(Rs2PrayerEnum.AUGURY, true);
            return;
        }
        if (lvl >= Rs2PrayerEnum.MYSTIC_MIGHT.getLevel()) {
            toggle(Rs2PrayerEnum.MYSTIC_MIGHT, true);
            if (isPrayerActive(Rs2PrayerEnum.MYSTIC_MIGHT)) return;
        }
        if (lvl >= Rs2PrayerEnum.MYSTIC_LORE.getLevel()) {
            toggle(Rs2PrayerEnum.MYSTIC_LORE, true);
            if (isPrayerActive(Rs2PrayerEnum.MYSTIC_LORE)) return;
        }
        if (lvl >= Rs2PrayerEnum.MYSTIC_WILL.getLevel()) {
            toggle(Rs2PrayerEnum.MYSTIC_WILL, true);
        }
    }


}