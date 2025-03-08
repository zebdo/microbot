package net.runelite.client.plugins.microbot.tempoross;

import net.runelite.api.NullObjectID;
import net.runelite.api.ObjectID;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.coords.Rs2WorldPoint;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;


public class TemporossWorkArea {
    public final WorldPoint exitNpc;
    public final WorldPoint safePoint;
    public final WorldPoint bucketPoint;
    public final WorldPoint pumpPoint;
    public final WorldPoint ropePoint;
    public final WorldPoint hammerPoint;
    public final WorldPoint harpoonPoint;
    public final WorldPoint mastPoint;
    public final WorldPoint totemPoint;
    public final WorldPoint rangePoint;
    public final WorldPoint spiritPoolPoint;

    public TemporossWorkArea(WorldPoint exitNpc, boolean isWest) {
        this.exitNpc = exitNpc;
        this.safePoint = exitNpc.dx(1).dy(1);

        if (isWest) {
            this.bucketPoint = exitNpc.dx(-3).dy(-1);
            this.pumpPoint = exitNpc.dx(-3).dy(-2);
            this.ropePoint = exitNpc.dx(-3).dy(-5);
            this.hammerPoint = exitNpc.dx(-3).dy(-6);
            this.harpoonPoint = exitNpc.dx(-2).dy(-7);
            this.mastPoint = exitNpc.dx(0).dy(-3);
            this.totemPoint = exitNpc.dx(8).dy(15);
            this.rangePoint = exitNpc.dx(3).dy(21);
            this.spiritPoolPoint = exitNpc.dx(11).dy(4);
        } else {
            this.bucketPoint = exitNpc.dx(3).dy(1);
            this.pumpPoint = exitNpc.dx(3).dy(2);
            this.ropePoint = exitNpc.dx(3).dy(5);
            this.hammerPoint = exitNpc.dx(3).dy(6);
            this.harpoonPoint = exitNpc.dx(2).dy(7);
            this.mastPoint = exitNpc.dx(0).dy(3);
            this.totemPoint = exitNpc.dx(-15).dy(-13);
            this.rangePoint = exitNpc.dx(-23).dy(-19);
            this.spiritPoolPoint = exitNpc.dx(-11).dy(-4);
        }
    }

    /**
     * Retrieves the bucket crate TileObject.
     *
     * <p>This method finds the bucket crate object in the game world using its object ID and the
     * instanced world point of the bucket point.</p>
     *
     * @return The TileObject representing the bucket crate, or null if not found.
     */
    public TileObject getBucketCrate() {
        // convert the bucketpoint to an instanced world point since the original bucket point is an already
        // converted worldpoint from an instance. The findObject uses the non instanced worldpoint to find the object
        // therefor we convert it back to it's original state.
        return Rs2GameObject.findObject(ObjectID.BUCKETS, Rs2WorldPoint.convertInstancedWorldPoint(bucketPoint));
    }

    /**
     * Retrieves the water pump TileObject.
     *
     * <p>This method finds the water pump object in the game world using its object ID and the
     * instanced world point of the pump point.</p>
     *
     * @return The TileObject representing the water pump, or null if not found.
     */
    public TileObject getPump() {
        return Rs2GameObject.findObject(ObjectID.WATER_PUMP_41000, Rs2WorldPoint.convertInstancedWorldPoint(pumpPoint));
    }

    /**
     * Retrieves the rope crate TileObject.
     *
     * <p>This method finds the rope crate object in the game world using its object ID and the
     * instanced world point of the rope point.</p>
     *
     * @return The TileObject representing the rope crate, or null if not found.
     */
    public TileObject getRopeCrate() {
        return Rs2GameObject.findObject(ObjectID.ROPES, Rs2WorldPoint.convertInstancedWorldPoint(ropePoint));
    }

    /**
     * Retrieves the hammer crate TileObject.
     *
     * <p>This method finds the hammer crate object in the game world using its object ID and the
     * instanced world point of the hammer point.</p>
     *
     * @return The TileObject representing the hammer crate, or null if not found.
     */
    public TileObject getHammerCrate() {
        return Rs2GameObject.findObject(ObjectID.HAMMERS_40964, Rs2WorldPoint.convertInstancedWorldPoint((hammerPoint)));
    }

    /**
     * Retrieves the harpoon crate TileObject.
     *
     * <p>This method finds the harpoon crate object in the game world using its object ID and the
     * instanced world point of the harpoon point.</p>
     *
     * @return The TileObject representing the harpoon crate, or null if not found.
     */
    public TileObject getHarpoonCrate() {
        return Rs2GameObject.findObject(ObjectID.HARPOONS, Rs2WorldPoint.convertInstancedWorldPoint(harpoonPoint));
    }

