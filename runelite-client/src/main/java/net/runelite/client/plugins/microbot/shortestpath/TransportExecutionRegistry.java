package net.runelite.client.plugins.microbot.shortestpath;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.skillcalculator.skills.MagicAction;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
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
	public enum Executor
	{
		AUTOMATIC_TRIGGER,
		CANOE,
		CHARTER_SHIP,
		FAIRY_RING,
		GNOME_GLIDER,
		HOT_AIR_BALLOON,
		ITEM_TELEPORT,
		MAGIC_CARPET,
		MAGIC_MUSHTREE,
		MINIGAME_TELEPORT,
		NPC_DIALOGUE,
		OBJECT,
		POH,
		QUETZAL,
		SEASONAL,
		SPELL_TELEPORT,
		SPIRIT_TREE,
		WILDERNESS_OBELISK
	}

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
		return executorFor(transport).isPresent();
	}

	/**
	 * Resolves the live walker branch that owns a transport without touching the
	 * client or current game state.
	 */
	public static Optional<Executor> executorFor(Transport transport)
	{
		if (transport == null || transport.getType() == null || transport.getDestination() == null)
		{
			return Optional.empty();
		}

		if ("TELEPORTATION_BOX".equals(transport.getCatalogType())
			|| "TELEPORTATION_PORTAL_POH".equals(transport.getCatalogType()))
		{
			return Optional.empty();
		}
		if (transport.getType() == TransportType.TELEPORTATION_SPELL)
		{
			if ("TELEPORTATION_SPELL_HOME".equals(transport.getCatalogType()))
			{
				return transport.getDisplayInfo() == null
					? Optional.empty()
					: Optional.of(Executor.SPELL_TELEPORT);
			}
			String displayInfo = transport.getDisplayInfo();
			if (displayInfo == null)
			{
				return Optional.empty();
			}
			String spellName = baseSpellName(displayInfo);
			boolean registered = Arrays.stream(MagicAction.values())
				.anyMatch(action -> action.getName().equalsIgnoreCase(spellName));
			return registered ? Optional.of(Executor.SPELL_TELEPORT) : Optional.empty();
		}
		if (transport.getType() == TransportType.MAGIC_MUSHTREE)
		{
			return MagicMushtree.getByDestination(transport.getDestination()) == null
				? Optional.empty()
				: Optional.of(Executor.MAGIC_MUSHTREE);
		}
		if (transport.getType() == TransportType.HOT_AIR_BALLOON)
		{
			return Set.of("Castle Wars", "Grand Tree", "Crafting Guild", "Entrana", "Taverley", "Varrock")
				.contains(transport.getDisplayInfo())
				? Optional.of(Executor.HOT_AIR_BALLOON)
				: Optional.empty();
		}

		Executor specialized = specializedExecutor(transport.getType());
		if (specialized != null)
		{
			return Optional.of(specialized);
		}

		if (GENERIC_OBJECT_EXECUTORS.contains(transport.getType()))
		{
			boolean automaticTrigger = transport.getType() == TransportType.TRANSPORT
				&& transport.getSource().startsWith("Skretzo/shortest-path@")
				&& transport.getObjectId() <= 0
				&& isBlank(transport.getAction())
				&& isBlank(transport.getName());
			if (transport.getOrigin() == null)
			{
				return Optional.empty();
			}
			if (automaticTrigger)
			{
				return Optional.of(Executor.AUTOMATIC_TRIGGER);
			}
			if ((transport.getObjectId() > 0 || !isBlank(transport.getName()))
				&& !isBlank(transport.getAction()))
			{
				return Optional.of(Executor.OBJECT);
			}
		}

		return Optional.empty();
	}

	/**
	 * Creates a serializable-style snapshot of the interaction the walker would
	 * attempt. This is intentionally side-effect free for headless tests.
	 */
	public static Optional<ExecutionIntent> executionIntentFor(Transport transport)
	{
		return executorFor(transport).map(executor -> new ExecutionIntent(executor, transport));
	}

	private static Executor specializedExecutor(TransportType type)
	{
		switch (type)
		{
			case BOAT:
			case SHIP:
			case NPC:
				return Executor.NPC_DIALOGUE;
			case CANOE:
				return Executor.CANOE;
			case CHARTER_SHIP:
				return Executor.CHARTER_SHIP;
			case FAIRY_RING:
				return Executor.FAIRY_RING;
			case GNOME_GLIDER:
				return Executor.GNOME_GLIDER;
			case HOT_AIR_BALLOON:
				return Executor.HOT_AIR_BALLOON;
			case MAGIC_CARPET:
				return Executor.MAGIC_CARPET;
			case MAGIC_MUSHTREE:
				return Executor.MAGIC_MUSHTREE;
			case POH:
				return Executor.POH;
			case QUETZAL:
				return Executor.QUETZAL;
			case SPIRIT_TREE:
				return Executor.SPIRIT_TREE;
			case TELEPORTATION_ITEM:
				return Executor.ITEM_TELEPORT;
			case TELEPORTATION_MINIGAME:
				return Executor.MINIGAME_TELEPORT;
			case TELEPORTATION_SPELL:
				return Executor.SPELL_TELEPORT;
			case WILDERNESS_OBELISK:
				return Executor.WILDERNESS_OBELISK;
			case SEASONAL_TRANSPORT:
				return Executor.SEASONAL;
			default:
				return null;
		}
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

	public static final class ExecutionIntent
	{
		private final Executor executor;
		private final TransportType transportType;
		private final String catalogType;
		private final WorldPoint origin;
		private final WorldPoint destination;
		private final int objectId;
		private final String action;
		private final String target;
		private final String displayInfo;

		private ExecutionIntent(Executor executor, Transport transport)
		{
			this.executor = executor;
			this.transportType = transport.getType();
			this.catalogType = transport.getCatalogType();
			this.origin = transport.getOrigin();
			this.destination = transport.getDestination();
			this.objectId = transport.getObjectId();
			this.action = transport.getAction();
			this.target = transport.getName();
			this.displayInfo = transport.getDisplayInfo();
		}

		public Executor getExecutor()
		{
			return executor;
		}

		public TransportType getTransportType()
		{
			return transportType;
		}

		public String getCatalogType()
		{
			return catalogType;
		}

		public WorldPoint getOrigin()
		{
			return origin;
		}

		public WorldPoint getDestination()
		{
			return destination;
		}

		public int getObjectId()
		{
			return objectId;
		}

		public String getAction()
		{
			return action;
		}

		public String getTarget()
		{
			return target;
		}

		public String getDisplayInfo()
		{
			return displayInfo;
		}
	}
}
