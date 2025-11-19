package net.runelite.client.plugins.microbot.inputrecorder.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.RuneLiteProperties;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.inputrecorder.models.InputRecordingSession;
import net.runelite.client.plugins.microbot.inputrecorder.models.RecordedActionType;
import net.runelite.client.plugins.microbot.inputrecorder.models.RecordedMenuAction;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Core service for recording mouse and keyboard input as menu actions.
 *
 * <p>This service hooks into RuneLite's event system and Java AWT input listeners
 * to capture all player input and convert it into structured RecordedMenuAction objects.</p>
 *
 * <p>Key responsibilities:</p>
 * <ul>
 *   <li>Subscribe to RuneLite events (MenuOptionClicked, GameTick)</li>
 *   <li>Hook mouse and keyboard listeners to the game canvas</li>
 *   <li>Convert raw input into normalized menu actions</li>
 *   <li>Manage recording sessions (start/stop/pause)</li>
 *   <li>Compress and throttle mouse movement data</li>
 *   <li>Serialize sessions to JSON files</li>
 * </ul>
 */
@Slf4j
@Singleton
public class InputRecordingService {

    @Inject
    private Client client;

    @Getter
    private InputRecordingSession currentSession;

    private int currentGameTick = 0;

    // Constants
    private static final String MICROBOT_EVENT_SOURCE = "Microbot";

    // Mouse movement compression
    private Point lastRecordedMousePosition;
    private long lastMouseMoveTimestamp = 0;
    private static final int MOUSE_MOVE_THROTTLE_MS = 50; // Record mouse moves at most every 50ms
    private static final int MOUSE_MOVE_DISTANCE_THRESHOLD = 10; // Only record if moved >10 pixels

    // Keyboard state tracking
    private final Set<Integer> pressedKeys = new HashSet<>();
    private volatile boolean shiftPressed = false;
    private volatile boolean ctrlPressed = false;
    private volatile boolean altPressed = false;

    // Event queue for thread-safe recording
    private final Queue<RecordedMenuAction> pendingActions = new ConcurrentLinkedQueue<>();

    // Configuration
    private boolean recordMouseMoves = true;
    private boolean recordKeyboardInput = true;
    private boolean recordChatInput = false; // Privacy: don't record chat by default

    // File storage
    private static final String RECORDING_DIR = ".microbot/recordings/";
    private final Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .setPrettyPrinting()
            .create();

    // Mouse and keyboard listeners
    private MouseListener mouseListener;
    private MouseMotionListener mouseMotionListener;
    private MouseWheelListener mouseWheelListener;
    private KeyListener keyListener;

    /**
     * Starts a new recording session
     */
    public synchronized void startRecording(String profileName, String sessionName) {
        if (currentSession != null && currentSession.isRecording()) {
            log.warn("Already recording a session. Stop current session first.");
            return;
        }

        currentSession = new InputRecordingSession();
        currentSession.setId(UUID.randomUUID());
        currentSession.setName(sessionName != null ? sessionName : generateSessionName());
        currentSession.setProfileName(profileName != null ? profileName : "default");
        currentSession.setStartedAt(System.currentTimeMillis());
        currentSession.setStartGameTick(currentGameTick);
        currentSession.setRecording(true);
        currentSession.setMicrobotVersion(RuneLiteProperties.getMicrobotVersion());
        currentSession.setRuneliteVersion(RuneLiteProperties.getVersion());

        // Capture player information
        if (client.getLocalPlayer() != null) {
            currentSession.setAccountName(client.getLocalPlayer().getName());
            currentSession.setCombatLevel(client.getLocalPlayer().getCombatLevel());

            WorldPoint wp = client.getLocalPlayer().getWorldLocation();
            if (wp != null) {
                currentSession.setStartWorldX(wp.getX());
                currentSession.setStartWorldY(wp.getY());
                currentSession.setStartPlane(wp.getPlane());
            }
        }

        // Reset tracking state
        lastRecordedMousePosition = null;
        lastMouseMoveTimestamp = 0;
        pressedKeys.clear();
        pendingActions.clear();

        // Hook input listeners
        hookInputListeners();

        log.info("Started recording session: {} [{}]", currentSession.getName(), currentSession.getId());
    }

