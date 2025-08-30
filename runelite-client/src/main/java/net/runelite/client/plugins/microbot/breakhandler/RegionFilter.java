package net.runelite.client.plugins.microbot.breakhandler;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.http.api.worlds.WorldRegion;

/**
 * Region options for selecting random worlds.
 */
@RequiredArgsConstructor
public enum RegionFilter {
    ANY(null, "Any"),
    US(WorldRegion.UNITED_STATES_OF_AMERICA, "US"),
    UK(WorldRegion.UNITED_KINGDOM, "UK"),
    GERMANY(WorldRegion.GERMANY, "Germany"),
    AUSTRALIA(WorldRegion.AUSTRALIA, "Australia");

    @Getter
    private final WorldRegion region;

    private final String name;

    @Override
    public String toString() {
        return name;
    }
}
