package net.runelite.client.plugins.microbot.shortestpath;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.transport.parser.ItemRequirementParser;
import net.runelite.client.plugins.microbot.shortestpath.transport.parser.QuestParser;
import net.runelite.client.plugins.microbot.shortestpath.transport.parser.SkillRequirementParser;
import net.runelite.client.plugins.microbot.shortestpath.transport.parser.TransportRecord;
import net.runelite.client.plugins.microbot.shortestpath.transport.parser.TsvParser;
import net.runelite.client.plugins.microbot.shortestpath.transport.parser.VarCheckType;
import net.runelite.client.plugins.microbot.shortestpath.transport.parser.VarRequirement;
import net.runelite.client.plugins.microbot.shortestpath.transport.parser.VarRequirementParser;
import net.runelite.client.plugins.microbot.shortestpath.transport.requirement.ItemRequirement;
import net.runelite.client.plugins.microbot.shortestpath.transport.requirement.TransportItems;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * This class represents a travel point between two WorldPoints.
 */
@Slf4j
public class Transport {
    private static final TsvParser TSV_PARSER = new TsvParser();
    private static final ItemRequirementParser ITEM_REQUIREMENT_PARSER = new ItemRequirementParser();
    private static final QuestParser QUEST_PARSER = new QuestParser();
    private static final SkillRequirementParser SKILL_REQUIREMENT_PARSER = new SkillRequirementParser();
    private static final VarRequirementParser VARBIT_REQUIREMENT_PARSER = VarRequirementParser.forVarbits();
    private static final VarRequirementParser VARPLAYER_REQUIREMENT_PARSER = VarRequirementParser.forVarPlayers();

    //START microbot variables
    @Getter
	@Setter
    private String action;
    @Getter
    private int objectId;
    @Getter
    private String name;

    /**
     * A location placeholder different from null to use for permutation transports
     */
    private static final WorldPoint LOCATION_PERMUTATION = new WorldPoint(-1, -1, -1);

    /**
     * The starting point of this transport
     */
    @Getter
    private WorldPoint origin = null;

    /**
     * The ending point of this transport
     */
    @Getter
    private WorldPoint destination = null;

    /**
     * The skill levels required to use this transport
     */
    @Getter
    private final int[] skillLevels = new int[Skill.values().length];

    /**
     * The quests required to use this transport
     */
    @Getter
    private Map<Quest, QuestState> quests = new HashMap<>();

    /**
     * The ids of items required to use this transport.
     * If the player has **any** of the matching list of items,
     * this transport is valid
     */
    @Getter
    @Setter
    private Set<Set<Integer>> itemIdRequirements = new HashSet<>();

    /**
     * The type of transport
     */
    @Getter
    private TransportType type;

    /**
     * The travel waiting time in number of ticks
     */
    @Getter
    private int duration;

    /**
     * Info to display for this transport. For spirit trees, fairy rings,
     * and others, this is the destination option to pick.
     */
    @Getter
    private String displayInfo;

    /**
     * If this is an item transport, this tracks if it is consumable (as opposed to having infinite uses)
     */
    @Getter
    private boolean isConsumable = false;

    /**
     * The maximum wilderness level that the transport can be used in
     */
    @Getter
    private int maxWildernessLevel = -1;

    /**
     * Any varbits to check for the transport to be valid. All must pass for a transport to be valid
     */
    @Getter
    private final Set<TransportVarbit> varbits = new HashSet<>();

    /**
     * Any varplayers to check for the transport to be valid. All must pass for a transport to be valid
     */
    @Getter
    private final Set<TransportVarPlayer> varplayers = new HashSet<>();

    @Getter
    private String currencyName = "";
    @Getter
    private int currencyAmount = 0;

    /**
     * Transport requires player to be in a members world
     */
    @Getter
    private boolean isMembers = false;

    /**
     * Canonical upstream AND/OR/quantity requirements. Legacy Microbot rows leave
     * this null and continue to use {@link #itemIdRequirements}.
     */
    @Getter
    private TransportItems canonicalItemRequirements;

    /**
     * Exact upstream catalog family, retained even when it maps to an existing
     * Microbot execution family.
     */
    @Getter
    private String catalogType;

    @Getter
    private String source = "microbot";

    @Getter
    private String regionOverride;


    /**
     * Creates a new transport from an origin-only transport
     * and a destination-only transport, and merges requirements
     */
    public Transport(Transport origin, Transport destination) {
        this.origin = origin.origin;
        this.destination = destination.destination;

        for (int i = 0; i < skillLevels.length; i++) {
            this.skillLevels[i] = Math.max(
                    origin.skillLevels[i],
                    destination.skillLevels[i]);
        }

        this.quests.putAll(origin.quests);
        this.quests.putAll(destination.quests);

        this.itemIdRequirements.addAll(origin.itemIdRequirements);
        this.itemIdRequirements.addAll(destination.itemIdRequirements);

        this.type = origin.type;

        this.duration = Math.max(
                origin.duration,
                destination.duration);

        this.displayInfo = destination.displayInfo;

        this.isConsumable |= origin.isConsumable;
        this.isConsumable |= destination.isConsumable;

        this.maxWildernessLevel = Math.max(
                origin.maxWildernessLevel,
                destination.maxWildernessLevel);

        this.varbits.addAll(origin.varbits);
        this.varbits.addAll(destination.varbits);

        this.varplayers.addAll(origin.varplayers);
        this.varplayers.addAll(destination.varplayers);

        //START microbot variables
        this.name = origin.getName();
        this.objectId = origin.getObjectId();
        this.action = origin.getAction();
        this.currencyName = origin.getCurrencyName();
        this.currencyAmount = origin.getCurrencyAmount();
        this.isMembers = origin.isMembers;
        this.canonicalItemRequirements = TransportItems.merge(
                origin.canonicalItemRequirements,
                destination.canonicalItemRequirements);
        this.catalogType = origin.catalogType;
        this.source = origin.source;
        this.regionOverride = destination.regionOverride != null
                ? destination.regionOverride
                : origin.regionOverride;
        //END microbot variables
    }

