package net.runelite.client.plugins.microbot.inputrecorder;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.inputrecorder.models.InputRecordingSession;
import net.runelite.client.plugins.microbot.inputrecorder.replay.ActionReplayer;
import net.runelite.client.plugins.microbot.inputrecorder.replay.ReplayOptions;
import net.runelite.client.plugins.microbot.inputrecorder.service.InputRecordingService;
import net.runelite.client.plugins.microbot.inputrecorder.ui.InputRecorderOverlay;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;

import javax.inject.Inject;
import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * Microbot Input Recorder Plugin
 *
 * <p>Records mouse and keyboard input as structured menu actions that can be:
 * <ul>
 *   <li>Replayed later to automate repetitive tasks</li>
 *   <li>Analyzed for behavioral profiling and ML training</li>
 *   <li>Compared across different play styles or accounts</li>
 *   <li>Exported as datasets for research</li>
 * </ul>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li>Captures all mouse clicks, movements, and keyboard input</li>
 *   <li>Converts raw input into OSRS-compatible menu actions</li>
 *   <li>Smart mouse movement compression to reduce data volume</li>
 *   <li>Session management (start/stop/pause/resume)</li>
 *   <li>JSON serialization for portability and analysis</li>
 *   <li>Replay engine with timing control and error handling</li>
 *   <li>Privacy controls (can exclude chat, account names)</li>
 * </ul>
 *
 * <p><b>Usage:</b></p>
 * <ul>
 *   <li>Ctrl+Shift+R: Start/stop recording</li>
 *   <li>Ctrl+Shift+P: Pause/resume recording</li>
 *   <li>Configure settings via plugin panel</li>
 *   <li>Recordings saved to ~/.microbot/recordings/</li>
 * </ul>
 *
 * @author Microbot Team
 * @version 1.0
 */
@Slf4j
@PluginDescriptor(
        name = PluginDescriptor.Default + "Input Recorder",
        description = "Records mouse and keyboard input as menu actions for analysis and replay",
        tags = {"microbot", "input", "recorder", "replay", "ml", "automation"},
        enabledByDefault = false
)
public class InputRecorderPlugin extends Plugin {

    @Inject
    private Client client;

    @Inject
    private InputRecorderConfig config;

    @Inject
    private EventBus eventBus;

    @Inject
    private InputRecordingService recordingService;

    @Inject
    private ActionReplayer actionReplayer;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private InputRecorderOverlay overlay;

    @Inject
    private KeyManager keyManager;

    private boolean wasLoggedIn = false;

    // Hotkey listeners
    private final HotkeyListener toggleRecordingHotkey = new HotkeyListener(() -> config.toggleRecordingHotkey())
    {
        @Override
        public void hotkeyPressed()
        {
            if (recordingService.isRecording()) {
                handleStopRecording();
            } else {
                handleStartRecording();
            }
        }
    };

    private final HotkeyListener pauseRecordingHotkey = new HotkeyListener(() -> config.pauseRecordingHotkey())
    {
        @Override
        public void hotkeyPressed()
        {
            if (recordingService.isRecording()) {
                if (recordingService.getCurrentSession().isPaused()) {
                    recordingService.resumeRecording();
                    log.info("Recording resumed");
                } else {
                    recordingService.pauseRecording();
                    log.info("Recording paused");
                }
            }
        }
    };

    private final HotkeyListener discardSessionHotkey = new HotkeyListener(() -> config.discardSessionHotkey())
    {
        @Override
        public void hotkeyPressed()
        {
            if (recordingService.isRecording()) {
                recordingService.discardSession();
                log.info("Recording session discarded");
            }
        }
    };

