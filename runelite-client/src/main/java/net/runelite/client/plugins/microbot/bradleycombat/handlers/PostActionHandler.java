package net.runelite.client.plugins.microbot.bradleycombat.handlers;

import com.google.inject.Inject;
import net.runelite.api.Player;
import net.runelite.api.events.AnimationChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.microbot.bradleycombat.BradleyCombatConfig;
import net.runelite.client.plugins.microbot.bradleycombat.actions.MageAction;
import net.runelite.client.plugins.microbot.bradleycombat.actions.MeleeAction;
import net.runelite.client.plugins.microbot.bradleycombat.actions.RangeAction;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.bradleycombat.actions.SpecAction;

public class PostActionHandler {
    private final BradleyCombatConfig config;

    @Inject
    public PostActionHandler(BradleyCombatConfig config) {
        this.config = config;
    }

    @Subscribe
    public void onAnimationChanged(AnimationChanged event) {
        Player local = Microbot.getClient().getLocalPlayer();
        if (local == null)
            return;
        int currentAnim = local.getAnimation();
        if (matches(currentAnim, config.postActionMeleePrimary())) {
            Microbot.getClientThread().runOnSeperateThread(() -> {
                new MeleeAction(config, 1).execute();
                return null;
            });
            return;
        } else if (matches(currentAnim, config.postActionMeleeSecondary())) {
            Microbot.getClientThread().runOnSeperateThread(() -> {
                new MeleeAction(config, 2).execute();
                return null;
            });
            return;
        } else if (matches(currentAnim, config.postActionMeleeTertiary())) {
            Microbot.getClientThread().runOnSeperateThread(() -> {
                new MeleeAction(config, 3).execute();
                return null;
            });
            return;
        } else if (matches(currentAnim, config.postActionRangePrimary())) {
            Microbot.getClientThread().runOnSeperateThread(() -> {
                new RangeAction(config, 1).execute();
                return null;
            });
            return;
        } else if (matches(currentAnim, config.postActionRangeSecondary())) {
            Microbot.getClientThread().runOnSeperateThread(() -> {
                new RangeAction(config, 2).execute();
                return null;
            });
            return;
        } else if (matches(currentAnim, config.postActionRangeTertiary())) {
            Microbot.getClientThread().runOnSeperateThread(() -> {
                new RangeAction(config, 3).execute();
                return null;
            });
            return;
        } else if (matches(currentAnim, config.postActionMagePrimary())) {
            Microbot.getClientThread().runOnSeperateThread(() -> {
                new MageAction(config, 1).execute();
                return null;
            });
            return;
        } else if (matches(currentAnim, config.postActionMageSecondary())) {
            Microbot.getClientThread().runOnSeperateThread(() -> {
                new MageAction(config, 2).execute();
                return null;
            });
            return;
        } else if (matches(currentAnim, config.postActionMageTertiary())) {
            Microbot.getClientThread().runOnSeperateThread(() -> {
                new MageAction(config, 3).execute();
                return null;
            });
            return;
        } else if (matches(currentAnim, config.postActionSpecPrimary())) {
            Microbot.getClientThread().runOnSeperateThread(() -> {
                new SpecAction(config, 1).execute();
                return null;
            });
            return;
        } else if (matches(currentAnim, config.postActionSpecSecondary())) {
            Microbot.getClientThread().runOnSeperateThread(() -> {
                new SpecAction(config, 2).execute();
                return null;
            });
            return;
        } else if (matches(currentAnim, config.postActionSpecTertiary())) {
            Microbot.getClientThread().runOnSeperateThread(() -> {
                new SpecAction(config, 3).execute();
                return null;
            });
        }
    }

    private boolean matches(int currentAnim, String configValue) {
        if (configValue == null || configValue.trim().isEmpty())
            return false;
        String[] parts = configValue.split("\\s*,\\s*");
        for (String part : parts) {
            try {
                int animId = Integer.parseInt(part.trim());
                if (animId == currentAnim)
                    return true;
            } catch (NumberFormatException ignored) {
            }
        }
        return false;
    }
}