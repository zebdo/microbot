package net.runelite.client.plugins.microbot.inventorysetups;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InventorySetupLayoutType
{
	// None
	PRESET("Preset", 0),

	// 3 slots
	ZIGZAG("ZigZag", 1);

	@Override
	public String toString()
	{
		return name;
	}

	private final String name;
	private final int identifier;
}
