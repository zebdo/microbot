package net.runelite.client.plugins.microbot.toa;

import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

import javax.inject.Inject;
import java.awt.*;

public class ToaOverlay extends OverlayPanel {
    private final ToaPlugin plugin;

    private final ModelOutlineRenderer modelOutlineRenderer;

    @Inject
    ToaOverlay(ToaPlugin plugin, ModelOutlineRenderer modelOutlineRenderer) {
        this.plugin = plugin;
        this.modelOutlineRenderer = modelOutlineRenderer;
        this.setPosition(OverlayPosition.DYNAMIC);
        this.setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        panelComponent.setPreferredLocation(new Point(200, 20));
        panelComponent.setPreferredSize(new Dimension(200, 300));
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Micro TOA V1.0.0")
                .color(Color.GREEN)
                .build());

        panelComponent.getChildren().add(LineComponent.builder().build());

        if (plugin.puzzleScript == null) return super.render(graphics);

        if (plugin.puzzleScript.puzzleroomState.getCurrentPuzzle() != null) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Current puzzle")
                    .right(plugin.puzzleScript.puzzleroomState.getCurrentPuzzle().name())
                    .build());
        }

        if (plugin.puzzleScript.puzzleroomState.getCurrentRoom() != null) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Current room")
                    .right(plugin.puzzleScript.puzzleroomState.getCurrentRoom().name())
                    .build());
        }


        if (plugin.puzzleScript.puzzleroomState.getNextRoom() != null) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Next room")
                    .right(plugin.puzzleScript.puzzleroomState.getNextRoom().name())
                    .build());
        }




        return super.render(graphics);
    }
}
