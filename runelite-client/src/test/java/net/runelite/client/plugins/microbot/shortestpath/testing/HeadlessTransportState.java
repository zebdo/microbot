package net.runelite.client.plugins.microbot.shortestpath.testing;

import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Immutable synthetic player/configuration state used by
 * {@link HeadlessTransportHarness}. No RuneLite client is required.
 */
public final class HeadlessTransportState
{
	private final boolean membersWorld;
	private final int defaultBoostedLevel;
	private final QuestState defaultQuestState;
	private final Map<Skill, Integer> boostedLevels;
	private final Map<Quest, QuestState> questStates;
	private final Map<Integer, Integer> varbits;
	private final Map<Integer, Integer> varplayers;
	private final Map<Integer, Integer> itemQuantities;
	private final Set<Integer> equippedItems;
	private final Map<String, Integer> currencyQuantities;
	private final Set<TransportType> enabledTypes;
	private final boolean teleportsEnabled;
	private final int currencyThreshold;

	private HeadlessTransportState(Builder builder)
	{
		membersWorld = builder.membersWorld;
		defaultBoostedLevel = builder.defaultBoostedLevel;
		defaultQuestState = builder.defaultQuestState;
		boostedLevels = Collections.unmodifiableMap(new EnumMap<>(builder.boostedLevels));
		questStates = Collections.unmodifiableMap(new EnumMap<>(builder.questStates));
		varbits = Collections.unmodifiableMap(new HashMap<>(builder.varbits));
		varplayers = Collections.unmodifiableMap(new HashMap<>(builder.varplayers));
		itemQuantities = Collections.unmodifiableMap(new HashMap<>(builder.itemQuantities));
		equippedItems = Collections.unmodifiableSet(new HashSet<>(builder.equippedItems));
		currencyQuantities = Collections.unmodifiableMap(new HashMap<>(builder.currencyQuantities));
		enabledTypes = builder.enabledTypes.isEmpty()
			? Collections.emptySet()
			: Collections.unmodifiableSet(EnumSet.copyOf(builder.enabledTypes));
		teleportsEnabled = builder.teleportsEnabled;
		currencyThreshold = builder.currencyThreshold;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public boolean isMembersWorld()
	{
		return membersWorld;
	}

	public int getBoostedLevel(Skill skill)
	{
		return boostedLevels.getOrDefault(skill, defaultBoostedLevel);
	}

	public QuestState getQuestState(Quest quest)
	{
		return questStates.getOrDefault(quest, defaultQuestState);
	}

	public int getVarbitValue(int id)
	{
		return varbits.getOrDefault(id, 0);
	}

	public int getVarplayerValue(int id)
	{
		return varplayers.getOrDefault(id, 0);
	}

	public int getItemQuantity(int itemId)
	{
		return itemQuantities.getOrDefault(itemId, 0) + (equippedItems.contains(itemId) ? 1 : 0);
	}

	public boolean isEquipped(int itemId)
	{
		return equippedItems.contains(itemId);
	}

	public int getCurrencyQuantity(String currencyName)
	{
		return currencyQuantities.getOrDefault(currencyName, 0);
	}

	public boolean isTypeEnabled(TransportType type)
	{
		return enabledTypes.contains(type);
	}

	public boolean isTeleportsEnabled()
	{
		return teleportsEnabled;
	}

	public int getCurrencyThreshold()
	{
		return currencyThreshold;
	}

	public static final class Builder
	{
		private boolean membersWorld = true;
		private int defaultBoostedLevel = 1;
		private QuestState defaultQuestState = QuestState.NOT_STARTED;
		private final Map<Skill, Integer> boostedLevels = new EnumMap<>(Skill.class);
		private final Map<Quest, QuestState> questStates = new EnumMap<>(Quest.class);
		private final Map<Integer, Integer> varbits = new HashMap<>();
		private final Map<Integer, Integer> varplayers = new HashMap<>();
		private final Map<Integer, Integer> itemQuantities = new HashMap<>();
		private final Set<Integer> equippedItems = new HashSet<>();
		private final Map<String, Integer> currencyQuantities = new HashMap<>();
		private final Set<TransportType> enabledTypes = EnumSet.allOf(TransportType.class);
		private boolean teleportsEnabled = true;
		private int currencyThreshold = Integer.MAX_VALUE;

		private Builder()
		{
		}

		public Builder membersWorld(boolean value)
		{
			membersWorld = value;
			return this;
		}

		public Builder allSkills(int level)
		{
			defaultBoostedLevel = level;
			return this;
		}

		public Builder skill(Skill skill, int level)
		{
			boostedLevels.put(skill, level);
			return this;
		}

		public Builder allQuests(QuestState state)
		{
			defaultQuestState = state;
			return this;
		}

		public Builder quest(Quest quest, QuestState state)
		{
			questStates.put(quest, state);
			return this;
		}

		public Builder varbit(int id, int value)
		{
			varbits.put(id, value);
			return this;
		}

		public Builder varplayer(int id, int value)
		{
			varplayers.put(id, value);
			return this;
		}

		public Builder item(int itemId, int quantity)
		{
			itemQuantities.put(itemId, quantity);
			return this;
		}

		public Builder equipped(int itemId)
		{
			equippedItems.add(itemId);
			return this;
		}

		public Builder currency(String name, int quantity)
		{
			currencyQuantities.put(name, quantity);
			return this;
		}

		public Builder disable(TransportType type)
		{
			enabledTypes.remove(type);
			return this;
		}

		public Builder teleportsEnabled(boolean value)
		{
			teleportsEnabled = value;
			return this;
		}

		public Builder currencyThreshold(int value)
		{
			currencyThreshold = value;
			return this;
		}

		public HeadlessTransportState build()
		{
			return new HeadlessTransportState(this);
		}
	}
}
