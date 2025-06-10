package net.runelite.client.plugins.microbot.agility.courses;

import java.util.List;
import static net.runelite.api.ObjectID.GAP_14947;
import static net.runelite.api.ObjectID.GAP_14990;
import static net.runelite.api.ObjectID.GAP_14991;
import static net.runelite.api.ObjectID.PILE_OF_FISH;
import static net.runelite.api.ObjectID.ROUGH_WALL_14946;
import static net.runelite.api.ObjectID.TIGHTROPE_14987;
import static net.runelite.api.ObjectID.TIGHTROPE_14992;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.agility.models.AgilityObstacleModel;
import net.runelite.client.plugins.microbot.util.misc.Operation;

public class RellekkaCourse implements AgilityCourseHandler
{
	@Override
	public WorldPoint getStartPoint()
	{
		return new WorldPoint(2625, 3677, 0);
	}

	@Override
	public List<AgilityObstacleModel> getObstacles()
	{
		return List.of(
			new AgilityObstacleModel(ROUGH_WALL_14946),
			new AgilityObstacleModel(GAP_14947, -1, 3672, Operation.GREATER, Operation.GREATER),
			new AgilityObstacleModel(TIGHTROPE_14987),
			new AgilityObstacleModel(GAP_14990),
			new AgilityObstacleModel(GAP_14991, -1, 3653, Operation.GREATER, Operation.LESS_EQUAL),
			new AgilityObstacleModel(TIGHTROPE_14992),
			new AgilityObstacleModel(PILE_OF_FISH)
		);
	}
}
