package net.runelite.client.plugins.microbot.globval.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.plugins.microbot.Microbot;

import java.awt.event.KeyEvent;


/**
 * An enumerated type representing the interface tabs and their WidgetInfo.
 */
@AllArgsConstructor
public enum InterfaceTab {
    COMBAT("Combat Options", VarbitID.STONE_COMBAT_KEY),
    SKILLS("Skills", VarbitID.STONE_STATS_KEY),
    QUESTS("Quest List", VarbitID.STONE_JOURNAL_KEY),
    INVENTORY("Inventory", VarbitID.STONE_INV_KEY),
    EQUIPMENT("Worn Equipment", VarbitID.STONE_WORN_KEY),
    PRAYER("Prayer", VarbitID.STONE_PRAYER_KEY),
    MAGIC("Magic", VarbitID.STONE_MAGIC_KEY),
    FRIENDS("Friends List", VarbitID.STONE_FRIENDS_KEY),
    SETTINGS("Settings", VarbitID.STONE_OPTIONS1_KEY),
    MUSIC("Music Player", VarbitID.STONE_MUSIC_KEY),
    LOGOUT("Logout", VarbitID.STONE_LOGOUT_KEY),
    CHAT("Chat Channel", VarbitID.STONE_CLANCHAT_KEY),
    ACC_MAN("Account Management", VarbitID.STONE_ACCOUNT_KEY),
    EMOTES("Emotes", VarbitID.STONE_OPTIONS2_KEY),
    // bogus widget info
    NOTHING_SELECTED("NothingSelected", -1);

    @Getter
    private final String name;
    @Getter
    private final int hotkeyVarbit;

    public int getHotkey() {
        // special case for nothing selected
        if (hotkeyVarbit == -1) return -1;

        switch (Microbot.getVarbitValue(hotkeyVarbit)) {
            case 1: return KeyEvent.VK_F1;
            case 2: return KeyEvent.VK_F2;
            case 3: return KeyEvent.VK_F3;
            case 4: return KeyEvent.VK_F4;
            case 5: return KeyEvent.VK_F5;
            case 6: return KeyEvent.VK_F6;
            case 7: return KeyEvent.VK_F7;
            case 8: return KeyEvent.VK_F8;
            case 9: return KeyEvent.VK_F9;
            case 10: return KeyEvent.VK_F10;
            case 11: return KeyEvent.VK_F11;
            case 12: return KeyEvent.VK_F12;
            case 13: return KeyEvent.VK_ESCAPE;
            default: return -1;
        }
    }
}
