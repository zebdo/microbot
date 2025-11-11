package net.runelite.client.plugins.microbot.util.prayer;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Prayer;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;

/**
 * Adds a Protect from Melee toggle inside the inventory panel.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InventoryPrayerIconManager
{
        private static final int ICON_SIZE = 26;
        private static final int ICON_OFFSET_X = 6;
        private static final int ICON_OFFSET_Y = 6;
        private static final int PANEL_EXPANSION = ICON_SIZE + (ICON_OFFSET_X * 2);

        private static Widget iconWidget;
        private static int expandedPanelGroupId = -1;
        private static int expandedPanelChildId = -1;
        private static int expandedPanelOriginalWidth = -1;

        /**
         * Attempts to build the inventory prayer icon and refresh its state.
         */
        public static void initialize()
        {
                Microbot.getClientThread().invokeLater(() ->
                {
                        if (!shouldOperate())
                        {
                                hideIcon();
                                return;
                        }

                        Widget panel = findInventoryPanel();
                        if (panel == null)
                        {
                                return;
                        }

                        buildIconIfNeeded(panel);
                        updateIconState(panel);
                });
        }

        /**
         * Removes any existing icon instance.
         */
        public static void shutdown()
        {
                Microbot.getClientThread().invokeLater(InventoryPrayerIconManager::removeIcon);
        }

        /**
         * Ensures the icon is rebuilt after the inventory group loads.
         */
        public static void onWidgetLoaded(WidgetLoaded event)
        {
                if (event.getGroupId() == InterfaceID.INVENTORY)
                {
                        initialize();
                }
        }

        /**
         * Hides the icon when the inventory group is closed.
         */
        public static void onWidgetClosed(WidgetClosed event)
        {
                if (event.getGroupId() == InterfaceID.INVENTORY)
                {
                        Microbot.getClientThread().invokeLater(InventoryPrayerIconManager::hideIcon);
                }
        }

        /**
         * Updates the icon sprite and visibility every game tick.
         */
        public static void onGameTick()
        {
                Microbot.getClientThread().invokeLater(() ->
                {
                        if (!shouldOperate())
                        {
                                hideIcon();
                                return;
                        }

                        Widget panel = findInventoryPanel();
                        if (panel == null)
                        {
                                hideIcon();
                                return;
                        }

                        if (iconWidget == null || iconWidget.getParentId() != panel.getId())
                        {
                                buildIconIfNeeded(panel);
                        }

                        updateIconState(panel);
                });
        }

        private static void buildIconIfNeeded(Widget panel)
        {
                if (iconWidget != null && iconWidget.getParentId() == panel.getId())
                {
                        return;
                }

                removeIcon();

                expandPanelWidth(panel);

                Widget icon = panel.createChild(-1, WidgetType.GRAPHIC);
                icon.setOriginalWidth(ICON_SIZE);
                icon.setOriginalHeight(ICON_SIZE);
                icon.setXPositionMode(WidgetPositionMode.ABSOLUTE_RIGHT);
                icon.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
                icon.setOriginalX(ICON_OFFSET_X);
                icon.setOriginalY(ICON_OFFSET_Y);
                icon.setSpriteId(SpriteID.Prayeroff.PROTECT_FROM_MELEE_DISABLED);
                icon.setHasListener(true);
                icon.setName("Protect from Melee");
                icon.setAction(1, "Activate");
                icon.setOnOpListener((JavaScriptCallback) ev ->
                        Microbot.getClientThread().invokeLater(InventoryPrayerIconManager::toggleProtectFromMelee));
                icon.setNoClickThrough(true);
                icon.revalidate();

                iconWidget = icon;
        }

        private static void updateIconState(Widget panel)
        {
                if (iconWidget == null || iconWidget.getParentId() != panel.getId())
                {
                        return;
                }

                boolean shouldShow = Rs2Tab.getCurrentTab() == InterfaceTab.INVENTORY;
                iconWidget.setHidden(!shouldShow);

                if (!shouldShow)
                {
                        return;
                }

                Client client = Microbot.getClient();
                boolean active = client.isPrayerActive(Prayer.PROTECT_FROM_MELEE);
                iconWidget.setSpriteId(active
                        ? SpriteID.Prayeron.PROTECT_FROM_MELEE
                        : SpriteID.Prayeroff.PROTECT_FROM_MELEE_DISABLED);
                iconWidget.setAction(1, active ? "Deactivate" : "Activate");
                iconWidget.setName(active ? "Protect from Melee (On)" : "Protect from Melee (Off)");
        }

        private static void toggleProtectFromMelee()
        {
                if (!shouldOperate() || iconWidget == null)
                {
                        return;
                }

                Client client = Microbot.getClient();
                boolean desiredState = !client.isPrayerActive(Prayer.PROTECT_FROM_MELEE);
                Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MELEE, desiredState);
                Widget panel = findInventoryPanel();
                if (panel != null)
                {
                        updateIconState(panel);
                }
        }

        private static void removeIcon()
        {
                if (iconWidget != null)
                {
                        iconWidget.setOnOpListener((JavaScriptCallback) null);
                        iconWidget.setHidden(true);
                        iconWidget = null;
                }

                restorePanelWidth();
        }

        private static void hideIcon()
        {
                if (iconWidget != null)
                {
                        iconWidget.setHidden(true);
                }
        }

        private static void expandPanelWidth(Widget panel)
        {
                if (panel == null)
                {
                        return;
                }

                int groupId = panel.getId() >>> 16;
                int childId = panel.getId() & 0xFFFF;

                if (expandedPanelGroupId != -1 && (expandedPanelGroupId != groupId || expandedPanelChildId != childId))
                {
                        restorePanelWidth();
                }

                if (expandedPanelGroupId != groupId || expandedPanelChildId != childId)
                {
                        expandedPanelGroupId = groupId;
                        expandedPanelChildId = childId;
                        expandedPanelOriginalWidth = panel.getOriginalWidth();
                }

                if (expandedPanelOriginalWidth == -1)
                {
                        return;
                }

                int requiredWidth = expandedPanelOriginalWidth + PANEL_EXPANSION;
                if (panel.getOriginalWidth() < requiredWidth)
                {
                        panel.setOriginalWidth(requiredWidth);
                        panel.revalidate();
                }
        }

        private static void restorePanelWidth()
        {
                if (expandedPanelGroupId == -1 || expandedPanelChildId == -1 || expandedPanelOriginalWidth == -1)
                {
                        return;
                }

                Client client = Microbot.getClient();
                if (client != null)
                {
                        Widget panel = client.getWidget(expandedPanelGroupId, expandedPanelChildId);
                        if (panel != null && panel.getOriginalWidth() != expandedPanelOriginalWidth)
                        {
                                panel.setOriginalWidth(expandedPanelOriginalWidth);
                                panel.revalidate();
                        }
                }

                expandedPanelGroupId = -1;
                expandedPanelChildId = -1;
                expandedPanelOriginalWidth = -1;
        }

        private static boolean shouldOperate()
        {
                Client client = Microbot.getClient();
                return client != null && client.getGameState() == GameState.LOGGED_IN;
        }

        private static Widget findInventoryPanel()
        {
                Client client = Microbot.getClient();
                if (client == null)
                {
                        return null;
                }

                Widget panel = client.getWidget(WidgetInfo.RESIZABLE_VIEWPORT_INVENTORY_CONTAINER);
                if (panel == null)
                {
                        panel = client.getWidget(WidgetInfo.RESIZABLE_VIEWPORT_BOTTOM_LINE_INVENTORY_CONTAINER);
                }
                if (panel == null)
                {
                        panel = client.getWidget(WidgetInfo.FIXED_VIEWPORT_INVENTORY_CONTAINER);
                }
                return panel;
        }
}
