package net.runelite.client.plugins.microbot.moonsOfPeril.handlers;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.moonsOfPeril.enums.GameObjects;
import net.runelite.client.plugins.microbot.moonsOfPeril.enums.Locations;
import net.runelite.client.plugins.microbot.moonsOfPeril.enums.State;
import net.runelite.client.plugins.microbot.moonsOfPeril.enums.Widgets;
import net.runelite.client.plugins.microbot.moonsOfPeril.moonsOfPerilConfig;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;

import static net.runelite.client.plugins.microbot.util.Global.sleep;

public class BlueMoonHandler implements BaseHandler {

    private static final String bossName = "Blue Moon";
    private static final int bossStatusWidgetID = Widgets.BLUE_MOON_ID.getID();
    private static final int bossStatueObjectID = GameObjects.BLUE_MOON_STATUE_ID.getID();
    private static final WorldPoint bossLobbyLocation = Locations.BLUE_LOBBY.getWorldPoint();
    private static final WorldPoint bossArenaCenter = Locations.BLUE_ARENA_CENTER.getWorldPoint();
    private final int sigilNpcID = GameObjects.SIGIL_NPC_ID.getID();
    private final int bossNpcID = GameObjects.BLUE_MOON_NPC_ID.getID();
    private String weaponMain;
    private String shield;

    public BlueMoonHandler(moonsOfPerilConfig cfg) {
        this.weaponMain = cfg.blueWeaponMain();
        this.shield = cfg.blueShield();
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
            if (BossHandler.isNormalAttackSequence(sigilNpcID)) {
                BossHandler.normalAttackSequence(sigilNpcID, bossNpcID);
            }
            sleep(600); // ~2 in-game ticks
        }
        Rs2Prayer.disableAllPrayers();
        return State.IDLE;
        // PLAYER SHOULD NOW BE INSIDE THE ARENA
        // TODO: add specific combat logic
    }

}
