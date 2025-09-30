package net.runelite.client.plugins.microbot.shortestpath.components;


import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.shortestpath.ShortestPathConfig;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.util.poh.PohTransport;
import net.runelite.client.plugins.microbot.util.poh.data.JewelleryBox;
import net.runelite.client.plugins.microbot.util.poh.data.JewelleryBoxType;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static net.runelite.client.plugins.microbot.shortestpath.ShortestPathPlugin.CONFIG_GROUP;


public class JewelleryBoxPanel extends JPanel {
    String JEWELLERY_BOX = "jewelleryBox";

    private JComboBox<JewelleryBoxType> jewelleryBoxCmb;

    public JewelleryBoxPanel(ShortestPathConfig config) {
        setBorder(new TitledBorder("Jewellery Box"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets.set(2, 4, 2, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        // Jewellery box dropdown
        add(new JLabel("Type"), gbc);
        jewelleryBoxCmb = new JComboBox<>(JewelleryBoxType.values());
        gbc.gridx++;
        add(jewelleryBoxCmb, gbc);


        jewelleryBoxCmb.addActionListener(e -> saveJewelleryBoxToConfig());
        jewelleryBoxCmb.setEnabled(false);
        loadJewelleryBox();
    }

    public void detectJewelleryBox() {
        jewelleryBoxCmb.setSelectedItem(JewelleryBoxType.getCurrentJewelleryBoxType());
        saveJewelleryBoxToConfig();
    }

    public void loadJewelleryBox() {
        try {
            JewelleryBoxType loadedBox = Microbot.getConfigManager().getConfiguration(CONFIG_GROUP, JEWELLERY_BOX, JewelleryBoxType.class);
            jewelleryBoxCmb.setSelectedItem(loadedBox);
        } catch (NullPointerException e) {
            Microbot.log("Failed to load jewellery box config");
            jewelleryBoxCmb.setSelectedItem(JewelleryBoxType.NONE);
        }
    }

    private void saveJewelleryBoxToConfig() {
        JewelleryBoxType config = (JewelleryBoxType) jewelleryBoxCmb.getSelectedItem();
        if (config == null) {
            config = JewelleryBoxType.NONE;
        }
        Microbot.getConfigManager().setConfiguration(CONFIG_GROUP, JEWELLERY_BOX, config);
    }

    public Map<WorldPoint, Set<Transport>> addTransports(WorldPoint exitPortal, Map<WorldPoint, Set<Transport>> allTransports) {
        JewelleryBoxType jewelleryBoxType = (JewelleryBoxType) jewelleryBoxCmb.getSelectedItem();
        if (jewelleryBoxType != JewelleryBoxType.NONE) {
            List<JewelleryBox> teleports = jewelleryBoxType.getAvailableTeleports();
            allTransports.computeIfAbsent(exitPortal, p -> new HashSet<>()).addAll(teleports.stream().map(t -> new PohTransport(exitPortal, t)).collect(Collectors.toList()));
        }
        return allTransports;
    }
}

