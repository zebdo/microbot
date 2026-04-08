package net.runelite.client.plugins.microbot.agentserver.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.walker.WalkerState;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class WalkHandler extends AgentHandler {

	private static final int DEFAULT_TIMEOUT_SECONDS = 30;
	private static final int MAX_TIMEOUT_SECONDS = 600;

	private static final AtomicInteger WALK_THREAD_COUNT = new AtomicInteger(1);
	private static final ExecutorService WALK_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "AgentServer-Walk-" + WALK_THREAD_COUNT.getAndIncrement());
		t.setDaemon(true);
		return t;
	});

	private static volatile Future<WalkerState> activeWalk;
	private static volatile WorldPoint activeWalkTarget;

	public WalkHandler(Gson gson) {
		super(gson);
	}

	@Override
	public String getPath() {
		return "/walk";
	}

	@Override
	protected void handleRequest(HttpExchange exchange) throws IOException {
		try {
			requirePost(exchange);
		} catch (HttpMethodException e) {
			sendJson(exchange, 405, errorResponse(e.getMessage()));
			return;
		}

		Map<String, Object> body;
		try {
			body = readJsonBody(exchange);
		} catch (Exception e) {
			sendJson(exchange, 400, errorResponse("Invalid JSON body"));
			return;
		}

		Number xNum = (Number) body.get("x");
		Number yNum = (Number) body.get("y");
		if (xNum == null || yNum == null) {
			sendJson(exchange, 400, errorResponse("Missing required fields: x, y"));
			return;
		}

		Number planeNum = (Number) body.get("plane");
		int plane = planeNum != null ? planeNum.intValue() : 0;

		boolean wait = Boolean.TRUE.equals(body.get("wait"));
		int timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
		if (body.get("timeout") instanceof Number) {
			timeoutSeconds = ((Number) body.get("timeout")).intValue();
			if (timeoutSeconds <= 0) timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
			if (timeoutSeconds > MAX_TIMEOUT_SECONDS) timeoutSeconds = MAX_TIMEOUT_SECONDS;
		}

		WorldPoint destination = new WorldPoint(xNum.intValue(), yNum.intValue(), plane);

		Future<WalkerState> walkFuture = submitWalk(destination);

		Map<String, Object> response = new LinkedHashMap<>();
		Map<String, Integer> dest = new LinkedHashMap<>();
		dest.put("x", destination.getX());
		dest.put("y", destination.getY());
		dest.put("plane", destination.getPlane());
		response.put("destination", dest);

		if (!wait) {
			response.put("success", true);
			response.put("walking", true);
			response.put("message", "Walk initiated");
			addPlayerPosition(response);
			sendJson(exchange, 200, response);
			return;
		}

		WalkerState state;
		boolean timedOut = false;
		try {
			state = walkFuture.get(timeoutSeconds, TimeUnit.SECONDS);
		} catch (TimeoutException te) {
			state = null;
			timedOut = true;
		} catch (Exception e) {
			log.warn("Walk failed: {}", e.toString());
			response.put("success", false);
			response.put("error", "Walk failed: " + e.getMessage());
			addPlayerPosition(response);
			sendJson(exchange, 500, response);
			return;
		}

		if (timedOut) {
			response.put("success", false);
			response.put("walking", true);
			response.put("timedOut", true);
			response.put("message", "Walk did not complete within " + timeoutSeconds + "s; still in progress");
		} else {
			response.put("success", state == WalkerState.ARRIVED);
			response.put("walking", false);
			response.put("state", state == null ? "UNKNOWN" : state.name());
		}
		addPlayerPosition(response);
		sendJson(exchange, 200, response);
	}

	private static synchronized Future<WalkerState> submitWalk(WorldPoint destination) {
		if (activeWalk != null && !activeWalk.isDone() && destination.equals(activeWalkTarget)) {
			return activeWalk;
		}
		activeWalkTarget = destination;
		activeWalk = WALK_EXECUTOR.submit(() -> Rs2Walker.walkWithState(destination));
		return activeWalk;
	}

	private static void addPlayerPosition(Map<String, Object> response) {
		WorldPoint playerPos = Microbot.getRs2PlayerStateCache().getLocalPlayerPosition();
		if (playerPos != null) {
			Map<String, Integer> pos = new LinkedHashMap<>();
			pos.put("x", playerPos.getX());
			pos.put("y", playerPos.getY());
			pos.put("plane", playerPos.getPlane());
			response.put("playerPosition", pos);
		}
	}
}
