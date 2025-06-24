package net.runelite.client.plugins.microbot.util.magic.thralls;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.microbot.util.magic.Rs2Spells;

/**
 * Represents the different types of thralls that can be summoned using the Arceuus spellbook.
 * <p>
 * Each thrall has a corresponding spell and type (Magic, Ranged, or Melee).
 * Thralls vary in strength from Lesser to Superior to Greater.
 */
@Getter
@RequiredArgsConstructor
public enum Thrall
{
	LESSER_GHOST(Rs2Spells.RESURRECT_LESSER_GHOST, ThrallType.MAGIC),
	LESSER_SKELETON(Rs2Spells.RESURRECT_LESSER_SKELETON, ThrallType.RANGED),
	LESSER_ZOMBIE(Rs2Spells.RESURRECT_LESSER_ZOMBIE, ThrallType.MELEE),
	SUPERIOR_GHOST(Rs2Spells.RESURRECT_SUPERIOR_GHOST, ThrallType.MAGIC),
	SUPERIOR_SKELETON(Rs2Spells.RESURRECT_SUPERIOR_SKELETON, ThrallType.RANGED),
	SUPERIOR_ZOMBIE(Rs2Spells.RESURRECT_SUPERIOR_ZOMBIE, ThrallType.MELEE),
	GREATER_GHOST(Rs2Spells.RESURRECT_GREATER_GHOST, ThrallType.MAGIC),
	GREATER_SKELETON(Rs2Spells.RESURRECT_GREATER_SKELETON, ThrallType.RANGED),
	GREATER_ZOMBIE(Rs2Spells.RESURRECT_GREATER_ZOMBIE, ThrallType.MELEE),
	;

	private final Rs2Spells rs2spell;
	private final ThrallType thrallType;

	/**
	 * Checks if the given thrall can be cast based on current game state.
	 *
	 * @param thrall the thrall to check
	 * @return true if the thrall can be summoned, false otherwise
	 */
	public static boolean canCast(Thrall thrall)
	{
		if (isActive())
		{
			return false;
		}
		return thrall.getRs2spell().hasRequirements() && Rs2Magic.hasRequiredRunes(thrall.getRs2spell());
	}

	/**
	 * Returns whether a thrall is currently active or on cooldown.
	 *
	 * @return true if a thrall is active or cooling down, false otherwise
	 */
	public static boolean isActive()
	{
		return (Microbot.getVarbitValue(VarbitID.ARCEUUS_RESURRECTION_ACTIVE) == 1 || Microbot.getVarbitValue(VarbitID.ARCEUUS_RESURRECTION_COOLDOWN) >= 1);
	}

	/**
	 * Attempts to summon the given thrall if casting conditions are met.
	 *
	 * @param thrall the thrall to summon
	 * @return true if the cast was successful, false otherwise
	 */
	public static boolean cast(Thrall thrall)
	{
		if (!canCast(thrall))
		{
			return false;
		}
		return Rs2Magic.cast(thrall.getRs2spell().getAction());
	}

	/**
	 * Gets the best available thrall of the given type that the player can currently cast.
	 *
	 * @param type the desired ThrallType (MAGIC, RANGED, or MELEE)
	 * @return the best available Thrall of that type, or null if none can be cast
	 */
	public static Thrall getBestThrall(ThrallType type)
	{
		return Arrays.stream(Thrall.values()).filter(thrall -> thrall.getThrallType() == type).filter(Thrall::canCast).findFirst().orElse(null);
	}
}
