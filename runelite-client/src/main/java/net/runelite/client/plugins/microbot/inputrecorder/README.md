# Microbot Input Recorder System

## Overview

The Input Recorder system is a comprehensive mouse and keyboard tracking framework that records player input and converts it into structured menu actions. These recordings can be:

- **Replayed** later to automate repetitive tasks
- **Analyzed** for behavioral profiling and ML training
- **Compared** across different play styles or accounts
- **Exported** as datasets for research

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    InputRecorderPlugin                          │
│  ┌──────────────┐  ┌──────────────┐  ┌─────────────────────┐  │
│  │ Config       │  │ Overlay      │  │ Lifecycle Hooks     │  │
│  │ (Settings)   │  │ (Status UI)  │  │ (startUp/shutDown)  │  │
│  └──────────────┘  └──────────────┘  └─────────────────────┘  │
└───────────────────────────┬─────────────────────────────────────┘
                            │
          ┌─────────────────┴─────────────────┐
          │                                   │
┌─────────▼──────────┐            ┌──────────▼────────────┐
│ InputRecording     │            │  ActionReplayer       │
│ Service            │            │                       │
│ ┌────────────────┐ │            │ - Replay sessions    │
│ │ Event Hooks:   │ │            │ - Convert to actions │
│ │ - Mouse        │ │            │ - Handle delays      │
│ │ - Keyboard     │ │            │                       │
│ │ - MenuClicked  │ │            └───────────────────────┘
│ │ - GameTick     │ │
│ └────────────────┘ │
│                    │
│ ┌────────────────┐ │
│ │ Session Mgmt:  │ │
│ │ - Start/Stop   │ │
│ │ - Pause/Resume │ │
│ │ - Serialize    │ │
│ └────────────────┘ │
└────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────┐
│         Data Models                         │
│                                             │
│  RecordedMenuAction                         │
│  ├─ timestamp, type, action                 │
│  ├─ opcode, params, identifier              │
│  ├─ mouse coords, world coords              │
│  └─ keyboard info, metadata                 │
│                                             │
│  InputRecordingSession                      │
│  ├─ id, timestamps, profile                 │
│  ├─ List<RecordedMenuAction>                │
│  └─ metadata                                │
└─────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────┐
│     JSON Storage (~/.microbot/recordings/)  │
└─────────────────────────────────────────────┘
```

## Core Components

### 1. Data Models

#### `RecordedActionType`
Enumeration of action types:
- `MENU_ACTION` - In-game menu actions (walk, attack, use item, etc.)
- `WIDGET_INTERACT` - UI widget clicks (inventory, prayers, settings, etc.)
- `CAMERA_MOVE` - Camera rotation/zoom
- `KEY_HOTKEY` - Keyboard hotkeys (F-keys, number keys, etc.)
- `RAW_MOUSE_MOVE` - Compressed mouse movement data
- `RAW_KEY_INPUT` - Raw keyboard input (text, chat)
- `MOUSE_SCROLL` - Mouse wheel events
- `MOUSE_DRAG` - Mouse drag operations

#### `RecordedMenuAction`
Represents a single recorded action with:
- **Timing**: timestamp, gameTick, relativeTimeMs
- **Action data**: type, action, target, description
- **Menu protocol**: opcode, param0, param1, identifier, itemId
- **Coordinates**: worldX/Y/Z, mouseX/Y, screen position
- **Keyboard**: keyCode, keyChar, modifiers (shift/ctrl/alt)
- **Context**: widgetInfo, cameraYaw/Pitch, category, metadata

#### `InputRecordingSession`
Container for a recording session with:
- **Identity**: UUID, name, profileName, scenarioName
- **Timing**: startedAt, stoppedAt, durationMs
- **Actions**: List of RecordedMenuAction objects
- **Player info**: accountName, combatLevel, totalLevel
- **Location**: startWorldX/Y/Z, regions visited
- **Statistics**: totalActions, actionTypeCounts, mouseDistance, avgInterval
- **Metadata**: tags, notes, customMetadata

### 2. InputRecordingService

Core service that:
- Subscribes to RuneLite events (`MenuOptionClicked`, `GameTick`)
- Hooks Java AWT input listeners (mouse, keyboard)
- Converts raw input into `RecordedMenuAction` objects
- Manages session lifecycle (start/stop/pause)
- Compresses mouse movements intelligently
- Serializes sessions to JSON files

**Key methods:**
```java
void startRecording(String profileName, String sessionName)
InputRecordingSession stopRecording()
void pauseRecording()
void resumeRecording()
void discardSession()
boolean isRecording()
```

### 3. ActionReplayer

Replay engine that:
- Converts `RecordedMenuAction` back to game actions
- Respects original timing or applies speed adjustments
- Handles missing targets gracefully (skip, find nearest, pause, abort)
- Supports replay customization via `ReplayOptions`

**Key methods:**
```java
void replay(InputRecordingSession session)
void replay(InputRecordingSession session, ReplayOptions options)
void stopReplay()
void pauseReplay()
void resumeReplay()
double getProgress()
```

### 4. InputRecorderPlugin

Main plugin class that:
- Integrates all components
- Registers event handlers and hotkeys
- Provides public API for external scripts
- Manages plugin lifecycle

**Hotkeys:**
- `Ctrl+Shift+R` - Start/stop recording
- `Ctrl+Shift+P` - Pause/resume recording
- `Ctrl+Shift+D` - Discard current session

### 5. InputRecorderOverlay

UI overlay showing:
- Recording status (idle/recording/paused)
- Session name, profile, duration
- Number of actions recorded
- Actions per minute rate
- Replay progress

### 6. Configuration

Extensive configuration options for:
- **Recording**: Enable/disable, mouse moves, keyboard input, chat input
- **Storage**: Directory, auto-save, max sessions, privacy settings
- **Replay**: Speed multiplier, timing, randomization, error handling

## Usage Examples

### Basic Recording

```java
// Via plugin API
InputRecorderPlugin plugin = /* get plugin instance */;
plugin.startRecording("bossing", "Vorkath Practice Run");

