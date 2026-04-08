package net.runelite.client.plugins.microbot.agentserver;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("agentServer")
public interface AgentServerConfig extends Config {

	@ConfigItem(
			keyName = "port",
			name = "Port",
			description = "HTTP server port for agent communication",
			position = 0
	)
	@Range(min = 1024, max = 65535)
	default int port() {
		return 8081;
	}

	@ConfigItem(
			keyName = "maxResults",
			name = "Max Results",
			description = "Default max results for list/query endpoints",
			position = 1
	)
	@Range(min = 10, max = 5000)
	default int maxResults() {
		return 200;
	}
}
