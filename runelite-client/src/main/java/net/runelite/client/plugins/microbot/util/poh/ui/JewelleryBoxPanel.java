package net.runelite.client.plugins.microbot.util.poh.ui;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.poh.PohConfig;
import net.runelite.client.plugins.microbot.util.poh.data.JewelleryBox;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

import static net.runelite.client.plugins.microbot.util.poh.PohConfig.CONFIG_GROUP;
import static net.runelite.client.plugins.microbot.util.poh.PohConfig.JEWELLERY_BOX;

public class JewelleryBoxPanel extends JPanel {

    private JComboBox<JewelleryBox> jewelleryBoxCmb;

    public JewelleryBoxPanel(PohConfig config) {
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
        jewelleryBoxCmb = new JComboBox<>(JewelleryBox.values());
        gbc.gridx++;
        add(jewelleryBoxCmb, gbc);


        jewelleryBoxCmb.addActionListener(e ->
        {
            saveJewelleryBoxToConfig();
        });
        jewelleryBoxCmb.setEnabled(false);
        jewelleryBoxCmb.setSelectedItem(config.jewelleryBoxType());
    }

    public void detectJewelleryBox() {
        jewelleryBoxCmb.setSelectedItem(JewelleryBox.getJewelleryBox());
        saveJewelleryBoxToConfig();
    }

    private void saveJewelleryBoxToConfig() {
        Microbot.getConfigManager().setConfiguration(CONFIG_GROUP, JEWELLERY_BOX, jewelleryBoxCmb.getSelectedItem());
    }
}
