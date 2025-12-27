package net.runelite.client.plugins.microbot.api.boat;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.boat.models.Rs2BoatModel;
import net.runelite.client.plugins.microbot.api.player.models.Rs2PlayerModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

@Slf4j
public final class Rs2BoatCache
{
    private int lastCheckedOnBoat = 0;
    private WorldEntity boat = null;


	public Rs2BoatCache()
	{
	}

	public Rs2BoatModel getLocalBoat()
	{
        if (lastCheckedOnBoat * 2 >= Microbot.getClient().getTickCount()) {
            return new Rs2BoatModel(boat);
        }

        boat = Microbot.getClientThread().invoke(() ->
		{
            lastCheckedOnBoat = Microbot.getClient().getTickCount();
			Client client = Microbot.getClient();
			Player player = client.getLocalPlayer();

			if (player == null)
			{
				return null;
			}

			WorldView playerView = player.getWorldView();

			if (!playerView.isTopLevel())
			{
				LocalPoint playerLocal = player.getLocalLocation();
				int worldViewId = playerLocal.getWorldView();

				return client.getTopLevelWorldView()
					.worldEntities()
					.byIndex(worldViewId);
			}

			return null;
		});

        return new Rs2BoatModel(boat);
	}

    public Rs2BoatModel getBoat(Rs2PlayerModel player)
    {
        if (lastCheckedOnBoat * 2 >= Microbot.getClient().getTickCount()) {
            return new Rs2BoatModel(boat);
        }

        boat = Microbot.getClientThread().invoke(() ->
        {
            lastCheckedOnBoat = Microbot.getClient().getTickCount();
            Client client = Microbot.getClient();

            if (player == null)
            {
                return null;
            }

            WorldView playerView = player.getWorldView();

            if (!playerView.isTopLevel())
            {
                LocalPoint playerLocal = player.getLocalLocation();
                int worldViewId = playerLocal.getWorldView();

                return client.getTopLevelWorldView()
                        .worldEntities()
                        .byIndex(worldViewId);
            }

            return null;
        });

        return new Rs2BoatModel(boat);
    }
}
