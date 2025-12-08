package net.runelite.client.plugins.microbot.api.boat.models;

import net.runelite.api.WorldEntity;
import net.runelite.api.WorldEntityConfig;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.IEntity;

public class Rs2BoatModel implements WorldEntity, IEntity
{
    protected final WorldEntity worldEntity;

    public Rs2BoatModel(WorldEntity worldEntity)
    {
        this.worldEntity = worldEntity;
    }

    @Override
    public WorldView getWorldView()
    {
        return Microbot.getClientThread().runOnClientThreadOptional(worldEntity::getWorldView).orElse(null);
    }

    @Override
    public LocalPoint getCameraFocus()
    {
        return Microbot.getClientThread().runOnClientThreadOptional(worldEntity::getCameraFocus).orElse(null);
    }

    @Override
    public LocalPoint getLocalLocation()
    {
        return Microbot.getClientThread().runOnClientThreadOptional(worldEntity::getLocalLocation).orElse(null);
    }

    @Override
    public WorldPoint getWorldLocation()
    {
        return Microbot.getClientThread().runOnClientThreadOptional(() ->
        {
            WorldView worldView = worldEntity.getWorldView();
            LocalPoint localLocation = worldEntity.getLocalLocation();

            if (worldView == null || localLocation == null)
            {
                return null;
            }

            return WorldPoint.fromLocal(worldView, localLocation.getX(), localLocation.getY(), worldView.getPlane());
        }).orElse(null);
    }

    @Override
    public int getOrientation()
    {
        return Microbot.getClientThread().runOnClientThreadOptional(worldEntity::getOrientation).orElse(0);
    }

    @Override
    public LocalPoint getTargetLocation()
    {
        return Microbot.getClientThread().runOnClientThreadOptional(worldEntity::getTargetLocation).orElse(null);
    }

    @Override
    public int getTargetOrientation()
    {
        return Microbot.getClientThread().runOnClientThreadOptional(worldEntity::getTargetOrientation).orElse(0);
    }

    @Override
    public LocalPoint transformToMainWorld(LocalPoint point)
    {
        return Microbot.getClientThread().runOnClientThreadOptional(() -> worldEntity.transformToMainWorld(point)).orElse(null);
    }

    @Override
    public boolean isHiddenForOverlap()
    {
        return Microbot.getClientThread().runOnClientThreadOptional(worldEntity::isHiddenForOverlap).orElse(false);
    }

    @Override
    public WorldEntityConfig getConfig()
    {
        return Microbot.getClientThread().runOnClientThreadOptional(worldEntity::getConfig).orElse(null);
    }

    @Override
    public int getOwnerType()
    {
        return Microbot.getClientThread().runOnClientThreadOptional(worldEntity::getOwnerType).orElse(OWNER_TYPE_NOT_PLAYER);
    }

    @Override
    public int getId()
    {
        WorldEntityConfig config = getConfig();
        return config != null ? config.getId() : -1;
    }

    @Override
    public String getName()
    {
        return "WorldEntity";
    }

    @Override
    public boolean click()
    {
        return click("");
    }

    @Override
    public boolean click(String action)
    {
        return false;
    }
}