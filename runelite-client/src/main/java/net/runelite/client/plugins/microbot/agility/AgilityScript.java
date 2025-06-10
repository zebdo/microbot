package net.runelite.client.plugins.microbot.agility;
import com.google.common.collect.ImmutableSet;
import java.util.function.Predicate;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.agility.AgilityPlugin;
import net.runelite.client.plugins.agility.Obstacle;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.agility.models.AgilityObstacleModel;
import net.runelite.client.plugins.microbot.util.camera.Rs2Camera;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.microbot.util.models.RS2Item;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.runelite.api.NullObjectID.*;
import static net.runelite.api.ObjectID.LADDER_36231;
import static net.runelite.api.ObjectID.LADDER_36232;
import static net.runelite.client.plugins.microbot.agility.enums.AgilityCourseName.GNOME_STRONGHOLD_AGILITY_COURSE;
import static net.runelite.client.plugins.microbot.agility.enums.AgilityCourseName.PRIFDDINAS_AGILITY_COURSE;

public class AgilityScript extends Script {

    public static String version = "1.1.1";
    final int MAX_DISTANCE = 2300;

    public List<AgilityObstacleModel> draynorCourse = new ArrayList<>();
    public List<AgilityObstacleModel> alkharidCourse = new ArrayList<>();
    public List<AgilityObstacleModel> varrockCourse = new ArrayList<>();
    public List<AgilityObstacleModel> gnomeStrongholdCourse = new ArrayList<>();
    public List<AgilityObstacleModel> canafisCourse = new ArrayList<>();
    public List<AgilityObstacleModel> faladorCourse = new ArrayList<>();
    public List<AgilityObstacleModel> seersCourse = new ArrayList<>();
    public List<AgilityObstacleModel> polnivCourse = new ArrayList<>();
    public List<AgilityObstacleModel> rellekkaCourse = new ArrayList<>();
    public List<AgilityObstacleModel> ardougneCourse = new ArrayList<>();
    public List<AgilityObstacleModel> prifddinasCourse = new ArrayList<>();
    public List<AgilityObstacleModel> apeatollCourse = new ArrayList<>();
    public List<AgilityObstacleModel> wyrmbasicCourse = new ArrayList<>();
    public List<AgilityObstacleModel> wyrmadvancedCourse = new ArrayList<>();
    public List<AgilityObstacleModel> shayzienbasicCourse = new ArrayList<>();
    public List<AgilityObstacleModel> shayzienadvancedCourse = new ArrayList<>();

    WorldPoint startCourse = null;

    public static int currentObstacle = 0;
    private static boolean isWalkingToStart = false;

    public static final Set<Integer> PORTAL_OBSTACLE_IDS = ImmutableSet.of(
            // Prifddinas portals
            NULL_36241, NULL_36242, NULL_36243, NULL_36244, NULL_36245, NULL_36246
    );
    public static final WorldArea prifFallArea = new WorldArea(3260,6103,15,9,0); // Priff first 2 fall area

    private List<AgilityObstacleModel> getCurrentCourse(MicroAgilityConfig config) {
        switch (config.agilityCourse()) {
            case DRAYNOR_VILLAGE_ROOFTOP_COURSE:
                return draynorCourse;
            case AL_KHARID_ROOFTOP_COURSE:
                return alkharidCourse;
            case VARROCK_ROOFTOP_COURSE:
                return varrockCourse;
            case GNOME_STRONGHOLD_AGILITY_COURSE:
                return gnomeStrongholdCourse;
            case CANIFIS_ROOFTOP_COURSE:
                return canafisCourse;
            case FALADOR_ROOFTOP_COURSE:
                return faladorCourse;
            case SEERS_VILLAGE_ROOFTOP_COURSE:
                return seersCourse;
            case POLLNIVNEACH_ROOFTOP_COURSE:
                return polnivCourse;
            case RELLEKKA_ROOFTOP_COURSE:
                return rellekkaCourse;
            case ARDOUGNE_ROOFTOP_COURSE:
                return ardougneCourse;
            case PRIFDDINAS_AGILITY_COURSE:
                return prifddinasCourse;
            case APE_ATOLL_AGILITY_COURSE:
                return apeatollCourse;
            case COLOSSAL_WYRM_BASIC_COURSE:
                return wyrmbasicCourse;
            case COLOSSAL_WYRM_ADVANCED_COURSE:
                return wyrmadvancedCourse;
            case SHAYZIEN_BASIC_COURSE:
                return shayzienbasicCourse;
            case SHAYZIEN_ADVANCED_COURSE:
                return shayzienadvancedCourse;
            default:
                return canafisCourse;
        }
    }

