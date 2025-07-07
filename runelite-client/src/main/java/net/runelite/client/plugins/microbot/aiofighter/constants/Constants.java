package net.runelite.client.plugins.microbot.aiofighter.constants;

import com.google.common.collect.ImmutableSet;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;

import java.util.Set;

public class Constants {

    public static final int CRAFTING_GUILD_REGION = 11571;

    public static final Set<Integer> LOG_IDS = ImmutableSet.of(
            ItemID.LOGS,
            ItemID.OAK_LOGS,
            ItemID.WILLOW_LOGS,
            ItemID.TEAK_LOGS,
            ItemID.MAPLE_LOGS,
            ItemID.MAHOGANY_LOGS,
            ItemID.YEW_LOGS,
            ItemID.MAGIC_LOGS,
            ItemID.REDWOOD_LOGS
    );

    public static final Set<Integer> DIGSITE_PENDANT_IDS = ImmutableSet.of(
            ItemID.NECKLACE_OF_DIGSITE_1,
            ItemID.NECKLACE_OF_DIGSITE_2,
            ItemID.NECKLACE_OF_DIGSITE_3,
            ItemID.NECKLACE_OF_DIGSITE_4,
            ItemID.NECKLACE_OF_DIGSITE_5
    );

    public static final int BIRD_HOUSE_EMPTY_SPACE = ObjectID.BIRDHOUSE_NOT_BUILT;

    public static final int MEADOW_NORTH_SPACE = ObjectID.BIRDHOUSE_1;
    public static final int MEADOW_SOUTH_SPACE = ObjectID.BIRDHOUSE_2;
    public static final int VERDANT_NORTH_SPACE = ObjectID.BIRDHOUSE_3;
    public static final int VERDANT_SOUTH_SPACE = ObjectID.BIRDHOUSE_4;

    public static final Set<Integer> BIRD_HOUSE_SPACES = ImmutableSet.of(
            MEADOW_NORTH_SPACE,
            MEADOW_SOUTH_SPACE,
            VERDANT_NORTH_SPACE,
            VERDANT_SOUTH_SPACE
    );

    public static final Set<Integer> BIRD_HOUSE_IDS = ImmutableSet.of(
            ObjectID.BIRDHOUSE_NORMAL_BUILT,
            ObjectID.BIRDHOUSE_NORMAL_BIRD,
            ObjectID.BIRDHOUSE_OAK_BUILT,
            ObjectID.BIRDHOUSE_OAK_BIRD,
            ObjectID.BIRDHOUSE_WILLOW_BUILT,
            ObjectID.BIRDHOUSE_WILLOW_BIRD,
            ObjectID.BIRDHOUSE_TEAK_BUILT,
            ObjectID.BIRDHOUSE_TEAK_BIRD,
            ObjectID.BIRDHOUSE_MAPLE_BUILT,
            ObjectID.BIRDHOUSE_MAPLE_BIRD,
            ObjectID.BIRDHOUSE_MAHOGANY_BUILT,
            ObjectID.BIRDHOUSE_MAHOGANY_BIRD,
            ObjectID.BIRDHOUSE_YEW_BUILT,
            ObjectID.BIRDHOUSE_YEW_BIRD,
            ObjectID.BIRDHOUSE_MAGIC_BUILT,
            ObjectID.BIRDHOUSE_MAGIC_BIRD,
            ObjectID.BIRDHOUSE_REDWOOD_BUILT,
            ObjectID.BIRDHOUSE_REDWOOD_BIRD
    );

    public static final Set<Integer> BIRD_HOUSE_EMPTY_IDS = ImmutableSet.of(
            ObjectID.BIRDHOUSE_NORMAL_FULL,
            ObjectID.BIRDHOUSE_OAK_FULL,
            ObjectID.BIRDHOUSE_WILLOW_FULL,
            ObjectID.BIRDHOUSE_TEAK_FULL,
            ObjectID.BIRDHOUSE_MAPLE_FULL,
            ObjectID.BIRDHOUSE_MAHOGANY_FULL,
            ObjectID.BIRDHOUSE_YEW_FULL,
            ObjectID.BIRDHOUSE_MAGIC_FULL,
            ObjectID.BIRDHOUSE_REDWOOD_FULL
    );

    public static final Set<Integer> BIRD_HOUSE_ITEM_IDS = ImmutableSet.of(
            ItemID.BIRDHOUSE_NORMAL,
            ItemID.BIRDHOUSE_OAK,
            ItemID.BIRDHOUSE_WILLOW,
            ItemID.BIRDHOUSE_TEAK,
            ItemID.BIRDHOUSE_MAPLE,
            ItemID.BIRDHOUSE_MAHOGANY,
            ItemID.BIRDHOUSE_YEW,
            ItemID.BIRDHOUSE_MAGIC,
            ItemID.BIRDHOUSE_REDWOOD
    );

    public static final Set<Integer> BIRD_HOUSE_SEED_IDS = ImmutableSet.of(
            ItemID.BARLEY_SEED,
            ItemID.HAMMERSTONE_HOP_SEED,
            ItemID.ASGARNIAN_HOP_SEED,
            ItemID.JUTE_SEED,
            ItemID.YANILLIAN_HOP_SEED,
            ItemID.KRANDORIAN_HOP_SEED
    );

    public static final Set<Integer> BIRD_NEST_IDS = ImmutableSet.of(
            ItemID.BIRD_NEST_EMPTY,
            ItemID.BIRD_NEST_EGG_GREEN,
            ItemID.BIRD_NEST_EGG_BLUE,
            ItemID.BIRD_NEST_SEEDS,
            ItemID.BIRD_NEST_RING,
            ItemID.BIRD_NEST_EMPTY,
            ItemID.BIRD_NEST_CHEAPSEEDS,
            ItemID.BIRD_NEST_DECENTSEEDS,
            ItemID.BIRD_NEST_SEEDS_JAN2019,
            ItemID.BIRD_NEST_DECENTSEEDS_JAN2019,
            ItemID.WC_CLUE_NEST_EASY,
            ItemID.WC_CLUE_NEST_MEDIUM,
            ItemID.WC_CLUE_NEST_HARD,
            ItemID.WC_CLUE_NEST_ELITE
    );

    public static final Set<Integer> MAGIC_MUSHTREE_IDS = ImmutableSet.of(
            ObjectID.FOSSIL_MAGIC_MUSHTREE_TRUNK1,
            ObjectID.FOSSIL_MAGIC_MUSHTREE_TRUNK2,
            ObjectID.FOSSIL_MAGIC_MUSHTREE_TRUNK3
    );

    public static final Set<Integer> ESSENCE_IDS = ImmutableSet.of(
            ItemID.BLANKRUNE,
            ItemID.BLANKRUNE_HIGH,
            ItemID.BLANKRUNE_DAEYALT
    );

    public static final Set<Integer> RUNE_IDS = ImmutableSet.of(
            ItemID.AIRRUNE,
            ItemID.MINDRUNE,
            ItemID.WATERRUNE,
            ItemID.EARTHRUNE,
            ItemID.FIRERUNE,
            ItemID.BODYRUNE,
            ItemID.COSMICRUNE,
            ItemID.CHAOSRUNE,
            ItemID.NATURERUNE,
            ItemID.LAWRUNE,
            ItemID.DEATHRUNE,
            ItemID.ASTRALRUNE,
            ItemID.BLOODRUNE,
            ItemID.SOULRUNE,
            ItemID.WRATHRUNE
    );

    public static final Set<Integer> ANVIL_IDS = ImmutableSet.of(
            ObjectID.ANVIL,
            ObjectID.EXPERIMENTAL_ANVIL,
            ObjectID.VIKING_ANVIL,
            ObjectID.DWARF_KELDAGRIM_ANVIL,
            ObjectID.DORGESH_BLACKSMITH_ANVIL,
            ObjectID.BRUT_ANVIL,
            ObjectID.LOVAKENGJ_ANVIL,
            ObjectID.LOVAKENGJ_ANVIL_ONLOG,
            ObjectID.WINT_ANVIL,
            ObjectID.RAIDS_TEKTON_ANVIL,
            ObjectID.DS2_GUILD_BLACKSMITH_ANVIL
    );

    public static final Set<Integer> STAMINA_POTION_IDS = ImmutableSet.of(
            ItemID._4DOSESTAMINA,
            ItemID._3DOSESTAMINA,
            ItemID._2DOSESTAMINA,
            ItemID._1DOSESTAMINA
    );

