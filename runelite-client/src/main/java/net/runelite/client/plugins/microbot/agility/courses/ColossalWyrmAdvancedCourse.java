package net.runelite.client.plugins.microbot.agility.courses;

import java.util.List;
import static net.runelite.api.ObjectID.EDGE_55192;
import static net.runelite.api.ObjectID.LADDER_55178;
import static net.runelite.api.ObjectID.LADDER_55191;
import static net.runelite.api.ObjectID.TIGHTROPE_55180;
import static net.runelite.api.ObjectID.TIGHTROPE_55194;
import static net.runelite.api.ObjectID.ZIPLINE_55179;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.agility.models.AgilityObstacleModel;
import net.runelite.client.plugins.microbot.util.misc.Operation;

public class ColossalWyrmAdvancedCourse implements AgilityCourseHandler
{
	@Override
	public WorldPoint getStartPoint()
	{
		return new WorldPoint(1652, 2931, 0);
	}

	@Override
	public List<AgilityObstacleModel> getObstacles()
	{
		return List.of(
			new AgilityObstacleModel(LADDER_55178),
			new AgilityObstacleModel(TIGHTROPE_55180, -1, 2926, Operation.GREATER, Operation.GREATER_EQUAL),
			new AgilityObstacleModel(LADDER_55191, -1, 2911, Operation.GREATER, Operation.LESS_EQUAL),
			new AgilityObstacleModel(EDGE_55192, 1647, -1, Operation.GREATER_EQUAL, Operation.GREATER_EQUAL),
			new AgilityObstacleModel(TIGHTROPE_55194),
			new AgilityObstacleModel(ZIPLINE_55179)
		);
	}
}
