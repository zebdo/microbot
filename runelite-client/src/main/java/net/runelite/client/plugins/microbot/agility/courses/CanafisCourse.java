package net.runelite.client.plugins.microbot.agility.courses;

import java.util.List;
import static net.runelite.api.ObjectID.GAP_14844;
import static net.runelite.api.ObjectID.GAP_14845;
import static net.runelite.api.ObjectID.GAP_14846;
import static net.runelite.api.ObjectID.GAP_14847;
import static net.runelite.api.ObjectID.GAP_14848;
import static net.runelite.api.ObjectID.GAP_14897;
import static net.runelite.api.ObjectID.POLEVAULT;
import static net.runelite.api.ObjectID.TALL_TREE_14843;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.agility.models.AgilityObstacleModel;

public class CanafisCourse implements AgilityCourseHandler
{
	@Override
	public WorldPoint getStartPoint()
	{
		return new WorldPoint(3507, 3489, 0);
	}

	@Override
	public List<AgilityObstacleModel> getObstacles()
	{
		return List.of(
			new AgilityObstacleModel(TALL_TREE_14843),
			new AgilityObstacleModel(GAP_14844),
			new AgilityObstacleModel(GAP_14845),
			new AgilityObstacleModel(GAP_14848),
			new AgilityObstacleModel(GAP_14846),
			new AgilityObstacleModel(POLEVAULT),
			new AgilityObstacleModel(GAP_14847),
			new AgilityObstacleModel(GAP_14897)
		);
	}
}
