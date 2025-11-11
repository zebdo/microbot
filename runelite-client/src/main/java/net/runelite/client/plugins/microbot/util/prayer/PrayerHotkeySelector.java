package net.runelite.client.plugins.microbot.util.prayer;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.ScriptEvent;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetSizeMode;
import net.runelite.api.widgets.WidgetTextAlignment;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.microbot.Microbot;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.Color;

import static net.runelite.api.FontID.BOLD_12;
import static net.runelite.api.FontID.PLAIN_11;

@Singleton
public class PrayerHotkeySelector
{
        private static final int PANEL_WIDTH = 224;
        private static final int PANEL_HEIGHT = 200;
        private static final int CHAT_OFFSET = 8;
        private static final int PADDING = 8;
        private static final int SLOT_SIZE = 30;
        private static final int SLOT_GAP = 8;
        private static final int GRID_COLUMNS = 7;
        private static final int GRID_ICON_SIZE = 26;
        private static final int GRID_GAP = 4;
        private static final int SLOT_TEXT_HEIGHT = 14;
        private static final int PANEL_OPACITY = 190;
        private static final int SLOT_OPACITY = 160;
        private static final int SLOT_OPACITY_SELECTED = 210;
        private static final int PANEL_BACKGROUND = new Color(20, 20, 20).getRGB();
        private static final int SLOT_BACKGROUND = new Color(36, 36, 36).getRGB();
        private static final int SLOT_BACKGROUND_SELECTED = new Color(52, 96, 158).getRGB();
        private static final int LABEL_COLOR = new Color(200, 200, 200).getRGB();
        private static final int LABEL_COLOR_SELECTED = Color.WHITE.getRGB();
        private static final int PLUS_COLOR = new Color(255, 255, 255, 190).getRGB();
        private static final int INSTRUCTION_COLOR = new Color(210, 210, 210).getRGB();

        private final Client client;
        private final EventBus eventBus;
        private final PrayerHotkeyAssignments assignments;

        private boolean active;
        private Widget container;
        private Widget[] slotBackgrounds;
        private Widget[] slotIcons;
        private Widget[] slotPlusLabels;
        private Widget[] slotLabels;
        private Widget selectionLabel;
        private int selectedSlot;

        @Inject
        PrayerHotkeySelector(Client client, EventBus eventBus, PrayerHotkeyAssignments assignments)
        {
                this.client = client;
                this.eventBus = eventBus;
                this.assignments = assignments;
        }

        public void open()
        {
                if (active)
                {
                        return;
                }

                active = true;
                eventBus.register(this);
                selectedSlot = Math.min(selectedSlot, PrayerHotkeyAssignments.SLOT_COUNT - 1);
                assignments.reload();

                Microbot.getClientThread().invokeLater(() ->
                {
                        if (!ensureBuilt())
                        {
                                return false;
                        }

                        updateSlotWidgets();
                        updateSlotHighlights();
                        updateSelectionLabel();
                        return true;
                });
        }

        public void close()
        {
                if (!active)
                {
                        return;
                }

                active = false;
                eventBus.unregister(this);

                Microbot.getClientThread().invokeLater(() ->
                {
                        destroy();
                        return true;
                });
        }

        public void toggle()
        {
                if (active)
                {
                        close();
                }
                else
                {
                        open();
                }
        }

        public boolean isOpen()
        {
                return active;
        }

        @Subscribe
        public void onGameTick(GameTick tick)
        {
                if (!active)
                {
                        return;
                }

                if (ensureBuilt())
                {
                        updateSlotWidgets();
                        updateSlotHighlights();
                        updateSelectionLabel();
                }
        }

