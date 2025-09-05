package net.runelite.client.plugins.microbot.SulphurNaguaAIO; // Stelle sicher, dass der Paketname stimmt

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.plugins.microbot.SulphurNaguaAIO.SulphurNaguaScript.SulphurNaguaState; // WICHTIGER IMPORT

import javax.inject.Inject;
import java.awt.*;

public class SulphurNaguaOverlay extends OverlayPanel {
    // Farben und Konstanten
    private static final Color BACKGROUND_COLOR = new Color(0, 0, 0, 150);
    private static final Color NORMAL_COLOR = Color.WHITE;
    private static final Color WARNING_COLOR = Color.YELLOW;
    private static final Color DANGER_COLOR = Color.RED;
    private static final Color SUCCESS_COLOR = Color.GREEN;
    private static final Color PREPARATION_COLOR = new Color(0, 170, 255); // Hellblau für Vorbereitung

    private final SulphurNaguaPlugin plugin;

    @Inject
    SulphurNaguaOverlay(SulphurNaguaPlugin plugin) {
        super(plugin);
        setPosition(OverlayPosition.TOP_LEFT);
        setNaughty();
        this.plugin = plugin;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.setPreferredSize(new Dimension(200, 300));
            panelComponent.setBackgroundColor(BACKGROUND_COLOR);

            // Titel
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Sulphur Nagua Fighter")
                    .leftColor(Color.white)
                    .build());

            // Sicherheitsabfrage, um Abstürze zu verhindern
            if (plugin.sulphurNaguaScript == null) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Status:")
                        .right("Initialisiere...")
                        .build());
                return super.render(graphics);
            }

            // Laufzeit
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Laufzeit:")
                    .right(plugin.getTimeRunning())
                    .rightColor(NORMAL_COLOR)
                    .build());

            // Aktueller Status
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Status:")
                    .right(plugin.sulphurNaguaScript.currentState.name())
                    .rightColor(getStateColor(plugin.sulphurNaguaScript.currentState))
                    .build());

            // Kills
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Kills:")
                    .right(String.valueOf(plugin.sulphurNaguaScript.totalNaguaKills))
                    .rightColor(NORMAL_COLOR)
                    .build());

            // XP-Informationen
            var xpGained = plugin.getXpGained();
            var xpPerHour = plugin.getXpPerHour();
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("XP Gained:")
                    .right(formatNumber(xpGained))
                    .rightColor(NORMAL_COLOR)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("XP/Stunde:")
                    .right(formatNumber(xpPerHour))
                    .rightColor(xpPerHour > 0 ? SUCCESS_COLOR : NORMAL_COLOR)
                    .build());

            // Footer mit Version
            panelComponent.getChildren().add(LineComponent.builder()
                    .right("v" + SulphurNaguaScript.version)
                    .rightColor(new Color(160, 160, 160))
                    .build());

        } catch (Exception ex) {
            Microbot.logStackTrace("SulphurNaguaOverlay Fehler:", ex);
        }
        return super.render(graphics);
    }

    private String formatNumber(long number) {
        return String.format("%,d", number);
    }

    // KORRIGIERT: Die Methode wurde an die neuen Zustände aus dem Script angepasst.
    private Color getStateColor(SulphurNaguaState state) {
        if (state == null) return NORMAL_COLOR;

        switch (state) {
            case FIGHTING:
                return DANGER_COLOR;
            case WALKING_TO_PREP: // Neuer Zustand hinzugefügt
            case WALKING_TO_FIGHT: // Neuer Zustand hinzugefügt
                return WARNING_COLOR;
            case PREPARATION:
                return PREPARATION_COLOR;
            case BANKING:
            default:
                return NORMAL_COLOR;
        }
    }
}