    /**
     * Stops the current recording session and saves to disk
     */
    public synchronized InputRecordingSession stopRecording() {
        if (currentSession == null || !currentSession.isRecording()) {
            log.warn("No active recording session to stop.");
            return null;
        }

        currentSession.setRecording(false);
        currentSession.setStoppedAt(System.currentTimeMillis());
        currentSession.setEndGameTick(currentGameTick);

        // Process any pending actions
        processPendingActions();

        // Compute statistics
        currentSession.computeStatistics();

        // Unhook input listeners
        unhookInputListeners();

        // Save to disk
        InputRecordingSession savedSession = currentSession;
        try {
            saveSession(savedSession);
            log.info("Stopped and saved recording session: {} ({} actions)",
                    savedSession.getName(), savedSession.getTotalActions());
        } catch (IOException e) {
            log.error("Failed to save recording session: {}", e.getMessage(), e);
        }

        currentSession = null;
        return savedSession;
    }

    /**
     * Pauses the current recording session
     */
    public synchronized void pauseRecording() {
        if (currentSession == null || !currentSession.isRecording() || currentSession.isPaused()) {
            return;
        }

        currentSession.setPaused(true);
        currentSession.setPausedAt(System.currentTimeMillis());
        log.info("Paused recording session");
    }

    /**
     * Resumes a paused recording session
     */
    public synchronized void resumeRecording() {
        if (currentSession == null || !currentSession.isPaused()) {
            return;
        }

        if (currentSession.getPausedAt() == null) {
            log.warn("Cannot resume: pausedAt timestamp is null");
            currentSession.setPaused(false);
            return;
        }

        long pauseDuration = System.currentTimeMillis() - currentSession.getPausedAt();
        currentSession.setTotalPausedMs(currentSession.getTotalPausedMs() + pauseDuration);
        currentSession.setPaused(false);
        currentSession.setPausedAt(null);
        log.info("Resumed recording session");
    }

    /**
     * Discards the current session without saving
     */
    public synchronized void discardSession() {
        if (currentSession != null) {
            currentSession.setRecording(false);
            unhookInputListeners();
            log.info("Discarded recording session: {}", currentSession.getName());
            currentSession = null;
        }
    }

    /**
     * Checks if currently recording
     */
    public boolean isRecording() {
        return currentSession != null && currentSession.isRecording() && !currentSession.isPaused();
    }

    // ===== Event Subscriptions =====

    /**
     * Captures menu clicks - the primary source of gameplay actions
     */
    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        if (!isRecording()) {
            return;
        }

        MenuEntry menuEntry = event.getMenuEntry();
        RecordedMenuAction action = convertMenuEntryToAction(menuEntry);

