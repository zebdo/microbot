package net.runelite.client.plugins.microbot.shortestpath;

import net.runelite.api.ItemID;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.plugins.microbot.shortestpath.testing.HeadlessTransportHarness;
import net.runelite.client.plugins.microbot.shortestpath.testing.HeadlessTransportState;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static net.runelite.client.plugins.microbot.shortestpath.testing.HeadlessTransportHarness.Rejection.ITEMS;
import static net.runelite.client.plugins.microbot.shortestpath.testing.HeadlessTransportHarness.Rejection.NETWORK_ACCESS;
import static net.runelite.client.plugins.microbot.shortestpath.testing.HeadlessTransportHarness.Rejection.QUEST;
import static net.runelite.client.plugins.microbot.shortestpath.testing.HeadlessTransportHarness.Rejection.SKILL;
import static net.runelite.client.plugins.microbot.shortestpath.testing.HeadlessTransportHarness.Rejection.VARBIT;
import static net.runelite.client.plugins.microbot.shortestpath.testing.HeadlessTransportHarness.Rejection.VARPLAYER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class HeadlessTransportHarnessTest
{
	private static HeadlessTransportHarness harness;

	@BeforeClass
	public static void loadResources()
	{
		harness = HeadlessTransportHarness.loadResources();
	}

	@Test
	public void syntheticSkillsVarbitsAndInventoryGateCanonicalSpell()
	{
		Transport varrockTeleport = find(
			"TELEPORTATION_SPELL",
			"Varrock Teleport",
			new WorldPoint(3213, 3424, 0));

		HeadlessTransportState lowMagic = varrockSpellState()
			.skill(Skill.MAGIC, 24)
			.item(ItemID.AIR_RUNE, 3)
			.item(ItemID.FIRE_RUNE, 1)
			.item(ItemID.LAW_RUNE, 1)
			.build();
		assertTrue(harness.evaluate(varrockTeleport, lowMagic).getRejections().contains(SKILL));

		HeadlessTransportState missingRunes = varrockSpellState().build();
		assertTrue(harness.evaluate(varrockTeleport, missingRunes).getRejections().contains(ITEMS));

		HeadlessTransportState wrongSpellbook = varrockSpellState()
			.varbit(4070, 1)
			.item(ItemID.AIR_RUNE, 3)
			.item(ItemID.FIRE_RUNE, 1)
			.item(ItemID.LAW_RUNE, 1)
			.build();
		assertTrue(harness.evaluate(varrockTeleport, wrongSpellbook).getRejections().contains(VARBIT));

		HeadlessTransportState ready = varrockSpellState()
			.item(ItemID.AIR_RUNE, 3)
			.item(ItemID.FIRE_RUNE, 1)
			.item(ItemID.LAW_RUNE, 1)
			.build();
		assertTrue(harness.evaluate(varrockTeleport, ready).isAllowed());
	}

	@Test
	public void syntheticQuestAndVarplayerGateQuetzalPlatform()
	{
		Transport colosseumWhistle = find(
			"QUETZAL_WHISTLE",
			"Quetzal whistle: Fortis Colosseum",
			new WorldPoint(1779, 3111, 0));

		HeadlessTransportState locked = HeadlessTransportState.builder()
			.allSkills(99)
			.item(33120, 1)
			.build();
		assertTrue(harness.evaluate(colosseumWhistle, locked).getRejections().contains(QUEST));
		assertTrue(harness.evaluate(colosseumWhistle, locked).getRejections().contains(VARPLAYER));

		HeadlessTransportState missingPlatform = HeadlessTransportState.builder()
			.allSkills(99)
			.quest(Quest.TWILIGHTS_PROMISE, QuestState.FINISHED)
			.item(33120, 1)
			.build();
		assertFalse(harness.evaluate(colosseumWhistle, missingPlatform).getRejections().contains(QUEST));
		assertTrue(harness.evaluate(colosseumWhistle, missingPlatform).getRejections().contains(VARPLAYER));

		HeadlessTransportState unlocked = HeadlessTransportState.builder()
			.allSkills(99)
			.quest(Quest.TWILIGHTS_PROMISE, QuestState.FINISHED)
			.varplayer(4182, 256)
			.item(33120, 1)
			.build();
		assertTrue(harness.evaluate(colosseumWhistle, unlocked).isAllowed());
	}

	@Test
	public void syntheticNetworkUnlockMatchesLiveFairyRingRules()
	{
		Transport fairyRing = harness.find(transport ->
			transport.getType() == TransportType.FAIRY_RING
				&& transport.getQuests().isEmpty())
			.get(0);

		HeadlessTransportState locked = HeadlessTransportState.builder()
			.allSkills(99)
			.build();
		assertTrue(harness.evaluate(fairyRing, locked).getRejections().contains(NETWORK_ACCESS));

		HeadlessTransportState staffAccess = HeadlessTransportState.builder()
			.allSkills(99)
			.quest(Quest.FAIRYTALE_II__CURE_A_QUEEN, QuestState.IN_PROGRESS)
			.item(ItemID.DRAMEN_STAFF, 1)
			.build();
		assertFalse(harness.evaluate(fairyRing, staffAccess).getRejections().contains(NETWORK_ACCESS));

		HeadlessTransportState diaryAccess = HeadlessTransportState.builder()
			.allSkills(99)
			.quest(Quest.FAIRYTALE_II__CURE_A_QUEEN, QuestState.IN_PROGRESS)
			.varbit(VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE, 1)
			.build();
		assertFalse(harness.evaluate(fairyRing, diaryAccess).getRejections().contains(NETWORK_ACCESS));
	}

	@Test
	public void realCollisionMapSelectsCrossPlaneTransportAndRecordsObjectIntent()
	{
		WorldPoint origin = new WorldPoint(3029, 3217, 0);
		WorldPoint destination = new WorldPoint(3032, 3217, 1);
		HeadlessTransportState state = HeadlessTransportState.builder()
			.allSkills(99)
			.allQuests(QuestState.FINISHED)
			.build();

		HeadlessTransportHarness.RouteResult result = harness.route(
			state,
			origin,
			destination,
			transport -> origin.equals(transport.getOrigin())
				&& destination.equals(transport.getDestination()));

		assertTrue(result.isReached());
		assertEquals(destination, result.getPath().get(result.getPath().size() - 1));
		assertEquals(1, result.getSelectedTransports().size());
		assertEquals(1, result.getExecutionIntents().size());

		Transport selected = result.getSelectedTransports().get(0);
		TransportExecutionRegistry.ExecutionIntent intent = result.getExecutionIntents().get(0);
		assertSame(selected, result.getRouteSteps().get(1).getTransportFromPrevious());
		assertEquals(TransportExecutionRegistry.Executor.OBJECT, intent.getExecutor());
		assertEquals(2083, intent.getObjectId());
		assertEquals("Cross", intent.getAction());
		assertEquals("Gangplank", intent.getTarget());
		assertEquals(origin, intent.getOrigin());
		assertEquals(destination, intent.getDestination());
	}

	private static HeadlessTransportState.Builder varrockSpellState()
	{
		return HeadlessTransportState.builder()
			.allSkills(99)
			.varbit(4070, 0);
	}

	private static Transport find(String catalogType, String displayInfo, WorldPoint destination)
	{
		List<Transport> matches = harness.find(transport ->
			catalogType.equals(transport.getCatalogType())
				&& displayInfo.equals(transport.getDisplayInfo())
				&& destination.equals(transport.getDestination()));
		assertEquals("expected one catalog match", 1, matches.size());
		return matches.get(0);
	}
}
