package net.runelite.client.plugins.microbot.shortestpath.pathfinder.policy;

import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.shortestpath.transport.requirement.ItemRequirement;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;
import java.util.function.ToIntFunction;

public final class TransportRequirementPolicy {
    private static final Set<TransportType> MEMBERS_ONLY_TYPES = EnumSet.of(
            TransportType.AGILITY_SHORTCUT,
            TransportType.GRAPPLE_SHORTCUT,
            TransportType.BOAT,
            TransportType.CHARTER_SHIP,
            TransportType.FAIRY_RING,
            TransportType.GNOME_GLIDER,
            TransportType.MINECART,
            TransportType.POH,
            TransportType.QUETZAL,
            TransportType.WILDERNESS_OBELISK,
            TransportType.TELEPORTATION_LEVER,
            TransportType.TELEPORTATION_MINIGAME,
            TransportType.MAGIC_CARPET,
            TransportType.SPIRIT_TREE);

    private TransportRequirementPolicy() {
    }

    public static boolean requiresMembersWorld(Transport transport) {
        return transport.isMembers() || MEMBERS_ONLY_TYPES.contains(transport.getType());
    }

    public static boolean hasNetworkAccess(
            TransportType type,
            Function<Quest, QuestState> questStateProvider,
            IntUnaryOperator varbitValueProvider,
            IntUnaryOperator itemQuantityProvider) {
        switch (type) {
            case FAIRY_RING:
                return questStateProvider.apply(Quest.FAIRYTALE_II__CURE_A_QUEEN)
                        != QuestState.NOT_STARTED
                        && (itemQuantityProvider.applyAsInt(ItemID.DRAMEN_STAFF) > 0
                        || itemQuantityProvider.applyAsInt(ItemID.LUNAR_MOONCLAN_LIMINAL_STAFF) > 0
                        || varbitValueProvider.applyAsInt(VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE) == 1);
            case GNOME_GLIDER:
                return questStateProvider.apply(Quest.THE_GRAND_TREE) == QuestState.FINISHED;
            case SPIRIT_TREE:
                return questStateProvider.apply(Quest.TREE_GNOME_VILLAGE) == QuestState.FINISHED;
            case QUETZAL:
                return questStateProvider.apply(Quest.TWILIGHTS_PROMISE) == QuestState.FINISHED;
            default:
                return true;
        }
    }

    public static boolean completedQuests(Transport transport, List<QuestState> questStateOrder) {
        return completedQuests(transport, questStateOrder, Rs2Player::getQuestState);
    }

    public static boolean completedQuests(
            Transport transport,
            List<QuestState> questStateOrder,
            Function<Quest, QuestState> questStateProvider) {
        return transport.getQuests().entrySet().stream()
                .allMatch(entry -> {
                    QuestState playerState = questStateProvider.apply(entry.getKey());
                    QuestState requiredState = entry.getValue();
                    int playerIndex = questStateOrder.indexOf(playerState);
                    int requiredIndex = questStateOrder.indexOf(requiredState);
                    if (requiredIndex < 0 || playerIndex < 0) {
                        return false;
                    }
                    return playerIndex >= requiredIndex;
                });
    }

    public static boolean varbitChecks(Transport transport) {
        return varbitChecks(transport, Microbot::getVarbitValue);
    }

    public static boolean varbitChecks(Transport transport, IntUnaryOperator varbitValueProvider) {
        return transport.getVarbits().isEmpty()
                || transport.getVarbits().stream()
                .allMatch(varbitCheck -> varbitCheck.matches(
                        varbitValueProvider.applyAsInt(varbitCheck.getVarbitId())));
    }

    public static boolean varplayerChecks(Transport transport) {
        return varplayerChecks(transport, Microbot::getVarbitPlayerValue);
    }

    public static boolean varplayerChecks(Transport transport, IntUnaryOperator varplayerValueProvider) {
        return transport.getVarplayers().isEmpty()
                || transport.getVarplayers().stream()
                .allMatch(varplayerCheck -> varplayerCheck.matches(
                        varplayerValueProvider.applyAsInt(varplayerCheck.getVarplayerId())));
    }

    public static boolean hasRequiredLevels(
            Transport transport,
            ToIntFunction<Skill> boostedLevelProvider) {
        int[] requiredLevels = transport.getSkillLevels();
        Skill[] skills = Skill.values();
        for (int i = 0; i < requiredLevels.length; i++) {
            if (requiredLevels[i] > 0
                    && boostedLevelProvider.applyAsInt(skills[i]) < requiredLevels[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasRequiredItems(
            Transport transport,
            IntUnaryOperator itemQuantityProvider,
            IntPredicate equippedItemProvider) {
        if (transport.getCanonicalItemRequirements() != null) {
            return transport.getCanonicalItemRequirements().getRequirements().stream()
                    .allMatch(requirement -> hasCanonicalItemRequirement(
                            requirement, itemQuantityProvider, equippedItemProvider));
        }

        if (transport.getItemIdRequirements().isEmpty()) {
            return true;
        }
        return transport.getItemIdRequirements().stream()
                .flatMap(java.util.Collection::stream)
                .anyMatch(itemId -> itemQuantityProvider.applyAsInt(itemId) > 0);
    }

    public static boolean exceedsCurrencyThreshold(Transport transport, int currencyThreshold) {
        int threshold = Math.max(0, currencyThreshold);
        if (transport.getCurrencyAmount() > threshold) {
            return true;
        }
        if (transport.getCanonicalItemRequirements() == null) {
            return false;
        }
        for (ItemRequirement requirement : transport.getCanonicalItemRequirements().getRequirements()) {
            if (requirement.getQuantity() <= threshold) {
                continue;
            }
            for (int itemId : requirement.getItemIds()) {
                if (itemId == ItemID.COINS || itemId == ItemID.ECTOTOKEN
                        || itemId == ItemID.VILLAGE_TRADE_STICKS
                        || itemId == ItemID.WARGUILD_TOKENS) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasCanonicalItemRequirement(
            ItemRequirement requirement,
            IntUnaryOperator itemQuantityProvider,
            IntPredicate equippedItemProvider) {
        for (int staffId : requirement.getStaffIds()) {
            if (equippedItemProvider.test(staffId)) {
                return true;
            }
        }
        for (int offhandId : requirement.getOffhandIds()) {
            if (equippedItemProvider.test(offhandId)) {
                return true;
            }
        }
        for (int itemId : requirement.getItemIds()) {
            if (itemQuantityProvider.applyAsInt(itemId) >= requirement.getQuantity()) {
                return true;
            }
        }
        return false;
    }
}
