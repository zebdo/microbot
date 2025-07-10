package net.runelite.client.plugins.microbot.util.tabs;

import net.runelite.api.VarClientInt;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.globval.VarcIntValues;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.util.Arrays;
import java.util.Objects;

import static net.runelite.client.plugins.microbot.util.Global.sleep;
import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

public class Rs2Tab {
    public static InterfaceTab getCurrentTab() {
        final int varcIntValue = Microbot.getClient().getVarcIntValue(VarClientInt.INVENTORY_TAB);
        switch (VarcIntValues.valueOf(varcIntValue)) {
            case TAB_COMBAT_OPTIONS:
                return InterfaceTab.COMBAT;
            case TAB_SKILLS:
                return InterfaceTab.SKILLS;
            case TAB_QUEST_LIST:
                return InterfaceTab.QUESTS;
            case TAB_INVENTORY:
                return InterfaceTab.INVENTORY;
            case TAB_WORN_EQUIPMENT:
                return InterfaceTab.EQUIPMENT;
            case TAB_PRAYER:
                return InterfaceTab.PRAYER;
            case TAB_SPELLBOOK:
                return InterfaceTab.MAGIC;
            case TAB_FRIEND_LIST:
                return InterfaceTab.FRIENDS;
            case TAB_LOGOUT:
                return InterfaceTab.LOGOUT;
            case TAB_SETTINGS:
                return InterfaceTab.SETTINGS;
            case TAB_MUSIC:
                return InterfaceTab.MUSIC;
            case TAB_CHAT_CHANNEL:
                return InterfaceTab.CHAT;
            case TAB_ACC_MANAGEMENT:
                return InterfaceTab.ACC_MAN;
            case TAB_EMOTES:
                return InterfaceTab.EMOTES;
            case TAB_NOT_SELECTED:
                return InterfaceTab.NOTHING_SELECTED;
            default:
                throw new IllegalStateException("Unexpected value: " + VarcIntValues.valueOf(varcIntValue));
        }
    }

    public static boolean isCurrentTab(InterfaceTab tab) {
        return getCurrentTab() == tab;
    }

    public static boolean switchTo(InterfaceTab tab) {
        if (tab == InterfaceTab.LOGOUT) return switchToLogout();
        final InterfaceTab currentTab = getCurrentTab();
        if (currentTab == tab) return true;

        final int hotkey;
        if (tab == InterfaceTab.NOTHING_SELECTED) {
            hotkey = currentTab.getHotkey();
        } else {
            hotkey = tab.getHotkey();
        }

        if (hotkey == -1) {
            if (Microbot.isLoggedIn()) {
                Microbot.showMessage("Keybinding not found for tab " + tab.getName() + ". Please fill in the keybinding in your settings");
                sleep(5000);
            }
            return false;
        }

        Rs2Keyboard.keyPress(hotkey);
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

    // TODO: make private and/or incorporate widget finding for all switchTo?
    @Deprecated(since = "Use switchTo")
    public static boolean switchToLogout() {
        if (getCurrentTab() == InterfaceTab.LOGOUT) return true;
        
        // Logout is not configured by default, but we should prefer it if it is configured as it is faster than clicking the widget
        final int hotkey = InterfaceTab.LOGOUT.getHotkey();
        if (hotkey == -1) {
            final Widget tab = getLogoutWidget();
            if (tab == null) return false;

            if (!Rs2Widget.clickWidget(tab)) return false;
        } else {
            Rs2Keyboard.keyPress(hotkey);
        }

        return sleepUntil(() -> isCurrentTab(InterfaceTab.LOGOUT));
    }

    private final static int[] LOGOUT_WIDGET_ID_VARIATIONS = {
            35913778, // Fixed Classic Display
            10551342, // Resizable Classic Display
            10747938  // Resizable Modern Display
    };
    private static Widget getLogoutWidget() {
        return Arrays.stream(LOGOUT_WIDGET_ID_VARIATIONS).mapToObj(Rs2Widget::getWidget).filter(Objects::nonNull)
                .findFirst().orElseGet(() -> {
                    Microbot.showMessage("Unable to find logout button widget!");
                    return null;
                });
    }
}
