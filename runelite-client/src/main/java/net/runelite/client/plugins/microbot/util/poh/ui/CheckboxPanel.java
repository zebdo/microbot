package net.runelite.client.plugins.microbot.util.poh.ui;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.poh.PohConfig;
import net.runelite.client.plugins.microbot.util.poh.data.MountedDigsite;
import net.runelite.client.plugins.microbot.util.poh.data.MountedGlory;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

import static net.runelite.client.plugins.microbot.util.poh.PohConfig.*;

public class CheckboxPanel extends JPanel {

    private JCheckBox mountedGloryCb;
    private JCheckBox mountedDigsiteCb;
    private JCheckBox mountedXericsCb;
    private JCheckBox mountedMythCapeCb;
    private JCheckBox fairyRingCb;
    private JCheckBox spiritTreeCb;
    private JCheckBox wildernessObeliskCb;

    private final PohConfig config;

    public CheckboxPanel(PohConfig config) {
        this.config = config;
        setBorder(new TitledBorder("House Features"));
        GridBagLayout layout = new GridBagLayout();
        setLayout(layout);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets.set(2, 4, 2, 4);

        mountedGloryCb = new JCheckBox("Mounted Amulet of Glory");
        mountedGloryCb.setEnabled(false);
        add(mountedGloryCb, gbc);

        gbc.gridy++;
        mountedDigsiteCb = new JCheckBox("Mounted Digsite Pendant");
        mountedDigsiteCb.setEnabled(false);
        add(mountedDigsiteCb, gbc);

        gbc.gridy++;
        mountedXericsCb = new JCheckBox("Mounted Xeric's Talisman");
        mountedXericsCb.setEnabled(false);
        add(mountedXericsCb, gbc);

        gbc.gridy++;
        mountedMythCapeCb = new JCheckBox("Mounted Mythical Cape");
        mountedMythCapeCb.setEnabled(false);
        add(mountedMythCapeCb, gbc);

        gbc.gridy++;
        fairyRingCb = new JCheckBox("Fairy ring");
        fairyRingCb.setEnabled(false);
        add(fairyRingCb, gbc);

        gbc.gridy++;
        spiritTreeCb = new JCheckBox("Spirit tree");
        spiritTreeCb.setEnabled(false);
        add(spiritTreeCb, gbc);

        gbc.gridy++;
        wildernessObeliskCb = new JCheckBox("Wilderness obelisk");
        wildernessObeliskCb.setEnabled(false);
        add(wildernessObeliskCb, gbc);

        hookListeners();
        loadConfigs();
    }

    private void hookListeners() {
        mountedGloryCb.addActionListener(e -> setBool(MOUNTED_GLORY, mountedGloryCb.isSelected()));
        mountedDigsiteCb.addActionListener(e -> setBool(MOUNTED_DIGSITE, mountedDigsiteCb.isSelected()));
        mountedXericsCb.addActionListener(e -> setBool(MOUNTED_XERICS, mountedXericsCb.isSelected()));
        mountedMythCapeCb.addActionListener(e -> setBool(MOUNTED_MYTHS, mountedMythCapeCb.isSelected()));
        fairyRingCb.addActionListener(e -> setBool(FAIRY_RING, fairyRingCb.isSelected()));
        spiritTreeCb.addActionListener(e -> setBool(SPIRIT_TREE, spiritTreeCb.isSelected()));
        wildernessObeliskCb.addActionListener(e -> setBool(WILDY_OBELISK, wildernessObeliskCb.isSelected()));
    }

    private void setBool(String key, boolean value) {
        Microbot.getConfigManager().setConfiguration(CONFIG_GROUP, key, value);
    }

    private void saveConfigs() {
        setBool(MOUNTED_GLORY, mountedGloryCb.isSelected());
        setBool(MOUNTED_DIGSITE, mountedDigsiteCb.isSelected());
        setBool(MOUNTED_XERICS, mountedXericsCb.isSelected());
        setBool(MOUNTED_MYTHS, mountedMythCapeCb.isSelected());
        setBool(FAIRY_RING, fairyRingCb.isSelected());
        setBool(SPIRIT_TREE, spiritTreeCb.isSelected());
        setBool(WILDY_OBELISK, wildernessObeliskCb.isSelected());
    }

    private void loadConfigs() {
        mountedGloryCb.setSelected(config.hasMountedGlory());
        mountedDigsiteCb.setSelected(config.hasMountedDigsitePendant());
        mountedXericsCb.setSelected(config.hasMountedXericsTalisman());
        mountedMythCapeCb.setSelected(config.hasMountedMythicalCape());
        fairyRingCb.setSelected(config.hasFairyRing());
        spiritTreeCb.setSelected(config.hasSpiritTree());
        wildernessObeliskCb.setSelected(config.hasWildernessObelisk());
    }

    public void detectPohFacilities() {
        mountedGloryCb.setSelected(MountedGlory.getObject() != null);
        mountedDigsiteCb.setSelected(MountedDigsite.getObject() != null);
        saveConfigs();
    }
}
