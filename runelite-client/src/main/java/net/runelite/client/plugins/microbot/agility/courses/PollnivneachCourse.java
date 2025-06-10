package net.runelite.client.plugins.microbot.agility.courses;

import java.util.List;
import static net.runelite.api.ObjectID.BANNER_14937;
import static net.runelite.api.ObjectID.BASKET_14935;
import static net.runelite.api.ObjectID.DRYING_LINE;
import static net.runelite.api.ObjectID.GAP_14938;
import static net.runelite.api.ObjectID.MARKET_STALL_14936;
import static net.runelite.api.ObjectID.MONKEYBARS;
import static net.runelite.api.ObjectID.ROUGH_WALL_14940;
import static net.runelite.api.ObjectID.TREE_14939;
import static net.runelite.api.ObjectID.TREE_14944;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.agility.models.AgilityObstacleModel;
import net.runelite.client.plugins.microbot.util.misc.Operation;

public class PollnivneachCourse implements AgilityCourseHandler
{
	@Override
	public WorldPoint getStartPoint()
	{
		return new WorldPoint(3351, 2961, 0);
	}

	@Override
	public List<AgilityObstacleModel> getObstacles()
	{
		return List.of(
			new AgilityObstacleModel(BASKET_14935),
			new AgilityObstacleModel(MARKET_STALL_14936, -1, 2968, Operation.GREATER, Operation.LESS_EQUAL),
			new AgilityObstacleModel(BANNER_14937, -1, 2976, Operation.GREATER, Operation.LESS_EQUAL),
			new AgilityObstacleModel(GAP_14938, 3362, -1, Operation.LESS_EQUAL, Operation.GREATER),
			new AgilityObstacleModel(TREE_14939, 3366, -1, Operation.LESS_EQUAL, Operation.GREATER),
			new AgilityObstacleModel(ROUGH_WALL_14940, -1, 2982, Operation.GREATER, Operation.GREATER_EQUAL),
			new AgilityObstacleModel(MONKEYBARS),
			new AgilityObstacleModel(TREE_14944, -1, 2996, Operation.GREATER, Operation.LESS_EQUAL),
			new AgilityObstacleModel(DRYING_LINE)
		);
	}
}
