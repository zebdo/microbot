package net.runelite.client.plugins.microbot.agility.courses;

import java.util.List;
import static net.runelite.api.ObjectID.EDGE_14925;
import static net.runelite.api.ObjectID.GAP_14903;
import static net.runelite.api.ObjectID.GAP_14904;
import static net.runelite.api.ObjectID.GAP_14919;
import static net.runelite.api.ObjectID.HAND_HOLDS_14901;
import static net.runelite.api.ObjectID.LEDGE_14920;
import static net.runelite.api.ObjectID.LEDGE_14921;
import static net.runelite.api.ObjectID.LEDGE_14922;
import static net.runelite.api.ObjectID.LEDGE_14924;
import static net.runelite.api.ObjectID.ROUGH_WALL_14898;
import static net.runelite.api.ObjectID.TIGHTROPE_14899;
import static net.runelite.api.ObjectID.TIGHTROPE_14905;
import static net.runelite.api.ObjectID.TIGHTROPE_14911;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.agility.models.AgilityObstacleModel;
import net.runelite.client.plugins.microbot.util.misc.Operation;

public class FaladorCourse implements AgilityCourseHandler
{
	@Override
	public WorldPoint getStartPoint()
	{
		return new WorldPoint(3036, 3341, 0);
	}

	@Override
	public List<AgilityObstacleModel> getObstacles()
	{
		return List.of(
			new AgilityObstacleModel(ROUGH_WALL_14898),
			new AgilityObstacleModel(TIGHTROPE_14899),
			new AgilityObstacleModel(HAND_HOLDS_14901),
			new AgilityObstacleModel(GAP_14903, -1, 3358, Operation.GREATER, Operation.LESS_EQUAL),
			new AgilityObstacleModel(GAP_14904, 3041, 3361, Operation.GREATER, Operation.GREATER_EQUAL),
			new AgilityObstacleModel(TIGHTROPE_14905),
			new AgilityObstacleModel(TIGHTROPE_14911),
			new AgilityObstacleModel(GAP_14919, -1, 3353, Operation.GREATER, Operation.GREATER_EQUAL),
			new AgilityObstacleModel(LEDGE_14920, 3016, -1, Operation.GREATER_EQUAL, Operation.GREATER),
			new AgilityObstacleModel(LEDGE_14921, -1, 3343, Operation.GREATER, Operation.GREATER_EQUAL),
			new AgilityObstacleModel(LEDGE_14922, -1, 3335, Operation.GREATER, Operation.GREATER_EQUAL),
			new AgilityObstacleModel(LEDGE_14924, 3017, -1, Operation.LESS, Operation.GREATER),
			new AgilityObstacleModel(EDGE_14925)
		);
	}
}
