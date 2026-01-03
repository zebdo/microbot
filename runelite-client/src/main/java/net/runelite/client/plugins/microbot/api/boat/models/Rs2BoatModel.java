package net.runelite.client.plugins.microbot.api.boat.models;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.IEntity;
import net.runelite.client.plugins.microbot.api.boat.Rs2BoatCache;
import net.runelite.client.plugins.microbot.api.boat.data.BoatType;
import net.runelite.client.plugins.microbot.api.boat.data.Heading;
import net.runelite.client.plugins.microbot.api.boat.data.PortTaskData;
import net.runelite.client.plugins.microbot.api.boat.data.PortTaskVarbits;
import net.runelite.client.plugins.microbot.api.player.models.Rs2PlayerModel;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.shortestpath.WorldPointUtil;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.menu.NewMenuEntry;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static net.runelite.api.gameval.AnimationID.*;
import static net.runelite.api.gameval.ObjectID1.*;
import static net.runelite.client.plugins.microbot.util.Global.sleep;
import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

@Slf4j
public class Rs2BoatModel implements WorldEntity, IEntity {

    protected final WorldEntity boat;


    public Rs2BoatModel(WorldEntity boat)
    {
        this.boat = boat;
    }

	// Temp fix for disembark plank ids
	public  final int[] GANGPLANK_DISEMBARK = {
		SAILING_GANGPLANK_PORT_SARIM,
		SAILING_GANGPLANK_THE_PANDEMONIUM,
		SAILING_GANGPLANK_LANDS_END,
		SAILING_GANGPLANK_MUSA_POINT,
		SAILING_GANGPLANK_HOSIDIUS,
		SAILING_GANGPLANK_RIMMINGTON,
		SAILING_GANGPLANK_CATHERBY,
		SAILING_GANGPLANK_PORT_PISCARILIUS,
		SAILING_GANGPLANK_BRIMHAVEN,
		SAILING_GANGPLANK_ARDOUGNE,
		SAILING_GANGPLANK_PORT_KHAZARD,
		SAILING_GANGPLANK_WITCHAVEN,
		SAILING_GANGPLANK_ENTRANA,
		SAILING_GANGPLANK_CIVITAS_ILLA_FORTIS,
		SAILING_GANGPLANK_CORSAIR_COVE,
		SAILING_GANGPLANK_CAIRN_ISLE,
		SAILING_GANGPLANK_SUNSET_COAST,
		SAILING_GANGPLANK_THE_SUMMER_SHORE,
		SAILING_GANGPLANK_ALDARIN,
		SAILING_GANGPLANK_RUINS_OF_UNKAH,
		SAILING_GANGPLANK_VOID_KNIGHTS_OUTPOST,
		SAILING_GANGPLANK_PORT_ROBERTS,
		SAILING_GANGPLANK_RED_ROCK,
		SAILING_GANGPLANK_RELLEKKA,
		SAILING_GANGPLANK_ETCETERIA,
		SAILING_GANGPLANK_PORT_TYRAS,
		SAILING_GANGPLANK_DEEPFIN_POINT,
		SAILING_GANGPLANK_JATIZSO,
		SAILING_GANGPLANK_NEITIZNOT,
		SAILING_GANGPLANK_PRIFDDINAS,
		SAILING_GANGPLANK_PISCATORIS,
		SAILING_GANGPLANK_LUNAR_ISLE,
		SAILING_MOORING_ISLE_OF_SOULS,
		SAILING_MOORING_WATERBIRTH_ISLAND,
		SAILING_MOORING_WEISS,
		SAILING_MOORING_DOGNOSE_ISLAND,
		SAILING_MOORING_REMOTE_ISLAND,
		SAILING_MOORING_THE_LITTLE_PEARL,
		SAILING_MOORING_THE_ONYX_CREST,
		SAILING_MOORING_LAST_LIGHT,
		SAILING_MOORING_CHARRED_ISLAND,
		SAILING_MOORING_VATRACHOS_ISLAND,
		SAILING_MOORING_ANGLERS_RETREAT,
		SAILING_MOORING_MINOTAURS_REST,
		SAILING_MOORING_ISLE_OF_BONES,
		SAILING_MOORING_TEAR_OF_THE_SOUL,
		SAILING_MOORING_WINTUMBER_ISLAND,
		SAILING_MOORING_THE_CROWN_JEWEL,
		SAILING_MOORING_RAINBOWS_END,
		SAILING_MOORING_SUNBLEAK_ISLAND,
		SAILING_MOORING_SHIMMERING_ATOLL,
		SAILING_MOORING_LAGUNA_AURORAE,
		SAILING_MOORING_CHINCHOMPA_ISLAND,
		SAILING_MOORING_LLEDRITH_ISLAND,
		SAILING_MOORING_YNYSDAIL,
		SAILING_MOORING_BUCCANEERS_HAVEN,
		SAILING_MOORING_DRUMSTICK_ISLE,
		SAILING_MOORING_BRITTLE_ISLE,
		SAILING_MOORING_GRIMSTONE
	};

