package net.runelite.client.plugins.microbot.util.inventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Varbits;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.Global;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.magic.Runes;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

/**
 * Utility class to manage and interact with the player's Rune Pouch.
 * Includes methods for reading, loading, and depositing rune pouch contents.
 */
public class Rs2RunePouch
{
	private static final int NUM_SLOTS = 4;
	private static final int[] AMOUNT_VARBITS = {
		Varbits.RUNE_POUCH_AMOUNT1,
		Varbits.RUNE_POUCH_AMOUNT2,
		Varbits.RUNE_POUCH_AMOUNT3,
		Varbits.RUNE_POUCH_AMOUNT4
	};
	private static final int[] RUNE_VARBITS = {
		Varbits.RUNE_POUCH_RUNE1,
		Varbits.RUNE_POUCH_RUNE2,
		Varbits.RUNE_POUCH_RUNE3,
		Varbits.RUNE_POUCH_RUNE4
	};

	private static final int BANK_PARENT_ID = InterfaceID.BANKSIDE;
	private static final int RUNEPOUCH_ROOT_CHILD_ID = 19;
	private static final int RUNEPOUCH_CLOSE_CHILD_ID = 22;
	private static final int RUNEPOUCH_DEPOSIT_ALL_CHILD_ID = 20;
	private static final List<Integer> RUNEPOUCH_LOADOUT_WIDGETS = Arrays.asList(28, 30, 32, 34);

	@Getter
	private static final List<PouchSlot> slots = new ArrayList<>();

	@Getter
	private static final Map<Integer, List<PouchSlot>> loadoutSlots = IntStream.range(0, NUM_SLOTS)
		.boxed()
		.collect(Collectors.toMap(
			i -> i,
			i -> new ArrayList<>()
		));

	/**
	 * Updates the internal state of the pouch from the current varbits.
	 */
	public static void fullUpdate()
	{
		if (!isEmpty())
		{
			slots.clear();
		}
		for (int i = 0; i < NUM_SLOTS; i++)
		{
			int rawRune = Microbot.getVarbitValue(RUNE_VARBITS[i]);
			int rawQuantity = Microbot.getVarbitValue(AMOUNT_VARBITS[i]);
			PouchSlot slot = new PouchSlot(Runes.byVarbitId(rawRune), rawQuantity);
			slots.add(slot);
		}
	}

	/**
	 * Updates the specific pouch slot when a varbit is changed.
	 *
	 * @param ev The varbit changed event.
	 */
	public static void onVarbitChanged(VarbitChanged ev)
	{

		assert Microbot.getClient().isClientThread();

		int varbitId = ev.getVarbitId();
		for (int i = 0; i < NUM_SLOTS; i++)
		{
			if (varbitId == RUNE_VARBITS[i])
			{
				int rawRune = Microbot.getClient().getVarbitValue(varbitId);
				slots.get(i).setRune(Runes.byVarbitId(rawRune));
				break;
			}
			if (varbitId == AMOUNT_VARBITS[i])
			{
				slots.get(i).setQuantity(Microbot.getClient().getVarbitValue(varbitId));
				break;
			}
		}
	}

	/**
	 * Handles reading rune pouch loadouts from the bank interface widgets.
	 *
	 * @param ev The widget loaded event.
	 */
	public static void onWidgetLoaded(WidgetLoaded ev)
	{

		assert Microbot.getClient().isClientThread();

		if (ev.getGroupId() != BANK_PARENT_ID)
		{
			return;
		}

		for (int loadOutIndex : RUNEPOUCH_LOADOUT_WIDGETS)
		{
			int index = RUNEPOUCH_LOADOUT_WIDGETS.indexOf(loadOutIndex);
			Widget rootLoadoutWidget = Microbot.getClient().getWidget(BANK_PARENT_ID, loadOutIndex);
			if (rootLoadoutWidget == null || rootLoadoutWidget.getDynamicChildren() == null)
			{
				continue;
			}

			List<PouchSlot> pouchSlots = new ArrayList<>();

			for (Widget childWidget : rootLoadoutWidget.getDynamicChildren())
			{
				if (childWidget.getIndex() == 0 || childWidget.getIndex() > 4)
				{
					continue;
				}

				int itemId = childWidget.getItemId();
				int quantity = childWidget.getItemQuantity();

				// Ignore X's or empty item slots
				Predicate<Integer> invalidItemId = i -> (i == 11526 || i == 6512 || i == 0);

				if (invalidItemId.test(itemId) || quantity <= 0)
				{
					continue;
				}

				PouchSlot slot = new PouchSlot(Runes.byItemId(itemId), quantity);
				pouchSlots.add(slot);
			}

			loadoutSlots.put(index, pouchSlots);
		}
	}

