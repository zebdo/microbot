package net.runelite.client.plugins.microbot.aiofighter.combat;

import net.runelite.api.EquipmentInventorySlot;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.aiofighter.AIOFighterConfig;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.misc.SpecialAttackWeaponEnum;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static net.runelite.client.plugins.microbot.util.Global.sleep;

public class UseSpecialAttackScript extends Script {

    private List<Rs2ItemModel> savedEquipment = null;

    public boolean run(AIOFighterConfig config) {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;
                if (!config.useSpecialAttack()) return;
                if (Rs2Equipment.isWearingFullGuthan()) return;
                
                String customSpecWeapon = config.customSpecWeapon().trim();
                
                // If no custom spec weapon configured, use default behavior
                if (customSpecWeapon.isEmpty()) {
                    if (Rs2Player.isInteracting()) {
                        Microbot.getSpecialAttackConfigs().useSpecWeapon();
                    }
                    return;
                }
                
                // Only proceed if we're in combat
                if (!Rs2Combat.inCombat() && !Rs2Player.isInteracting()) return;
                
                int specCost = config.specAttackCost() * 10;
                int currentSpecEnergy = Rs2Combat.getSpecEnergy();
                
                // Check if spec weapon exists in inventory or equipped
                boolean hasSpecWeapon = Rs2Inventory.hasItem(customSpecWeapon) || 
                                       Rs2Equipment.isWearing(customSpecWeapon);
                if (!hasSpecWeapon) return;
                
                boolean isSpecWeaponEquipped = Rs2Equipment.isWearing(customSpecWeapon);
                
                if (currentSpecEnergy >= specCost && !isSpecWeaponEquipped) {
                    // Switch to spec weapon
                    
                    // Check 2H constraints first - need at least 1 free slot for shield
                    boolean is2H = isSpecWeapon2H(customSpecWeapon);
                    if (is2H && Rs2Equipment.isWearingShield() && Rs2Inventory.emptySlotCount() < 1) {
                        return; // Cannot equip 2H weapon without space for shield
                    }
                    
                    // Save equipment only after we know we can switch
                    if (savedEquipment == null) {
                        savedEquipment = new ArrayList<>(Rs2Equipment.items());
                    }
                    
                    Rs2Inventory.wear(customSpecWeapon);
                    sleep(600);
                    
                } else if (currentSpecEnergy < specCost && isSpecWeaponEquipped && savedEquipment != null) {
                    // Switch back to original equipment
                    restoreOriginalEquipment();
                }
                
                // Use special attack if conditions are met
                if (isSpecWeaponEquipped && currentSpecEnergy >= specCost) {
                    Rs2Combat.setSpecState(true, specCost);
                }
                
            } catch (Exception ex) {
                Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        return true;
    }
    
    private boolean isSpecWeapon2H(String weaponName) {
        String lowerName = weaponName.toLowerCase();
        for (SpecialAttackWeaponEnum weapon : SpecialAttackWeaponEnum.values()) {
            if (lowerName.contains(weapon.getName())) {
                return weapon.is2H();
            }
        }
        // Default to checking common 2H patterns if not in enum
        return lowerName.contains("2h") || lowerName.contains("godsword") || 
               lowerName.contains("halberd") || lowerName.contains("bow");
    }
    
    private void restoreOriginalEquipment() {
        if (savedEquipment == null) return;
        
        Rs2ItemModel originalWeapon = savedEquipment.stream()
                .filter(item -> item.getSlot() == EquipmentInventorySlot.WEAPON.getSlotIdx())
                .findFirst().orElse(null);
        
        Rs2ItemModel originalShield = savedEquipment.stream()
                .filter(item -> item.getSlot() == EquipmentInventorySlot.SHIELD.getSlotIdx())
                .findFirst().orElse(null);
        
        // Re-equip weapon first (important for 2H -> 1H+Shield transitions)
        if (originalWeapon != null && Rs2Inventory.hasItem(originalWeapon.getName())) {
            Rs2Inventory.wear(originalWeapon.getName());
            sleep(600);
        }
        
        // Then re-equip shield if we had one (2H spec weapon would have unequipped it)
        if (originalShield != null && Rs2Inventory.hasItem(originalShield.getName())) {
            Rs2Inventory.wear(originalShield.getName());
            sleep(600);
        }
        
        savedEquipment = null;
    }

    public void shutdown() {
        savedEquipment = null;
        super.shutdown();
    }
}