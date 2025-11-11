package net.runelite.client.plugins.microbot.util.prayer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Prayer;
import net.runelite.api.gameval.SpriteID;
import net.runelite.client.util.Text;

@Getter
@RequiredArgsConstructor
public enum PrayerHotkeyOption
{
        NONE(null, null, -1, -1),

        THICK_SKIN(Rs2PrayerEnum.THICK_SKIN, Prayer.THICK_SKIN,
                SpriteID.Prayeron.THICK_SKIN, SpriteID.Prayeroff.THICK_SKIN_DISABLED),
        BURST_OF_STRENGTH(Rs2PrayerEnum.BURST_STRENGTH, Prayer.BURST_OF_STRENGTH,
                SpriteID.Prayeron.BURST_OF_STRENGTH, SpriteID.Prayeroff.BURST_OF_STRENGTH_DISABLED),
        CLARITY_OF_THOUGHT(Rs2PrayerEnum.CLARITY_THOUGHT, Prayer.CLARITY_OF_THOUGHT,
                SpriteID.Prayeron.CLARITY_OF_THOUGHT, SpriteID.Prayeroff.CLARITY_OF_THOUGHT_DISABLED),
        SHARP_EYE(Rs2PrayerEnum.SHARP_EYE, Prayer.SHARP_EYE,
                SpriteID.Prayeron.SHARP_EYE, SpriteID.Prayeroff.SHARP_EYE_DISABLED),
        MYSTIC_WILL(Rs2PrayerEnum.MYSTIC_WILL, Prayer.MYSTIC_WILL,
                SpriteID.Prayeron.MYSTIC_WILL, SpriteID.Prayeroff.MYSTIC_WILL_DISABLED),
        ROCK_SKIN(Rs2PrayerEnum.ROCK_SKIN, Prayer.ROCK_SKIN,
                SpriteID.Prayeron.ROCK_SKIN, SpriteID.Prayeroff.ROCK_SKIN_DISABLED),
        SUPERHUMAN_STRENGTH(Rs2PrayerEnum.SUPERHUMAN_STRENGTH, Prayer.SUPERHUMAN_STRENGTH,
                SpriteID.Prayeron.SUPERHUMAN_STRENGTH, SpriteID.Prayeroff.SUPERHUMAN_STRENGTH_DISABLED),
        IMPROVED_REFLEXES(Rs2PrayerEnum.IMPROVED_REFLEXES, Prayer.IMPROVED_REFLEXES,
                SpriteID.Prayeron.IMPROVED_REFLEXES, SpriteID.Prayeroff.IMPROVED_REFLEXES_DISABLED),
        RAPID_RESTORE(Rs2PrayerEnum.RAPID_RESTORE, Prayer.RAPID_RESTORE,
                SpriteID.Prayeron.RAPID_RESTORE, SpriteID.Prayeroff.RAPID_RESTORE_DISABLED),
        RAPID_HEAL(Rs2PrayerEnum.RAPID_HEAL, Prayer.RAPID_HEAL,
                SpriteID.Prayeron.RAPID_HEAL, SpriteID.Prayeroff.RAPID_HEAL_DISABLED),
        PROTECT_ITEM(Rs2PrayerEnum.PROTECT_ITEM, Prayer.PROTECT_ITEM,
                SpriteID.Prayeron.PROTECT_ITEM, SpriteID.Prayeroff.PROTECT_ITEM_DISABLED),
        HAWK_EYE(Rs2PrayerEnum.HAWK_EYE, Prayer.HAWK_EYE,
                SpriteID.Prayeron.HAWK_EYE, SpriteID.Prayeroff.HAWK_EYE_DISABLED),
        MYSTIC_LORE(Rs2PrayerEnum.MYSTIC_LORE, Prayer.MYSTIC_LORE,
                SpriteID.Prayeron.MYSTIC_LORE, SpriteID.Prayeroff.MYSTIC_LORE_DISABLED),
        STEEL_SKIN(Rs2PrayerEnum.STEEL_SKIN, Prayer.STEEL_SKIN,
                SpriteID.Prayeron.STEEL_SKIN, SpriteID.Prayeroff.STEEL_SKIN_DISABLED),
        ULTIMATE_STRENGTH(Rs2PrayerEnum.ULTIMATE_STRENGTH, Prayer.ULTIMATE_STRENGTH,
                SpriteID.Prayeron.ULTIMATE_STRENGTH, SpriteID.Prayeroff.ULTIMATE_STRENGTH_DISABLED),
        INCREDIBLE_REFLEXES(Rs2PrayerEnum.INCREDIBLE_REFLEXES, Prayer.INCREDIBLE_REFLEXES,
                SpriteID.Prayeron.INCREDIBLE_REFLEXES, SpriteID.Prayeroff.INCREDIBLE_REFLEXES_DISABLED),
        PROTECT_FROM_MAGIC(Rs2PrayerEnum.PROTECT_MAGIC, Prayer.PROTECT_FROM_MAGIC,
                SpriteID.Prayeron.PROTECT_FROM_MAGIC, SpriteID.Prayeroff.PROTECT_FROM_MAGIC_DISABLED),
        PROTECT_FROM_MISSILES(Rs2PrayerEnum.PROTECT_RANGE, Prayer.PROTECT_FROM_MISSILES,
                SpriteID.Prayeron.PROTECT_FROM_MISSILES, SpriteID.Prayeroff.PROTECT_FROM_MISSILES_DISABLED),
        PROTECT_FROM_MELEE(Rs2PrayerEnum.PROTECT_MELEE, Prayer.PROTECT_FROM_MELEE,
                SpriteID.Prayeron.PROTECT_FROM_MELEE, SpriteID.Prayeroff.PROTECT_FROM_MELEE_DISABLED),
        EAGLE_EYE(Rs2PrayerEnum.EAGLE_EYE, Prayer.EAGLE_EYE,
                SpriteID.Prayeron.EAGLE_EYE, SpriteID.Prayeroff.EAGLE_EYE_DISABLED),
        DEAD_EYE(Rs2PrayerEnum.DEAD_EYE, Prayer.DEADEYE,
                SpriteID.Prayeron.DEADEYE, SpriteID.Prayeroff.DEADEYE_DISABLED),
        MYSTIC_MIGHT(Rs2PrayerEnum.MYSTIC_MIGHT, Prayer.MYSTIC_MIGHT,
                SpriteID.Prayeron.MYSTIC_MIGHT, SpriteID.Prayeroff.MYSTIC_MIGHT_DISABLED),
        MYSTIC_VIGOUR(Rs2PrayerEnum.MYSTIC_VIGOUR, Prayer.MYSTIC_VIGOUR,
                SpriteID.Prayeron.MYSTIC_VIGOUR, SpriteID.Prayeroff.MYSTIC_VIGOUR_DISABLED),
        RETRIBUTION(Rs2PrayerEnum.RETRIBUTION, Prayer.RETRIBUTION,
                SpriteID.Prayeron.RETRIBUTION, SpriteID.Prayeroff.RETRIBUTION_DISABLED),
        REDEMPTION(Rs2PrayerEnum.REDEMPTION, Prayer.REDEMPTION,
                SpriteID.Prayeron.REDEMPTION, SpriteID.Prayeroff.REDEMPTION_DISABLED),
        SMITE(Rs2PrayerEnum.SMITE, Prayer.SMITE,
                SpriteID.Prayeron.SMITE, SpriteID.Prayeroff.SMITE_DISABLED),
        PRESERVE(Rs2PrayerEnum.PRESERVE, Prayer.PRESERVE,
                SpriteID.Prayeron.PRESERVE, SpriteID.Prayeroff.PRESERVE_DISABLED),
        CHIVALRY(Rs2PrayerEnum.CHIVALRY, Prayer.CHIVALRY,
                SpriteID.Prayeron.CHIVALRY, SpriteID.Prayeroff.CHIVALRY_DISABLED),
        PIETY(Rs2PrayerEnum.PIETY, Prayer.PIETY,
                SpriteID.Prayeron.PIETY, SpriteID.Prayeroff.PIETY_DISABLED),
        RIGOUR(Rs2PrayerEnum.RIGOUR, Prayer.RIGOUR,
                SpriteID.Prayeron.RIGOUR, SpriteID.Prayeroff.RIGOUR_DISABLED),
        AUGURY(Rs2PrayerEnum.AUGURY, Prayer.AUGURY,
                SpriteID.Prayeron.AUGURY, SpriteID.Prayeroff.AUGURY_DISABLED);

        private final Rs2PrayerEnum prayerEnum;
        private final Prayer prayer;
        private final int activeSpriteId;
        private final int inactiveSpriteId;

        public int getSpriteId(boolean active)
        {
                return active ? activeSpriteId : inactiveSpriteId;
        }

        public String getDisplayName()
        {
                if (this == NONE)
                {
                        return "Empty";
                }

                if (prayer != null)
                {
                        return Text.titleCase(prayer);
                }

                return Text.titleCase(this);
        }

        @Override
        public String toString()
        {
                return getDisplayName();
        }
}
