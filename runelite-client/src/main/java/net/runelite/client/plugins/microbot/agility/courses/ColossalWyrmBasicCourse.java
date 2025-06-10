package net.runelite.client.plugins.microbot.agility.courses;

import java.util.List;
import static net.runelite.api.ObjectID.LADDER_55178;
import static net.runelite.api.ObjectID.LADDER_55190;
import static net.runelite.api.ObjectID.ROPE_55186;
import static net.runelite.api.ObjectID.TIGHTROPE_55180;
import static net.runelite.api.ObjectID.TIGHTROPE_55184;
import static net.runelite.api.ObjectID.ZIPLINE_55179;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.agility.models.AgilityObstacleModel;
import net.runelite.client.plugins.microbot.util.misc.Operation;

public class ColossalWyrmBasicCourse implements AgilityCourseHandler
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
			new AgilityObstacleModel(TIGHTROPE_55184, 1647, -1, Operation.GREATER_EQUAL, Operation.GREATER_EQUAL),
			new AgilityObstacleModel(ROPE_55186, 1635, -1, Operation.LESS_EQUAL, Operation.GREATER_EQUAL),
			new AgilityObstacleModel(LADDER_55190, 1628, -1, Operation.LESS_EQUAL, Operation.GREATER),
			new AgilityObstacleModel(ZIPLINE_55179)
		);
	}
}
