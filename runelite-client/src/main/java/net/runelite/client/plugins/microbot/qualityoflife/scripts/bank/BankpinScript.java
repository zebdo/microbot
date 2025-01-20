package net.runelite.client.plugins.microbot.qualityoflife.scripts.bank;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.qualityoflife.QoLConfig;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;

import java.util.concurrent.TimeUnit;

public class BankpinScript extends Script {
    public boolean run(QoLConfig config) {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run() || config.bankPin().isBlank()) return;

                Rs2Bank.handleBankPin(config.bankPin());

            } catch(Exception ex) {
                System.out.println(ex.getMessage());
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        return true;
    }
}
