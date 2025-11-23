package net.runelite.client.plugins.microbot.shortestpath;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.List;

/**
 * Debug overlay that draws the sailing navigation path lines on the game world.
 */
public class SailingPathOverlay extends Overlay
{
    private final Client client;
    private final ShortestPathPlugin plugin;

    private static final Color PATH_COLOR = new Color(0, 150, 255, 150);
    private static final Color WAYPOINT_COLOR = new Color(255, 255, 0, 200);
    private static final Color CURRENT_WAYPOINT_COLOR = new Color(0, 255, 0, 255);
    private static final Color START_COLOR = new Color(0, 255, 0, 200);
    private static final Color END_COLOR = new Color(255, 0, 0, 200);
    private static final int WAYPOINT_RADIUS = 5;

    @Inject
    public SailingPathOverlay(Client client, ShortestPathPlugin plugin)
    {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(Overlay.PRIORITY_LOW);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        SailingPanel sailingPanel = plugin.getSailingPanel();
        if (sailingPanel == null || !sailingPanel.isDebugOverlayEnabled())
        {
            return null;
        }

        List<WorldPoint> path = sailingPanel.getCurrentPath();
        if (path == null || path.isEmpty())
        {
            return null;
        }

        int currentWaypointIndex = sailingPanel.getCurrentWaypointIndex();

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw lines between waypoints
        for (int i = 1; i < path.size(); i++)
        {
            WorldPoint start = path.get(i - 1);
            WorldPoint end = path.get(i);

            // Color based on whether we've passed this segment
            Color lineColor;
            if (i <= currentWaypointIndex)
            {
                // Already passed - dim color
                lineColor = new Color(100, 100, 100, 100);
            }
            else if (i == currentWaypointIndex + 1)
            {
                // Current segment - bright color
                lineColor = new Color(0, 255, 100, 200);
            }
            else
            {
                // Future segment - normal color
                lineColor = PATH_COLOR;
            }

            drawLine(graphics, start, end, lineColor);
        }

        // Draw waypoints
        for (int i = 0; i < path.size(); i++)
        {
            WorldPoint waypoint = path.get(i);
            Color color;
            int radius = WAYPOINT_RADIUS;

            if (i == 0)
            {
                // Start point
                color = START_COLOR;
                radius = 8;
            }
            else if (i == path.size() - 1)
            {
                // End point
                color = END_COLOR;
                radius = 8;
            }
            else if (i == currentWaypointIndex)
            {
                // Current target waypoint
                color = CURRENT_WAYPOINT_COLOR;
                radius = 7;
            }
            else if (i < currentWaypointIndex)
            {
                // Already passed
                color = new Color(100, 100, 100, 150);
                radius = 4;
            }
            else
            {
                // Future waypoint
                color = WAYPOINT_COLOR;
            }

            drawWaypoint(graphics, waypoint, color, radius, i);
        }

        // Draw info text
        drawInfoText(graphics, path, currentWaypointIndex);

        return null;
    }

    private void drawLine(Graphics2D graphics, WorldPoint startLoc, WorldPoint endLoc, Color color)
    {
        LocalPoint lpStart = LocalPoint.fromWorld(client, startLoc);
        LocalPoint lpEnd = LocalPoint.fromWorld(client, endLoc);

        if (lpStart == null || lpEnd == null)
        {
            return;
        }

        final int z = client.getPlane();
        final int startHeight = Perspective.getTileHeight(client, lpStart, z);
        final int endHeight = Perspective.getTileHeight(client, lpEnd, z);

        Point p1 = Perspective.localToCanvas(client, lpStart.getX(), lpStart.getY(), startHeight);
        Point p2 = Perspective.localToCanvas(client, lpEnd.getX(), lpEnd.getY(), endHeight);

        if (p1 == null || p2 == null)
        {
            return;
        }

        Line2D.Double line = new Line2D.Double(p1.getX(), p1.getY(), p2.getX(), p2.getY());

        // Draw outline
        graphics.setColor(Color.BLACK);
        graphics.setStroke(new BasicStroke(5));
        graphics.draw(line);

        // Draw line
        graphics.setColor(color);
        graphics.setStroke(new BasicStroke(3));
        graphics.draw(line);
    }

    private void drawWaypoint(Graphics2D graphics, WorldPoint location, Color color, int radius, int index)
    {
        LocalPoint lp = LocalPoint.fromWorld(client, location);
        if (lp == null)
        {
            return;
        }

        final int z = client.getPlane();
        final int height = Perspective.getTileHeight(client, lp, z);

        Point p = Perspective.localToCanvas(client, lp.getX(), lp.getY(), height);
        if (p == null)
        {
            return;
        }

        // Draw circle
        Ellipse2D.Double circle = new Ellipse2D.Double(
            p.getX() - radius,
            p.getY() - radius,
            radius * 2,
            radius * 2
        );

        // Outline
        graphics.setColor(Color.BLACK);
        graphics.setStroke(new BasicStroke(2));
        graphics.draw(circle);

        // Fill
        graphics.setColor(color);
        graphics.fill(circle);

        // Draw index number
        String indexText = String.valueOf(index);
        FontMetrics fm = graphics.getFontMetrics();
        int textWidth = fm.stringWidth(indexText);
        int textHeight = fm.getHeight();

        int textX = (int) p.getX() - textWidth / 2;
        int textY = (int) p.getY() - radius - 5;

        // Text shadow
        graphics.setColor(Color.BLACK);
        graphics.drawString(indexText, textX + 1, textY + 1);

        // Text
        graphics.setColor(Color.WHITE);
        graphics.drawString(indexText, textX, textY);
    }

    private void drawInfoText(Graphics2D graphics, List<WorldPoint> path, int currentWaypointIndex)
    {
        int x = 10;
        int y = 50;

        graphics.setFont(graphics.getFont().deriveFont(Font.BOLD, 12f));

        // Background
        String[] lines = {
            "Sailing Debug Overlay",
            "Total Waypoints: " + path.size(),
            "Current Target: " + (currentWaypointIndex < path.size() ? currentWaypointIndex : "Done"),
            "Remaining: " + Math.max(0, path.size() - currentWaypointIndex)
        };

        int maxWidth = 0;
        for (String line : lines)
        {
            maxWidth = Math.max(maxWidth, graphics.getFontMetrics().stringWidth(line));
        }

        graphics.setColor(new Color(0, 0, 0, 180));
        graphics.fillRoundRect(x - 5, y - 15, maxWidth + 15, lines.length * 18 + 10, 5, 5);

        // Text
        graphics.setColor(Color.CYAN);
        graphics.drawString(lines[0], x, y);

        graphics.setColor(Color.WHITE);
        for (int i = 1; i < lines.length; i++)
        {
            graphics.drawString(lines[i], x, y + i * 18);
        }
    }
}
