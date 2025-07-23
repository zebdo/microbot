package net.runelite.client.plugins.microbot.TaF.GemCrabKiller;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ImageComponent;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;

public class GemCrabKillerOverlay extends OverlayPanel {
    private static final Color TITLE_COLOR = Color.decode("#a4ffff");
    private static final Color BACKGROUND_COLOR = new Color(0, 0, 0, 150);
    private static final Color NORMAL_COLOR = Color.WHITE;
    private static final Color WARNING_COLOR = Color.YELLOW;
    private static final Color DANGER_COLOR = Color.RED;
    private static final Color SUCCESS_COLOR = Color.GREEN;
    private final GemCrabKillerPlugin plugin;
    private final ImageComponent image;

    @Inject
    GemCrabKillerOverlay(GemCrabKillerPlugin plugin) {
        super(plugin);
        setPosition(OverlayPosition.TOP_LEFT);
        setNaughty();
        image = new ImageComponent(getCrabImageFromResources());
        this.plugin = plugin;
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
            panelComponent.getChildren().add(image);
            // Script running time
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Runtime:")
                    .right(plugin.getTimeRunning())
                    .rightColor(NORMAL_COLOR)
                    .build());
            // State information
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("State:")
                    .right(plugin.gemCrabKillerScript.gemCrabKillerState.name())
                    .rightColor(Color.WHITE)
                    .build());
            var xpGained = plugin.getXpGained();
            var xpPerHour = plugin.getXpPerHour();

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("XP Gained:")
                    .right(formatNumber(xpGained))
                    .rightColor(NORMAL_COLOR)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("XP/Hour:")
                    .right(formatNumber(xpPerHour))
                    .rightColor(xpPerHour > 0 ? SUCCESS_COLOR : NORMAL_COLOR)
                    .build());

            // Footer with version
            panelComponent.getChildren().add(LineComponent.builder().build()); // Spacer
            panelComponent.getChildren().add(LineComponent.builder()
                    .right("v" + GemCrabKillerScript.version)
                    .rightColor(new Color(160, 160, 160))
                    .build());

        } catch (Exception ex) {
            Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
        }
        return super.render(graphics);
    }

    private String formatNumber(long number) {
        return String.format("%,d", number);
    }
}
