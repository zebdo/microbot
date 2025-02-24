package net.runelite.client.plugins.microbot.bradleycombat;

import net.runelite.client.config.*;
import net.runelite.client.plugins.microbot.bradleycombat.enums.PrayerStyle;
import net.runelite.client.plugins.microbot.bradleycombat.enums.SpecType;
import net.runelite.client.plugins.microbot.util.magic.Rs2CombatSpells;

import java.awt.*;

@ConfigInformation("<html>" + "<h3>BradleyCombat Configuration</h3>" + "<p><b>Setup Instructions:</b></p>" + "<ul>" + "<li>Disable anti-ban features in your client for optimal performance.</li>" + "<li>Disable natural mouse movement to ensure precise actions.</li>" + "<li>Enable <b>CAPS LOCK</b> for hotkeys to maintain chat functionality during combat while keeping all keys accessible.</li>" + "</ul>" + "</html>")
@ConfigGroup("bradleycombat")
public interface BradleyCombatConfig extends Config {

    @ConfigSection(name = "Melee: Primary", description = "Primary melee configuration", position = 1, closedByDefault = true)
    String meleePrimary = "meleePrimary";

    @ConfigItem(keyName = "hotkeyMeleePrimary", name = "Hotkey for Melee", description = "Hotkey for primary melee gear swap", position = 1, section = meleePrimary)
    default Keybind hotkeyMeleePrimary() {
        return Keybind.NOT_SET;
    }

    @ConfigItem(keyName = "gearIDsMeleePrimary", name = "Gear IDs", description = "Comma-separated list of Gear IDs for primary melee", position = 2, section = meleePrimary)
    default String gearIDsMeleePrimary() {
        return "";
    }

    @ConfigItem(keyName = "attackTargetMeleePrimary", name = "Attack Target", description = "Attack target after primary melee gear swap", position = 3, section = meleePrimary)
    default boolean attackTargetMeleePrimary() {
        return false;
    }

    @ConfigItem(keyName = "useVengeanceMeleePrimary", name = "Use Vengeance", description = "If enabled, casts Vengeance during primary melee", position = 4, section = meleePrimary)
    default boolean useVengeanceMeleePrimary() {
        return false;
    }

    @ConfigItem(keyName = "postActionMeleePrimary", name = "Trigger with Animation(s)", description = "Comma-separated list of animation IDs that trigger post-action for primary melee", position = 5, section = meleePrimary)
    default String postActionMeleePrimary() {
        return "";
    }

    @ConfigSection(name = "Melee: Secondary", description = "Secondary melee configuration", position = 2, closedByDefault = true)
    String meleeSecondary = "meleeSecondary";

    @ConfigItem(keyName = "hotkeyMeleeSecondary", name = "Hotkey for Melee", description = "Hotkey for secondary melee gear swap", position = 1, section = meleeSecondary)
    default Keybind hotkeyMeleeSecondary() {
        return Keybind.NOT_SET;
    }

    @ConfigItem(keyName = "gearIDsMeleeSecondary", name = "Gear IDs", description = "Comma-separated list of Gear IDs for secondary melee", position = 2, section = meleeSecondary)
    default String gearIDsMeleeSecondary() {
        return "";
    }

    @ConfigItem(keyName = "attackTargetMeleeSecondary", name = "Attack Target", description = "Attack target after secondary melee gear swap", position = 3, section = meleeSecondary)
    default boolean attackTargetMeleeSecondary() {
        return false;
    }

    @ConfigItem(keyName = "useVengeanceMeleeSecondary", name = "Use Vengeance", description = "If enabled, casts Vengeance during secondary melee", position = 4, section = meleeSecondary)
    default boolean useVengeanceMeleeSecondary() {
        return false;
    }

    @ConfigItem(keyName = "postActionMeleeSecondary", name = "Trigger with Animation(s)", description = "Comma-separated list of animation IDs that trigger post-action for secondary melee", position = 5, section = meleeSecondary)
    default String postActionMeleeSecondary() {
        return "";
    }

