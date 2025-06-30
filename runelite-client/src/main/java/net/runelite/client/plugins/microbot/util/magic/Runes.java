package net.runelite.client.plugins.microbot.util.magic;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ItemID;

@Getter
@RequiredArgsConstructor
public enum Runes
{
	AIR(1, ItemID.AIRRUNE),
	WATER(2, ItemID.WATERRUNE),
	EARTH(3, ItemID.EARTHRUNE),
	FIRE(4, ItemID.FIRERUNE),
	MIND(5, ItemID.MINDRUNE),
	CHAOS(6, ItemID.CHAOSRUNE),
	DEATH(7, ItemID.DEATHRUNE),
	BLOOD(8, ItemID.BLOODRUNE),
	COSMIC(9, ItemID.COSMICRUNE),
	NATURE(10, ItemID.NATURERUNE),
	LAW(11, ItemID.LAWRUNE),
	BODY(12, ItemID.BODYRUNE),
	SOUL(13, ItemID.SOULRUNE),
	ASTRAL(14, ItemID.ASTRALRUNE),
	MIST(15, ItemID.MISTRUNE),
	MUD(16, ItemID.MUDRUNE),
	DUST(17, ItemID.DUSTRUNE),
	LAVA(18, ItemID.LAVARUNE),
	STEAM(19, ItemID.STEAMRUNE),
	SMOKE(20, ItemID.SMOKERUNE),
	WRATH(21, ItemID.WRATHRUNE),
	SUNFIRE(22, ItemID.SUNFIRERUNE);

	@Getter(AccessLevel.PUBLIC)
	private final int id;
	@Getter(AccessLevel.PUBLIC)
	private final int itemId;

	public Set<Runes> getBaseRunes() {
		switch (this) {
			case MIST: return Set.of(AIR, WATER);
			case DUST: return Set.of(AIR, EARTH);
			case MUD: return Set.of(WATER, EARTH);
			case SMOKE: return Set.of(AIR, FIRE);
			case STEAM: return Set.of(WATER, FIRE);
			case LAVA: return Set.of(EARTH, FIRE);
			default: return Set.of(this);
		}
	}

	private static final Map<Integer, Runes> BY_VARBIT_ID = Arrays.stream(values()).collect(Collectors.toMap(Runes::getId, Function.identity()));

	public static Runes byVarbitId(int varbitValue)
	{
		return BY_VARBIT_ID.get(varbitValue);
	}

	public static Runes byItemId(int itemId) {
		return Arrays.stream(values()).filter(r -> r.getItemId() == itemId).findFirst().orElse(null);
	}
}
