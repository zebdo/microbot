package net.runelite.client.plugins.microbot.util.grandexchange;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Objects;
import java.util.function.Predicate;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.MenuAction;
import net.runelite.api.VarClientStr;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.menu.NewMenuEntry;
import net.runelite.client.plugins.microbot.util.misc.Rs2UiHelper;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.security.Encryption;
import net.runelite.client.plugins.microbot.util.security.Login;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.api.annotations.Component;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static net.runelite.client.plugins.microbot.util.Global.*;

public class Rs2GrandExchange
{
	@Component
	private static final int COLLECT_ALL_BUTTON = 30474246;
	@Component
	private static final int GE_FRAME = InterfaceID.GeOffers.FRAME;
	private static final String GE_TRACKER_API_URL = "https://www.ge-tracker.com/api/items/";

	/**
	 * close the grand exchange interface
	 */
	public static void closeExchange()
	{
		Microbot.status = "Closing Grand Exchange";
		if (!isOpen())
		{
			return;
		}
		Rs2Widget.clickChildWidget(GE_FRAME, 11);
		sleepUntil(() -> !isOpen());
	}

	/**
	 * Back button. Goes back from buy/sell offer screen to all slots overview.
	 */
	public static void backToOverview()
	{
		Microbot.status = "Back to overview";
		if (!isOpen() && !isOfferScreenOpen())
		{
			return;
		}
		Rs2Widget.clickWidget(30474244);
		sleepUntil(() -> !isOfferScreenOpen());
	}

	/**
	 * check if the grand exchange screen is open
	 *
	 * @return
	 */
	public static boolean isOpen()
	{
		return Rs2Widget.isWidgetVisible(InterfaceID.GE_OFFERS, 1);
	}

	/**
	 * Check if the offer screen is open (buy/sell offer screen)
	 *
	 * @return
	 */
	public static boolean isOfferScreenOpen()
	{
		return Rs2Widget.isWidgetVisible(InterfaceID.GE_OFFERS, 23);
	}

	/**
	 * Opens the grand exchange
	 *
	 * @return
	 */
	public static boolean openExchange()
	{
		Microbot.status = "Opening Grand Exchange";
		try
		{
			if (Rs2Inventory.isItemSelected())
			{
				Microbot.getMouse().click();
			}
			if (isOpen())
			{
				return true;
			}
			Rs2NpcModel npc = Rs2Npc.getNpc("Grand Exchange Clerk");
			if (npc == null)
			{
				return false;
			}
			Rs2Npc.interact(npc, "exchange");
			if (Rs2Bank.isBankPinWidgetVisible())
			{
				if ((Login.activeProfile.getBankPin() == null || Login.activeProfile.getBankPin().isEmpty()) || Login.activeProfile.getBankPin().equalsIgnoreCase("**bankpin**"))
				{
					return false;
				}

				Rs2Bank.handleBankPin(Encryption.decrypt(Login.activeProfile.getBankPin()));
			}
			return sleepUntil(Rs2GrandExchange::isOpen, 5000);
		}
		catch (Exception ex)
		{
			Microbot.logStackTrace("Rs2GrandExchange", ex);
		}
		return false;
	}