    @ConfigSection(name = "Melee: Tertiary", description = "Tertiary melee configuration", position = 3, closedByDefault = true)
    String meleeTertiary = "meleeTertiary";

    @ConfigItem(keyName = "hotkeyMeleeTertiary", name = "Hotkey for Melee", description = "Hotkey for tertiary melee gear swap", position = 1, section = meleeTertiary)
    default Keybind hotkeyMeleeTertiary() {
        return Keybind.NOT_SET;
    }

    @ConfigItem(keyName = "gearIDsMeleeTertiary", name = "Gear IDs", description = "Comma-separated list of Gear IDs for tertiary melee", position = 2, section = meleeTertiary)
    default String gearIDsMeleeTertiary() {
        return "";
    }

    @ConfigItem(keyName = "attackTargetMeleeTertiary", name = "Attack Target", description = "Attack target after tertiary melee gear swap", position = 3, section = meleeTertiary)
    default boolean attackTargetMeleeTertiary() {
        return false;
    }

    @ConfigItem(keyName = "useVengeanceMeleeTertiary", name = "Use Vengeance", description = "If enabled, casts Vengeance during tertiary melee", position = 4, section = meleeTertiary)
    default boolean useVengeanceMeleeTertiary() {
        return false;
    }

    @ConfigItem(keyName = "postActionMeleeTertiary", name = "Trigger with Animation(s)", description = "Comma-separated list of animation IDs that trigger post-action for tertiary melee", position = 5, section = meleeTertiary)
    default String postActionMeleeTertiary() {
        return "";
    }

    @ConfigSection(name = "Range: Primary", description = "Primary range configuration", position = 4, closedByDefault = true)
    String rangePrimary = "rangePrimary";

    @ConfigItem(keyName = "hotkeyRangePrimary", name = "Hotkey for Range", description = "Hotkey for primary range gear swap", position = 1, section = rangePrimary)
    default Keybind hotkeyRangePrimary() {
        return Keybind.NOT_SET;
    }

    @ConfigItem(keyName = "gearIDsRangePrimary", name = "Gear IDs", description = "Comma-separated list of Gear IDs for primary range", position = 2, section = rangePrimary)
    default String gearIDsRangePrimary() {
        return "";
    }

    @ConfigItem(keyName = "attackTargetRangePrimary", name = "Attack Target", description = "Attack target after primary range gear swap", position = 3, section = rangePrimary)
    default boolean attackTargetRangePrimary() {
        return false;
    }

    @ConfigItem(keyName = "useVengeanceRangePrimary", name = "Use Vengeance", description = "If enabled, casts Vengeance during primary range", position = 4, section = rangePrimary)
    default boolean useVengeanceRangePrimary() {
        return false;
    }

    @ConfigItem(keyName = "postActionRangePrimary", name = "Trigger with Animation(s)", description = "Comma-separated list of animation IDs that trigger post-action for primary range", position = 5, section = rangePrimary)
    default String postActionRangePrimary() {
        return "";
    }

    @ConfigSection(name = "Range: Secondary", description = "Secondary range configuration", position = 5, closedByDefault = true)
    String rangeSecondary = "rangeSecondary";

    @ConfigItem(keyName = "hotkeyRangeSecondary", name = "Hotkey for Range", description = "Hotkey for secondary range gear swap", position = 1, section = rangeSecondary)
    default Keybind hotkeyRangeSecondary() {
        return Keybind.NOT_SET;
    }

    @ConfigItem(keyName = "gearIDsRangeSecondary", name = "Gear IDs", description = "Comma-separated list of Gear IDs for secondary range", position = 2, section = rangeSecondary)
    default String gearIDsRangeSecondary() {
        return "";
    }

    @ConfigItem(keyName = "attackTargetRangeSecondary", name = "Attack Target", description = "Attack target after secondary range gear swap", position = 3, section = rangeSecondary)
    default boolean attackTargetRangeSecondary() {
        return false;
    }