        @Subscribe
        public void onGameStateChanged(GameStateChanged stateChanged)
        {
                if (!active)
                {
                        return;
                }

                GameState state = stateChanged.getGameState();
                if (state == GameState.LOGGED_IN)
                {
                        assignments.reload();
                        ensureBuilt();
                        updateSlotWidgets();
                        updateSlotHighlights();
                        updateSelectionLabel();
                }
                else if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING)
                {
                        destroy();
                }
        }

        private boolean ensureBuilt()
        {
                if (!active)
                {
                        return true;
                }

                Widget chatWidget = getChatWidget();
                if (chatWidget == null || chatWidget.isHidden())
                {
                        destroy();
                        return false;
                }

                if (container != null && container.getParent() == chatWidget && !container.isHidden())
                {
                        return true;
                }

                destroy();
                buildInterface(chatWidget);
                return true;
        }

        private void buildInterface(Widget chatWidget)
        {
                container = chatWidget.createChild(-1, WidgetType.RECTANGLE);
                container.setWidthMode(WidgetSizeMode.ABSOLUTE);
                container.setHeightMode(WidgetSizeMode.ABSOLUTE);
                container.setOriginalWidth(PANEL_WIDTH);
                container.setOriginalHeight(PANEL_HEIGHT);
                container.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
                container.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
                container.setOriginalX(CHAT_OFFSET);
                container.setOriginalY(-PANEL_HEIGHT - CHAT_OFFSET);
                container.setTextColor(PANEL_BACKGROUND);
                container.setOpacity(PANEL_OPACITY);
                container.setFilled(true);
                container.setNoClickThrough(true);
                container.setBorderType(1);
                container.setName("Prayer hotkey setup");
                container.revalidate();

                Widget title = container.createChild(-1, WidgetType.TEXT);
                title.setFontId(BOLD_12);
                title.setText("Prayer Hotkeys");
                title.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
                title.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
                title.setWidthMode(WidgetSizeMode.ABSOLUTE);
                title.setHeightMode(WidgetSizeMode.ABSOLUTE);
                title.setOriginalX(PADDING);
                title.setOriginalY(PADDING);
                title.setOriginalWidth(PANEL_WIDTH - PADDING * 2);
                title.setOriginalHeight(16);
                title.setXTextAlignment(WidgetTextAlignment.LEFT);
                title.setYTextAlignment(WidgetTextAlignment.CENTER);
                title.setTextColor(Color.WHITE.getRGB());
                title.revalidate();

                slotBackgrounds = new Widget[PrayerHotkeyAssignments.SLOT_COUNT];
                slotIcons = new Widget[PrayerHotkeyAssignments.SLOT_COUNT];
                slotPlusLabels = new Widget[PrayerHotkeyAssignments.SLOT_COUNT];
                slotLabels = new Widget[PrayerHotkeyAssignments.SLOT_COUNT];

                int slotsWidth = PrayerHotkeyAssignments.SLOT_COUNT * SLOT_SIZE
                        + (PrayerHotkeyAssignments.SLOT_COUNT - 1) * SLOT_GAP;
                int availableWidth = PANEL_WIDTH - PADDING * 2;
                int slotsStartX = PADDING + Math.max(0, (availableWidth - slotsWidth) / 2);
                int slotY = PADDING + 20;

                for (int i = 0; i < PrayerHotkeyAssignments.SLOT_COUNT; i++)
                {
                        final int slotIndex = i;
                        int slotX = slotsStartX + i * (SLOT_SIZE + SLOT_GAP);

                        Widget background = container.createChild(-1, WidgetType.RECTANGLE);
                        background.setWidthMode(WidgetSizeMode.ABSOLUTE);
                        background.setHeightMode(WidgetSizeMode.ABSOLUTE);
                        background.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
                        background.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
                        background.setOriginalX(slotX);
                        background.setOriginalY(slotY);
                        background.setOriginalWidth(SLOT_SIZE);
                        background.setOriginalHeight(SLOT_SIZE);
                        background.setTextColor(SLOT_BACKGROUND);
                        background.setOpacity(SLOT_OPACITY);
                        background.setFilled(true);
                        background.setBorderType(1);
                        background.setAction(0, "Select");
                        background.setAction(1, "Clear");
                        background.setHasListener(true);
                        background.setOnOpListener((JavaScriptCallback) event -> handleSlotClick(event, slotIndex));
                        background.setName("Hotkey " + (slotIndex + 1));
                        background.revalidate();
                        slotBackgrounds[i] = background;

                        Widget icon = container.createChild(-1, WidgetType.GRAPHIC);
                        icon.setWidthMode(WidgetSizeMode.ABSOLUTE);
                        icon.setHeightMode(WidgetSizeMode.ABSOLUTE);
                        icon.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
                        icon.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
                        icon.setOriginalX(slotX + 2);
                        icon.setOriginalY(slotY + 2);
                        icon.setOriginalWidth(SLOT_SIZE - 4);
                        icon.setOriginalHeight(SLOT_SIZE - 4);
                        icon.setSpriteId(-1);
                        icon.revalidate();
                        slotIcons[i] = icon;

                        Widget plus = container.createChild(-1, WidgetType.TEXT);
                        plus.setFontId(BOLD_12);
                        plus.setText("+");
                        plus.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
                        plus.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
                        plus.setWidthMode(WidgetSizeMode.ABSOLUTE);
                        plus.setHeightMode(WidgetSizeMode.ABSOLUTE);
                        plus.setOriginalX(slotX);
                        plus.setOriginalY(slotY + 6);
                        plus.setOriginalWidth(SLOT_SIZE);
                        plus.setOriginalHeight(SLOT_SIZE - 12);
                        plus.setXTextAlignment(WidgetTextAlignment.CENTER);
                        plus.setYTextAlignment(WidgetTextAlignment.CENTER);
                        plus.setTextColor(PLUS_COLOR);
                        plus.revalidate();
                        slotPlusLabels[i] = plus;

                        Widget label = container.createChild(-1, WidgetType.TEXT);
                        label.setFontId(PLAIN_11);
                        label.setText("H" + (i + 1));
                        label.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
                        label.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
                        label.setWidthMode(WidgetSizeMode.ABSOLUTE);
                        label.setHeightMode(WidgetSizeMode.ABSOLUTE);
                        label.setOriginalX(slotX);
                        label.setOriginalY(slotY + SLOT_SIZE + 2);
                        label.setOriginalWidth(SLOT_SIZE);
                        label.setOriginalHeight(SLOT_TEXT_HEIGHT);
                        label.setXTextAlignment(WidgetTextAlignment.CENTER);
                        label.setYTextAlignment(WidgetTextAlignment.CENTER);
                        label.setTextColor(LABEL_COLOR);
                        label.revalidate();
                        slotLabels[i] = label;
                }

                selectionLabel = container.createChild(-1, WidgetType.TEXT);
                selectionLabel.setFontId(PLAIN_11);
                selectionLabel.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
                selectionLabel.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
                selectionLabel.setWidthMode(WidgetSizeMode.ABSOLUTE);
                selectionLabel.setHeightMode(WidgetSizeMode.ABSOLUTE);
                selectionLabel.setOriginalX(PADDING);
                selectionLabel.setOriginalY(slotY + SLOT_SIZE + SLOT_TEXT_HEIGHT + 8);
                selectionLabel.setOriginalWidth(PANEL_WIDTH - PADDING * 2);
                selectionLabel.setOriginalHeight(SLOT_TEXT_HEIGHT);
                selectionLabel.setXTextAlignment(WidgetTextAlignment.LEFT);
                selectionLabel.setYTextAlignment(WidgetTextAlignment.CENTER);
                selectionLabel.setTextColor(Color.WHITE.getRGB());
                selectionLabel.revalidate();

                Widget instructions = container.createChild(-1, WidgetType.TEXT);
                instructions.setFontId(PLAIN_11);
                instructions.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
                instructions.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
                instructions.setWidthMode(WidgetSizeMode.ABSOLUTE);
                instructions.setHeightMode(WidgetSizeMode.ABSOLUTE);
                instructions.setOriginalX(PADDING);
                instructions.setOriginalY(selectionLabel.getOriginalY() + SLOT_TEXT_HEIGHT + 6);
                instructions.setOriginalWidth(PANEL_WIDTH - PADDING * 2);
                instructions.setOriginalHeight(28);
                instructions.setXTextAlignment(WidgetTextAlignment.LEFT);
                instructions.setYTextAlignment(WidgetTextAlignment.TOP);
                instructions.setTextColor(INSTRUCTION_COLOR);
                instructions.setText("Left-click a prayer to assign. Right-click a hotkey to clear.");
                instructions.revalidate();

                int gridStartY = instructions.getOriginalY() + SLOT_TEXT_HEIGHT + 6;
                int gridStartX = PADDING;
                int index = 0;
                for (PrayerHotkeyOption option : PrayerHotkeyOption.values())
                {
                        if (option == PrayerHotkeyOption.NONE)
                        {
                                continue;
                        }

                        final PrayerHotkeyOption assignOption = option;
                        int row = index / GRID_COLUMNS;
                        int column = index % GRID_COLUMNS;

                        Widget prayerWidget = container.createChild(-1, WidgetType.GRAPHIC);
                        prayerWidget.setWidthMode(WidgetSizeMode.ABSOLUTE);
                        prayerWidget.setHeightMode(WidgetSizeMode.ABSOLUTE);
                        prayerWidget.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
                        prayerWidget.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
                        prayerWidget.setOriginalX(gridStartX + column * (GRID_ICON_SIZE + GRID_GAP));
                        prayerWidget.setOriginalY(gridStartY + row * (GRID_ICON_SIZE + GRID_GAP));
                        prayerWidget.setOriginalWidth(GRID_ICON_SIZE);
                        prayerWidget.setOriginalHeight(GRID_ICON_SIZE);
                        prayerWidget.setSpriteId(assignOption.getSpriteId(true));
                        prayerWidget.setAction(0, "Assign");
                        prayerWidget.setHasListener(true);
                        prayerWidget.setOnOpListener((JavaScriptCallback) event -> handlePrayerSelection(assignOption, event));
                        prayerWidget.setName(assignOption.getDisplayName());
                        prayerWidget.setNoClickThrough(true);
                        prayerWidget.revalidate();

                        index++;
                }

                updateSlotWidgets();
                updateSlotHighlights();
                updateSelectionLabel();
        }

        private void handleSlotClick(ScriptEvent event, int slotIndex)
        {
                if (event.getOp() == 2)
                {
                        assignments.clearSlot(slotIndex);
                        updateSlotWidgets();
                        if (slotIndex == selectedSlot)
                        {
                                updateSelectionLabel();
                        }
                        return;
                }

                setSelectedSlot(slotIndex);
        }

        private void handlePrayerSelection(PrayerHotkeyOption option, ScriptEvent event)
        {
                if (event.getOp() != 1)
                {
                        return;
                }

                assignments.setSlot(selectedSlot, option);
                updateSlotWidgets();
                updateSelectionLabel();
        }

        private void setSelectedSlot(int slotIndex)
        {
                if (slotIndex < 0 || slotIndex >= PrayerHotkeyAssignments.SLOT_COUNT)
                {
                        return;
                }

                selectedSlot = slotIndex;
                updateSlotHighlights();
                updateSelectionLabel();
        }

        private void updateSlotWidgets()
        {
                if (slotIcons == null)
                {
                        return;
                }

                for (int i = 0; i < slotIcons.length; i++)
                {
                        Widget icon = slotIcons[i];
                        Widget plus = slotPlusLabels != null && i < slotPlusLabels.length ? slotPlusLabels[i] : null;
                        if (icon == null)
                        {
                                continue;
                        }

                        PrayerHotkeyOption option = assignments.getSlot(i);
                        if (option != null && option != PrayerHotkeyOption.NONE)
                        {
                                icon.setHidden(false);
                                icon.setSpriteId(option.getSpriteId(true));
                                icon.setName(option.getDisplayName());
                                if (plus != null)
                                {
                                        plus.setHidden(true);
                                }
                        }
                        else
                        {
                                icon.setHidden(true);
                                icon.setSpriteId(-1);
                                icon.setName("Empty");
                                if (plus != null)
                                {
                                        plus.setHidden(false);
                                }
                        }
                }
        }

        private void updateSlotHighlights()
        {
                if (slotBackgrounds == null)
                {
                        return;
                }

                for (int i = 0; i < slotBackgrounds.length; i++)
                {
                        Widget background = slotBackgrounds[i];
                        Widget label = slotLabels != null && i < slotLabels.length ? slotLabels[i] : null;
                        if (background != null)
                        {
                                background.setTextColor(i == selectedSlot ? SLOT_BACKGROUND_SELECTED : SLOT_BACKGROUND);
                                background.setOpacity(i == selectedSlot ? SLOT_OPACITY_SELECTED : SLOT_OPACITY);
                        }
                        if (label != null)
                        {
                                label.setTextColor(i == selectedSlot ? LABEL_COLOR_SELECTED : LABEL_COLOR);
                        }
                }
        }

        private void updateSelectionLabel()
        {
                if (selectionLabel == null)
                {
                        return;
                }

                PrayerHotkeyOption option = assignments.getSlot(selectedSlot);
                selectionLabel.setText(String.format("Hotkey %d: %s", selectedSlot + 1, option.getDisplayName()));
        }

        private Widget getChatWidget()
        {
                Widget widget = client.getWidget(WidgetInfo.CHATBOX_PARENT);
                if (isLayer(widget))
                {
                        return widget;
                }

                widget = client.getWidget(WidgetInfo.RESIZABLE_VIEWPORT_CHATBOX_PARENT);
                if (isLayer(widget))
                {
                        return widget;
                }

                widget = client.getWidget(WidgetInfo.RESIZABLE_VIEWPORT_BOTTOM_LINE_CHATBOX_PARENT);
                if (isLayer(widget))
                {
                        return widget;
                }

                return null;
        }

        private boolean isLayer(Widget widget)
        {
                return widget != null && widget.getType() == WidgetType.LAYER;
        }

        private void destroy()
        {
                if (container != null)
                {
                        container.deleteAllChildren();
                        container.setHidden(true);
                        container = null;
                }

                slotBackgrounds = null;
                slotIcons = null;
                slotPlusLabels = null;
                slotLabels = null;
                selectionLabel = null;
        }
}
