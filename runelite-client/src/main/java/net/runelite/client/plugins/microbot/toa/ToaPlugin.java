package net.runelite.client.plugins.microbot.toa;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.toa.puzzleroom.PuzzleScript;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = PluginDescriptor.Default + "Toa",
        description = "Microbot Toa plugin",
        tags = {"toa", "microbot"},
        enabledByDefault = false
)
@Slf4j
public class ToaPlugin extends Plugin {
    @Inject
    private ToaConfig config;
    @Provides
    ToaConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(ToaConfig.class);
    }

    @Inject
    private OverlayManager overlayManager;
    @Inject
    private ToaOverlay toaOverlay;

    @Inject
    ToaScript toaScript;
    @Inject
    PuzzleScript puzzleScript;

    @Override
    protected void startUp() throws AWTException {
        if (overlayManager != null) {
            overlayManager.add(toaOverlay);
        }
        toaScript.run(config);
        puzzleScript.run(config);
    }

    protected void shutDown() {
        toaScript.shutdown();
        puzzleScript.shutdown();
        overlayManager.remove(toaOverlay);
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned e) {
        puzzleScript.sequencePuzzleSolver.onGameObjectSpawned(e);
        puzzleScript.additionPuzzleSolver.onGameObjectSpawned(e);

    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned e)
    {
        puzzleScript.additionPuzzleSolver.onGameObjectDespawned(e);
    }

    @Subscribe
    public void onGraphicsObjectCreated(GraphicsObjectCreated e) {
        puzzleScript.sequencePuzzleSolver.onGraphicsObjectCreated(e);
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        if (event.getType() != ChatMessageType.SPAM && event.getType() != ChatMessageType.GAMEMESSAGE)
        {
            return;
        }

        puzzleScript.readTargetNumberFromChat(event.getMessage());
        puzzleScript.lightPuzzleSolver.onChatMessage(event);
        puzzleScript.additionPuzzleSolver.onChatMessage(event);

        if (event.getMessage().contains("has been completed!"))
        {
            puzzleScript.setNextPuzzleRoom();
        }
    }

}