    @ConfigItem(keyName = "useVengeanceRangeSecondary", name = "Use Vengeance", description = "If enabled, casts Vengeance during secondary range", position = 4, section = rangeSecondary)
    default boolean useVengeanceRangeSecondary() {
        return false;
    }

    @ConfigItem(keyName = "postActionRangeSecondary", name = "Trigger with Animation(s)", description = "Comma-separated list of animation IDs that trigger post-action for secondary range", position = 5, section = rangeSecondary)
    default String postActionRangeSecondary() {
        return "";
    }

    @ConfigSection(name = "Range: Tertiary", description = "Tertiary range configuration", position = 6, closedByDefault = true)
    String rangeTertiary = "rangeTertiary";

    @ConfigItem(keyName = "hotkeyRangeTertiary", name = "Hotkey for Range", description = "Hotkey for tertiary range gear swap", position = 1, section = rangeTertiary)
    default Keybind hotkeyRangeTertiary() {
        return Keybind.NOT_SET;
    }

    @ConfigItem(keyName = "gearIDsRangeTertiary", name = "Gear IDs", description = "Comma-separated list of Gear IDs for tertiary range", position = 2, section = rangeTertiary)
    default String gearIDsRangeTertiary() {
        return "";
    }

    @ConfigItem(keyName = "attackTargetRangeTertiary", name = "Attack Target", description = "Attack target after tertiary range gear swap", position = 3, section = rangeTertiary)
    default boolean attackTargetRangeTertiary() {
        return false;
    }

    @ConfigItem(keyName = "useVengeanceRangeTertiary", name = "Use Vengeance", description = "If enabled, casts Vengeance during tertiary range", position = 4, section = rangeTertiary)
    default boolean useVengeanceRangeTertiary() {
        return false;
    }

    @ConfigItem(keyName = "postActionRangeTertiary", name = "Trigger with Animation(s)", description = "Comma-separated list of animation IDs that trigger post-action for tertiary range", position = 5, section = rangeTertiary)
    default String postActionRangeTertiary() {
        return "";
    }

    @ConfigSection(name = "Mage: Primary", description = "Primary mage configuration", position = 7, closedByDefault = true)
    String magePrimary = "magePrimary";

    @ConfigItem(keyName = "hotkeyMagePrimary", name = "Hotkey for Mage", description = "Hotkey for primary mage gear swap", position = 1, section = magePrimary)
    default Keybind hotkeyMagePrimary() {
        return Keybind.NOT_SET;
    }

    @ConfigItem(keyName = "gearIDsMagePrimary", name = "Gear IDs", description = "Comma-separated list of Gear IDs for primary mage", position = 2, section = magePrimary)
    default String gearIDsMagePrimary() {
        return "";
    }

    @ConfigItem(keyName = "selectedCombatSpellPrimary", name = "Selected Spell", description = "Combat spell to cast for primary mage", position = 3, section = magePrimary)
    default Rs2CombatSpells selectedCombatSpellPrimary() {
        return Rs2CombatSpells.WIND_STRIKE;
    }

    @ConfigItem(keyName = "postActionMagePrimary", name = "Trigger with Animation(s)", description = "Comma-separated list of animation IDs that trigger post-action for primary mage", position = 4, section = magePrimary)
    default String postActionMagePrimary() {
        return "";
    }

    @ConfigSection(name = "Mage: Secondary", description = "Secondary mage configuration", position = 8, closedByDefault = true)
    String mageSecondary = "mageSecondary";

    @ConfigItem(keyName = "hotkeyMageSecondary", name = "Hotkey for Mage", description = "Hotkey for secondary mage gear swap", position = 1, section = mageSecondary)
    default Keybind hotkeyMageSecondary() {
        return Keybind.NOT_SET;
    }

    @ConfigItem(keyName = "gearIDsMageSecondary", name = "Gear IDs", description = "Comma-separated list of Gear IDs for secondary mage", position = 2, section = mageSecondary)
    default String gearIDsMageSecondary() {
        return "";
    }

