package net.runelite.client.plugins.microbot.util.prayer;

import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.SpritePixels;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseListener;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.MicrobotConfig;
import net.runelite.client.plugins.microbot.MicrobotPlugin;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;

@Singleton
public class PrayerHotkeyOverlay extends Overlay
{
        private static final int SLOT_COUNT = 5;
        private static final int SLOT_SIZE = 34;
        private static final int SLOT_GAP = 4;
        private static final int SLOT_CORNER_RADIUS = 6;
        private static final int CHAT_MARGIN = 6;
        private static final Font LABEL_FONT = new Font("Arial", Font.BOLD, 11);
        private static final Stroke BORDER_STROKE = new BasicStroke(1.2f);

        private final MicrobotConfig config;
        private final ConfigManager configManager;
        private final Client client;
        private final SpriteManager spriteManager;
        private final Rectangle[] slotBounds = new Rectangle[SLOT_COUNT];

        private final MouseListener mouseListener = new MouseAdapter()
        {
                @Override
                public java.awt.event.MouseEvent mouseMoved(java.awt.event.MouseEvent mouseEvent)
                {
                        if (!shouldHandleInput())
                        {
                                hoveredIndex = -1;
                                return mouseEvent;
                        }

                        hoveredIndex = findSlotIndex(mouseEvent.getPoint());
                        return mouseEvent;
                }

                @Override
                public java.awt.event.MouseEvent mousePressed(java.awt.event.MouseEvent mouseEvent)
                {
                        if (!shouldHandleInput())
                        {
                                return mouseEvent;
                        }

                        if (mouseEvent.getSource().toString().equals("Microbot"))
                        {
                                return mouseEvent;
                        }

                        int slotIndex = findSlotIndex(mouseEvent.getPoint());
                        if (slotIndex == -1)
                        {
                                return mouseEvent;
                        }

                        PrayerHotkeyOption option = getOptionForSlot(slotIndex);

                        if (SwingUtilities.isLeftMouseButton(mouseEvent))
                        {
                                if (option == PrayerHotkeyOption.NONE)
                                {
                                        return mouseEvent;
                                }

                                mouseEvent.consume();
                                Microbot.getClientThread().invokeLater(() -> togglePrayer(option));
                        }
                        else if (SwingUtilities.isRightMouseButton(mouseEvent))
                        {
                                mouseEvent.consume();
                                clearSlot(slotIndex);
                        }

                        return mouseEvent;
                }
        };

        private boolean listenersHooked;
        @Getter
        private int hoveredIndex = -1;
        private boolean renderedLastFrame;

        @Inject
        PrayerHotkeyOverlay(MicrobotPlugin plugin, MicrobotConfig config, ConfigManager configManager, Client client, SpriteManager spriteManager)
        {
                super(plugin);
                setPosition(OverlayPosition.DYNAMIC);
                setPriority(PRIORITY_HIGH);
                setLayer(OverlayLayer.ABOVE_WIDGETS);
                drawAfterInterface(WidgetInfo.CHATBOX.getGroupId());

                this.config = config;
                this.configManager = configManager;
                this.client = client;
                this.spriteManager = spriteManager;
        }

        @Override
        public Dimension render(Graphics2D graphics)
        {
                renderedLastFrame = false;

                if (client == null || client.getGameState() != GameState.LOGGED_IN)
                {
                        hoveredIndex = -1;
                        return null;
                }

                Widget chatWidget = client.getWidget(WidgetInfo.CHATBOX_TRANSPARENT_BACKGROUND);
                if (chatWidget == null)
                {
                        chatWidget = client.getWidget(WidgetInfo.CHATBOX);
                        if (chatWidget == null)
                        {
                                hoveredIndex = -1;
                                return null;
                        }
                }

                Rectangle chatBounds = chatWidget.getBounds();
                if (chatBounds == null)
                {
                        hoveredIndex = -1;
                        return null;
                }

                renderedLastFrame = true;

                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int startX = chatBounds.x + SLOT_GAP;
                int startY = chatBounds.y - SLOT_SIZE - CHAT_MARGIN;

                for (int i = 0; i < SLOT_COUNT; i++)
                {
                        Rectangle bounds = slotBounds[i];
                        if (bounds == null)
                        {
                                bounds = new Rectangle();
                                slotBounds[i] = bounds;
                        }

                        int x = startX + i * (SLOT_SIZE + SLOT_GAP);
                        int y = startY;
                        bounds.setBounds(x, y, SLOT_SIZE, SLOT_SIZE);

                        drawSlot(graphics, bounds, i);
                }

                return null;
        }

        public void hookMouseListener()
        {
                if (!listenersHooked)
                {
                        Microbot.getMouseManager().registerMouseListener(mouseListener);
                        listenersHooked = true;
                }
        }