    private void init(MicroAgilityConfig config) {
        switch (config.agilityCourse()) {
            case GNOME_STRONGHOLD_AGILITY_COURSE:
                startCourse = new WorldPoint(2474, 3436, 0);
                break;
            case DRAYNOR_VILLAGE_ROOFTOP_COURSE:
                startCourse = new WorldPoint(3103, 3279, 0);
                break;
            case AL_KHARID_ROOFTOP_COURSE:
                startCourse = new WorldPoint(3273, 3195, 0);
                break;
            case VARROCK_ROOFTOP_COURSE:
                startCourse = new WorldPoint(3221, 3414, 0);
                break;
            case CANIFIS_ROOFTOP_COURSE:
                startCourse = new WorldPoint(3507, 3489, 0);
                break;
            case FALADOR_ROOFTOP_COURSE:
                startCourse = new WorldPoint(3036, 3341, 0);
                break;
            case SEERS_VILLAGE_ROOFTOP_COURSE:
                startCourse = new WorldPoint(2729, 3486, 0);
                break;
            case POLLNIVNEACH_ROOFTOP_COURSE:
                startCourse = new WorldPoint(3351, 2961, 0);
                break;
            case RELLEKKA_ROOFTOP_COURSE:
                startCourse = new WorldPoint(2625, 3677, 0);
                break;
            case ARDOUGNE_ROOFTOP_COURSE:
                startCourse = new WorldPoint(2673, 3298, 0);
                break;
            case PRIFDDINAS_AGILITY_COURSE:
                startCourse = new WorldPoint(3253, 6109, 0);
                break;
            case APE_ATOLL_AGILITY_COURSE:
                startCourse = new WorldPoint(2754, 2742, 0);
                break;
            case COLOSSAL_WYRM_BASIC_COURSE:
            case COLOSSAL_WYRM_ADVANCED_COURSE:
                startCourse = new WorldPoint(1652, 2931, 0);
                break;
            case SHAYZIEN_BASIC_COURSE:
            case SHAYZIEN_ADVANCED_COURSE:
                startCourse = new WorldPoint(1551, 3632, 0);
                break;
        }
    }