    @ConfigItem(keyName = "selectedCombatSpellSecondary", name = "Selected Spell", description = "Combat spell to cast for secondary mage", position = 3, section = mageSecondary)
    default Rs2CombatSpells selectedCombatSpellSecondary() {
        return Rs2CombatSpells.WIND_STRIKE;
    }

    @ConfigItem(keyName = "postActionMageSecondary", name = "Trigger with Animation(s)", description = "Comma-separated list of animation IDs that trigger post-action for secondary mage", position = 4, section = mageSecondary)
    default String postActionMageSecondary() {
        return "";
    }

    @ConfigSection(name = "Mage: Tertiary", description = "Tertiary mage configuration", position = 9, closedByDefault = true)
    String mageTertiary = "mageTertiary";

    @ConfigItem(keyName = "hotkeyMageTertiary", name = "Hotkey for Mage", description = "Hotkey for tertiary mage gear swap", position = 1, section = mageTertiary)
    default Keybind hotkeyMageTertiary() {
        return Keybind.NOT_SET;
    }

    @ConfigItem(keyName = "gearIDsMageTertiary", name = "Gear IDs", description = "Comma-separated list of Gear IDs for tertiary mage", position = 2, section = mageTertiary)
    default String gearIDsMageTertiary() {
        return "";
    }

    @ConfigItem(keyName = "selectedCombatSpellTertiary", name = "Selected Spell", description = "Combat spell to cast for tertiary mage", position = 3, section = mageTertiary)
    default Rs2CombatSpells selectedCombatSpellTertiary() {
        return Rs2CombatSpells.WIND_STRIKE;
    }

    @ConfigItem(keyName = "postActionMageTertiary", name = "Trigger with Animation(s)", description = "Comma-separated list of animation IDs that trigger post-action for tertiary mage", position = 4, section = mageTertiary)
    default String postActionMageTertiary() {
        return "";
    }

    @ConfigSection(name = "Special Attack: Primary", description = "Primary special attack configuration", position = 10, closedByDefault = true)
    String specialAttackPrimary = "specialAttackPrimary";

    @ConfigItem(keyName = "hotkeySpecialAttackPrimary", name = "Hotkey for Special Attack", description = "Hotkey for special attack gear swap (primary)", position = 1, section = specialAttackPrimary)
    default Keybind hotkeySpecialAttackPrimary() {
        return Keybind.NOT_SET;
    }

    @ConfigItem(keyName = "gearIDsSpecialAttackPrimary", name = "Gear IDs", description = "Comma-separated list of Gear IDs for special attack (primary)", position = 2, section = specialAttackPrimary)
    default String gearIDsSpecialAttackPrimary() {
        return "";
    }

    @ConfigItem(keyName = "specPrayerStylePrimary", name = "Prayer Style", description = "Prayer to activate after using special attack (primary)", position = 3, section = specialAttackPrimary)
    default PrayerStyle specPrayerStylePrimary() {
        return PrayerStyle.MELEE;
    }

    @ConfigItem(keyName = "specTypePrimary", name = "Spec Type", description = "Single or Double spec for special attack (primary)", position = 4, section = specialAttackPrimary)
    default SpecType specTypePrimary() {
        return SpecType.SINGLE;
    }

    @ConfigItem(keyName = "attackTargetSpecPrimary", name = "Attack Target", description = "Attack target after special attack gear swap (primary)", position = 5, section = specialAttackPrimary)
    default boolean attackTargetSpecPrimary() {
        return false;
    }

    @ConfigItem(keyName = "specEnergyPrimary", name = "Spec Energy Consumed", description = "Spec energy used per attempt for special attack (primary)", position = 6, section = specialAttackPrimary)
    default int specEnergyPrimary() {
        return 50;
    }

    @ConfigItem(keyName = "useVengeanceSpecPrimary", name = "Use Vengeance", description = "If enabled, casts Vengeance during special attack (primary)", position = 7, section = specialAttackPrimary)
    default boolean useVengeanceSpecPrimary() {
        return false;
    }

