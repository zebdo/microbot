package net.runelite.client.plugins.microbot.util.walker.stall;

import net.runelite.api.widgets.ComponentID;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.leaguetransport.Rs2LeaguesTransport;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

public final class Rs2WalkerStallPolicy {
    private Rs2WalkerStallPolicy() {
    }

    public static boolean shouldSkipStallAccounting(long leaguesPendingMaxAgeMs) {
        if (Rs2LeaguesTransport.isTeleportInProgress()) {
            return true;
        }
        if (Rs2LeaguesTransport.isLeaguesAreaTeleportPending(leaguesPendingMaxAgeMs)) {
            return true;
        }
        if (Rs2Dialogue.isInDialogue()) {
            return true;
        }
        return !Rs2Widget.isHidden(ComponentID.FAIRY_RING_TELEPORT_BUTTON);
    }

    public static long computeThresholdMs(long baseMs,
                                          double combatMultiplier,
                                          double animatingMultiplier,
                                          double movingMultiplier,
                                          double interimMultiplier,
                                          double interactingMultiplier,
                                          boolean inCombat,
                                          boolean animating,
                                          boolean moving,
                                          boolean hasInterimTarget,
                                          boolean interactingNearPath) {
        double multiplier = 1.0;
        if (inCombat) {
            multiplier = Math.max(multiplier, combatMultiplier);
        }
        if (animating) {
            multiplier = Math.max(multiplier, animatingMultiplier);
        }
        if (moving) {
            multiplier = Math.max(multiplier, movingMultiplier);
        }
        if (hasInterimTarget) {
            multiplier = Math.max(multiplier, interimMultiplier);
        }
        if (interactingNearPath) {
            multiplier = Math.max(multiplier, interactingMultiplier);
        }
        return Math.round(baseMs * multiplier);
    }
}
