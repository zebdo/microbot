package net.runelite.client.plugins.microbot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLiteProperties;
import net.runelite.client.ui.ClientUI;
import org.jetbrains.annotations.NotNull;

@Slf4j
@Singleton
public class MicrobotVersionChecker
{
	private final AtomicBoolean newVersionAvailable = new AtomicBoolean(false);
	private static final String REMOTE_VERSION_URL = "https://microbot.cloud/api/version/client";
	private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory()
	{
		@Override
		public Thread newThread(@NotNull Runnable r)
		{
			Thread t = new Thread(r);
			t.setName(this.getClass().getSimpleName());
			return t;
		}
	});

	private void runVersionCheck()
	{
		if (newVersionAvailable.get())
		{
			return;
		}

		try
		{
			String remoteVersion = fetchRemoteVersion();
			String localVersion = RuneLiteProperties.getMicrobotVersion();
			if (remoteVersion != null && !remoteVersion.trim().equals(localVersion))
			{
				newVersionAvailable.set(true);
				notifyNewVersionAvailable(remoteVersion, localVersion);
			}
			else
			{
				log.debug("Microbot client is up to date: {}", localVersion);
			}
		}
		catch (Exception e)
		{
			log.warn("Could not check Microbot client version", e);
		}
	}

	private String fetchRemoteVersion() throws Exception
	{
		URL url = new URL(REMOTE_VERSION_URL);
		try (BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream())))
		{
			return in.readLine();
		}
	}

	private void notifyNewVersionAvailable(String remoteVersion, String localVersion)
	{
		SwingUtilities.invokeLater(() -> {
			try
			{
				String oldTitle = ClientUI.getFrame().getTitle();
				if (!oldTitle.contains("(NEW CLIENT AVAILABLE)"))
				{
					log.info("New Microbot client version available: {} (current: {})", remoteVersion, localVersion);
					ClientUI.getFrame().setTitle(oldTitle + " (NEW CLIENT AVAILABLE)");
				}
			}
			catch (Exception e)
			{
				log.warn("Failed to update client title", e);
			}
		});
	}

	public void checkForUpdate()
	{
		scheduledExecutorService.scheduleWithFixedDelay(this::runVersionCheck, 0, 10, TimeUnit.MINUTES);
	}

	public void shutdown() {
		scheduledExecutorService.shutdownNow();
		newVersionAvailable.set(false);
	}
}