    /**
     * Base Transport constructor
     */
    public Transport(WorldPoint origin, WorldPoint destination, String displayInfo, TransportType transportType, boolean isMember, int duration) {
        this.origin = origin;
        this.destination = destination;
        this.displayInfo = displayInfo;
        this.type = transportType;
        this.catalogType = transportType == null ? null : transportType.name();
        this.isMembers = isMember;
        this.duration = duration;
    }

    /**
     * Object interaction Transport constructor
     */
    public Transport(WorldPoint origin, WorldPoint destination, String displayInfo, TransportType transportType, boolean isMember, String action, String target, int objectId) {
        this(origin, destination, displayInfo, transportType, isMember, 1);
        this.action = action;
        this.name = target;
        this.objectId = objectId;
    }

    /**
     * Transport constructor with item requirements
     */
    public Transport(WorldPoint destination, String displayInfo, TransportType transportType, boolean isMember, int maxWildernessLevel, Set<Set<Integer>> itemIdRequirements) {
        this(null, destination, displayInfo, transportType, isMember, 1);
        this.maxWildernessLevel = maxWildernessLevel;
        this.itemIdRequirements = itemIdRequirements != null ? new HashSet<>(itemIdRequirements) : new HashSet<>();
    }

    /**
     * Transport constructor with skill requirements
     */
    public Transport(WorldPoint destination, String displayInfo, TransportType transportType, boolean isMember, int maxWildernessLevel, Map<Skill, Integer> skillRequirement) {
        this(null, destination, displayInfo, transportType, isMember, 1);
        this.maxWildernessLevel = maxWildernessLevel;
        if (skillRequirement != null) {
            for (Map.Entry<Skill, Integer> entry : skillRequirement.entrySet()) {
                this.skillLevels[entry.getKey().ordinal()] = entry.getValue();
            }
        }
    }

