package net.runelite.client.plugins.microbot.util.prayer;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PrayerHotkeyConfigAccess
{
        private static Runnable openSelectorAction;

        public void setOpenSelectorAction(Runnable action)
        {
                openSelectorAction = action;
        }

        public void clearOpenSelectorAction()
        {
                openSelectorAction = null;
        }

        public void openSelector()
        {
                if (openSelectorAction != null)
                {
                        openSelectorAction.run();
                }
        }
}