	public static boolean processOffer(GrandExchangeRequest request) {
		if (!isValidRequest(request) || !useGrandExchange()) {
			return false;
		}

		boolean success = false;

		switch (request.getAction()) {
			case COLLECT:
				Widget offerSlot = request.getSlot() != null ? GrandExchangeWidget.getSlot(request.getSlot()) : null;
				if (offerSlot == null) {
					success = collectAll(request.isToBank());
					break;
				}

				Widget itemNameWidget = offerSlot.getChild(19);
				if (itemNameWidget == null || itemNameWidget.getText() == null) {
					success = collectAll(request.isToBank());
					break;
				}

				String currentItemName = itemNameWidget.getText();
				boolean doesItemMatch = request.isExact()
					? currentItemName.equalsIgnoreCase(request.getItemName())
					: currentItemName.toLowerCase().contains(request.getItemName().toLowerCase());

				if (!doesItemMatch) {
					break;
				}

				viewOffer(offerSlot);
				sleepUntil(Rs2GrandExchange::isOfferScreenOpen);
				success = collectOffer(request.isToBank());
				break;

			case BUY:
				Widget buyOffer = GrandExchangeWidget.getOfferBuyButton(
					request.getSlot() != null ? request.getSlot() : getAvailableSlot());
				if (buyOffer == null) break;

				Rs2Widget.clickWidgetFast(buyOffer);
				sleepUntil(GrandExchangeWidget::isOfferTextVisible);

				Rs2Widget.sleepUntilHasWidgetText("Start typing the name of an item to search for it", 162, 51, false, 5000);
				Rs2Keyboard.typeString(request.getItemName());

				if (!Rs2Widget.sleepUntilHasWidgetText(request.getItemName(), 162, 43, request.isExact(), 5000)) break;

				sleep(1800); // TODO: make this conditional.

				Pair<Widget, Integer> itemResult = getSearchResultWidget(request.getItemName(), request.isExact());
				if (itemResult == null) break;

				Rs2Widget.clickWidgetFast(itemResult.getLeft(), itemResult.getRight(), 1);
				sleepUntil(() -> GrandExchangeWidget.getPricePerItemButton_X() != null);

				setPrice(request.getPrice());
				if (request.getPercent() != 0) {
					adjustPriceByPercent(request.getPercent());
				}
				setQuantity(request.getQuantity());
				confirm();
				success = sleepUntil(() -> !isOfferScreenOpen());
				break;

			case SELL:
				if (!Rs2Inventory.hasItem(request.getItemName(), request.isExact())) break;
				if (getAvailableSlots().length == 0) break;

				if (!Rs2Inventory.interact(request.getItemName(), "Offer", request.isExact())) break;

				sleepUntil(GrandExchangeWidget::isOfferTextVisible);

				if (request.getPrice() > 0) {
					setPrice(request.getPrice());
				}
				if (request.getPercent() != 0) {
					adjustPriceByPercent(request.getPercent());
				}
				if (request.getQuantity() > 0) {
					setQuantity(request.getQuantity());
				}

				confirm();
				success = sleepUntil(() -> !isOfferScreenOpen());
				break;
		}

		if (success && request.isCloseAfterCompletion()) {
			closeExchange();
		}

		return success;
	}


	private static boolean isValidRequest(GrandExchangeRequest request)
	{
		if (request == null || request.getAction() == null)
		{
			return false;
		}

		Predicate<GrandExchangeRequest> DEFAULT_PREDICATE = gxr -> gxr.getItemName() != null && !gxr.getItemName().isBlank() && request.getQuantity() > 0;
		Predicate<GrandExchangeRequest> PRICE_PREDICATE = gxr -> gxr.getPrice() > 0;

		switch (request.getAction())
		{
			case BUY:
				return DEFAULT_PREDICATE.test(request);
			case SELL:
				Predicate<GrandExchangeRequest> combined = DEFAULT_PREDICATE.and(PRICE_PREDICATE);
				return combined.test(request);
			case COLLECT:
				return true;
			default:
				return false;
		}
	}

	private static void viewOffer(Widget widget)
	{
		if (widget == null)
		{
			return;
		}
		// MenuEntryImpl(getOption=View offer, getTarget=, getIdentifier=1, getType=CC_OP, getParam0=2, getParam1=30474247, getItemId=-1, isForceLeftClick=false, getWorldViewId=-1, isDeprioritized=false)
		NewMenuEntry menuEntry = new NewMenuEntry("View offer", "", 1, MenuAction.CC_OP, 2, widget.getId(), false);
		Rectangle bounds = widget.getBounds();
		Microbot.doInvoke(menuEntry, bounds);
	}