        public void unhookMouseListener()
        {
                if (listenersHooked)
                {
                        Microbot.getMouseManager().unregisterMouseListener(mouseListener);
                        listenersHooked = false;
                        hoveredIndex = -1;
                }
        }

        private void drawSlot(Graphics2D graphics, Rectangle bounds, int index)
        {
                PrayerHotkeyOption option = getOptionForSlot(index);
                boolean isHovered = hoveredIndex == index;
                boolean isActive = option != PrayerHotkeyOption.NONE && client.isPrayerActive(option.getPrayer());

                Color background = isActive ? new Color(15, 115, 12, 180) : new Color(27, 27, 27, 185);
                if (isHovered)
                {
                        background = background.brighter();
                }

                graphics.setColor(background);
                graphics.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, SLOT_CORNER_RADIUS, SLOT_CORNER_RADIUS);

                graphics.setColor(new Color(0, 0, 0, 200));
                graphics.setStroke(BORDER_STROKE);
                graphics.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, SLOT_CORNER_RADIUS, SLOT_CORNER_RADIUS);

                if (option != PrayerHotkeyOption.NONE)
                {
                        int spriteId = option.getSpriteId(isActive);
                        if (spriteId >= 0)
                        {
                                SpritePixels sprite = spriteManager.getSprite(spriteId, 0);
                                if (sprite != null)
                                {
                                        BufferedImage image = sprite.toBufferedImage();
                                        int imgX = bounds.x + (bounds.width - image.getWidth()) / 2;
                                        int imgY = bounds.y + (bounds.height - image.getHeight()) / 2;
                                        graphics.drawImage(image, imgX, imgY, null);
                                }
                        }
                }
                else
                {
                        graphics.setColor(new Color(255, 255, 255, 60));
                        int cx = bounds.x + bounds.width / 2;
                        int cy = bounds.y + bounds.height / 2;
                        graphics.drawLine(cx - 6, cy, cx + 6, cy);
                        graphics.drawLine(cx, cy - 6, cx, cy + 6);
                }

                graphics.setFont(LABEL_FONT);
                FontMetrics metrics = graphics.getFontMetrics();
                String label = "H" + (index + 1);
                int textWidth = metrics.stringWidth(label);
                int textX = bounds.x + (bounds.width - textWidth) / 2;
                int textY = bounds.y + bounds.height - 4;
                graphics.setColor(new Color(255, 255, 255, 180));
                graphics.drawString(label, textX, textY);
        }

        private PrayerHotkeyOption getOptionForSlot(int slotIndex)
        {
                switch (slotIndex)
                {
                        case 0:
                                return config.prayerHotkey1();
                        case 1:
                                return config.prayerHotkey2();
                        case 2:
                                return config.prayerHotkey3();
                        case 3:
                                return config.prayerHotkey4();
                        case 4:
                                return config.prayerHotkey5();
                        default:
                                return PrayerHotkeyOption.NONE;
                }
        }

        private void togglePrayer(PrayerHotkeyOption option)
        {
                if (option == null || option == PrayerHotkeyOption.NONE)
                {
                        return;
                }

                Client currentClient = Microbot.getClient();
                if (currentClient == null)
                {
                        return;
                }

                boolean shouldEnable = !currentClient.isPrayerActive(option.getPrayer());
                Rs2Prayer.toggle(option.getPrayerEnum(), shouldEnable);
        }

        private void clearSlot(int slotIndex)
        {
                if (configManager == null)
                {
                        return;
                }

                String key;
                switch (slotIndex)
                {
                        case 0:
                                key = MicrobotConfig.keyPrayerHotkey1;
                                break;
                        case 1:
                                key = MicrobotConfig.keyPrayerHotkey2;
                                break;
                        case 2:
                                key = MicrobotConfig.keyPrayerHotkey3;
                                break;
                        case 3:
                                key = MicrobotConfig.keyPrayerHotkey4;
                                break;
                        case 4:
                                key = MicrobotConfig.keyPrayerHotkey5;
                                break;
                        default:
                                return;
                }

                configManager.setConfiguration(MicrobotConfig.configGroup, key, PrayerHotkeyOption.NONE);
        }

        private int findSlotIndex(Point canvasPoint)
        {
                if (slotBounds == null)
                {
                        return -1;
                }

                for (int i = 0; i < slotBounds.length; i++)
                {
                        Rectangle bounds = slotBounds[i];
                        if (bounds != null && bounds.contains(canvasPoint))
                        {
                                return i;
                        }
                }

                return -1;
        }

        private boolean shouldHandleInput()
        {
                return listenersHooked && renderedLastFrame && client != null && client.getGameState() == GameState.LOGGED_IN;
        }
}
