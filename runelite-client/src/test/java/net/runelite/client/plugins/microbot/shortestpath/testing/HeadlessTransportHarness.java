package net.runelite.client.plugins.microbot.shortestpath.testing;

import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.PrimitiveIntHashMap;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportExecutionRegistry;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.shortestpath.WorldPointUtil;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.Pathfinder;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.PathfinderConfig;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.RouteStep;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.SplitFlagMap;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.policy.TransportRequirementPolicy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Runs catalog eligibility and real collision-map pathfinding without a
 * logged-in RuneLite client. It records execution intents but deliberately
 * never clicks an object, NPC, widget, item, or spell.
 */
public final class HeadlessTransportHarness
{
	private static final List<QuestState> QUEST_STATE_ORDER = Arrays.asList(
		QuestState.NOT_STARTED,
		QuestState.IN_PROGRESS,
		QuestState.FINISHED);
	private static final long DEFAULT_PATHFINDING_CUTOFF_MS = 5_000;
	private final SplitFlagMap collisionMap;
	private final Map<WorldPoint, Set<Transport>> catalog;

	private HeadlessTransportHarness(
		SplitFlagMap collisionMap,
		Map<WorldPoint, Set<Transport>> catalog)
	{
		this.collisionMap = collisionMap;
		this.catalog = catalog;
	}

	public static HeadlessTransportHarness loadResources()
	{
		return new HeadlessTransportHarness(
			SplitFlagMap.fromResources(),
			Transport.loadAllFromResources());
	}

	public List<Transport> find(Predicate<Transport> selector)
	{
		return catalog.values().stream()
			.flatMap(Set::stream)
			.filter(selector)
			.collect(Collectors.toList());
	}

	public Eligibility evaluate(Transport transport, HeadlessTransportState state)
	{
		EnumSet<Rejection> rejections = EnumSet.noneOf(Rejection.class);
		if (!TransportExecutionRegistry.canExecute(transport))
		{
			rejections.add(Rejection.NO_EXECUTOR);
			return new Eligibility(rejections);
		}
		if (!state.isTypeEnabled(transport.getType()))
		{
			rejections.add(Rejection.FEATURE_DISABLED);
		}
		if (!state.isMembersWorld()
			&& TransportRequirementPolicy.requiresMembersWorld(transport))
		{
			rejections.add(Rejection.MEMBERS_WORLD);
		}
		if (!TransportRequirementPolicy.hasRequiredLevels(transport, state::getBoostedLevel))
		{
			rejections.add(Rejection.SKILL);
		}
		if (!TransportRequirementPolicy.completedQuests(
			transport, QUEST_STATE_ORDER, state::getQuestState))
		{
			rejections.add(Rejection.QUEST);
		}
		if (!TransportRequirementPolicy.hasNetworkAccess(
			transport.getType(),
			state::getQuestState,
			state::getVarbitValue,
			state::getItemQuantity))
		{
			rejections.add(Rejection.NETWORK_ACCESS);
		}
		if (!TransportRequirementPolicy.varbitChecks(transport, state::getVarbitValue))
		{
			rejections.add(Rejection.VARBIT);
		}
		if (!TransportRequirementPolicy.varplayerChecks(transport, state::getVarplayerValue))
		{
			rejections.add(Rejection.VARPLAYER);
		}
		if (TransportRequirementPolicy.exceedsCurrencyThreshold(
			transport, state.getCurrencyThreshold()))
		{
			rejections.add(Rejection.CURRENCY_THRESHOLD);
		}
		if (transport.getCurrencyAmount() > 0
			&& state.getCurrencyQuantity(transport.getCurrencyName()) < transport.getCurrencyAmount())
		{
			rejections.add(Rejection.CURRENCY);
		}
		if (!TransportRequirementPolicy.hasRequiredItems(
			transport, state::getItemQuantity, state::isEquipped))
		{
			rejections.add(Rejection.ITEMS);
		}
		if (TransportType.isTeleport(transport.getType(), transport.getOrigin())
			&& !state.isTeleportsEnabled())
		{
			rejections.add(Rejection.TELEPORTS_DISABLED);
		}
		return new Eligibility(rejections);
	}