    /**
     * Retrieves the mast TileObject.
     *
     * <p>This method finds the mast object in the game world using its object ID and the
     * instanced world point of the mast point. It checks for specific null object IDs to identify the mast.</p>
     *
     * @return The TileObject representing the mast, or null if not found.
     */
    public TileObject getMast() {
        TileObject mast = Rs2GameObject.findGameObjectByLocation(Rs2WorldPoint.convertInstancedWorldPoint(mastPoint));
        if (mast != null && (mast.getId() == NullObjectID.NULL_41352 || mast.getId() == NullObjectID.NULL_41353)) {
            return mast;
        }
        return null;
    }

    /**
     * Retrieves the broken mast TileObject.
     *
     * <p>This method finds the broken mast object in the game world using its object ID and the
     * instanced world point of the mast point. It checks for specific damaged mast object IDs to identify the broken mast.</p>
     *
     * @return The TileObject representing the broken mast, or null if not found.
     */
    public TileObject getBrokenMast() {
        TileObject mast = Rs2GameObject.findGameObjectByLocation(Rs2WorldPoint.convertInstancedWorldPoint(mastPoint));
        if (mast != null && (mast.getId() == ObjectID.DAMAGED_MAST_40996 || mast.getId() == ObjectID.DAMAGED_MAST_40997))
            return mast;

        return null;
    }

    /**
     * Retrieves the totem TileObject.
     *
     * <p>This method finds the totem object in the game world using its object ID and the
     * instanced world point of the totem point. It checks for specific null object IDs to identify the totem.</p>
     *
     * @return The TileObject representing the totem, or null if not found.
     */
    public TileObject getTotem() {
        TileObject totem = Rs2GameObject.findGameObjectByLocation(Rs2WorldPoint.convertInstancedWorldPoint(totemPoint));
        if (totem != null && (totem.getId() == NullObjectID.NULL_41355 || totem.getId() == NullObjectID.NULL_41354)) {
            return totem;
        }
        return null;
    }

    /**
     * Retrieves the broken totem TileObject.
     *
     * <p>This method finds the broken totem object in the game world using its object ID and the
     * instanced world point of the totem point. It checks for specific damaged totem object IDs to identify the broken totem.</p>
     *
     * @return The TileObject representing the broken totem, or null if not found.
     */
    public TileObject getBrokenTotem() {
        TileObject totem = Rs2GameObject.findGameObjectByLocation(Rs2WorldPoint.convertInstancedWorldPoint(totemPoint));
        if (totem != null && (totem.getId() == ObjectID.DAMAGED_TOTEM_POLE || totem.getId() == ObjectID.DAMAGED_TOTEM_POLE_41011))
            return totem;

        return null;
    }

    /**
     * Retrieves the range TileObject.
     *
     * <p>This method finds the range object in the game world using its object ID and the
     * instanced world point of the range point.</p>
     *
     * @return The TileObject representing the range, or null if not found.
     */
    public TileObject getRange() {
        return Rs2GameObject.findObject(ObjectID.SHRINE_41236, Rs2WorldPoint.convertInstancedWorldPoint(rangePoint));
    }

    public TileObject getClosestTether() {
        TileObject mast = getMast();
        TileObject totem = getTotem();

        if (mast == null) {
            return totem;
        }

        if (totem == null) {
            return mast;
        }

        Rs2WorldPoint mastLocation = new Rs2WorldPoint(mast.getWorldLocation());
        Rs2WorldPoint totemLocation = new Rs2WorldPoint(totem.getWorldLocation());
        Rs2WorldPoint playerLocation = new Rs2WorldPoint(Microbot.getClient().getLocalPlayer().getWorldLocation());

        return mastLocation.distanceToPath(playerLocation.getWorldPoint()) <
                totemLocation.distanceToPath(playerLocation.getWorldPoint()) ? mast : totem;
    }

    public String getAllPointsAsString() {
        String sb = "exitNpc=" + exitNpc +
                ", safePoint=" + safePoint +
                ", bucketPoint=" + bucketPoint +
                ", pumpPoint=" + pumpPoint +
                ", ropePoint=" + ropePoint +
                ", hammerPoint=" + hammerPoint +
                ", harpoonPoint=" + harpoonPoint +
                ", mastPoint=" + mastPoint +
                ", totemPoint=" + totemPoint +
                ", rangePoint=" + rangePoint +
                ", spiritPoolPoint=" + spiritPoolPoint;

        return sb;
    }
}
