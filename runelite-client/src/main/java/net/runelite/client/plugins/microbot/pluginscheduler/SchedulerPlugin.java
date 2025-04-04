package net.runelite.client.plugins.microbot.pluginscheduler;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.accountselector.AutoLoginPlugin;
import net.runelite.client.plugins.microbot.breakhandler.BreakHandlerConfig;
import net.runelite.client.plugins.microbot.breakhandler.BreakHandlerPlugin;
import net.runelite.client.plugins.microbot.breakhandler.BreakHandlerScript;
import net.runelite.client.plugins.microbot.pluginscheduler.api.ConditionProvider;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.Condition;
import net.runelite.client.plugins.microbot.pluginscheduler.type.PluginScheduleEntry;
import net.runelite.client.plugins.microbot.pluginscheduler.ui.SchedulerWindow;
import net.runelite.client.plugins.microbot.util.antiban.AntibanPlugin;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.antiban.enums.CombatSkills;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.security.Login;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.plugins.microbot.pluginscheduler.ui.PluginScheduleEntry.*;
import net.runelite.client.plugins.microbot.pluginscheduler.ui.SchedulerPanel;
import javax.inject.Inject;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.runelite.client.plugins.microbot.util.antiban.enums.Activity;
import net.runelite.client.plugins.microbot.util.antiban.enums.ActivityIntensity;

