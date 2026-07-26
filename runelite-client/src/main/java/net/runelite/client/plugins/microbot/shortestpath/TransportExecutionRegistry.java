package net.runelite.client.plugins.microbot.shortestpath;

import net.runelite.client.plugins.skillcalculator.skills.MagicAction;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * Planner-side view of the walker execution contract.
 *
 * <p>A catalog row must never be offered to automated pathfinding unless the
 * walker can dispatch it. The registry deliberately lives outside
 * {@code Rs2Walker}; checking capability is pure and does not interact with the
 * client or game state.</p>
 */
public final class TransportExecutionRegistry
{
	private static final Set<TransportType> SPECIALIZED_EXECUTORS = EnumSet.of(
		TransportType.BOAT,
		TransportType.CANOE,
		TransportType.CHARTER_SHIP,
		TransportType.SHIP,
		TransportType.FAIRY_RING,
		TransportType.QUETZAL,
		TransportType.GNOME_GLIDER,
		TransportType.HOT_AIR_BALLOON,
		TransportType.MAGIC_MUSHTREE,
		TransportType.POH,
		TransportType.SPIRIT_TREE,
		TransportType.TELEPORTATION_MINIGAME,
		TransportType.TELEPORTATION_ITEM,
		TransportType.TELEPORTATION_SPELL,
		TransportType.WILDERNESS_OBELISK,
		TransportType.MAGIC_CARPET,
		TransportType.SEASONAL_TRANSPORT,
		TransportType.NPC);

	private static final Set<TransportType> GENERIC_OBJECT_EXECUTORS = EnumSet.of(
		TransportType.TRANSPORT,
		TransportType.AGILITY_SHORTCUT,
		TransportType.GRAPPLE_SHORTCUT,
		TransportType.MINECART,
		TransportType.TELEPORTATION_LEVER,
		TransportType.TELEPORTATION_PORTAL);

	private TransportExecutionRegistry()
	{
	}

	public static boolean canExecute(Transport transport)
	{
		if (transport == null || transport.getType() == null || transport.getDestination() == null)
		{
			return false;
		}

		if ("TELEPORTATION_BOX".equals(transport.getCatalogType())
			|| "TELEPORTATION_PORTAL_POH".equals(transport.getCatalogType()))
		{
			return false;
		}
		if (transport.getType() == TransportType.TELEPORTATION_SPELL)
		{
			if ("TELEPORTATION_SPELL_HOME".equals(transport.getCatalogType()))
			{
				return transport.getDisplayInfo() != null;
			}
			String displayInfo = transport.getDisplayInfo();
			if (displayInfo == null)
			{
				return false;
			}
			String spellName = baseSpellName(displayInfo);
			return Arrays.stream(MagicAction.values())
				.anyMatch(action -> action.getName().equalsIgnoreCase(spellName));
		}
		if (transport.getType() == TransportType.MAGIC_MUSHTREE)
		{
			return MagicMushtree.getByDestination(transport.getDestination()) != null;
		}
		if (transport.getType() == TransportType.HOT_AIR_BALLOON)
		{
			return Set.of("Castle Wars", "Grand Tree", "Crafting Guild", "Entrana", "Taverley", "Varrock")
				.contains(transport.getDisplayInfo());
		}

		if (SPECIALIZED_EXECUTORS.contains(transport.getType()))
		{
			return true;
		}

		if (GENERIC_OBJECT_EXECUTORS.contains(transport.getType()))
		{
			boolean automaticTrigger = transport.getType() == TransportType.TRANSPORT
				&& transport.getSource().startsWith("Skretzo/shortest-path@")
				&& transport.getObjectId() <= 0
				&& isBlank(transport.getAction())
				&& isBlank(transport.getName());
			return transport.getOrigin() != null
				&& (automaticTrigger
					|| ((transport.getObjectId() > 0 || !isBlank(transport.getName()))
						&& !isBlank(transport.getAction())));
		}

		return false;
	}

	private static String baseSpellName(String displayInfo)
	{
		String name = displayInfo.contains(":")
			? displayInfo.substring(0, displayInfo.indexOf(':')).trim()
			: displayInfo.trim();
		return name.replaceFirst("(?i)\\s+\\((inside|outside)\\)$", "");
	}

	private static boolean isBlank(String value)
	{
		return value == null || value.trim().isEmpty();
	}
}