    @ConfigItem(keyName = "postActionSpecPrimary", name = "Trigger with Animation(s)", description = "Comma-separated list of animation IDs that trigger post-action for primary special attack", position = 8, section = specialAttackPrimary)
    default String postActionSpecPrimary() {
        return "";
    }

    @ConfigSection(name = "Special Attack: Secondary", description = "Secondary special attack configuration", position = 11, closedByDefault = true)
    String specialAttackSecondary = "specialAttackSecondary";

    @ConfigItem(keyName = "hotkeySpecialAttackSecondary", name = "Hotkey for Special Attack", description = "Hotkey for special attack gear swap (secondary)", position = 1, section = specialAttackSecondary)
    default Keybind hotkeySpecialAttackSecondary() {
        return Keybind.NOT_SET;
    }

    @ConfigItem(keyName = "gearIDsSpecialAttackSecondary", name = "Gear IDs", description = "Comma-separated list of Gear IDs for special attack (secondary)", position = 2, section = specialAttackSecondary)
    default String gearIDsSpecialAttackSecondary() {
        return "";
    }

    @ConfigItem(keyName = "specPrayerStyleSecondary", name = "Prayer Style", description = "Prayer to activate after using special attack (secondary)", position = 3, section = specialAttackSecondary)
    default PrayerStyle specPrayerStyleSecondary() {
        return PrayerStyle.MELEE;
    }

    @ConfigItem(keyName = "specTypeSecondary", name = "Spec Type", description = "Single or Double spec for special attack (secondary)", position = 4, section = specialAttackSecondary)
    default SpecType specTypeSecondary() {
        return SpecType.SINGLE;
    }

    @ConfigItem(keyName = "attackTargetSpecSecondary", name = "Attack Target", description = "Attack target after special attack gear swap (secondary)", position = 5, section = specialAttackSecondary)
    default boolean attackTargetSpecSecondary() {
        return false;
    }

    @ConfigItem(keyName = "specEnergySecondary", name = "Spec Energy Consumed", description = "Spec energy used per attempt for special attack (secondary)", position = 6, section = specialAttackSecondary)
    default int specEnergySecondary() {
        return 50;
    }

    @ConfigItem(keyName = "useVengeanceSpecSecondary", name = "Use Vengeance", description = "If enabled, casts Vengeance during special attack (secondary)", position = 7, section = specialAttackSecondary)
    default boolean useVengeanceSpecSecondary() {
        return false;
    }

    @ConfigItem(keyName = "postActionSpecSecondary", name = "Trigger with Animation(s)", description = "Comma-separated list of animation IDs that trigger post-action for secondary special attack", position = 8, section = specialAttackSecondary)
    default String postActionSpecSecondary() {
        return "";
    }

    @ConfigSection(name = "Special Attack: Tertiary", description = "Tertiary special attack configuration", position = 12, closedByDefault = true)
    String specialAttackTertiary = "specialAttackTertiary";

    @ConfigItem(keyName = "hotkeySpecialAttackTertiary", name = "Hotkey for Special Attack", description = "Hotkey for special attack gear swap (tertiary)", position = 1, section = specialAttackTertiary)
    default Keybind hotkeySpecialAttackTertiary() {
        return Keybind.NOT_SET;
    }

    @ConfigItem(keyName = "gearIDsSpecialAttackTertiary", name = "Gear IDs", description = "Comma-separated list of Gear IDs for special attack (tertiary)", position = 2, section = specialAttackTertiary)
    default String gearIDsSpecialAttackTertiary() {
        return "";
    }

    @ConfigItem(keyName = "specPrayerStyleTertiary", name = "Prayer Style", description = "Prayer to activate after using special attack (tertiary)", position = 3, section = specialAttackTertiary)
    default PrayerStyle specPrayerStyleTertiary() {
        return PrayerStyle.MELEE;
    }

