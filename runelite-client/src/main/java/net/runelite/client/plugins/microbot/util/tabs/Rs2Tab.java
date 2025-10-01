package net.runelite.client.plugins.microbot.util.tabs;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

@Slf4j
public class Rs2Tab {
    private static final int TAB_SWITCH_SCRIPT = 915;

    public static InterfaceTab getCurrentTab() {
        final int varcIntValue = Microbot.getClient().getVarcIntValue(VarClientID.TOPLEVEL_PANEL);
        switch (varcIntValue) {
            case 0:
                return InterfaceTab.COMBAT;
            case 1:
                return InterfaceTab.SKILLS;
            case 2:
                return InterfaceTab.QUESTS;
            case 3:
                return InterfaceTab.INVENTORY;
            case 4:
                return InterfaceTab.EQUIPMENT;
            case 5:
                return InterfaceTab.PRAYER;
            case 6:
                return InterfaceTab.MAGIC;
            case 7:
                return InterfaceTab.FRIENDS;
            case 8:
                return InterfaceTab.LOGOUT;
            case 9:
                return InterfaceTab.SETTINGS;
            case 10:
                return InterfaceTab.MUSIC;
            case 11:
                return InterfaceTab.CHAT;
            case 12:
                return InterfaceTab.ACC_MAN;
            case 13:
                return InterfaceTab.EMOTES;
            case -1:
                return InterfaceTab.NOTHING_SELECTED;
            default:
                throw new IllegalStateException("Unexpected value: " + varcIntValue);
        }
    }

    public static boolean isCurrentTab(InterfaceTab tab) {
        return getCurrentTab() == tab;
    }

    public static boolean switchTo(InterfaceTab tab) {
        if (isCurrentTab(tab)) return true;

        if (tab == InterfaceTab.NOTHING_SELECTED && Microbot.getVarbitValue(VarbitID.RESIZABLE_STONE_ARRANGEMENT) == 0)
            return false;

        int hotkey = tab.getHotkey();
        if (hotkey == -1) {
            log.warn("Tab {} does not have a hotkey assigned, cannot switch to it.", tab.getName());
            return false;
        } else {
            Rs2Keyboard.keyPress(hotkey);
        }

        return sleepUntil(() -> isCurrentTab(tab));
    }

    @Deprecated(since = "Use switchTo")
    public static boolean switchToInventoryTab() {
        return switchTo(InterfaceTab.INVENTORY);
    }

    @Deprecated(since = "Use switchTo")
    public static boolean switchToCombatOptionsTab() {
        return switchTo(InterfaceTab.COMBAT);
    }

    @Deprecated(since = "Use switchTo")
    public static boolean switchToSkillsTab() {
        return switchTo(InterfaceTab.SKILLS);
    }

    @Deprecated(since = "Use switchTo")
    public static boolean switchToQuestTab() {
        return switchTo(InterfaceTab.QUESTS);
    }

    @Deprecated(since = "Use switchTo")
    public static boolean switchToEquipmentTab() {
        return switchTo(InterfaceTab.EQUIPMENT);
    }

    @Deprecated(since = "Use switchTo")
    public static boolean switchToPrayerTab() {
        return switchTo(InterfaceTab.PRAYER);
    }

    @Deprecated(since = "Use switchTo")
    public static boolean switchToMagicTab() {
        return switchTo(InterfaceTab.MAGIC);
    }

    @Deprecated(since = "Use switchTo")
    public static boolean switchToGroupingTab() {
        return switchTo(InterfaceTab.CHAT);
    }

    @Deprecated(since = "Use switchTo")
    public static boolean switchToFriendsTab() {
        return switchTo(InterfaceTab.FRIENDS);
    }

    @Deprecated(since = "Use switchTo")
    public static boolean switchToAccountManagementTab() {
        return switchTo(InterfaceTab.ACC_MAN);
    }

    @Deprecated(since = "Use switchTo")
    public static boolean switchToSettingsTab() {
        return switchTo(InterfaceTab.SETTINGS);
    }

    @Deprecated(since = "Use switchTo")
    public static boolean switchToEmotesTab() {
        return switchTo(InterfaceTab.EMOTES);
    }

    @Deprecated(since = "Use switchTo")
    public static boolean switchToMusicTab() {
        return switchTo(InterfaceTab.MUSIC);
    }

    @Deprecated(since = "Use switchTo")
    public static boolean switchToLogout() {
        return switchTo(InterfaceTab.LOGOUT);
    }

	public static Widget getSpellBookTab()
	{
		if (Microbot.getClient().isResized())
		{
			if (Microbot.getVarbitValue(VarbitID.RESIZABLE_STONE_ARRANGEMENT) == 1)
			{
				return Rs2Widget.getWidget(InterfaceID.ToplevelPreEoc.STONE6);
			}
			return Rs2Widget.getWidget(InterfaceID.ToplevelOsrsStretch.STONE6);
		}
		return Rs2Widget.getWidget(InterfaceID.Toplevel.STONE6);
	}
}
