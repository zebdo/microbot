package net.runelite.client.plugins.microbot.api.player.models;

import java.awt.Polygon;
import lombok.Getter;
import net.runelite.api.HeadIcon;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.AnimationID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.IEntity;
import net.runelite.client.plugins.microbot.api.boat.Rs2Boat;
import net.runelite.client.plugins.microbot.util.ActorModel;
import org.apache.commons.lang3.NotImplementedException;

@Getter
public class Rs2PlayerModel extends ActorModel implements Player, IEntity {

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
        if (Rs2Boat.isOnBoat()) {
            return Rs2Boat.getPlayerBoatLocation();
        } else {
            return super.getWorldLocation();
        }
    }

    @Override
    public int getId()
    {
        return player.getId();
    }

    @Override
    public PlayerComposition getPlayerComposition()
    {
        return player.getPlayerComposition();
    }

    @Override
    public Polygon[] getPolygons()
    {
        return player.getPolygons();
    }

    @Override
    public int getTeam()
    {
        return player.getTeam();
    }

    @Override
    public boolean isFriendsChatMember()
    {
        return player.isFriendsChatMember();
    }

    @Override
    public boolean isFriend()
    {
        return player.isFriend();
    }

    @Override
    public boolean isClanMember()
    {
        return player.isClanMember();
    }

    @Override
    public HeadIcon getOverheadIcon()
    {
        return player.getOverheadIcon();
    }

    @Override
    public int getSkullIcon()
    {
        return player.getSkullIcon();
    }

    @Override
    public void setSkullIcon(int skullIcon)
    {
        player.setSkullIcon(skullIcon);
    }

    @Override
    public int getFootprintSize()
    {
        return 0;
    }

    @Override
    public boolean click() {
        throw new NotImplementedException("click() not implemented yet for Rs2PlayerModel - player interactions are not well-defined in the current codebase");
    }

    @Override
    public boolean click(String action) {
        throw new NotImplementedException("click(String action) not implemented yet for Rs2PlayerModel - player interactions are not well-defined in the current codebase");
    }


    //TODO: We need a method that will search the name of the animation in the list of ids
    // example: "salvage" as animation name and compare it against the player.getAnimation() id

/*    public boolean isSalvaging() {
        return player.getAnimation() ==
    }*/
}
