package net.runelite.client.plugins.microbot.TaF.TearsOfGuthix;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.runelite.api.DecorativeObject;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.TaF.DemonicGorillaKiller.DemonicGorillaScript;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.runelite.client.plugins.microbot.util.antiban.enums.ActivityIntensity.EXTREME;

public class TearsOfGuthixScript extends Script {
    public static final String VERSION = "1.0";
    private static final int TOG_REGION = 12948;
    public static final int JUNA = 3193;
    public static State BOT_STATUS = State.TRAVELLING;
    public enum State {GETTING_WORLD, TRAVELLING, GETTING_TEARS}
    public Map<DecorativeObject, Instant> Streams = new HashMap<>();
    private final int TearsCaveX = 3252;
    private int bestWorld = 0;
    private boolean hasBeenRunning = false;

    {
        Microbot.enableAutoRunOn = false;
        Rs2Antiban.resetAntibanSettings();
        Rs2AntibanSettings.usePlayStyle = true;
        Rs2AntibanSettings.simulateFatigue = false;
        Rs2AntibanSettings.simulateAttentionSpan = false;
        Rs2AntibanSettings.behavioralVariability = true;
        Rs2AntibanSettings.nonLinearIntervals = true;
        Rs2AntibanSettings.dynamicActivity = false;
        Rs2AntibanSettings.profileSwitching = false;
        Rs2AntibanSettings.naturalMouse = true;
        Rs2AntibanSettings.simulateMistakes = false;
        Rs2AntibanSettings.moveMouseOffScreen = false;
        Rs2AntibanSettings.moveMouseRandomly = false;
        Rs2Antiban.setActivityIntensity(EXTREME);
    }