	private static boolean collectOffer(boolean toBank)
	{

		assert isOfferScreenOpen() : "Offer screen is not open, cannot collect offer.";

		Widget[] children = GrandExchangeWidget.getCollectButtons();
		String desiredAction;
		int identifier;
		int param0;
		if (children.length == 0)
		{
			return false;
		}

		// MenuEntryImpl(getOption=Bank, getTarget=<col=ff9040>Coins</col>, getIdentifier=3, getType=CC_OP, getParam0=3, getParam1=30474264, getItemId=995, isForceLeftClick=false, getWorldViewId=-1, isDeprioritized=false)
		// MenuEntryImpl(getOption=Collect-notes, getTarget=<col=ff9040>Pure essence</col>, getIdentifier=1, getType=CC_OP, getParam0=2, getParam1=30474264, getItemId=7936, isForceLeftClick=false, getWorldViewId=-1, isDeprioritized=false)
		// MenuEntryImpl(getOption=Collect-items, getTarget=<col=ff9040>Pure essence</col>, getIdentifier=2, getType=CC_OP, getParam0=2, getParam1=30474264, getItemId=7936, isForceLeftClick=false, getWorldViewId=-1, isDeprioritized=false)
		// MenuEntryImpl(getOption=Collect, getTarget=<col=ff9040>Coins</col>, getIdentifier=2, getType=CC_OP, getParam0=2, getParam1=30474264, getItemId=995, isForceLeftClick=false, getWorldViewId=-1, isDeprioritized=false)
		// MenuEntryImpl(getOption=Bank, getTarget=<col=ff9040>Coins</col>, getIdentifier=3, getType=CC_OP, getParam0=2, getParam1=30474264, getItemId=995, isForceLeftClick=false, getWorldViewId=-1, isDeprioritized=false)
		for (int i = 0; i < children.length; i++)
		{
			Widget child = children[i];
			String[] actions = child.getActions();
			if (actions == null || actions.length == 0)
			{
				continue;
			}

			int length = Math.toIntExact(Arrays.stream(actions).filter(Objects::nonNull).count());

			if (length > 3)
			{
				identifier = toBank ? 3 : child.getItemQuantity() == 1 ? 2 : 1;
				desiredAction = toBank ? "Bank" : child.getItemQuantity() == 1 ? "Collect-items" : "Collect-notes";
			}
			else
			{
				identifier = toBank ? 3 : 2;
				desiredAction = toBank ? "Bank" : "Collect";
			}
			param0 = i == 0 ? 2 : 3;
			NewMenuEntry menuEntry = new NewMenuEntry(desiredAction, "", identifier, MenuAction.CC_OP, param0, child.getId(), false);
			Rectangle bounds = child.getBounds() != null && Rs2UiHelper.isRectangleWithinCanvas(child.getBounds()) ? child.getBounds() : Rs2UiHelper.getDefaultRectangle();
			Microbot.doInvoke(menuEntry, bounds);
			if (!Rs2AntibanSettings.naturalMouse)
			{
				sleep(250, 750);
			}
		}
		return sleepUntil(() -> !isOfferScreenOpen());
	}

	private static void adjustPriceByPercent(int percent)
	{
		if (percent == 0)
		{
			return;
		}

		boolean isIncrease = percent > 0;
		int absPercent = Math.abs(percent);
		int basePrice = Microbot.getVarbitValue(VarbitID.GE_NEWOFFER_TYPE);

		if (absPercent % 5 == 0)
		{
			Widget adjust5Widget = isIncrease
				? GrandExchangeWidget.getPricePerItemButton_Plus5Percent()
				: GrandExchangeWidget.getPricePerItemButton_Minus5Percent();

			if (adjust5Widget == null)
			{
				Microbot.log("Unable to find +-5% button widget.");
				return;
			}

			int times = absPercent / 5;
			IntStream.range(0, times).forEach(i -> {
				Rs2Widget.clickWidget(adjust5Widget);
				sleepUntil(() -> GrandExchangeWidget.hasOfferPriceChanged(basePrice), 1600);
			});
		}
		else
		{
			Widget adjustXWidget = isIncrease
				? GrandExchangeWidget.getPricePerItemButton_PlusXPercent()
				: GrandExchangeWidget.getPricePerItemButton_MinusXPercent();

			if (adjustXWidget == null)
			{
				Microbot.log("Unable to find +-X% button widget.");
				return;
			}

			int currentPercent = Rs2UiHelper.extractNumber(adjustXWidget.getText());

			if (currentPercent != absPercent)
			{
				if (currentPercent == -1)
				{
					Rs2Widget.clickWidget(adjustXWidget);
				}
				else
				{
//					MenuEntryImpl(getOption=Customise, getTarget=, getIdentifier=2, getType=CC_OP, getParam0=14, getParam1=30474266, getItemId=-1, isForceLeftClick=false, getWorldViewId=-1, isDeprioritized=false)
//					MenuEntryImpl(getOption=Customise, getTarget=, getIdentifier=2, getType=CC_OP, getParam0=15, getParam1=30474266, getItemId=-1, isForceLeftClick=false, getWorldViewId=-1, isDeprioritized=false)
					NewMenuEntry menuEntry = new NewMenuEntry("Customise", "", 2, MenuAction.CC_OP, isIncrease ? 15 : 14, adjustXWidget.getId(), false);
					Rectangle bounds = adjustXWidget.getBounds() != null && Rs2UiHelper.isRectangleWithinCanvas(adjustXWidget.getBounds()) ? adjustXWidget.getBounds() : Rs2UiHelper.getDefaultRectangle();
					Microbot.doInvoke(menuEntry, bounds);
				}

				sleepUntil(() -> Rs2Widget.hasWidget("Set a percentage to decrease/increase"), 2000);
				Rs2Keyboard.typeString(Integer.toString(absPercent));
				Rs2Keyboard.enter();
				sleepUntil(() -> {
					Widget updatedWidget = isIncrease
						? GrandExchangeWidget.getPricePerItemButton_PlusXPercent()
						: GrandExchangeWidget.getPricePerItemButton_MinusXPercent();
					return updatedWidget != null && Rs2UiHelper.extractNumber(updatedWidget.getText()) != currentPercent;
				}, 2000);
			}

			Rs2Widget.clickWidget(adjustXWidget);
			sleepUntil(() -> GrandExchangeWidget.hasOfferPriceChanged(basePrice), 2000);
		}
	}


