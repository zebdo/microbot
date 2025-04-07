package net.runelite.client.plugins.microbot.toa;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.toa.data.ToaState;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import java.util.concurrent.TimeUnit;


public class ToaScript extends Script {

    public static boolean test = false;

    public static ToaState toaState;

    public boolean run(ToaConfig config) {
        Microbot.enableAutoRunOn = false;
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;
                long startTime = System.currentTimeMillis();

                calculateToaState();

                if (toaState == null) return;


                switch(toaState) {
                    case PuzzleRoom:
                        
                        break;
                }



                long endTime = System.currentTimeMillis();
                long totalTime = endTime - startTime;
                System.out.println("Total time for loop " + totalTime);

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        return true;
    }

    private void calculateToaState() {
        if (Rs2Player.getWorldLocation().getRegionID() == 14162) {
            toaState = ToaState.PuzzleRoom;
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

    public static void readRoomPuzzleLayout() {

    }
}