    public boolean run(TearsOfGuthixConfig config) {
        Microbot.log("Starting Tears of Guthix script");
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn() || !super.run()) return;
                switch (BOT_STATUS) {
                    case TRAVELLING:
                        if (Rs2Player.getWorldLocation().getRegionID() == TOG_REGION) {
                            Microbot.log("Already in Tears of Guthix region, collecting tears...");
                            BOT_STATUS = State.GETTING_WORLD;
                            break;
                        }
                        var junaLocation = new WorldPoint(3251,9516,2);
                        if (!Rs2Inventory.hasItem("Games necklace")) {
                            Microbot.log("No games necklace in inventory, going to bank...");
                            Rs2Bank.walkToBank();
                            Rs2Bank.openBank();
                            sleepUntil(Rs2Bank::isOpen);
                            Rs2Bank.withdrawItem("Games necklace");
                            Rs2Inventory.waitForInventoryChanges(1200);
                            if (Rs2Inventory.hasItem("Games necklace")) {
                                Rs2Bank.closeBank();
                            } else {
                                Microbot.log("No games necklace in bank, stopping script.");
                                shutdown();
                            }
                        } else {
                            Microbot.log("Travelling to Tears of Guthix...");
                            Rs2Walker.walkTo(junaLocation);
                            BOT_STATUS = State.GETTING_WORLD;
                        }
                        break;
                    case GETTING_WORLD:
                        if (config.queryForOptimalWorld()) {
                            Microbot.log("Querying for optimal world...");
                            getBestWorld();
                        } else {
                            Microbot.log("Skipping world query, using current world.");
                            BOT_STATUS = State.GETTING_TEARS;
                        }
                        break;
                    case GETTING_TEARS:
                        getTears();
                        break;
                }
            } catch (Exception ex) {
                System.out.println("Exception message: " + ex.getMessage());
                ex.printStackTrace();
            }
        }, 0, 600, TimeUnit.MILLISECONDS);
        return true;
    }

    /**
     * Handles the process of collecting tears in the Tears of Guthix minigame.
     * This includes talking to Juna to enter the cave and collecting blue tears.
     */
    private void getTears() {
        // Check if player is in the correct region
        if (Rs2Player.getWorldLocation().getRegionID() != TOG_REGION) {
            if (hasBeenRunning) {
                Microbot.log("Done collecting tears - shutting down.");
                shutdown();
            }
            Microbot.log("Not in Tears of Guthix region, travelling...");
            BOT_STATUS = State.TRAVELLING;
            return;
        }

        // Check if player is in the tear collection area or still in the entrance
        if (Rs2Player.getWorldLocation().getX() > TearsCaveX) {
            hasBeenRunning = true;
            collectTears();
        } else {
            enterMinigame();
        }
    }

    /**
     * Talks to Juna to enter the tears collection area
     */
    boolean hadDialogue = false;
    private void enterMinigame() {
        if (!hadDialogue) {
            Microbot.log("Entering Minigame...");
            if (Rs2Equipment.hasEquippedSlot(EquipmentInventorySlot.WEAPON)) {
                Microbot.log("Unequipping weapon...");
                Rs2Equipment.unEquip(EquipmentInventorySlot.WEAPON);
            }
            if (Rs2Equipment.hasEquippedSlot(EquipmentInventorySlot.SHIELD)) {
                Microbot.log("Unequipping shield...");
                Rs2Equipment.unEquip(EquipmentInventorySlot.SHIELD);
            }
            Rs2GameObject.interact(JUNA,"Story");
        }
        while (Rs2Dialogue.hasContinue() && this.isRunning()) {
            Rs2Dialogue.clickContinue();
            hadDialogue = true;
        }
    }

    /**
     * Finds and collects the optimal blue tear streams
     */
    private void collectTears() {
        // Wait if player is moving
        if (Rs2Player.isMoving()) {
            return;
        }

        // Get all available blue streams
        List<DecorativeObject> blueStreams = findBlueStreams();

        // If there are no blue streams, wait for them to appear
        if (blueStreams.isEmpty()) {
            Microbot.log("Waiting for blue tear streams to appear");
            sleep(600, 1200);
            return;
        }

        // Check if already collecting a blue stream
        boolean isCollecting = isCollectingTears();

        // Only change streams if not collecting
        if (!isCollecting) {
            DecorativeObject bestStream = findBestStream(blueStreams);

            if (bestStream != null) {
                Instant spawnTime = Streams.get(bestStream);
                if (spawnTime != null) {
                    long timeRemaining = 9 - Duration.between(spawnTime, Instant.now()).getSeconds();

                    // Only switch if not collecting or the new stream has significant time left
                    if (!isCollecting || timeRemaining > 6) {
                        clickStream(bestStream, timeRemaining);
                    }
                }
            }
        }
    }

    /**
     * Returns a list of all blue tear streams
     */
    private List<DecorativeObject> findBlueStreams() {
        return Streams.keySet().stream()
                .filter(stream -> {
                    int id = stream.getId();
                    return id == ObjectID.TOG_WEEPING_WALL_GOOD_R ||
                            id == ObjectID.TOG_WEEPING_WALL_GOOD_L;
                })
                .collect(Collectors.toList());
    }

    /**
     * Checks if the player is currently collecting tears
     */
    private boolean isCollectingTears() {
        return (Rs2Player.isAnimating() && (Rs2Player.getAnimation() == 2043));
    }

    /**
     * Finds the best stream based on time remaining and distance
     */
    private DecorativeObject findBestStream(List<DecorativeObject> blueStreams) {
        final WorldPoint playerLocation = Rs2Player.getWorldLocation();
        // Find the stream with the maximum score based on time remaining and distance
        return blueStreams.stream()
                .max(Comparator.<DecorativeObject>comparingInt(stream -> {
                    // Calculate score based on time remaining and distance
                    Instant spawnTime = Streams.get(stream);
                    if (spawnTime == null) return 0;

                    long ageInSeconds = Duration.between(spawnTime, Instant.now()).getSeconds();
                    int timeRemaining = (int)(9 - ageInSeconds);
                    if (timeRemaining <= 0) return 0;

                    // Weight time remaining heavily (0-90 points)
                    int timeScore = timeRemaining * 10;

                    // Factor in distance (0-30 points)
                    int distance = playerLocation.distanceTo(stream.getWorldLocation());
                    int distScore = Math.max(0, 30 - (distance * 7));

                    return timeScore + distScore;
                }))
                .orElse(null);
    }

    /**
     * Clicks on the selected stream and logs the action
     */
    private void clickStream(DecorativeObject stream, long timeRemaining) {
        Microbot.getClientThread().invoke(() -> {
            Microbot.getMouse().click(stream.getCanvasLocation());
            Microbot.log("Collecting blue tear stream (Time remaining: ~" + timeRemaining + "s)");
            sleep(300, 600);
            return true;
        });
    }

    private void getBestWorld() {
        if (bestWorld != 0) {
            if (Microbot.getClient().getWorld() == bestWorld) {
                Microbot.log("Already in the best world: " + bestWorld);
                BOT_STATUS = State.GETTING_TEARS;
                return;
            }
            boolean isHopped = Microbot.hopToWorld(bestWorld);
            if (!isHopped) return;
            sleepUntil(() -> Microbot.getClient().getGameState() == GameState.HOPPING);
            sleepUntil(() -> Microbot.getClient().getGameState() == GameState.LOGGED_IN);
            BOT_STATUS = State.GETTING_TEARS;
            return;
        }
        Microbot.log("Fetching optimal world for tears collection...");

        // Create HTTP client and request
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.togcrowdsourcing.com/worldinfo"))
                .build();

        try {
            // Send request and get response
            String jsonResponse = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .join();

            // Parse JSON response
            Gson gson = new Gson();
            Type listType = new TypeToken<List<TOGWorld>>() {}.getType();
            List<TOGWorld> worldData = gson.fromJson(jsonResponse, listType);

            Microbot.log("Fetched data for " + worldData.size() + " worlds");

            // First prioritize worlds with pattern "bbbggg" (highest blue count at beginning)
            TOGWorld optimalWorld = worldData.stream()
                    .filter(world -> world.getStream_order().equals("bbbggg") && world.getHits() > 30)
                    .max(Comparator.comparingInt(TOGWorld::getHits))
                    .orElse(null);

            // If no optimal world with "bbbggg", look for worlds with longest blue sequence
            if (optimalWorld == null) {
                optimalWorld = worldData.stream()
                        .filter(world -> world.getLongestBlueSequence() >= 3 && world.getHits() > 20)
                        .max(Comparator.comparingInt(TOGWorld::getHits))
                        .orElse(null);
            }

            // If still no world found, just get highest hits with decent blue count
            if (optimalWorld == null) {
                optimalWorld = worldData.stream()
                        .filter(world -> world.getBlueCount() >= 3)
                        .max(Comparator.comparingInt(TOGWorld::getHits))
                        .orElse(null);
            }

            if (optimalWorld != null) {
                bestWorld = optimalWorld.getWorld_number();
                Microbot.log("Found optimal world: " + bestWorld +
                        " (Hits: " + optimalWorld.getHits() +
                        ", Pattern: " + optimalWorld.getStream_order() + ")");

            } else {
                Microbot.log("No optimal world found, continuing with current world");
                BOT_STATUS = State.GETTING_TEARS;
            }
        } catch (Exception e) {
            Microbot.log("Error fetching TOG world data: " + e.getMessage());
            BOT_STATUS = State.GETTING_TEARS; // Continue with current world
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        BOT_STATUS = State.TRAVELLING;
        hasBeenRunning = false;
        Streams = new HashMap<>();
        bestWorld = 0;
        if (mainScheduledFuture != null && !mainScheduledFuture.isCancelled()) {
            mainScheduledFuture.cancel(true);
        }
        Microbot.log("Shutting down Tears of Guthix script");
    }
}
