package net.runelite.client.plugins.microbot.agility.courses;

import java.util.List;
import static net.runelite.api.ObjectID.CLOTHES_LINE;
import static net.runelite.api.ObjectID.EDGE;
import static net.runelite.api.ObjectID.GAP_14414;
import static net.runelite.api.ObjectID.GAP_14833;
import static net.runelite.api.ObjectID.GAP_14834;
import static net.runelite.api.ObjectID.GAP_14835;
import static net.runelite.api.ObjectID.LEDGE_14836;
import static net.runelite.api.ObjectID.ROUGH_WALL_14412;
import static net.runelite.api.ObjectID.WALL_14832;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.agility.models.AgilityObstacleModel;
import net.runelite.client.plugins.microbot.util.misc.Operation;

public class VarrockCourse implements AgilityCourseHandler
{
	@Override
	public WorldPoint getStartPoint()
	{
		return new WorldPoint(3221, 3414, 0);
	}

	@Override
	public List<AgilityObstacleModel> getObstacles()
	{
		return List.of(
			new AgilityObstacleModel(ROUGH_WALL_14412),
			new AgilityObstacleModel(CLOTHES_LINE),
			new AgilityObstacleModel(GAP_14414),
			new AgilityObstacleModel(WALL_14832),
			new AgilityObstacleModel(GAP_14833), // this obstacle doesn't always work for some reason
			new AgilityObstacleModel(GAP_14834),
			new AgilityObstacleModel(GAP_14835, -1, 3402, Operation.GREATER, Operation.LESS_EQUAL),
			new AgilityObstacleModel(LEDGE_14836, -1, 3408, Operation.GREATER, Operation.LESS_EQUAL),
			new AgilityObstacleModel(EDGE)
		);
	}
}