    Transport(Map<String, String> fieldMap, TransportType transportType) {
        final String DELIM = " ";
        final String DELIM_MULTI = ";";
        final String DELIM_STATE = "=";

        String value;

        // If the origin field is null the transport is a teleportation item or spell
        // If the origin field has 3 elements it is a coordinate of a transport
        // Otherwise it is a transport that needs to be expanded into all permutations (e.g. fairy ring)
        if ((value = fieldMap.get("Origin")) != null) {
            String[] originArray = value.split(DELIM);
            origin = originArray.length == 3 ? new WorldPoint(
                    Integer.parseInt(originArray[0]),
                    Integer.parseInt(originArray[1]),
                    Integer.parseInt(originArray[2])) : LOCATION_PERMUTATION;
        }

        if ((value = fieldMap.get("Destination")) != null) {
            String[] destinationArray = value.split(DELIM);
            destination = destinationArray.length == 3 ? new WorldPoint(
                    Integer.parseInt(destinationArray[0]),
                    Integer.parseInt(destinationArray[1]),
                    Integer.parseInt(destinationArray[2])) : LOCATION_PERMUTATION;
        }

        //START microbot variables
        if ((value = fieldMap.get("menuOption menuTarget objectID")) != null && !value.trim().isEmpty()) {
            value = value.trim(); // Remove leading/trailing spaces

            // Regex pattern for semicolon-separated values
            String regex = "^([^;]+);([^;]+);(\\d+)$";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
            java.util.regex.Matcher matcher = pattern.matcher(value);

            if (matcher.matches()) {
                // Extract matched groups
                action = matcher.group(1).trim();   // First group: menuOption (action)
                name = matcher.group(2).trim();    // Second group: menuTarget (name)
                objectId = Integer.parseInt(matcher.group(3).trim()); // Third group: objectID
            } else {
                log.debug("Skipped invalid menuOption/menuTarget/objectID value: {}", value);
            }
        }

        if ((value = fieldMap.get("Currency")) != null) {
            String[] parts = value.split(DELIM);
            if (parts.length > 1) {
                try {
                    currencyAmount = Integer.parseInt(parts[0].trim());
                    currencyName = parts[1].trim();
                } catch (NumberFormatException e) {
                    log.debug("Skipping invalid Currency field: {}", value);
                }
            }
        }
        //END microbot variables

        if ((value = fieldMap.get("Skills")) != null && !value.trim().isEmpty()) {
            String[] skillRequirements = value.split(DELIM_MULTI);

            for (String requirement : skillRequirements) {
                String[] levelAndSkill = requirement.split(DELIM);

                if (levelAndSkill.length < 2) {
                    continue;
                }

                int level = Integer.parseInt(levelAndSkill[0]);
                String skillName = levelAndSkill[1];

                Skill[] skills = Skill.values();
                for (int i = 0; i < skills.length; i++) {
                    if (skills[i].getName().equals(skillName)) {
                        skillLevels[i] = level;
                        break;
                    }
                }
            }
        }

        if ((value = fieldMap.get("Item IDs")) != null && !value.trim().isEmpty()) {
            String[] itemIdsList = value.split(DELIM_MULTI);
            for (String listIds : itemIdsList) {
                Set<Integer> multiitemList = new HashSet<>();
                String[] itemIds = listIds.split(DELIM);
                for (String item : itemIds) {
                    int itemId = Integer.parseInt(item);
                    multiitemList.add(itemId);
                }
                itemIdRequirements.add(multiitemList);
            }
        }

        if ((value = fieldMap.get("Quests")) != null && !value.trim().isEmpty()) {
            this.quests = parseQuestStates(value);
        }

        if ((value = fieldMap.get("Duration")) != null && !value.trim().isEmpty()) {
            this.duration = Integer.parseInt(value);
        }

        if (TransportType.isTeleport(transportType, origin)) {
            // Teleports should always have a non-zero wait,
            // so the pathfinder doesn't calculate the cost by distance
            this.duration = Math.max(this.duration, 1);
        }

        if ((value = fieldMap.get("Display info")) != null) {
            this.displayInfo = value;
        }

        if ((value = fieldMap.get("Consumable")) != null) {
            this.isConsumable = "T".equals(value) || "yes".equals(value.toLowerCase());
        }

        if ((value = fieldMap.get("Wilderness level")) != null && !value.trim().isEmpty()) {
            this.maxWildernessLevel = Integer.parseInt(value);
        }

        if ((value = fieldMap.get("isMembers")) != null && !value.trim().isEmpty()) {
            this.isMembers = "Y".equals(value.trim()) || "yes".equals(value.trim().toLowerCase());
        }

        if ((value = fieldMap.get("Varbits")) != null && !value.trim().isEmpty()) {
            for (String varbitCheck : value.split(DELIM_MULTI)) {
                if (varbitCheck.isBlank()) {
                    continue;
                }
                String[] parts;
                TransportVarbit.Operator operator;

                if (varbitCheck.contains(">")) {
                    parts = varbitCheck.split(">");
                    operator = TransportVarbit.Operator.GREATER_THAN;
                } else if (varbitCheck.contains("<")) {
                    parts = varbitCheck.split("<");
                    operator = TransportVarbit.Operator.LESS_THAN;
                } else if (varbitCheck.contains("=")) {
                    parts = varbitCheck.split("=");
                    operator = TransportVarbit.Operator.EQUAL;
                } else if (varbitCheck.contains("&")) {
                    parts = varbitCheck.split("&");
                    operator = TransportVarbit.Operator.BIT_SET;
                } else if (varbitCheck.contains("@")) {
                    parts = varbitCheck.split("@");
                    operator = TransportVarbit.Operator.COOLDOWN_MINUTES;
                } else {
                    log.debug("Skipping invalid varbit token: {}", varbitCheck);
                    continue;
                }

                try {
                    int varbitId = Integer.parseInt(parts[0].trim());
                    int varbitValue = Integer.parseInt(parts[1].trim());
                    varbits.add(new TransportVarbit(varbitId, varbitValue, operator));
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    log.debug("Skipping malformed varbit token: {}", varbitCheck);
                }
            }
        }

        if ((value = fieldMap.get("Varplayers")) != null && !value.trim().isEmpty()) {
            for (String varplayerCheck : value.split(DELIM_MULTI)) {
                if (varplayerCheck.isBlank()) {
                    continue;
                }
                String[] parts;
                TransportVarPlayer.Operator operator;

                if (varplayerCheck.contains(">")) {
                    parts = varplayerCheck.split(">");
                    operator = TransportVarPlayer.Operator.GREATER_THAN;
                } else if (varplayerCheck.contains("<")) {
                    parts = varplayerCheck.split("<");
                    operator = TransportVarPlayer.Operator.LESS_THAN;
                } else if (varplayerCheck.contains("=")) {
                    parts = varplayerCheck.split("=");
                    operator = TransportVarPlayer.Operator.EQUAL;
                } else if (varplayerCheck.contains("&")) {
                    parts = varplayerCheck.split("&");
                    operator = TransportVarPlayer.Operator.BIT_SET;
                } else if (varplayerCheck.contains("@")) {
                    parts = varplayerCheck.split("@");
                    operator = TransportVarPlayer.Operator.COOLDOWN_MINUTES;
                } else {
                    log.debug("Skipping invalid varplayer token: {}", varplayerCheck);
                    continue;
                }

                try {
                    int varplayerId = Integer.parseInt(parts[0].trim());
                    int varplayerValue = Integer.parseInt(parts[1].trim());
                    varplayers.add(new TransportVarPlayer(varplayerId, varplayerValue, operator));
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    log.debug("Skipping malformed varplayer token: {}", varplayerCheck);
                }
            }
        }

        this.type = transportType;
        this.catalogType = transportType.name();
        if (TransportType.AGILITY_SHORTCUT.equals(transportType) &&
                (getRequiredLevel(Skill.RANGED) > 1 || getRequiredLevel(Skill.STRENGTH) > 1)) {
            this.type = TransportType.GRAPPLE_SHORTCUT;
        }
    }