// ... play the game ...

InputRecordingSession session = plugin.stopRecording();
System.out.println("Recorded " + session.getTotalActions() + " actions");
```

### Recording with Service

```java
@Inject
private InputRecordingService recordingService;

// Start recording
recordingService.startRecording("skilling", "Woodcutting Session");

// Pause temporarily
recordingService.pauseRecording();

// Resume
recordingService.resumeRecording();

// Stop and save
InputRecordingSession session = recordingService.stopRecording();
```

### Replay

```java
// Load a saved session
File sessionFile = new File("~/.microbot/recordings/20250118-143022_bossing_VorkathPractice.json");
InputRecordingSession session = recordingService.loadSession(sessionFile);

// Replay with custom options
ReplayOptions options = ReplayOptions.builder()
    .speedMultiplier(1.5)  // 1.5x speed
    .respectTiming(true)
    .randomizeDelays(true)
    .skipMouseMoves(false)
    .targetNotFoundBehavior(TargetNotFoundBehavior.SKIP_AND_CONTINUE)
    .verboseLogging(true)
    .build();

actionReplayer.replay(session, options);
```

### ML Analysis

```java
// Load multiple sessions for analysis
List<InputRecordingSession> sessions = new ArrayList<>();
for (File file : recordingService.listSavedSessions()) {
    sessions.add(recordingService.loadSession(file));
}

