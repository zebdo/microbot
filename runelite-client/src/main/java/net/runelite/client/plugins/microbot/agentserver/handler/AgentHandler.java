package net.runelite.client.plugins.microbot.agentserver.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public abstract class AgentHandler implements HttpHandler {

	private static final int MAX_BODY_SIZE = 16 * 1024;

	protected final Gson gson;

	protected AgentHandler(Gson gson) {
		this.gson = gson;
	}

	public abstract String getPath();

	protected abstract void handleRequest(HttpExchange exchange) throws IOException;

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
		exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
		exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

		if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
			exchange.sendResponseHeaders(204, -1);
			return;
		}

		try {
			handleRequest(exchange);
		} catch (Exception e) {
			log.error("Error handling {}", exchange.getRequestURI(), e);
			sendJson(exchange, 500, errorResponse("Internal server error: " + e.getMessage()));
		}
	}

	protected void requireGet(HttpExchange exchange) throws HttpMethodException {
		if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
			throw new HttpMethodException("Method not allowed. Use GET.");
		}
	}

	protected void requirePost(HttpExchange exchange) throws HttpMethodException {
		if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
			throw new HttpMethodException("Method not allowed. Use POST.");
		}
	}

	protected void sendJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
		String json = gson.toJson(body);
		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "application/json");
		exchange.sendResponseHeaders(statusCode, bytes.length);
		try (OutputStream os = exchange.getResponseBody()) {
			os.write(bytes);
		}
	}

	protected Map<String, Object> errorResponse(String message) {
		Map<String, Object> err = new LinkedHashMap<>();
		err.put("error", message);
		return err;
	}

	protected Map<String, String> parseQuery(URI uri) {
		Map<String, String> params = new LinkedHashMap<>();
		String query = uri.getQuery();
		if (query == null) return params;
		for (String pair : query.split("&")) {
			String[] kv = pair.split("=", 2);
			if (kv.length == 2) {
				params.put(kv[0], URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
			} else if (kv.length == 1) {
				params.put(kv[0], "");
			}
		}
		return params;
	}

	protected Map<String, Object> readJsonBody(HttpExchange exchange) throws IOException {
		byte[] bodyBytes = exchange.getRequestBody().readAllBytes();
		if (bodyBytes.length > MAX_BODY_SIZE) {
			throw new IllegalArgumentException("Request body too large");
		}
		String body = new String(bodyBytes, StandardCharsets.UTF_8);
		@SuppressWarnings("unchecked")
		Map<String, Object> parsed = gson.fromJson(body, LinkedHashMap.class);
		return parsed;
	}

	protected int getIntParam(Map<String, String> params, String key, int defaultValue) {
		String val = params.get(key);
		if (val == null) return defaultValue;
		try {
			return Integer.parseInt(val);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	protected <T> Map<String, Object> paginate(List<T> items, int offset, int limit, String key) {
		int total = items.size();
		int start = Math.min(offset, total);
		int end = Math.min(offset + limit, total);
		List<T> page = items.subList(start, end);
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("count", page.size());
		result.put("total", total);
		result.put("offset", offset);
		result.put("limit", limit);
		result.put(key, page);
		return result;
	}

	protected String getSubPath(HttpExchange exchange, String basePath) {
		String full = exchange.getRequestURI().getPath();
		if (full.length() <= basePath.length()) return "";
		return full.substring(basePath.length());
	}

	public static class HttpMethodException extends Exception {
		public HttpMethodException(String message) {
			super(message);
		}
	}
}