	public RouteResult route(
		HeadlessTransportState state,
		WorldPoint start,
		WorldPoint destination,
		Predicate<Transport> selector)
	{
		Map<WorldPoint, Set<Transport>> eligible = eligibleTransports(state, selector);
		PathfinderConfig config = new HeadlessPathfinderConfig(
			collisionMap, eligible, DEFAULT_PATHFINDING_CUTOFF_MS);
		Pathfinder pathfinder = new Pathfinder(config, start, destination);
		pathfinder.run();

		List<WorldPoint> path = new ArrayList<>(pathfinder.getPath());
		List<RouteStep> steps = new ArrayList<>(pathfinder.getRouteSteps());
		List<Transport> selected = new ArrayList<>(pathfinder.getSelectedTransports());
		List<TransportExecutionRegistry.ExecutionIntent> intents = selected.stream()
			.map(transport -> TransportExecutionRegistry.executionIntentFor(transport)
				.orElseThrow(() -> new AssertionError(
					"Selected transport has no execution intent: " + transport)))
			.collect(Collectors.toList());
		boolean reached = !path.isEmpty() && destination.equals(path.get(path.size() - 1));
		return new RouteResult(reached, path, steps, selected, intents, pathfinder.getStats());
	}

	private Map<WorldPoint, Set<Transport>> eligibleTransports(
		HeadlessTransportState state,
		Predicate<Transport> selector)
	{
		Map<WorldPoint, Set<Transport>> eligible = new HashMap<>();
		for (Map.Entry<WorldPoint, Set<Transport>> entry : catalog.entrySet())
		{
			Set<Transport> atOrigin = entry.getValue().stream()
				.filter(selector)
				.filter(transport -> evaluate(transport, state).isAllowed())
				.collect(Collectors.toCollection(HashSet::new));
			if (!atOrigin.isEmpty())
			{
				eligible.put(entry.getKey(), atOrigin);
			}
		}
		return eligible;
	}

	public enum Rejection
	{
		CURRENCY,
		CURRENCY_THRESHOLD,
		FEATURE_DISABLED,
		ITEMS,
		MEMBERS_WORLD,
		NETWORK_ACCESS,
		NO_EXECUTOR,
		QUEST,
		SKILL,
		TELEPORTS_DISABLED,
		VARBIT,
		VARPLAYER
	}

	public static final class Eligibility
	{
		private final Set<Rejection> rejections;

		private Eligibility(Set<Rejection> rejections)
		{
			this.rejections = rejections.isEmpty()
				? Collections.emptySet()
				: Collections.unmodifiableSet(EnumSet.copyOf(rejections));
		}

		public boolean isAllowed()
		{
			return rejections.isEmpty();
		}

		public Set<Rejection> getRejections()
		{
			return rejections;
		}
	}

	public static final class RouteResult
	{
		private final boolean reached;
		private final List<WorldPoint> path;
		private final List<RouteStep> routeSteps;
		private final List<Transport> selectedTransports;
		private final List<TransportExecutionRegistry.ExecutionIntent> executionIntents;
		private final Pathfinder.PathfinderStats stats;

		private RouteResult(
			boolean reached,
			List<WorldPoint> path,
			List<RouteStep> routeSteps,
			List<Transport> selectedTransports,
			List<TransportExecutionRegistry.ExecutionIntent> executionIntents,
			Pathfinder.PathfinderStats stats)
		{
			this.reached = reached;
			this.path = Collections.unmodifiableList(path);
			this.routeSteps = Collections.unmodifiableList(routeSteps);
			this.selectedTransports = Collections.unmodifiableList(selectedTransports);
			this.executionIntents = Collections.unmodifiableList(executionIntents);
			this.stats = stats;
		}

		public boolean isReached()
		{
			return reached;
		}

		public List<WorldPoint> getPath()
		{
			return path;
		}

		public List<RouteStep> getRouteSteps()
		{
			return routeSteps;
		}

		public List<Transport> getSelectedTransports()
		{
			return selectedTransports;
		}

		public List<TransportExecutionRegistry.ExecutionIntent> getExecutionIntents()
		{
			return executionIntents;
		}

		public Pathfinder.PathfinderStats getStats()
		{
			return stats;
		}
	}

	private static final class HeadlessPathfinderConfig extends PathfinderConfig
	{
		private final long cutoffMillis;

		private HeadlessPathfinderConfig(
			SplitFlagMap map,
			Map<WorldPoint, Set<Transport>> transports,
			long cutoffMillis)
		{
			super(map, transports, Collections.emptyList(), null, null);
			this.cutoffMillis = cutoffMillis;

			Set<Transport> teleports = new HashSet<>();
			PrimitiveIntHashMap<Set<Transport>> packed = getTransportsPacked();
			for (Map.Entry<WorldPoint, Set<Transport>> entry : transports.entrySet())
			{
				Set<Transport> values = new HashSet<>(entry.getValue());
				if (entry.getKey() == null)
				{
					teleports.addAll(values);
					continue;
				}
				getTransports().put(entry.getKey(), values);
				packed.put(WorldPointUtil.packWorldPoint(entry.getKey()), values);
			}
			setUsableTeleports(teleports);
		}

		@Override
		public long getCalculationCutoffMillis()
		{
			return cutoffMillis;
		}

		@Override
		public int getTransportCost(Transport transport)
		{
			return 0;
		}
	}
}