    public static final Set<Integer> BREW_POTION_IDS = ImmutableSet.of(
            ItemID._4DOSEPOTIONOFSARADOMIN,
            ItemID._3DOSEPOTIONOFSARADOMIN,
            ItemID._2DOSEPOTIONOFSARADOMIN,
            ItemID._1DOSEPOTIONOFSARADOMIN,
            ItemID.RAIDS_VIAL_XERICAID_WEAK_1,
            ItemID.RAIDS_VIAL_XERICAID_WEAK_2,
            ItemID.RAIDS_VIAL_XERICAID_WEAK_3,
            ItemID.RAIDS_VIAL_XERICAID_WEAK_4,
            ItemID.RAIDS_VIAL_XERICAID_1,
            ItemID.RAIDS_VIAL_XERICAID_2,
            ItemID.RAIDS_VIAL_XERICAID_3,
            ItemID.RAIDS_VIAL_XERICAID_4,
            ItemID.RAIDS_VIAL_XERICAID_STRONG_1,
            ItemID.RAIDS_VIAL_XERICAID_STRONG_2,
            ItemID.RAIDS_VIAL_XERICAID_STRONG_3,
            ItemID.RAIDS_VIAL_XERICAID_STRONG_4
    );

    public static final Set<Integer> RESTORE_POTION_IDS = ImmutableSet.of(
            ItemID._4DOSESTATRESTORE,
            ItemID._3DOSESTATRESTORE,
            ItemID._2DOSESTATRESTORE,
            ItemID._1DOSESTATRESTORE
    );

    public static final Set<Integer> ANTI_POISON_POTION_IDS = ImmutableSet.of(
            ItemID._4DOSEANTIPOISON,
            ItemID._3DOSEANTIPOISON,
            ItemID._2DOSEANTIPOISON,
            ItemID._1DOSEANTIPOISON,
            ItemID._4DOSE2ANTIPOISON,
            ItemID._3DOSE2ANTIPOISON,
            ItemID._2DOSE2ANTIPOISON,
            ItemID._1DOSE2ANTIPOISON,
            ItemID.ANTIDOTE_4,
            ItemID.ANTIDOTE_3,
            ItemID.ANTIDOTE_2,
            ItemID.ANTIDOTE_1,
            ItemID.ANTIDOTE__4,
            ItemID.ANTIDOTE__3,
            ItemID.ANTIDOTE__2,
            ItemID.ANTIDOTE__1
    );

    public static final Set<Integer> ANTI_FIRE_POTION_IDS = ImmutableSet.of(
            ItemID._4DOSE1ANTIDRAGON,
            ItemID._3DOSE1ANTIDRAGON,
            ItemID._2DOSE1ANTIDRAGON,
            ItemID._1DOSE1ANTIDRAGON,
            ItemID._4DOSE2ANTIDRAGON,
            ItemID._3DOSE2ANTIDRAGON,
            ItemID._2DOSE2ANTIDRAGON,
            ItemID._1DOSE2ANTIDRAGON,
            ItemID._4DOSE3ANTIDRAGON,
            ItemID._3DOSE3ANTIDRAGON,
            ItemID._2DOSE3ANTIDRAGON,
            ItemID._1DOSE3ANTIDRAGON
    );

    public static final Set<Integer> PRAYER_RESTORE_POTION_IDS = ImmutableSet.of(
            ItemID._4DOSEPRAYERRESTORE,
            ItemID._3DOSEPRAYERRESTORE,
            ItemID._2DOSEPRAYERRESTORE,
            ItemID._1DOSEPRAYERRESTORE
    );

    public static final Set<Integer> STRENGTH_POTION_IDS = ImmutableSet.of(
            ItemID.STRENGTH4,
            ItemID._3DOSE1STRENGTH,
            ItemID._2DOSE1STRENGTH,
            ItemID._1DOSE1STRENGTH
    );

    public static final Set<Integer> SUPER_STRENGTH_POTION_IDS = ImmutableSet.of(
            ItemID._4DOSE2STRENGTH,
            ItemID._3DOSE2STRENGTH,
            ItemID._2DOSE2STRENGTH,
            ItemID._1DOSE2STRENGTH
    );

    public static final Set<Integer> ATTACK_POTION_IDS = ImmutableSet.of(
            ItemID._4DOSE1ATTACK,
            ItemID._3DOSE1ATTACK,
            ItemID._2DOSE1ATTACK,
            ItemID._1DOSE1ATTACK
    );

    public static final Set<Integer> SUPER_ATTACK_POTION_IDS = ImmutableSet.of(
            ItemID._4DOSE2ATTACK,
            ItemID._3DOSE2ATTACK,
            ItemID._2DOSE2ATTACK,
            ItemID._1DOSE2ATTACK
    );

    public static final Set<Integer> DEFENCE_POTION_IDS = ImmutableSet.of(
            ItemID._4DOSE1DEFENSE,
            ItemID._3DOSE1DEFENSE,
            ItemID._2DOSE1DEFENSE,
            ItemID._1DOSE1DEFENSE
    );

    public static final Set<Integer> SUPER_DEFENCE_POTION_IDS = ImmutableSet.of(
            ItemID._4DOSE2DEFENSE,
            ItemID._3DOSE2DEFENSE,
            ItemID._2DOSE2DEFENSE,
            ItemID._1DOSE2DEFENSE
    );

    public static final Set<Integer> RANGED_POTION_IDS = ImmutableSet.of(
            ItemID._4DOSERANGERSPOTION,
            ItemID._3DOSERANGERSPOTION,
            ItemID._2DOSERANGERSPOTION,
            ItemID._1DOSERANGERSPOTION
    );

    public static final Set<Integer> MAGIC_POTION_IDS = ImmutableSet.of(
            ItemID._4DOSE1MAGIC,
            ItemID._3DOSE1MAGIC,
            ItemID._2DOSE1MAGIC,
            ItemID._1DOSE1MAGIC
    );

    public static final Set<Integer> ENERGY_POTION_IDS = ImmutableSet.of(
            ItemID._4DOSE1ENERGY,
            ItemID._3DOSE1ENERGY,
            ItemID._2DOSE1ENERGY,
            ItemID._1DOSE1ENERGY
    );

    public static final Set<Integer> MINEABLE_GEM_IDS = ImmutableSet.of(
            ItemID.UNCUT_SAPPHIRE,
            ItemID.UNCUT_EMERALD,
            ItemID.UNCUT_RUBY,
            ItemID.UNCUT_DIAMOND
    );

    public static final Set<Integer> ESSENCE_POUCH_IDS = ImmutableSet.of(
            ItemID.RCU_POUCH_COLOSSAL,
            ItemID.RCU_POUCH_COLOSSAL_DEGRADE,
            ItemID.RCU_POUCH_GIANT,
            ItemID.RCU_POUCH_GIANT_DEGRADE,
            ItemID.RCU_POUCH_LARGE,
            ItemID.RCU_POUCH_LARGE_DEGRADE,
            ItemID.RCU_POUCH_MEDIUM,
            ItemID.RCU_POUCH_MEDIUM_DEGRADE,
            ItemID.RCU_POUCH_SMALL
    );

    public static final Set<Integer> DEGRADED_ESSENCE_POUCH_IDS = ImmutableSet.of(
            ItemID.RCU_POUCH_COLOSSAL_DEGRADE,
            ItemID.RCU_POUCH_GIANT_DEGRADE,
            ItemID.RCU_POUCH_LARGE_DEGRADE,
            ItemID.RCU_POUCH_MEDIUM_DEGRADE
    );

    public static final Set<Integer> ARDOUGNE_CLOAK_IDS = ImmutableSet.of(
            ItemID.ARDY_CAPE_EASY,
            ItemID.ARDY_CAPE_MEDIUM,
            ItemID.ARDY_CAPE_HARD,
            ItemID.ARDY_CAPE_ELITE,
            ItemID.CERT_ARRAVCERTIFICATE,
            ItemID.SKILLCAPE_MAX_ARDY
    );

    public static final Set<Integer> DUELING_RING_IDS = ImmutableSet.of(
            ItemID.RING_OF_DUELING_1,
            ItemID.RING_OF_DUELING_2,
            ItemID.RING_OF_DUELING_3,
            ItemID.RING_OF_DUELING_4,
            ItemID.RING_OF_DUELING_5,
            ItemID.RING_OF_DUELING_6,
            ItemID.RING_OF_DUELING_7,
            ItemID.RING_OF_DUELING_8
    );

