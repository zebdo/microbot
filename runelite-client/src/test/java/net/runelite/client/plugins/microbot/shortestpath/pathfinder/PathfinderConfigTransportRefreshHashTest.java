package net.runelite.client.plugins.microbot.shortestpath.pathfinder;

import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import org.junit.Test;

import static org.junit.Assert.assertNotEquals;

public class PathfinderConfigTransportRefreshHashTest {

    @Test
    public void verificationHashDiffersForNotStartedVsInProgressQuestState() {
        int[] boostedLevels = new int[]{1, 50, 99};
        int[] sortedVarbits = new int[0];
        int[] sortedVarplayers = new int[0];
        int trackedQuestId = 987654;
        int[] sortedQuestIds = new int[]{trackedQuestId};
        int clientOfKourendId = Quest.CLIENT_OF_KOUREND.getId();

        int hashNotStarted = PathfinderConfig.computeTransportRefreshVerificationHash(
                boostedLevels,
                sortedVarbits,
                sortedVarplayers,
                sortedQuestIds,
                questId -> {
                    if (questId == trackedQuestId) {
                        return QuestState.NOT_STARTED;
                    }
                    if (questId == clientOfKourendId) {
                        return QuestState.FINISHED;
                    }
                    return QuestState.NOT_STARTED;
                });

        int hashInProgress = PathfinderConfig.computeTransportRefreshVerificationHash(
                boostedLevels,
                sortedVarbits,
                sortedVarplayers,
                sortedQuestIds,
                questId -> {
                    if (questId == trackedQuestId) {
                        return QuestState.IN_PROGRESS;
                    }
                    if (questId == clientOfKourendId) {
                        return QuestState.FINISHED;
                    }
                    return QuestState.NOT_STARTED;
                });

        assertNotEquals("Quest state transition should invalidate cached transport refresh snapshot",
                hashNotStarted, hashInProgress);
    }
}
