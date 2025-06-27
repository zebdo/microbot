package net.runelite.client.plugins.microbot.moonsOfPeril;

import lombok.Getter;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;

import net.runelite.client.plugins.microbot.moonsOfPeril.enums.State;
import net.runelite.client.plugins.microbot.moonsOfPeril.handlers.*;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class moonsOfPerilScript extends Script {

    @Getter
    private State state = State.IDLE;
    public static boolean test = false;
    public static volatile State CURRENT_STATE = State.IDLE;

    private final Map<State, BaseHandler> handlers = new EnumMap<>(State.class);

    public boolean run(moonsOfPerilConfig config) {
        initHandlers(config);

        Microbot.enableAutoRunOn = false;
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;
                long start = System.currentTimeMillis();

                /* ---------------- MAIN LOOP ---------------- */
                state = determineState();
                CURRENT_STATE = state;
                BaseHandler h = handlers.get(state);
                if (h != null && h.validate()) {
                    h.execute();
                }
                /* ------------------------------------------- */

                Microbot.log("Loop " + (System.currentTimeMillis() - start) + " ms");
            } catch (Exception ex) {
                Microbot.log("MoonsOfPerilScript error: " + ex.getMessage());
            }
        }, 0, 600, TimeUnit.MILLISECONDS);            // 600 ms ≈ one game tick

        return true;
    }

    /* ------------------------------------------------------------------ */
    /* One-off wiring of state → handler instances                        */
    /* ------------------------------------------------------------------ */
    private void initHandlers(moonsOfPerilConfig cfg) {
        handlers.put(State.IDLE,        new IdleHandler());
        handlers.put(State.RESUPPLY,    new ResupplyHandler(cfg));
        handlers.put(State.ECLIPSE_MOON,new EclipseMoonHandler(cfg));
        handlers.put(State.BLUE_MOON,   new BlueMoonHandler(cfg));
        handlers.put(State.BLOOD_MOON,  new BloodMoonHandler(cfg));
        handlers.put(State.REWARDS,     new RewardHandler());
        handlers.put(State.DEATH,       new DeathHandler());
    }

    /* ------------------------------------------------------------------ */
    /* state logic                */
    /* ------------------------------------------------------------------ */
    private State determineState() {
        /* 1 ─ In case of death */
        if (isPlayerDead())                 return State.DEATH;

        /* 2 ─ if all bosses are dead --> end-of-run chest loot */
        if (readyToLootChest())             return State.REWARDS;

        /* 3 ─ Do resupply as needed before boss phases */
        if (needsResupply())                  return State.RESUPPLY;

        /* 4 ─ boss phases in order */
        if (eclipseMoonSequence())       return State.ECLIPSE_MOON;
        if (blueMoonSequence())         return State.BLUE_MOON;
        if (bloodMoonSequence())         return State.BLOOD_MOON;

        /* 5 ─ nothing to do */
        return State.IDLE;
    }

    /* ---------- Supplies ------------------------------------------------- */
    private boolean needsResupply()
    {
        // Skip while we're already resupplying
        if (state == State.RESUPPLY) {
            return false;
        }
        BaseHandler resupply = handlers.get(State.RESUPPLY);
        return resupply != null && resupply.validate();
    }

    /* ---------- Eclipse Moon -------------------------------------------- */
    private boolean eclipseMoonSequence()
    {
        BaseHandler eclipse = handlers.get(State.ECLIPSE_MOON);
        return eclipse != null && eclipse.validate();
    }

    /* ---------- Blue Moon ------------------------------------------------ */
    private boolean blueMoonSequence()
    {
        BaseHandler blue = handlers.get(State.BLUE_MOON);
        return blue != null && blue.validate();
    }

    /* ---------- Blood Moon ---------------------------------------------- */
    private boolean bloodMoonSequence()
    {
        BaseHandler blood = handlers.get(State.BLOOD_MOON);
        return blood != null && blood.validate();
    }

    /* ---------- Rewards Chest ---------------------------------------------- */
    private boolean readyToLootChest() {
        BaseHandler reward = handlers.get(State.REWARDS);
        return reward != null && reward.validate();
    }

    /* ---------- Death Handler ---------------------------------------------- */
    private boolean isPlayerDead() {
        BaseHandler reward = handlers.get(State.DEATH);
        return reward != null && reward.validate();
    }

    /* ------------------------------------------------------------------ */
    /* Clean shutdown – cancels the scheduled task and frees resources    */
    /* ------------------------------------------------------------------ */
    @Override
    public void shutdown() {
        super.shutdown();
    }
}
