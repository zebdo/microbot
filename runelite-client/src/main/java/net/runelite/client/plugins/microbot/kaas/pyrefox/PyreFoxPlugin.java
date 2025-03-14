package net.runelite.client.plugins.microbot.kaas.pyrefox;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.kaas.pyrefox.enums.PyreFoxState;
import net.runelite.client.plugins.microbot.kaas.pyrefox.managers.PyreFoxScript;
import net.runelite.client.plugins.microbot.kaas.pyrefox.managers.PyreFoxStateManager;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.misc.TimeUtils;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;
import java.time.Instant;

@PluginDescriptor(
    name = "<html>[<font color=orange>KaaS</font>]" + " PyreFox",
    description = "Hunts pyre foxes for you!",
    tags = {"kaas", "hunter", "pyre", "fox"},
    enabledByDefault = false
)
@Slf4j
public class PyreFoxPlugin extends Plugin {
    public static int catchCounter = 0;
    public static PyreFoxState currentState = PyreFoxState.INITIALIZE;
    public static boolean hasInitialized = false;
    public static boolean hasSlayerTask = true;
    // Hot Reload : Run in debug mode. Reload classes with CTRL + SHIFT + F9.
    // Only context changes, new methods or signature changes don't reload.


    @Inject
    private PyreFoxConfig config;
    private Instant scriptStartTime;

    @Provides
    PyreFoxConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(PyreFoxConfig.class);
    }

    @Inject
    private OverlayManager overlayManager;
    @Inject
    private PyreFoxOverlay overlay;

    @Inject
    public PyreFoxScript script;

    public PyreFoxStateManager stateManager = new PyreFoxStateManager();

    public static Rs2NpcModel target = null;


    @Override
    protected void startUp() throws AWTException
    {
        this.scriptStartTime = Instant.now();

        Rs2Antiban.antibanSetupTemplates.applyHunterSetup();
//        Rs2AntibanSettings.microBreakDurationLow = 1;
//        Rs2AntibanSettings.microBreakDurationHigh = 3;
//        Rs2AntibanSettings.takeMicroBreaks = true;
//        Rs2AntibanSettings.microBreakChance = 0.02;

        if (overlayManager != null) {
            overlayManager.add(overlay);
        }

        script.run(config);
        stateManager.run(config);
    }

    protected void shutDown() {
        script.shutdown();
        stateManager.shutdown();
        Microbot.pauseAllScripts = true;
        overlayManager.remove(overlay);
    }
    int ticks = 10;
    @Subscribe
    public void onGameTick(GameTick tick)
    {
        if (ticks > 0) {
            ticks--;
        } else {
            ticks = 10;
        }

    }

    protected String getTimeRunning() {
        return scriptStartTime != null ? TimeUtils.getFormattedDurationBetween(scriptStartTime, Instant.now()) : "";
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        boolean containsKeyword = (event.getMessage().toLowerCase().contains("you've caught a pyre")); // You've caught a pyre fox
        boolean isCorrectType = event.getType() == ChatMessageType.GAMEMESSAGE || event.getType() == ChatMessageType.SPAM;

        if (isCorrectType && containsKeyword)
        {
            catchCounter++;
        }
    }
}
