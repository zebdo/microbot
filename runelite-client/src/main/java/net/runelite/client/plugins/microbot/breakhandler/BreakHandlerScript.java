package net.runelite.client.plugins.microbot.breakhandler;

import lombok.Setter;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.logical.LockCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.logical.LogicalCondition;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.security.Login;
import net.runelite.client.ui.ClientUI;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BreakHandlerScript extends Script {
    public static String version = "1.0.0";

    public static int breakIn = -1;
    public static int breakDuration = -1;

    public static int totalBreaks = 0;

    public static Duration duration;
    public static Duration breakInDuration;
    @Setter
    public static boolean lockState = false;
    private String title = "";
    private BreakHandlerConfig config;

    public static boolean isBreakActive() {
        return breakDuration > 0;
    }

    public static String formatDuration(Duration duration, String header) {
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;
        return String.format(header + " %02d:%02d:%02d", hours, minutes, seconds);
    }

    public boolean run(BreakHandlerConfig config) {
        this.config = config;
        Microbot.enableAutoRunOn = false;
        title = ClientUI.getFrame().getTitle();
        breakIn = Rs2Random.between(config.timeUntilBreakStart() * 60, config.timeUntilBreakEnd() * 60);
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                
                if (config.breakNow() && !Microbot.pauseAllScripts.get() && !isLockState()) {
                    Microbot.log("Break triggered via config toggle: " + config.breakNow());
                    startBreak();
                    return;
                }
               
                if (config.playSchedule().isOutsideSchedule() && config.usePlaySchedule() && !isLockState()) {
                    Duration untilNextSchedule = config.playSchedule().timeUntilNextSchedule();
                    breakIn = -1;
                    breakDuration = (int) untilNextSchedule.toSeconds();
                }

                if (breakIn > 0 && breakDuration <= 0) {
                    if(!(Rs2AntibanSettings.takeMicroBreaks && config.onlyMicroBreaks()))
                        breakIn--;

                    duration = Duration.between(LocalDateTime.now(), LocalDateTime.now().plusSeconds(breakIn));
                    breakInDuration = duration;
                }

                if (breakDuration > 0) {
                    breakDuration--;
                    duration = Duration.between(LocalDateTime.now(), LocalDateTime.now().plusSeconds(breakDuration));
                    long hours = BreakHandlerScript.duration.toHours();
                    long minutes = BreakHandlerScript.duration.toMinutes() % 60;
                    long seconds = BreakHandlerScript.duration.getSeconds() % 60;
                    if (Rs2AntibanSettings.takeMicroBreaks && Rs2AntibanSettings.microBreakActive) {
                        ClientUI.getFrame().setTitle(String.format("Micro break duration: %02d:%02d:%02d", hours, minutes, seconds));
                    } else if (config.playSchedule().isOutsideSchedule() && config.usePlaySchedule()) {
                        ClientUI.getFrame().setTitle(String.format("Next schedule in: %02d:%02d:%02d", hours, minutes, seconds));
                    } else {
                        ClientUI.getFrame().setTitle(String.format("Break duration: %02d:%02d:%02d", hours, minutes, seconds));
                    }
                }

                if (breakDuration <= 0 && Microbot.pauseAllScripts.get()) {
                    if (Rs2AntibanSettings.universalAntiban && Rs2AntibanSettings.actionCooldownActive)
                        return;
					Microbot.pauseAllScripts.compareAndSet(true, false);
                    if (breakIn <= 0 && !isLockState())
                        breakIn = Rs2Random.between(config.timeUntilBreakStart() * 60, config.timeUntilBreakEnd() * 60);

                    if (config.useRandomWorld()) {
                        new Login(Login.getRandomWorld(Login.activeProfile.isMember()));
                    } else {
                        new Login();
                    }
                    totalBreaks++;
                    ClientUI.getFrame().setTitle(title);
                    if (Rs2AntibanSettings.takeMicroBreaks) {
                        Rs2AntibanSettings.microBreakActive = false;
                    }
                    if (config.breakNow()) {
                        Microbot.getConfigManager().setConfiguration(BreakHandlerConfig.configGroup, "breakNow", false);
                    }
                    return;
                }

                if ((breakIn <= 0 && !Microbot.pauseAllScripts.get() && !isLockState()) || (Rs2AntibanSettings.microBreakActive && !Microbot.pauseAllScripts.get() && !isLockState())) {
                    startBreak();
                }

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        return true;
    }

    private void startBreak() {
        // Log before processing the break
        Microbot.log("Starting break. breakNow setting: " + config.breakNow());

		Microbot.pauseAllScripts.compareAndSet(false, true);

        if (Rs2AntibanSettings.microBreakActive)
            return;
        if (config.playSchedule().isOutsideSchedule() && config.usePlaySchedule()) {
            Rs2Player.logout();
            return;
        }

        breakDuration = Rs2Random.between(config.breakDurationStart() * 60, config.breakDurationEnd() * 60);
        if (config.logoutAfterBreak()) {
            Rs2Player.logout();
        }
    }

    @Override
    public void shutdown() {
        breakIn = 0;
        breakDuration = 0;
        ClientUI.getFrame().setTitle(title);
        super.shutdown();
    }

    public void reset() {
        breakIn = 0;
        breakDuration = 0;
        ClientUI.getFrame().setTitle(title);
    }

    /**
     * Checks if the break handler is currently in a locked state.
     * This includes both the manual lock state and any locked conditions from schedulable plugins.
     * 
     * @return true if locked, false otherwise
     */
    public static boolean isLockState() {
        return lockState || hasLockedSchedulablePlugins();
    }

    /**
     * Detects if any enabled SchedulablePlugin has locked LockConditions.
     * This prevents the break handler from taking breaks during critical plugin operations.
     * 
     * @return true if any schedulable plugin has locked conditions, false otherwise
     */
    private static boolean hasLockedSchedulablePlugins() {
        try {
            // Get all enabled plugins from the plugin manager
            return Microbot.getPluginManager().getPlugins().stream()
                .filter(plugin -> Microbot.getPluginManager().isPluginEnabled(plugin))
                .filter(plugin -> plugin instanceof net.runelite.client.plugins.microbot.pluginscheduler.api.SchedulablePlugin)
                .map(plugin -> (net.runelite.client.plugins.microbot.pluginscheduler.api.SchedulablePlugin) plugin)
                .anyMatch(schedulablePlugin -> {
                    try {
                        // Get the stop condition from the schedulable plugin
                        LogicalCondition stopCondition = schedulablePlugin.getStopCondition();
                        if (stopCondition != null) {
                            // Find all LockConditions in the logical condition structure using the utility method
                            List<LockCondition> lockConditions = stopCondition.findAllLockConditions();
                            // Check if any LockCondition is currently locked
                            return lockConditions.stream().anyMatch(LockCondition::isLocked);
                        }
                        return false;
                    } catch (Exception e) {
                        Microbot.log("Error checking stop conditions for schedulable plugin - " + e.getMessage());
                        return false;
                    }
                });
        } catch (Exception e) {
            Microbot.log("Error checking schedulable plugins for lock conditions: " + e.getMessage());
            return false;
        }
    }
}
