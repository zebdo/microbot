package net.runelite.client.plugins.microbot.agility.courses;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;
import static net.runelite.api.NullObjectID.NULL_36241;
import static net.runelite.api.NullObjectID.NULL_36242;
import static net.runelite.api.NullObjectID.NULL_36243;
import static net.runelite.api.NullObjectID.NULL_36244;
import static net.runelite.api.NullObjectID.NULL_36245;
import static net.runelite.api.NullObjectID.NULL_36246;
import static net.runelite.api.ObjectID.CHIMNEY_36227;
import static net.runelite.api.ObjectID.DARK_HOLE_36229;
import static net.runelite.api.ObjectID.DARK_HOLE_36238;
import static net.runelite.api.ObjectID.LADDER_36221;
import static net.runelite.api.ObjectID.LADDER_36231;
import static net.runelite.api.ObjectID.LADDER_36232;
import static net.runelite.api.ObjectID.ROOF_EDGE;
import static net.runelite.api.ObjectID.ROPE_BRIDGE_36233;
import static net.runelite.api.ObjectID.ROPE_BRIDGE_36235;
import static net.runelite.api.ObjectID.TIGHTROPE_36225;
import static net.runelite.api.ObjectID.TIGHTROPE_36234;
import static net.runelite.api.ObjectID.TIGHTROPE_36236;
import static net.runelite.api.ObjectID.TIGHTROPE_36237;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.agility.models.AgilityObstacleModel;
import net.runelite.client.plugins.microbot.util.Global;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.misc.Operation;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

public class PrifddinasCourse implements AgilityCourseHandler
{
	private final Set<Integer> PORTAL_OBSTACLE_IDS = ImmutableSet.of(
		NULL_36241, NULL_36242, NULL_36243, NULL_36244, NULL_36245, NULL_36246
	);

	private final WorldArea prifFallArea = new WorldArea(3260, 6103, 15, 9, 0);

	@Override
	public WorldPoint getStartPoint()
	{
		return new WorldPoint(3253, 6109, 0);
	}

	@Override
	public List<AgilityObstacleModel> getObstacles()
	{
		return List.of(
			new AgilityObstacleModel(LADDER_36221, 3253, -1, Operation.LESS_EQUAL, Operation.GREATER), // before 3253,6109, after 3255,6109
			new AgilityObstacleModel(TIGHTROPE_36225, 3258, -1, Operation.LESS_EQUAL, Operation.GREATER), // before 3255,6109 , after 3272, 6105
			new AgilityObstacleModel(CHIMNEY_36227, -1, 6106, Operation.GREATER, Operation.LESS_EQUAL), // before 3273,6105 , after 3269,6112
			new AgilityObstacleModel(ROOF_EDGE, -1, 6115, Operation.GREATER, Operation.LESS_EQUAL), // before 3269,6112 , after 3269,6117
			new AgilityObstacleModel(DARK_HOLE_36229, 3267, 6117, Operation.GREATER, Operation.LESS_EQUAL), // before 3269,6117  , after 2269,3389
			new AgilityObstacleModel(LADDER_36231, -1, 3392, Operation.GREATER, Operation.LESS_EQUAL), // before 2269,3389 , after 2269,3393
			new AgilityObstacleModel(ROPE_BRIDGE_36233, 2265, -1, Operation.GREATER_EQUAL, Operation.GREATER), // before 2269,3393 , after 2257,3390
			new AgilityObstacleModel(TIGHTROPE_36234, 2254, -1, Operation.GREATER_EQUAL, Operation.GREATER), // before 2257,3390, after 2247,3397
			new AgilityObstacleModel(LADDER_36232,-1,6140,Operation.GREATER,Operation.GREATER), // if fail TIGHTROPE_36234 , before 3274,6147, after
			new AgilityObstacleModel(ROPE_BRIDGE_36235, -1, 3398, Operation.GREATER, Operation.LESS_EQUAL), // before 2247,3397, after 2246,3406
			new AgilityObstacleModel(TIGHTROPE_36236, -1, 3409, Operation.GREATER, Operation.LESS_EQUAL), // before 2246,3406, after 2250,3416
			new AgilityObstacleModel(TIGHTROPE_36237,-1, 3419, Operation.GREATER, Operation.LESS_EQUAL), // before 2250,3416 , after 2260 3425 //2253,3417 portal spawn
			new AgilityObstacleModel(DARK_HOLE_36238, -1, 3431, Operation.GREATER, Operation.LESS_EQUAL) // before 2260,3425 , after 3240,6109
		);
	}

	public boolean handlePortal() {
		TileObject portal = Rs2GameObject.findObject(PORTAL_OBSTACLE_IDS.toArray(new Integer[0]));
		if (portal != null && Microbot.getClientThread().runOnClientThreadOptional(portal::getClickbox).isPresent())
		{
			if (Rs2GameObject.interact(portal, "travel"))
			{
				Global.sleep(2000, 3000);
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean handleWalkToStart(WorldPoint playerWorldLocation, LocalPoint playerLocalLocation) {
		if (Microbot.getClient().getTopLevelWorldView().getPlane() != 0) return false;

		if (prifFallArea.contains(playerWorldLocation)) {
			Rs2Walker.walkTo(getStartPoint(), 8);
			Microbot.log("Going back to course's starting point");
			return true;
		}
		return false;
	}
}