    public static final Set<Integer> EXPLORERS_RING_IDS = ImmutableSet.of(
            ItemID.LUMBRIDGE_RING_MEDIUM,
            ItemID.LUMBRIDGE_RING_HARD,
            ItemID.LUMBRIDGE_RING_ELITE
    );

    public static final Set<Integer> AMULET_OF_GLORY_IDS = ImmutableSet.of(
            ItemID.AMULET_OF_GLORY_1,
            ItemID.AMULET_OF_GLORY_2,
            ItemID.AMULET_OF_GLORY_3,
            ItemID.AMULET_OF_GLORY_4,
            ItemID.AMULET_OF_GLORY_5,
            ItemID.AMULET_OF_GLORY_6,
            ItemID.TRAIL_AMULET_OF_GLORY_1,
            ItemID.TRAIL_AMULET_OF_GLORY_2,
            ItemID.TRAIL_AMULET_OF_GLORY_3,
            ItemID.TRAIL_AMULET_OF_GLORY_4,
            ItemID.TRAIL_AMULET_OF_GLORY_5,
            ItemID.TRAIL_AMULET_OF_GLORY_6,
            ItemID.AMULET_OF_GLORY_INF
    );

    public static final Set<Integer> SKILL_NECKLACE_IDS = ImmutableSet.of(
            ItemID.JEWL_NECKLACE_OF_SKILLS_1,
            ItemID.JEWL_NECKLACE_OF_SKILLS_2,
            ItemID.JEWL_NECKLACE_OF_SKILLS_3,
            ItemID.JEWL_NECKLACE_OF_SKILLS_4,
            ItemID.JEWL_NECKLACE_OF_SKILLS_5,
            ItemID.JEWL_NECKLACE_OF_SKILLS_6
    );

    public static final Set<Integer> COMPOST_BIN_IDS = ImmutableSet.of(
            ObjectID.FARMING_COMPOST_BIN_1,
            ObjectID.FARMING_COMPOST_BIN_2,
            ObjectID.FARMING_COMPOST_BIN_3,
            ObjectID.FARMING_COMPOST_BIN_4,
            ObjectID.FARMING_COMPOST_BIN_5,
            ObjectID.FARMING_COMPOST_BIN_6
    );

    public static final Set<Integer> NOTABLE_PRODUCE_IDS = ImmutableSet.of(
            ItemID.POTATO,
            ItemID.ONION,
            ItemID.TOMATO,
            ItemID.SWEETCORN,
            ItemID.STRAWBERRY,
            ItemID.WATERMELON,
            ItemID.SNAPE_GRASS,
            ItemID.MARIGOLD,
            ItemID.ROSEMARY,
            ItemID.NASTURTIUM,
            ItemID.WOADLEAF,
            ItemID.LIMPWURT_ROOT,
            ItemID.WHITELILLY,
            ItemID.UNIDENTIFIED_GUAM,
            ItemID.UNIDENTIFIED_MARENTILL,
            ItemID.UNIDENTIFIED_TARROMIN,
            ItemID.UNIDENTIFIED_HARRALANDER,
            ItemID.UNIDENTIFIED_RANARR,
            ItemID.UNIDENTIFIED_TOADFLAX,
            ItemID.UNIDENTIFIED_IRIT,
            ItemID.UNIDENTIFIED_AVANTOE,
            ItemID.UNIDENTIFIED_KWUARM,
            ItemID.UNIDENTIFIED_SNAPDRAGON,
            ItemID.UNIDENTIFIED_CADANTINE,
            ItemID.UNIDENTIFIED_LANTADYME,
            ItemID.UNIDENTIFIED_DWARF_WEED,
            ItemID.UNIDENTIFIED_TORSTOL,
            ItemID.BARLEY,
            ItemID.HAMMERSTONE_HOPS,
            ItemID.ASGARNIAN_HOPS,
            ItemID.JUTE_FIBRE,
            ItemID.YANILLIAN_HOPS,
            ItemID.KRANDORIAN_HOPS,
            ItemID.WILDBLOOD_HOPS,
            ItemID.REDBERRIES,
            ItemID.CADAVABERRIES,
            ItemID.DWELLBERRIES,
            ItemID.JANGERBERRIES,
            ItemID.WHITE_BERRIES,
            ItemID.POISONIVY_BERRIES,
            ItemID.COOKING_APPLE,
            ItemID.BANANA,
            ItemID.ORANGE,
            ItemID.CURRY_LEAF,
            ItemID.PINEAPPLE,
            ItemID.PAPAYA,
            ItemID.COCONUT,
            ItemID.DRAGONFRUIT,
            ItemID.GIANT_SEAWEED,
            ItemID.GRAPES,
            ItemID.BITTERCAP_MUSHROOM,
            ItemID.CACTUS_SPINE,
            ItemID.CACTUS_POTATO
    );

    public static final Set<Integer> TOOL_LEPRECHAUN_IDS = ImmutableSet.of(
            NpcID.FARMING_TOOLS_LEPRECHAUN,
            NpcID.MYARM_LEPRECHAUN,
            NpcID.FOSSIL_LEPRECHAUN_UNDERWATER,
            NpcID.FARMING_TOOLS_LEPRECHAUN_DRAYNOR,
            NpcID.FARMING_TOOLS_LEPRECHAUN_VARLAMORE
    );

    public static final Set<Integer> ALLOTMENT_PATCH_IDS = ImmutableSet.of(
            ObjectID.FARMING_VEG_PATCH_1,
            ObjectID.FARMING_VEG_PATCH_2,
            ObjectID.FARMING_VEG_PATCH_3,
            ObjectID.FARMING_VEG_PATCH_4,
            ObjectID.FARMING_VEG_PATCH_5
    );

    public static final Set<Integer> FLOWER_PATCH_IDS = ImmutableSet.of(
            ObjectID.FARMING_FLOWER_PATCH_1,
            ObjectID.FARMING_FLOWER_PATCH_2,
            ObjectID.FARMING_FLOWER_PATCH_3,
            ObjectID.FARMING_FLOWER_PATCH_4,
            ObjectID.FARMING_FLOWER_PATCH_5
            // ObjectID.NULL_33649 // No equivalent found, commented out
    );

    public static final Set<Integer> HERB_PATCH_IDS = ImmutableSet.of(
            ObjectID.FARMING_HERB_PATCH_1,
            ObjectID.FARMING_HERB_PATCH_2,
            ObjectID.FARMING_HERB_PATCH_3,
            ObjectID.FARMING_HERB_PATCH_4
    );

    public static final Set<Integer> HOPS_PATCH_IDS = ImmutableSet.of();

    public static final Set<Integer> BUSH_PATCH_IDS = ImmutableSet.of(34006);

    public static final Set<Integer> TREE_PATCH_IDS = ImmutableSet.of(
            ObjectID.FARMING_TREE_PATCH_1,
            ObjectID.FARMING_TREE_PATCH_2,
            ObjectID.FARMING_TREE_PATCH_3,
            ObjectID.FARMING_TREE_PATCH_4,
            ObjectID.FARMING_TREE_PATCH_5,
            33732
    );

    public static final Set<Integer> FRUIT_TREE_PATCH_IDS = ImmutableSet.of(
            ObjectID.FARMING_FRUIT_TREE_PATCH_1,
            ObjectID.FARMING_FRUIT_TREE_PATCH_2,
            ObjectID.FARMING_FRUIT_TREE_PATCH_3,
            ObjectID.FARMING_FRUIT_TREE_PATCH_4,
            ObjectID.FARMING_FRUIT_TREE_PATCH_5,
            34007
    );

    public static final Set<Integer> HARDWOOD_TREE_PATCH_IDS = ImmutableSet.of(
            ObjectID.FARMING_HARDWOOD_TREE_PATCH_2,
            ObjectID.FARMING_HARDWOOD_TREE_PATCH_3,
            ObjectID.FARMING_HARDWOOD_TREE_PATCH_1
    );

    public static final Set<Integer> SPIRIT_TREE_PATCH_IDS = ImmutableSet.of(33733);

    public static final Set<Integer> SEAWEED_PATCH_IDS = ImmutableSet.of();

    public static final Set<Integer> CACTUS_PATCH_IDS = ImmutableSet.of(33761);

    public static final Set<Integer> GRAPE_PATCH_IDS = ImmutableSet.of();

    public static final Set<Integer> MUSHROOM_PATCH_IDS = ImmutableSet.of();

