package net.runelite.client.plugins.microbot.example;

import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;

public class ExampleScriptOverlay extends Overlay {
    private final ExampleScript script;

    @Inject
    public ExampleScriptOverlay(ExampleScript script) {
        this.script = script;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        List<Point> points = script.getLastTestPoints();
        if (points != null) {
            graphics.setColor(Color.RED);
            for (Point p : points) {
                if (p != null) {
                    graphics.fillOval(p.getX() - 3, p.getY() - 3, 6, 6);
                }
            }
        }
        return null;
    }
}