	/**
	 * @param itemName
	 * @param price
	 * @param quantity
	 * @return true if item has been bought succesfully
	 */
	public static boolean buyItem(String itemName, int price, int quantity)
	{
		GrandExchangeRequest request = GrandExchangeRequest.builder()
			.action(GrandExchangeAction.BUY)
			.itemName(itemName)
			.price(price)
			.quantity(quantity)
			.build();
		return processOffer(request);
	}

	private static void confirm()
	{
		Microbot.getMouse().click(GrandExchangeWidget.getConfirm().getBounds());
		sleepUntil(() -> Rs2Widget.hasWidget("Your offer is much higher"), 2000);
		if (Rs2Widget.hasWidget("Your offer is much higher"))
		{
			Rs2Widget.clickWidget("Yes");
		}
	}

	private static void setQuantity(int quantity)
	{
		if (quantity != getOfferQuantity())
		{
			Widget quantityButtonX = GrandExchangeWidget.getQuantityButton_X();
			Microbot.getMouse().click(quantityButtonX.getBounds());
			sleepUntil(() -> Rs2Widget.getWidget(InterfaceID.Chatbox.MES_TEXT2) != null); //GE Enter Price/Quantity
			sleep(600, 1000);
			setChatboxValue(quantity);
			sleep(500, 750);
			Rs2Keyboard.enter();
			sleep(1000);
		}
	}

	private static void setPrice(int price)
	{
		if (price != getOfferPrice())
		{
			Widget pricePerItemButtonX = GrandExchangeWidget.getPricePerItemButton_X();
			Microbot.getMouse().click(pricePerItemButtonX.getBounds());
			sleepUntil(() -> Rs2Widget.getWidget(InterfaceID.Chatbox.MES_TEXT2) != null); //GE Enter Price
			sleep(600, 1000);
			setChatboxValue(price);
			sleep(500, 750);
			Rs2Keyboard.enter();
			sleep(1000);
		}

	}

	private static boolean useGrandExchange()
	{
		return isOpen() || openExchange() || walkToGrandExchange();
	}

	/**
	 * Sell item to the grand exchange
	 *
	 * @param itemName name of the item to sell
	 * @param quantity quantity of the item to sell
	 * @param price    price of the item to sell
	 * @return
	 */
	public static boolean sellItem(String itemName, int quantity, int price)
	{
		GrandExchangeRequest request = GrandExchangeRequest.builder()
			.action(GrandExchangeAction.SELL)
			.itemName(itemName)
			.quantity(quantity)
			.price(price)
			.build();
		return processOffer(request);
	}

