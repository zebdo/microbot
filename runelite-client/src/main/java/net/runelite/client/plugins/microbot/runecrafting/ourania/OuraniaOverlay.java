package net.runelite.client.plugins.microbot.runecrafting.ourania;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import javax.inject.Inject;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.misc.TimeUtils;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.util.ColorUtil;

public class OuraniaOverlay extends OverlayPanel
{
	private final OuraniaPlugin plugin;
	private final OuraniaConfig config;
	private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");

	@Inject
	OuraniaOverlay(OuraniaPlugin plugin, OuraniaConfig config)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
		setNaughty();
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		try
		{
			panelComponent.setPreferredSize(new Dimension(200, 300));
			panelComponent.getChildren().add(TitleComponent.builder()
				.text("Ourania Altar V" + OuraniaPlugin.version)
				.color(ColorUtil.fromHex("0077B6"))
				.build());

			panelComponent.getChildren().add(LineComponent.builder().build());

			panelComponent.getChildren().add(LineComponent.builder()
				.left("Status:")
				.right(Microbot.status)
				.build());

			panelComponent.getChildren().add(LineComponent.builder()
				.left("Run time:")
				.right(TimeUtils.getFormattedDurationBetween(plugin.getStartTime(), Instant.now()))
				.build());

			if (!config.toggleProfitCalculator())
			{
				panelComponent.getChildren().add(LineComponent.builder()
					.left("Profit:")
					.right(Integer.toString(plugin.getProfit()))
					.build());
			}
		}
		catch (Exception ex)
		{
			Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
		}
		return super.render(graphics);
	}
}
