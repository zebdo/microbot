package net.runelite.client.plugins.microbot.agentserver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Provides;
import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.agentserver.handler.*;
import net.runelite.client.ui.DrawManager;

import javax.inject.Inject;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@PluginDescriptor(
		name = PluginDescriptor.Mocrosoft + "Agent Server",
		description = "HTTP server for AI agent communication - exposes widget inspection, game interaction, and state endpoints",
		tags = {"agent", "ai", "server", "automation"},
		enabledByDefault = true
)
@Slf4j
public class AgentServerPlugin extends Plugin {

	@Inject
	private AgentServerConfig config;

	@Inject
	private Client client;

	@Inject
	private DrawManager drawManager;

	private HttpServer server;
	private ExecutorService executor;
	private Thread shutdownHook;
	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

	@Provides
	AgentServerConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(AgentServerConfig.class);
	}

	@Override
	protected void startUp() throws Exception {
		int port = config.port();
		int maxResults = config.maxResults();

		stopServer();

		executor = Executors.newFixedThreadPool(4, new ThreadFactory() {
			private final AtomicInteger count = new AtomicInteger(1);

			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r, "AgentServer-" + count.getAndIncrement());
				t.setDaemon(true);
				return t;
			}
		});

		try {
			server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
		} catch (java.net.BindException e) {
			log.warn("Port {} already in use, killing existing process", port);
			killProcessOnPort(port);
			Thread.sleep(500);
			server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
		}

		server.setExecutor(executor);

		List<AgentHandler> handlers = Arrays.asList(
				new WidgetListHandler(gson, maxResults),
				new WidgetSearchHandler(gson, maxResults),
				new WidgetDescribeHandler(gson),
				new WidgetClickHandler(gson),
				new StateHandler(gson, client),
				new InventoryHandler(gson),
				new NpcHandler(gson, maxResults),
				new ObjectHandler(gson, maxResults),
				new WalkHandler(gson),
				new BankHandler(gson),
				new DialogueHandler(gson),
				new GroundItemHandler(gson, maxResults),
				new SkillsHandler(gson, client),
				new ScriptHandler(gson),
				new LoginHandler(gson, client),
				new ScreenshotHandler(gson, client, drawManager),
				new VarbitHandler(gson),
				new WidgetInvokeHandler(gson),
				new SettingsHandler(gson),
				new KeyboardHandler(gson)
		);

		for (AgentHandler handler : handlers) {
			server.createContext(handler.getPath(), handler);
		}

		shutdownHook = new Thread(this::stopServer, "AgentServer-Shutdown");
		Runtime.getRuntime().addShutdownHook(shutdownHook);
		server.start();
		log.info("Agent server started on port {} with {} endpoints", port, handlers.size());
	}

	@Override
	protected void shutDown() throws Exception {
		stopServer();
	}

	private synchronized void stopServer() {
		if (server != null) {
			server.stop(0);
			server = null;
			log.info("Agent server stopped");
		}
		if (executor != null) {
			executor.shutdownNow();
			try {
				executor.awaitTermination(2, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			executor = null;
		}
		if (shutdownHook != null) {
			try {
				Runtime.getRuntime().removeShutdownHook(shutdownHook);
			} catch (IllegalStateException ignored) {
			}
			shutdownHook = null;
		}
	}

	private void killProcessOnPort(int port) {
		try {
			String os = System.getProperty("os.name", "").toLowerCase();
			ProcessBuilder pb;
			if (os.contains("win")) {
				pb = new ProcessBuilder("cmd", "/c",
						"for /f \"tokens=5\" %a in ('netstat -aon ^| findstr :" + port + "') do taskkill /PID %a /F");
			} else {
				pb = new ProcessBuilder("bash", "-c", "fuser -k " + port + "/tcp 2>/dev/null || lsof -ti:" + port + " | xargs kill 2>/dev/null");
			}
			Process p = pb.start();
			p.waitFor(5, TimeUnit.SECONDS);
			log.info("Killed process on port {}", port);
		} catch (Exception e) {
			log.warn("Failed to kill process on port {}: {}", port, e.getMessage());
		}
	}
}
