package net.runelite.client.plugins.microbot.kaas.pyrefox;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;

public class PyreFoxConstants
{
	public static final WorldPoint PYRE_FOX_CENTER_POINT = new WorldPoint(1617, 2999, 0);
	public static WorldPoint TRAP_OBJECT_POINT = null;
	public static volatile int GATHER_LOGS_AT_AMOUNT = 0;
	public static volatile int GATHER_LOGS_AMOUNT = 0;

	// 19215, 19217, 50726
	public static final int GAMEOBJECT_ROCK_NO_TRAP = 19215;
	public static final int GAMEOBJECT_ROCK_TRAP = 19217;
	public static final int GAMEOBJECT_ROCK_FOX_CAUGHT = 50726;


	/**
	 * Sets the gather logs at amount to something new,
	 * prevents always getting logs at 0 logs or w/e.
	 */
	public static void rerollGatherAmounts()
	{
		Microbot.log("Rerolling gathering amounts...");
		GATHER_LOGS_AT_AMOUNT = Rs2Random.between(0, 3);
		GATHER_LOGS_AMOUNT = Rs2Random.between(
			Math.max(Rs2Inventory.getEmptySlots()-10, 0),
			Math.min(Rs2Inventory.getEmptySlots(), 13)
		);
		Microbot.log("Rerolled");
	}
}