    @ConfigItem(keyName = "specTypeTertiary", name = "Spec Type", description = "Single or Double spec for special attack (tertiary)", position = 4, section = specialAttackTertiary)
    default SpecType specTypeTertiary() {
        return SpecType.SINGLE;
    }

    @ConfigItem(keyName = "attackTargetSpecTertiary", name = "Attack Target", description = "Attack target after special attack gear swap (tertiary)", position = 5, section = specialAttackTertiary)
    default boolean attackTargetSpecTertiary() {
        return false;
    }

    @ConfigItem(keyName = "specEnergyTertiary", name = "Spec Energy Consumed", description = "Spec energy used per attempt for special attack (tertiary)", position = 6, section = specialAttackTertiary)
    default int specEnergyTertiary() {
        return 50;
    }

    @ConfigItem(keyName = "useVengeanceSpecTertiary", name = "Use Vengeance", description = "If enabled, casts Vengeance during special attack (tertiary)", position = 7, section = specialAttackTertiary)
    default boolean useVengeanceSpecTertiary() {
        return false;
    }

    @ConfigItem(keyName = "postActionSpecTertiary", name = "Trigger with Animation(s)", description = "Comma-separated list of animation IDs that trigger post-action for tertiary special attack", position = 8, section = specialAttackTertiary)
    default String postActionSpecTertiary() {
        return "";
    }

    @ConfigSection(name = "Tank", description = "Tank configuration", position = 13, closedByDefault = true)
    String tankSection = "tankSection";

    @ConfigItem(keyName = "hotkeyTank", name = "Hotkey for Tank Gear", description = "Optional hotkey to manually trigger tank gear swap. Tanking is normally triggered by animation events.", position = 1, section = tankSection)
    default Keybind hotkeyTank() {
        return Keybind.NOT_SET;
    }

    @ConfigItem(keyName = "gearIDsTank", name = "Tank Gear IDs", description = "Comma-separated list of Gear IDs for tank mode", position = 2, section = tankSection)
    default String gearIDsTank() {
        return "";
    }

    @ConfigItem(keyName = "tankAnimations", name = "Trigger On Animation(s)", description = "Comma-separated list of animation IDs that trigger a tank swap.", position = 3, section = tankSection)
    default String tankAnimations() {
        return "";
    }

    @ConfigSection(name = "Prayers", description = "Prayer hotkeys", position = 14, closedByDefault = true)
    String prayerSection = "prayerSection";

    @ConfigItem(keyName = "protectFromMagic", name = "Protect from Magic", description = "Protect from Magic keybind", position = 0, section = prayerSection)
    default Keybind protectFromMagic() {
        return Keybind.NOT_SET;
    }

    @ConfigItem(keyName = "protectFromMissles", name = "Protect from Missiles", description = "Protect from Missiles keybind", position = 1, section = prayerSection)
    default Keybind protectFromMissles() {
        return Keybind.NOT_SET;
    }

    @ConfigItem(keyName = "protectFromMelee", name = "Protect from Melee", description = "Protect from Melee keybind", position = 2, section = prayerSection)
    default Keybind protectFromMelee() {
        return Keybind.NOT_SET;
    }

    @ConfigItem(keyName = "enableOffensiveSpells", name = "Enable Offensive Spells", description = "If enabled, offensive spell toggling becomes available.", position = 4, section = prayerSection)
    default boolean enableOffensiveSpells() {
        return false;
    }

    @ConfigItem(keyName = "hasRigour", name = "Rigour Unlocked", description = "If you have Rigour unlocked", position = 5, section = prayerSection)
    default boolean hasRigour() {
        return false;
    }

    @ConfigItem(keyName = "hasAugury", name = "Augury Unlocked", description = "If you have Augury unlocked", position = 6, section = prayerSection)
    default boolean hasAugury() {
        return false;
    }

    @ConfigItem(keyName = "hasPiety", name = "Piety Unlocked", description = "If you have Piety unlocked", position = 7, section = prayerSection)
    default boolean hasPiety() {
        return false;
    }

