package net.runelite.client.plugins.microbot.util.cache;

import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.events.*;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.util.cache.serialization.CacheSerializable;
import net.runelite.client.plugins.microbot.util.cache.util.LogOutputMode;
import net.runelite.client.plugins.microbot.util.cache.util.Rs2CacheLoggingUtils;
import net.runelite.client.plugins.microbot.util.poh.PohTeleports;
import net.runelite.client.plugins.microbot.util.poh.PohTransport;
import net.runelite.client.plugins.microbot.util.poh.data.*;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class Rs2PohCache extends Rs2Cache<String, List<PohTeleport>> implements CacheSerializable {

    public final static Type TYPE_TOKEN = new TypeToken<Map<String, List<PohTeleport>>>() {
    }.getType();
    public static final String POH_CACHE_KEY = "PohCache";

    private static Rs2PohCache instance;

    private Rs2PohCache() {
        super(POH_CACHE_KEY, CacheMode.MANUAL_ONLY);
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
            setTeleports(NexusPortal.class, NexusPortal.getAvailableTeleports());
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        if (!Rs2Cache.isInPOH() || event.getGameState() != GameState.LOGGED_IN) {
            return;
        }
        //At this point teleport objects still aren't loaded
        //But we can update the nexus portal's teleports
        setTeleports(NexusPortal.class, NexusPortal.getAvailableTeleports());
    }

    @Subscribe
    public void onDecorativeObjectSpawned(DecorativeObjectSpawned event) {
        DecorativeObject go = event.getDecorativeObject();
        if (go == null) return;
        if (MountedMythical.isMountedMythsCape(go)) {
            setTeleports(MountedMythical.class, List.of(MountedMythical.values()));
        } else if (MountedXerics.isMountedXerics(go)) {
            setTeleports(MountedXerics.class, List.of(MountedXerics.values()));
        } else if (MountedDigsite.isMountedDigsite(go)) {
            setTeleports(MountedDigsite.class, List.of(MountedDigsite.values()));
        } else if (MountedGlory.isMountedGlory(go)) {
            setTeleports(MountedGlory.class, List.of(MountedGlory.values()));
        }
    }

    @Subscribe
    public void onDecorativeObjectDespawned(DecorativeObjectDespawned event) {
        DecorativeObject go = event.getDecorativeObject();
        if (go == null) return;
        if (MountedMythical.isMountedMythsCape(go)) {
            remove(MountedMythical.class.getSimpleName());
        } else if (MountedXerics.isMountedXerics(go)) {
            remove(MountedXerics.class.getSimpleName());
        } else if (MountedDigsite.isMountedDigsite(go)) {
            remove(MountedDigsite.class.getSimpleName());
        } else if (MountedGlory.isMountedGlory(go)) {
            remove(MountedGlory.class.getSimpleName());
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event) {
        //TODO("Make sure we only add transport's when we're inside our own home.")
        GameObject go = event.getGameObject();
        if (go == null) return;
        PohPortal portal = PohPortal.getPohPortal(go);
        if (portal != null) {
            addTeleport(PohPortal.class, portal);
            return;
        }
        JewelleryBoxType jewelleryBoxType = JewelleryBoxType.getJewelleryBoxType(go);
        if (jewelleryBoxType != null) {
            setTeleports(JewelleryBox.class, jewelleryBoxType.getAvailableTeleports());
        } else if (NexusPortal.isNexusPortal(go)) {
            setTeleports(NexusPortal.class, NexusPortal.getAvailableTeleports());
        } else if (PohTeleports.isFairyRing(go)) {
            log.info("Found fairy rings in POH");
            put("fairyRings", Collections.emptyList());
        }
    }


    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event) {
        GameObject go = event.getGameObject();
        if (go == null) return;
        PohPortal portal = PohPortal.getPohPortal(go);
        if (portal != null) {
            removeTeleport(PohPortal.class, portal);
            return;
        }
        JewelleryBoxType jewelleryBoxType = JewelleryBoxType.getJewelleryBoxType(go);
        if (jewelleryBoxType != null) {
            remove(JewelleryBox.class.getSimpleName());
        } else if (NexusPortal.isNexusPortal(go)) {
            remove(NexusPortal.class.getSimpleName());
        } else if (PohTeleports.isFairyRing(go)) {
            log.info("Removing Fairy Rings from POH");
            remove("fairyRings");
        }
    }

    private void removeTeleport(Class<? extends Enum<? extends PohTeleport>> type, PohTeleport teleport) {
        String key = type.getSimpleName();
        List<PohTeleport> teleports = get(key, ArrayList::new);
        if (!teleports.contains(teleport)) {
            return;
        }
        teleports.remove(teleport);
        put(key, teleports);
        log.info("Removing {} Teleport from {}", teleport.name(), key);
    }

    private void addTeleport(Class<? extends Enum<? extends PohTeleport>> type, PohTeleport teleport) {
        String key = type.getSimpleName();
        List<PohTeleport> teleports = get(key, ArrayList::new);
        if (teleports.contains(teleport)) {
            return;
        }
        teleports.add(teleport);
        put(key, teleports);
        log.info("Adding {} Teleport to {}", teleport.name(), key);
    }

    private void setTeleports(Class<? extends Enum<? extends PohTeleport>> type, List<? extends PohTeleport> teleports) {
        String key = type.getSimpleName();
        put(key, Collections.unmodifiableList(teleports));
        log.info("Putting {} Teleports to {}", get(key).size(), key);
    }

    @Override
    public void update() {
        //Unused
    }

    @Override
    public String getConfigKey() {
        return POH_CACHE_KEY;
    }

    @Override
    public boolean shouldPersist() {
        return true;
    }

    public static boolean isTransportUsable(Transport transport) {
        if (transport == null) return false;
        if (transport.getType() == TransportType.FAIRY_RING) {
            //For fairy ring we only have to check if the cache contains the key
            return getInstance().containsKey("fairyRings");
        }
        // For anything else we have to see if the PohTeleport exists in the cache
        if (!(transport instanceof PohTransport)) return false;

        PohTransport pohTransport = (PohTransport) transport;
        PohTeleport teleport = pohTransport.getTeleport();
        String key = teleport.getClass().getSimpleName();
        return getInstance().containsKey(key);
    }

    public static List<PohTransport> getAvailableTransports() {
        return getInstance().values().stream()
                .flatMap(Collection::stream).map(PohTransport::new)
                .collect(Collectors.toList());
    }


    public static void logState(LogOutputMode mode) {
        Rs2PohCache cache = getInstance();
        StringBuilder logContent = new StringBuilder();

        logContent.append("=== POH Cache States ===\n");
        logContent.append(String.format("%-20s %-8s %-50s\n",
                "Cache Key", "Count", "Available Teleports"));
        logContent.append("-".repeat(80)).append("\n");

        cache.keyStream().forEach(key -> {
            List<PohTeleport> tt = cache.get(key);
            logContent.append(String.format("%-20s %-8s \n",
                    key,
                    tt.size(),
                    tt.stream().map(PohTeleport::name).reduce("", (a, b) -> a + ", " + b)
            ));
        });


        // Summary statistics
        logContent.append("-".repeat(80)).append("\n");

        logContent.append(String.format("Total Teleports: %d \n",
                cache.keyStream().count()));

        logContent.append("=== End POH Cache States ===\n");

        Rs2CacheLoggingUtils.outputCacheLog(
                POH_CACHE_KEY,
                logContent.toString(),
                mode
        );
    }

}