    private Transport(TransportRecord record, TransportType transportType, String catalogType) {
        this.type = transportType;
        this.catalogType = catalogType;
        this.source = "Skretzo/shortest-path@e3dc7c5";

        if (record.hasKey(TransportRecord.Fields.ORIGIN)) {
            origin = parseCanonicalPoint(record.getOrigin());
        }
        if (record.hasKey(TransportRecord.Fields.DESTINATION)) {
            destination = parseCanonicalPoint(record.getDestination());
        }

        if (record.has(TransportRecord.Fields.OBJECT_INFO)) {
            parseCanonicalObjectInfo(record.getObjectInfo());
        }
        if (record.has(TransportRecord.Fields.SKILLS)) {
            int[] canonicalSkills = SKILL_REQUIREMENT_PARSER.parse(record.getSkills());
            System.arraycopy(canonicalSkills, 0, skillLevels, 0,
                    Math.min(skillLevels.length, canonicalSkills.length));
        }
        if (record.has(TransportRecord.Fields.ITEMS)) {
            canonicalItemRequirements = ITEM_REQUIREMENT_PARSER.parse(record.getItems());
            if (canonicalItemRequirements != null) {
                for (ItemRequirement requirement : canonicalItemRequirements.getRequirements()) {
                    Set<Integer> alternatives = new LinkedHashSet<>();
                    for (int itemId : requirement.getItemIds()) {
                        alternatives.add(itemId);
                    }
                    if (!alternatives.isEmpty()) {
                        itemIdRequirements.add(alternatives);
                    }
                }
            }
        }
        if (record.has(TransportRecord.Fields.QUESTS)) {
            for (Quest quest : QUEST_PARSER.parse(record.getQuests())) {
                quests.put(quest, QuestState.FINISHED);
            }
        }
        if (record.has(TransportRecord.Fields.DURATION)) {
            duration = Integer.parseInt(record.getDuration());
        }
        if (TransportType.isTeleport(transportType, origin)) {
            duration = Math.max(duration, 1);
        }
        if (record.has(TransportRecord.Fields.DISPLAY_INFO)) {
            displayInfo = record.getDisplayInfo();
        }
        if (record.has(TransportRecord.Fields.CONSUMABLE)) {
            isConsumable = "T".equalsIgnoreCase(record.getConsumable())
                    || "yes".equalsIgnoreCase(record.getConsumable());
        }
        if (record.has(TransportRecord.Fields.WILDERNESS_LEVEL)) {
            maxWildernessLevel = Integer.parseInt(record.getWildernessLevel());
        }
        if (record.has(TransportRecord.Fields.VARBITS)) {
            addCanonicalVarRequirements(VARBIT_REQUIREMENT_PARSER.parse(record.getVarbits()));
        }
        if (record.has(TransportRecord.Fields.VAR_PLAYERS)) {
            addCanonicalVarRequirements(VARPLAYER_REQUIREMENT_PARSER.parse(record.getVarPlayers()));
        }
        if (record.has(TransportRecord.Fields.REGION_OVERRIDE)) {
            regionOverride = record.getRegionOverride();
        }

        if (TransportType.AGILITY_SHORTCUT.equals(type)
                && (getRequiredLevel(Skill.RANGED) > 1 || getRequiredLevel(Skill.STRENGTH) > 1)) {
            type = TransportType.GRAPPLE_SHORTCUT;
        }
    }

