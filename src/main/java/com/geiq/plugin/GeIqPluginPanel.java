package com.geiq.plugin;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;

class GeIqPluginPanel extends PluginPanel
{
	private static final String GE_IQ_URL = "https://ge-iq.com";

	private final JLabel statusValue = new JLabel("Not configured");
	private final JLabel syncCodeValue = new JLabel("-");
	private final JLabel totalSyncedValue = new JLabel("0 trades");
	private final JLabel lastSyncValue = new JLabel("Never");

	private long lastSyncMs = 0;

	GeIqPluginPanel()
	{
		super(false);
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(new EmptyBorder(10, 10, 10, 10));

		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);
		content.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel title = new JLabel("GE IQ Sync");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(ColorScheme.BRAND_ORANGE);
		title.setAlignmentX(Component.LEFT_ALIGNMENT);
		content.add(title);
		content.add(Box.createVerticalStrut(12));

		content.add(buildRow("Status", statusValue));
		content.add(Box.createVerticalStrut(10));
		content.add(buildRow("Sync code", syncCodeValue));
		content.add(Box.createVerticalStrut(10));
		content.add(buildRow("Total synced", totalSyncedValue));
		content.add(Box.createVerticalStrut(10));
		content.add(buildRow("Last sync", lastSyncValue));
		content.add(Box.createVerticalStrut(18));

		JButton openButton = new JButton("Open GE IQ");
		openButton.setFocusPainted(false);
		openButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		openButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
		openButton.addActionListener(e -> LinkBrowser.browse(GE_IQ_URL));
		content.add(openButton);

		add(content, BorderLayout.NORTH);

		Timer refreshTimer = new Timer(15000, e -> updateLastSyncDisplay());
		refreshTimer.start();
	}

	private JPanel buildRow(String label, JLabel valueLabel)
	{
		JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);
		row.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel l = new JLabel(label);
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		l.setAlignmentX(Component.LEFT_ALIGNMENT);

		valueLabel.setFont(FontManager.getRunescapeFont());
		valueLabel.setForeground(ColorScheme.BRAND_ORANGE);
		valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

		row.add(l);
		row.add(valueLabel);
		return row;
	}

	void update(String syncCode, boolean enabled, int totalSynced, long lastSync)
	{
		this.lastSyncMs = lastSync;
		SwingUtilities.invokeLater(() -> {
			String code = syncCode == null ? "" : syncCode.trim().toUpperCase();
			boolean configured = code.length() == 6;

			if (!configured)
			{
				statusValue.setText("Not configured");
				statusValue.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
			}
			else if (!enabled)
			{
				statusValue.setText("Disabled");
				statusValue.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			}
			else
			{
				statusValue.setText("Connected");
				statusValue.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
			}

			syncCodeValue.setText(configured ? code : "-");
			totalSyncedValue.setText(totalSynced + (totalSynced == 1 ? " trade" : " trades"));
			updateLastSyncDisplay();
		});
	}

	private void updateLastSyncDisplay()
	{
		if (lastSyncMs <= 0)
		{
			lastSyncValue.setText("Never");
			return;
		}
		long diff = System.currentTimeMillis() - lastSyncMs;
		long sec = diff / 1000;
		String text;
		if (sec < 60)
		{
			text = sec + "s ago";
		}
		else if (sec < 3600)
		{
			text = (sec / 60) + "m ago";
		}
		else if (sec < 86400)
		{
			text = (sec / 3600) + "h ago";
		}
		else
		{
			text = (sec / 86400) + "d ago";
		}
		lastSyncValue.setText(text);
	}
}