    /**
     * Provides the plugin configuration
     */
    @Provides
    InputRecorderConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(InputRecorderConfig.class);
    }

    /**
     * Plugin startup - register event handlers and hotkeys
     */
    @Override
    protected void startUp() {
        log.info("Input Recorder plugin started");

        // Register the recording service with event bus
        eventBus.register(recordingService);

        // Add overlay
        overlayManager.add(overlay);

        // Register hotkeys
        registerHotkeys();

        // Apply configuration
        applyConfiguration();

        log.info("Input Recorder ready. Storage: {}", recordingService.getRecordingDirectory());
    }

    /**
     * Plugin shutdown - cleanup resources
     */
    @Override
    protected void shutDown() {
        log.info("Input Recorder plugin stopping");

        // Stop any active recording
        if (recordingService.isRecording()) {
            recordingService.stopRecording();
        }

        // Stop any active replay
        if (actionReplayer.isReplaying()) {
            actionReplayer.stopReplay();
        }

        // Unregister from event bus
        eventBus.unregister(recordingService);

        // Remove overlay
        overlayManager.remove(overlay);

        // Unregister hotkeys
        unregisterHotkeys();

        log.info("Input Recorder stopped");
    }

    /**
     * Registers keyboard hotkeys for recording control
     */
    private void registerHotkeys() {
        keyManager.registerKeyListener(toggleRecordingHotkey);
        keyManager.registerKeyListener(pauseRecordingHotkey);
        keyManager.registerKeyListener(discardSessionHotkey);
        log.debug("Hotkeys registered");
    }

    /**
     * Unregisters all hotkeys
     */
    private void unregisterHotkeys() {
        keyManager.unregisterKeyListener(toggleRecordingHotkey);
        keyManager.unregisterKeyListener(pauseRecordingHotkey);
        keyManager.unregisterKeyListener(discardSessionHotkey);
        log.debug("Hotkeys unregistered");
    }

    /**
     * Handles starting a new recording session
     */
    private void handleStartRecording() {
        if (!config.enableRecording()) {
            log.warn("Recording is disabled in config. Enable it first.");
            return;
        }

        String profileName = config.profileName();
        String sessionName = generateSessionName();

        recordingService.startRecording(profileName, sessionName);
        log.info("Started recording: {} [{}]", sessionName, profileName);
    }

    /**
     * Handles stopping the current recording session
     */
    private void handleStopRecording() {
        InputRecordingSession session = recordingService.stopRecording();
        if (session != null) {
            log.info("Stopped recording: {} ({} actions, {} duration)",
                    session.getName(),
                    session.getTotalActions(),
                    formatDuration(session.getDurationMs()));
        }
    }

    /**
     * Generates a session name based on current context
     */
    private String generateSessionName() {
        // Could be enhanced to include location, activity, etc.
        return "Session " + System.currentTimeMillis();
    }

    /**
     * Formats duration for display
     */
    private String formatDuration(Long millis) {
        if (millis == null) {
            return "0s";
        }

        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }

    /**
     * Applies configuration settings to services
     */
    private void applyConfiguration() {
        recordingService.setRecordMouseMoves(config.recordMouseMoves());
        recordingService.setRecordKeyboardInput(config.recordKeyboardInput());
        recordingService.setRecordChatInput(config.recordChatInput());
    }

    /**
     * Handles configuration changes
     */
    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (!event.getGroup().equals("inputrecorder")) {
            return;
        }

        log.debug("Config changed: {} = {}", event.getKey(), event.getNewValue());
        applyConfiguration();
    }

    /**
     * Handles game state changes (e.g., login/logout)
     */
    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        GameState state = event.getGameState();

        // Auto-start recording on login if enabled
        if (state == GameState.LOGGED_IN && !wasLoggedIn) {
            wasLoggedIn = true;

            if (config.autoStartRecording() && !recordingService.isRecording()) {
                log.info("Auto-starting recording (logged in)");
                handleStartRecording();
            }
        }

        // Auto-stop recording on logout
        if (state == GameState.LOGIN_SCREEN || state == GameState.CONNECTION_LOST) {
            wasLoggedIn = false;

            if (recordingService.isRecording()) {
                log.info("Auto-stopping recording (logged out)");
                handleStopRecording();
            }
        }
    }

    // ===== Public API for external access =====

    /**
     * Starts a new recording session programmatically
     */
    public void startRecording(String profileName, String sessionName) {
        recordingService.startRecording(profileName, sessionName);
    }

    /**
     * Stops the current recording session
     */
    public InputRecordingSession stopRecording() {
        return recordingService.stopRecording();
    }

    /**
     * Pauses the current recording
     */
    public void pauseRecording() {
        recordingService.pauseRecording();
    }

    /**
     * Resumes a paused recording
     */
    public void resumeRecording() {
        recordingService.resumeRecording();
    }

    /**
     * Checks if currently recording
     */
    public boolean isRecording() {
        return recordingService.isRecording();
    }

    /**
     * Gets the current recording session
     */
    public InputRecordingSession getCurrentSession() {
        return recordingService.getCurrentSession();
    }

    /**
     * Lists all saved recording sessions
     */
    public java.util.List<File> listSavedSessions() {
        return recordingService.listSavedSessions();
    }

    /**
     * Loads a recording session from a file
     */
    public InputRecordingSession loadSession(File file) throws IOException {
        return recordingService.loadSession(file);
    }

    /**
     * Replays a recording session with default options
     */
    public void replaySession(InputRecordingSession session) {
        actionReplayer.replay(session);
    }

    /**
     * Replays a recording session with custom options
     */
    public void replaySession(InputRecordingSession session, ReplayOptions options) {
        actionReplayer.replay(session, options);
    }

    /**
     * Stops the current replay
     */
    public void stopReplay() {
        actionReplayer.stopReplay();
    }

    /**
     * Pauses the current replay
     */
    public void pauseReplay() {
        actionReplayer.pauseReplay();
    }

    /**
     * Resumes a paused replay
     */
    public void resumeReplay() {
        actionReplayer.resumeReplay();
    }

    /**
     * Checks if currently replaying
     */
    public boolean isReplaying() {
        return actionReplayer.isReplaying();
    }

    /**
     * Gets the replay progress (0.0 to 1.0)
     */
    public double getReplayProgress() {
        return actionReplayer.getProgress();
    }
}