// Extract features for ML
for (InputRecordingSession session : sessions) {
    // Calculate features
    double avgMouseDistance = session.getTotalMouseDistance() / (double) session.getTotalActions();
    double actionsPerSecond = session.getTotalActions() / (session.getDurationMs() / 1000.0);

    // Count action types
    int combatActions = session.getActionTypeCounts().getOrDefault(RecordedActionType.MENU_ACTION, 0);
    int uiActions = session.getActionTypeCounts().getOrDefault(RecordedActionType.WIDGET_INTERACT, 0);

    // Analyze timing patterns
    List<RecordedMenuAction> actions = session.getActions();
    List<Long> intervals = new ArrayList<>();
    for (int i = 1; i < actions.size(); i++) {
        long interval = actions.get(i).getTimestamp() - actions.get(i-1).getTimestamp();
        intervals.add(interval);
    }

    // Calculate statistics
    double avgInterval = intervals.stream().mapToLong(Long::longValue).average().orElse(0.0);
    double stdDevInterval = calculateStdDev(intervals, avgInterval);

    System.out.printf("Session: %s | APM: %.1f | AvgInterval: %.0fms | StdDev: %.0fms%n",
        session.getName(), actionsPerSecond * 60, avgInterval, stdDevInterval);
}
```

## JSON Format

### Example Recording Session

```json
{
  "id": "a3f8d9c7-1234-5678-90ab-cdef12345678",
  "name": "Zulrah Kill #1",
  "profileName": "bossing",
  "scenarioName": null,
  "startedAt": 1705587022451,
  "stoppedAt": 1705587145780,
  "durationMs": 123329,
  "startGameTick": 1450,
  "endGameTick": 1656,
  "actions": [
    {
      "timestamp": 1705587022500,
      "gameTick": 1450,
      "relativeTimeMs": 49,
      "type": "WIDGET_INTERACT",
      "action": "Activate",
      "target": "Protect from Magic",
      "opcode": 57,
      "param0": 5,
      "param1": 35454980,
      "identifier": 0,
      "itemId": null,
      "worldX": null,
      "worldY": null,
      "plane": null,
      "mouseX": 587,
      "mouseY": 403,
      "mouseButton": 1,
      "keyCode": null,
      "keyChar": null,
      "shiftDown": false,
      "ctrlDown": false,
      "altDown": false,
      "widgetInfo": "Widget[541:5]",
      "widgetGroupId": 541,
      "widgetChildId": 5,
      "cameraYaw": 1024,
      "cameraPitch": 512,
      "description": "Activate Protect from Magic [CC_OP]",
      "category": "ui",
      "successful": null,
      "metadata": null
    },
    {
      "timestamp": 1705587023120,
      "gameTick": 1451,
      "relativeTimeMs": 669,
      "type": "MENU_ACTION",
      "action": "Cast",
      "target": "<col=00ff00>Vengeance</col>",
      "opcode": 25,
      "param0": 0,
      "param1": 14286880,
      "identifier": 1,
      "itemId": null,
      "worldX": null,
      "worldY": null,
      "plane": null,
      "mouseX": 721,
      "mouseY": 440,
      "mouseButton": 1,
      "keyCode": null,
      "keyChar": null,
      "shiftDown": false,
      "ctrlDown": false,
      "altDown": false,
      "widgetInfo": "Widget[218:123]",
      "widgetGroupId": 218,
      "widgetChildId": 123,
      "cameraYaw": 1024,
      "cameraPitch": 512,
      "description": "Cast <col=00ff00>Vengeance</col> [CC_OP]",
      "category": "combat",
      "successful": null,
      "metadata": null
    },
    {
      "timestamp": 1705587024050,
      "gameTick": 1452,
      "relativeTimeMs": 1599,
      "type": "MENU_ACTION",
      "action": "Attack",
      "target": "<col=ff0000>Zulrah<col=ff00> (level-725)</col>",
      "opcode": 9,
      "param0": 0,
      "param1": 0,
      "identifier": 2042,
      "itemId": null,
      "worldX": 2268,
      "worldY": 3072,
      "plane": 0,
      "mouseX": 512,
      "mouseY": 340,
      "mouseButton": 1,
      "keyCode": null,
      "keyChar": null,
      "shiftDown": false,
      "ctrlDown": false,
      "altDown": false,
      "widgetInfo": null,
      "widgetGroupId": null,
      "widgetChildId": null,
      "cameraYaw": 1024,
      "cameraPitch": 512,
      "description": "Attack <col=ff0000>Zulrah<col=ff00> (level-725)</col> [NPC_SECOND_OPTION]",
      "category": "combat",
      "successful": null,
      "metadata": null
    },
    {
      "timestamp": 1705587024180,
      "gameTick": 1452,
      "relativeTimeMs": 1729,
      "type": "KEY_HOTKEY",
      "action": "Key: F1",
      "target": null,
      "opcode": null,
      "param0": null,
      "param1": null,
      "identifier": null,
      "itemId": null,
      "worldX": null,
      "worldY": null,
      "plane": null,
      "mouseX": null,
      "mouseY": null,
      "mouseButton": null,
      "keyCode": 112,
      "keyChar": "�",
      "shiftDown": false,
      "ctrlDown": false,
      "altDown": false,
      "widgetInfo": null,
      "widgetGroupId": null,
      "widgetChildId": null,
      "cameraYaw": null,
      "cameraPitch": null,
      "description": null,
      "category": null,
      "successful": null,
      "metadata": null
    },
    {
      "timestamp": 1705587024890,
      "gameTick": 1453,
      "relativeTimeMs": 2439,
      "type": "RAW_MOUSE_MOVE",
      "action": "Mouse move",
      "target": null,
      "opcode": null,
      "param0": null,
      "param1": null,
      "identifier": null,
      "itemId": null,
      "worldX": null,
      "worldY": null,
      "plane": null,
      "mouseX": 634,
      "mouseY": 422,
      "mouseButton": null,
      "keyCode": null,
      "keyChar": null,
      "shiftDown": false,
      "ctrlDown": false,
      "altDown": false,
      "widgetInfo": null,
      "widgetGroupId": null,
      "widgetChildId": null,
      "cameraYaw": null,
      "cameraPitch": null,
      "description": null,
      "category": null,
      "successful": null,
      "metadata": null
    }
  ],
  "accountName": null,
  "combatLevel": 126,
  "totalLevel": 2277,
  "startWorldX": 2268,
  "startWorldY": 3070,
  "startPlane": 0,
  "regions": [8279, 9023],
  "totalActions": 5,
  "actionTypeCounts": {
    "MENU_ACTION": 2,
    "WIDGET_INTERACT": 1,
    "KEY_HOTKEY": 1,
    "RAW_MOUSE_MOVE": 1
  },
  "totalMouseDistance": 1245,
  "averageActionInterval": 597.25,
  "totalPausedMs": 0,
  "formatVersion": "1.0",
  "microbotVersion": "1.12.4",
  "runeliteVersion": "2.6.0",
  "tags": ["zulrah", "pvm", "practice"],
  "notes": "First successful kill with new gear setup",
  "customMetadata": {}
}
```

## Data Compression & Performance

### Mouse Movement Compression

The system uses intelligent throttling to reduce mouse movement data:

1. **Time-based throttling**: Only record mouse position every N milliseconds (default: 50ms)
2. **Distance threshold**: Only record if mouse moved >N pixels (default: 10px)
3. **Result**: ~95% reduction in mouse movement data while preserving movement patterns

**Storage estimates:**
- **Without compression**: ~60 events/second = 216,000 events/hour = ~50MB JSON/hour
- **With compression**: ~5-10 events/second = 18,000-36,000 events/hour = ~4-8MB JSON/hour
- **Typical 1-hour session**: 2,000-5,000 actions, 1-3MB JSON file

### Tick vs Real-Time

Actions store **both** tick and real-time timestamps:
- **Real-time** (ms): Precise timing for replay and ML analysis
- **Game tick**: Essential for tick-perfect actions (prayer flicking, combo eating, etc.)

This dual approach enables:
- Accurate replay regardless of game lag
- Analysis of reaction times and input patterns
- Tick-perfect replay for PvM mechanics

## ML & Profiling Features

### Extractable Features

From a recording session, you can derive:

1. **Timing features:**
   - Average time between actions
   - Variance in action timing
   - Reaction time to game events
   - Actions per minute (APM)

2. **Mouse features:**
   - Total distance traveled
   - Average distance per action
   - Mouse movement patterns (straight lines vs curved)
   - Click accuracy (distance from target center)

3. **Gameplay patterns:**
   - Combat vs skilling vs banking ratio
   - Hotkey usage frequency (F-keys, prayer switches)
   - Menu navigation patterns
   - Camera adjustment frequency

4. **Behavioral signatures:**
   - Idle time patterns
   - Error/misclick rate
   - Consistency metrics
   - Multitasking indicators

### Use Cases

1. **Bot detection training**: Compare human vs bot input patterns
2. **Skill assessment**: Measure PvM proficiency, reaction times
3. **Efficiency analysis**: Identify optimization opportunities
4. **Playstyle classification**: Cluster players by behavior
5. **Account sharing detection**: Compare input patterns over time

## Important Notes & Pitfalls

### RuneLite Input Hooks

**Problem**: RuneLite's menu system can be complex
- Menu entries are sometimes modified by plugins
- Some actions don't generate `MenuOptionClicked` events
- Widget IDs can change between game updates

**Solution**:
- We hook **both** menu events and raw input listeners
- Fallback to raw coordinates if menu entry unavailable
- Store multiple identifiers (opcode, widget ID, world coords)

### Synthetic Events

**Problem**: Microbot's own mouse/keyboard actions could be recorded
- Creates infinite loops if replaying recorded synthetic events
- Inflates action counts artificially

**Solution**:
- Check `event.getSource()` to filter out "Microbot" source events
- Only record player-initiated input

### Privacy Considerations

**Important**: This system can record sensitive information:
- Chat messages (if `recordChatInput` enabled)
- Account names (if `includeAccountName` enabled)
- Gameplay patterns that might identify a player

**Recommendations**:
- Disable chat recording by default
- Anonymize account names in shared datasets
- Clearly inform users what is being recorded
- Provide easy deletion of recordings

### Replay Limitations

**Known issues**:
1. **Target unavailability**: NPCs/objects may have moved or despawned
2. **State differences**: Game state may differ from recording time
3. **Widget changes**: UI layouts can change between recording and replay
4. **Timing precision**: Network lag not captured in recordings

**Mitigation**:
- Implement fallback behaviors (`ReplayOptions.targetNotFoundBehavior`)
- Validate critical state before actions
- Add manual intervention points for complex sequences
- Consider replays as "best effort" rather than guaranteed execution

## Future Enhancements

Potential improvements:
1. **Visual replay**: Render a replay as a video with overlays
2. **Action diffing**: Compare two sessions and highlight differences
3. **Pattern recognition**: Automatically detect common action sequences
4. **Cloud storage**: Sync recordings across devices
5. **Compression**: Use binary format instead of JSON for large sessions
6. **Encryption**: Protect sensitive recordings with passwords
7. **Annotation**: Add timestamps and notes during recording
8. **Live streaming**: Stream recordings to viewers in real-time

## API Reference

See JavaDoc comments in source files for detailed API documentation:
- `InputRecorderPlugin` - Main plugin entry point
- `InputRecordingService` - Recording management
- `ActionReplayer` - Replay functionality
- `RecordedMenuAction` - Action data structure
- `InputRecordingSession` - Session container

## License

This code is part of the Microbot project and follows its license terms.