@Slf4j
@PluginDescriptor(
        name = PluginDescriptor.Mocrosoft + "Plugin Scheduler",
        description = "Schedule plugins at your will",
        tags = {"microbot", "schedule", "automation"},
        enabledByDefault = false
)
public class SchedulerPlugin extends Plugin {
    @Inject
    private SchedulerConfig config;
    final static String configGroup = "pluginscheduler";
    @Provides
    SchedulerConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(SchedulerConfig.class);
    }

    @Inject
    private ClientToolbar clientToolbar;    
    @Inject
    private ScheduledExecutorService executorService;

    private NavigationButton navButton;
    private SchedulerPanel panel;
    private ScheduledFuture<?> updateTask;
    private SchedulerWindow schedulerWindow;
    @Getter
    private PluginScheduleEntry currentPlugin;
    private List<PluginScheduleEntry> scheduledPlugins = new ArrayList<>();



    private int initCheckCount = 0;
    private static final int MAX_INIT_CHECKS = 10;

    @Getter
    private SchedulerState currentState = SchedulerState.UNINITIALIZED;
    private GameState lastGameState = GameState.UNKNOWN;
    
    // Add these fields after the existing instance variables
    
    // Activity and state tracking
    private final Map<Skill, Integer> skillExp = new EnumMap<>(Skill.class);
    private Skill lastSkillChanged;
    private Instant lastActivityTime = Instant.now();
    private Instant loginTime;
    private Activity currentActivity;
    private ActivityIntensity currentIntensity;
    private int idleTime = 0;
    
    // Break tracking
    private boolean isOnBreak = false;
    private Duration currentBreakDuration = Duration.ZERO;
    private Duration timeUntilNextBreak = Duration.ZERO;

    @Override
    protected void startUp() {
        
        panel = new SchedulerPanel(this);

        final BufferedImage icon = ImageUtil.loadImageResource(SchedulerPlugin.class, "icon.png");
        navButton = NavigationButton.builder()
                .tooltip("Plugin Scheduler")
                .priority(10)
                .icon(icon)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);

        // Load saved schedules from config
        

        // Check initialization status before fully enabling scheduler
        checkInitialization();

        // Run the main loop
        updateTask = executorService.scheduleAtFixedRate(() -> {
            SwingUtilities.invokeLater(() -> {
                // Only run scheduling logic if fully initialized
                if (    isSchedulerActive()) {
                    checkSchedule();
                } else if (currentState == SchedulerState.INITIALIZING || currentState == SchedulerState.UNINITIALIZED ) {
                    // Retry initialization check if not already checking
                    checkInitialization();
                }
                updatePanels();
            });
        }, 0, 1, TimeUnit.SECONDS);              
    }
    public boolean isScheduledPluginRunning() {
        return currentPlugin != null && currentPlugin.isRunning();
    }
    private boolean isAutoLoginEnabled() {    
        return Microbot.isPluginEnabled(AutoLoginPlugin.class);
    }
    private boolean isBreakHandlerEnabled() {
        return Microbot.isPluginEnabled(BreakHandlerPlugin.class);
    }
    private boolean isAntibanEnabled() {
        return Microbot.isPluginEnabled(AntibanPlugin.class);
    }
    
    /**
     * Sets the current scheduler state and updates UI
     */
    private void setState(SchedulerState newState) {
        if (currentState != newState) {
            log.debug("Scheduler state changed: {} -> {}", currentState, newState);
            currentState = newState;
            updatePanels();
        }
    }

    /**
     * Checks if all required plugins are loaded and initialized.
     * This runs until initialization is complete or max check count is reached.
     */
    private void checkInitialization() {
        if (!isInitializing()) {
            return;
        }
        
        setState(SchedulerState.INITIALIZING);
        
        // Schedule repeated checks until initialized or max checks reached
        
        SwingUtilities.invokeLater(() -> {
            // Check if client is at login screen            
            List<Plugin> conditionProviders = new ArrayList<>();
            if  (Microbot.getPluginManager() == null|| Microbot.getClient() == null) {
                return;
                
            }else{
                // Find all plugins implementing StoppingConditionProvider
                conditionProviders = Microbot.getPluginManager().getPlugins().stream()
                    .filter(plugin -> plugin instanceof ConditionProvider
                            )
                    .collect(Collectors.toList());
                List<Plugin> enabledList = conditionProviders.stream()
                    .filter(plugin -> Microbot.getPluginManager().isPluginEnabled(plugin))
                    .collect(Collectors.toList());
            }
            
            boolean isAtLoginScreen = Microbot.getClient().getGameState() == GameState.LOGIN_SCREEN;
            // If all conditions met, mark as initialized
            if (isAtLoginScreen) {
                log.info("Scheduler initialization complete - {} stopping condition providers loaded", 
                    conditionProviders.size());
                
                loadScheduledPlugin();
                for (Plugin plugin : conditionProviders) {
                    try {
                        Microbot.getClientThread().runOnSeperateThread(() -> {
                            Microbot.stopPlugin(plugin);
                            return false;
                        });                                                        
                    } catch (Exception e) {                            
                    }
                }
                
                setState(SchedulerState.READY);
                
                // Initial cleanup of one-time plugins after loading
                cleanupCompletedOneTimePlugins();
            } 
            // If max checks reached, mark as initialized but log warning
            else if (++initCheckCount >= MAX_INIT_CHECKS) {
                log.warn("Scheduler initialization timed out");
                loadScheduledPlugin();
            
                setState(SchedulerState.ERROR);
            } 
            // Otherwise, schedule another check
            else {
                log.info("Waiting for initialization: loginScreen={}, providers={}/{}, checks={}/{}",
                    isAtLoginScreen, 
                    conditionProviders.stream().count(),
                    conditionProviders.size(),
                    initCheckCount,
                    MAX_INIT_CHECKS
                    );
                setState(SchedulerState.INITIALIZING);
                checkInitialization();
            }
        });
        
    }


    public void openSchedulerWindow() {
        if (schedulerWindow == null) {
            schedulerWindow = new SchedulerWindow(this);
        }

        if (!schedulerWindow.isVisible()) {
            schedulerWindow.setVisible(true);
        } else {
            schedulerWindow.toFront();
            schedulerWindow.requestFocus();
        }
    }

    @Override
    protected void shutDown() {
        clientToolbar.removeNavigation(navButton);
        forceStopCurrentPlugin();
        
        if (updateTask != null) {
            updateTask.cancel(false);
            updateTask = null;
        }

        if (schedulerWindow != null) {
            schedulerWindow.dispose(); // This will stop the timer
            schedulerWindow = null;
        }
    }

   /**
     * Starts the scheduler
    */
    public void startScheduler() {
        // If already active, nothing to do
        if (isSchedulerActive()) {
            log.info("Scheduler already active");
            return;
        }
                        
        // If initialized, start immediately
        if (SchedulerState.READY == currentState || currentState == SchedulerState.HOLD) {
            
            setState(SchedulerState.SCHEDULING);
            log.info("Plugin Scheduler started");
            
            // Check schedule immediately when started
            SwingUtilities.invokeLater(() -> {
                checkSchedule();
            });
            return;
        }                            
    }
    public boolean isSchedulerActive() {
        return currentState != SchedulerState.UNINITIALIZED &&
               currentState != SchedulerState.INITIALIZING &&
               currentState != SchedulerState.ERROR &&
               currentState != SchedulerState.HOLD &&
               currentState != SchedulerState.READY;
    }

    /**
     * Determines if the scheduler is actively running a plugin or about to run one
     */
    public boolean isActivelyRunning() {
        return isSchedulerActive() && 
               (currentState == SchedulerState.RUNNING_PLUGIN ||
                currentState == SchedulerState.STARTING_PLUGIN ||
                currentState == SchedulerState.WAITING_FOR_LOGIN);
    }

    /**
     * Determines if the scheduler is in a waiting state between plugins
     */
    public boolean isWaiting() {
        return isSchedulerActive() &&
               (currentState == SchedulerState.SCHEDULING ||
                currentState == SchedulerState.WAITING_FOR_SCHEDULE ||
                currentState == SchedulerState.SHORT_BREAK);
    }

    public boolean isInitializing() {
        return currentState == SchedulerState.INITIALIZING || currentState == SchedulerState.UNINITIALIZED;
    }
    /**
     * Stops the scheduler
     */
    public void stopScheduler() {
        if (!isSchedulerActive()) {
            return; // Already stopped
        }
        
        log.info("Stopping scheduler...");                        
        // Force stop any running plugin
        forceStopCurrentPlugin();        
        // Final state after fully stopped
        setState(SchedulerState.HOLD);
    }

    private void checkSchedule() {
        // Update break status
        isOnBreak = isOnBreak();
        
        // First, check if we need to stop the current plugin
        if (isScheduledPluginRunning()) {
            checkCurrentPlugin();
        }
        
        // If no plugin is running, check for scheduled plugins
        if (!isScheduledPluginRunning()) {
            // Get the next scheduled plugin within the next 5 minutes
            PluginScheduleEntry nextPlugin = getNextScheduledPluginWithinTime(Duration.ofMinutes(5));
            
            // If a plugin is ready to run now, start it and interrupt any active break
            if (nextPlugin != null && nextPlugin.isDueToRun()) {
                // If we're on a break, interrupt it
                if (isOnBreak) {
                    interruptBreak();
                }
                scheduleNextPlugin();
            }
            // If no plugin is ready to run now, but one is coming up soon (within 5 minutes)
            else if (nextPlugin != null) {
                // If we're not on a break and there's nothing running, take a short break until next plugin
                if (!isOnBreak && !isScheduledPluginRunning()) {
                    startShortBreakUntilNextPlugin(nextPlugin);
                }
            }
            // If no plugin is scheduled to run soon, enable proper breaks
            else if (!isOnBreak && currentState == SchedulerState.SCHEDULING) {
                unlockBreakHandler();
                log.info("No plugins scheduled in the next 5 minutes, enabling break handler");
                setState(SchedulerState.WAITING_FOR_SCHEDULE);
            }
        }
        
        // Clean up completed one-time plugins
        cleanupCompletedOneTimePlugins();
    }


    /**
     * Interrupts an active break to allow a plugin to start
     */
    private void interruptBreak() {
        if (!isBreakHandlerEnabled()) {
            return;
        }
        
        log.info("Interrupting active break to start scheduled plugin");
        
        // Set break duration to 0 to end the break
        BreakHandlerScript.breakDuration = 0;
        
        // Also reset the breakNow setting if it was set
        Microbot.getConfigManager().setConfiguration(BreakHandlerConfig.configGroup, "breakNow", false);
        
        // Ensure we're not locked for future breaks
        unlockBreakHandler();
        
        // Wait a moment for the break to fully end
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Starts a short break until the next plugin is scheduled to run
     */
    private void startShortBreakUntilNextPlugin(PluginScheduleEntry nextPlugin) {
        if (!isBreakHandlerEnabled()) {
            return;
        }
        
        Optional<ZonedDateTime> nextStartTime = nextPlugin.getNextStartTriggerTime();
        if (!nextStartTime.isPresent()) {
            return;
        }
        
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        Duration timeUntilNext = Duration.between(now, nextStartTime.get());
        
        // Only start a break if we have more than 30 seconds until the next plugin
        if (timeUntilNext.getSeconds() <= 30) {
            return;
        }
        
        log.info("Starting short break until next plugin: {} (scheduled in {})", 
                nextPlugin.getCleanName(), formatDuration(timeUntilNext));
        
        // Subtract 10 seconds to ensure we're back before the plugin needs to start
        long breakSeconds = Math.max(10, timeUntilNext.getSeconds() - 10);
        
        // Configure a break that ends just before the next plugin starts
        BreakHandlerScript.breakDuration = (int)breakSeconds;
        
        // Set state to indicate we're in a controlled short break
        setState(SchedulerState.SHORT_BREAK);
    }

    /**
     * Format a duration for display
     */
    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

  

    /**
     * Schedules the next plugin to run if none is running
     */
    private void scheduleNextPlugin() {
        // First, get all due and enabled plugins
        List<PluginScheduleEntry> duePlugins = scheduledPlugins.stream()
                .filter(plugin -> plugin.isDueToRun() && plugin.isEnabled())
                .collect(Collectors.toList());
        
        if (duePlugins.isEmpty()) {
            return;
        }
        
        // First prioritize by priority value (higher = more important)
        duePlugins.sort(Comparator.comparing(PluginScheduleEntry::getPriority).reversed());
        
        // Then for equal priorities:
        // 1. Non-default plugins take precedence over default plugins
        // 2. Non-randomizable plugins take precedence over randomizable plugins
        List<PluginScheduleEntry> highPriorityPlugins = new ArrayList<>();
        int highestPriority = duePlugins.get(0).getPriority();
        
        // Get all plugins with the highest priority
        for (PluginScheduleEntry plugin : duePlugins) {
            if (plugin.getPriority() == highestPriority) {
                highPriorityPlugins.add(plugin);
            } else {
                break; // List is sorted, so we can break once we hit lower priorities
            }
        }
        
        // From the highest priority plugins, select based on default/random status
        List<PluginScheduleEntry> nonDefaultPlugins = highPriorityPlugins.stream()
                .filter(p -> !p.isDefault())
                .collect(Collectors.toList());
        
        // If we have non-default plugins, prioritize them
        if (!nonDefaultPlugins.isEmpty()) {
            // Further prioritize by non-randomizable status
            List<PluginScheduleEntry> nonRandomNonDefault = nonDefaultPlugins.stream()
                    .filter(p -> !p.isAllowRandomScheduling())
                    .collect(Collectors.toList());
                    
            if (!nonRandomNonDefault.isEmpty()) {
                // Start the first non-random, non-default plugin
                startPlugin(nonRandomNonDefault.get(0));
            } else {
                // Use weighted selection among non-default randomizable plugins
                PluginScheduleEntry selected = selectPluginWeighted(nonDefaultPlugins);
                startPlugin(selected);
            }
        } else {
            // All plugins are default, so use the existing logic for defaults
            List<PluginScheduleEntry> nonRandomDefaults = highPriorityPlugins.stream()
                    .filter(p -> !p.isAllowRandomScheduling())
                    .collect(Collectors.toList());
                    
            if (!nonRandomDefaults.isEmpty()) {
                // Start the first non-random default plugin
                startPlugin(nonRandomDefaults.get(0));
            } else {
                // Use weighted selection among default randomizable plugins
                PluginScheduleEntry selected = selectPluginWeighted(highPriorityPlugins);
                startPlugin(selected);
            }
        }
        
        saveScheduledPlugins();
    }

    /**
     * Selects a plugin using weighted random selection.
     * Plugins with lower run counts have higher probability of being selected.
     */
    private PluginScheduleEntry selectPluginWeighted(List<PluginScheduleEntry> plugins) {
        // Return the only plugin if there's just one
        if (plugins.size() == 1) {
            return plugins.get(0);
        }
        
        // Calculate weights - plugins with lower run counts get higher weights
        // Find the maximum run count
        int maxRuns = plugins.stream()
                .mapToInt(PluginScheduleEntry::getRunCount)
                .max()
                .orElse(0);
        
        // Add 1 to avoid division by zero and to ensure all plugins have some chance
        maxRuns = maxRuns + 1;
        
        // Calculate weights as (maxRuns + 1) - runCount for each plugin
        // This gives higher weight to plugins that have run less often
        double[] weights = new double[plugins.size()];
        double totalWeight = 0;
        
        for (int i = 0; i < plugins.size(); i++) {
            // Weight = (maxRuns + 1) - plugin's run count
            weights[i] = maxRuns - plugins.get(i).getRunCount() + 1;
            totalWeight += weights[i];
        }
        
        // Generate random value between 0 and totalWeight
        double randomValue = Math.random() * totalWeight;
        
        // Select plugin based on random value and weights
        double weightSum = 0;
        for (int i = 0; i < plugins.size(); i++) {
            weightSum += weights[i];
            if (randomValue < weightSum) {
                // Log the selection for debugging
                log.debug("Selected plugin '{}' with weight {}/{} (run count: {})",
                        plugins.get(i).getCleanName(), 
                        weights[i], 
                        totalWeight,
                        plugins.get(i).getRunCount());
                return plugins.get(i);
            }
        }
        
        // Fallback to the last plugin (shouldn't normally happen)
        return plugins.get(plugins.size() - 1);
    }

    public void startPlugin(PluginScheduleEntry scheduledPlugin) {
        if (scheduledPlugin == null) return;
        log.info("Starting scheduled plugin: " + scheduledPlugin.getCleanName());
     
        // Ensure break handler is unlocked before starting a plugin
        // This allows the plugin to take breaks as needed
        unlockBreakHandler();
        
        // If we're on a break, interrupt it
        if (isOnBreak()) {
            interruptBreak();
        }
        
        setState(SchedulerState.STARTING_PLUGIN);
        currentPlugin = scheduledPlugin;

        if (!scheduledPlugin.start()) {
            log.error("Failed to start plugin: " + scheduledPlugin.getCleanName());
            currentPlugin = null;
            setState(SchedulerState.SCHEDULING);
            return;
        }
        
        if (!Microbot.isLoggedIn()) {
            log.info("Login required before running plugin: " + scheduledPlugin.getCleanName());
            Microbot.getClientThread().runOnClientThread(Login::new);
            startLoginMonitoringThread(scheduledPlugin);
            return;
        }

        setState(SchedulerState.RUNNING_PLUGIN);
    }

    public void forceStopCurrentPlugin() {
        if (currentPlugin != null) {
            log.info("Force Stopping current plugin: " + currentPlugin.getCleanName());
            
            setState(SchedulerState.HARD_STOPPING_PLUGIN);
            
            currentPlugin.hardStop();
            
            // Wait a short time to see if the plugin stops immediately
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            if (!currentPlugin.isRunning()) {
                log.info("Plugin stopped successfully: " + currentPlugin.getCleanName());
                currentPlugin = null;
                setState(SchedulerState.SCHEDULING);
            } else {
                log.error("Failed to hard stop plugin: " + currentPlugin.getCleanName());                
            }
        }
        updatePanels();
    }

    @Subscribe
    public void onPluginChanged(PluginChanged event) {
        if (currentPlugin != null && event.getPlugin() == currentPlugin.getPlugin() && !currentPlugin.isRunning()) {
            currentPlugin = null;
            if (    currentState == SchedulerState.RUNNING_PLUGIN 
                || currentState == SchedulerState.SOFT_STOPPING_PLUGIN
                || currentState == SchedulerState.HARD_STOPPING_PLUGIN 
                || currentState == SchedulerState.WAITING_FOR_LOGIN
                || currentState == SchedulerState.STARTING_PLUGIN) {                
                setState(SchedulerState.SCHEDULING);
            }
            updatePanels();
        }
    }

    /**
     * Update all UI panels with the current state
     */
    void updatePanels() {
        if (panel != null) {
            panel.refresh();
        }

        if (schedulerWindow != null && schedulerWindow.isVisible()) {
            schedulerWindow.refresh();
        }
    }

    public void addScheduledPlugin(PluginScheduleEntry plugin) {
        plugin.setLastRunTime(ZonedDateTime.now(ZoneId.systemDefault()));
        scheduledPlugins.add(plugin);
    }

    public void removeScheduledPlugin(PluginScheduleEntry plugin) {
        scheduledPlugins.remove(plugin);
    }

    public void updateScheduledPlugin(PluginScheduleEntry oldPlugin, PluginScheduleEntry newPlugin) {
        int index = scheduledPlugins.indexOf(oldPlugin);
        if (index >= 0) {
            scheduledPlugins.set(index, newPlugin);
        }
    }
     /**
     * Adds conditions to a scheduled plugin
     */
    private void saveConditionsToScheduledPlugin(PluginScheduleEntry plugin, List<Condition> userStopConditions,
                                        List<Condition> userStartConditions, 
                                      boolean requireAll, boolean stopOnConditionsMet) {
        if (plugin == null) return;
        
        // Clear existing conditions
        plugin.getStopConditionManager().getConditions().clear();
        
        // Add new conditions
        for (Condition condition : userStopConditions) {
            plugin.addStopCondition(condition);
        }
        
        // Set condition manager properties
        if (requireAll) {
            plugin.getStopConditionManager().setRequireAll();
        } else {
            plugin.getStopConditionManager().setRequireAny();
        }
        
        
        
        // Save to config
        saveScheduledPlugins();
    }
   /**
     * Gets the list of plugins that have stop conditions set
     */
    public List<PluginScheduleEntry> getScheduledPluginsWithStopConditions() {
        return scheduledPlugins.stream()
                .filter(p -> !p.getStopConditionManager().getConditions().isEmpty())
                .collect(Collectors.toList());
    }
    public List<PluginScheduleEntry> getScheduledPlugins() {
        return new ArrayList<>(scheduledPlugins);
    }

    public void saveScheduledPlugins() {
        // Convert to JSON and save to config
        String json = PluginScheduleEntry.toJson(scheduledPlugins);

        log.info("Saving scheduled plugins to config: {}", json);
        //config.setScheduledPlugins(json);
        if (Microbot.getConfigManager() == null) {
            return;
        }
        Microbot.getConfigManager().setConfiguration(SchedulerConfig.CONFIG_GROUP, "scheduledPlugins", json);
        
    }

    private void loadScheduledPlugin() {
        try {
            // Load from config and parse JSON
            if (Microbot.getConfigManager() == null) {
                return;
            }
            String json = Microbot.getConfigManager().getConfiguration(SchedulerConfig.CONFIG_GROUP, "scheduledPlugins");
            log.info("Loading scheduled plugins from config: {}", json);
            
            if (json != null && !json.isEmpty()) {
                scheduledPlugins = PluginScheduleEntry.fromJson(json);
                
                // Apply stop settings from config to all loaded plugins
                for (PluginScheduleEntry plugin : scheduledPlugins) {
                    // Set timeout values from config
                    plugin.setSoftStopRetryInterval(Duration.ofSeconds(config.softStopRetrySeconds()));
                    plugin.setHardStopTimeout(Duration.ofSeconds(config.hardStopTimeoutSeconds()));
                    
                    // Resolve plugin references
                    resolvePluginReferences(plugin);
                    
                    log.info("Loaded scheduled plugin: {} with {} conditions: \nuserCondition ({}): \n\t{}\npluginConditions({}): \n\t{} ", 
                            plugin.getName(), 
                            plugin.getStopConditionManager().getConditions().size(),
                            plugin.getStopConditionManager().getUserLogicalCondition().getTotalConditionCount(),
                            plugin.getStopConditionManager().getUserLogicalCondition().getDescription(),
                            plugin.getStopConditionManager().getPluginCondition().getTotalConditionCount(),
                            plugin.getStopConditionManager().getPluginCondition().getDescription()
                            );
                    
                    // Log condition details at debug level
                    if (config.debugMode()) {
                        plugin.logConditionInfo(plugin.getStopConditionManager().getConditions(),"LOADING - Stop Conditions", true);
                        plugin.logConditionInfo(plugin.getStartConditionManager().getConditions(),"LOADING - Start Conditions", true);
                    }
                }
                
                // Force UI update after loading plugins
                SwingUtilities.invokeLater(this::updatePanels);
            }
        } catch (Exception e) {
            log.error("Error loading scheduled plugins", e);
            scheduledPlugins = new ArrayList<>();
        }
    }

    /**
     * Resolves plugin references for a ScheduledPlugin instance.
     * This must be done after deserialization since Plugin objects can't be serialized directly.
     */
    private void resolvePluginReferences(PluginScheduleEntry scheduled) {
        if (scheduled.getName() == null) {
            return;
        }
        
        // Find the plugin by name
        Plugin plugin = Microbot.getPluginManager().getPlugins().stream()
                .filter(p -> p.getName().equals(scheduled.getName()))
                .findFirst()
                .orElse(null);
        
        if (plugin != null) {
            scheduled.setPlugin(plugin);
            
            // If plugin implements StoppingConditionProvider, make sure any plugin-defined
            // conditions are properly registered
            if (plugin instanceof ConditionProvider) {
                log.debug("Found StoppingConditionProvider plugin: {}", plugin.getName());
                // This will preserve user-defined conditions while adding plugin-defined ones
                scheduled.registerPluginStoppingConditions();
            }
        } else {
            log.warn("Could not find plugin with name: {}", scheduled.getName());
        }
    }

    public List<String> getAvailablePlugins() {
        return Microbot.getPluginManager().getPlugins().stream()
                .filter(plugin -> {
                    PluginDescriptor descriptor = plugin.getClass().getAnnotation(PluginDescriptor.class);
                    return descriptor != null && descriptor.canBeScheduled();
                })
                .map(Plugin::getName)
                .sorted()
                .collect(Collectors.toList());
    }

    public PluginScheduleEntry getNextScheduledPlugin() {
        if (scheduledPlugins.isEmpty()) {
            return null;
        }

        // First sort plugins by priority (highest first)
        List<PluginScheduleEntry> prioritizedPlugins = scheduledPlugins.stream()
                .filter(plugin -> plugin.isEnabled())
                .sorted(Comparator.comparing(PluginScheduleEntry::getPriority).reversed())
                .collect(Collectors.toList());
        
        if (prioritizedPlugins.isEmpty()) {
            return null;
        }
        
        // Group plugins by priority
        Map<Integer, List<PluginScheduleEntry>> pluginsByPriority = prioritizedPlugins.stream()
                .collect(Collectors.groupingBy(PluginScheduleEntry::getPriority));
        
        // Find the highest priority value
        int highestPriority = pluginsByPriority.keySet().stream()
                .max(Integer::compareTo)
                .orElse(0);
        
        // Get plugins with highest priority
        List<PluginScheduleEntry> highestPriorityPlugins = pluginsByPriority.get(highestPriority);
        
        // From these, first select non-default plugins
        List<PluginScheduleEntry> nonDefaultPlugins = highestPriorityPlugins.stream()
                .filter(p -> !p.isDefault())
                .collect(Collectors.toList());
        
        // If we have non-default plugins with the highest priority, find the earliest one
        if (!nonDefaultPlugins.isEmpty()) {
            return nonDefaultPlugins.stream()
                    .min(Comparator.comparing(p -> 
                        p.getNextStartTriggerTime().orElse(ZonedDateTime.now(ZoneId.systemDefault()))))
                    .orElse(null);
        }
        
        // Otherwise, find the earliest among all highest priority plugins
        return highestPriorityPlugins.stream()
                .min(Comparator.comparing(p -> 
                    p.getNextStartTriggerTime().orElse(ZonedDateTime.now(ZoneId.systemDefault()))))
                .orElse(null);
    }

    /**
     * Gets the next scheduled plugin within the specified time window
     */
    private PluginScheduleEntry getNextScheduledPluginWithinTime(Duration timeWindow) {
        if (scheduledPlugins.isEmpty()) {
            return null;
        }

        // First, get all plugins that will trigger within the time window
        List<PluginScheduleEntry> candidatePlugins = new ArrayList<>();
        ZonedDateTime cutoffTime = ZonedDateTime.now(ZoneId.systemDefault()).plus(timeWindow);

        for (PluginScheduleEntry plugin : scheduledPlugins) {
            if (!plugin.isEnabled()) {
                continue;
            }

            // Get next run time
            Optional<ZonedDateTime> nextStartTime = plugin.getNextStartTriggerTime();
            ZonedDateTime nextRunTime = nextStartTime.orElse(null);
            
            // Skip if no run time or outside our window
            if (nextRunTime == null || nextRunTime.isAfter(cutoffTime)) {
                continue;
            }
            
            candidatePlugins.add(plugin);
        }
        
        if (candidatePlugins.isEmpty()) {
            return null;
        }
        
        // Sort by priority (highest first)
        candidatePlugins.sort(Comparator.comparing(PluginScheduleEntry::getPriority).reversed());
        
        // First, get all plugins with the highest priority
        List<PluginScheduleEntry> highestPriorityPlugins = new ArrayList<>();
        int highestPriority = candidatePlugins.get(0).getPriority();
        
        for (PluginScheduleEntry plugin : candidatePlugins) {
            if (plugin.getPriority() == highestPriority) {
                highestPriorityPlugins.add(plugin);
            } else {
                break; // List is sorted, so we can break once we hit lower priorities
            }
        }
        
        // From the highest priority plugins, prefer non-default ones
        List<PluginScheduleEntry> nonDefaultPlugins = highestPriorityPlugins.stream()
                .filter(p -> !p.isDefault())
                .collect(Collectors.toList());
        
        if (!nonDefaultPlugins.isEmpty()) {
            // Sort by time (earliest first)
            nonDefaultPlugins.sort(Comparator.comparing(p -> 
                p.getNextStartTriggerTime().orElse(ZonedDateTime.now(ZoneId.systemDefault()))));
            return nonDefaultPlugins.get(0);
        }
        
        // If all are default plugins, pick the earliest among them
        highestPriorityPlugins.sort(Comparator.comparing(p -> 
            p.getNextStartTriggerTime().orElse(ZonedDateTime.now(ZoneId.systemDefault()))));
        return highestPriorityPlugins.get(0);
    }
   
   
    /*     * 
     * Checks if the current plugin should be stopped based on conditions
     */
    private void checkCurrentPlugin() {
        if (currentPlugin == null || !currentPlugin.isRunning()) {
            return;
        }
        
        // Call the update hook if the plugin is a condition provider
        Plugin runningPlugin = currentPlugin.getPlugin();
        if (runningPlugin instanceof ConditionProvider) {
            ((ConditionProvider) runningPlugin).onConditionCheck();
        }
        
        // Log condition progress if debug mode is enabled
        if (config.debugMode()) {
            // Log current progress of all conditions
            currentPlugin.logConditionInfo(currentPlugin.getStopConditions(),"DEBUG_CHECK Running Plugin", true);
            
            // If there are progress-tracking conditions, log their progress percentage
            double overallProgress = currentPlugin.getStopConditionProgress();
            if (overallProgress > 0) {
                log.info("Overall condition progress for '{}': {}%", 
                    currentPlugin.getCleanName(), 
                    String.format("%.1f", overallProgress));
            }
        }
        
        // Check if conditions are met
        currentPlugin.checkConditionsAndStop();
        if (currentPlugin.isRunning()) {                       
            if (!currentPlugin.isRunning()){
                log.info("Plugin '{}' stopped because conditions were met", 
                currentPlugin.getCleanName());
                currentPlugin = null;
                setState(SchedulerState.SCHEDULING);
            }
        }
    }

      

    /**
     * Gets condition progress for a scheduled plugin.
     * @param scheduled The scheduled plugin
     * @return Progress percentage (0-100)
     */
    public double getStopConditionProgress(PluginScheduleEntry scheduled) {
        if (scheduled == null || scheduled.getStopConditionManager().getConditions().isEmpty()) {
            return 0;
        }
        
        return scheduled.getStopConditionProgress();
    }
    
    /**
     * Gets the list of plugins that have conditions set
     */
    public List<PluginScheduleEntry> getPluginsWithConditions() {
        return scheduledPlugins.stream()
                .filter(p -> !p.getStopConditionManager().getConditions().isEmpty())
                .collect(Collectors.toList());
    }
    
    /**
     * Adds conditions to a scheduled plugin
     */
    public void saveStopConditionsToPlugin(PluginScheduleEntry plugin, List<Condition> conditions, 
                                      boolean requireAll, boolean stopOnConditionsMet) {
        if (plugin == null) return;
        
        // Clear existing conditions
        plugin.getStopConditionManager().getConditions().clear();
        
        // Add new conditions
        for (Condition condition : conditions) {
            plugin.addStopCondition(condition);
        }
        
        // Set condition manager properties
        if (requireAll) {
            plugin.getStopConditionManager().setRequireAll();
        } else {
            plugin.getStopConditionManager().setRequireAny();
        }
        
        
        
        // Save to config
        saveScheduledPlugins();
    }

    /**
     * Returns the currently running scheduled plugin
     * @return The currently running plugin or null if none is running
     */
    public PluginScheduleEntry getCurrentPlugin() {
        return currentPlugin;
    }

    /**
     * Checks if a completed one-time plugin should be removed
     * @param scheduled The scheduled plugin to check
     * @return True if the plugin should be removed
     */
    private boolean shouldRemoveCompletedOneTimePlugin(PluginScheduleEntry scheduled) {
        // Check if it's been run at least once and is not currently running
        boolean hasRun = scheduled.getRunCount() > 0 && !scheduled.isRunning();
        
        // Check if it can't be triggered again (based on start conditions)
        boolean cantTriggerAgain = !scheduled.canStartTriggerAgain();
        
        return hasRun && cantTriggerAgain;
    }

    /**
     * Cleans up the scheduled plugins list by removing completed one-time plugins
     */
    private void cleanupCompletedOneTimePlugins() {
        List<PluginScheduleEntry> toRemove = scheduledPlugins.stream()
                .filter(this::shouldRemoveCompletedOneTimePlugin)
                .collect(Collectors.toList());
        
        if (!toRemove.isEmpty()) {
            scheduledPlugins.removeAll(toRemove);
            saveScheduledPlugins();
            log.info("Removed {} completed one-time plugins", toRemove.size());
        }
    }
    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        log.info(getName() + " - Game state changed: " + gameStateChanged.getGameState());
        
        // Track login time
        if (gameStateChanged.getGameState() == GameState.LOGGED_IN 
                && (lastGameState == GameState.LOGIN_SCREEN || lastGameState == GameState.HOPPING)) {
            loginTime = Instant.now();
            log.info("Login detected, setting login time: {}", loginTime);
            
            // Reset idle counter on login
            idleTime = 0;
        }
        
        if (gameStateChanged.getGameState() == GameState.LOGGED_IN) {
            // If the game state is LOGGED_IN, start the scheduler  
                      
        } else if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN) {
            // If the game state is LOGIN_SCREEN, stop the current plugin                        
            forceStopCurrentPlugin();
            // Clear login time when logging out
            loginTime = null;
        } else if (gameStateChanged.getGameState() == GameState.HOPPING) {
            // If the game state is HOPPING, stop the current plugin
            forceStopCurrentPlugin();
        } else if (gameStateChanged.getGameState() == GameState.CONNECTION_LOST) {
            // If the game state is CONNECTION_LOST, stop the current plugin
            forceStopCurrentPlugin();
            // Clear login time when connection is lost
            loginTime = null;
        }
       
        this.lastGameState = gameStateChanged.getGameState();    
    }
    @Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals("PluginScheduler"))
		{
			for (PluginScheduleEntry plugin : scheduledPlugins) {
                plugin.setSoftStopRetryInterval(Duration.ofSeconds(config.softStopRetrySeconds()));
                plugin.setHardStopTimeout(Duration.ofSeconds(config.hardStopTimeoutSeconds()));
            }
		}
	}

    @Subscribe
    public void onStatChanged(StatChanged statChanged) {
        // Reset idle time when gaining experience
        idleTime = 0;
        lastActivityTime = Instant.now();
        
        final Skill skill = statChanged.getSkill();
        final int exp = statChanged.getXp();
        final Integer previous = skillExp.put(skill, exp);

        if (lastSkillChanged != null && (lastSkillChanged.equals(skill) || 
                (CombatSkills.isCombatSkill(lastSkillChanged) && CombatSkills.isCombatSkill(skill)))) {                      

            return;
        }

        lastSkillChanged = skill;

        if (previous == null || previous >= exp) {
            return;
        }



        // Update our local tracking of activity
        Activity activity = Activity.fromSkill(skill);
        if (activity != null) {
            currentActivity = activity;
            if (config.debugMode()) {
                log.debug("Activity updated from skill: {} -> {}", skill.getName(), activity);
            }
        }
        
        ActivityIntensity intensity = ActivityIntensity.fromSkill(skill);
        if (intensity != null) {
            currentIntensity = intensity;
        }
    }

    /**
     * Forces the bot to take a break immediately if BreakHandler is enabled
     * @return true if break was initiated, false otherwise
     */
    private boolean forceBreak() {
        if (!isBreakHandlerEnabled()) {
            log.warn("Cannot force break: BreakHandler plugin not enabled");
            return false;
        }
        
        // Set the breakNow config to true to trigger a break
        Microbot.getConfigManager().setConfiguration(BreakHandlerConfig.configGroup, "breakNow", true);
        log.info("Break requested via forceBreak()");
        return true;
    }
    
    private boolean takeMicroBreak(){
        if (!isAntibanEnabled()) {
            log.warn("Cannot take micro break: Antiban plugin not enabled");
            return false;
        }
        if (Rs2Player.isFullHealth() ) {            
            if (Rs2Antiban.takeMicroBreakByChance() || BreakHandlerScript.isBreakActive())
                return true;
        }
        return false;
    }
    private boolean lockBreakHandler() {
        // Check if BreakHandler is enabled and not already locked
        if (isBreakHandlerEnabled() &&  !BreakHandlerScript.isBreakActive() && !BreakHandlerScript.isLockState()) {
            BreakHandlerScript.setLockState(true);
            return true; // Successfully locked
        }
        return false; // Failed to lock
    }
    private void unlockBreakHandler() {
        // Check if BreakHandler is enabled and not already unlocked
        if (isBreakHandlerEnabled() && BreakHandlerScript.isLockState()) {
            BreakHandlerScript.setLockState(false);            
        }

    }


    /**
     * Checks if the bot is currently on a break
     * @return true if on break, false otherwise
     */
    public boolean isOnBreak() {
        // Check if BreakHandler is enabled and on a break
        return isBreakHandlerEnabled() && BreakHandlerScript.isBreakActive();
    }

    /**
     * Gets the current activity being performed
     * @return The current activity, or null if not tracking
     */
    public Activity getCurrentActivity() {
        return currentActivity;
    }

    /**
     * Gets the current activity intensity
     * @return The current activity intensity, or null if not tracking
     */
    public ActivityIntensity getCurrentIntensity() {
        return currentIntensity;
    }

    /**
     * Gets the current idle time in game ticks
     * @return Idle time (ticks)
     */
    public int getIdleTime() {
        return idleTime;
    }

    /**
     * Gets the time elapsed since login
     * @return Duration since login, or Duration.ZERO if not logged in
     */
    public Duration getLoginDuration() {
        if (loginTime == null) {
            return Duration.ZERO;
        }
        return Duration.between(loginTime, Instant.now());
    }

    /**
     * Gets the time since last detected activity
     * @return Duration since last activity
     */
    public Duration getTimeSinceLastActivity() {
        return Duration.between(lastActivityTime, Instant.now());
    }

    /**
     * Gets the time until the next scheduled break
     * @return Duration until next break
     */
    public Duration getTimeUntilNextBreak() {
        if (isBreakHandlerEnabled() && BreakHandlerScript.breakIn > 0) {
            return Duration.ofSeconds(BreakHandlerScript.breakIn);
        }
        return timeUntilNextBreak;
    }

    /**
     * Gets the duration of the current break
     * @return Current break duration
     */
    public Duration getCurrentBreakDuration() {
        if (isBreakHandlerEnabled() && BreakHandlerScript.breakDuration > 0) {
            return Duration.ofSeconds(BreakHandlerScript.breakDuration);
        }
        return currentBreakDuration;
    }

    private Optional<Duration> getScheduleInterval(PluginScheduleEntry plugin) {
        if (plugin.hasAnyStartConditions()) {
            Optional<ZonedDateTime> nextTrigger = plugin.getNextStartTriggerTime();
            if (nextTrigger.isPresent()) {
                ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
                return Optional.of(Duration.between(now, nextTrigger.get()));
            }
        }
        return Optional.empty();
    }

    private void startLoginMonitoringThread(PluginScheduleEntry scheduledPlugin) {
        Thread loginMonitor = new Thread(() -> {
            try {
                log.debug("Login monitoring thread started for plugin '{}'", scheduledPlugin.getName());
                int loginAttempts = 0;
                final int MAX_LOGIN_ATTEMPTS = 3;
                
                // Keep checking until login completes or max attempts reached
                while (loginAttempts < MAX_LOGIN_ATTEMPTS) {
                    // Wait for login attempt to complete
                    Thread.sleep(5000);
                    
                    if (Microbot.isLoggedIn()) {
                        // Successfully logged in, now increment the run count
                        log.info("Login successful, finalizing plugin start: {}", scheduledPlugin.getName());
                        SwingUtilities.invokeLater(() -> {                            
                            setState(SchedulerState.RUNNING_PLUGIN);
                        });
                        return;
                    }
                    
                    loginAttempts++;
                    log.info("Login attempt {} of {} for plugin: {}", 
                        loginAttempts, MAX_LOGIN_ATTEMPTS, scheduledPlugin.getName());
                    
                    // Try login again if needed
                    if (loginAttempts < MAX_LOGIN_ATTEMPTS) {
                        Microbot.getClientThread().runOnClientThread(Login::new);
                    }
                }
                
                // If we get here, login failed too many times
                log.error("Failed to login after {} attempts, aborting plugin: {}", 
                    MAX_LOGIN_ATTEMPTS, scheduledPlugin.getName());
                SwingUtilities.invokeLater(() -> {
                    // Clean up and set proper state
                    currentPlugin = null;
                    setState(SchedulerState.SCHEDULING);
                });
            } catch (InterruptedException e) {
                log.debug("Login monitoring thread for '{}' was interrupted", scheduledPlugin.getName());
            }
        });
        
        loginMonitor.setName("LoginMonitor-" + scheduledPlugin.getName());
        loginMonitor.setDaemon(true);
        loginMonitor.start();
    }
}