	/**
	 * Collect all the grand exchange slots to the bank or inventory
	 *
	 * @param collectToBank
	 * @return
	 */
	public static boolean collectAll(boolean collectToBank)
	{
		if (isAllSlotsEmpty())
		{
			return true;
		}
		if (Rs2Inventory.isFull())
		{
			if (Rs2Bank.useBank())
			{
				Rs2Bank.depositAll();
			}
		}
		if (!isOpen())
		{
			openExchange();
		}
		sleepUntil(Rs2GrandExchange::isOpen);
		Widget collectButton = Rs2Widget.getWidget(COLLECT_ALL_BUTTON);
		if (collectButton == null)
		{
			return false;
		}
		// MenuEntryImpl(getOption=Collect to bank, getTarget=, getIdentifier=2, getType=CC_OP, getParam0=0, getParam1=30474246, getItemId=-1, isForceLeftClick=false, getWorldViewId=-1, isDeprioritized=false)
		// MenuEntryImpl(getOption=Collect to inventory, getTarget=, getIdentifier=1, getType=CC_OP, getParam0=0, getParam1=30474246, getItemId=-1, isForceLeftClick=false, getWorldViewId=-1, isDeprioritized=false)
		NewMenuEntry entry = new NewMenuEntry(collectToBank ? "Collect to bank" : "Collect to inventory", "", collectToBank ? 2 : 1, MenuAction.CC_OP, 0, collectButton.getId(), false);
		Rectangle bounds = collectButton.getBounds() != null && Rs2UiHelper.isRectangleWithinCanvas(collectButton.getBounds())
			? collectButton.getBounds()
			: Rs2UiHelper.getDefaultRectangle();
		Microbot.doInvoke(entry, bounds);
		return true;
	}

	public static boolean collectAllToInventory()
	{
		return collectAll(false);
	}

	/**
	 * Collect all the grand exchange items to your bank
	 *
	 * @return
	 */
	public static boolean collectAllToBank()
	{
		return collectAll(true);
	}

	/**
	 * sells all the tradeable loot items from a specific npc name
	 *
	 * @param npcName
	 * @return true if there is no more loot to sell
	 */
	public static boolean sellLoot(String npcName, List<String> itemsToNotSell)
	{

		boolean withdrewLootItems = Rs2Bank.withdrawLootItems(npcName, itemsToNotSell);

		if (withdrewLootItems)
		{
			return sellInventory();
		}


		return false;
	}

	/**
	 * Sells all the tradeable items in your inventory
	 *
	 * @return
	 */
	public static boolean sellInventory()
	{
		Rs2Inventory.items().forEachOrdered(item -> {
			if (!item.isTradeable())
			{
				return;
			}

			if (Rs2GrandExchange.getAvailableSlot() == null && Rs2GrandExchange.hasSoldOffer())
			{
				Rs2GrandExchange.collectAllToBank();
				sleep(600);
			}

			// Sells at 5% under current offer price.
			GrandExchangeRequest request = GrandExchangeRequest.builder()
				.action(GrandExchangeAction.SELL)
				.itemName(item.getName())
				.percent(-5)
				.build();

			processOffer(request);
		});
		return Rs2Inventory.isEmpty();
	}

	/**
	 * Aborts the offer
	 *
	 * @param name          name of the item to abort offer on
	 * @param collectToBank collect the item to the bank
	 * @return true if the offer has been aborted
	 */
	public static boolean abortOffer(String name, boolean collectToBank)
	{
		if (!useGrandExchange())
		{
			return false;
		}

		Optional<GrandExchangeSlots> matchingSlot = Arrays.stream(GrandExchangeSlots.values())
			.filter(slot -> {
				Widget parent = GrandExchangeWidget.getSlot(slot);
				if (parent == null || isSlotAvailable(slot))
				{
					return false;
				}

				Widget child = parent.getChild(19);
				return child != null && child.getText().equalsIgnoreCase(name);
			})
			.findFirst();

		if (matchingSlot.isEmpty())
		{
			return false;
		}

		Widget parent = GrandExchangeWidget.getSlot(matchingSlot.get());
		NewMenuEntry menuEntry = new NewMenuEntry("Abort offer", "", 2, MenuAction.CC_OP, 2, parent.getId(), false);
		Rectangle bounds = parent.getBounds() != null && Rs2UiHelper.isRectangleWithinCanvas(parent.getBounds())
			? parent.getBounds()
			: Rs2UiHelper.getDefaultRectangle();
		Microbot.doInvoke(menuEntry, bounds);
		return collectAll(collectToBank);
	}

