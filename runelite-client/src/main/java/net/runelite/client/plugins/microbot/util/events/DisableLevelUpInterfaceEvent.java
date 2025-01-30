package net.runelite.client.plugins.microbot.util.events;

import net.runelite.api.annotations.Component;
import net.runelite.client.plugins.microbot.BlockingEvent;
import net.runelite.client.plugins.microbot.BlockingEventPriority;
import net.runelite.client.plugins.microbot.util.settings.Rs2Settings;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

public class DisableLevelUpInterfaceEvent implements BlockingEvent {
    /**
     * Checking the Report button will ensure that we are logged in, as there seems to be a small moment in time
     * when at the welcome screen that Rs2Settings.isLevelUpNotificationsEnabled() will return true then turn back to false
     * even if {@code Varbits.DISABLE_LEVEL_UP_INTERFACE} is 1 after clicking play button
     */
    @Component
    private final int REPORT_BUTTON_COMPONENT_ID = 10616833;
    
    @Override
    public boolean validate() {
        if (!Rs2Widget.isWidgetVisible(REPORT_BUTTON_COMPONENT_ID)) return false;
        return Rs2Settings.isLevelUpNotificationsEnabled();
    }

    @Override
    public boolean execute() {
        return Rs2Settings.disableLevelUpNotifications();
    }

    @Override
    public BlockingEventPriority priority() {
        return BlockingEventPriority.HIGH;
    }
}
