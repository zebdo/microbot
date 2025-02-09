package net.runelite.client.plugins.microbot.util.npc;

import net.runelite.api.Point;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Rs2NpcModel implements net.runelite.api.NPC {

    private WorldView worldView;
    private boolean interacting;
    private Actor interactingActor;
    private int healthRatio;
    private int healthScale;
    private WorldPoint worldLocation;
    private LocalPoint localLocation;
    private int orientation;
    private int currentOrientation;
    private int animation;
    private int poseAnimation;
    private int poseAnimationFrame;
    private int idlePoseAnimation;
    private int idleRotateLeft;
    private int idleRotateRight;
    private int walkAnimation;
    private int walkRotateLeft;
    private int walkRotateRight;
    private int walkRotate180;
    private int runAnimation;
    private int animationFrame;
    private IterableHashTable<ActorSpotAnim> spotAnims;
    private int graphic;
    private int graphicHeight;
    private int spotAnimFrame;
    private Polygon canvasTilePoly;
    private String overheadText;
    private int overheadCycle;
    private boolean isDead;
    private int id;
    private String name;
    private int combatLevel;
    private int index;
    private NPCComposition composition;
    private NPCComposition transformedComposition;
    private NpcOverrides modelOverrides;
    private NpcOverrides chatheadOverrides;
    private Node next;
    private Node previous;
    private long hash;
    private Model model;
    private int modelHeight;
    private int logicalHeight;
    private Shape convexHull;
    private WorldArea worldArea;

    public Rs2NpcModel(NPC npc) {
        worldView = npc.getWorldView();
        interacting = npc.isInteracting();
        interactingActor = npc.getInteracting();
        healthRatio = npc.getHealthRatio();
        healthScale = npc.getHealthScale();
        worldLocation = npc.getWorldLocation();
        localLocation = npc.getLocalLocation();
        orientation = npc.getOrientation();
        currentOrientation = npc.getCurrentOrientation();
        animation = npc.getAnimation();
        poseAnimation = npc.getPoseAnimation();
        poseAnimationFrame = npc.getPoseAnimationFrame();
        idlePoseAnimation = npc.getIdlePoseAnimation();
        idleRotateLeft = npc.getIdleRotateLeft();
        idleRotateRight = npc.getIdleRotateRight();
        walkAnimation = npc.getWalkAnimation();
        walkRotateLeft = npc.getWalkRotateLeft();
        walkRotateRight = npc.getWalkRotateRight();
        walkRotate180 = npc.getWalkRotate180();
        runAnimation = npc.getRunAnimation();
        animationFrame = npc.getAnimationFrame();
        spotAnims = npc.getSpotAnims();
        graphic = npc.getGraphic();
        graphicHeight = npc.getGraphicHeight();
        spotAnimFrame = npc.getSpotAnimFrame();
        canvasTilePoly = npc.getCanvasTilePoly();
        overheadText = npc.getOverheadText();
        overheadCycle = npc.getOverheadCycle();
        isDead = npc.isDead();
        id = npc.getId();
        name = Microbot.getClientThread().runOnClientThread(npc::getName);
        combatLevel =  Microbot.getClientThread().runOnClientThread(npc::getCombatLevel);
        index = npc.getIndex();
        composition = npc.getComposition();
        transformedComposition = npc.getTransformedComposition();
        modelOverrides = npc.getModelOverrides();
        chatheadOverrides = npc.getChatheadOverrides();
        next = npc.getNext();
        previous = npc.getPrevious();
        hash = npc.getHash();
        model = npc.getModel();
        modelHeight = npc.getModelHeight();
        logicalHeight = npc.getLogicalHeight();
        convexHull = npc.getConvexHull();
        worldArea = npc.getWorldArea();
    }

    @Override
    public WorldView getWorldView() {
        return worldView;
    }

    @Override
    public boolean isInteracting() {
        return interacting;
    }

    @Override
    public Actor getInteracting() {
        return interactingActor;
    }

    @Override
    public int getHealthRatio() {
        return healthRatio;
    }

    @Override
    public int getHealthScale() {
        return healthScale;
    }

    @Override
    public WorldPoint getWorldLocation() {
        return worldLocation;
    }

    @Override
    public LocalPoint getLocalLocation() {
        return localLocation;
    }

    @Override
    public int getOrientation() {
        return orientation;
    }

    @Override
    public int getCurrentOrientation() {
        return currentOrientation;
    }

    @Override
    public int getAnimation() {
        return animation;
    }

    @Override
    public int getPoseAnimation() {
        return poseAnimation;
    }

    @Override
    public void setPoseAnimation(int animation) {
        this.poseAnimation = animation;
    }

    @Override
    public int getPoseAnimationFrame() {
        return poseAnimationFrame;
    }

    @Override
    public void setPoseAnimationFrame(int frame) {
        this.poseAnimationFrame = frame;
    }

    @Override
    public int getIdlePoseAnimation() {
        return idlePoseAnimation;
    }

    @Override
    public void setIdlePoseAnimation(int animation) {
        this.idlePoseAnimation = animation;
    }

    @Override
    public int getIdleRotateLeft() {
        return idleRotateLeft;
    }

    @Override
    public void setIdleRotateLeft(int animationID) {
        this.idleRotateLeft = animationID;
    }

    @Override
    public int getIdleRotateRight() {
        return idleRotateRight;
    }

    @Override
    public void setIdleRotateRight(int animationID) {
        this.idleRotateRight = animationID;
    }

    @Override
    public int getWalkAnimation() {
        return walkAnimation;
    }

    @Override
    public void setWalkAnimation(int animationID) {
        this.walkAnimation = animationID;
    }

    @Override
    public int getWalkRotateLeft() {
        return walkRotateLeft;
    }

    @Override
    public void setWalkRotateLeft(int animationID) {
        this.walkRotateLeft = animationID;
    }

    @Override
    public int getWalkRotateRight() {
        return walkRotateRight;
    }

    @Override
    public void setWalkRotateRight(int animationID) {
        this.walkRotateRight = animationID;
    }

    @Override
    public int getWalkRotate180() {
        return walkRotate180;
    }

    @Override
    public void setWalkRotate180(int animationID) {
        this.walkRotate180 = animationID;
    }

    @Override
    public int getRunAnimation() {
        return runAnimation;
    }

    @Override
    public void setRunAnimation(int animationID) {
        this.runAnimation = animationID;
    }

    @Override
    public void setAnimation(int animation) {
        this.animation = animation;
    }

    @Override
    public int getAnimationFrame() {
        return animationFrame;
    }

    @Override
    public void setActionFrame(int frame) {
        this.animationFrame = frame; // Assuming ActionFrame is related to AnimationFrame
    }

    @Override
    public void setAnimationFrame(int frame) {
        this.animationFrame = frame;
    }

    @Override
    public IterableHashTable<ActorSpotAnim> getSpotAnims() {
        return spotAnims;
    }

    @Override
    public boolean hasSpotAnim(int spotAnimId) {
        // Implementation needed
        return false;
    }

    @Override
    public void createSpotAnim(int id, int spotAnimId, int height, int delay) {
        // Implementation needed
    }

    @Override
    public void removeSpotAnim(int id) {
        // Implementation needed
    }

    @Override
    public void clearSpotAnims() {
        // Implementation needed
    }

    @Override
    public int getGraphic() {
        return graphic;
    }

    @Override
    public void setGraphic(int graphic) {
        this.graphic = graphic;
    }

    @Override
    public int getGraphicHeight() {
        return graphicHeight;
    }

    @Override
    public void setGraphicHeight(int height) {
        this.graphicHeight = height;
    }

    @Override
    public int getSpotAnimFrame() {
        return spotAnimFrame;
    }

    @Override
    public void setSpotAnimFrame(int spotAnimFrame) {
        this.spotAnimFrame = spotAnimFrame;
    }

    @Override
    public Polygon getCanvasTilePoly() {
        return canvasTilePoly;
    }

    @Nullable
    @Override
    public Point getCanvasTextLocation(Graphics2D graphics, String text, int zOffset) {
        // Implementation needed
        return null;
    }

    @Override
    public Point getCanvasImageLocation(BufferedImage image, int zOffset) {
        // Implementation needed
        return null;
    }

    @Override
    public Point getCanvasSpriteLocation(SpritePixels sprite, int zOffset) {
        // Implementation needed
        return null;
    }

    @Override
    public Point getMinimapLocation() {
        // Implementation needed
        return null;
    }

    @Override
    public int getLogicalHeight() {
        return logicalHeight;
    }

    @Override
    public Shape getConvexHull() {
        return convexHull;
    }

    @Override
    public WorldArea getWorldArea() {
        return worldArea;
    }

    @Override
    public String getOverheadText() {
        return overheadText;
    }

    @Override
    public void setOverheadText(String overheadText) {
        this.overheadText = overheadText;
    }

    @Override
    public int getOverheadCycle() {
        return overheadCycle;
    }

    @Override
    public void setOverheadCycle(int cycles) {
        this.overheadCycle = cycles;
    }

    @Override
    public boolean isDead() {
        return isDead;
    }

    @Override
    public void setDead(boolean dead) {
        this.isDead = dead;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getCombatLevel() {
        return combatLevel;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public NPCComposition getComposition() {
        return composition;
    }

    @Nullable
    @Override
    public NPCComposition getTransformedComposition() {
        return transformedComposition;
    }

    @Nullable
    @Override
    public NpcOverrides getModelOverrides() {
        return modelOverrides;
    }

    @Nullable
    @Override
    public NpcOverrides getChatheadOverrides() {
        return chatheadOverrides;
    }

    @Override
    public Node getNext() {
        return next;
    }

    @Override
    public Node getPrevious() {
        return previous;
    }

    @Override
    public long getHash() {
        return hash;
    }

    @Override
    public Model getModel() {
        return model;
    }

    @Override
    public int getModelHeight() {
        return modelHeight;
    }

    @Override
    public void setModelHeight(int modelHeight) {
        this.modelHeight = modelHeight;
    }
}