    public boolean run(MicroAgilityConfig config) {
        Microbot.enableAutoRunOn = true;
        currentObstacle = 0;

        Rs2Antiban.resetAntibanSettings();
        Rs2Antiban.antibanSetupTemplates.applyAgilitySetup();
        init(config);
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;
                if (startCourse == null) {
                    Microbot.showMessage("Agility course: " + config.agilityCourse().name() + " is not supported.");
                    sleep(10000);
                    return;
                }

                final List<RS2Item> marksOfGrace = AgilityPlugin.getMarksOfGrace();
                final LocalPoint playerLocation = Microbot.getClient().getLocalPlayer().getLocalLocation();
                final WorldPoint playerWorldLocation = Microbot.getClient().getLocalPlayer().getWorldLocation();

                // Eat food.
                Rs2Player.eatAt(config.hitpoints());

                if(isWalkingToStart) Microbot.log("isWalkingToStart: true");
                else if (Rs2Player.isMoving()) return;
                if (Rs2Player.isAnimating()) return;

                if (currentObstacle >= getCurrentCourse(config).size()) {
                    currentObstacle = 0;
                }

                if (config.agilityCourse() == PRIFDDINAS_AGILITY_COURSE) {
                    TileObject portal = Rs2GameObject.findObject(PORTAL_OBSTACLE_IDS.toArray(new Integer[0]));
                    if (portal != null && Microbot.getClientThread().runOnClientThreadOptional(portal::getClickbox).isPresent()) {
                        if (Rs2GameObject.interact(portal, "travel")) {
                            sleep(2000, 3000);
                            return;
                        }
                    }
                }

                if (Microbot.getClient().getTopLevelWorldView().getPlane() == 0 && playerWorldLocation.distanceTo(startCourse) > 6 && config.agilityCourse() != GNOME_STRONGHOLD_AGILITY_COURSE) {
                    currentObstacle = 0;
                    LocalPoint startCourseLocal = LocalPoint.fromWorld(Microbot.getClient().getTopLevelWorldView(), startCourse);
                    if (config.agilityCourse() == PRIFDDINAS_AGILITY_COURSE && prifFallArea.contains(Rs2Player.getWorldLocation())){ // Check if fell from first tightrope and chimney ( handles walking to start for prif)
                        Rs2Walker.walkTo(startCourse, 8);
                        Microbot.log("Going back to course's starting point");
                        isWalkingToStart = true;
                        return;
                    };
                    if (startCourseLocal == null || playerLocation.distanceTo(startCourseLocal) >= MAX_DISTANCE) { // This max_distance check does not capture prif course so used above
                        tryAlchingItem(config);
                        if (config.agilityCourse() != PRIFDDINAS_AGILITY_COURSE && Rs2Player.getWorldLocation().distanceTo(startCourse) < 100) {
                          // when stuck on third failure point on prif, it will try to walk back use walker instead of clicking the ladder back up as distanceTo start course is 38 and max distance is 5095
                            Rs2Walker.walkTo(startCourse, 8);
                            Microbot.log("Going back to course's starting point");
                            isWalkingToStart = true;
                            return;
                        }
                    }
                }

                if (!marksOfGrace.isEmpty() && !Rs2Inventory.isFull()) {
                    for (RS2Item markOfGraceTile : marksOfGrace) {
                        if (Microbot.getClient().getTopLevelWorldView().getPlane() != markOfGraceTile.getTile().getPlane())
                            continue;
                        if (!Rs2GameObject.canReach(markOfGraceTile.getTile().getWorldLocation()))
                            continue;
                        Rs2GroundItem.loot(markOfGraceTile.getItem().getId());
                        Rs2Player.waitForWalking();
                        return;
                    }
                }

                for (Map.Entry<TileObject, Obstacle> entry : AgilityPlugin.getObstacles().entrySet()) {
                    TileObject object = entry.getKey();
                    Obstacle obstacle = entry.getValue();

                    Tile tile = obstacle.getTile();
                    if (tile.getPlane() == Microbot.getClient().getTopLevelWorldView().getPlane() && object.getLocalLocation().distanceTo(playerLocation) < MAX_DISTANCE) {

                        final int agilityExp = Microbot.getClient().getSkillExperience(Skill.AGILITY);

                        List<AgilityObstacleModel> courses = getCurrentCourse(config);

                        List<Integer> objectIds = courses.stream()
                                .filter(x -> x.getOperationX().check(Rs2Player.getWorldLocation().getX(), x.getRequiredX()) && x.getOperationY().check(Rs2Player.getWorldLocation().getY(), x.getRequiredY()))
                                .map(AgilityObstacleModel::getObjectID)
                                .collect(Collectors.toList());

						Predicate<TileObject> vaildObjectPredicate = o -> {
							if (!objectIds.contains(o.getId())) return false;
							if (Microbot.getClient().getLocalPlayer().getWorldLocation().getPlane() != o.getPlane()) return false;

							if (o instanceof GroundObject) {
								return Rs2GameObject.canReach(o.getWorldLocation(), 2, 2);
							}

							if (o instanceof GameObject) {
								GameObject _o = (GameObject) o;

								switch (o.getId()) {
									case ObjectID.MARKET_STALL_14936:
										return Rs2GameObject.canReach(_o.getWorldLocation(), _o.sizeX(), _o.sizeY(), 4, 4);
									case ObjectID.BEAM_42220:
										return o.getWorldLocation().distanceTo(Microbot.getClient().getLocalPlayer().getWorldLocation()) < 6;
									default:
										return Rs2GameObject.canReach(_o.getWorldLocation(), _o.sizeX() + 2, _o.sizeY() + 2, 4, 4);
								}
							}

							return true;
						};

                        TileObject gameObject = Rs2GameObject.getAll(vaildObjectPredicate).stream().findFirst().orElse(null);

                        if (gameObject == null) {
                            Microbot.log("No agility obstacle found. Report this as a bug if this keeps happening.");
                            return;
                        }

                        tryAlchingItem(config);

                        if (!Rs2Camera.isTileOnScreen(gameObject)) {
                            Rs2Walker.walkMiniMap(gameObject.getWorldLocation());
                        }

                        if (Rs2GameObject.interact(gameObject)) {
                            isWalkingToStart = false;
                            if (gameObject.getId() == LADDER_36231 || gameObject.getId() == LADDER_36232){ // for priff ladder, so it does not double click it
                                sleepUntil(()-> Rs2Player.isNearArea(new WorldPoint(2269,3393,2),1)|| Rs2Player.isNearArea(new WorldPoint(2243,3394,2),1));
                                sleep(300,500);
                            }
                            if (gameObject.getId() != LADDER_36231 && gameObject.getId() != LADDER_36232 && waitForAgilityObstacleToFinish(agilityExp,config))
                                break;
                        }

                    }
                }
            } catch (Exception ex) {
                Microbot.log("An error occurred: " + ex.getMessage(), ex);
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
        return true;
    }

