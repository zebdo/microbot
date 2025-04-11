package net.runelite.client.plugins.microbot.kittentracker;

import net.runelite.api.ItemID;
import net.runelite.client.plugins.microbot.BlockingEvent;
import net.runelite.client.plugins.microbot.BlockingEventPriority;
import net.runelite.client.plugins.microbot.util.Global;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;

import javax.inject.Inject;

public class KittenAttentionEvent implements BlockingEvent
{
    private final KittenPlugin kittenPlugin;
    @Inject
    public KittenAttentionEvent(KittenPlugin kittenPlugin)
    {
        this.kittenPlugin = kittenPlugin;
    }

    @Override
    public boolean validate()
    {
        return Rs2Inventory.contains(ItemID.BALL_OF_WOOL)
                && (KittenPlugin.ATTENTION_FIRST_WARNING_TIME_LEFT_IN_SECONDS * 1000) >= kittenPlugin.getTimeBeforeNeedingAttention();
    }

    @Override
    public boolean execute()
    {
        Rs2Npc.getNpcs("Kitten").findFirst().ifPresent(kitten -> Rs2Inventory.useItemOnNpc(ItemID.BALL_OF_WOOL, kitten));
        Global.sleepUntil(() -> (KittenPlugin.ATTENTION_FIRST_WARNING_TIME_LEFT_IN_SECONDS * 1000) < kittenPlugin.getTimeBeforeNeedingAttention(),10000);
        return true;
    }

    @Override
    public BlockingEventPriority priority()
    {
        return BlockingEventPriority.NORMAL;
    }
}
