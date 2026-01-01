package net.runelite.client.plugins.microbot.mouserecorder;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class MouseMacroRecorderPanel extends PluginPanel
{
	private final JLabel statusLabel = new JLabel();
	private final JLabel eventCountLabel = new JLabel();
	private final JLabel moveCountLabel = new JLabel();
	private final JLabel clickCountLabel = new JLabel();
	private final JButton recordButton = new JButton();
	private final JTextArea exportArea = new JTextArea();

	public MouseMacroRecorderPanel(MouseMacroRecorderPlugin plugin)
	{
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
		header.setBackground(ColorScheme.DARK_GRAY_COLOR);
		header.setBorder(new EmptyBorder(10, 10, 10, 10));

		statusLabel.setForeground(Color.WHITE);
		eventCountLabel.setForeground(Color.WHITE);
		moveCountLabel.setForeground(Color.WHITE);
		clickCountLabel.setForeground(Color.WHITE);

		JPanel statusPanel = new JPanel(new GridLayout(2, 2, 8, 4));
		statusPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		statusPanel.add(statusLabel);
		statusPanel.add(eventCountLabel);
		statusPanel.add(moveCountLabel);
		statusPanel.add(clickCountLabel);

		recordButton.addActionListener(actionEvent -> plugin.toggleRecording());

		JButton clearButton = new JButton("Clear");
		clearButton.addActionListener(actionEvent -> plugin.clearRecording());

		JButton exportJsonButton = new JButton("Export JSON");
		exportJsonButton.addActionListener(actionEvent -> copyToClipboard(plugin.exportJson()));

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		buttonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		buttonPanel.add(recordButton);
		buttonPanel.add(clearButton);
		buttonPanel.add(exportJsonButton);

		header.add(statusPanel);
		header.add(Box.createVerticalStrut(8));
		header.add(buttonPanel);

		exportArea.setEditable(false);
		exportArea.setLineWrap(true);
		exportArea.setWrapStyleWord(true);
		exportArea.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		exportArea.setForeground(Color.WHITE);
		exportArea.setBorder(new EmptyBorder(10, 10, 10, 10));

		add(header, BorderLayout.NORTH);
		add(new JScrollPane(exportArea), BorderLayout.CENTER);

		updateState(false, 0, 0, 0);
	}

	public void updateState(boolean recording, int totalEvents, int moveEvents, int clickEvents)
	{
		statusLabel.setText("Status: " + (recording ? "Recording" : "Stopped"));
		eventCountLabel.setText("Events: " + totalEvents);
		moveCountLabel.setText("Moves: " + moveEvents);
		clickCountLabel.setText("Clicks: " + clickEvents);
		recordButton.setText(recording ? "Stop Recording" : "Start Recording");
	}

	public void showExport(String export)
	{
		exportArea.setText(export);
		exportArea.setCaretPosition(0);
	}

	private void copyToClipboard(String export)
	{
		showExport(export);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(export), null);
	}
}
