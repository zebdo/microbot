package net.runelite.client.plugins.microbot.crafting.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Gems
{
    NONE(" ", 1),
    OPAL("Opal", 1),
    JADE("Jade", 13),
    RED_TOPAZ("Red Topaz", 16),
    SAPPHIRE("Sapphire", 20),
    EMERALD("Emerald", 27),
    RUBY("Ruby", 34),
    DIAMOND("Diamond", 43),
    DRAGONSTONE("Dragonstone", 55),
    ONYX("Onyx", 67),
    AMETHYST_BOLT_TIPS("Amethyst bolt tips", 83),
    AMETHYST_ARROWTIPS("Amethyst arrowtips", 85),
    AMETHYST_JAVELIN_HEADS("Amethyst javelin heads", 87),
    AMETHYST_DART_TIPS("Amethyst dart tips", 89),
    ZENYTE("Zenyte", 89);

    private final String name;
    private final int levelRequired;

    @Override
    public String toString()
    {
        return name;
    }
}