package net.runelite.client.plugins.microbot.bradleycombat.handlers;

import net.runelite.api.Actor;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.bradleycombat.BradleyCombatPlugin;

public class MenuHandler {
    private Actor lastActor = null;

    @Subscribe
    public void onMenuOpened(MenuOpened event) {
        if (event.getFirstEntry() == null || event.getFirstEntry().getActor() == null)
            return;
        lastActor = event.getFirstEntry().getActor();
        Microbot.getClient().getMenu().createMenuEntry(-1)
                .setOption("Set Target")
                .setTarget(event.getFirstEntry().getTarget())
                .setType(MenuAction.RUNELITE);
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked ev) {
        String opt = ev.getMenuOption();
        if ("Set Target".equals(opt)) {
            if (lastActor != null) {
                if (BradleyCombatPlugin.getTarget() != null && BradleyCombatPlugin.getTarget().equals(lastActor))
                    BradleyCombatPlugin.setTarget(null);
                else
                    BradleyCombatPlugin.setTarget(lastActor);
            }
        } else if ("Follow".equals(opt) || "Attack".equals(opt)) {
            Actor a = ev.getMenuEntry().getActor();
            if (a != null)
                BradleyCombatPlugin.setTarget(a);
        }
    }
}