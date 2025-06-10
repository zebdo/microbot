package net.runelite.client.plugins.microbot.agility.courses;

import java.util.List;
import static net.runelite.api.ObjectID.EDGE_14931;
import static net.runelite.api.ObjectID.GAP_14928;
import static net.runelite.api.ObjectID.GAP_14929;
import static net.runelite.api.ObjectID.GAP_14930;
import static net.runelite.api.ObjectID.TIGHTROPE_14932;
import static net.runelite.api.ObjectID.WALL_14927;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.agility.models.AgilityObstacleModel;

public class SeersCourse implements AgilityCourseHandler
{
	@Override
	public WorldPoint getStartPoint()
	{
		return new WorldPoint(2729, 3486, 0);
	}

	@Override
	public List<AgilityObstacleModel> getObstacles()
	{
		return List.of(
			new AgilityObstacleModel(WALL_14927),
			new AgilityObstacleModel(GAP_14928),
			new AgilityObstacleModel(TIGHTROPE_14932),
			new AgilityObstacleModel(GAP_14929),
			new AgilityObstacleModel(GAP_14930),
			new AgilityObstacleModel(EDGE_14931)
		);
	}
}
