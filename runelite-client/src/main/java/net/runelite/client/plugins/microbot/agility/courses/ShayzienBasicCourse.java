package net.runelite.client.plugins.microbot.agility.courses;

import java.util.List;
import static net.runelite.api.ObjectID.BAR_42213;
import static net.runelite.api.ObjectID.GAP_42216;
import static net.runelite.api.ObjectID.LADDER_42209;
import static net.runelite.api.ObjectID.MONKEYBARS_42211;
import static net.runelite.api.ObjectID.TIGHTROPE_42212;
import static net.runelite.api.ObjectID.TIGHTROPE_42214;
import static net.runelite.api.ObjectID.TIGHTROPE_42215;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.agility.models.AgilityObstacleModel;
import net.runelite.client.plugins.microbot.util.misc.Operation;

public class ShayzienBasicCourse implements AgilityCourseHandler
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
			new AgilityObstacleModel(BAR_42213),
			new AgilityObstacleModel(TIGHTROPE_42214),
			new AgilityObstacleModel(TIGHTROPE_42215, -1, 3643, Operation.GREATER, Operation.GREATER_EQUAL),
			new AgilityObstacleModel(GAP_42216)
		);
	}
}