    public static final Set<Integer> BELLADONNA_PATCH_IDS = ImmutableSet.of();

    public static final Set<Integer> HESPORI_PATCH_IDS = ImmutableSet.of(34630);

    public static final Set<Integer> ANIMA_PATCH_IDS = ImmutableSet.of(33998);

    public static final Set<Integer> CALQUAT_PATCH_IDS = ImmutableSet.of(
            ObjectID.FARMING_CALQUAT_TREE_PATCH
    );

    public static final Set<Integer> CRYSTAL_PATCH_IDS = ImmutableSet.of();

    public static final Set<Integer> CELASTRUS_PATCH_IDS = ImmutableSet.of(34629);

    public static final Set<Integer> REDWOOD_PATCH_IDS = ImmutableSet.of(34055);

    public static final Set<Integer> ALLOTMENT_SEED_IDS = ImmutableSet.of(
            ItemID.POTATO_SEED,
            ItemID.ONION_SEED,
            ItemID.CABBAGE_SEED,
            ItemID.TOMATO_SEED,
            ItemID.SWEETCORN_SEED,
            ItemID.STRAWBERRY_SEED,
            ItemID.WATERMELON_SEED,
            ItemID.SNAPE_GRASS_SEED
    );

    public static final Set<Integer> FLOWER_SEED_IDS = ImmutableSet.of(
            ItemID.MARIGOLD_SEED,
            ItemID.ROSEMARY_SEED,
            ItemID.NASTURTIUM_SEED,
            ItemID.WOAD_SEED,
            ItemID.LIMPWURT_SEED,
            ItemID.WHITE_LILY_SEED
    );

    public static final Set<Integer> HERB_SEED_IDS = ImmutableSet.of(
            ItemID.GUAM_SEED,
            ItemID.MARRENTILL_SEED,
            ItemID.TARROMIN_SEED,
            ItemID.HARRALANDER_SEED,
            ItemID.RANARR_SEED,
            ItemID.TOADFLAX_SEED,
            ItemID.IRIT_SEED,
            ItemID.AVANTOE_SEED,
            ItemID.KWUARM_SEED,
            ItemID.SNAPDRAGON_SEED,
            ItemID.CADANTINE_SEED,
            ItemID.LANTADYME_SEED,
            ItemID.DWARF_WEED_SEED,
            ItemID.TORSTOL_SEED
    );

    public static final Set<Integer> HOPS_SEED_IDS = ImmutableSet.of(
            ItemID.BARLEY_SEED,
            ItemID.HAMMERSTONE_HOP_SEED,
            ItemID.ASGARNIAN_HOP_SEED,
            ItemID.JUTE_SEED,
            ItemID.YANILLIAN_HOP_SEED,
            ItemID.KRANDORIAN_HOP_SEED,
            ItemID.WILDBLOOD_HOP_SEED
    );

    public static final Set<Integer> BUSH_SEED_IDS = ImmutableSet.of(
            ItemID.REDBERRY_BUSH_SEED,
            ItemID.CADAVABERRY_BUSH_SEED,
            ItemID.DWELLBERRY_BUSH_SEED,
            ItemID.JANGERBERRY_BUSH_SEED,
            ItemID.WHITEBERRY_BUSH_SEED,
            ItemID.POISONIVY_BUSH_SEED
    );

    public static final Set<Integer> TREE_SEED_IDS = ImmutableSet.of(
            ItemID.ACORN,
            ItemID.WILLOW_SEED,
            ItemID.MAPLE_SEED,
            ItemID.YEW_SEED,
            ItemID.MAGIC_TREE_SEED,
            ItemID.APPLE_TREE_SEED,
            ItemID.BANANA_TREE_SEED,
            ItemID.ORANGE_TREE_SEED,
            ItemID.CURRY_TREE_SEED,
            ItemID.PINEAPPLE_TREE_SEED,
            ItemID.PAPAYA_TREE_SEED,
            ItemID.PALM_TREE_SEED,
            ItemID.DRAGONFRUIT_TREE_SEED,
            ItemID.TEAK_SEED,
            ItemID.MAHOGANY_SEED,
            ItemID.CALQUAT_TREE_SEED,
            ItemID.SOTE_CRYSTAL_SEED_DEAD,
            ItemID.SPIRIT_TREE_SEED,
            ItemID.CELASTRUS_TREE_SEED,
            ItemID.REDWOOD_TREE_SEED
    );

    public static final Set<Integer> TREE_SEEDLING_IDS = ImmutableSet.of(
            ItemID.PLANTPOT_ACORN,
            ItemID.PLANTPOT_WILLOW_SEED,
            ItemID.PLANTPOT_MAPLE_SEED,
            ItemID.PLANTPOT_YEW_SEED
    );

    public static final Set<Integer> TREE_SAPLING_IDS = ImmutableSet.of(
            ItemID.PLANTPOT_OAK_SAPLING,
            ItemID.PLANTPOT_WILLOW_SAPLING,
            ItemID.PLANTPOT_MAPLE_SAPLING,
            ItemID.PLANTPOT_YEW_SAPLING,
            5374 // MAGIC_SAPLING
    );

    public static final Set<Integer> FRUIT_TREE_SAPLING_IDS = ImmutableSet.of(
            ItemID.PLANTPOT_APPLE_SAPLING,
            ItemID.PLANTPOT_BANANA_SAPLING,
            ItemID.PLANTPOT_ORANGE_SAPLING,
            ItemID.PLANTPOT_CURRY_SAPLING,
            ItemID.PLANTPOT_PINEAPPLE_SAPLING
    );

    public static final Set<Integer> HARDWOOD_TREE_SAPLING_IDS = ImmutableSet.of(
            ItemID.TEAK_SAPLING,
            ItemID.MAHOGANY_SAPLING
    );

    public static final Set<Integer> ANIMA_SEED_IDS = ImmutableSet.of(
            ItemID.KRONOS_SEED,
            ItemID.IASOR_SEED,
            ItemID.ATTAS_SEED
    );

    public static final Set<Integer> CACTUS_SEED_IDS = ImmutableSet.of(
            ItemID.CACTUS_SEED,
            ItemID.POTATO_CACTUS_SEED
    );

    public static final Set<Integer> COMPOST_IDS = ImmutableSet.of(
            ItemID.COMPOST,
            ItemID.SUPERCOMPOST,
            ItemID.ULTRACOMPOST,
            ItemID.BOTTOMLESS_COMPOST_BUCKET,
            ItemID.BOTTOMLESS_COMPOST_BUCKET_22997
    );

    public static final Set<Integer> WATERING_CAN_IDS = ImmutableSet.of(
            ItemID.WATERING_CAN1,
            ItemID.WATERING_CAN2,
            ItemID.WATERING_CAN3,
            ItemID.WATERING_CAN4,
            ItemID.WATERING_CAN5,
            ItemID.WATERING_CAN6,
            ItemID.WATERING_CAN7,
            ItemID.WATERING_CAN8
    );

    public static final Set<Integer> GRIMY_HERB_IDS = ImmutableSet.of(
            ItemID.GRIMY_GUAM_LEAF,
            ItemID.GRIMY_MARRENTILL,
            ItemID.GRIMY_TARROMIN,
            ItemID.GRIMY_HARRALANDER,
            ItemID.GRIMY_RANARR_WEED,
            ItemID.GRIMY_TOADFLAX,
            ItemID.GRIMY_IRIT_LEAF,
            ItemID.GRIMY_AVANTOE,
            ItemID.GRIMY_KWUARM,
            ItemID.GRIMY_SNAPDRAGON,
            ItemID.GRIMY_CADANTINE,
            ItemID.GRIMY_LANTADYME,
            ItemID.GRIMY_DWARF_WEED,
            ItemID.GRIMY_TORSTOL
    );

    public static final Set<Integer> CLEAN_HERB_IDS = ImmutableSet.of(
            ItemID.GUAM_LEAF,
            ItemID.MARRENTILL,
            ItemID.TARROMIN,
            ItemID.HARRALANDER,
            ItemID.RANARR_WEED,
            ItemID.TOADFLAX,
            ItemID.IRIT_LEAF,
            ItemID.AVANTOE,
            ItemID.KWUARM,
            ItemID.SNAPDRAGON,
            ItemID.CADANTINE,
            ItemID.LANTADYME,
            ItemID.DWARF_WEED,
            ItemID.TORSTOL
    );

