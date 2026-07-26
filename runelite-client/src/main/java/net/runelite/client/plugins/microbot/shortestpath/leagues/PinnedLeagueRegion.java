package net.runelite.client.plugins.microbot.shortestpath.leagues;

import net.runelite.client.plugins.microbot.util.leaguetransport.LeaguesRegion;

/**
 * Region names used by the pinned Skretzo/shortest-path league map.
 */
public enum PinnedLeagueRegion
{
	VARLAMORE(LeaguesRegion.VARLAMORE),
	KARAMJA(LeaguesRegion.KARAMJA),
	ASGARNIA(LeaguesRegion.ASGARNIA),
	KANDARIN(LeaguesRegion.KANDARIN),
	FREMENNIK(LeaguesRegion.FREMENNIK),
	KOUREND(LeaguesRegion.KEBOS_AND_KOUREND),
	WILDERNESS(LeaguesRegion.WILDERNESS),
	MORYTANIA(LeaguesRegion.MORYTANIA),
	DESERT(LeaguesRegion.DESERT),
	TIRANNWN(LeaguesRegion.TIRANNWN),
	MISTHALIN(LeaguesRegion.MISTHALIN),
	NEUTRAL(null);

	private final LeaguesRegion localRegion;

	PinnedLeagueRegion(LeaguesRegion localRegion)
	{
		this.localRegion = localRegion;
	}

	public LeaguesRegion getLocalRegion()
	{
		return localRegion;
	}

	public boolean isAlwaysAllowed()
	{
		return this == VARLAMORE || this == NEUTRAL;
	}

	public boolean isAlwaysBlocked()
	{
		return this == MISTHALIN;
	}
}
