package net.runelite.client.plugins.microbot.util.walker.door;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Rs2WalkerAwaitsTest {
    @Test
    public void shouldAcceptIdleDoorAwait_requiresResolvedEdge() {
        assertFalse(Rs2WalkerAwaits.shouldAcceptIdleDoorAwait(false, false, 1300L, false));
        assertTrue(Rs2WalkerAwaits.shouldAcceptIdleDoorAwait(false, false, 1301L, true));
    }

    @Test
    public void shouldAcceptIdleDoorAwait_rejectsMovingOrAnimating() {
        assertFalse(Rs2WalkerAwaits.shouldAcceptIdleDoorAwait(true, false, 5000L, true));
        assertFalse(Rs2WalkerAwaits.shouldAcceptIdleDoorAwait(false, true, 5000L, true));
    }

    @Test
    public void shouldAcceptIdleDoorAwait_rejectsBeforeMinimumElapsed() {
        assertFalse(Rs2WalkerAwaits.shouldAcceptIdleDoorAwait(false, false, 1200L, true));
        assertFalse(Rs2WalkerAwaits.shouldAcceptIdleDoorAwait(false, false, 800L, true));
    }
}