	/**
	 * Checks if the rune pouch is currently empty.
	 *
	 * @return True if all slots are empty, false otherwise.
	 */
	public static boolean isEmpty()
	{
		return slots.stream().noneMatch(s -> s.getRune() != null);
	}

	/**
	 * Checks if the rune pouch contains the given item ID.
	 *
	 * @param itemId The item ID to check.
	 * @return True if present, false otherwise.
	 */
	public static boolean contains(int itemId)
	{
		return slots.stream()
			.filter(s -> s.getRune() != null && s.getQuantity() > 0)
			.anyMatch(s -> s.getRune().getItemId() == itemId);
	}

	/**
	 * Checks if the rune pouch contains the given rune.
	 *
	 * @param rune The rune to check.
	 * @return True if present, false otherwise.
	 */
	public static boolean contains(Runes rune)
	{
		return slots.stream()
			.filter(s -> s.getRune() != null && s.getQuantity() > 0)
			.anyMatch(s -> Objects.equals(s.getRune(), rune));
	}

	/**
	 * Checks if the rune pouch contains at least the specified amount of the given item ID.
	 *
	 * @param itemId The item ID.
	 * @param amt    The required quantity.
	 * @return True if sufficient quantity is present, false otherwise.
	 */
	public static boolean contains(int itemId, int amt)
	{
		return slots.stream()
			.filter(s -> s.getRune() != null && s.getRune().getItemId() == itemId)
			.anyMatch(s -> s.getQuantity() >= amt);
	}

	/**
	 * Gets the quantity of the specified item ID in the rune pouch.
	 *
	 * @param itemId The item ID.
	 * @return Quantity present or 0 if not found.
	 */
	public static int getQuantity(int itemId)
	{
		return slots.stream()
			.filter(s -> s.getRune() != null && s.getRune().getItemId() == itemId)
			.mapToInt(PouchSlot::getQuantity)
			.findFirst()
			.orElse(0);
	}

	/**
	 * Checks if the rune pouch contains the required runes.
	 *
	 * @param requiredRunes Map of runes to required amounts.
	 * @param exact         If true, requires exact match.
	 * @return True if the required runes are contained.
	 */
	public static boolean contains(Map<Runes, Integer> requiredRunes, boolean exact)
	{
		if (requiredRunes == null || requiredRunes.isEmpty())
		{
			return true;
		}

		Map<Runes, Integer> current = slots.stream()
			.filter(s -> s.getRune() != null && s.getQuantity() > 0)
			.collect(Collectors.toMap(
				PouchSlot::getRune,
				PouchSlot::getQuantity
			));

		Map<Runes, Integer> filteredRequired = requiredRunes.entrySet().stream()
			.filter(e -> e.getKey() != null && e.getValue() != null && e.getValue() > 0)
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


		return exact ? current.equals(filteredRequired) :
			filteredRequired.entrySet().stream().allMatch(e -> current.getOrDefault(e.getKey(), 0) >= e.getValue());
	}

	/**
	 * Checks if the rune pouch contains at least the required runes.
	 *
	 * @param requiredRunes Map of required runes and amounts.
	 * @return True if all required runes are present in sufficient quantities.
	 */
	public static boolean contains(Map<Runes, Integer> requiredRunes)
	{
		return contains(requiredRunes, false);
	}

