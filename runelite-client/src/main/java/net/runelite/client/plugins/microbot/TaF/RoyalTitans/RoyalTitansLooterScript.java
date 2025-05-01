package net.runelite.client.plugins.microbot.TaF.RoyalTitans;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.grounditem.LootingParameters;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static net.runelite.client.plugins.microbot.TaF.RoyalTitans.RoyalTitansShared.*;

public class RoyalTitansLooterScript extends Script {
    @Inject
    public RoyalTitansScript royalTitansScript;
    public String itemsToLoot = "Zenyte shard,Ballista spring,Ballista limbs,Ballista frame,Monkey tail,Heavy frame,Light frame,Rune platelegs,Rune plateskirt,Rune chainbody,Dragon scimitar,Law rune,Death rune,Runite bolts,Grimy kwuarm,Grimy cadantine,Grimy dwarf weed,Grimy lantadyme,Ranarr seed,Snapdragon seed,Torstol seed,Yew seed,Magic seed,Palm tree seed,Spirit seed,Dragonfruit tree seed,Celastrus seed,Redwood tree seed,Prayer potion(3),Shark,Coins,Saradomin brew(2),Rune javelin heads,Dragon javelin heads,Adamantite bar,Diamond,Runite bar";
    int minFreeSlots = 0;
    private RoyalTitansConfig.RoyalTitan LootedTitan = null;

    public RoyalTitansLooterScript() {

    }

    public boolean run(RoyalTitansConfig config, RoyalTitansScript script) {
        royalTitansScript = script;
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!super.run()) return;
                if (!Microbot.isLoggedIn()) return;
                if (royalTitansScript.state.equals(BotStatus.BANKING) || royalTitansScript.state.equals(BotStatus.TRAVELLING) || royalTitansScript.state.equals(BotStatus.WAITING))
                    return;
                if (Rs2Inventory.isFull() || Rs2Inventory.getEmptySlots() <= minFreeSlots) return;
                if (!isInBossRegion()) return;
                var iceTitanDead = Rs2Npc.getNpcs(ICE_TITAN_DEAD_ID).findFirst().orElse(null);
                var fireTitanDead = Rs2Npc.getNpcs(FIRE_TITAN_DEAD_ID).findFirst().orElse(null);
                var iceTitan = Rs2Npc.getNpcs(ICE_TITAN_ID).findFirst().orElse(null);
                var fireTitan = Rs2Npc.getNpcs(FIRE_TITAN_ID).findFirst().orElse(null);
                boolean looted = false;
                // Only loot when the giants are dead to not obstruct the fight
                if (iceTitan != null && !iceTitan.isDead() || fireTitan != null && !fireTitan.isDead()) {
                    return;
                }
                // Both titans are dead and we have looted - Check ground for items
                if (fireTitanDead != null && iceTitanDead != null) {
                    royalTitansScript.subState = "Looting ground items";
                    lootItemsOnName(config);
                    lootRunes(config);
                    lootCoins(config);
                    lootUntradeableItems(config);
                    return;
                }
                if (LootedTitanLastIteration) {
                    return;
                }
                royalTitansScript.subState = "Handling looting from Titans";

                switch (config.loot()) {
                    case ICE_TITAN:
                        looted = Rs2Npc.interact(iceTitanDead, "Loot");
                        break;
                    case FIRE_TITAN:
                        looted = Rs2Npc.interact(fireTitanDead, "Loot");
                        break;
                    case ALTERNATE:
                        if (LootedTitan == null) {
                            LootedTitan = RoyalTitansConfig.RoyalTitan.ICE_TITAN;
                        } else if (LootedTitan == RoyalTitansConfig.RoyalTitan.ICE_TITAN) {
                            LootedTitan = RoyalTitansConfig.RoyalTitan.FIRE_TITAN;
                        } else {
                            LootedTitan = RoyalTitansConfig.RoyalTitan.ICE_TITAN;
                        }
                        if (LootedTitan == RoyalTitansConfig.RoyalTitan.ICE_TITAN) {
                            looted = Rs2Npc.interact(iceTitanDead, "Loot");
                        } else {
                            looted = Rs2Npc.interact(fireTitanDead, "Loot");
                        }
                        break;
                    case RANDOM:
                        if (Math.random() < 0.5) {
                            looted = Rs2Npc.interact(iceTitanDead, "Loot");
                        } else {
                            looted = Rs2Npc.interact(fireTitanDead, "Loot");
                        }
                        break;
                }
                if (looted) {
                    royalTitansScript.kills++;
                    LootedTitanLastIteration = true;
                    evaluateAndConsumePotions(config);
                }

            } catch (Exception ex) {
                System.out.println("Royal Titan Looter: " + ex.getMessage());
                ex.printStackTrace();
            }

        }, 0, 200, TimeUnit.MILLISECONDS);
        return true;
    }

    private boolean isInBossRegion() {
        return Rs2Player.getWorldLocation().getRegionID() == BOSS_REGION;
    }

    private void lootUntradeableItems(RoyalTitansConfig config) {
        LootingParameters untradeableItemsParams = new LootingParameters(
                15,
                1,
                1,
                minFreeSlots,
                false,
                false,
                "untradeable"
        );
        if (Rs2GroundItem.lootUntradables(untradeableItemsParams)) {
            Microbot.pauseAllScripts = false;
        }
    }

    private void lootRunes(RoyalTitansConfig config) {
        LootingParameters runesParams = new LootingParameters(
                15,
                1,
                1,
                minFreeSlots,
                false,
                false,
                " rune"
        );
        if (Rs2GroundItem.lootItemsBasedOnNames(runesParams)) {
            Microbot.pauseAllScripts = false;
        }
    }

    private void lootCoins(RoyalTitansConfig config) {
        LootingParameters coinsParams = new LootingParameters(
                15,
                1,
                1,
                minFreeSlots,
                false,
                false,
                "coins"
        );
        if (Rs2GroundItem.lootCoins(coinsParams)) {
            Microbot.pauseAllScripts = false;
        }
    }

    private void lootItemsOnName(RoyalTitansConfig config) {
        LootingParameters valueParams = new LootingParameters(
                15,
                1,
                1,
                minFreeSlots,
                false,
                false,
                itemsToLoot.trim().split(",")
        );
        if (Rs2GroundItem.lootItemsBasedOnNames(valueParams)) {
            Microbot.pauseAllScripts = false;
        }
    }
}
