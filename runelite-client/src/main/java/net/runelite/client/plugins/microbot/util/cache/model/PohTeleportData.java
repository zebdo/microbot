package net.runelite.client.plugins.microbot.util.cache.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.runelite.client.plugins.microbot.util.poh.data.PohTransport;
import net.runelite.client.plugins.microbot.util.poh.data.PohTransportable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A data structure representing a set of teleportable data and associated transports within
 * a Player Owned House (POH) context. This class facilitates managing teleportable entities
 * and their corresponding transport logic.
 * <p>
 * It supports the association of teleportable entities (via the {@link PohTransportable}
 * interface) and their transport mechanisms (via the {@link PohTransport} class). The data
 * is organized by maintaining a list of teleportable entity names and their transport instances.
 * <p>
 * This class also allows the dynamic addition of new teleportable entities, ensuring no
 * duplicates are added. The primary purpose of the class is to act as a container for managing
 * POH-related teleport and transport operations.
 * <p>
 * Key features:
 * - Stores a list of teleportable entity names and transport objects.
 * - Dynamically adds teleportable entities and updates the corresponding transports.
 * - Retrieves a transportable entity by its name using a backing Enum class.
 * - Provides access to the transport list and transport-related metadata such as count
 * and existence of transports.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PohTeleportData implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * We keep the teleportableNames as they are a direct representation of the Original Enum's value.
     */
    private List<String> teleportableNames = new ArrayList<>();
    private List<PohTransport> transports = new ArrayList<>();

    private Class<? extends Enum> clazz;

    public PohTeleportData(Class<? extends Enum> originator, List<? extends PohTransportable> teleportables) {
        this.clazz = originator;

        if (teleportables != null) {
            for (PohTransportable teleportable : teleportables) {
                this.teleportableNames.add(teleportable.toString());
                this.transports.add(new PohTransport(teleportable));
            }
        }
    }

    public void addTransportable(String name) {
        PohTransportable obj = getTransportable(name);
        if (obj != null) {
            addTransportable(obj);
        }
    }

    public void addTransportable(PohTransportable teleportable) {
        if (this.teleportableNames.contains(teleportable.toString())) {
            return;
        }
        this.teleportableNames.add(teleportable.toString());
        this.transports.add(new PohTransport(teleportable));
    }

    public void removeTransportable(PohTransportable teleportable) {
        if (!this.teleportableNames.contains(teleportable.toString())) {
            return;
        }
        this.teleportableNames.remove(teleportable.toString());
        this.transports.removeIf(t -> t.getDisplayInfo().equals(teleportable.displayInfo()));
    }

    @SuppressWarnings("unchecked")
    private <T extends PohTransportable> T getTransportable(String name) {
        try {
            return (T) Enum.valueOf(clazz, name);
        } catch (Exception e) {
            return null;
        }
    }

    public List<PohTransport> getTransports() {
        return Collections.unmodifiableList(transports);
    }

    public boolean hasTransports() {
        return !transports.isEmpty();
    }

    public int getTransportCount() {
        return transports.size();
    }
}

