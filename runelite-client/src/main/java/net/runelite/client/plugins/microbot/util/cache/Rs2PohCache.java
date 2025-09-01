package net.runelite.client.plugins.microbot.util.cache;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.microbot.util.cache.model.PohTeleportData;
import net.runelite.client.plugins.microbot.util.cache.serialization.CacheSerializable;
import net.runelite.client.plugins.microbot.util.cache.util.LogOutputMode;
import net.runelite.client.plugins.microbot.util.cache.util.Rs2CacheLoggingUtils;
import net.runelite.client.plugins.microbot.util.poh.data.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class Rs2PohCache extends Rs2Cache<String, PohTeleportData> implements CacheSerializable {
    public static final String POH_CACHE_KEY = "PohCache";

    // Cache keys for different POH elements
    public static final String KEY_PORTALS = "portals";
    public static final String KEY_NEXUS = "nexus";
    public static final String KEY_MOUNTED_GLORY = "mountedGlory";
    public static final String KEY_MOUNTED_DIGSITE = "mountedDigsite";
    public static final String KEY_MOUNTED_XERICS = "mountedXerics";
    public static final String KEY_MOUNTED_MYTHS = "mountedMyths";
    public static final String KEY_JEWELLERY_BOX = "jewelleryBox";
    public static final String KEY_FAIRY_RING = "fairyRing";
    public static final String KEY_WILDY_OBELISK = "wildyObelisk";
    public static final String KEY_SPIRIT_TREE = "spiritTree";

    private static Rs2PohCache instance;

    private Rs2PohCache() {
        super(POH_CACHE_KEY, CacheMode.EVENT_DRIVEN_ONLY);
        this.withPersistence(POH_CACHE_KEY);
    }

    public static synchronized Rs2PohCache getInstance() {
        if (instance == null) {
            instance = new Rs2PohCache();
        }
        return instance;
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event) {
        if (Arrays.stream(NexusPortal.VARBITS).anyMatch(v -> v == event.getVarpId())) {
            updateNexus();
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        if (!Rs2Cache.isInPOH() || event.getGameState() != GameState.LOGGED_IN) {
            return;
        }
        //At this point teleport objects still aren't loaded
        //But we can update the nexus portal's teleports
        updateNexus();
    }

    @Subscribe
    public void onWidgetClosed(WidgetClosed event) {
        if (event.getGroupId() == InterfaceID.POH_LOADING) {
            //At this point teleport objects are not yet loaded
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event) {
        GameObject go = event.getGameObject();
        PohPortal portal = PohPortal.getPohPortal(go);
        if (portal != null) {
            addPortal(portal);
            log.debug("Portal spawned: {}, we now have {} portals", portal, this.get(KEY_PORTALS).getTransportCount());
            return;
        }
        JewelleryBoxType jewelleryBoxType = JewelleryBoxType.getJewelleryBoxType(go);
        if (jewelleryBoxType != null) {
            setJewelleryBox(jewelleryBoxType);
            log.debug("JewelleryBox spawned: {}, we now have {} JewelleryTeleports", jewelleryBoxType, this.get(KEY_JEWELLERY_BOX).getTransportCount());
        }
        if (MountedDigsite.isMountedDigsite(go)) {
            setMountedDigsite(true);
            log.debug("Mounted digsite spawned, we now have {} DigsiteTeleports", this.get(KEY_MOUNTED_DIGSITE).getTransportCount());
        } else if (MountedGlory.isMountedGlory(go)) {
            setMountedGlory(true);
            log.debug("Mounted Glory spawned, we now have {} GloryTeleports", this.get(KEY_MOUNTED_GLORY).getTransportCount());
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event) {
        GameObject go = event.getGameObject();
        PohPortal portal = PohPortal.getPohPortal(go);
        if (portal != null) {
            removePortal(portal);
            log.debug("Portal despawned: {}, we now have {} portals", portal, this.get(KEY_PORTALS).getTransportCount());
            return;
        }
        JewelleryBoxType jewelleryBoxType = JewelleryBoxType.getJewelleryBoxType(go);
        if (jewelleryBoxType != null) {
            setJewelleryBox(JewelleryBoxType.NONE);
            log.debug("JewelleryBox despawned: {}, we now have {} JewelleryTeleports", jewelleryBoxType, this.get(KEY_JEWELLERY_BOX).getTransportCount());
        }
        if (MountedDigsite.isMountedDigsite(go)) {
            setMountedDigsite(false);
            log.debug("Mounted digsite despawned");
        } else if (MountedGlory.isMountedGlory(go)) {
            setMountedGlory(false);
            log.debug("Mounted Glory despawned");
        }
    }

    @Override
    public void update() {
        //Unused
    }

    private void removePortal(PohPortal portal) {
        if (!containsKey(KEY_PORTALS)) return;
        get(KEY_PORTALS).removeTransportable(portal);
    }

    private void addPortal(PohPortal portal) {
        if (containsKey(KEY_PORTALS)) {
            get(KEY_PORTALS).addTransportable(portal);
        } else {
            put(KEY_PORTALS, new PohTeleportData(PohPortal.class, Collections.singletonList(portal)));
        }
    }

    private void setJewelleryBox(JewelleryBoxType jewelleryBoxType) {
        put(KEY_JEWELLERY_BOX, new PohTeleportData(JewelleryBox.class, jewelleryBoxType.getAvailableTeleports()));
    }

    private void setMountedGlory(boolean enabled) {
        if (enabled) {
            put(KEY_MOUNTED_GLORY, new PohTeleportData(MountedGlory.class, MountedGlory.getTransports()));
        } else {
            remove(KEY_MOUNTED_GLORY);
        }
    }

    private void setMountedDigsite(boolean enabled) {
        if (enabled) {
            put(KEY_MOUNTED_DIGSITE, new PohTeleportData(MountedGlory.class, MountedGlory.getTransports()));
        } else {
            remove(KEY_MOUNTED_GLORY);
        }
    }

    private void updateNexus() {
        List<NexusPortal> availableNexus = NexusPortal.getAvailableTeleports();
        PohTeleportData nexusData = new PohTeleportData(NexusPortal.class, availableNexus);
        log.debug("Updating {} with {}: {} available portals", KEY_NEXUS, availableNexus.size(), nexusData.getTransportCount());
        put(KEY_NEXUS, nexusData);
    }


    @Override
    public String getConfigKey() {
        return POH_CACHE_KEY;
    }

    @Override
    public boolean shouldPersist() {
        return true;
    }

    public static List<PohTransport> getAllAvailableTransports() {
        return getAllTeleportData().flatMap(data -> data.getTransports().stream())
                .collect(Collectors.toList());

    }

    public static Stream<PohTeleportData> getAllTeleportData() {
        return getInstance().values().stream();
    }

    public static void logState(LogOutputMode mode) {
        Rs2PohCache cache = getInstance();
        StringBuilder logContent = new StringBuilder();

        logContent.append("=== POH Cache States ===\n");
        logContent.append(String.format("%-20s %-8s %-50s\n",
                "Cache Key", "Count", "Available Teleports"));
        logContent.append("-".repeat(80)).append("\n");

        cache.keyStream().forEach(key -> {
            PohTeleportData data = cache.get(key);

            if (data != null && data.hasTransports()) {
                String teleportsList = String.join(", ", data.getTeleportableNames());

                // Truncate if too long for display
                if (teleportsList.length() > 47) {
                    teleportsList = teleportsList.substring(0, 44) + "...";
                }

                logContent.append(String.format("%-20s %-8d %-50s\n",
                        key,
                        data.getTransportCount(),
                        teleportsList
                ));
            } else {
                logContent.append(String.format("%-20s %-8s %-50s\n",
                        key,
                        "0",
                        "None available"
                ));
            }
        });


        // Summary statistics
        logContent.append("-".repeat(80)).append("\n");
        int totalTransports = cache.values().stream()
                .mapToInt(data -> data != null ? data.getTransportCount() : 0)
                .sum();

        long categoriesWithTransports = cache.values().stream()
                .filter(data -> data != null && data.hasTransports())
                .count();

        logContent.append(String.format("Total Categories: %d | Active Categories: %d | Total Transports: %d\n",
                cache.keyStream().count(), categoriesWithTransports, totalTransports));

        logContent.append("=== End POH Cache States ===\n");

        Rs2CacheLoggingUtils.outputCacheLog(
                POH_CACHE_KEY,
                logContent.toString(),
                mode
        );
    }
}

