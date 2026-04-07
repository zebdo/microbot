package net.runelite.client.plugins.microbot.agentserver.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.MicrobotConfig;
import net.runelite.client.plugins.microbot.util.settings.Rs2Settings;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class SettingsHandler extends AgentHandler {

	private static final Map<String, String> VALID_CONFIG_KEYS = Map.of(
		"disableLevelUpInterface", MicrobotConfig.keyDisableLevelUpInterface,
		"disableWorldSwitcherConfirmation", MicrobotConfig.keyDisableWorldSwitcherConfirmation
	);

	public SettingsHandler(Gson gson) {
		super(gson);
	}

	@Override
	public String getPath() {
		return "/settings";
	}

	@Override
	protected void handleRequest(HttpExchange exchange) throws IOException {
		String subPath = getSubPath(exchange, "/settings");

		if ("/level-up".equals(subPath)) {
			handleLevelUp(exchange);
		} else if ("/config".equals(subPath)) {
			handleConfig(exchange);
		} else {
			sendJson(exchange, 404, errorResponse("Unknown sub-path: " + subPath));
		}
	}

	private void handleLevelUp(HttpExchange exchange) throws IOException {
		if ("GET".equals(exchange.getRequestMethod())) {
			boolean enabled = Rs2Settings.isLevelUpNotificationsEnabled();
			Map<String, Object> response = new LinkedHashMap<>();
			response.put("enabled", enabled);
			sendJson(exchange, 200, response);
			return;
		}

		try {
			requirePost(exchange);
		} catch (HttpMethodException e) {
			sendJson(exchange, 405, errorResponse(e.getMessage()));
			return;
		}

		Map<String, Object> request;
		try {
			request = readJsonBody(exchange);
		} catch (Exception e) {
			sendJson(exchange, 400, errorResponse("Invalid JSON body"));
			return;
		}

		Boolean enable = (Boolean) request.get("enable");
		if (enable == null) {
			sendJson(exchange, 400, errorResponse("Required: enable (true/false)"));
			return;
		}

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("action", enable ? "enable" : "disable");

		try {
			boolean result;
			if (enable) {
				result = Rs2Settings.enableLevelUpNotifications(true);
			} else {
				result = Rs2Settings.disableLevelUpNotifications(true);
			}
			response.put("success", result);
			response.put("enabled", Rs2Settings.isLevelUpNotificationsEnabled());
		} catch (Exception e) {
			response.put("success", false);
			response.put("error", e.getMessage());
		}

		sendJson(exchange, 200, response);
	}

	private void handleConfig(HttpExchange exchange) throws IOException {
		ConfigManager configManager = Microbot.getConfigManager();
		if (configManager == null) {
			sendJson(exchange, 500, errorResponse("ConfigManager not available"));
			return;
		}

		if ("GET".equals(exchange.getRequestMethod())) {
			MicrobotConfig config = configManager.getConfig(MicrobotConfig.class);
			Map<String, Object> response = new LinkedHashMap<>();
			response.put("disableLevelUpInterface", config != null && config.disableLevelUpInterface());
			response.put("disableWorldSwitcherConfirmation", config != null && config.disableWorldSwitcherConfirmation());
			sendJson(exchange, 200, response);
			return;
		}

		try {
			requirePost(exchange);
		} catch (HttpMethodException e) {
			sendJson(exchange, 405, errorResponse(e.getMessage()));
			return;
		}

		Map<String, Object> request;
		try {
			request = readJsonBody(exchange);
		} catch (Exception e) {
			sendJson(exchange, 400, errorResponse("Invalid JSON body"));
			return;
		}

		String key = (String) request.get("key");
		Object value = request.get("value");
		if (key == null || value == null) {
			sendJson(exchange, 400, errorResponse("Required: key, value"));
			return;
		}

		String configKey = VALID_CONFIG_KEYS.get(key);
		if (configKey == null) {
			sendJson(exchange, 400, errorResponse("Unknown key: " + key + ". Valid keys: " + VALID_CONFIG_KEYS.keySet()));
			return;
		}

		configManager.setConfiguration(MicrobotConfig.configGroup, configKey, value.toString());

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("key", key);
		response.put("configKey", configKey);
		response.put("value", value);
		response.put("success", true);
		sendJson(exchange, 200, response);
	}
}