    @Getter
	private  Heading currentHeading = Heading.SOUTH;


    @Override
    public WorldView getWorldView()
    {
        return Microbot.getClientThread().runOnClientThreadOptional(boat::getWorldView).orElse(null);
    }

    @Override
    public LocalPoint getCameraFocus()
    {
        return Microbot.getClientThread().runOnClientThreadOptional(boat::getCameraFocus).orElse(null);
    }

    @Override
    public LocalPoint getLocalLocation()
    {
        return Microbot.getClientThread().runOnClientThreadOptional(boat::getLocalLocation).orElse(null);
    }

    @Override
    public WorldPoint getWorldLocation()
    {
        return Microbot.getClientThread().runOnClientThreadOptional(() ->
        {
            WorldView worldView = boat.getWorldView();
            LocalPoint localLocation = boat.getLocalLocation();

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
        return Microbot.getClientThread().runOnClientThreadOptional(boat::getOrientation).orElse(0);
    }

    @Override
    public LocalPoint getTargetLocation()
    {
        return Microbot.getClientThread().runOnClientThreadOptional(boat::getTargetLocation).orElse(null);
    }

    @Override
    public int getTargetOrientation()
    {
        return Microbot.getClientThread().runOnClientThreadOptional(boat::getTargetOrientation).orElse(0);
    }

    @Override
    public LocalPoint transformToMainWorld(LocalPoint point)
    {
        return Microbot.getClientThread().runOnClientThreadOptional(() -> boat.transformToMainWorld(point)).orElse(null);
    }

    @Override
    public boolean isHiddenForOverlap()
    {
        return Microbot.getClientThread().runOnClientThreadOptional(boat::isHiddenForOverlap).orElse(false);
    }

    @Override
    public WorldEntityConfig getConfig()
    {
        return Microbot.getClientThread().runOnClientThreadOptional(boat::getConfig).orElse(null);
    }

    @Override
    public int getOwnerType()
    {
        return Microbot.getClientThread().runOnClientThreadOptional(boat::getOwnerType).orElse(OWNER_TYPE_NOT_PLAYER);
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

    public BoatType getBoatType()
    {
        if (Microbot.getVarbitPlayerValue(VarPlayerID.SAILING_SIDEPANEL_BOAT_TYPE) == 8110)
        {
            return BoatType.RAFT;
        }
        return BoatType.RAFT;
    }

    public  int getSteeringForBoatType()
    {
        switch (getBoatType())
        {
            case RAFT:
                return SAILING_BOAT_STEERING_KANDARIN_1X3_WOOD;
            case SKIFF:
                // Return SKIFF steering object ID
            case SLOOP:
                // Return SLOOP steering object ID
            default:
                return SAILING_BOAT_STEERING_KANDARIN_1X3_WOOD;
        }
    }

    public  boolean isOnBoat()
    {
        return boat != null;
    }

    public  boolean isNavigating()
    {
        return Microbot.getVarbitValue(VarbitID.SAILING_SIDEPANEL_PLAYER_AT_HELM) == 1;
    }

    public  boolean navigate()
    {
        if (!isOnBoat())
        {
            return false;
        }

        if (isNavigating())
        {
            return true;
        }

        Rs2GameObject.interact(getSteeringForBoatType(), "Navigate");
        sleepUntil(() -> isNavigating(), 5000);
        return isNavigating();
    }

    public  WorldPoint getPlayerBoatLocation()
    {
        if (boat == null)
        {
            return null;
        }

        return Microbot.getClientThread().invoke(() ->
        {
            Player player = Microbot.getClient().getLocalPlayer();
            if (player == null)
            {
                return null;
            }

            WorldPoint playerLocation = player.getWorldLocation();
            LocalPoint localPoint = LocalPoint.fromWorld(
                    player.getWorldView(),
                    playerLocation
            );

            var mainWorldProjection = player
                    .getWorldView()
                    .getMainWorldProjection();

            if (mainWorldProjection == null)
            {
                return playerLocation;
            }

            float[] projection = mainWorldProjection
                    .project(localPoint.getX(), 0, localPoint.getY());

            return Microbot.getClientThread().invoke(() -> WorldPoint.fromLocal(
                    Microbot.getClient().getTopLevelWorldView(),
                    (int) projection[0],
                    (int) projection[2],
                    0
            ));
        });
    }

    public  boolean boardBoat()
    {
        if (isOnBoat())
        {
            return true;
        }

        int[] SAILING_GANGPLANKS = {
                59831,
                59832,
                59833,
                59834,
                59835,
                59836,
                59837,
                59838,
                59839,
                59840,
                59841,
                59842,
                59843,
                59844,
                59845,
                59846,
                59847,
                59848,
                59849,
                59850,
                59851,
                59852,
                59853,
                59854,
                59855,
                59856,
                59857,
                59858,
                59859,
                59860,
                59861,
                59862,
                59863,
                59864,
                59865,
                59866
        };
        Rs2GameObject.interact(SAILING_GANGPLANKS, "Board");
        sleepUntil(() -> isOnBoat(), 5000);
        return isOnBoat();
    }

    public  boolean disembarkBoat()
    {
        if (!isOnBoat())
        {
            return true;
        }
        int[] SAILING_GANGPLANKS = {
                59831,
                59832,
                59833,
                59834,
                59835,
                59836,
                59837,
                59838,
                59839,
                59840,
                59841,
                59842,
                59843,
                59844,
                59845,
                59846,
                59847,
                59848,
                59849,
                59850,
                59851,
                59852,
                59853,
                59854,
                59855,
                59856,
                59857,
                59858,
                59859,
                59860,
                59861,
                59862,
                59863,
                59864,
                59865,
                59866
        };
        WorldView wv = Microbot.getClient().getTopLevelWorldView();

        Scene scene = wv.getScene();
        Tile[][][] tiles = scene.getTiles();

        int z = wv.getPlane();

        for (int x = 0; x < tiles[z].length; ++x)
        {
            for (int y = 0; y < tiles[z][x].length; ++y)
            {
                Tile tile = tiles[z][x][y];

                if (tile == null)
                {
                    continue;
                }

                Player player = Microbot.getClient().getLocalPlayer();
                if (player == null)
                {
                    continue;
                }

                if (tile.getGroundObject() == null)
                {
                    continue;
                }

                if (Arrays.stream(SAILING_GANGPLANKS).anyMatch(id -> id == tile.getGroundObject().getId()))
                {
                    Rs2GameObject.clickObject(tile.getGroundObject(), "Disembark");
                    sleepUntil(() -> !isOnBoat(), 5000);
                }
            }
        }
        Rs2GameObject.interact(SAILING_GANGPLANKS, "disembark");
        sleepUntil(() -> !isOnBoat(), 5000);
        return !isOnBoat();
    }

    public  boolean isMovingForward()
    {
        final int movingForward = 2;
        return Microbot.getVarbitValue(VarbitID.SAILING_SIDEPANEL_BOAT_MOVE_MODE) == movingForward;
    }

    public  boolean isMovingBackward()
    {
        final int movingBackward = 3;
        return Microbot.getVarbitValue(VarbitID.SAILING_SIDEPANEL_BOAT_MOVE_MODE) == movingBackward;
    }

    public  boolean isStandingStill()
    {
        final int standingStill = 0;
        return Microbot.getVarbitValue(VarbitID.SAILING_SIDEPANEL_BOAT_MOVE_MODE) == standingStill;
    }

    public  boolean clickSailButton()
    {
        var widget = Rs2Widget.getWidget(InterfaceID.SailingSidepanel.FACILITIES_ROWS);
        var setSailButton = widget.getDynamicChildren()[0];
        return Rs2Widget.clickWidget(setSailButton);
    }

    public  void setSails()
    {
        if (!isNavigating())
        {
            return;
        }
        Rs2Tab.switchTo(InterfaceTab.COMBAT);
        if (!isMovingForward())
        {
            clickSailButton();
            sleepUntil(() -> isMovingForward(), 2500);
        }
    }

    public  void unsetSails()
    {
        if (!isNavigating())
        {
            return;
        }
        Rs2Tab.switchTo(InterfaceTab.COMBAT);
        if (!isStandingStill())
        {
            clickSailButton();
            sleepUntil(() -> isStandingStill(), 2500);
        }
    }

    public  void sailTo(WorldPoint target)
    {
        if (!isOnBoat())
        {
            var result = boardBoat();
            if (!result)
            {
                log.info("Failed to board boat.");
            }
            return;
        }
        if (!isNavigating())
        {
            var result = navigate();
            if (!result)
            {
                log.info("Failed to navigate boat.");
            }
            return;
        }

        var direction = getDirection(target);
        var heading = Heading.getHeading(direction);
        setHeading(heading);

        if (!isMovingForward())
        {
            setSails();
        }
    }

    public int getDirection(WorldPoint target)
    {
        WorldPoint current = getPlayerBoatLocation();
        int deltaX = target.getX() - current.getX();
        int deltaY = target.getY() - current.getY();

        if (deltaX == 0 && deltaY == 0) {
            return Heading.SOUTH.getValue();
        }

        double angleDegrees = Math.toDegrees(Math.atan2(deltaY, deltaX));
        double headingDegrees = (270.0 - angleDegrees + 360.0) % 360.0;
        int headingValue = (int) ((headingDegrees + 11.25) / 22.5) & 0xF;

        for (Heading heading : Heading.values()) {
            if (heading.getValue() == headingValue) {
                return heading.getValue();
            }
        }

        return Heading.SOUTH.getValue();
    }

    private  double getAngle(WorldPoint target)
    {
        WorldPoint worldPoint = getWorldLocation();
        int playerX = worldPoint.getX();
        int playerY = worldPoint.getY();

        int targetX = target.getX();
        int targetY = target.getY();

        double dx = targetX - playerX;
        double dy = targetY - playerY;

        return Math.toDegrees(Math.atan2(dy, dx));
    }

    public  void setHeading(Heading heading)
    {
        if (heading == currentHeading)
        {
            return;
        }
        currentHeading = heading;
        var menuEntry = new NewMenuEntry()
                .option("Set-Heading")
                .target("")
                .identifier(heading.getValue())
                .type(MenuAction.SET_HEADING)
                .param0(0)
                .param1(0)
                .forceLeftClick(false);
        var worldview = Microbot.getClientThread().invoke(() -> Microbot.getClient().getLocalPlayer().getWorldView());

        if (worldview == null)
        {
            menuEntry.setWorldViewId(Microbot.getClient().getTopLevelWorldView().getId());
        }
        else
        {
            menuEntry.setWorldViewId(worldview.getId());
        }
        Microbot.doInvoke(menuEntry, new java.awt.Rectangle(1, 1, Microbot.getClient().getCanvasWidth(), Microbot.getClient().getCanvasHeight()));
    }

    public boolean trimSails()
    {
        sleep(2500, 3500);
        if (!isOnBoat())
        {
            return false;
        }
        final int[] SAIL_IDS = {
                SAILING_BOAT_SAIL_KANDARIN_1X3_WOOD,
                SAILING_BOAT_SAIL_KANDARIN_1X3_OAK,
                SAILING_BOAT_SAIL_KANDARIN_1X3_TEAK,
                SAILING_BOAT_SAIL_KANDARIN_1X3_MAHOGANY,
                SAILING_BOAT_SAIL_KANDARIN_1X3_CAMPHOR,
                SAILING_BOAT_SAIL_KANDARIN_1X3_IRONWOOD,
                SAILING_BOAT_SAIL_KANDARIN_1X3_ROSEWOOD,
                SAILING_BOAT_SAIL_KANDARIN_2X5_WOOD,
                SAILING_BOAT_SAIL_KANDARIN_2X5_OAK,
                SAILING_BOAT_SAIL_KANDARIN_2X5_TEAK,
                SAILING_BOAT_SAIL_KANDARIN_2X5_MAHOGANY,
                SAILING_BOAT_SAIL_KANDARIN_2X5_CAMPHOR,
                SAILING_BOAT_SAIL_KANDARIN_2X5_IRONWOOD,
                SAILING_BOAT_SAIL_KANDARIN_2X5_ROSEWOOD,
                SAILING_BOAT_SAIL_KANDARIN_3X8_WOOD,
                SAILING_BOAT_SAIL_KANDARIN_3X8_OAK,
                SAILING_BOAT_SAIL_KANDARIN_3X8_TEAK,
                SAILING_BOAT_SAIL_KANDARIN_3X8_MAHOGANY,
                SAILING_BOAT_SAIL_KANDARIN_3X8_CAMPHOR,
                SAILING_BOAT_SAIL_KANDARIN_3X8_IRONWOOD,
                SAILING_BOAT_SAIL_KANDARIN_3X8_ROSEWOOD,
                SAILING_BOAT_SAILS_COLOSSAL_REGULAR
        };
        Rs2GameObject.interact(SAIL_IDS, "trim");
        return sleepUntil(() -> Microbot.isGainingExp, 5000);
    }

    public  boolean openCargo()
    {
        if (!isOnBoat())
        {
            return false;
        }
        final int[] SAILING_BOAT_CARGO_HOLDS = {
                SAILING_BOAT_CARGO_HOLD_REGULAR_RAFT,
                SAILING_BOAT_CARGO_HOLD_REGULAR_RAFT_OPEN,
                SAILING_BOAT_CARGO_HOLD_OAK_RAFT,
                SAILING_BOAT_CARGO_HOLD_OAK_RAFT_OPEN,
                SAILING_BOAT_CARGO_HOLD_TEAK_RAFT,
                SAILING_BOAT_CARGO_HOLD_TEAK_RAFT_OPEN,
                SAILING_BOAT_CARGO_HOLD_MAHOGANY_RAFT,
                SAILING_BOAT_CARGO_HOLD_MAHOGANY_RAFT_OPEN,
                SAILING_BOAT_CARGO_HOLD_CAMPHOR_RAFT,
                SAILING_BOAT_CARGO_HOLD_CAMPHOR_RAFT_OPEN,
                SAILING_BOAT_CARGO_HOLD_IRONWOOD_RAFT,
                SAILING_BOAT_CARGO_HOLD_IRONWOOD_RAFT_OPEN,
                SAILING_BOAT_CARGO_HOLD_ROSEWOOD_RAFT,
                SAILING_BOAT_CARGO_HOLD_ROSEWOOD_RAFT_OPEN,
                SAILING_BOAT_CARGO_HOLD_REGULAR_2X5,
                SAILING_BOAT_CARGO_HOLD_REGULAR_2X5_OPEN,
                SAILING_BOAT_CARGO_HOLD_OAK_2X5,
                SAILING_BOAT_CARGO_HOLD_OAK_2X5_OPEN,
                SAILING_BOAT_CARGO_HOLD_TEAK_2X5,
                SAILING_BOAT_CARGO_HOLD_TEAK_2X5_OPEN,
                SAILING_BOAT_CARGO_HOLD_MAHOGANY_2X5,
                SAILING_BOAT_CARGO_HOLD_MAHOGANY_2X5_OPEN,
                SAILING_BOAT_CARGO_HOLD_CAMPHOR_2X5,
                SAILING_BOAT_CARGO_HOLD_CAMPHOR_2X5_OPEN,
                SAILING_BOAT_CARGO_HOLD_IRONWOOD_2X5,
                SAILING_BOAT_CARGO_HOLD_IRONWOOD_2X5_OPEN,
                SAILING_BOAT_CARGO_HOLD_ROSEWOOD_2X5,
                SAILING_BOAT_CARGO_HOLD_ROSEWOOD_2X5_OPEN,
                SAILING_BOAT_CARGO_HOLD_REGULAR_LARGE,
                SAILING_BOAT_CARGO_HOLD_REGULAR_LARGE_OPEN,
                SAILING_BOAT_CARGO_HOLD_OAK_LARGE,
                SAILING_BOAT_CARGO_HOLD_OAK_LARGE_OPEN,
                SAILING_BOAT_CARGO_HOLD_TEAK_LARGE,
                SAILING_BOAT_CARGO_HOLD_TEAK_LARGE_OPEN,
                SAILING_BOAT_CARGO_HOLD_MAHOGANY_LARGE,
                SAILING_BOAT_CARGO_HOLD_MAHOGANY_LARGE_OPEN,
                SAILING_BOAT_CARGO_HOLD_CAMPHOR_LARGE,
                SAILING_BOAT_CARGO_HOLD_CAMPHOR_LARGE_OPEN,
                SAILING_BOAT_CARGO_HOLD_IRONWOOD_LARGE,
                SAILING_BOAT_CARGO_HOLD_IRONWOOD_LARGE_OPEN,
                SAILING_BOAT_CARGO_HOLD_ROSEWOOD_LARGE,
                SAILING_BOAT_CARGO_HOLD_ROSEWOOD_LARGE_OPEN
        };
        return Rs2GameObject.interact(SAILING_BOAT_CARGO_HOLDS, "open");
    }

    public Map<PortTaskVarbits, Integer> getPortTasksVarbits()
    {
        return Arrays.stream(PortTaskVarbits.values())
                .map(x -> Map.entry(x, Microbot.getVarbitValue(x.getId())))
                .filter(e -> e.getValue() > 0 && e.getKey().getType() == PortTaskVarbits.TaskType.ID)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public PortTaskData getPortTaskData(int varbitValue)
    {
        if (varbitValue <= 0)
        {
            return null;
        }

        return Arrays.stream(PortTaskData.values())
                .filter(x -> x.getId() == varbitValue)
                .findFirst()
                .orElse(null);
    }


}