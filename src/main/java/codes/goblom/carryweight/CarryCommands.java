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

import codes.goblom.executor.CommandContext;
import codes.goblom.executor.CommandInfo;
import codes.goblom.executor.CommandListener;
import codes.goblom.executor.Executor;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 *
 * @author Bryan Larson
 */
class CarryCommands implements CommandListener {    
    
    protected CarryCommands() { }
    
    @CommandInfo(
            name = "reset",
            alias = { "r" },
            permission = Permissions.RESET_CONFIG,
            description = "resets your config back to default values"
    )
    public void reset(CommandContext context) {
        if (context.isTabExecutor()) return;
        
        CarryPlugin.instance.saveResource("config.yml", true);
        context.message("Config reset back to default values", "Please restart you server.");
    }
    
    @CommandInfo(
            name = "calculate",
            alias = { "calc" },
            permission = Permissions.CALCULATE,
            description = "Calculate the weight of an online player",
            usage = "[player]"
    )
    public void calculate(CommandContext context) {
        if (!context.hasArg(0)) {
            if (!context.isTabExecutor()) {
                context.message(ChatColor.RED + "Requires a [player]");
            }
            
            Bukkit.getOnlinePlayers().forEach((p) -> context.suggest(p.getName()));
            return;
        }
        
        String playerName = context.getArg(0);
        Player player = Bukkit.getPlayerExact(playerName);
        
        if (player == null) {
            if (playerName == null || playerName.isEmpty()) {
                context.message(ChatColor.RED + "Requires a [player]");
                
                Bukkit.getOnlinePlayers().forEach((p) -> context.suggest(p.getName()));
            } else {
                List<String> names = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
//                List<String> names = Lists.transform(new ArrayList<>(Bukkit.getOnlinePlayers()), Player::getName);
                List<String> matched = Executor.copyPartialMatches(names, playerName);
            
                if (matched == null || matched.isEmpty()) {
                    context.message("Player name '" + playerName + "' is not online.");
                } else {
                    matched.forEach((s) -> context.suggest(s));
                }
            }
            
            return;
        }
        
        if (context.isTabExecutor()) return;
        
        context.message("The Carried Weight of " + player.getName() + " is " + CarryWeight.getPlayer(player).calculateCarryWeight());
    }
    
    @CommandInfo(
            name = "setmaterial",
            alias = "sm",
            description = "Set the weight of a material",
            permission = Permissions.SET_MATERIAL,
            usage = "[material] [weight]"
    )
    public void setMaterial(CommandContext context) {
        if (!context.hasArg(0)) {
            if (!context.isTabExecutor()) {
                context.message(ChatColor.RED + "Requires a [material]");
            }
            
            Lists.newArrayList(Material.values()).forEach((m) -> context.suggest(m.name()));
            return;
        }
        
        String matName = context.getArg(0);
        Material mat = Material.matchMaterial(matName);
        
        if (mat == null) {
            if (matName == null || matName.isEmpty()) {
                context.message(ChatColor.RED + "Requires a [material]");
                Lists.newArrayList(Material.values()).forEach((m) -> context.suggest(m.name()));
            } else {
                List<String> names = Lists.transform(Lists.newArrayList(Material.values()), (m) -> m.name());
                List<String> matched = Executor.copyPartialMatches(names, matName);
                
                if (matched == null || matched.isEmpty()) {
                    context.message("Material '" + matName + " does not exist.");
                } else {
                    matched.forEach((s) -> context.suggest(s));
                }
            }
            
            return;
        }
        
        if (mat.isAir()) {
            context.message("Cannot change weight of " + mat.name() + ". Is Air.");
            return;
        }
        
        if (!context.hasArg(1)) {
            context.message(ChatColor.RED + "Requires a [weight].");
            context.message(ChatColor.RED + "Can be any number above 0");
            
            return;
        }
        
        String amount = context.getArg(1);
        double weight;
        
        try {
            weight = Double.valueOf(amount);
            
            if (weight < 0) {
                context.message(ChatColor.RED + "Weight cannot be less than 0.");
                return;
            }
        } catch (NumberFormatException e) {
            context.message(ChatColor.RED + amount + " is not a number");
            return;
        }
        
        if (context.isTabExecutor()) return;
        
        CarryWeight.setMaterialWeight(mat, weight);
        context.message("Changed weight of " + mat.name() + " to " + weight);
    }
    
        @CommandInfo(
            name = "setcarry",
            alias = "sc",
            description = "Set the max carry weight of a player",
            permission = Permissions.SET_CARRY,
            usage = "[player] [weight]"
    )
    public void setCarry(CommandContext context) {
        Set<String> playerNames = Sets.newHashSet();
        Bukkit.getOnlinePlayers().forEach((p) -> playerNames.add(p.getName()));
//        Lists.newArrayList(Bukkit.getOfflinePlayers())/*.stream().filter(!playerNames.contains(OfflinePlayer::getName))*/.forEach((op) -> playerNames.add(op.getName()));
        
        if (!context.hasArg(0)) {
            if (!context.isTabExecutor()) {
                context.message(ChatColor.RED + "Requires a [player]");
            }
            
            playerNames.forEach((p) -> context.suggest(p));
            return;
        }
        
        String playerName = context.getArg(0);
        Player player = Bukkit.getPlayer(playerName); //TODO: Support OfflinePlayer
        
        if (player == null) {
            if (playerName == null || playerName.isEmpty()) {
                context.message(ChatColor.RED + "Requires a [player]");
                
                Bukkit.getOnlinePlayers().forEach((p) -> context.suggest(p.getName()));
            } else {
                List<String> matched = Executor.copyPartialMatches(playerNames, playerName);
            
                if (matched == null || matched.isEmpty()) {
                    context.message("Player name '" + playerName + "' is not online.");
                } else {
                    matched.forEach((s) -> context.suggest(s));
                }
            }
            
            return;
        }
        
        if (!context.hasArg(1)) {
            context.message(ChatColor.RED + "Requires a [weight].");
            context.message(ChatColor.RED + "Can be any number above 0");
            
            return;
        }
        
        String amount = context.getArg(1);
        double weight;
        
        try {
            weight = Double.valueOf(amount);
            
            if (weight < 0) {
                context.message(ChatColor.RED + "Weight cannot be less than 0.");
                return;
            }
        } catch (NumberFormatException e) {
            context.message(ChatColor.RED + amount + " is not a number");
            return;
        }
        
        if (context.isTabExecutor()) return;
        
        CarryWeight.getPlayer(player).setCustomMaxCarryWeight(weight);
        context.message("Set the Max Carry Weight of " + player.getName() + " to " + weight);
    }
}
