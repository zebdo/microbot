package net.runelite.client.plugins.devtools;

import net.runelite.api.Client;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;
import java.awt.font.TextLayout;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

public class MicrobotMouseOverlay extends Overlay {
    private final Client client;
    private final DevToolsPlugin plugin;
    private float angle = 0.0f; // Rotation angle

    @Inject
    MicrobotMouseOverlay(Client client, DevToolsPlugin plugin) {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(Overlay.PRIORITY_LOW);
        // Increase the angle
        new Thread(() -> {
            try {
                while (true) {
                    angle += 0.007f; // Increment angle
                    if (angle >= 2 * Math.PI) {
                        angle -= (float) (2 * Math.PI);
                    }

                    Thread.sleep(5); // Control frame rate
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }




    @Override
    public Dimension render(Graphics2D g) {
        if (plugin.getMouseMovement().isActive()) {
            if (!Microbot.getMouse().getTimer().isRunning()) {
                Microbot.getMouse().getPoints().clear();
                Microbot.getMouse().getTimer().start();
            }
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            // Enable anti-aliasing for smooth rendering
            int size = 64; // Cursor image size
            BufferedImage cursorImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            cursorImage.setAccelerationPriority(1.0f);
            Graphics2D g2d = cursorImage.createGraphics();


            // Enable anti-aliasing for smooth rendering
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

            // Rotate the graphics
            g2d.setColor(Microbot.getMouse().getRainbowColor());
            g2d.setFont(new Font("Serif", Font.PLAIN, 30));
            String crosshair = "⛌";

            // Measure the bounds of the character
            FontMetrics fm = g2d.getFontMetrics();
            int charWidth = fm.stringWidth(crosshair);
            int charHeight = fm.getAscent(); // Only use the ascent to avoid including descent

            // Rotate and center the character
            g2d.translate(size / 2.0, size / 2.0); // Move to the center of the cursor image
            g2d.rotate(angle); // Apply rotation
            g2d.translate(-charWidth / 2.0, charHeight / 4.0); // Center the character

            TextLayout layout = new TextLayout(crosshair, g2d.getFont(), g2d.getFontRenderContext());
            layout.draw(g2d, 0, 3);
            // Draw the character
            //g2d.drawString(crosshair, 0, 3);
            // Dispose of the graphics




            g2d.dispose();
            // Mouse position
            int x = Microbot.getMouse().getLastMove().getX();
            int y = Microbot.getMouse().getLastMove().getY();



            // Draw the crosshair centered
            float drawX = x - size / 2.0f;
            float drawY = y - size / 2.0f;
            g.drawImage(cursorImage, (int) drawX, (int) drawY, null);
            //OverlayUtil.renderTextLocation(g, new net.runelite.api.Point(drawX, drawY), "✛", Microbot.getMouse().getRainbowColor());




            g.setStroke(new BasicStroke(3));
            //g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            var points = Microbot.getMouse().getPoints();
            if (points.size() > 1) {
                Path2D path = new Path2D.Double();

// Move to the first point
                net.runelite.api.Point firstPoint = points.getFirst();
                path.moveTo(firstPoint.getX(), firstPoint.getY());

// For each intermediate pair of points, use a midpoint-based quadTo
                for (int i = 1; i < points.size() - 2; i++)
                {
                    net.runelite.api.Point pCurrent = points.get(i);
                    net.runelite.api.Point pNext = points.get(i + 1);

                    // Calculate midpoints for a smoother curve
                    double midX = (pCurrent.getX() + pNext.getX()) / 2.0;
                    double midY = (pCurrent.getY() + pNext.getY()) / 2.0;

                    // Draw a quadratic curve from pCurrent toward midX/midY
                    path.quadTo(
                            pCurrent.getX(), pCurrent.getY(),
                            midX, midY
                    );
                }

// Finally, connect the last two points with a final quadTo
                net.runelite.api.Point secondLast = points.get(points.size() - 2);
                net.runelite.api.Point last = points.getLast();
                path.quadTo(
                        secondLast.getX(), secondLast.getY(),
                        last.getX(), last.getY()
                );

// Optionally set a thicker stroke with round caps/joins for a “brush” feel
                g.setColor(Microbot.getMouse().getRainbowColor());
                g.setStroke(new BasicStroke(
                        3.0f,                      // thickness
                        BasicStroke.CAP_ROUND,     // end cap
                        BasicStroke.JOIN_ROUND     // join style
                ));

// Draw the smooth path
                g.draw(path);
            }
            // draw trail of mouse movements

        } else {
            Microbot.getMouse().getPoints().clear();
            Microbot.getMouse().getTimer().stop();
        }

        return null;
    }
}