    @ConfigSection(name = "Potions", description = "Potion usage", position = 15, closedByDefault = true)
    String potionsSection = "potionsSection";

    @ConfigItem(keyName = "restoreHotkey", name = "Restore Hotkey", description = "Hotkey to use a super restore potion.", position = 3, section = potionsSection)
    default Keybind restoreHotkey() {
        return Keybind.NOT_SET;
    }

    @ConfigItem(keyName = "brewHotkey", name = "Brew Hotkey", description = "Hotkey to use a Saradomin brew potion.", position = 4, section = potionsSection)
    default Keybind brewHotkey() {
        return Keybind.NOT_SET;
    }

    @ConfigItem(keyName = "combatPotionHotkey", name = "Combat Potion Hotkey", description = "Hotkey to use a combat potion.", position = 5, section = potionsSection)
    default Keybind combatPotionHotkey() {
        return Keybind.NOT_SET;
    }

    @ConfigItem(keyName = "rangePotionHotkey", name = "Range Potion Hotkey", description = "Hotkey to use a range potion.", position = 6, section = potionsSection)
    default Keybind rangePotionHotkey() {
        return Keybind.NOT_SET;
    }

    @ConfigSection(name = "Food", description = "Food usage", position = 16, closedByDefault = true)
    String foodSection = "foodSection";

    @ConfigItem(keyName = "singleEatHotkey", name = "Single Eat Hotkey", description = "Hotkey to eat a single food item.", position = 1, section = foodSection)
    default Keybind singleEatHotkey() {
        return Keybind.NOT_SET;
    }

    @ConfigItem(keyName = "singleEatFood", name = "Single Eat Food", description = "Comma-separated list of food IDs or names for single eat.", position = 2, section = foodSection)
    default String singleEatFood() {
        return "";
    }

    @ConfigItem(keyName = "doubleEatHotkey", name = "Double Eat Hotkey", description = "Hotkey to perform a double-eat sequence.", position = 3, section = foodSection)
    default Keybind doubleEatHotkey() {
        return Keybind.NOT_SET;
    }

    @ConfigItem(keyName = "doubleEatFood", name = "Double Eat Food", description = "Comma-separated list of food IDs or names for double eat.", position = 4, section = foodSection)
    default String doubleEatFood() {
        return "";
    }

    @ConfigItem(keyName = "tripleEatHotkey", name = "Triple Eat Hotkey", description = "Hotkey to perform a triple-eat sequence (with brew in between).", position = 5, section = foodSection)
    default Keybind tripleEatHotkey() {
        return Keybind.NOT_SET;
    }

    @ConfigItem(keyName = "tripleEatFood", name = "Triple Eat Food", description = "Comma-separated list of food IDs or names for triple eat.", position = 6, section = foodSection)
    default String tripleEatFood() {
        return "";
    }

    @ConfigSection(name = "User Interface", description = "User interface controls", position = 17, closedByDefault = true)
    String userInterfaceSection = "userInterfaceSection";

    @ConfigItem(keyName = "clearTargetHotkey", name = "Clear Target Hotkey", description = "Hotkey used to clear the target.", position = 0, section = userInterfaceSection)
    default Keybind clearTargetHotkey() {
        return Keybind.NOT_SET;
    }

    @Alpha
    @ConfigItem(keyName = "targetTileColor", name = "Target Tile Color", description = "Color used to render the target tile highlight.", position = 1, section = userInterfaceSection)
    default Color targetTileColor() {
        return Color.RED;
    }

    @ConfigSection(name = "Settings", description = "Additional settings", position = 18, closedByDefault = true)
    String settingsSection = "settingsSection";

    @ConfigItem(keyName = "walkUnderHotkey", name = "Walk Under Hotkey", description = "Hotkey used to perform the 'Walk Under' action.", position = 0, section = settingsSection)
    default Keybind walkUnderHotkey() {
        return Keybind.NOT_SET;
    }
}