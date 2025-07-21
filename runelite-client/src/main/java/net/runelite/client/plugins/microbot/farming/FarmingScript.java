/*
 * Copyright (c) 2025, George M <https://github.com/g-mason0> + TaF <https://github.com/SteffenCarlsen>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice, this
 *     list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.microbot.farming;

import com.google.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameObject;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.questhelper.helpers.mischelpers.farmruns.CropState;
import net.runelite.client.plugins.microbot.questhelper.helpers.mischelpers.farmruns.FarmingPatch;
import net.runelite.client.plugins.microbot.questhelper.helpers.mischelpers.farmruns.FarmingRegion;
import net.runelite.client.plugins.microbot.util.Rs2InventorySetup;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;

@Slf4j
public class FarmingScript extends Script
{
	private final FarmingPlugin plugin;
	private final FarmingConfig config;
	private Rs2InventorySetup farmingInventorySetup;
	private boolean doesEquipmentMatch;
	private boolean doesInventoryMatch;
	private FarmingRegion currentFarmingRegion;
	private FarmingPatch currentFarmingPatch;

	@Getter
	FarmingScriptState state;

	@Inject
	public FarmingScript(FarmingPlugin plugin, FarmingConfig config)
	{
		this.plugin = plugin;
		this.config = config;
	}

	public boolean run() {
		mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
			try {
				if (!super.run()) return;
				if (!Microbot.isLoggedIn()) return;
				preFlightChecks();

				if (plugin.getStopCondition().isSatisfied())
				{
					Rs2Bank.walkToBank();
					plugin.reportFinished("Stop condition satisfied, stopping script.", true);
					return;
				}

				if (state == null) return;

				switch (state) {
					case START:
						if (!canWeFarm()) return;
						state = FarmingScriptState.TRAVEL;
						break;

					case BANK:
						break;

					case TRAVEL:
						// TODO: Implement shortest patch logic to get to the next patch
						// TODO: Travel should ensure that we set currentFarmingPatch to the next patch we need to farm
						break;

					case FARM:
						updatePatchAndRegion();
						if (currentFarmingPatch == null || currentFarmingRegion == null) return;

						farmCurrentPatch();
						break;
				}


			} catch (Exception e) {
				log.error("Error in {}:", getClass().getSimpleName(), e);
			}
		}, 0, 600, TimeUnit.MILLISECONDS);
		return true;
	}

	private void farmCurrentPatch() {
		var patchObject = getPatchObject(currentFarmingPatch);
		if (patchObject == null) {
			log.error("No patch object found for the current farming patch.");
			shutdown(); // TODO: Stop the script gracefully
			return;
		}
		log.debug("Found patch object: {}", currentFarmingPatch.getName());
		var currentPatchState = plugin.getPatchStateMap().get(currentFarmingPatch);

		switch (currentPatchState) {
			case EMPTY:
			case GROWING:
			case DEAD:
			case UNCHECKED:
			case HARVESTABLE:
			case FILLING:
			case DISEASED:
		}
	}

	private boolean canWeFarm() {
		if (config.useSeedVault()) {
			Rs2Bank.walkToBank(BankLocation.FARMING_GUILD);
			// TODO handle seed vault logic
		} else {
			Rs2Bank.walkToBank();
			if (!farmingInventorySetup.doesInventoryMatch() || !farmingInventorySetup.doesEquipmentMatch()) {
				var loadedEquipment = farmingInventorySetup.loadEquipment();
				var loadedInventory = farmingInventorySetup.loadInventory();

				if (!loadedEquipment || !loadedInventory) {
					plugin.reportFinished("Failed to load inventory or equipment setup. Please check your configuration.", false);
					return false;
				}

				doesEquipmentMatch = farmingInventorySetup.doesEquipmentMatch();
				doesInventoryMatch = farmingInventorySetup.doesInventoryMatch();
			}
		}
		return true;
	}

	/**
	 * Performs pre-flight checks to ensure the script can run properly.
	 * 1. Inventory setup check
	 * 2. Farming patch availability check
	 */
	private void preFlightChecks() {
		// Inventory setup check
		if (config.inventorySetup() == null) {
			plugin.reportFinished("InventorySetup is null. Please ensure that you have configured your inventory setup in the plugin settings. If a value is set, try to reselect it", false);
			//TODO: Stop the script gracefully
			return;
		}
		farmingInventorySetup = new Rs2InventorySetup(config.inventorySetup(), mainScheduledFuture);
		log.debug("Farming inventory setup has been successfully initialized.");

		// Farming patch availability check
		plugin.update();
		var patchesToVisit = plugin.getPatchesNeedingAttention();
		if (patchesToVisit.isEmpty()) {
			plugin.reportFinished("No farming patches available for farming.", false);
			return;
		}
		log.debug("Found {} farming patches needing attention.", patchesToVisit.size());
	}

	/**
	 * Finds the closest {@link GameObject} representing a farming patch within the area defined by the given {@link FarmingPatch}.
	 *
	 * @param patch the {@link FarmingPatch} to search for; may be {@code null}
	 * @return the closest {@link GameObject} in the patch area, or {@code null} if none is found or if the patch is {@code null}
	 */
	private GameObject getPatchObject(FarmingPatch patch) {
		if (patch == null) {
			log.warn("Patch is null, cannot get patch object.");
			return null;
		}

		List<Integer> objectIds = Rs2GameObject.getObjectIdsByName("Patch");

		List<GameObject> objects = Rs2GameObject.getGameObjects(o -> {
			if (!objectIds.isEmpty() && !objectIds.contains(o.getId())) return false;

			return patch.getPatchArea().contains(o.getWorldLocation().getX(), o.getWorldLocation().getY());
		});

		Optional<GameObject> closestObject = Rs2GameObject.pickClosest(objects, GameObject::getWorldLocation, patch.getLocation());

		if (closestObject.isPresent()) {
			return closestObject.get();
		} else {
			log.warn("No objects found in the specified area.");
			return null;
		}
	}

	/**
     * Updates the current farming region and patch based on the state of patches needing attention.
     * <p>
     * Logic:
     * <ul>
     *   <li>If no patches need attention, clears the current region and patch.</li>
     *   <li>If no region is set, initializes the region and patch to the first needing attention.</li>
     *   <li>If all patches in the current region are growing, clears the current region and patch.</li>
     *   <li>If the current patch no longer needs attention, switches to the next patch in the region needing attention.</li>
     *   <li>Otherwise, keeps the current patch as is.</li>
     * </ul>
     * This method should be called regularly to ensure the script targets the correct region and patch.
     */
    private void updatePatchAndRegion() {
		Map<FarmingPatch, CropState> patchesNeedingAttention = plugin.getPatchesNeedingAttention();
		if (patchesNeedingAttention.isEmpty()) {
			log.debug("No patches need attention.");
			clearCurrentRegionAndPatch();
			return;
		}

		if (currentFarmingRegion == null) {
			initializeRegionAndPatch(patchesNeedingAttention);
			return;
		}

		if (areAllPatchesGrowing(currentFarmingRegion, patchesNeedingAttention)) {
			log.debug("All patches in current region are growing. Clearing current region and patch.");
			clearCurrentRegionAndPatch();
			return;
		}

		if (shouldSwitchPatch(patchesNeedingAttention)) {
			switchToNextPatch(patchesNeedingAttention);
		} else {
			log.debug("Current patch still needs attention, not switching.");
		}
	}

	private void initializeRegionAndPatch(Map<FarmingPatch, CropState> patchesNeedingAttention) {
		for (FarmingPatch patch : patchesNeedingAttention.keySet()) {
			currentFarmingRegion = patch.getRegion();
			currentFarmingPatch = patch;
			log.debug("Initialized farming region: {} and patch: {}", currentFarmingRegion.getName(), currentFarmingPatch.getName());
			break;
		}
	}

	private boolean areAllPatchesGrowing(FarmingRegion region, Map<FarmingPatch, CropState> patchesNeedingAttention) {
		return plugin.getPatchStateMap().keySet().stream()
			.filter(patch -> Objects.equals(patch.getRegion(), region))
			.noneMatch(patchesNeedingAttention::containsKey);
	}

	private boolean shouldSwitchPatch(Map<FarmingPatch, CropState> patchesNeedingAttention) {
		return currentFarmingPatch == null || !patchesNeedingAttention.containsKey(currentFarmingPatch);
	}

	private void switchToNextPatch(Map<FarmingPatch, CropState> patchesNeedingAttention) {
		Optional<FarmingRegion> nextRegion = patchesNeedingAttention.keySet().stream()
			.map(FarmingPatch::getRegion)
			.filter(Objects::nonNull)
			.filter(region -> patchesNeedingAttention.keySet().stream().anyMatch(patch -> Objects.equals(patch.getRegion(), region)))
			.findFirst();

		if (nextRegion.isPresent()) {
			currentFarmingRegion = nextRegion.get();
			currentFarmingPatch = patchesNeedingAttention.keySet().stream()
				.filter(patch -> Objects.equals(patch.getRegion(), currentFarmingRegion))
				.findFirst().orElse(null);
			log.debug("Switched to region: {} and patch: {} needing attention.",
				currentFarmingRegion.getName(),
				currentFarmingPatch != null ? currentFarmingPatch.getName() : "none");
		} else {
			clearCurrentRegionAndPatch();
			log.debug("No region or patch needing attention found.");
		}
	}

	private void clearCurrentRegionAndPatch() {
		currentFarmingRegion = null;
		currentFarmingPatch = null;
	}
}