        if (action != null) {
            recordAction(action);
        }
    }

    /**
     * Tracks game ticks for tick-accurate timing
     */
    @Subscribe
    public void onGameTick(GameTick event) {
        currentGameTick++;

        // Process pending actions accumulated since last tick
        processPendingActions();
    }

    // ===== Input Listeners =====

    /**
     * Hooks mouse and keyboard listeners to the game canvas
     */
    private void hookInputListeners() {
        Canvas canvas = client.getCanvas();
        if (canvas == null) {
            log.warn("Cannot hook input listeners: canvas is null");
            return;
        }

        // Mouse click listener
        mouseListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (isRecording() && MICROBOT_EVENT_SOURCE.equals(e.getSource())) {
                    // Don't record synthetic Microbot events
                    return;
                }
                onMousePressed(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (isRecording() && MICROBOT_EVENT_SOURCE.equals(e.getSource())) {
                    return;
                }
                onMouseReleased(e);
            }
        };

        // Mouse motion listener
        mouseMotionListener = new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (recordMouseMoves && isRecording()) {
                    onMouseMoved(e);
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (recordMouseMoves && isRecording()) {
                    onMouseDragged(e);
                }
            }
        };

        // Mouse wheel listener
        mouseWheelListener = new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (isRecording()) {
                    onMouseWheelMoved(e);
                }
            }
        };

        // Keyboard listener
        keyListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (recordKeyboardInput && isRecording()) {
                    onKeyPressed(e);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (recordKeyboardInput && isRecording()) {
                    onKeyReleased(e);
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                if (recordKeyboardInput && isRecording() && recordChatInput) {
                    onKeyTyped(e);
                }
            }
        };

        canvas.addMouseListener(mouseListener);
        canvas.addMouseMotionListener(mouseMotionListener);
        canvas.addMouseWheelListener(mouseWheelListener);
        canvas.addKeyListener(keyListener);

        log.debug("Input listeners hooked to canvas");
    }

    /**
     * Removes input listeners from the canvas
     */
    private void unhookInputListeners() {
        Canvas canvas = client.getCanvas();
        if (canvas == null || mouseListener == null) {
            return;
        }

        canvas.removeMouseListener(mouseListener);
        canvas.removeMouseMotionListener(mouseMotionListener);
        canvas.removeMouseWheelListener(mouseWheelListener);
        canvas.removeKeyListener(keyListener);

        mouseListener = null;
        mouseMotionListener = null;
        mouseWheelListener = null;
        keyListener = null;

        log.debug("Input listeners unhooked from canvas");
    }

    // ===== Mouse Event Handlers =====

    private void onMousePressed(MouseEvent e) {
        // Mouse press is usually handled via MenuOptionClicked
        // This is a backup for clicks that don't generate menu entries
    }

    private void onMouseReleased(MouseEvent e) {
        // Track mouse releases for drag operations
    }

    private void onMouseMoved(MouseEvent e) {
        long now = System.currentTimeMillis();
        Point currentPos = e.getPoint();

        // Throttle: don't record too frequently
        if (now - lastMouseMoveTimestamp < MOUSE_MOVE_THROTTLE_MS) {
            return;
        }

        // Distance threshold: only record significant movements
        if (lastRecordedMousePosition != null) {
            double distance = currentPos.distance(lastRecordedMousePosition);
            if (distance < MOUSE_MOVE_DISTANCE_THRESHOLD) {
                return;
            }
        }

        RecordedMenuAction action = RecordedMenuAction.builder()
                .type(RecordedActionType.RAW_MOUSE_MOVE)
                .timestamp(now)
                .gameTick(currentGameTick)
                .relativeTimeMs(now - currentSession.getStartedAt())
                .mouseX(currentPos.x)
                .mouseY(currentPos.y)
                .action("Mouse move")
                .build();

        recordAction(action);

        lastRecordedMousePosition = currentPos;
        lastMouseMoveTimestamp = now;
    }

    private void onMouseDragged(MouseEvent e) {
        RecordedMenuAction action = RecordedMenuAction.builder()
                .type(RecordedActionType.MOUSE_DRAG)
                .timestamp(System.currentTimeMillis())
                .gameTick(currentGameTick)
                .relativeTimeMs(System.currentTimeMillis() - currentSession.getStartedAt())
                .mouseX(e.getX())
                .mouseY(e.getY())
                .mouseButton(e.getButton())
                .action("Mouse drag")
                .build();

        recordAction(action);
    }

    private void onMouseWheelMoved(MouseWheelEvent e) {
        RecordedMenuAction action = RecordedMenuAction.builder()
                .type(RecordedActionType.MOUSE_SCROLL)
                .timestamp(System.currentTimeMillis())
                .gameTick(currentGameTick)
                .relativeTimeMs(System.currentTimeMillis() - currentSession.getStartedAt())
                .mouseX(e.getX())
                .mouseY(e.getY())
                .param0(e.getWheelRotation()) // Store scroll amount
                .action("Mouse scroll: " + (e.getWheelRotation() > 0 ? "down" : "up"))
                .build();

        recordAction(action);
    }

    // ===== Keyboard Event Handlers =====

    private void onKeyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();

        // Track modifier keys
        updateModifierState(keyCode, true);

        // Don't double-record
        if (pressedKeys.contains(keyCode)) {
            return;
        }
        pressedKeys.add(keyCode);

        // Determine action type
        RecordedActionType actionType = classifyKeyPress(keyCode);

        RecordedMenuAction action = RecordedMenuAction.builder()
                .type(actionType)
                .timestamp(System.currentTimeMillis())
                .gameTick(currentGameTick)
                .relativeTimeMs(System.currentTimeMillis() - currentSession.getStartedAt())
                .keyCode(keyCode)
                .keyChar(e.getKeyChar())
                .shiftDown(shiftPressed)
                .ctrlDown(ctrlPressed)
                .altDown(altPressed)
                .action(describeKeyPress(keyCode))
                .build();

        recordAction(action);
    }

    private void onKeyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        updateModifierState(keyCode, false);
        pressedKeys.remove(keyCode);
    }

    private void onKeyTyped(KeyEvent e) {
        // Record chat/text input if enabled
        if (!recordChatInput) {
            return;
        }

        RecordedMenuAction action = RecordedMenuAction.builder()
                .type(RecordedActionType.RAW_KEY_INPUT)
                .timestamp(System.currentTimeMillis())
                .gameTick(currentGameTick)
                .relativeTimeMs(System.currentTimeMillis() - currentSession.getStartedAt())
                .keyChar(e.getKeyChar())
                .action("Typed: " + e.getKeyChar())
                .category("chat")
                .build();

        recordAction(action);
    }

    private void updateModifierState(int keyCode, boolean pressed) {
        if (keyCode == KeyEvent.VK_SHIFT) {
            shiftPressed = pressed;
        } else if (keyCode == KeyEvent.VK_CONTROL) {
            ctrlPressed = pressed;
        } else if (keyCode == KeyEvent.VK_ALT) {
            altPressed = pressed;
        }
    }

    private RecordedActionType classifyKeyPress(int keyCode) {
        // F-keys, ESC, number keys = hotkeys
        if (keyCode >= KeyEvent.VK_F1 && keyCode <= KeyEvent.VK_F12) {
            return RecordedActionType.KEY_HOTKEY;
        }
        if (keyCode == KeyEvent.VK_ESCAPE || (keyCode >= KeyEvent.VK_0 && keyCode <= KeyEvent.VK_9)) {
            return RecordedActionType.KEY_HOTKEY;
        }
        return RecordedActionType.RAW_KEY_INPUT;
    }

    private String describeKeyPress(int keyCode) {
        return "Key: " + KeyEvent.getKeyText(keyCode);
    }

    // ===== Menu Entry Conversion =====

    /**
     * Converts a RuneLite MenuEntry into a RecordedMenuAction
     */
    private RecordedMenuAction convertMenuEntryToAction(MenuEntry entry) {
        long now = System.currentTimeMillis();

        RecordedMenuAction.RecordedMenuActionBuilder builder = RecordedMenuAction.builder()
                .timestamp(now)
                .gameTick(currentGameTick)
                .relativeTimeMs(now - currentSession.getStartedAt())
                .action(entry.getOption())
                .target(entry.getTarget())
                .opcode(entry.getType().getId())
                .param0(entry.getParam0())
                .param1(entry.getParam1())
                .identifier(entry.getIdentifier())
                .itemId(entry.getItemId())
                .shiftDown(shiftPressed)
                .ctrlDown(ctrlPressed)
                .altDown(altPressed);

        // Capture mouse position
        Point mousePos = client.getMouseCanvasPosition();
        if (mousePos != null) {
            builder.mouseX(mousePos.getX())
                    .mouseY(mousePos.getY());
        }

        // Capture camera orientation
        builder.cameraYaw(client.getCameraYaw())
                .cameraPitch(client.getCameraPitch());

        // Determine action type and category
        MenuAction menuAction = entry.getType();
        RecordedActionType actionType = classifyMenuAction(menuAction);
        builder.type(actionType);

        String category = categorizeAction(menuAction, entry.getOption());
        builder.category(category);

        // Extract world coordinates if applicable
        if (menuAction == MenuAction.WALK) {
            Scene scene = client.getTopLevelWorldView().getScene();
            if (scene != null) {
                int sceneX = entry.getParam0();
                int sceneY = entry.getParam1();
                int baseX = client.getTopLevelWorldView().getBaseX();
                int baseY = client.getTopLevelWorldView().getBaseY();
                builder.worldX(baseX + sceneX)
                        .worldY(baseY + sceneY)
                        .plane(client.getTopLevelWorldView().getPlane());
            }
        }

        // Extract widget information
        if (entry.getWidget() != null) {
            builder.widgetGroupId(entry.getWidget().getId() >> 16)
                    .widgetChildId(entry.getWidget().getId() & 0xFFFF)
                    .widgetInfo(String.format("Widget[%d:%d]",
                            entry.getWidget().getId() >> 16,
                            entry.getWidget().getId() & 0xFFFF));
        }

        // Build description
        String description = buildActionDescription(entry);
        builder.description(description);

        return builder.build();
    }

    private RecordedActionType classifyMenuAction(MenuAction menuAction) {
        switch (menuAction) {
            case WALK:
            case NPC_FIRST_OPTION:
            case NPC_SECOND_OPTION:
            case NPC_THIRD_OPTION:
            case NPC_FOURTH_OPTION:
            case NPC_FIFTH_OPTION:
            case GAME_OBJECT_FIRST_OPTION:
            case GAME_OBJECT_SECOND_OPTION:
            case GAME_OBJECT_THIRD_OPTION:
            case GAME_OBJECT_FOURTH_OPTION:
            case GAME_OBJECT_FIFTH_OPTION:
            case ITEM_USE_ON_NPC:
            case ITEM_USE_ON_GAME_OBJECT:
            case ITEM_USE_ON_GROUND_ITEM:
            case SPELL_CAST_ON_NPC:
            case SPELL_CAST_ON_GAME_OBJECT:
            case GROUND_ITEM_FIRST_OPTION:
            case GROUND_ITEM_SECOND_OPTION:
            case GROUND_ITEM_THIRD_OPTION:
            case GROUND_ITEM_FOURTH_OPTION:
            case GROUND_ITEM_FIFTH_OPTION:
                return RecordedActionType.MENU_ACTION;

            case CC_OP:
            case CC_OP_LOW_PRIORITY:
            case WIDGET_TARGET:
            case WIDGET_TARGET_ON_WIDGET:
            case WIDGET_TARGET_ON_NPC:
            case WIDGET_TARGET_ON_GAME_OBJECT:
            case WIDGET_TARGET_ON_GROUND_ITEM:
                return RecordedActionType.WIDGET_INTERACT;

            default:
                return RecordedActionType.MENU_ACTION;
        }
    }

    private String categorizeAction(MenuAction menuAction, String option) {
        // Categorize for ML/profiling
        if (option == null) {
            return "unknown";
        }

        String lowerOption = option.toLowerCase();

        if (lowerOption.contains("attack") || lowerOption.contains("cast")) {
            return "combat";
        }
        if (lowerOption.contains("mine") || lowerOption.contains("chop") || lowerOption.contains("fish")) {
            return "skilling";
        }
        if (lowerOption.contains("bank") || lowerOption.contains("deposit")) {
            return "banking";
        }
        if (lowerOption.contains("walk")) {
            return "movement";
        }
        if (menuAction.toString().startsWith("CC_")) {
            return "ui";
        }

        return "other";
    }

    private String buildActionDescription(MenuEntry entry) {
        StringBuilder sb = new StringBuilder();
        sb.append(entry.getOption());

        if (entry.getTarget() != null && !entry.getTarget().isEmpty()) {
            sb.append(" ").append(entry.getTarget());
        }

        sb.append(" [").append(entry.getType()).append("]");

        if (entry.getItemId() > 0) {
            sb.append(" ItemID:").append(entry.getItemId());
        }

        return sb.toString();
    }

    // ===== Action Recording =====

    /**
     * Records an action to the current session
     */
    private void recordAction(RecordedMenuAction action) {
        if (currentSession == null || !currentSession.isRecording() || currentSession.isPaused()) {
            return;
        }

        pendingActions.offer(action);
    }

    /**
     * Processes pending actions and adds them to the session
     */
    private void processPendingActions() {
        if (currentSession == null) {
            return;
        }

        RecordedMenuAction action;
        while ((action = pendingActions.poll()) != null) {
            currentSession.addAction(action);

            if (log.isDebugEnabled()) {
                log.debug("Recorded: {}", action.toCompactString());
            }
        }
    }

    // ===== Session Persistence =====

    /**
     * Saves a session to disk as JSON
     */
    private void saveSession(InputRecordingSession session) throws IOException {
        Path recordingPath = Paths.get(System.getProperty("user.home"), RECORDING_DIR);
        Files.createDirectories(recordingPath);

        String fileName = generateFileName(session);
        Path filePath = recordingPath.resolve(fileName);

        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            gson.toJson(session, writer);
        }

        log.info("Saved recording session to: {}", filePath);
    }

    /**
     * Generates a filename for a session
     */
    private String generateFileName(InputRecordingSession session) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                .withZone(ZoneId.systemDefault());
        String timestamp = formatter.format(Instant.ofEpochMilli(session.getStartedAt()));

        String safeName = session.getName().replaceAll("[^a-zA-Z0-9-]", "_");

        return String.format("%s_%s_%s.json",
                timestamp,
                session.getProfileName(),
                safeName);
    }

    /**
     * Generates a default session name
     */
    private String generateSessionName() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault());
        return "Session " + formatter.format(Instant.now());
    }

    // ===== Configuration Setters =====

    public void setRecordMouseMoves(boolean record) {
        this.recordMouseMoves = record;
    }

    public void setRecordKeyboardInput(boolean record) {
        this.recordKeyboardInput = record;
    }

    public void setRecordChatInput(boolean record) {
        this.recordChatInput = record;
    }

    /**
     * Returns the recording directory path
     */
    public Path getRecordingDirectory() {
        return Paths.get(System.getProperty("user.home"), RECORDING_DIR);
    }

    /**
     * Lists all saved recording sessions
     */
    public List<File> listSavedSessions() {
        Path recordingPath = getRecordingDirectory();
        File dir = recordingPath.toFile();

        if (!dir.exists() || !dir.isDirectory()) {
            return Collections.emptyList();
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) {
            return Collections.emptyList();
        }

        List<File> sessionFiles = Arrays.asList(files);
        sessionFiles.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        return sessionFiles;
    }

    /**
     * Loads a session from a file
     */
    public InputRecordingSession loadSession(File file) throws IOException {
        String json = new String(Files.readAllBytes(file.toPath()));
        return gson.fromJson(json, InputRecordingSession.class);
    }
}
