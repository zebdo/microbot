package net.runelite.client.plugins.microbot.agility.courses;

import java.util.List;
import static net.runelite.api.ObjectID.CRATE_11632;
import static net.runelite.api.ObjectID.GAP_11631;
import static net.runelite.api.ObjectID.NARROW_WALL;
import static net.runelite.api.ObjectID.ROUGH_WALL;
import static net.runelite.api.ObjectID.TIGHTROPE;
import static net.runelite.api.ObjectID.TIGHTROPE_11406;
import static net.runelite.api.ObjectID.WALL_11630;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.agility.models.AgilityObstacleModel;
import net.runelite.client.plugins.microbot.util.misc.Operation;

public class DraynorCourse implements AgilityCourseHandler
{

	@Override
	public WorldPoint getStartPoint()
	{
		return new WorldPoint(3103, 3279, 0);
	}

	@Override
	public List<AgilityObstacleModel> getObstacles()
	{
		return List.of(
			new AgilityObstacleModel(ROUGH_WALL),
			new AgilityObstacleModel(TIGHTROPE),
			new AgilityObstacleModel(TIGHTROPE_11406),
			new AgilityObstacleModel(NARROW_WALL),
			new AgilityObstacleModel(WALL_11630, -1, 3256, Operation.GREATER, Operation.GREATER_EQUAL),
			new AgilityObstacleModel(GAP_11631, -1, 3255, Operation.GREATER, Operation.LESS_EQUAL),
			new AgilityObstacleModel(CRATE_11632)
		);
	}
}
