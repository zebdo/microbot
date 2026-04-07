package net.runelite.client.plugins.microbot.agentserver.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class WalkHandler extends AgentHandler {

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

		WorldPoint destination = new WorldPoint(xNum.intValue(), yNum.intValue(), plane);
		boolean success = Rs2Walker.walkTo(destination);

		WorldPoint playerPos = Microbot.getRs2PlayerStateCache().getLocalPlayerPosition();

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("success", success);

		Map<String, Integer> dest = new LinkedHashMap<>();
		dest.put("x", destination.getX());
		dest.put("y", destination.getY());
		dest.put("plane", destination.getPlane());
		response.put("destination", dest);

		if (playerPos != null) {
			Map<String, Integer> pos = new LinkedHashMap<>();
			pos.put("x", playerPos.getX());
			pos.put("y", playerPos.getY());
			pos.put("plane", playerPos.getPlane());
			response.put("playerPosition", pos);
		}

		sendJson(exchange, 200, response);
	}
}
