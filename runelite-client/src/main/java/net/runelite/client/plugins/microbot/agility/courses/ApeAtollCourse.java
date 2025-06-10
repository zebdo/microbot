package net.runelite.client.plugins.microbot.agility.courses;

import java.util.List;
import static net.runelite.api.ObjectID.MONKEYBARS_15417;
import static net.runelite.api.ObjectID.ROPE_15487;
import static net.runelite.api.ObjectID.SKULL_SLOPE;
import static net.runelite.api.ObjectID.STEPPING_STONE_15412;
import static net.runelite.api.ObjectID.TROPICAL_TREE_15414;
import static net.runelite.api.ObjectID.TROPICAL_TREE_16062;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.agility.models.AgilityObstacleModel;
import net.runelite.client.plugins.microbot.util.misc.Operation;

public class ApeAtollCourse implements AgilityCourseHandler
{
	@Override
	public WorldPoint getStartPoint()
	{
		return new WorldPoint(2754, 2742, 0);
	}

	@Override
	public List<AgilityObstacleModel> getObstacles()
	{
		return List.of(
			new AgilityObstacleModel(STEPPING_STONE_15412, 2755, 2742, Operation.GREATER_EQUAL, Operation.GREATER_EQUAL),
			new AgilityObstacleModel(TROPICAL_TREE_15414, -1, 2742, Operation.GREATER, Operation.GREATER_EQUAL),
			new AgilityObstacleModel(MONKEYBARS_15417),
			new AgilityObstacleModel(SKULL_SLOPE),
			new AgilityObstacleModel(ROPE_15487, 2752, -1, Operation.LESS_EQUAL, Operation.GREATER),
			new AgilityObstacleModel(TROPICAL_TREE_16062, 2756, -1, Operation.GREATER_EQUAL, Operation.GREATER)
		);
	}
}
