package net.runelite.client.plugins.microbot.moonsOfPeril.handlers;

import net.runelite.client.plugins.microbot.moonsOfPeril.enums.State;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;

public class IdleHandler implements BaseHandler {

    @Override
    public boolean validate() {
        Rs2Prayer.disableAllPrayers();
        BossHandler.eatIfNeeded(70);
        BossHandler.drinkIfNeeded(70);
        return false;
    }

    @Override
    public State execute() {
        return null;
    }
}