	/**
	 * Loads the required runes into the rune pouch either from a saved loadout or by withdrawing from the bank.
	 *
	 * @param requiredRunes Runes and their quantities to load.
	 * @return True if the runes were successfully loaded, false otherwise.
	 */
	public static boolean load(Map<Runes, Integer> requiredRunes)
	{
		if (!Rs2Bank.isOpen() || !Rs2Inventory.hasRunePouch())
		{
			return false;
		}

		if (contains(requiredRunes, false))
		{
			return true;
		}

		Rs2Inventory.interact(RunePouchType.getPouchIds(), "Configure");
		Global.sleepUntil(() -> Rs2Widget.isWidgetVisible(BANK_PARENT_ID, RUNEPOUCH_ROOT_CHILD_ID));

		for (Map.Entry<Integer, List<PouchSlot>> entry : loadoutSlots.entrySet())
		{
			List<PouchSlot> loadout = entry.getValue();

			Map<Runes, Integer> loadoutMap = loadout.stream()
				.filter(slot -> slot.getRune() != null && slot.getQuantity() > 0)
				.collect(Collectors.toMap(
					PouchSlot::getRune,
					PouchSlot::getQuantity
				));

			if (loadoutMap.equals(requiredRunes))
			{
				int widgetIndex = RUNEPOUCH_LOADOUT_WIDGETS.get(entry.getKey());
				Rs2Widget.clickWidget(BANK_PARENT_ID, (widgetIndex + 1));
				Global.sleepUntil(() -> getRunes().entrySet().stream().allMatch(e -> requiredRunes.getOrDefault(Runes.byItemId(e.getKey()), 0) <= e.getValue()));
				return closeRunePouch();
			}
		}

		if (!depositAll())
		{
			return false;
		}

		// Withdraw runes from the bank
		for (Map.Entry<Runes, Integer> entry : requiredRunes.entrySet())
		{
			Runes rune = entry.getKey();
			int qty = entry.getValue();

			if (!Rs2Bank.withdrawX(rune.getItemId(), qty))
			{
				Microbot.log("Failed to withdraw rune: " + rune + " x" + qty, Level.WARNING);
				return false;
			}
			Global.sleepUntil(() -> contains(rune.getItemId(), qty));
		}

		Global.sleepUntil(() -> {
			Map<Integer, Integer> currentRunes = getRunes();
			return requiredRunes.entrySet().stream().allMatch(e -> currentRunes.getOrDefault(e.getKey().getItemId(), 0) >= e.getValue());
		});

		return closeRunePouch();
	}

	/**
	 * Retrieves the current rune pouch contents.
	 *
	 * @return A map of rune item IDs to their quantities.
	 */
	public static Map<Integer, Integer> getRunes()
	{
		return slots.stream()
			.filter(s -> s.getRune() != null && s.getQuantity() > 0)
			.collect(Collectors.toUnmodifiableMap(
				s -> s.getRune().getItemId(),
				PouchSlot::getQuantity
			));
	}

	/**
	 * Deposits all runes from the pouch into the bank.
	 *
	 * @return True if all runes were successfully deposited.
	 */
	public static boolean depositAll()
	{
		if (isEmpty())
		{
			return true;
		}
		if (!Rs2Bank.isOpen() || !Rs2Inventory.hasRunePouch())
		{
			return false;
		}

		if (!isRunePouchOpen())
		{
			openRunePouch();
		}

		Rs2Widget.clickWidget(BANK_PARENT_ID, RUNEPOUCH_DEPOSIT_ALL_CHILD_ID);
		return Global.sleepUntil(Rs2RunePouch::isEmpty);
	}

	/**
	 * Checks if the rune pouch UI is currently open.
	 *
	 * @return True if open.
	 */
	public static boolean isRunePouchOpen()
	{
		return Rs2Widget.isWidgetVisible(BANK_PARENT_ID, RUNEPOUCH_ROOT_CHILD_ID);
	}

	/**
	 * Opens the rune pouch UI.
	 *
	 * @return True if the pouch was successfully opened.
	 */
	public static boolean openRunePouch()
	{
		if (isRunePouchOpen())
		{
			return true;
		}
		Rs2Inventory.interact(RunePouchType.getPouchIds(), "Configure");
		return Global.sleepUntil(Rs2RunePouch::isRunePouchOpen);
	}

	/**
	 * Closes the rune pouch UI.
	 *
	 * @return True if the pouch was successfully closed.
	 */
	public static boolean closeRunePouch()
	{
		if (!isRunePouchOpen())
		{
			return true;
		}
		Rs2Widget.clickWidget(BANK_PARENT_ID, RUNEPOUCH_CLOSE_CHILD_ID);
		return Global.sleepUntil(() -> !isRunePouchOpen());
	}

	/**
	 * Represents a slot inside the rune pouch, containing a rune and its quantity.
	 */
	@Getter
	@Setter
	private static class PouchSlot
	{
		PouchSlot(Runes rune, int quantity)
		{
			this.rune = rune;
			this.quantity = quantity;
		}

		private Runes rune;
		private int quantity;
	}
}