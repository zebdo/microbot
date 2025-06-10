package net.runelite.client.plugins.microbot.agility.courses;

import java.util.List;
import static net.runelite.api.ObjectID.GAP_15609;
import static net.runelite.api.ObjectID.GAP_15610;
import static net.runelite.api.ObjectID.GAP_15611;
import static net.runelite.api.ObjectID.GAP_15612;
import static net.runelite.api.ObjectID.PLANK_26635;
import static net.runelite.api.ObjectID.STEEP_ROOF;
import static net.runelite.api.ObjectID.WOODEN_BEAMS;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.agility.models.AgilityObstacleModel;
import net.runelite.client.plugins.microbot.util.misc.Operation;

public class ArdougneCourse implements AgilityCourseHandler
{
	@Override
	public WorldPoint getStartPoint()
	{
		return new WorldPoint(2673, 3298, 0);
	}

	@Override
	public List<AgilityObstacleModel> getObstacles()
	{
		return List.of(
			new AgilityObstacleModel(WOODEN_BEAMS),
			new AgilityObstacleModel(GAP_15609),
			new AgilityObstacleModel(PLANK_26635),
			new AgilityObstacleModel(GAP_15610),
			new AgilityObstacleModel(GAP_15611, -1, 3310, Operation.GREATER, Operation.GREATER_EQUAL),
			new AgilityObstacleModel(STEEP_ROOF),
			new AgilityObstacleModel(GAP_15612)
		);
	}
}