	/**
	 * Aborts all offers
	 *
	 * @param collectToBank collect the items to the bank
	 * @return true if all offers were aborted successfully
	 */
	public static boolean abortAllOffers(boolean collectToBank)
	{
		if (!useGrandExchange())
		{
			return false;
		}

		Arrays.stream(GrandExchangeSlots.values())
			.filter(slot -> {
				Widget parent = GrandExchangeWidget.getSlot(slot);
				return parent != null && !isSlotAvailable(slot);
			})
			.forEach(slot -> {
				Widget parent = GrandExchangeWidget.getSlot(slot);
				NewMenuEntry menuEntry = new NewMenuEntry("Abort offer", "", 2, MenuAction.CC_OP, 2, parent.getId(), false);
				Rectangle bounds = parent.getBounds() != null && Rs2UiHelper.isRectangleWithinCanvas(parent.getBounds())
					? parent.getBounds()
					: Rs2UiHelper.getDefaultRectangle();
				Microbot.doInvoke(menuEntry, bounds);
				if (!Rs2AntibanSettings.naturalMouse)
				{
					sleep(250, 750);
				}
			});

		sleep(1000);
		collectAll(collectToBank);
		return isAllSlotsEmpty();
	}

	public static Pair<Widget, Integer> getSearchResultWidget(String search, boolean exact)
	{
		Widget parent = Microbot.getClientThread().runOnClientThreadOptional(() -> Microbot.getClient().getWidget(InterfaceID.Chatbox.MES_LAYER_SCROLLCONTENTS)).orElse(null);

		if (parent == null || parent.getChildren() == null)
		{
			return null;
		}

		Widget child = Arrays.stream(parent.getChildren()).filter(x -> {
				String widgetText = Rs2UiHelper.stripColTags(x.getText());
				return exact ? widgetText.equalsIgnoreCase(search) : widgetText.toLowerCase().contains(search.toLowerCase());
			})
			.findFirst()
			.orElse(null);

		if (child != null)
		{
			List<Widget> children = Arrays.stream(parent.getChildren()).collect(Collectors.toList());
			int index = children.indexOf(child);
			int originalWidgetIndex = index - 1;
			return Pair.of(children.get(originalWidgetIndex), originalWidgetIndex);
		}
		return null;
	}


	public static boolean isSlotAvailable(GrandExchangeSlots slot)
	{
		Widget parent = GrandExchangeWidget.getSlot(slot);
		return Optional.ofNullable(parent)
			.map(p -> {
				Widget child = p.getChild(2);
				if (child == null) return false;
				return child.isSelfHidden();
			})
			.orElse(false);
	}

	/**
	 * Returns all slots with active offers
	 *
	 * @return array of slots containing active offers
	 */
	public static GrandExchangeSlots[] getActiveOfferSlots()
	{
		return Arrays.stream(GrandExchangeSlots.values())
			.filter(slot -> {
				Widget parent = GrandExchangeWidget.getSlot(slot);
				return parent != null && !isSlotAvailable(slot);
			})
			.toArray(GrandExchangeSlots[]::new);
	}

	public static boolean isAllSlotsEmpty()
	{
		return Arrays.stream(getAvailableSlots()).count() == getMaxSlots();
	}

	public static boolean hasBoughtOffer()
	{
		return Arrays.stream(Microbot.getClient().getGrandExchangeOffers()).anyMatch(x -> x.getState() == GrandExchangeOfferState.BOUGHT);
	}

	public static boolean hasFinishedBuyingOffers()
	{
		GrandExchangeOffer[] offers = Microbot.getClient().getGrandExchangeOffers();
		boolean hasBought = Arrays.stream(offers)
			.anyMatch(offer -> offer.getState() == GrandExchangeOfferState.BOUGHT);
		boolean isBuying = Arrays.stream(offers)
			.anyMatch(offer -> offer.getState() == GrandExchangeOfferState.BUYING);
		return hasBought && !isBuying;
	}

	public static boolean hasSoldOffer()
	{
		return Arrays.stream(Microbot.getClient().getGrandExchangeOffers()).anyMatch(x -> x.getState() == GrandExchangeOfferState.SOLD);
	}

	public static boolean hasFinishedSellingOffers()
	{
		GrandExchangeOffer[] offers = Microbot.getClient().getGrandExchangeOffers();
		boolean hasSold = Arrays.stream(offers)
			.anyMatch(offer -> offer.getState() == GrandExchangeOfferState.SOLD);
		boolean isSelling = Arrays.stream(offers)
			.anyMatch(offer -> offer.getState() == GrandExchangeOfferState.SELLING);
		return hasSold && !isSelling;
	}


