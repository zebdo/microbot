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
import org.apache.commons.lang3.NotImplementedException;

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


    @Override
    public boolean click() {
        throw new NotImplementedException("click() not implemented yet for Rs2PlayerModel - player interactions are not well-defined in the current codebase");
    }

    @Override
    public boolean click(String action) {
        throw new NotImplementedException("click(String action) not implemented yet for Rs2PlayerModel - player interactions are not well-defined in the current codebase");
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
