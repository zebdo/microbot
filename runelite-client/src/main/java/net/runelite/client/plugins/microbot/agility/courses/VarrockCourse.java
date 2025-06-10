package net.runelite.client.plugins.microbot.agility.courses;

import java.util.List;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;
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
			new AgilityObstacleModel(ObjectID.ROOFTOPS_VARROCK_WALLCLIMB),
			new AgilityObstacleModel(ObjectID.ROOFTOPS_VARROCK_CLOTHESLINE),
			new AgilityObstacleModel(ObjectID.ROOFTOPS_VARROCK_LEAPTORUINS),
			new AgilityObstacleModel(ObjectID.ROOFTOPS_VARROCK_WALLSWING),
			new AgilityObstacleModel(ObjectID.ROOFTOPS_VARROCK_WALLSCRAMBLE), // this obstacle doesn't always work for some reason
			new AgilityObstacleModel(ObjectID.ROOFTOPS_VARROCK_LEAPTOBALCONY),
			new AgilityObstacleModel(ObjectID.ROOFTOPS_VARROCK_LEAPDOWN, -1, 3402, Operation.GREATER, Operation.LESS_EQUAL),
			new AgilityObstacleModel(ObjectID.ROOFTOPS_VARROCK_STEPUPROOF, -1, 3408, Operation.GREATER, Operation.LESS_EQUAL),
			new AgilityObstacleModel(ObjectID.ROOFTOPS_VARROCK_FINISH)
		);
	}
}
