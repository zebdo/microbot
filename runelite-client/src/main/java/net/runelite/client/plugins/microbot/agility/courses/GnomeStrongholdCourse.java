package net.runelite.client.plugins.microbot.agility.courses;

import java.util.List;
import static net.runelite.api.ObjectID.BALANCING_ROPE_23557;
import static net.runelite.api.ObjectID.LOG_BALANCE_23145;
import static net.runelite.api.ObjectID.OBSTACLE_NET_23134;
import static net.runelite.api.ObjectID.OBSTACLE_NET_23135;
import static net.runelite.api.ObjectID.OBSTACLE_PIPE_23138;
import static net.runelite.api.ObjectID.TREE_BRANCH_23559;
import static net.runelite.api.ObjectID.TREE_BRANCH_23560;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.agility.models.AgilityObstacleModel;
import net.runelite.client.plugins.microbot.util.misc.Operation;

public class GnomeStrongholdCourse implements AgilityCourseHandler
{
	@Override
	public WorldPoint getStartPoint()
	{
		return new WorldPoint(2474, 3436, 0);
	}

	@Override
	public List<AgilityObstacleModel> getObstacles()
	{
		return List.of(
			new AgilityObstacleModel(LOG_BALANCE_23145, -1, 3436, Operation.GREATER, Operation.GREATER_EQUAL),
			new AgilityObstacleModel(OBSTACLE_NET_23134, 2476, 3426, Operation.LESS_EQUAL, Operation.GREATER_EQUAL),
			new AgilityObstacleModel(TREE_BRANCH_23559),
			new AgilityObstacleModel(BALANCING_ROPE_23557, 2477, -1, Operation.LESS_EQUAL, Operation.GREATER),
			new AgilityObstacleModel(TREE_BRANCH_23560),
			new AgilityObstacleModel(OBSTACLE_NET_23135, 2483, 3425, Operation.GREATER_EQUAL, Operation.LESS_EQUAL),
			new AgilityObstacleModel(OBSTACLE_PIPE_23138, -1, 3430, Operation.GREATER, Operation.LESS_EQUAL)
		);
	}
}
