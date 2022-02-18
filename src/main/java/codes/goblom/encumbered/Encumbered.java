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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 *
 * @author Bryan Larson
 */
public class Encumbered {
    
    /**
     * Taken from default server values for new players
     */
    public static final float DEFAULT_FLY_SPEED = 0.1F;
    public static final float DEFAULT_WALK_SPEED = 0.2F;
    
    protected static boolean accountAmount = true;
    protected static double defaultMaxCarryWeight = 100.0;
    protected static boolean canPickupIfExceedMaxCarryWeight = true;
    protected static boolean weightedTooltip = true;
    
    protected static final Map<Material, Double> MATERIAL_WEIGHTS = Maps.newHashMap();
    protected static final List<EncumberedPlayer> ENCUMBERED_PLAYERS = Lists.newArrayList();
    
    private static final String WEIGHT_STR = "Weight: ";
    
    public static EncumberedPlayer getPlayer(OfflinePlayer player) {
        return getPlayer(player.getUniqueId());
    }
    
    public static EncumberedPlayer getPlayer(UUID id) {
        return ENCUMBERED_PLAYERS.stream().filter((p) -> { return p.getUuid().equals(id); }).findFirst().orElseGet(() -> {
            EncumberedPlayer player = new EncumberedPlayer(id);
            ENCUMBERED_PLAYERS.add(player);
            
            return player;
        });
    }
    
    public static double getMaterialWeight(Material mat) {
        if (mat.isAir() || !MATERIAL_WEIGHTS.containsKey(mat)) {
            return 0.0;
        }
        
        return MATERIAL_WEIGHTS.getOrDefault(mat, 0.0);
    }
    
    public static Map<Material, Double> getAllRecordedWeights() {
        return new HashMap(MATERIAL_WEIGHTS);
    }
    
    public static void setMaterialWeight(Material mat, double amount) {
        if (mat.isAir()) throw new UnsupportedOperationException(mat.name() + " is not a supported Material");
        
        MATERIAL_WEIGHTS.put(mat, amount);
        EncumberedPlugin.instance.weights.set(mat.name(), amount);
        
        try {
            EncumberedPlugin.instance.weights.save(EncumberedPlugin.instance.weightsFile);
        } catch (IOException ex) {
            EncumberedPlugin.instance.getLogger().log(Level.WARNING, "Unable to save weights.yml. Error: {0}", ex);
        }
    }
    
    public static double calculateWeight(ItemStack stack) {
        if (stack == null) return 0.0;
        
        double weight = getMaterialWeight(stack.getType());
        
        if (accountAmount) {
            weight *= stack.getAmount();
        }
        
        return weight;
    }
    
    public static ItemStack addWeightTooltip(ItemStack stack) {
        if (!weightedTooltip) return stack;
        
        double weight = calculateWeight(stack);
        
        ItemMeta meta = stack.getItemMeta();
        List<String> lore = meta.getLore();
        boolean added = false;
        
        if (lore != null) {
            for (int i = 0; i < lore.size(); i++) {
                String line = lore.get(i);
                
                if (line == null || line.isEmpty()) continue;
                
                if (line.startsWith(WEIGHT_STR)) {
                    added = true;
                    lore.set(i, WEIGHT_STR + weight);
                    break;
                }
            }
        } else {
            lore = new ArrayList();
            
            int spacing = 1;
            for (int i = 0; i < spacing; i++) {
                lore.add("");
            }
        }
        
        if (!added) {
            lore.add(WEIGHT_STR + weight);
        }
        
        meta.setLore(lore);
        stack.setItemMeta(meta);
        
        return stack;
    }
}
