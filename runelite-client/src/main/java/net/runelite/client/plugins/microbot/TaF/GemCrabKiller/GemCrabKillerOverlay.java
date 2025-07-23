package net.runelite.client.plugins.microbot.TaF.GemCrabKiller;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ButtonComponent;
import net.runelite.client.ui.overlay.components.ImageComponent;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;

public class GemCrabKillerOverlay extends OverlayPanel {
    private final GemCrabKillerPlugin plugin;
    private final ImageComponent image;
    private static final Color TITLE_COLOR = Color.decode("#a4ffff");
    private static final Color BACKGROUND_COLOR = new Color(0, 0, 0, 150);
    private static final Color NORMAL_COLOR = Color.WHITE;
    private static final Color WARNING_COLOR = Color.YELLOW;
    private static final Color DANGER_COLOR = Color.RED;
    private static final Color SUCCESS_COLOR = Color.GREEN;
    @Inject
    GemCrabKillerOverlay(GemCrabKillerPlugin plugin)
    {
        super(plugin);
        setPosition(OverlayPosition.TOP_LEFT);
        setNaughty();
        image = new ImageComponent(getCrabImageFromResources());
        this.plugin = plugin;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.setPreferredSize(new Dimension(220, 300));
            panelComponent.setBackgroundColor(BACKGROUND_COLOR);

            // Title section
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("TaF's Gem Crab Killer")
                    .color(TITLE_COLOR)
                    .build());
            // Script running time
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Runtime:")
                    .right(plugin.getTimeRunning())
                    .rightColor(NORMAL_COLOR)
                    .build());

        } catch (Exception ex) {
            Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
        }
        return super.render(graphics);
    }

    private static BufferedImage getCrabImageFromResources() {
        try {
            Class<?> clazz = Class.forName("net.runelite.client.plugins.microbot.TaF.GemCrabKiller");
            return ImageUtil.loadImageResource(clazz, "GemStoneCrab.png");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
