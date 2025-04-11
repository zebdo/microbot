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

import net.runelite.client.plugins.microbot.util.antiban.enums.CombatSkills;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.security.Login;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;
import net.runelite.client.plugins.microbot.pluginscheduler.ui.SchedulerPanel;
import javax.inject.Inject;
import javax.swing.*;

import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;
import static net.runelite.client.plugins.microbot.util.Global.sleep;
import java.awt.image.BufferedImage;
import java.lang.StackWalker.Option;
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
        name = PluginDescriptor.Mocrosoft + PluginDescriptor.VOX +  "Plugin Scheduler",
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
    
    private Duration currentBreakDuration = Duration.ZERO;
    private Duration timeUntilNextBreak = Duration.ZERO;
    private Optional<ZonedDateTime> breakStartTime = Optional.empty();
    // login tracking
    private Thread loginMonitor;
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
            boolean isLoggedIn = Microbot.getClient().getGameState() == GameState.LOGGED_IN;
            boolean isAtLoginAuth = Microbot.getClient().getGameState() == GameState.LOGIN_SCREEN_AUTHENTICATOR;
            // If all conditions met, mark as initialized
            if (isAtLoginScreen || isLoggedIn || isAtLoginAuth) {
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
                // Ensure BreakHandler is enabled when we start scheduler
                //if (!isBreakHandlerEnabled() && config.enableBreakHandlerAutomatically()) {
                  //  if (enableBreakHandler()) {
                    //    log.info("Automatically enabled BreakHandler plugin");
                    //}
                //}
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
        forceStopCurrentPluginScheduleEntry();
        if (this.loginMonitor != null && this.loginMonitor.isAlive()) {
            this.loginMonitor.interrupt();
            this.loginMonitor = null;            
        }
        if (updateTask != null) {
            updateTask.cancel(false);
            updateTask = null;
        }

        if (schedulerWindow != null) {
            schedulerWindow.dispose(); // This will stop the timer
            schedulerWindow = null;
        }
        this.currentState = SchedulerState.UNINITIALIZED;
        this.lastGameState = GameState.UNKNOWN;
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
     * Determines if the scheduler is in a waiting state between scheduling a plugin
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
    public boolean isStopping() {
        return currentState == SchedulerState.SOFT_STOPPING_PLUGIN ||
               currentState == SchedulerState.HARD_STOPPING_PLUGIN;
    }
    /**
     * Starts the scheduler
    */
    public void startScheduler() {
        log.info("Starting scheduler reqeust...");
        Microbot.getClientThread().runOnClientThreadOptional(()-> {
            // If already active, nothing to do
            if (isSchedulerActive()) {
                log.info("Scheduler already active");
                return true;
            }
            log.info("Starting scheduler...");
            // Ensure BreakHandler is enabled when we start scheduler
            if (!isBreakHandlerEnabled() && config.enableBreakHandlerAutomatically()) {
                log.info(" Start enabling BreakHandler plugin");
                if (enableBreakHandler()) {
                    log.info("Automatically enabled BreakHandler plugin");
                }
            }      
            // If initialized, start immediately
            if (SchedulerState.READY == currentState || currentState == SchedulerState.HOLD) {
                
                setState(SchedulerState.SCHEDULING);
                log.info("Plugin Scheduler started");
                
                // Check schedule immediately when started
                SwingUtilities.invokeLater(() -> {
                    checkSchedule();
                });
                return true;
            }         
            return true;                   
        });
        return;
    }
    /**
     * Stops the scheduler
     */
    public void stopScheduler() {

        Microbot.getClientThread().runOnClientThreadOptional(()-> {
         
                if (loginMonitor != null && loginMonitor.isAlive()) {
                    loginMonitor.interrupt();

                }
                if (!isSchedulerActive()) {
                    return false; // Already stopped
                }
                setState(SchedulerState.HOLD);
                log.info("Stopping scheduler...");                                    
                if (currentPlugin != null) {
                    forceStopCurrentPluginScheduleEntry();
                }
                // Final state after fully stopped                                
                if (isBreakHandlerEnabled() && config.enableBreakHandlerAutomatically()) {        
                    if (disableBreakHandler()) {
                        log.info("Automatically disabled BreakHandler plugin");
                    }
                }      
                setState(SchedulerState.HOLD);
                logout();
            
                //return true;
            //});
            log.info("Scheduler stopped - status: {}", currentState);
            return false;
            }
        );
    }
 
    private void checkSchedule() {
        // Update break status
        boolean isOnBreak = isOnBreak();
        if (SchedulerState.LOGIN == currentState || 
            SchedulerState.WAITING_FOR_LOGIN == currentState ||
            SchedulerState.HARD_STOPPING_PLUGIN == currentState ||
            SchedulerState.SOFT_STOPPING_PLUGIN == currentState ||
            currentState == SchedulerState.HOLD) {
            return;
        }
        
        // First, check if we need to stop the current plugin
        if (isScheduledPluginRunning()) {
            checkCurrentPlugin();
            
        }
        
        // If no plugin is running, check for scheduled plugins
        if (!isScheduledPluginRunning()) {                        
            PluginScheduleEntry nextPluginWith = getNextScheduledPluginWithinTime(Duration.ofMinutes(5));            
                            
            // Get the next scheduled plugin within the next 5 minutes
            
            if (nextPluginWith != null) {
                boolean nextWithinFlag = false;
                int withinSeconds = 60;
                if (nextPluginWith.getCurrentStartTriggerTime().isPresent()){
                    log.info("Next scheduled plugin within 5 min {} (scheduled in {})", 
                    nextPluginWith.getCleanName(), 
                        formatDuration(Duration.between(ZonedDateTime.now(ZoneId.systemDefault()), nextPluginWith.getCurrentStartTriggerTime().get())));                            
                        nextWithinFlag = Duration.between(ZonedDateTime.now(ZoneId.systemDefault()), nextPluginWith.getCurrentStartTriggerTime().get()).compareTo(Duration.ofSeconds(withinSeconds)) < 0;
                }else{
                    log.info("Next scheduled plugin within 5 min {} (now)", 
                    nextPluginWith.getCleanName());   
                    nextWithinFlag = true;                         
                }
                // If we're on a break, interrupt it
                
                if (isOnBreak && (  nextWithinFlag ) ){
                    log.info("Interrupting active break to start scheduled plugin: {}", nextPluginWith.getCleanName());
                    interruptBreak();
                }
                if (currentState == SchedulerState.SHORT_BREAK) {
                    setState(SchedulerState.WAITING_FOR_SCHEDULE);                    
                }
                
                if (!isActivelyRunning()){
                    scheduleNextPlugin();
                }else{
                    log.info("Plugin is already running, waiting for it to finish");

                }
            }else{
                  // If we're not on a break and there's nothing running, take a short break until next plugin
                if (!isOnBreak && currentState != SchedulerState.WAITING_FOR_SCHEDULE && currentState == SchedulerState.SCHEDULING)  {                   
                    startShortBreakUntilNextPlugin();
                }
            }
                    
        }
        
        // Clean up completed one-time plugins
        cleanupCompletedOneTimePlugins();
    }


    /**
     * Interrupts an active break to allow a plugin to start
     */
    private void interruptBreak() {

        currentBreakDuration = Duration.ZERO;
        breakStartTime = Optional.empty();

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
        if (BreakHandlerScript.isBreakActive() ){            
            SwingUtilities.invokeLater(() -> {
                log.info("Break was not interrupted successfully");
                interruptBreak();   
            });
            return;
        }
        log.info("Break interrupted successfully");        
        
    }

    /**
     * Starts a short break until the next plugin is scheduled to run
     */
    private void startShortBreakUntilNextPlugin() {
        if (!isBreakHandlerEnabled()) {
            return;
        }
        PluginScheduleEntry nextPlugin = getNextScheduledPlugin();
        Duration timeUntilNext = Duration.ZERO;
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        if (nextPlugin == null) {
            timeUntilNext = Duration.between(now, now.plusMinutes(5));            
        }else{
            Optional<ZonedDateTime> nextStartTime = nextPlugin.getCurrentStartTriggerTime();                        
            if (!nextStartTime.isPresent()) {
                // just a random time until next plugin
                timeUntilNext = Duration.ofSeconds(Rs2Random.between(60, 600));
            }else{
                timeUntilNext = Duration.between(now, nextStartTime.get());
            }
        }
                                
        // Only start a break if we have more than 60 seconds until the next plugin
        if (timeUntilNext.getSeconds() <= 60) {
            return;
        }
        if (nextPlugin != null){
            log.info("Starting short break until next plugin: {} (scheduled in {})", 
                nextPlugin.getCleanName(), formatDuration(timeUntilNext));
        }
        
        // Subtract 10 seconds to ensure we're back before the plugin needs to start
        long breakSeconds = Math.max(10, timeUntilNext.getSeconds() - 10);
        
        // Configure a break that ends just before the next plugin starts
        BreakHandlerScript.breakDuration = (int)breakSeconds;
        currentBreakDuration = Duration.ofSeconds(breakSeconds);
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
        Optional<PluginScheduleEntry> selected = getNextScheduledPlugin(true,null);
        if (selected.isEmpty()) {
            log.info("No plugins scheduled to run");
            return;
        }
        // If we're on a break, interrupt it, only we have initialized the break
        if (isOnBreak() && currentBreakDuration != null && currentBreakDuration.getSeconds() > 0) {
            log.info("Interrupting active break to start scheduled plugin: {}", selected.get().getCleanName());
            interruptBreak();
        }
        log.info("Starting scheduled plugin: {}", selected.get().getCleanName());        
        startPluginScheduleEntry(selected.get());
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

    public void startPluginScheduleEntry(PluginScheduleEntry scheduledPlugin) {
        Microbot.getClientThread().runOnClientThreadOptional( ()-> {
            if (scheduledPlugin == null) return false;
            log.info("Starting scheduled plugin: " + scheduledPlugin.getCleanName());
        
            // Ensure break handler is unlocked before starting a plugin
            unlockBreakHandler();
            
            // If we're on a break, interrupt it
            if (isOnBreak()) {
                interruptBreak();
            }
            SchedulerState stateBeforeScheduling = currentState;
            setState(SchedulerState.STARTING_PLUGIN);
            currentPlugin = scheduledPlugin;

            // Check for stop conditions if enforcement is enabled -> ensure we have stop condition so the plugin doesn't run forever (only manual stop possible otherwise)
            if (config.enforceStopConditions() && scheduledPlugin.getStopConditionManager().getConditions().isEmpty()) {
                // Show confirmation dialog on EDT to prevent blocking
                SwingUtilities.invokeLater(() -> {
                    int result = JOptionPane.showConfirmDialog(
                        null,
                        "Plugin '" + scheduledPlugin.getCleanName() + "' has no stop conditions set.\n" +
                        "It will run until manually stopped.\n\n" +
                        "Would you like to configure stop conditions now?",
                        "No Stop Conditions",
                        JOptionPane.YES_NO_CANCEL_OPTION
                    );
                    
                    if (result == JOptionPane.YES_OPTION) {
                        // User wants to add stop conditions
                        openSchedulerWindow();
                        if (schedulerWindow != null) {
                            // Switch to stop conditions tab
                            schedulerWindow.selectPlugin(scheduledPlugin);
                            schedulerWindow.switchToStopConditionsTab();
                            schedulerWindow.toFront();
                            // Keep in STARTING_PLUGIN state - will be resumed when conditions are added
                            // The currentPlugin is already set, so it will be in "pending start" status
                        }
                    } 
                    else if (result == JOptionPane.NO_OPTION) {
                        // User confirms to run without stop conditions
                        continueStartingPluginScheduleEntry(scheduledPlugin);
                    } 
                    else {
                        // User canceled - abort starting
                        //currentPlugin.updateStartCondtions(); TODO consider reset. because we dont want to start the plugin agin immediately ? TESTING ?
                        currentPlugin = null;
                        setState(stateBeforeScheduling);
                        
                    }
                });
                return false;
            } else {
                // Stop conditions exist or enforcement disabled - proceed normally
                continueStartingPluginScheduleEntry(scheduledPlugin);
                return true;
            }
        });
    }

    /**
     * Resets any pending plugin start operation
     */
    public void resetPendingStart() {
        if (currentState == SchedulerState.STARTING_PLUGIN || currentState == SchedulerState.WAITING_FOR_LOGIN) {

            currentPlugin = null;
            setState(SchedulerState.SCHEDULING);
        }
    }

    /**
     * Continues the plugin starting process after stop condition checks
     */
    public void continueStartingPluginScheduleEntry(PluginScheduleEntry scheduledPlugin) {
        Microbot.getClientThread().runOnClientThreadOptional( ()-> {      
                if (!Microbot.isLoggedIn()) {
                    log.info("Login required before running plugin: " + scheduledPlugin.getCleanName());            
                    startLoginMonitoringThread(scheduledPlugin);
                    return false;
                }
                if (!scheduledPlugin.start()) {
                    log.error("Failed to start plugin: " + scheduledPlugin.getCleanName());
                    currentPlugin = null;
                    setState(SchedulerState.SCHEDULING);
                    return false;
                }
                setState(SchedulerState.RUNNING_PLUGIN);
                return true;
            }
        );
    }

    public void forceStopCurrentPluginScheduleEntry() {
        if (currentPlugin != null && currentPlugin.isRunning()) {
            log.info("Force Stopping current plugin: " + currentPlugin.getCleanName());
            if (currentState == SchedulerState.RUNNING_PLUGIN) {
                setState(SchedulerState.HARD_STOPPING_PLUGIN);                
            }                        
            currentPlugin.hardStop(false);            
            // Wait a short time to see if the plugin stops immediately            
            if (currentPlugin != null) {                
            
                if (!currentPlugin.isRunning()) {                                                                
                    log.info("Plugin stopped successfully: " + currentPlugin.getCleanName());                                    
                    
                } else {
                    SwingUtilities.invokeLater(() -> {
                        forceStopCurrentPluginScheduleEntry();
                    });
                    log.info("Failed to hard stop plugin: " + currentPlugin.getCleanName());                
                }
            }
        }
        updatePanels();
    }

    @Subscribe
    public void onPluginChanged(PluginChanged event) {
        if (currentPlugin != null && event.getPlugin() == currentPlugin.getPlugin() && !currentPlugin.isRunning()) {            
            if (    currentState == SchedulerState.RUNNING_PLUGIN 
                || currentState == SchedulerState.SOFT_STOPPING_PLUGIN
                || currentState == SchedulerState.HARD_STOPPING_PLUGIN 
                || currentState == SchedulerState.WAITING_FOR_LOGIN
                || currentState == SchedulerState.STARTING_PLUGIN) {    
                if (currentState != SchedulerState.HOLD) {
                    log.info("Plugin stopped {}, we can schedule " + currentPlugin.getCleanName());
                    setState(SchedulerState.SCHEDULING);    
                }
                
            }
            currentPlugin = null;
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
        plugin.setEnabled(false);
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

        //log.info("Saving scheduled plugins to config: {}", json);
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
            log.info("Loading scheduled plugins from config: {}\n\n", json);
            
            if (json != null && !json.isEmpty()) {
                scheduledPlugins = PluginScheduleEntry.fromJson(json);
                
                // Apply stop settings from config to all loaded plugins
                for (PluginScheduleEntry plugin : scheduledPlugins) {
                    // Set timeout values from config
                    plugin.setSoftStopRetryInterval(Duration.ofSeconds(config.softStopRetrySeconds()));
                    plugin.setHardStopTimeout(Duration.ofSeconds(config.hardStopTimeoutSeconds()));
                    
                    // Resolve plugin references
                    resolvePluginReferences(plugin);
                    
                    StringBuilder logMessage = new StringBuilder();
                    logMessage.append(String.format("Loaded scheduled plugin: %s with %d conditions:\n",
                        plugin.getName(), 
                        plugin.getStopConditionManager().getConditions().size()));

                    // Stop conditions section
                    logMessage.append(String.format("Stop user condition (%d):\n\t%s\n",
                        plugin.getStopConditionManager().getUserLogicalCondition().getTotalConditionCount(),
                        plugin.getStopConditionManager().getUserLogicalCondition().getDescription()));
                    logMessage.append(String.format("Stop plugin conditions (%d):\n\t%s\n",
                        plugin.getStopConditionManager().getPluginCondition().getTotalConditionCount(),
                        plugin.getStopConditionManager().getPluginCondition().getDescription()));

                    // Start conditions section
                    logMessage.append(String.format("Start user condition (%d):\n\t%s\n",
                        plugin.getStartConditionManager().getUserLogicalCondition().getTotalConditionCount(),
                        plugin.getStartConditionManager().getUserLogicalCondition().getDescription()));
                    logMessage.append(String.format("Start plugin conditions (%d):\n\t%s",
                        plugin.getStartConditionManager().getPluginCondition().getTotalConditionCount(),
                        plugin.getStartConditionManager().getPluginCondition().getDescription()));

                    log.info(logMessage.toString());                   
                    
                    // Log condition details at debug level
                    if (Microbot.isDebug()) {
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
        return getNextScheduledPlugin(true, null).orElse(null);
    }

    private PluginScheduleEntry getNextScheduledPluginWithinTime(Duration timeWindow) {
        return getNextScheduledPlugin(false, timeWindow).orElse(null);
    }
   
    /**
     * Core method to find the next plugin based on various criteria.
     * This replaces the three separate methods with a consolidated implementation.
     * 
     * @param isDueToRun If true, only returns plugins that are due to run now
     * @param timeWindow If not null, limits to plugins triggered within this time window
     * @return Optional containing the next plugin to run, or empty if none match criteria
     */
    public Optional<PluginScheduleEntry> getNextScheduledPlugin(boolean isDueToRun, Duration timeWindow) {
        if (scheduledPlugins.isEmpty()) {
            return Optional.empty();
        }

        // Apply filters based on parameters
        List<PluginScheduleEntry> filteredPlugins = scheduledPlugins.stream()
            .filter(PluginScheduleEntry::isEnabled)
            .filter(plugin -> {
                // Filter by whether it's due to run now if requested
                if (isDueToRun && !plugin.isDueToRun()) {
                    return false;
                }
                
                // Filter by time window if specified
                if (timeWindow != null) {
                    Optional<ZonedDateTime> nextStartTime = plugin.getCurrentStartTriggerTime();
                    if (!nextStartTime.isPresent()) {
                        return false;
                    }
                    
                    ZonedDateTime cutoffTime = ZonedDateTime.now(ZoneId.systemDefault()).plus(timeWindow);
                    if (nextStartTime.get().isAfter(cutoffTime)) {
                        return false;
                    }
                }
                
                // Must have a valid next trigger time
                return plugin.getCurrentStartTriggerTime().isPresent();
            })
            .collect(Collectors.toList());
        
        if (filteredPlugins.isEmpty()) {
            return Optional.empty();
        }
        
        // Sort by priority (highest first)
        filteredPlugins.sort(Comparator.comparing(PluginScheduleEntry::getPriority).reversed());
        
        // Get plugins with the highest priority
        int highestPriority = filteredPlugins.get(0).getPriority();
        List<PluginScheduleEntry> highestPriorityPlugins = filteredPlugins.stream()
            .filter(p -> p.getPriority() == highestPriority)
            .collect(Collectors.toList());
        
        // From these, prefer non-default plugins
        List<PluginScheduleEntry> nonDefaultPlugins = highestPriorityPlugins.stream()
            .filter(p -> !p.isDefault())
            .collect(Collectors.toList());
        
        List<PluginScheduleEntry> candidatePlugins = !nonDefaultPlugins.isEmpty() ? nonDefaultPlugins : highestPriorityPlugins;
        
        // Apply final selection criteria
        PluginScheduleEntry selectedPlugin;
        
        // First check for non-randomizable plugins
        List<PluginScheduleEntry> nonRandomPlugins = candidatePlugins.stream()
            .filter(p -> !p.isAllowRandomScheduling())
            .collect(Collectors.toList());
            
        if (!nonRandomPlugins.isEmpty()) {
            // For non-random plugins, select the earliest
            selectedPlugin = nonRandomPlugins.stream()
                .min(Comparator.comparing(p -> 
                    p.getCurrentStartTriggerTime().orElse(ZonedDateTime.now(ZoneId.systemDefault()))))
                .orElse(null);
        } else {
            // For randomizable plugins, use weighted selection
            selectedPlugin = selectPluginWeighted(candidatePlugins);
        }
        
        return Optional.ofNullable(selectedPlugin);
    }

    /**
     * Checks if the current plugin should be stopped based on conditions
     */
    private void checkCurrentPlugin() {
        if (currentPlugin == null || !currentPlugin.isRunning()) {            
            //should not happen because only called when is isScheduledPluginRunning() is true
            return;
        }
        
        // Call the update hook if the plugin is a condition provider
        Plugin runningPlugin = currentPlugin.getPlugin();
        if (runningPlugin instanceof ConditionProvider) {
            ((ConditionProvider) runningPlugin).onConditionCheck();
        }
        
        // Log condition progress if debug mode is enabled
        if (Microbot.isDebug()) {
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
        currentPlugin.checkConditionsAndStop(true);
        if (!currentPlugin.isRunning() ){
            log.info("Plugin '{}' stopped because conditions were met", 
            currentPlugin.getCleanName());
            currentPlugin = null;
            setState(SchedulerState.SCHEDULING);
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
     * Checks if a specific plugin schedule entry is currently running
     * This explicitly compares by reference, not just by name
     */
    public boolean isRunningEntry(PluginScheduleEntry entry) {
        return entry.isRunning();
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
            
            // Clear login time when logging out
            loginTime = null;
        } else if (gameStateChanged.getGameState() == GameState.HOPPING) {
            // If the game state is HOPPING, stop the current plugin
            
        } else if (gameStateChanged.getGameState() == GameState.CONNECTION_LOST) {
            // If the game state is CONNECTION_LOST, stop the current plugin            
            // Clear login time when connection is lost
            loginTime = null;
        
        } else if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN_AUTHENTICATOR) {
            // If the game state is LOGGING_IN, stop the current plugin
            
            // Clear login time when logging out
            loginTime = null;
            stopScheduler();

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
            if (Microbot.isDebug()) {
                log.debug("Activity updated from skill: {} -> {}", skill.getName(), activity);
            }
        }
        
        ActivityIntensity intensity = ActivityIntensity.fromSkill(skill);
        if (intensity != null) {
            currentIntensity = intensity;
        }
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
            Optional<ZonedDateTime> nextTrigger = plugin.getCurrentStartTriggerTime();
            if (nextTrigger.isPresent()) {
                ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
                return Optional.of(Duration.between(now, nextTrigger.get()));
            }
        }
        return Optional.empty();
    }

    private void startLoginMonitoringThread(PluginScheduleEntry scheduledPlugin) {
        if (loginMonitor != null && loginMonitor.isAlive()) {
            log.info("Login monitoring thread already running for plugin '{}'", scheduledPlugin.getName());
            return;
        }
        setState(SchedulerState.LOGIN);
        this.loginMonitor = new Thread(() -> {
            try {
                log.debug("Login monitoring thread started for plugin '{}'", scheduledPlugin.getName());
                int loginAttempts = 0;
                final int MAX_LOGIN_ATTEMPTS = 3;
                
                // Keep checking until login completes or max attempts reached
                while (loginAttempts < MAX_LOGIN_ATTEMPTS) {
                    // Wait for login attempt to complete
                    Thread.sleep(5000);                                        
                    log.info("Login attempt {} of {} for plugin: {}", 
                        loginAttempts, MAX_LOGIN_ATTEMPTS, scheduledPlugin.getName());                    
                    // Try login again if needed
                    if (loginAttempts < MAX_LOGIN_ATTEMPTS) {
                        login();                           
                    }
                    if (Microbot.isLoggedIn()) {
                        // Successfully logged in, now increment the run count
                        log.info("Login successful, finalizing plugin start: {}", scheduledPlugin.getName());
                        continueStartingPluginScheduleEntry(scheduledPlugin);
                        //SwingUtilities.invokeLater(() -> {                            
                          //  setState(SchedulerState.RUNNING_PLUGIN);
                        //});
                        return;
                    }
                    if (Microbot.getClient().getGameState() != GameState.LOGGED_IN && 
                        Microbot.getClient().getGameState() != GameState.LOGGING_IN){
                        loginAttempts++;
                    }
                }
                
                // If we get here, login failed too many times
                log.error("Failed to login after {} attempts, aborting plugin: {}", 
                    MAX_LOGIN_ATTEMPTS, scheduledPlugin.getName());
                SwingUtilities.invokeLater(() -> {
                    // Clean up and set proper state
                    if (currentPlugin != null) {
                        currentPlugin.softStop(false);
                        setState(SchedulerState.SOFT_STOPPING_PLUGIN);
                    }else{
                        setState(SchedulerState.SCHEDULING);
                    }                                        
                    
                });
            } catch (InterruptedException e) {
                log.debug("Login monitoring thread for '{}' was interrupted", scheduledPlugin.getName());
            }
        });
        
        loginMonitor.setName("LoginMonitor-" + scheduledPlugin.getName());
        loginMonitor.setDaemon(true);
        loginMonitor.start();
    }
    private void logout() {
        if (Microbot.getClient().getGameState() == GameState.LOGGED_IN) {            
            if (isAutoLoginEnabled()){
                boolean successfulDisabled = disableAutoLogin();        
                if (successfulDisabled) {
                    
                } else {
                    log.error("Failed to disable AutoLogin plugin");
                }
            }
           
            Rs2Player.logout();
        }
    }
    private void login(){         

        // First check if AutoLogin plugin is available and enabled
        if (!isAutoLoginEnabled() && config.enableAutoLoginForSchedules()) {
            // Try to enable AutoLogin plugin
            if (enableAutoLogin()) {
                log.info("AutoLogin plugin enabled for scheduled login");
                // Give it a moment to initialize
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
                      
        // Fallback to manual login if AutoLogin is not available
        //log.info("Using manual login (AutoLogin plugin not available)");
        boolean successfulLogin = Microbot.getClientThread().runOnClientThreadOptional(()-> {                                               
                //check which login index means we are in authifcation or not a member        
                //TODO add these to "LOGIN" class  -> net.runelite.client.plugins.microbot.util.security
                int currentLoginIndex = Microbot.getClient().getLoginIndex();         
                boolean tryMemberWorld = currentLoginIndex !=-1; //TODO get correct one               
                if (currentLoginIndex == 4  || currentLoginIndex == 3){ // we are in the auth screen and cannot login
                    //3 mean wrong authtifaction
                    return false; // we are in auth
                }
                if (currentLoginIndex == 34){ // we are not a member and cannot login
                    int loginScreenWidth = 804;
                    int startingWidth = (Microbot.getClient().getCanvasWidth() / 2) - (loginScreenWidth / 2);
                    Microbot.getMouse().click(365 + startingWidth, 308); //clicks a button "OK" when you've been disconnected
                    sleep(600);
                    tryMemberWorld = false; // we are not a member
                }
                if (currentLoginIndex == 2){
                    return true; // connected to the server
                }
                log.info("currentLoginIndex: {} - member {}", currentLoginIndex, tryMemberWorld);
                new Login(Login.getRandomWorld(tryMemberWorld));
            
                return true; 
            }   
        ).orElse(false);
        log.info("login successful: {}", successfulLogin);
        if (!successfulLogin){
            stopScheduler();
        }
    }
    /**
     * Prints detailed diagnostic information about all scheduled plugins
     */
    public void debugAllScheduledPlugins() {
        log.info("==== PLUGIN SCHEDULER DIAGNOSTICS ====");
        log.info("Current state: {}", currentState);
        log.info("Number of scheduled plugins: {}", scheduledPlugins.size());
        
        for (PluginScheduleEntry plugin : scheduledPlugins) {
            log.info("\n----- Plugin: {} -----", plugin.getCleanName());
            log.info("Enabled: {}", plugin.isEnabled());
            log.info("Running: {}", plugin.isRunning());
            log.info("Is default: {}", plugin.isDefault());
            log.info("Due to run: {}", plugin.isDueToRun());
            log.info("Has start conditions: {}", plugin.hasAnyStartConditions());
            
            if (plugin.hasAnyStartConditions()) {
                log.info("Start conditions met: {}", plugin.getStartConditionManager().areConditionsMet());
                
                // Get next trigger time if any
                Optional<ZonedDateTime> nextTrigger = plugin.getCurrentStartTriggerTime();
                log.info("Next trigger time: {}", 
                    nextTrigger.isPresent() ? nextTrigger.get() : "None found");
                
                // Print detailed diagnostics
                log.info("\nDetailed start condition diagnosis:");
                log.info(plugin.diagnoseStartConditions());
            }
        }
        log.info("==== END DIAGNOSTICS ====");
    }
    
    /**
     * Enables the AutoLogin plugin
     * @return true if plugin was enabled successfully, false otherwise
     */
    private boolean enableAutoLogin() {
        if (isAutoLoginEnabled()) {
            return true; // Already enabled
        }               
        Microbot.getClientThread().runOnSeperateThread(() -> {          
            Plugin autoLoginPlugin = Microbot.getPlugin(AutoLoginPlugin.class.getName());
            if (autoLoginPlugin == null) {
                log.error("Failed to find AutoLoginPlugin");
                return false;
            }              
            log.info("AutoLoginPlugin starting");      
            Microbot.startPlugin(autoLoginPlugin);
            return true;
        });
    
        log.info("AutoLoginPlugin enabled");
        return true;
    }

    /**
     * Disables the AutoLogin plugin
     * @return true if plugin was disabled successfully, false otherwise
     */
    private boolean disableAutoLogin() {
        if (!isAutoLoginEnabled()) {
            return true; // Already disabled
        }               
        Microbot.getClientThread().runOnSeperateThread(() -> {      
            Plugin autoLoginPlugin = Microbot.getPlugin(AutoLoginPlugin.class.getName());
            if (autoLoginPlugin == null) {
                log.error("Failed to find AutoLoginPlugin");
                return false;
            }                  
            Microbot.stopPlugin(autoLoginPlugin);
            return true;
        });
        if (isAutoLoginEnabled()) {
            SwingUtilities.invokeLater(() -> {
                disableAutoLogin();
            });
            log.error("Failed to disable AutoLoginPlugin");
            return false;
        }
        log.info("AutoLoginPlugin disabled");
        return true;
    }

    /**
     * Enables the BreakHandler plugin
     * @return true if plugin was enabled successfully, false otherwise
     */
    private boolean enableBreakHandler() {
        if (isBreakHandlerEnabled()) {
            return true; // Already enabled
        }                      
        Microbot.getClientThread().runOnSeperateThread(() -> { 
            Plugin breakHandlerPlugin = Microbot.getPlugin(BreakHandlerPlugin.class.getName());
            log.info("BreakHandlerPlugin suggested to be enabled");
            if (breakHandlerPlugin == null) {
                log.error("Failed to find BreakHandlerPlugin");
                return false;
            }      
            log.info("BreakHandlerPlugin starting");      
            Microbot.startPlugin(breakHandlerPlugin);
            return true;
        });
        
        log.info("BreakHandlerPlugin wait");
        sleepUntil(()->isBreakHandlerEnabled(), 500);
        if (!isBreakHandlerEnabled()) {
            log.error("Failed to enable BreakHandlerPlugin");
            return false;
        }
        log.info("BreakHandlerPlugin enabled");
        return true;
    }

    /**
     * Disables the BreakHandler plugin
     * @return true if plugin was disabled successfully, false otherwise
     */
    private boolean disableBreakHandler() {
        if (!isBreakHandlerEnabled()) {
            log.info("BreakHandlerPlugin already disabled");
            return true; // Already disabled
        }                       
        BreakHandlerScript.setLockState(false); // Ensure we unlock before disabling
        log.info("disableBreakHandler - are we on client thread->; {}", Microbot.getClient().isClientThread());
        
        Microbot.getClientThread().runOnSeperateThread(() -> {   
            Plugin breakHandlerPlugin = Microbot.getPlugin(BreakHandlerPlugin.class.getName());
            if (breakHandlerPlugin == null) {
                log.error("Failed to find BreakHandlerPlugin");
                return false;
            }   
            log.info("BreakHandlerPlugin stopping");                  
            Microbot.stopPlugin(breakHandlerPlugin);
            return true;
        });
                                    
        if (isBreakHandlerEnabled()) {
            SwingUtilities.invokeLater(() -> {
                disableBreakHandler();
            });
            log.error("Failed to disable BreakHandlerPlugin");
            return false;
        }
        log.info("BreakHandlerPlugin disabled");
        return true;
    }


    /**
     * Sets the current scheduler state and updates UI
     */
    private void setState(SchedulerState newState) {
        if (currentState != newState) {
            log.debug("Scheduler state changed: {} -> {}", currentState, newState);
            breakStartTime = Optional.empty();
            // Set additional state information based on context
            switch (newState) {
                case INITIALIZING:
                    newState.setStateInformation(String.format("Checking for required plugins (%d/%d)", 
                        initCheckCount, MAX_INIT_CHECKS));
                    break;
                    
                case ERROR:
                    newState.setStateInformation(String.format("Initialization failed after %d/%d attempts. Client may not be at login screen.", 
                        initCheckCount, MAX_INIT_CHECKS));
                    break;
                    
                case WAITING_FOR_LOGIN:
                    newState.setStateInformation("Waiting for player to login to start plugin");
                    break;
                    
                case SOFT_STOPPING_PLUGIN:
                    newState.setStateInformation(currentPlugin != null ? 
                        "Attempting to gracefully stop " + currentPlugin.getCleanName() : 
                        "Attempting to gracefully stop plugin");
                    break;
                    
                case HARD_STOPPING_PLUGIN:
                    newState.setStateInformation(currentPlugin != null ? 
                        "Forcing " + currentPlugin.getCleanName() + " to stop" : 
                        "Forcing plugin to stop");
                    break;
                    
                case RUNNING_PLUGIN:
                    newState.setStateInformation(currentPlugin != null ? 
                        "Running " + currentPlugin.getCleanName() : 
                        "Running plugin");
                    break;
                    
                case STARTING_PLUGIN:
                    newState.setStateInformation(currentPlugin != null ? 
                        "Starting " + currentPlugin.getCleanName() : 
                        "Starting plugin");
                    break;
                    
                case SHORT_BREAK:
                    PluginScheduleEntry nextPlugin = getNextScheduledPlugin();
                    if (nextPlugin != null) {
                        newState.setStateInformation("Taking short break until " + 
                            nextPlugin.getCleanName() + " is scheduled to run");
                    } else {
                        newState.setStateInformation("Taking a short break");
                    }
                    breakStartTime = Optional.of(ZonedDateTime.now(ZoneId.systemDefault()));
                    break;
                    
                case WAITING_FOR_SCHEDULE:
                    newState.setStateInformation("Waiting for the next scheduled plugin to become due");
                    break;
                    
                case SCHEDULING:
                    newState.setStateInformation("Actively checking plugin schedules");
                    break;
                    
                case READY:
                    newState.setStateInformation("Ready to run - click Start to begin scheduling");
                    break;
                    
                case HOLD:
                    newState.setStateInformation("Scheduler was manually stopped");
                    break;
                    
                default:
                    newState.setStateInformation(""); // Clear any previous information
                    break;
            }
            
            currentState = newState;
            updatePanels();
        }
    }

}