    public static final Set<Integer> CRAFTING_CAPE_IDS = ImmutableSet.of(
            ItemID.CRAFTING_CAPE,
            ItemID.CRAFTING_CAPET
    );

    public static final Set<Integer> CONSTRUCTION_CAPE_IDS = ImmutableSet.of(
            ItemID.CONSTRUCT_CAPE,
            ItemID.CONSTRUCT_CAPET
    );

    public static final Set<Integer> REJUVENATION_POOL_IDS = ImmutableSet.of(
            ObjectID.POOL_OF_REFRESHMENT,
            ObjectID.ORNATE_POOL_OF_REJUVENATION,
            ObjectID.FROZEN_ORNATE_POOL_OF_REJUVENATION
    );

    public static final Set<Integer> LIZARDMAN_SHAMAN_IDS = ImmutableSet.of(
            NpcID.LIZARDMAN_SHAMAN,
            NpcID.LIZARDMAN_SHAMAN_6767,
            NpcID.LIZARDMAN_SHAMAN_7573,
            NpcID.LIZARDMAN_SHAMAN_7574,
            NpcID.LIZARDMAN_SHAMAN_7745,
            NpcID.LIZARDMAN_SHAMAN_7744,
            NpcID.LIZARDMAN_SHAMAN_8565
    );

    public static final Set<Integer> CELL_IDS = ImmutableSet.of(
            ItemID.WEAK_CELL,
            ItemID.MEDIUM_CELL,
            ItemID.STRONG_CELL,
            ItemID.OVERCHARGED_CELL
    );

    public static final Set<Integer> INACTIVE_CELL_TILE_IDS = ImmutableSet.of(
            ObjectID.INACTIVE_CELL_TILE,
            ObjectID.INACTIVE_CELL_TILE_43739
    );

    public static final Set<Integer> ACTIVE_CELL_TILE_IDS = ImmutableSet.of(
            ObjectID.WEAK_CELL_TILE,
            ObjectID.MEDIUM_CELL_TILE,
            ObjectID.STRONG_CELL_TILE,
            ObjectID.OVERPOWERED_CELL_TILE
    );

    public static final Set<Integer> SPECIAL_AXE_IDS = ImmutableSet.of(
            ItemID.DRAGON_AXE,
            ItemID.DRAGON_AXE_OR,
            ItemID.INFERNAL_AXE,
            ItemID.INFERNAL_AXE_OR,
            ItemID.INFERNAL_AXE_UNCHARGED,
            ItemID.INFERNAL_AXE_UNCHARGED_25371,
            ItemID.CRYSTAL_AXE,
            ItemID.CRYSTAL_AXE_23862,
            ItemID.CRYSTAL_AXE_INACTIVE,
            ItemID._3RD_AGE_AXE
    );

    public static final Set<Integer> SPECIAL_PICKAXE_IDS = ImmutableSet.of(
            ItemID.DRAGON_PICKAXE,
            ItemID.DRAGON_PICKAXE_OR,
            ItemID.INFERNAL_PICKAXE,
            ItemID.INFERNAL_PICKAXE_OR,
            ItemID.INFERNAL_PICKAXE_UNCHARGED,
            ItemID.INFERNAL_PICKAXE_UNCHARGED_25369,
            ItemID.CRYSTAL_PICKAXE,
            ItemID.CRYSTAL_PICKAXE_23863,
            ItemID.CRYSTAL_PICKAXE_INACTIVE,
            ItemID._3RD_AGE_PICKAXE
    );

    public static final Set<Integer> SPECIAL_HARPOON_IDS = ImmutableSet.of(
            ItemID.DRAGON_HARPOON,
            ItemID.DRAGON_HARPOON_OR,
            ItemID.INFERNAL_HARPOON,
            ItemID.INFERNAL_HARPOON_OR,
            ItemID.INFERNAL_HARPOON_UNCHARGED,
            ItemID.INFERNAL_HARPOON_UNCHARGED_25367,
            ItemID.CRYSTAL_HARPOON,
            ItemID.CRYSTAL_HARPOON_23864,
            ItemID.CRYSTAL_HARPOON_INACTIVE
    );

    public static final Set<Integer> PHARAOHS_SCEPTRE_IDS = ImmutableSet.of(
            ItemID.PHARAOHS_SCEPTRE,
            ItemID.PHARAOHS_SCEPTRE_9045,
            ItemID.PHARAOHS_SCEPTRE_9046,
            ItemID.PHARAOHS_SCEPTRE_9047,
            ItemID.PHARAOHS_SCEPTRE_9048,
            ItemID.PHARAOHS_SCEPTRE_9049,
            ItemID.PHARAOHS_SCEPTRE_9050,
            ItemID.PHARAOHS_SCEPTRE_9051,
            ItemID.PHARAOHS_SCEPTRE_13074,
            ItemID.PHARAOHS_SCEPTRE_13075,
            ItemID.PHARAOHS_SCEPTRE_13076,
            ItemID.PHARAOHS_SCEPTRE_13077,
            ItemID.PHARAOHS_SCEPTRE_13078,
            ItemID.PHARAOHS_SCEPTRE_16176,
            ItemID.PHARAOHS_SCEPTRE_21445,
            ItemID.PHARAOHS_SCEPTRE_21446,
            ItemID.PHARAOHS_SCEPTRE_26948,
            ItemID.PHARAOHS_SCEPTRE_26950
    );

    public static final Set<Integer> BARROWS_UNDEGRADED_IDS = ImmutableSet.of(
            ItemID.AHRIMS_HOOD,
            ItemID.AHRIMS_STAFF,
            ItemID.AHRIMS_ROBETOP,
            ItemID.AHRIMS_ROBESKIRT,
            ItemID.DHAROKS_HELM,
            ItemID.DHAROKS_GREATAXE,
            ItemID.DHAROKS_PLATEBODY,
            ItemID.DHAROKS_PLATELEGS,
            ItemID.GUTHANS_HELM,
            ItemID.GUTHANS_WARSPEAR,
            ItemID.GUTHANS_PLATEBODY,
            ItemID.GUTHANS_CHAINSKIRT,
            ItemID.KARILS_COIF,
            ItemID.KARILS_CROSSBOW,
            ItemID.KARILS_LEATHERTOP,
            ItemID.KARILS_LEATHERSKIRT,
            ItemID.TORAGS_HELM,
            ItemID.TORAGS_HAMMERS,
            ItemID.TORAGS_PLATEBODY,
            ItemID.TORAGS_PLATELEGS,
            ItemID.VERACS_HELM,
            ItemID.VERACS_FLAIL,
            ItemID.VERACS_BRASSARD,
            ItemID.VERACS_PLATESKIRT
    );

    public static final Set<Integer> TRIDENT_IDS = ImmutableSet.of(
            ItemID.TRIDENT_OF_THE_SEAS_E,
            ItemID.TRIDENT_OF_THE_SEAS,
            ItemID.TRIDENT_OF_THE_SEAS_FULL,
            ItemID.TRIDENT_OF_THE_SWAMP_E,
            ItemID.TRIDENT_OF_THE_SWAMP
    );

