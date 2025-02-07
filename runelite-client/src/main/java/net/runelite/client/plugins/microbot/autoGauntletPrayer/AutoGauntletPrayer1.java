//package net.runelite.client.plugins.microbot.autoGauntletPrayer;
//
//import net.runelite.api.*;
//import net.runelite.api.events.AnimationChanged;
//import net.runelite.api.events.GameTick;
//import net.runelite.api.events.ProjectileMoved;
//import net.runelite.client.eventbus.Subscribe;
//import net.runelite.client.plugins.Plugin;
//import net.runelite.client.plugins.PluginDescriptor;
//import net.runelite.client.plugins.microbot.Microbot;
//import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
//import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
//import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
//import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
//import net.runelite.client.plugins.microbot.util.reflection.Rs2Reflection;
//
//import java.util.Set;
//
//import static net.runelite.client.plugins.microbot.Microbot.log;
//import static net.runelite.client.plugins.microbot.util.Global.sleep;
//
//@PluginDescriptor(
//        name = PluginDescriptor.LiftedMango + "Auto Gauntlet Prayer",
//        description = "Auto Gauntlet Prayer plugin",
//        tags = {"Gauntlet", "pvm", "prayer", "money making", "auto", "boss"},
//        enabledByDefault = false
//)
//public class AutoGauntletPrayer1 extends Plugin {
//    private final int RANGE_PROJECTILE_MINIBOSS = 1705;
//    private final int MAGE_PROJECTILE_MINIBOSS = 1701;
//    private final int RANGE_PROJECTILE = 1711;
//    private final int MAGE_PROJECTILE = 1707;
//    private final int CG_RANGE_PROJECTILE = 1712;
//    private final int CG_MAGE_PROJECTILE = 1708;
//    private final int DEACTIVATE_MAGE_PROJECTILE = 1713;
//    private final int MAGE_ANIMATION = 8754;
//    private final int RANGE_ANIMATION = 8755;
//    private Rs2PrayerEnum nextPrayer = null;
//
//    private static final Set<Integer> HUNLLEF_IDS = Set.of(
//            9035, 9036, 9037, 9038, // Corrupted Hunllef variants
//            9021, 9022, 9023, 9024  // Crystalline Hunllef variants
//    );
//
//    @Override
//    protected void startUp() throws Exception {
//        log("Leviathan plugin started!");
//    }
//
//    @Override
//    protected void shutDown() throws Exception {
//        log("Gauntlet plugin stopped!");
//        // Deactivate all protective prayers on shutdown
//        Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MELEE, false);
//        Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MAGIC, false);
//        Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_RANGE, false);
//    }
//
////    @Subscribe
////    public void onGraphicsObjectCreated(GraphicsObjectCreated event) {
////        log("Graphics object created with ID: " + event.getGraphicsObject().getId());
////    }
//
//    @Subscribe
//    public void onGameTick(GameTick event) {
//
//        NPC hunllef = Microbot.getClient().getNpcs().stream()
//                .filter(npc -> HUNLLEF_IDS.contains(npc.getId()))
//                .findFirst()
//                .orElse(null);
//
//        if (hunllef == null) {
//            return;
//        }
//
//        HeadIcon headIcon = Rs2Reflection.getHeadIcon(hunllef);
////        System.out.println("Overhead: " + headIcon);
//        if (headIcon == HeadIcon.RANGED && !Rs2Inventory.contains("Halberd")) {
////            sleep(10, 200);
//            Rs2Inventory.equip("Crystal staff");
//            Rs2Inventory.equip("Corrupted staff");
//        } else if (headIcon == HeadIcon.MAGIC && !Rs2Inventory.contains("Halberd")) {
////            sleep(10, 200);
//            Rs2Inventory.equip("Crystal bow");
//            Rs2Inventory.equip("Corrupted bow");
//        } else if (headIcon == HeadIcon.MELEE && !Rs2Inventory.contains("bow")) {
////            sleep(10, 200);
//            Rs2Inventory.equip("Crystal staff");
//            Rs2Inventory.equip("Corrupted staff");
//        } else if (headIcon == HeadIcon.MAGIC && !Rs2Inventory.contains("bow")) {
////            sleep(10, 200);
//            Rs2Inventory.equip("halberd");
//        }
//    }
//
//    @Subscribe
//    public void onProjectileMoved(ProjectileMoved event) {
//        int projectileId = event.getProjectile().getId();
//        switch (projectileId) {
//            case MAGE_PROJECTILE:
//            case CG_MAGE_PROJECTILE:
//            case MAGE_PROJECTILE_MINIBOSS:
//                nextPrayer = Rs2PrayerEnum.PROTECT_MAGIC;
//                break;
//            case RANGE_PROJECTILE:
//            case CG_RANGE_PROJECTILE:
//            case RANGE_PROJECTILE_MINIBOSS:
//                nextPrayer = Rs2PrayerEnum.PROTECT_RANGE;
//                break;
//        }
//
//        if (nextPrayer != null && !Rs2Prayer.isPrayerActive(nextPrayer)) {
//            Rs2Prayer.toggle(nextPrayer, true);
//        }
//
//        if (Rs2Equipment.hasEquipped(ItemID.CRYSTAL_BOW_PERFECTED) && !Rs2Prayer.isPrayerActive(Rs2PrayerEnum.RIGOUR)
//                || Rs2Equipment.hasEquipped(ItemID.CORRUPTED_BOW_PERFECTED) && !Rs2Prayer.isPrayerActive(Rs2PrayerEnum.RIGOUR)
//                || Rs2Equipment.hasEquipped(ItemID.CORRUPTED_BOW_ATTUNED) && !Rs2Prayer.isPrayerActive(Rs2PrayerEnum.RIGOUR)) {
//            Rs2Prayer.toggle(Rs2PrayerEnum.RIGOUR, true);
//        }
//        if (Rs2Equipment.hasEquipped(ItemID.CRYSTAL_STAFF_PERFECTED) && !Rs2Prayer.isPrayerActive(Rs2PrayerEnum.AUGURY)
//                || Rs2Equipment.hasEquipped(ItemID.CRYSTAL_STAFF_BASIC) && !Rs2Prayer.isPrayerActive(Rs2PrayerEnum.AUGURY)
//                || Rs2Equipment.hasEquipped(ItemID.CRYSTAL_STAFF_ATTUNED) && !Rs2Prayer.isPrayerActive(Rs2PrayerEnum.AUGURY)
//                || Rs2Equipment.hasEquipped(ItemID.CORRUPTED_STAFF_PERFECTED) && !Rs2Prayer.isPrayerActive(Rs2PrayerEnum.AUGURY)) {
//            Rs2Prayer.toggle(Rs2PrayerEnum.AUGURY, true);
//        }
//        if (Rs2Equipment.hasEquipped(ItemID.CRYSTAL_HALBERD_PERFECTED) && !Rs2Prayer.isPrayerActive(Rs2PrayerEnum.PIETY)
//                || Rs2Equipment.hasEquipped(ItemID.CRYSTAL_HALBERD_BASIC) && !Rs2Prayer.isPrayerActive(Rs2PrayerEnum.PIETY)
//                || Rs2Equipment.hasEquipped(ItemID.CRYSTAL_HALBERD_ATTUNED) && !Rs2Prayer.isPrayerActive(Rs2PrayerEnum.PIETY)
//                || Rs2Equipment.hasEquipped(ItemID.CORRUPTED_HALBERD_PERFECTED) && !Rs2Prayer.isPrayerActive(Rs2PrayerEnum.PIETY)
//                || Rs2Equipment.hasEquipped(ItemID.CORRUPTED_HALBERD_ATTUNED) && !Rs2Prayer.isPrayerActive(Rs2PrayerEnum.PIETY)) {
//            Rs2Prayer.toggle(Rs2PrayerEnum.PIETY, true);
//        }
//    }
//
//    @Subscribe
//    public void onAnimationChanged(AnimationChanged event) {
//        if (!(event.getActor() instanceof NPC)) return;
//
//        NPC npc = (NPC) event.getActor();
//        if (!HUNLLEF_IDS.contains(npc.getId())) return;
//
//        int animationID = npc.getAnimation();
//        switch (animationID) {
//            case MAGE_ANIMATION:
//                nextPrayer = Rs2PrayerEnum.PROTECT_MAGIC;
//                if (!Rs2Prayer.isPrayerActive(nextPrayer)) {
//                    Rs2Prayer.toggle(nextPrayer, true);
//                }
//                break;
//            case RANGE_ANIMATION:
//                nextPrayer = Rs2PrayerEnum.PROTECT_RANGE;
//                if (!Rs2Prayer.isPrayerActive(nextPrayer)) {
//                    Rs2Prayer.toggle(nextPrayer, true);
//                }
//                break;
//        }
//    }
//}
