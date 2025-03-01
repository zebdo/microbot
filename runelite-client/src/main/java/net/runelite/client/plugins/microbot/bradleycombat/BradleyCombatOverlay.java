package net.runelite.client.plugins.microbot.bradleycombat;

import com.google.common.base.Strings;
import net.runelite.api.Actor;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.awt.*;

public class BradleyCombatOverlay extends Overlay {
    private final BradleyCombatConfig config;

    @Inject
    BradleyCombatOverlay(BradleyCombatPlugin plugin, BradleyCombatConfig config) {
        super(plugin);
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setNaughty();
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            if (BradleyCombatPlugin.getTarget() != null && !BradleyCombatPlugin.getTarget().isDead()) {
                WorldPoint targetLoc = BradleyCombatPlugin.getTarget().getWorldLocation();
                if (targetLoc != null) {
                    Color highlightColor = config.targetTileColor();
                    drawTile(graphics, targetLoc, highlightColor, "", new BasicStroke(2));
                }
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return null;
    }

    private void drawTile(Graphics2D graphics, WorldPoint point, Color color, @Nullable String label, Stroke borderStroke) {
        if (point == null) {
            return;
        }
        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        if (playerLocation == null || point.distanceTo(playerLocation) >= 32) {
            return;
        }
        LocalPoint lp = LocalPoint.fromWorld(Microbot.getClient(), point);
        if (lp == null) {
            return;
        }
        Polygon poly = Perspective.getCanvasTilePoly(Microbot.getClient(), lp);
        if (poly != null) {
            OverlayUtil.renderPolygon(graphics, poly, color, new Color(0, 0, 0, 50), borderStroke);
        }
        if (!Strings.isNullOrEmpty(label)) {
            Point canvasTextLocation = Perspective.getCanvasTextLocation(Microbot.getClient(), graphics, lp, label, 0);
            if (canvasTextLocation != null) {
                OverlayUtil.renderTextLocation(graphics, canvasTextLocation, label, color);
            }
        }
    }

    @Subscribe
    public void onActorDeath(ActorDeath event) {
        Actor deadActor = event.getActor();
        if (deadActor != null && deadActor == BradleyCombatPlugin.getTarget()) {
            BradleyCombatPlugin.setTarget(null);
        }
    }
}