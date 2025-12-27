package net.runelite.client.plugins.microbot.api.player.models;

import java.awt.Polygon;
import lombok.Getter;
import net.runelite.api.HeadIcon;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.IEntity;
import net.runelite.client.plugins.microbot.api.actor.Rs2ActorModel;
import net.runelite.client.plugins.microbot.api.boat.Rs2BoatCache;
import net.runelite.client.plugins.microbot.api.player.data.SalvagingAnimations;
import net.runelite.client.plugins.microbot.util.ActorModel;

@Getter
public class Rs2PlayerModel extends Rs2ActorModel implements IEntity {

    private final Player player;

    public Rs2PlayerModel()
    {
        super(Microbot.getClient().getLocalPlayer());
        this.player = Microbot.getClient().getLocalPlayer();
    }

    public Rs2PlayerModel(final Player player)
    {
        super(player);
        this.player = player;
    }

    @Override
    public WorldPoint getWorldLocation()
    {
        return super.getWorldLocation();
    }

    @Override
    public int getId()
    {
        return player.getId();
    }


    /**
     * Player interactions are not currently implemented.
     * Player-to-player interactions in RuneScape are complex and context-dependent
     * (PvP, trading, following, etc.) and require careful handling to avoid misuse.
     *
     * @return false (not implemented)
     */
    @Override
    public boolean click() {
        return false;
    }

    /**
     * Player interactions are not currently implemented.
     * Player-to-player interactions in RuneScape are complex and context-dependent
     * (PvP, trading, following, etc.) and require careful handling to avoid misuse.
     *
     * For PvP interactions, consider using Rs2Combat utilities directly.
     * For trading, use Rs2Trade utilities (if available).
     *
     * @param action the intended action (not implemented)
     * @return false (not implemented)
     */
    @Override
    public boolean click(String action) {
        return false;
    }

    // Sailing stuff
    public  boolean isSalvaging() {
        int anim = new Rs2PlayerModel().getAnimation();
        for (int id : SalvagingAnimations.SALVAGING_ANIMATIONS) {
            if (anim == id) {
                return true;
            }
        }
        return false;
    }
}
