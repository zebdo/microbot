package net.runelite.client.plugins.microbot.util.prayer;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.microbot.MicrobotConfig;

@Singleton
public class PrayerHotkeyAssignments
{
        public static final int SLOT_COUNT = 5;
        public static final String SLOT_KEY_PREFIX = "prayerHotkeySlot";

        private final ConfigManager configManager;
        private final PrayerHotkeyOption[] slots = new PrayerHotkeyOption[SLOT_COUNT];

        @Inject
        PrayerHotkeyAssignments(ConfigManager configManager)
        {
                this.configManager = configManager;
                Arrays.fill(slots, PrayerHotkeyOption.NONE);
                reload();
        }

        public void reload()
        {
                for (int i = 0; i < SLOT_COUNT; i++)
                {
                        slots[i] = readSlot(i);
                }
        }

        public PrayerHotkeyOption getSlot(int index)
        {
                if (!isValidIndex(index))
                {
                        return PrayerHotkeyOption.NONE;
                }

                return slots[index];
        }

        public void setSlot(int index, PrayerHotkeyOption option)
        {
                if (!isValidIndex(index))
                {
                        return;
                }

                PrayerHotkeyOption value = option == null ? PrayerHotkeyOption.NONE : option;
                slots[index] = value;

                if (value == PrayerHotkeyOption.NONE)
                {
                        configManager.unsetConfiguration(MicrobotConfig.configGroup, getKey(index));
                }
                else
                {
                        configManager.setConfiguration(MicrobotConfig.configGroup, getKey(index), value.name());
                }
        }

        public void clearSlot(int index)
        {
                setSlot(index, PrayerHotkeyOption.NONE);
        }

        private PrayerHotkeyOption readSlot(int index)
        {
                String stored = configManager.getConfiguration(MicrobotConfig.configGroup, getKey(index));
                if (stored == null || stored.isEmpty())
                {
                        return PrayerHotkeyOption.NONE;
                }

                try
                {
                        return PrayerHotkeyOption.valueOf(stored);
                }
                catch (IllegalArgumentException ex)
                {
                        return PrayerHotkeyOption.NONE;
                }
        }

        public static String getKey(int index)
        {
                return SLOT_KEY_PREFIX + (index + 1);
        }

        private boolean isValidIndex(int index)
        {
                return index >= 0 && index < SLOT_COUNT;
        }
}
