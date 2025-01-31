package net.runelite.client.plugins.microbot.brimAgility;

import net.runelite.api.GameObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;


public class BrimScript extends Script {

    private boolean Gotticket = false;
    private LocalDateTime dateTime = LocalDateTime.now();
    private int currentMinute = dateTime.getMinute();
    private int nextAcceptableMinute = (currentMinute+2);

    public static boolean test = false;
    public boolean run(BrimConfig config) {
        Microbot.enableAutoRunOn = false;
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;
                long startTime = System.currentTimeMillis();

                WorldPoint tile1 =(new WorldPoint(2801, 9568, 3));
                WorldPoint tile2 =(new WorldPoint(2798, 9568, 3));

                WorldPoint reward =(new WorldPoint(2794, 9568, 0));
                WorldPoint reward2 =(new WorldPoint(2805, 9568, 0));

                if(Rs2Player.getHealthPercentage()<=generateRandomNumber(40,60)){
                    if(Rs2Inventory.contains(it->it!=null&&it.isFood())){
                        Rs2Inventory.interact(it->it!=null&&it.isFood(), "Eat");
                        sleep(1000, 2000);
                    }
                }

                int pre = Rs2Inventory.count("Brimhaven voucher");
                if(Microbot.getClient().getHintArrowPoint()!=null){
                if(Microbot.getClient().getHintArrowPoint().equals(reward)||Microbot.getClient().getHintArrowPoint().equals(reward2)){
                    Microbot.log("Ticket dispensor is close enough");
                    if(Gotticket==false) {
                        Microbot.log("Getting our ticket");
                        if(Microbot.getClient().getHintArrowPoint().equals(reward)){
                            Rs2GameObject.interact(new WorldPoint(2794, 9568, 3));
                        }
                        if(Microbot.getClient().getHintArrowPoint().equals(reward2)){
                            Rs2GameObject.interact(new WorldPoint(2805, 9568, 3));
                        }
                        sleep(8000, 12000);
                        int post = Rs2Inventory.count("Brimhaven voucher");
                        if ((pre != post)
                                || (Rs2Player.getWorldLocation().distanceTo(new WorldPoint(2794, 9568, 3))<=1)
                                || (Rs2Player.getWorldLocation().distanceTo(new WorldPoint(2805, 9568, 3))<=1)) {
                            Gotticket = true;
                            dateTime = LocalDateTime.now();
                            currentMinute = dateTime.getMinute();
                            nextAcceptableMinute = (currentMinute + 1) % 60;
                        }
                        return;
                    } else {
                        currentMinute = dateTime.getMinute();
                        Microbot.log("We allready got our ticket");
                        if(nextAcceptableMinute == currentMinute || nextAcceptableMinute <= currentMinute){
                            Microbot.log("It's been two minutes taking another ticket");
                            Gotticket = false;
                        }
                    }
                }
                }

                if(Rs2Player.getWorldLocation().equals(tile1)){
                    Rs2Walker.walkCanvas(tile2);
                    sleep(1000, 2000);
                    return;
                }

                if(Rs2Player.getWorldLocation().equals(tile2)){
                    Rs2Walker.walkCanvas(tile1);
                    sleep(1000, 2000);
                    return;
                }

                if(!Rs2Player.getWorldLocation().equals(tile2) && !Rs2Player.getWorldLocation().equals(tile1)){
                    Rs2Walker.walkCanvas(tile1);
                    sleep(1000, 2000);
                    return;
                }

                if(Rs2Dialogue.isInDialogue()){
                    Rs2Dialogue.clickContinue();
                    sleep(1000, 2000);
                }

                if(Rs2Player.getHealthPercentage()<=generateRandomNumber(40,60)){
                    if(Rs2Inventory.contains(it->it!=null&&it.isFood())){
                        Rs2Inventory.interact(it->it!=null&&it.isFood(), "Eat");
                        sleep(1000, 2000);
                    }
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

    @Override
    public void shutdown() {
        Gotticket = false;
        super.shutdown();
    }
    public int generateRandomNumber(int min, int max) {
        return Rs2Random.nextInt(min, max, 1000, true);
    }
}