package net.runelite.client.plugins.microbot.agility.courses;

import java.util.List;
import static net.runelite.api.ObjectID.BEAM;
import static net.runelite.api.ObjectID.BEAM_42220;
import static net.runelite.api.ObjectID.EDGE_42218;
import static net.runelite.api.ObjectID.EDGE_42219;
import static net.runelite.api.ObjectID.LADDER_42209;
import static net.runelite.api.ObjectID.MONKEYBARS_42211;
import static net.runelite.api.ObjectID.TIGHTROPE_42212;
import static net.runelite.api.ObjectID.ZIPLINE;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.agility.models.AgilityObstacleModel;
import net.runelite.client.plugins.microbot.util.misc.Operation;

public class ShayzienAdvancedCourse implements AgilityCourseHandler
{
	@Override
	public WorldPoint getStartPoint()
	{
		return new WorldPoint(1551, 3632, 0);
	}

	@Override
	public List<AgilityObstacleModel> getObstacles()
	{
		return List.of(
			new AgilityObstacleModel(LADDER_42209),
			new AgilityObstacleModel(MONKEYBARS_42211),
			new AgilityObstacleModel(TIGHTROPE_42212, -1, 3635, Operation.GREATER, Operation.LESS_EQUAL),
			new AgilityObstacleModel(BEAM, -1, -3633, Operation.GREATER, Operation.GREATER_EQUAL),
			new AgilityObstacleModel(EDGE_42218, -1, 3635, Operation.GREATER, Operation.GREATER_EQUAL),
			new AgilityObstacleModel(EDGE_42219, -1, 3630, Operation.GREATER, Operation.LESS_EQUAL),
			new AgilityObstacleModel(BEAM_42220, 1511, -1, Operation.LESS_EQUAL, Operation.GREATER_EQUAL),
			new AgilityObstacleModel(ZIPLINE)
		);
	}
}
