/*
 * Copyright (C) 2020 Bryan Larson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package codes.goblom.encumbered;

import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 *
 * @author Bryan Larson
 */
public class EncumberedPlayer {
    
    @Getter
    private final UUID uuid;
    
    @Getter
    private double maxCarryWeight;
    
    protected EncumberedPlayer(UUID id) {
        this.uuid = id;
        
        this.maxCarryWeight = EncumberedPlugin.instance.overrides.getDouble(uuid.toString(), Encumbered.defaultMaxCarryWeight);
    }
    
    public void setCustomMaxCarryWeight(double amount) {
        this.maxCarryWeight = amount;
        
        EncumberedPlugin.instance.overrides.set(uuid.toString(), amount);
        try {
            EncumberedPlugin.instance.overrides.save(EncumberedPlugin.instance.overridesFile);
        } catch (IOException ex) {
            EncumberedPlugin.instance.getLogger().log(Level.WARNING, "Unable to save overrides.yml. Error: {0}", ex);
        }
    }
    
    private boolean isOnline() {
        return getPlayer() != null;
    }
    
    private Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }
    
    /**
     * 
     * @return The weight the player is holding; -1 if player is offline
     */
    public double calculateCarriedWeight() {
//        if (op instanceof Player) {
        if (isOnline()) {
            Player player = getPlayer();
            double current = 0;
            
            PlayerInventory inv = player.getInventory();
            
            // Armor
            for (ItemStack armor : inv.getArmorContents()) {
                current += Encumbered.calculateWeight(armor);
            }
            
            // Contents
            for (ItemStack item : inv.getContents()) {
                current += Encumbered.calculateWeight(item);
            }
            
            return current;
        }
        
        return -1; //Player is offline.
    }
    
    /**
     * 
     * @param stack The item we want to carry
     * @return false if player is offline or cannot carry, true if bypass or has space
     */
    public boolean canCarry(ItemStack stack) {
        if (!isOnline()) return false; //Player is offline
        if (canBypass()) return true; //Player can carry everything
        if (Encumbered.canPickupIfExceedMaxCarryWeight) return true;
        
        double current = calculateCarriedWeight();
        double max = getMaxCarryWeight();
        
        if (current <= max) {
            double calc = Encumbered.calculateWeight(stack);
            
            return (current + calc) <= max;
        }
        
        return false;
    }
    
    /**
     * 
     * @return True of player has carryweight.bypass or is in CREATIVE. false if player is offline or does not have requirements
     */
    public boolean canBypass() {
        if (isOnline()) {
            return getPlayer().getGameMode() == GameMode.CREATIVE || 
                   getPlayer().getGameMode() == GameMode.SPECTATOR|| 
                   getPlayer().hasPermission(Permissions.BYPASS);
        }
        
        return false;
    }
    
}