	private static int getMaxSlots()
	{
		return Rs2Player.isMember() ? 8 : 3;
	}

	public static boolean walkToGrandExchange()
	{
		return Rs2Walker.walkTo(BankLocation.GRAND_EXCHANGE.getWorldPoint());
	}


	public static int getOfferPrice(int itemId)
	{
		HttpClient httpClient = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(GE_TRACKER_API_URL + itemId))
			.build();

		try
		{
			String jsonResponse = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
				.thenApply(HttpResponse::body)
				.join();

			JsonParser parser = new JsonParser();
			JsonObject jsonElement = parser.parse(new StringReader(jsonResponse)).getAsJsonObject();
			JsonObject data = jsonElement.getAsJsonObject("data");

			return data.get("buying").getAsInt();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return -1;
		}
	}

	public static int getSellPrice(int itemId)
	{
		HttpClient httpClient = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(GE_TRACKER_API_URL + itemId))
			.build();

		try
		{
			String jsonResponse = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
				.thenApply(HttpResponse::body)
				.join();

			JsonParser parser = new JsonParser();
			JsonObject jsonElement = parser.parse(new StringReader(jsonResponse)).getAsJsonObject();
			JsonObject data = jsonElement.getAsJsonObject("data");

			return data.get("selling").getAsInt();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return -1;
		}
	}

	public static int getPrice(int itemId)
	{
		HttpClient httpClient = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(GE_TRACKER_API_URL + itemId))
			.build();

		try
		{
			String jsonResponse = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
				.thenApply(HttpResponse::body)
				.join();

			JsonParser parser = new JsonParser();
			JsonObject jsonElement = parser.parse(new StringReader(jsonResponse)).getAsJsonObject();
			JsonObject data = jsonElement.getAsJsonObject("data");

			return data.get("overall").getAsInt();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return -1;
		}
	}

	public static int getBuyingVolume(int itemId)
	{
		HttpClient httpClient = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(GE_TRACKER_API_URL + itemId))
			.build();

		try
		{
			String jsonResponse = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
				.thenApply(HttpResponse::body)
				.join();

			JsonParser parser = new JsonParser();
			JsonObject jsonElement = parser.parse(new StringReader(jsonResponse)).getAsJsonObject();
			JsonObject data = jsonElement.getAsJsonObject("data");

			return data.get("buyingQuantity").getAsInt();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return -1;
		}
	}

	public static int getSellingVolume(int itemId)
	{
		HttpClient httpClient = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(GE_TRACKER_API_URL + itemId))
			.build();

		try
		{
			String jsonResponse = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
				.thenApply(HttpResponse::body)
				.join();

			JsonParser parser = new JsonParser();
			JsonObject jsonElement = parser.parse(new StringReader(jsonResponse)).getAsJsonObject();
			JsonObject data = jsonElement.getAsJsonObject("data");

			return data.get("sellingQuantity").getAsInt();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return -1;
		}
	}

	static int getOfferQuantity()
	{
		return Microbot.getVarbitValue(4396);
	}

	static int getOfferPrice()
	{
		return Microbot.getVarbitValue(4398);
	}

	public static void setChatboxValue(int value)
	{
		var chatboxInputWidget = Rs2Widget.getWidget(InterfaceID.Chatbox.MES_TEXT2);
		if (chatboxInputWidget == null)
		{
			return;
		}
		chatboxInputWidget.setText(value + "*");
		Microbot.getClientThread().runOnClientThreadOptional(() -> {
			Microbot.getClient().setVarcStrValue(VarClientStr.INPUT_TEXT, String.valueOf(value));
			return null;
		});

	}

	public static GrandExchangeSlots getAvailableSlot()
	{
		GrandExchangeSlots[] result = getAvailableSlots();
		return Arrays.stream(result).findFirst().orElse(null);
	}

	public static GrandExchangeSlots[] getAvailableSlots()
	{
		int maxSlots = getMaxSlots();

		return Arrays.stream(GrandExchangeSlots.values())
			.limit(maxSlots)
			.filter(Rs2GrandExchange::isSlotAvailable).toArray(GrandExchangeSlots[]::new);
	}
}
