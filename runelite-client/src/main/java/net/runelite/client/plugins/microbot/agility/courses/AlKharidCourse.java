package net.runelite.client.plugins.microbot.agility.courses;

import java.util.List;
import static net.runelite.api.ObjectID.CABLE;
import static net.runelite.api.ObjectID.GAP_14399;
import static net.runelite.api.ObjectID.ROOF_TOP_BEAMS;
import static net.runelite.api.ObjectID.ROUGH_WALL_11633;
import static net.runelite.api.ObjectID.TIGHTROPE_14398;
import static net.runelite.api.ObjectID.TIGHTROPE_14409;
import static net.runelite.api.ObjectID.TROPICAL_TREE_14404;
import static net.runelite.api.ObjectID.ZIP_LINE_14403;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.agility.models.AgilityObstacleModel;

public class AlKharidCourse implements AgilityCourseHandler
{
	@Override
	public WorldPoint getStartPoint()
	{
		return new WorldPoint(3273, 3195, 0);
	}

	@Override
	public List<AgilityObstacleModel> getObstacles()
	{
		return List.of(
			new AgilityObstacleModel(ROUGH_WALL_11633),
			new AgilityObstacleModel(TIGHTROPE_14398),
			new AgilityObstacleModel(CABLE),
			new AgilityObstacleModel(ZIP_LINE_14403),
			new AgilityObstacleModel(TROPICAL_TREE_14404),
			new AgilityObstacleModel(ROOF_TOP_BEAMS),
			new AgilityObstacleModel(TIGHTROPE_14409),
			new AgilityObstacleModel(GAP_14399)
		);
	}
}
