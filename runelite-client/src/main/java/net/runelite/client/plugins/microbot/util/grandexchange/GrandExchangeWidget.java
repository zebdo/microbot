package net.runelite.client.plugins.microbot.util.grandexchange;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

public class GrandExchangeWidget
{

	static Widget getOfferContainer()
	{
		return Microbot.getClient().getWidget(InterfaceID.GE_OFFERS, 26);
	}

	static boolean isOfferTextVisible()
	{
		return Rs2Widget.isWidgetVisible(ComponentID.GRAND_EXCHANGE_OFFER_DESCRIPTION);
	}

	static Widget getOfferBuyButton(GrandExchangeSlots slot)
	{
		return getSlotChild(slot, 0);
	}

	static Widget getOfferSellButton(GrandExchangeSlots slot)
	{
		return getSlotChild(slot, 1);
	}

	static Widget getQuantityButton_Minus()
	{
		return getOfferChild(1);
	}

	static Widget getQuantityButton_Plus()
	{
		return getOfferChild(2);
	}

	static Widget getQuantityButton_1()
	{
		return getOfferChild(3);
	}

	static Widget getQuantityButton_10()
	{
		return getOfferChild(4);
	}

	static Widget getQuantityButton_100()
	{
		return getOfferChild(5);
	}

	static Widget getQuantityButton_1000()
	{
		return getOfferChild(6);
	}

	static Widget getQuantityButton_X()
	{
		return getOfferChild(7);
	}

	static Widget getPricePerItemButton_Minus()
	{
		return getOfferChild(8);
	}

	static Widget getPricePerItemButton_Plus()
	{
		return getOfferChild(9);
	}

	static Widget getPricePerItemButton_Minus5Percent()
	{
		return getOfferChild(10);
	}

	static Widget getPricePerItemButton_MinusXPercent()
	{
		return getOfferChild(14);
	}

	static Widget getPricePerItemButton_GuidePrice()
	{
		return getOfferChild(11);
	}

	static Widget getPricePerItemButton_X()
	{
		return getOfferChild(12);
	}

	static Widget getPricePerItemButton_Plus5Percent()
	{
		return getOfferChild(13);
	}

	static Widget getPricePerItemButton_PlusXPercent()
	{
		return getOfferChild(15);
	}

	static Widget getChooseItem()
	{
		return getOfferChild(20);
	}

	static Widget getConfirm()
	{
		var parent = getOfferContainer();

		return Rs2Widget.findWidget("Confirm", Arrays.stream(parent.getDynamicChildren()).collect(Collectors.toList()), true);
	}

	static boolean hasOfferPriceChanged(int basePrice)
	{
		return basePrice != getItemPrice();
	}

	static Widget getItemPriceWidget()
	{
		return getOfferChild(41);
	}

	static int getItemPrice()
	{
		return Integer.parseInt(getItemPriceWidget().getText().replace(" coins", ""));
	}

	static Widget getSlot(GrandExchangeSlots slot)
	{
		switch (slot)
		{
			case ONE:
				return Rs2Widget.getWidget(InterfaceID.GE_OFFERS, 7);
			case TWO:
				return Rs2Widget.getWidget(InterfaceID.GE_OFFERS, 8);
			case THREE:
				return Rs2Widget.getWidget(InterfaceID.GE_OFFERS, 9);
			case FOUR:
				return Rs2Widget.getWidget(InterfaceID.GE_OFFERS, 10);
			case FIVE:
				return Rs2Widget.getWidget(InterfaceID.GE_OFFERS, 11);
			case SIX:
				return Rs2Widget.getWidget(InterfaceID.GE_OFFERS, 12);
			case SEVEN:
				return Rs2Widget.getWidget(InterfaceID.GE_OFFERS, 13);
			case EIGHT:
				return Rs2Widget.getWidget(InterfaceID.GE_OFFERS, 14);
			default:
				return null;
		}
	}

	static Widget[] getCollectButtons()
	{
		Widget parent = Microbot.getClient().getWidget(InterfaceID.GE_OFFERS, 24);
		List<Widget> buttons = new ArrayList<>();
		if (parent != null && parent.getChildren() != null)
		{
			for (Widget child : parent.getChildren())
			{
				if (child.getActions() == null)
				{
					continue;
				}

				if (Arrays.stream(child.getActions()).filter(Objects::nonNull).anyMatch(act -> act.toLowerCase().contains("collect")))
				{
					buttons.add(child);
				}
			}
		}
		return buttons.toArray(new Widget[0]);
	}

	private static Widget getWidgetChild(Widget parent, int childIndex)
	{
		return Optional.ofNullable(parent)
			.map(p -> p.getChild(childIndex))
			.orElse(null);
	}

	private static Widget getOfferChild(int childIndex)
	{
		return getWidgetChild(getOfferContainer(), childIndex);
	}

	private static Widget getSlotChild(GrandExchangeSlots slot, int childIndex)
	{
		return getWidgetChild(getSlot(slot), childIndex);
	}
}