package net.runelite.client.plugins.microbot.moonsOfPeril;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameObject;
import net.runelite.api.GraphicsObject;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.tile.Rs2Tile;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = PluginDescriptor.Default + "Moons of Peril Beta",
        description = "A plugin to defeat the Perilous Moons bosses",
        tags = {"bossing", "microbot"},
        enabledByDefault = false
)
@Slf4j
public class moonsOfPerilPlugin extends Plugin {
    @Inject
    private moonsOfPerilConfig config;
    @Provides
    moonsOfPerilConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(moonsOfPerilConfig.class);
    }

    @Inject
    private OverlayManager overlayManager;
    @Inject
    private moonsOfPerilOverlay moonsOfPerilOverlay;

    @Inject
    moonsOfPerilScript moonsOfPerilScript;


    @Override
    protected void startUp() throws AWTException {
        if (overlayManager != null) {
            overlayManager.add(moonsOfPerilOverlay);
        }
        moonsOfPerilScript.run(config);
        Rs2Tile.init();
    }

    @Subscribe
    public void onGraphicsObjectCreated(GraphicsObjectCreated event) {
        final GraphicsObject iceShard = event.getGraphicsObject();
        if (iceShard.getId() == SpotanimID.VFX_DJINN_ICE_FLOOR_SPAWN_01) {
            Microbot.log("[EVENT] GraphicsObjectCreated id=" + iceShard.getId());
            Rs2Tile.addDangerousGraphicsObjectTile(iceShard, 600 * 3);
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event) {
        final GameObject bloodPool = event.getGameObject();
        if (bloodPool.getId() == SpotanimID.VFX_DJINN_ICE_FLOOR_SPAWN_01) {
            Microbot.log("[EVENT] GameObjectCreated id=" + bloodPool.getId());
            Rs2Tile.addDangerousGameObjectTile(bloodPool, 600 * 3);
        }
    }

/*    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event) {
        final GameObject bloodPool = event.getGameObject();
        if (bloodPool.getId() == ObjectID.PMOON_BOSS_BLOOD_POOL) {
            Rs2Tile.addDangerousGameObjectTile(bloodPool, 600 * 3);
        }*/

    protected void shutDown() {
        moonsOfPerilScript.shutdown();
        overlayManager.remove(moonsOfPerilOverlay);
    }
    int ticks = 10;
    @Subscribe
    public void onGameTick(GameTick tick)
    {
        //System.out.println(getName().chars().mapToObj(i -> (char)(i + 3)).map(String::valueOf).collect(Collectors.joining()));

        if (ticks > 0) {
            ticks--;
        } else {
            ticks = 10;
        }

    }

}
