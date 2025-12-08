package net.runelite.client.plugins.microbot.util.sailing;

import net.runelite.api.WorldEntity;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.plugins.microbot.api.boat.Rs2Boat;
import net.runelite.client.plugins.microbot.util.sailing.data.BoatType;
import net.runelite.client.plugins.microbot.util.sailing.data.Heading;
import net.runelite.client.plugins.microbot.util.sailing.data.PortTaskData;
import net.runelite.client.plugins.microbot.util.sailing.data.PortTaskVarbits;

import java.util.Map;

/**
 * Legacy facade for sailing helpers. Prefer calling {@link Rs2Boat} directly.
 */
@Deprecated
public class Rs2Sailing
{
	private Rs2Sailing()
	{
	}

	public static void handleChatMessage(ChatMessage event)
	{
		Rs2Boat.handleChatMessage(event);
	}

	public static BoatType getBoatType()
	{
		return Rs2Boat.getBoatType();
	}

	public static int getSteeringForBoatType()
	{
		return Rs2Boat.getSteeringForBoatType();
	}

	public static WorldEntity getBoat()
	{
		return Rs2Boat.getBoat();
	}

	public static boolean isNavigating()
	{
		return Rs2Boat.isNavigating();
	}

	public static boolean navigate()
	{
		return Rs2Boat.navigate();
	}

	public static boolean isOnBoat()
	{
		return Rs2Boat.isOnBoat();
	}

	public static WorldPoint getPlayerBoatLocation()
	{
		return Rs2Boat.getPlayerBoatLocation();
	}

	public static boolean boardBoat()
	{
		return Rs2Boat.boardBoat();
	}

	public static boolean disembarkBoat()
	{
		return Rs2Boat.disembarkBoat();
	}

	public static boolean isMovingForward()
	{
		return Rs2Boat.isMovingForward();
	}

	public static boolean isMovingBackward()
	{
		return Rs2Boat.isMovingBackward();
	}

	public static boolean isStandingStill()
	{
		return Rs2Boat.isStandingStill();
	}

	public static boolean clickSailButton()
	{
		return Rs2Boat.clickSailButton();
	}

	public static void setSails()
	{
		Rs2Boat.setSails();
	}

	public static void unsetSails()
	{
		Rs2Boat.unsetSails();
	}

	public static void sailTo(WorldPoint target)
	{
		Rs2Boat.sailTo(target);
	}

	public static int getDirection(WorldPoint target)
	{
		return Rs2Boat.getDirection(target);
	}

	public static void setHeading(Heading heading)
	{
		Rs2Boat.setHeading(heading);
	}

	public static boolean trimSails()
	{
		return Rs2Boat.trimSails();
	}

	public static boolean openCargo()
	{
		return Rs2Boat.openCargo();
	}

	public static Map<PortTaskVarbits, Integer> getPortTasksVarbits()
	{
		return Rs2Boat.getPortTasksVarbits();
	}

	public static PortTaskData getPortTaskData(int varbitValue)
	{
		return Rs2Boat.getPortTaskData(varbitValue);
	}
}