    public static final Set<Integer> TELEPORT_IDS = ImmutableSet.of(
            ItemID.ANNAKARL_TELEPORT,
            ItemID.APE_ATOLL_TELEPORT,
            ItemID.ARCEUUS_LIBRARY_TELEPORT,
            ItemID.ARDOUGNE_TELEPORT,
            ItemID.ARDOUGNE_TELEPORT_SCROLL,
            ItemID.BARBARIAN_TELEPORT,
            ItemID.BARROWS_TELEPORT,
            ItemID.BATTLEFRONT_TELEPORT,
            ItemID.BLIGHTED_TELEPORT_SPELL_SACK,
            ItemID.BRIMHAVEN_TELEPORT,
            ItemID.CAMELOT_TELEPORT,
            ItemID.CARRALLANGER_TELEPORT,
            ItemID.CATHERBY_TELEPORT,
            ItemID.CEMETERY_TELEPORT,
            ItemID.CORRUPTED_TELEPORT_CRYSTAL,
            ItemID.CRYSTAL_TELEPORT_SEED,
            ItemID.DAREEYAK_TELEPORT,
            ItemID.DEADMAN_TELEPORT_TABLET,
            ItemID.DIGSITE_TELEPORT,
            ItemID.DRAYNOR_MANOR_TELEPORT,
            ItemID.ENHANCED_CRYSTAL_TELEPORT_SEED,
            ItemID.ETERNAL_TELEPORT_CRYSTAL,
            ItemID.FALADOR_TELEPORT,
            ItemID.FELDIP_HILLS_TELEPORT,
            ItemID.FENKENSTRAINS_CASTLE_TELEPORT,
            ItemID.FISHING_GUILD_TELEPORT,
            ItemID.GHORROCK_TELEPORT,
            ItemID.HARMONY_ISLAND_TELEPORT,
            ItemID.HOSIDIUS_TELEPORT,
            ItemID.ICE_PLATEAU_TELEPORT,
            ItemID.IORWERTH_CAMP_TELEPORT,
            ItemID.KEY_MASTER_TELEPORT,
            ItemID.KHARYRLL_TELEPORT,
            ItemID.KHAZARD_TELEPORT,
            ItemID.KOUREND_CASTLE_TELEPORT,
            ItemID.LASSAR_TELEPORT,
            ItemID.LUMBERYARD_TELEPORT,
            ItemID.LUMBRIDGE_TELEPORT,
            ItemID.LUNAR_ISLE_TELEPORT,
            ItemID.MIND_ALTAR_TELEPORT,
            ItemID.MOONCLAN_TELEPORT,
            ItemID.MORTTON_TELEPORT,
            ItemID.MOS_LEHARMLESS_TELEPORT,
            ItemID.NARDAH_TELEPORT,
            ItemID.OURANIA_TELEPORT,
            ItemID.PADDEWWA_TELEPORT,
            ItemID.PEST_CONTROL_TELEPORT,
            ItemID.PISCATORIS_TELEPORT,
            ItemID.POLLNIVNEACH_TELEPORT,
            ItemID.PRIFDDINAS_TELEPORT,
            ItemID.RELLEKKA_TELEPORT,
            ItemID.REVENANT_CAVE_TELEPORT,
            ItemID.RIMMINGTON_TELEPORT,
            ItemID.SALVE_GRAVEYARD_TELEPORT,
            ItemID.SCAPERUNE_TELEPORT,
            ItemID.SENNTISTEN_TELEPORT,
            ItemID.SHATTERED_TELEPORT_SCROLL,
            ItemID.SPEEDY_TELEPORT_SCROLL,
            ItemID.TAI_BWO_WANNAI_TELEPORT,
            ItemID.TARGET_TELEPORT,
            ItemID.TARGET_TELEPORT_SCROLL,
            ItemID.TAVERLEY_TELEPORT,
            ItemID.TELEPORT_CARD,
            ItemID.TELEPORT_CRYSTAL,
            ItemID.TELEPORT_CRYSTAL_1,
            ItemID.TELEPORT_CRYSTAL_2,
            ItemID.TELEPORT_CRYSTAL_3,
            ItemID.TELEPORT_CRYSTAL_4,
            ItemID.TELEPORT_CRYSTAL_5,
            ItemID.TELEPORT_FOCUS,
            ItemID.TELEPORT_TO_HOUSE,
            ItemID.TELEPORT_TRAP,
            ItemID.TRAILBLAZER_RELOADED_HOME_TELEPORT_SCROLL,
            ItemID.TRAILBLAZER_TELEPORT_SCROLL,
            ItemID.TROLLHEIM_TELEPORT,
            ItemID.TWISTED_TELEPORT_SCROLL,
            ItemID.VARROCK_TELEPORT,
            ItemID.VOLCANIC_MINE_TELEPORT,
            ItemID.WATCHTOWER_TELEPORT,
            ItemID.WATERBIRTH_TELEPORT,
            ItemID.WATSON_TELEPORT,
            ItemID.WEST_ARDOUGNE_TELEPORT,
            ItemID.WILDERNESS_CRABS_TELEPORT,
            ItemID.WISE_OLD_MANS_TELEPORT_TABLET,
            ItemID.YANILLE_TELEPORT,
            ItemID.ZULANDRA_TELEPORT
    );

    public static final Set<Integer> FOOD_ITEM_IDS = ImmutableSet.of(
            ItemID.ANGLERFISH, ItemID.BLIGHTED_ANGLERFISH,
            ItemID.DARK_CRAB,
            ItemID.COOKED_KARAMBWAN,
            ItemID.TUNA_POTATO,
            ItemID.MANTA_RAY, ItemID.BLIGHTED_MANTA_RAY,
            ItemID.SEA_TURTLE,
            ItemID.SHARK, ItemID.SHARK_6969, ItemID.SHARK_20390,
            ItemID.PADDLEFISH,
            ItemID.PYSK_FISH_0, ItemID.SUPHI_FISH_1, ItemID.LECKISH_FISH_2, ItemID.BRAWK_FISH_3, ItemID.MYCIL_FISH_4, ItemID.ROQED_FISH_5, ItemID.KYREN_FISH_6,
            ItemID.GUANIC_BAT_0, ItemID.PRAEL_BAT_1, ItemID.GIRAL_BAT_2, ItemID.PHLUXIA_BAT_3, ItemID.KRYKET_BAT_4, ItemID.MURNG_BAT_5, ItemID.PSYKK_BAT_6,
            ItemID.UGTHANKI_KEBAB, ItemID.UGTHANKI_KEBAB_1885, ItemID.SUPER_KEBAB,
            ItemID.MUSHROOM_POTATO,
            ItemID.CURRY,
            ItemID.EGG_POTATO,
            ItemID.POTATO_WITH_CHEESE,
            ItemID.MONKFISH, ItemID.MONKFISH_20547,
            ItemID.COOKED_JUBBLY,
            ItemID.COOKED_OOMLIE_WRAP,
            ItemID.CHILLI_POTATO,
            ItemID.POTATO_WITH_BUTTER,
            ItemID.SWORDFISH,
            ItemID.BASS,
            ItemID.TUNA_AND_CORN,
            ItemID.LOBSTER,
            ItemID.STEW,
            ItemID.JUG_OF_WINE,
            ItemID.LAVA_EEL,
            ItemID.CAVE_EEL,
            ItemID.MUSHROOM__ONION,
            ItemID.RAINBOW_FISH,
            ItemID.COOKED_FISHCAKE,
            ItemID.COOKED_CHOMPY, ItemID.COOKED_CHOMPY_7228,
            ItemID.COOKED_SWEETCORN, ItemID.SWEETCORN_7088,
            ItemID.KEBAB,
            ItemID.DRAGONFRUIT,
            ItemID.TUNA, ItemID.CHOPPED_TUNA,
            ItemID.SALMON,
            ItemID.EGG_AND_TOMATO,
            ItemID.PEACH,
            ItemID.COOKED_SLIMY_EEL,
            ItemID.PIKE,
            ItemID.COD,
            ItemID.ROAST_BEAST_MEAT,
            ItemID.TROUT,
            ItemID.PAPAYA_FRUIT,
            ItemID.SPIDER_ON_STICK, ItemID.SPIDER_ON_STICK_6297, ItemID.SPIDER_ON_SHAFT, ItemID.SPIDER_ON_SHAFT_6299, ItemID.SPIDER_ON_SHAFT_6303,
            ItemID.FAT_SNAIL_MEAT,
            ItemID.MACKEREL,
            ItemID.GIANT_CARP,
            ItemID.ROAST_BIRD_MEAT,
            ItemID.FROG_SPAWN,
            ItemID.COOKED_MYSTERY_MEAT,
            ItemID.COOKED_RABBIT,
            ItemID.CHILLI_CON_CARNE,
            ItemID.FRIED_MUSHROOMS,
            ItemID.FRIED_ONIONS,
            ItemID.SCRAMBLED_EGG,
            ItemID.HERRING,
            ItemID.THIN_SNAIL_MEAT, ItemID.LEAN_SNAIL_MEAT,
            ItemID.BREAD,
            ItemID.BAKED_POTATO,
            ItemID.ONION__TOMATO,
            ItemID.SLICE_OF_CAKE, ItemID.CHOCOLATE_SLICE,
            ItemID.SARDINE,
            ItemID.UGTHANKI_MEAT,
            ItemID.COOKED_MEAT, ItemID.COOKED_MEAT_4293,
            ItemID.COOKED_CHICKEN, ItemID.COOKED_CHICKEN_4291,
            ItemID.SPICY_SAUCE,
            ItemID.CHEESE,
            ItemID.SPICY_MINCED_MEAT,
            ItemID.MINCED_MEAT,
            ItemID.BANANA, ItemID.SLICED_BANANA,
            ItemID.TOMATO, ItemID.CHOPPED_TOMATO, ItemID.SPICY_TOMATO,
            ItemID.ANCHOVIES,
            ItemID.SHRIMPS,
            ItemID.POTATO,
            ItemID.WATERMELON_SLICE, ItemID.PINEAPPLE_RING, ItemID.PINEAPPLE_CHUNKS,
            ItemID.ONION, ItemID.CHOPPED_ONION,
            ItemID.ORANGE, ItemID.ORANGE_SLICES,
            ItemID.STRAWBERRY,
            ItemID.CABBAGE,
            ItemID.MINT_CAKE,
            ItemID.PURPLE_SWEETS, ItemID.PURPLE_SWEETS_10476,
            ItemID.HONEY_LOCUST,
            ItemID.BANDAGES, ItemID.BANDAGES_25202, ItemID.BANDAGES_25730,
            ItemID.STRANGE_FRUIT,
            ItemID.WHITE_TREE_FRUIT,
            ItemID.GOUT_TUBER,
            ItemID.JANGERBERRIES,
            ItemID.DWELLBERRIES,
            ItemID.CAVE_NIGHTSHADE,
            ItemID.POT_OF_CREAM,
            ItemID.EQUA_LEAVES,
            ItemID.EDIBLE_SEAWEED,
            ItemID.SCARRED_SCRAPS,
            ItemID.RATIONS
    );

