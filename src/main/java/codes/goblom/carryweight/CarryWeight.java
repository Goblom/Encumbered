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
package codes.goblom.carryweight;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author Bryan Larson
 */
public class CarryWeight {
    
    public static final float DEFAULT_FLY_SPEED = 0.1F;
    public static final float DEFAULT_WALK_SPEED = 0.2F;
    
    protected static boolean accountAmount = false;
    protected static double defaultMaxCarryWeight = 100.0;
    protected static boolean canPickupIfExceedMaxCarryWeight = true;
    
    protected static final Map<Material, Double> MATERIAL_WEIGHTS = Maps.newHashMap();
    protected static final List<WeightedPlayer> WEIGHTED_PLAYERS = Lists.newArrayList();
    
    public static WeightedPlayer getPlayer(OfflinePlayer player) {
        return getPlayer(player.getUniqueId());
    }
    
    public static WeightedPlayer getPlayer(UUID id) {
        return WEIGHTED_PLAYERS.stream().filter((p) -> { return p.getUuid().equals(id); }).findFirst().orElseGet(() -> {
            WeightedPlayer player = new WeightedPlayer(id);
            WEIGHTED_PLAYERS.add(player);
            
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
        CarryPlugin.instance.getConfig().set("Material Weights." + mat.name(), amount);
        CarryPlugin.instance.saveConfig();
    }
    
    public static double calculateWeight(ItemStack stack) {
        if (stack == null) return 0.0;
        
        double weight = getMaterialWeight(stack.getType());
        
        if (accountAmount) {
            weight *= stack.getAmount();
        }
        
        return weight;
    }
}
