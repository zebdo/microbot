package net.runelite.client.plugins.microbot.moonsOfPeril.handlers;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.moonsOfPeril.enums.GameObjects;
import net.runelite.client.plugins.microbot.moonsOfPeril.enums.Locations;
import net.runelite.client.plugins.microbot.moonsOfPeril.enums.State;
import net.runelite.client.plugins.microbot.moonsOfPeril.enums.Widgets;
import net.runelite.client.plugins.microbot.moonsOfPeril.moonsOfPerilConfig;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import static net.runelite.client.plugins.microbot.util.Global.sleep;

public class BloodMoonHandler implements BaseHandler {

    private static final String bossName = "Blood Moon";
    private static final int bossStatusWidgetID = Widgets.BLOOD_MOON_ID.getID();
    private static final int bossStatueObjectID = GameObjects.BLOOD_MOON_STATUE_ID.getID();
    private static final WorldPoint bossLobbyLocation = Locations.BLOOD_LOBBY.getWorldPoint();
    private static final WorldPoint bossArenaCenter = Locations.BLOOD_ARENA_CENTER.getWorldPoint();
    private String weaponMain;
    private String shield;

    public BloodMoonHandler(moonsOfPerilConfig cfg) {
        this.weaponMain = cfg.bloodWeaponMain();
        this.shield = cfg.bloodShield();
    }

    @Override
    public boolean validate() {
        // run while boss is alive
        sleep(2_000);
        return BossHandler.bossIsAlive(bossName, bossStatusWidgetID);
    }

    @Override
    public State execute() {
        BossHandler.walkToBoss(bossName, bossLobbyLocation);
        sleep(1_000);
        BossHandler.fightPreparation(weaponMain, shield);
        sleep(1_000);
        BossHandler.enterBossArena(bossName, bossStatueObjectID, bossLobbyLocation);
        while (Rs2Player.distanceTo(bossArenaCenter) <= 20) {
            sleep(1_000);
            // TODO: add specific combat logic
        }
        return State.IDLE;
    }

}