    public static final Set<Integer> GRACEFUL_HOOD = ImmutableSet.of(
            ItemID.GRACEFUL_HOOD,
            ItemID.GRACEFUL_HOOD_11851,
            ItemID.GRACEFUL_HOOD_13579,
            ItemID.GRACEFUL_HOOD_13580,
            ItemID.GRACEFUL_HOOD_13591,
            ItemID.GRACEFUL_HOOD_13592,
            ItemID.GRACEFUL_HOOD_13603,
            ItemID.GRACEFUL_HOOD_13604,
            ItemID.GRACEFUL_HOOD_13615,
            ItemID.GRACEFUL_HOOD_13616,
            ItemID.GRACEFUL_HOOD_13627,
            ItemID.GRACEFUL_HOOD_13628,
            ItemID.GRACEFUL_HOOD_13667,
            ItemID.GRACEFUL_HOOD_13668,
            ItemID.GRACEFUL_HOOD_21061,
            ItemID.GRACEFUL_HOOD_21063,
            ItemID.GRACEFUL_HOOD_24743,
            ItemID.GRACEFUL_HOOD_24745,
            ItemID.GRACEFUL_HOOD_25069,
            ItemID.GRACEFUL_HOOD_25071
    );

    public static final Set<Integer> GRACEFUL_TOP = ImmutableSet.of(
            ItemID.GRACEFUL_TOP,
            ItemID.GRACEFUL_TOP_11855,
            ItemID.GRACEFUL_TOP_13583,
            ItemID.GRACEFUL_TOP_13584,
            ItemID.GRACEFUL_TOP_13595,
            ItemID.GRACEFUL_TOP_13596,
            ItemID.GRACEFUL_TOP_13607,
            ItemID.GRACEFUL_TOP_13608,
            ItemID.GRACEFUL_TOP_13619,
            ItemID.GRACEFUL_TOP_13620,
            ItemID.GRACEFUL_TOP_13631,
            ItemID.GRACEFUL_TOP_13632,
            ItemID.GRACEFUL_TOP_13671,
            ItemID.GRACEFUL_TOP_13672,
            ItemID.GRACEFUL_TOP_21067,
            ItemID.GRACEFUL_TOP_21069,
            ItemID.GRACEFUL_TOP_24749,
            ItemID.GRACEFUL_TOP_24751,
            ItemID.GRACEFUL_TOP_25075,
            ItemID.GRACEFUL_TOP_25077
    );

    public static final Set<Integer> GRACEFUL_LEGS = ImmutableSet.of(
            ItemID.GRACEFUL_LEGS,
            ItemID.GRACEFUL_LEGS_11857,
            ItemID.GRACEFUL_LEGS_13585,
            ItemID.GRACEFUL_LEGS_13586,
            ItemID.GRACEFUL_LEGS_13597,
            ItemID.GRACEFUL_LEGS_13598,
            ItemID.GRACEFUL_LEGS_13609,
            ItemID.GRACEFUL_LEGS_13610,
            ItemID.GRACEFUL_LEGS_13621,
            ItemID.GRACEFUL_LEGS_13622,
            ItemID.GRACEFUL_LEGS_13633,
            ItemID.GRACEFUL_LEGS_13634,
            ItemID.GRACEFUL_LEGS_13673,
            ItemID.GRACEFUL_LEGS_13674,
            ItemID.GRACEFUL_LEGS_21070,
            ItemID.GRACEFUL_LEGS_21072,
            ItemID.GRACEFUL_LEGS_24752,
            ItemID.GRACEFUL_LEGS_24754,
            ItemID.GRACEFUL_LEGS_25078,
            ItemID.GRACEFUL_LEGS_25080
    );

    public static final Set<Integer> RING_OF_ENDURANCE_IDS = ImmutableSet.of(
            ItemID.RING_OF_ENDURANCE,
            ItemID.RING_OF_ENDURANCE_UNCHARGED,
            ItemID.RING_OF_ENDURANCE_UNCHARGED_24844
    );

    public static final Set<Integer> GRACEFUL_BOOTS = ImmutableSet.of(
            ItemID.GRACEFUL_BOOTS,
            ItemID.GRACEFUL_BOOTS_11861,
            ItemID.GRACEFUL_BOOTS_13589,
            ItemID.GRACEFUL_BOOTS_13590,
            ItemID.GRACEFUL_BOOTS_13601,
            ItemID.GRACEFUL_BOOTS_13602,
            ItemID.GRACEFUL_BOOTS_13613,
            ItemID.GRACEFUL_BOOTS_13614,
            ItemID.GRACEFUL_BOOTS_13625,
            ItemID.GRACEFUL_BOOTS_13626,
            ItemID.GRACEFUL_BOOTS_13637,
            ItemID.GRACEFUL_BOOTS_13638,
            ItemID.GRACEFUL_BOOTS_13677,
            ItemID.GRACEFUL_BOOTS_13678,
            ItemID.GRACEFUL_BOOTS_21076,
            ItemID.GRACEFUL_BOOTS_21078,
            ItemID.GRACEFUL_BOOTS_24758,
            ItemID.GRACEFUL_BOOTS_24760,
            ItemID.GRACEFUL_BOOTS_25084,
            ItemID.GRACEFUL_BOOTS_25086
    );

    public static final Set<Integer> GRACEFUL_GLOVES = ImmutableSet.of(
            ItemID.GRACEFUL_GLOVES,
            ItemID.GRACEFUL_GLOVES_11859,
            ItemID.GRACEFUL_GLOVES_13587,
            ItemID.GRACEFUL_GLOVES_13588,
            ItemID.GRACEFUL_GLOVES_13599,
            ItemID.GRACEFUL_GLOVES_13600,
            ItemID.GRACEFUL_GLOVES_13611,
            ItemID.GRACEFUL_GLOVES_13612,
            ItemID.GRACEFUL_GLOVES_13623,
            ItemID.GRACEFUL_GLOVES_13624,
            ItemID.GRACEFUL_GLOVES_13635,
            ItemID.GRACEFUL_GLOVES_13636,
            ItemID.GRACEFUL_GLOVES_13675,
            ItemID.GRACEFUL_GLOVES_13676,
            ItemID.GRACEFUL_GLOVES_21073,
            ItemID.GRACEFUL_GLOVES_21075,
            ItemID.GRACEFUL_GLOVES_24755,
            ItemID.GRACEFUL_GLOVES_24757,
            ItemID.GRACEFUL_GLOVES_25081,
            ItemID.GRACEFUL_GLOVES_25083
    );

    public static final Set<Integer> GRACEFUL_CAPE = ImmutableSet.of(
            ItemID.GRACEFUL_CAPE,
            ItemID.GRACEFUL_CAPE_11853,
            ItemID.GRACEFUL_CAPE_13581,
            ItemID.GRACEFUL_CAPE_13582,
            ItemID.GRACEFUL_CAPE_13593,
            ItemID.GRACEFUL_CAPE_13594,
            ItemID.GRACEFUL_CAPE_13605,
            ItemID.GRACEFUL_CAPE_13606,
            ItemID.GRACEFUL_CAPE_13617,
            ItemID.GRACEFUL_CAPE_13618,
            ItemID.GRACEFUL_CAPE_13629,
            ItemID.GRACEFUL_CAPE_13630,
            ItemID.GRACEFUL_CAPE_13669,
            ItemID.GRACEFUL_CAPE_13670,
            ItemID.GRACEFUL_CAPE_21064,
            ItemID.GRACEFUL_CAPE_21066,
            ItemID.GRACEFUL_CAPE_24746,
            ItemID.GRACEFUL_CAPE_24748,
            ItemID.GRACEFUL_CAPE_25072,
            ItemID.GRACEFUL_CAPE_25074
    );

