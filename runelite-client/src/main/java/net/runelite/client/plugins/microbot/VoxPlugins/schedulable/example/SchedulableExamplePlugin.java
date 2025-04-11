package net.runelite.client.plugins.microbot.VoxPlugins.schedulable.example;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.pluginscheduler.api.ConditionProvider;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.Condition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.logical.LogicalCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.logical.OrCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.resource.LootItemCondition;

import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.IntervalCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.event.ScheduledStopEvent;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.woodcutting.enums.WoodcuttingTree;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.time.Duration;

@PluginDescriptor(
        name = "Schedulable Example",
        description = "Designed for use the scheduler and test its features",
        tags = {"microbot", "woodcutting", "combat","scheduler"},
        enabledByDefault = false,
        canBeScheduled = true
)
@Slf4j
public class SchedulableExamplePlugin extends Plugin implements ConditionProvider {
    
    @Inject
    private SchedulableExampleConfig config;
    
    @Inject
    private Client client;
   
    @Provides
    SchedulableExampleConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(SchedulableExampleConfig.class);
    }
    
    private SchedulableExampleScript script;
    private WorldPoint lastLocation = null;
    private int logsCollected = 0;
    
    @Override
    protected void startUp() {
        loadLastLocation();
        script = new SchedulableExampleScript();
        
        
        script.run(config,lastLocation);
        
        
    }
    
    @Override
    protected void shutDown() {
        if (script != null) {
            saveCurrentLocation();
            script.shutdown();
            script = null;
        }
    }
    
    private void loadLastLocation() {
        WorldPoint savedLocation = config.lastLocation();
        if (savedLocation == null) {
            log.warn("No saved location found in config.");
            this.lastLocation = Rs2Player.getWorldLocation();
            return;
        }
        this.lastLocation =  savedLocation;
       
    }
    
    private void saveCurrentLocation() {
        if (client.getLocalPlayer() != null) {
            WorldPoint currentLoc = client.getLocalPlayer().getWorldLocation();
            config.setLastLocation(currentLoc);
        }
    }
    

    @Override
    public LogicalCondition getStopCondition() {
        // Create an OR condition - stop when either time is up OR we have enough logs
        OrCondition orCondition = new OrCondition();
        
        // Add time condition
        int minMinutes = config.minRuntime();
        int maxMinutes = config.maxRuntime(); 
        
        orCondition.addCondition(IntervalCondition.createRandomized(
            Duration.ofMinutes(minMinutes),
            Duration.ofMinutes(maxMinutes)
        ));
        
       
        LogicalCondition lootItemCondition = makeLogicalLootItemCondition();
        orCondition.addCondition(lootItemCondition);
            
        return orCondition;
    }
    private LogicalCondition makeLogicalLootItemCondition() {
        // Add logs condition
        String lootItemsString = config.lootItems();
        List<String> lootItemsList = new ArrayList<>();
        if (lootItemsString != null && !lootItemsString.isEmpty()) {
            String[] lootItemsArray = lootItemsString.split(",");
            int validPatternsCount = 0;
            for (String item : lootItemsArray) {
                String trimmedItem = item.trim();
                try {
                    // Validate regex pattern
                    java.util.regex.Pattern.compile(trimmedItem);
                    lootItemsList.add(trimmedItem);
                    log.info("Valid loot item pattern found: {}", trimmedItem);
                    validPatternsCount++;
                } catch (java.util.regex.PatternSyntaxException e) {
                    log.warn("Invalid regex pattern: '{}' - {}", trimmedItem, e.getMessage());
                }
            }
            log.info("Total valid loot item patterns: {}", validPatternsCount);
        }
        boolean andLogical = config.itemsToLootLogical();
        int minLootItems = config.minItems();
        int maxLootItems = config.maxItems();
        List<Integer> minLootItemPerPattern = new ArrayList<>();
        List<Integer> maxLootItemPerPattern = new ArrayList<>();
        for (String item : lootItemsList) {
            int minLoot = Rs2Random.between(minLootItems, maxLootItems);
               
            int maxLoot = Rs2Random.between(minLoot, maxLootItems);
            //Clip max to maxLootItems when smaller than minLoot 
            if (maxLoot < minLoot) {
                maxLoot = maxLootItems;
            }
            minLootItemPerPattern.add(minLoot);
            maxLootItemPerPattern.add(maxLoot);
        }
        boolean includeNoted = config.includeNoted();
        boolean allowNoneOwner = config.allowNoneOwner();
        LogicalCondition lootItemCondition = null;
        if(andLogical){
            lootItemCondition = LootItemCondition.createAndCondition(
                lootItemsList,
                minLootItemPerPattern,
                maxLootItemPerPattern,
                includeNoted,
                allowNoneOwner
            );
        }else{
            lootItemCondition = LootItemCondition.createOrCondition(
                lootItemsList,
                minLootItemPerPattern,
                maxLootItemPerPattern,
                includeNoted,
                allowNoneOwner
            );
        }
        return lootItemCondition;
    }
    @Override
    public void onConditionCheck() {
        // Update logs count when condition is checked
        if (script != null) {
            logsCollected = script.getLogsCollected();
        }
    }
    
    // Method for the scheduler to check progress
    public int getLogsCollected() {
        return logsCollected;
    }
          /**
     * Handles the {@link ScheduledStopEvent} posted by the scheduler when stop conditions are met.
     * <p>
     * This event handler is automatically called when the scheduler determines that
     * all required conditions for stopping have been met. The default implementation 
     * calls {@link Microbot#stopPlugin(net.runelite.client.plugins.Plugin)} to gracefully
     * stop the plugin.
     * <p>
     * Plugin developers should generally not override this method unless they need
     * custom stop behavior. If overridden, make sure to either call the default 
     * implementation or properly stop the plugin.
     * <p>
     * Note: This is an EventBus subscriber method and requires the implementing
     * plugin to be registered with the EventBus for it to be called.
     *
     * @param event The stop event containing the plugin reference that should be stopped
     */
    @Override
    @Subscribe
    public void onScheduledStopEvent(ScheduledStopEvent event) {  
        config.setLastLocation(Rs2Player.getWorldLocation());      
        if (event.getPlugin() == this) {
            System.out.println("Scheduling stop for plugin: " + event.getPlugin().getClass().getSimpleName());
            // Schedule the stop operation on the client thread instead of doing it directly
            Microbot.getClientThread().invokeLater(() -> {
                try {
                    Microbot.getPluginManager().setPluginEnabled(this, false);
                    Microbot.getPluginManager().stopPlugin(this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }
}