    private static WorldPoint parseCanonicalPoint(String value) {
        if (value == null || value.isEmpty()) {
            return LOCATION_PERMUTATION;
        }
        String[] parts = value.trim().split(" ");
        if (parts.length != 3) {
            return LOCATION_PERMUTATION;
        }
        return new WorldPoint(
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]));
    }

    private void parseCanonicalObjectInfo(String value) {
        String normalized = value == null ? "" : value.trim();
        int firstSpace = normalized.indexOf(' ');
        int lastSpace = normalized.lastIndexOf(' ');
        if (firstSpace <= 0) {
            throw new IllegalArgumentException("Invalid canonical object info: " + value);
        }
        action = normalized.substring(0, firstSpace).trim();
        if (lastSpace > firstSpace) {
            String possibleId = normalized.substring(lastSpace + 1).trim();
            try {
                objectId = Integer.parseInt(possibleId);
                name = normalized.substring(firstSpace + 1, lastSpace).trim();
                return;
            } catch (NumberFormatException ignored) {
                // Permutation destination rows may intentionally omit the object ID.
            }
        }
        name = normalized.substring(firstSpace + 1).trim();
    }

    private void addCanonicalVarRequirements(Set<VarRequirement> requirements) {
        for (VarRequirement requirement : requirements) {
            if (requirement.isVarbit()) {
                varbits.add(new TransportVarbit(
                        requirement.getId(),
                        requirement.getValue(),
                        toVarbitOperator(requirement.getCheckType())));
            } else {
                varplayers.add(new TransportVarPlayer(
                        requirement.getId(),
                        requirement.getValue(),
                        toVarplayerOperator(requirement.getCheckType())));
            }
        }
    }

    private static TransportVarbit.Operator toVarbitOperator(VarCheckType type) {
        switch (type) {
            case BIT_SET:
                return TransportVarbit.Operator.BIT_SET;
            case COOLDOWN_MINUTES:
                return TransportVarbit.Operator.COOLDOWN_MINUTES;
            case GREATER:
                return TransportVarbit.Operator.GREATER_THAN;
            case SMALLER:
                return TransportVarbit.Operator.LESS_THAN;
            case EQUAL:
            default:
                return TransportVarbit.Operator.EQUAL;
        }
    }

    private static TransportVarPlayer.Operator toVarplayerOperator(VarCheckType type) {
        switch (type) {
            case BIT_SET:
                return TransportVarPlayer.Operator.BIT_SET;
            case COOLDOWN_MINUTES:
                return TransportVarPlayer.Operator.COOLDOWN_MINUTES;
            case GREATER:
                return TransportVarPlayer.Operator.GREATER_THAN;
            case SMALLER:
                return TransportVarPlayer.Operator.LESS_THAN;
            case EQUAL:
            default:
                return TransportVarPlayer.Operator.EQUAL;
        }
    }

    /**
     * The skill level required to use this transport
     */
    private int getRequiredLevel(Skill skill) {
        return skillLevels[skill.ordinal()];
    }

    /**
     * Whether the transport has one or more quest requirements
     */
    public boolean isQuestLocked() {
        return !quests.isEmpty();
    }

	private static Map<Quest, QuestState> parseQuestStates(String questStatesCombined)
	{
		Map<Quest, QuestState> questStateMap = new HashMap<>();
		String[] entries = questStatesCombined.split(";");
		for (String entry : entries)
		{
			String questName;
			String stateStr;
			if (entry.contains("=")) {
				String[] parts = entry.split("=");
				if (parts.length != 2) continue;
				questName = parts[0].trim();
				stateStr = parts[1].trim();
			} else {
				questName = entry.trim();
				stateStr = QuestState.FINISHED.name();
			}
			for (Quest quest : Quest.values())
			{
				if (quest.getName().equalsIgnoreCase(questName))
				{
					try
					{
						QuestState state = QuestState.valueOf(stateStr);
						questStateMap.put(quest, state);
					}
					catch (IllegalArgumentException e)
					{
						// Invalid state string, skip
					}
					break;
				}
			}
		}
		return questStateMap;
	}

    private static void addTransports(Map<WorldPoint, Set<Transport>> transports, String path, TransportType transportType) {
        addTransports(transports, path, transportType, 0);
    }

    private static void addTransports(Map<WorldPoint, Set<Transport>> transports, String path, TransportType transportType, int radiusThreshold) {
        final String DELIM_COLUMN = "\t";
        final String PREFIX_COMMENT = "#";

        try {
            java.io.InputStream stream = ShortestPathPlugin.class.getResourceAsStream(path);
            if (stream == null) {
                log.warn("Transport resource missing, skipping: {}", path);
                return;
            }
            String s = new String(Util.readAllBytes(stream), StandardCharsets.UTF_8);
            Scanner scanner = new Scanner(s);
            if (!scanner.hasNextLine()) {
                scanner.close();
                log.warn("Transport resource empty, skipping: {}", path);
                return;
            }

            // Header line is the first line in the file and will start with either '#' or '# '
            String headerLine = scanner.nextLine();
            if (headerLine.endsWith("\r")) {
                headerLine = headerLine.substring(0, headerLine.length() - 1);
            }
            headerLine = headerLine.startsWith(PREFIX_COMMENT + " ") ? headerLine.replace(PREFIX_COMMENT + " ", PREFIX_COMMENT) : headerLine;
            headerLine = headerLine.startsWith(PREFIX_COMMENT) ? headerLine.replace(PREFIX_COMMENT, "") : headerLine;
            String[] headers = headerLine.split(DELIM_COLUMN);

            Set<Transport> newTransports = new HashSet<>();

            int lineNumber = 1;
            while (scanner.hasNextLine()) {
                lineNumber++;
                String line = scanner.nextLine();
                if (line.endsWith("\r")) {
                    line = line.substring(0, line.length() - 1);
                }

                if (line.startsWith(PREFIX_COMMENT) || line.isBlank()) {
                    continue;
                }

                String[] fields = line.split(DELIM_COLUMN);
                Map<String, String> fieldMap = new HashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    if (i < fields.length) {
                        fieldMap.put(headers[i], fields[i]);
                    }
                }

                try {
                    Transport transport = new Transport(fieldMap, transportType);
                    newTransports.add(transport);
                } catch (RuntimeException e) {
                    log.warn("Skipping transport row {} in {}: {}", lineNumber, path, e.getMessage());
                }
            }
            scanner.close();

            /*
             * A transport with origin A and destination B is one-way and must
             * be duplicated as origin B and destination A to become two-way.
             * Example: key-locked doors
             *
             * A transport with origin A and a missing destination is one-way,
             * but can go from origin A to all destinations with a missing origin.
             * Example: fairy ring AIQ -> <blank>
             *
             * A transport with a missing origin and destination B is one-way,
             * but can go from all origins with a missing destination to destination B.
             * Example: fairy ring <blank> -> AIQ
             *
             * Identical transports from origin A to destination A are skipped, and
             * non-identical transports from origin A to destination A can be skipped
             * by specifying a radius threshold to ignore almost identical coordinates.
             * Example: fairy ring AIQ -> AIQ
             */
            Set<Transport> transportOrigins = new HashSet<>();
            Set<Transport> transportDestinations = new HashSet<>();
            for (Transport transport : newTransports) {
                WorldPoint origin = transport.getOrigin();
                WorldPoint destination = transport.getDestination();
                // Logic to determine ordinary transport vs teleport vs permutation (e.g. fairy ring)
                if ((origin == null && destination == null)
                        || (LOCATION_PERMUTATION.equals(origin) && LOCATION_PERMUTATION.equals(destination))) {
                    continue;
                } else if (!LOCATION_PERMUTATION.equals(origin) && origin != null
                        && LOCATION_PERMUTATION.equals(destination)) {
                    transportOrigins.add(transport);
                } else if (LOCATION_PERMUTATION.equals(origin)
                        && !LOCATION_PERMUTATION.equals(destination) && destination != null) {
                    transportDestinations.add(transport);
                }
                if (!LOCATION_PERMUTATION.equals(origin)
                        && destination != null && !LOCATION_PERMUTATION.equals(destination)
                        && (origin == null || !origin.equals(destination))) {
                    putTransport(transports, transport, true);
                }
            }
            for (Transport origin : transportOrigins) {
                for (Transport destination : transportDestinations) {
                    if (origin.getOrigin().distanceTo2D(destination.getDestination()) > radiusThreshold) {
                        putTransport(transports, new Transport(origin, destination), true);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Failed to read transport file {}: {}", path, e.getMessage());
        }
    }

    private static void addCanonicalTransports(
            Map<WorldPoint, Set<Transport>> transports,
            String resourceName,
            TransportType transportType,
            String catalogType,
            int radiusThreshold) {
        String resourcePath = "upstream/transports/" + resourceName;
        try (java.io.InputStream stream = ShortestPathPlugin.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IllegalStateException("Pinned upstream transport resource is missing: " + resourcePath);
            }
            String contents = new String(Util.readAllBytes(stream), StandardCharsets.UTF_8);
            Set<Transport> parsed = new LinkedHashSet<>();
            for (TransportRecord record : TSV_PARSER.parse(contents)) {
                parsed.add(new Transport(record, transportType, catalogType));
            }
            expandCanonicalTransports(transports, parsed, radiusThreshold);
        } catch (IOException e) {
            throw new RuntimeException("Unable to load pinned upstream transport resource " + resourcePath, e);
        }
    }

    private static void expandCanonicalTransports(
            Map<WorldPoint, Set<Transport>> transports,
            Set<Transport> parsed,
            int radiusThreshold) {
        Set<Transport> origins = new LinkedHashSet<>();
        Set<Transport> destinations = new LinkedHashSet<>();
        for (Transport transport : parsed) {
            WorldPoint origin = transport.getOrigin();
            WorldPoint destination = transport.getDestination();
            if ((origin == null && destination == null)
                    || (LOCATION_PERMUTATION.equals(origin) && LOCATION_PERMUTATION.equals(destination))) {
                continue;
            }
            if (origin != null && !LOCATION_PERMUTATION.equals(origin)
                    && LOCATION_PERMUTATION.equals(destination)) {
                origins.add(transport);
            } else if (LOCATION_PERMUTATION.equals(origin)
                    && destination != null && !LOCATION_PERMUTATION.equals(destination)) {
                destinations.add(transport);
            }
            if (!LOCATION_PERMUTATION.equals(origin)
                    && destination != null
                    && !LOCATION_PERMUTATION.equals(destination)
                    && (origin == null || !origin.equals(destination))) {
                putTransport(transports, transport, false);
            }
        }

        for (Transport origin : origins) {
            for (Transport destination : destinations) {
                if (origin.getOrigin().distanceTo2D(destination.getDestination()) > radiusThreshold) {
                    putTransport(transports, new Transport(origin, destination), false);
                }
            }
        }
    }

    private static void putTransport(
            Map<WorldPoint, Set<Transport>> transports,
            Transport transport,
            boolean replaceExisting) {
        Set<Transport> atOrigin = transports.computeIfAbsent(transport.getOrigin(), ignored -> new LinkedHashSet<>());
        if (replaceExisting) {
            Transport pinnedShortcut = atOrigin.stream()
                    .filter(existing -> existing.getSource().startsWith("Skretzo/shortest-path@"))
                    .filter(existing -> existing.getType() == TransportType.TRANSPORT
                            || existing.getType() == TransportType.AGILITY_SHORTCUT)
                    .filter(existing -> transport.getType() == TransportType.AGILITY_SHORTCUT
                            || transport.getType() == TransportType.GRAPPLE_SHORTCUT)
                    .filter(existing -> sameAutomationIdentity(existing, transport))
                    .findFirst()
                    .orElse(null);
            if (pinnedShortcut != null) {
                // Upstream deliberately keeps many traversals in transports.tsv.
                // Reuse Microbot's more specific executor classification without
                // replacing the canonical requirements and metadata.
                pinnedShortcut.type = transport.getType();
                return;
            }
            boolean pinnedBaseOwnsIdentity = atOrigin.stream()
                    .anyMatch(existing -> existing.getSource().startsWith("Skretzo/shortest-path@")
                            && sameCatalogIdentity(existing, transport));
            if (pinnedBaseOwnsIdentity) {
                // The checked-in Microbot TSVs predate the canonical schema. Keep their
                // genuinely local rows, but do not let a stale duplicate erase quantities,
                // item alternatives, region overrides, or corrected upstream metadata.
                return;
            }
            atOrigin.removeIf(existing -> sameCatalogIdentity(existing, transport));
        }
        atOrigin.add(transport);
    }

    private static boolean sameCatalogIdentity(Transport left, Transport right) {
        return left.getType() == right.getType()
                && Objects.equals(left.getOrigin(), right.getOrigin())
                && Objects.equals(left.getDestination(), right.getDestination())
                && Objects.equals(left.getDisplayInfo(), right.getDisplayInfo());
    }

    private static boolean sameAutomationIdentity(Transport left, Transport right) {
        return Objects.equals(left.getOrigin(), right.getOrigin())
                && Objects.equals(left.getDestination(), right.getDestination())
                && left.getObjectId() == right.getObjectId()
                && Objects.equals(left.getAction(), right.getAction())
                && Objects.equals(left.getName(), right.getName());
    }

    private static void refinePinnedShortcutTypes(Map<WorldPoint, Set<Transport>> transports) {
        Map<Integer, List<Transport>> localShortcutsByObjectId = new HashMap<>();
        transports.values().stream()
                .flatMap(Set::stream)
                .filter(transport -> "microbot".equals(transport.getSource()))
                .filter(transport -> transport.getType() == TransportType.AGILITY_SHORTCUT
                        || transport.getType() == TransportType.GRAPPLE_SHORTCUT)
                .forEach(transport -> localShortcutsByObjectId
                        .computeIfAbsent(transport.getObjectId(), ignored -> new ArrayList<>())
                        .add(transport));

        transports.values().stream()
                .flatMap(Set::stream)
                .filter(transport -> transport.getSource().startsWith("Skretzo/shortest-path@"))
                .filter(transport -> transport.getType() == TransportType.TRANSPORT
                        || transport.getType() == TransportType.AGILITY_SHORTCUT)
                .forEach(pinned -> localShortcutsByObjectId
                        .getOrDefault(pinned.getObjectId(), Collections.emptyList())
                        .stream()
                        .filter(local -> Objects.equals(pinned.getAction(), local.getAction()))
                        .filter(local -> Objects.equals(pinned.getName(), local.getName()))
                        .filter(local -> pinned.getOrigin() != null && pinned.getDestination() != null
                                && local.getOrigin() != null && local.getDestination() != null)
                        .filter(local -> pinned.getOrigin().distanceTo2D(local.getOrigin()) <= 1
                                && pinned.getDestination().distanceTo2D(local.getDestination()) <= 1)
                        .findFirst()
                        .ifPresent(local -> pinned.type = local.getType()));
    }

    private static void appendPinnedUpstreamTransportFiles(HashMap<WorldPoint, Set<Transport>> transports) {
        addCanonicalTransports(transports, "transports.tsv", TransportType.TRANSPORT, "TRANSPORT", 0);
        addCanonicalTransports(transports, "agility_shortcuts.tsv", TransportType.AGILITY_SHORTCUT, "AGILITY_SHORTCUT", 0);
        addCanonicalTransports(transports, "boats.tsv", TransportType.BOAT, "BOAT", 0);
        addCanonicalTransports(transports, "canoes.tsv", TransportType.CANOE, "CANOE", 0);
        addCanonicalTransports(transports, "charter_ships.tsv", TransportType.CHARTER_SHIP, "CHARTER_SHIP", 0);
        addCanonicalTransports(transports, "ships.tsv", TransportType.SHIP, "SHIP", 0);
        addCanonicalTransports(transports, "fairy_rings.tsv", TransportType.FAIRY_RING, "FAIRY_RING", 6);
        addCanonicalTransports(transports, "gnome_gliders.tsv", TransportType.GNOME_GLIDER, "GNOME_GLIDER", 6);
        addCanonicalTransports(transports, "hot_air_balloons.tsv", TransportType.HOT_AIR_BALLOON, "HOT_AIR_BALLOON", 7);
        addCanonicalTransports(transports, "magic_carpets.tsv", TransportType.MAGIC_CARPET, "MAGIC_CARPET", 0);
        addCanonicalTransports(transports, "magic_mushtrees.tsv", TransportType.MAGIC_MUSHTREE, "MAGIC_MUSHTREE", 5);
        addCanonicalTransports(transports, "minecarts.tsv", TransportType.MINECART, "MINECART", 0);
        addCanonicalTransports(transports, "quetzals.tsv", TransportType.QUETZAL, "QUETZAL", 5);
        addCanonicalTransports(transports, "quetzal_whistle.tsv", TransportType.TELEPORTATION_ITEM, "QUETZAL_WHISTLE", 0);
        addCanonicalTransports(transports, "seasonal_transports.tsv", TransportType.SEASONAL_TRANSPORT, "SEASONAL_TRANSPORTS", 0);
        addCanonicalTransports(transports, "spirit_trees.tsv", TransportType.SPIRIT_TREE, "SPIRIT_TREE", 5);
        addCanonicalTransports(transports, "teleportation_boxes.tsv", TransportType.TELEPORTATION_PORTAL, "TELEPORTATION_BOX", 0);
        addCanonicalTransports(transports, "teleportation_items.tsv", TransportType.TELEPORTATION_ITEM, "TELEPORTATION_ITEM", 0);
        addCanonicalTransports(transports, "teleportation_levers.tsv", TransportType.TELEPORTATION_LEVER, "TELEPORTATION_LEVER", 0);
        addCanonicalTransports(transports, "teleportation_minigames.tsv", TransportType.TELEPORTATION_MINIGAME, "TELEPORTATION_MINIGAME", 0);
        addCanonicalTransports(transports, "teleportation_portals.tsv", TransportType.TELEPORTATION_PORTAL, "TELEPORTATION_PORTAL", 0);
        addCanonicalTransports(transports, "teleportation_portals_poh.tsv", TransportType.TELEPORTATION_PORTAL, "TELEPORTATION_PORTAL_POH", 0);
        addCanonicalTransports(transports, "teleportation_spells.tsv", TransportType.TELEPORTATION_SPELL, "TELEPORTATION_SPELL", 0);
        addCanonicalTransports(transports, "teleportation_spells_home.tsv", TransportType.TELEPORTATION_SPELL, "TELEPORTATION_SPELL_HOME", 0);
        addCanonicalTransports(transports, "wilderness_obelisks.tsv", TransportType.WILDERNESS_OBELISK, "WILDERNESS_OBELISK", 0);
    }

    private static void appendStandardTransportFiles(HashMap<WorldPoint, Set<Transport>> transports) {
        addTransports(transports, "transports.tsv", TransportType.TRANSPORT);
        addTransports(transports, "agility_shortcuts.tsv", TransportType.AGILITY_SHORTCUT);
        addTransports(transports, "boats.tsv", TransportType.BOAT);
        addTransports(transports, "canoes.tsv", TransportType.CANOE);
        addTransports(transports, "charter_ships.tsv", TransportType.CHARTER_SHIP);
        addTransports(transports, "ships.tsv", TransportType.SHIP);
        addTransports(transports, "fairy_rings.tsv", TransportType.FAIRY_RING);
        addTransports(transports, "gnome_gliders.tsv", TransportType.GNOME_GLIDER, 6);
        addTransports(transports, "minecarts.tsv", TransportType.MINECART);
        addTransports(transports, "spirit_trees.tsv", TransportType.SPIRIT_TREE, 5);
        addTransports(transports, "quetzals.tsv", TransportType.QUETZAL, 6);
        addTransports(transports, "teleportation_items.tsv", TransportType.TELEPORTATION_ITEM);
        addTransports(transports, "teleportation_minigames.tsv", TransportType.TELEPORTATION_MINIGAME);
        addTransports(transports, "teleportation_levers.tsv", TransportType.TELEPORTATION_LEVER);
        addTransports(transports, "teleportation_portals.tsv", TransportType.TELEPORTATION_PORTAL);
        addTransports(transports, "teleportation_spells.tsv", TransportType.TELEPORTATION_SPELL);
        addTransports(transports, "wilderness_obelisks.tsv", TransportType.WILDERNESS_OBELISK);
        addTransports(transports, "magic_carpets.tsv", TransportType.MAGIC_CARPET);
        addTransports(transports, "hot_air_balloons.tsv", TransportType.HOT_AIR_BALLOON, 7);
        addTransports(transports, "magic_mushtrees.tsv", TransportType.MAGIC_MUSHTREE, 5);
        addTransports(transports, "seasonal_transports.tsv", TransportType.SEASONAL_TRANSPORT);
        addTransports(transports, "npcs.tsv", TransportType.NPC);
    }

    public static HashMap<WorldPoint, Set<Transport>> loadAllFromResources() {
        HashMap<WorldPoint, Set<Transport>> transports = new HashMap<>();
        appendPinnedUpstreamTransportFiles(transports);
        appendStandardTransportFiles(transports);
        refinePinnedShortcutTypes(transports);
        log.info("Loaded transport catalog with {} origins", transports.size());
        return transports;
    }

    /**
     * Reload transport TSVs from the plugin classpath (same as {@link #loadAllFromResources()}).
     * Apply to {@link PathfinderConfig} via {@link ShortestPathPlugin} config hot-reload.
     */
    public static HashMap<WorldPoint, Set<Transport>> reloadFromResources() {
        return loadAllFromResources();
    }

    // To string method for debugging
    @Override
    public String toString() {
        return "Transport{" +
                "action='" + action + '\'' +
                ", objectId=" + objectId +
                ", name='" + name + '\'' +
                ", origin=" + origin +
                ", destination=" + destination +
                ", skillLevels=" + Arrays.toString(skillLevels) +
                ", quests=" + quests +
                ", itemIdRequirements=" + itemIdRequirements +
                ", type=" + type +
                ", duration=" + duration +
                ", displayInfo='" + displayInfo + '\'' +
                ", isConsumable=" + isConsumable +
                ", maxWildernessLevel=" + maxWildernessLevel +
                ", varbits=" + varbits +
                ", varplayers=" + varplayers +
                ", currencyName='" + currencyName + '\'' +
                ", currencyAmount=" + currencyAmount +
                ", isMembers=" + isMembers +
                '}';
    }
}