    public static final Set<Integer> BANK_OBJECT_IDS =
            ImmutableSet.of(
                    ObjectID.BANK_BOOTH,
                    ObjectID.BANK_BOOTH_10083,
                    ObjectID.BANK_BOOTH_10355,
                    ObjectID.BANK_BOOTH_10357,
                    ObjectID.BANK_BOOTH_10517,
                    ObjectID.BANK_BOOTH_10527,
                    ObjectID.BANK_BOOTH_10583,
                    ObjectID.BANK_BOOTH_10584,
                    ObjectID.NULL_10777,
                    ObjectID.BANK_BOOTH_11338,
                    ObjectID.BANK_BOOTH_12798,
                    ObjectID.BANK_BOOTH_12799,
                    ObjectID.BANK_BOOTH_12800,
                    ObjectID.BANK_BOOTH_12801,
                    ObjectID.BANK_BOOTH_14367,
                    ObjectID.BANK_BOOTH_14368,
                    ObjectID.BANK_BOOTH_16642,
                    ObjectID.BANK_BOOTH_16700,
                    ObjectID.BANK_BOOTH_18491,
                    ObjectID.BANK_BOOTH_20325,
                    ObjectID.BANK_BOOTH_20326,
                    ObjectID.BANK_BOOTH_20327,
                    ObjectID.BANK_BOOTH_20328,
                    ObjectID.BANK_BOOTH_22819,
                    ObjectID.BANK_BOOTH_24101,
                    ObjectID.BANK_BOOTH_24347,
                    ObjectID.BANK_BOOTH_25808,
                    ObjectID.BANK_BOOTH_27254,
                    ObjectID.BANK_BOOTH_27260,
                    ObjectID.BANK_BOOTH_27263,
                    ObjectID.BANK_BOOTH_27265,
                    ObjectID.BANK_BOOTH_27267,
                    ObjectID.BANK_BOOTH_27292,
                    ObjectID.BANK_BOOTH_27718,
                    ObjectID.BANK_BOOTH_27719,
                    ObjectID.BANK_BOOTH_27720,
                    ObjectID.BANK_BOOTH_27721,
                    ObjectID.BANK_BOOTH_28429,
                    ObjectID.BANK_BOOTH_28430,
                    ObjectID.BANK_BOOTH_28431,
                    ObjectID.BANK_BOOTH_28432,
                    ObjectID.BANK_BOOTH_28433,
                    ObjectID.BANK_BOOTH_28546,
                    ObjectID.BANK_BOOTH_28547,
                    ObjectID.BANK_BOOTH_28548,
                    ObjectID.BANK_BOOTH_28549,
                    ObjectID.BANK_BOOTH_32666,
                    ObjectID.NULL_34810,
                    ObjectID.BANK_BOOTH_36559,
                    ObjectID.BANK_BOOTH_37959,
                    ObjectID.BANK_BOOTH_39238,
                    ObjectID.BANK_BOOTH_42837,
                    ObjectID.BANK_CHEST,
                    ObjectID.BANK_CHEST_4483,
                    ObjectID.BANK_CHEST_10562,
                    ObjectID.BANK_CHEST_14382,
                    ObjectID.BANK_CHEST_14886,
                    ObjectID.BANK_CHEST_16695,
                    ObjectID.BANK_CHEST_16696,
                    ObjectID.BANK_CHEST_19051,
                    ObjectID.BANK_CHEST_21301,
                    ObjectID.BANK_CHEST_26707,
                    ObjectID.BANK_CHEST_26711,
                    ObjectID.BANK_CHEST_28594,
                    ObjectID.BANK_CHEST_28595,
                    ObjectID.BANK_CHEST_28816,
                    ObjectID.BANK_CHEST_28861,
                    ObjectID.BANK_CHEST_29321,
                    ObjectID.BANK_CHEST_30087,
                    ObjectID.BANK_CHEST_30267,
                    ObjectID.BANK_CHESTWRECK,
                    ObjectID.BANK_CHEST_30926,
                    ObjectID.BANK_CHEST_30989,
                    ObjectID.BANK_CHEST_34343,
                    ObjectID.BANK_CHEST_40473,
                    ObjectID.BANK_CHEST_41315,
                    ObjectID.BANK_CHEST_41493,
                    ObjectID.BANK_CHEST_43697,
                    ObjectID.NULL_12308,
                    ObjectID.BANK_CHEST_44630,
                    ObjectID.GRAND_EXCHANGE_BOOTH,
                    ObjectID.GRAND_EXCHANGE_BOOTH_10061,
                    ObjectID.GRAND_EXCHANGE_BOOTH_30390);

    public static final Set<Integer> BANK_NPC_IDS =
            ImmutableSet.of(
                    NpcID.BANKER,
                    NpcID.BANKER_1479,
                    NpcID.BANKER_1480,
                    NpcID.BANKER_1613,
                    NpcID.BANKER_1618,
                    NpcID.BANKER_1633,
                    NpcID.BANKER_1634,
                    NpcID.BANKER_2117,
                    NpcID.BANKER_2118,
                    NpcID.BANKER_2119,
                    NpcID.BANKER_2292,
                    NpcID.BANKER_2293,
                    NpcID.BANKER_2368,
                    NpcID.BANKER_2369,
                    NpcID.BANKER_2633,
                    NpcID.BANKER_2897,
                    NpcID.BANKER_2898,
                    NpcID.GHOST_BANKER,
                    NpcID.BANKER_3089,
                    NpcID.BANKER_3090,
                    NpcID.BANKER_3091,
                    NpcID.BANKER_3092,
                    NpcID.BANKER_3093,
                    NpcID.BANKER_3094,
                    NpcID.BANKER_TUTOR,
                    NpcID.BANKER_3318,
                    NpcID.SIRSAL_BANKER,
                    NpcID.BANKER_3887,
                    NpcID.BANKER_3888,
                    NpcID.BANKER_4054,
                    NpcID.BANKER_4055,
                    NpcID.NARDAH_BANKER,
                    NpcID.GNOME_BANKER,
                    NpcID.BANKER_6859,
                    NpcID.BANKER_6860,
                    NpcID.BANKER_6861,
                    NpcID.BANKER_6862,
                    NpcID.BANKER_6863,
                    NpcID.BANKER_6864,
                    NpcID.BANKER_6939,
                    NpcID.BANKER_6940,
                    NpcID.BANKER_6941,
                    NpcID.BANKER_6942,
                    NpcID.BANKER_6969,
                    NpcID.BANKER_6970,
                    NpcID.BANKER_7057,
                    NpcID.BANKER_7058,
                    NpcID.BANKER_7059,
                    NpcID.BANKER_7060,
                    NpcID.BANKER_7077,
                    NpcID.BANKER_7078,
                    NpcID.BANKER_7079,
                    NpcID.BANKER_7080,
                    NpcID.BANKER_7081,
                    NpcID.BANKER_7082,
                    NpcID.BANKER_8321,
                    NpcID.BANKER_8322,
                    NpcID.BANKER_8589,
                    NpcID.BANKER_8590,
                    NpcID.BANKER_8666,
                    NpcID.BANKER_9127,
                    NpcID.BANKER_9128,
                    NpcID.BANKER_9129,
                    NpcID.BANKER_9130,
                    NpcID.BANKER_9131,
                    NpcID.BANKER_9132,
                    NpcID.BANKER_9484,
                    NpcID.BANKER_9718,
                    NpcID.BANKER_9719,
                    NpcID.BANKER_10389,
                    NpcID.BANKER_10734,
                    NpcID.BANKER_10735,
                    NpcID.BANKER_10736,
                    NpcID.BANKER_10737);
    public static final Set<Integer> PORTAL_NEXUS_IDS = ImmutableSet.of(
            ObjectID.PORTAL_NEXUS,
            ObjectID.PORTAL_NEXUS_33409,
            ObjectID.PORTAL_NEXUS_33410
    );
}