    private void tryAlchingItem(MicroAgilityConfig config) {
        if (!config.alchemy()) return;

        String itemForAlching = getAlchItem(config);

        if (itemForAlching == null) {
            Microbot.log("No items specified for alching or none available.");
        } else {
            Rs2Magic.alch(itemForAlching, 50, 75);
        }
    }

    private String getAlchItem(MicroAgilityConfig config) {
        String itemsInput = config.itemsToAlch().trim();
        if (itemsInput.isEmpty()) {
            return null;
        }

		List<String> itemsToAlch;

		if (itemsInput.contains(",") || itemsInput.contains(", ")) {
			itemsToAlch = Arrays.stream(itemsInput.split(","))
				.map(String::trim)
				.map(String::toLowerCase)
				.collect(Collectors.toList());
		} else {
			itemsToAlch = Collections.singletonList(itemsInput.trim().toLowerCase());
		}

        if (itemsToAlch.isEmpty()) {
            return null;
        }

        return itemsToAlch.get(0);
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

    private boolean waitForAgilityObstacleToFinish(final int agilityExp,MicroAgilityConfig config) {
        double healthPlaceholder= Rs2Player.getHealthPercentage();
        int timeout;
        if(config.agilityCourse() == PRIFDDINAS_AGILITY_COURSE){
            timeout = 15000; // would try to click previous obstacle if not long enough timeout
        }else{
            timeout = 10000;
        }
        sleepUntilOnClientThread(
                () -> agilityExp != Microbot.getClient().getSkillExperience(Skill.AGILITY) ||healthPlaceholder>
                        Rs2Player.getHealthPercentage(), timeout);
        if (agilityExp != Microbot.getClient().getSkillExperience(Skill.AGILITY) || Microbot.getClient().getTopLevelWorldView().getPlane() == 0) {
            currentObstacle++;
            return true;
        }
        return false;
    }
}


