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
import codes.goblom.executor.Executor;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author Bryan Larson
 */
public class CarryPlugin extends JavaPlugin implements Listener, Runnable {
    private static final List<Material> CLIMBABLE_BLOCKS = Arrays.asList(Material.WATER, Material.LADDER, Material.VINE);
    
    protected static CarryPlugin instance;
    protected static boolean debug = false;
    
    private static final int MESSAGE_INTERVAL = 3;
    
    private static final String SHORT_PREFIX = "[CW]";
    private static final String PREFIX = "[CarryWeight]";
    
    private Executor exec;
    private Cache<UUID, Long> messageQueue;
    
    @Override
    public void onLoad() {        
        instance = this;
    }
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        debug = getConfig().getBoolean("Debug", debug);
        
        int messageInterval = getConfig().getInt("Message Interval", MESSAGE_INTERVAL);
        messageQueue = CacheBuilder.newBuilder()
                .expireAfterWrite(messageInterval, TimeUnit.MINUTES)
                .removalListener((RemovalNotification<UUID, Long> notif) -> {
                    UUID id = notif.getKey();
                    
                    WeightedPlayer wp = CarryWeight.getPlayer(id);
                    double current = wp.calculateCarryWeight();
                    double max = wp.getMaxCarryWeight();
                    
                    if (wp.canBypass() || current < max) {
                        Player player = Bukkit.getPlayer(id);
                        
                        if (player == null) return;
                        
                        player.setWalkSpeed(CarryWeight.DEFAULT_WALK_SPEED);
                        player.setFlySpeed(CarryWeight.DEFAULT_FLY_SPEED);
                    }
                })
                .build();
        
        CarryWeight.accountAmount = getConfig().getBoolean("Account for Amount", CarryWeight.accountAmount);
        CarryWeight.defaultMaxCarryWeight = getConfig().getDouble("Default Max Carry Weight", CarryWeight.defaultMaxCarryWeight);
        CarryWeight.canPickupIfExceedMaxCarryWeight = getConfig().getBoolean("Can Pickup Item if Exceed Max Carry", CarryWeight.canPickupIfExceedMaxCarryWeight);
        CarryWeight.weightedTooltip = getConfig().getBoolean("Show Weight in Tooltip", CarryWeight.weightedTooltip);
        
        for (String matName : getConfig().getConfigurationSection("Material Weights").getKeys(false)) {
            Material mat = Material.matchMaterial(matName);
            double weight = getConfig().getDouble("Material Weights." + matName);
            
            if (mat == null) {
                getLogger().warning("Material '" + matName + "' doesn't seem to exist... Skipping...");
                continue;
            }
            
            if (mat.isAir()) {
                getLogger().warning("Found AIR type material in Material Weights... Skipping...");
                continue;
            }
            
            CarryWeight.MATERIAL_WEIGHTS.put(mat, weight);
        }
        
        Bukkit.getPluginManager().registerEvents(this, this);
        
        this.exec = new Executor(this) {
            @Override
            public void sendHelp(CommandContext context) {
                for (CommandInfo info : getRegisteredCommands()) {
                    if (!info.permission().isEmpty() && !context.getSender().hasPermission(info.permission())) continue;
                    
                    StringBuilder sb = new StringBuilder(info.name());
                    
                    if (!info.description().isEmpty()) {
                        sb.append(" - ").append(info.description());
                    }
                    
                    context.message(sb.toString());
                    
                    if (!info.usage().isEmpty()) {
                        StringBuilder usage = new StringBuilder("Usage: ");
                                      usage.append(info.name());
                                      usage.append(" ");
                                      usage.append(info.usage());
                                      
                        context.message(usage.toString());
                    }
                    
                    context.message("");
                }
            }

            @Override
            public void onError(CommandSender sender, Throwable e) {
                sendMessage(sender, "Error: " + e.getMessage());

                if (debug) {
                    e.printStackTrace();
                }
            }

            @Override
            public void sendMessage(CommandSender sender, String message) {
                boolean shortPrefix = CarryPlugin.instance.getConfig().getBoolean("Short Prefix", false);

                sender.sendMessage((shortPrefix ? SHORT_PREFIX : PREFIX) + " " + message);
            }
        };
        
        exec.addExecutor(new CarryCommands());
        PluginCommand cmd = getCommand("carryweight");
                      cmd.setDescription("CarryWeight Command Stub");
                      cmd.setUsage("/carryweight " + Lists.transform(exec.getRegisteredCommands(), CommandInfo::name) + " (args)");
                      cmd.setExecutor(exec);
                      cmd.setTabCompleter(exec);
                      
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, this, 1, 5); // Every 5 Ticks
    }
    
    @Override
    public void onDisable() {
        CarryWeight.MATERIAL_WEIGHTS.clear();
        CarryWeight.WEIGHTED_PLAYERS.clear();
    }
    
    @Override
    public void run() {
        Collection<? extends Player> snapshot = Bukkit.getOnlinePlayers();
        
        snapshot.forEach((player) -> {
            WeightedPlayer wp = CarryWeight.getPlayer(player);
            
            if (wp.canBypass()) return;
            
            double current = wp.calculateCarryWeight();
            double max = wp.getMaxCarryWeight();
            
            if (current >= max) {
                if (messageQueue.getIfPresent(player.getUniqueId()) == null) {
                    messageQueue.put(player.getUniqueId(), System.nanoTime());
                    exec.sendMessage(player, "You are over encumbered. Drop a few items to speed up.");
                }
                
                player.setFlySpeed((float) getConfig().getDouble("Overencumbered.Fly Speed", 0.1));
                player.setWalkSpeed((float) getConfig().getDouble("Overencumbered.Walk Speed", 0.2));
            } else if (messageQueue.getIfPresent(player.getUniqueId()) != null) {
                player.setWalkSpeed(CarryWeight.DEFAULT_WALK_SPEED);
                player.setFlySpeed(CarryWeight.DEFAULT_FLY_SPEED);
                messageQueue.invalidate(player.getUniqueId());
            }
        });
    }
    
    @EventHandler
    public void tooltipInvOpen(InventoryOpenEvent event) {
        if (!CarryWeight.weightedTooltip) return;
        
        ItemStack[] contents = event.getInventory().getStorageContents();
        
        for (int i = 0; i < contents.length; i++) {
            ItemStack content = contents[i];
            
            if (content == null) continue;
            
            content = CarryWeight.addWeightTooltip(content);
            contents[i] = content;
        }
        
        event.getInventory().setContents(contents);
    }
    
    @EventHandler
    public void tooltipInvDrag(InventoryDragEvent event) {
        if (!CarryWeight.weightedTooltip) return;
        
        event.setCursor(CarryWeight.addWeightTooltip(event.getCursor()));
    }
    
    @EventHandler( priority = EventPriority.LOW )
    public void onItemPickup(EntityPickupItemEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        ItemStack item = event.getItem().getItemStack();
        WeightedPlayer wp = CarryWeight.getPlayer(player);
        
        if (!wp.canCarry(item)) {
            event.setCancelled(true);
            event.getItem().setPickupDelay(5);
            
            double itemWeight = CarryWeight.calculateWeight(item);
            double current = wp.calculateCarryWeight();
            double max = wp.getMaxCarryWeight();
            
            exec.sendMessage(player, "You are carrying too much. Please drop " + ((current + itemWeight) - max) + " weight.");
        }
        
        if (!CarryWeight.weightedTooltip) return;
        
        event.getItem().setItemStack(CarryWeight.addWeightTooltip(item));
    }
    
    @EventHandler( priority = EventPriority.LOW )
    public void onToggleSprint(PlayerToggleSprintEvent event) {
        if (event.isCancelled()) return;
        
        WeightedPlayer wp = CarryWeight.getPlayer(event.getPlayer());
        
        if (wp.canBypass()) return;
        
        double current = wp.calculateCarryWeight();
        double max = wp.getMaxCarryWeight();
        
        if (current >= max && event.isSprinting()) {
            event.setCancelled(true);
            event.getPlayer().setSprinting(false);
            
            exec.sendMessage(event.getPlayer(), "Cannot spring while over encumbered.");
        }
    }
    
    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        player.setWalkSpeed(CarryWeight.DEFAULT_WALK_SPEED);
        player.setFlySpeed(CarryWeight.DEFAULT_FLY_SPEED);
    }
    
    @EventHandler
    public void onDisable(PluginDisableEvent event) {
        if (event.getPlugin().equals(this)) {
            Bukkit.getOnlinePlayers().forEach((player) -> {
                player.setFlySpeed(CarryWeight.DEFAULT_FLY_SPEED);
                player.setWalkSpeed(CarryWeight.DEFAULT_WALK_SPEED);
            });
        }
    }
    
    @EventHandler
    public void onPlayerJump(PlayerMoveEvent event) {
        if (event.isCancelled()) return;
        
        Player player = event.getPlayer();
        WeightedPlayer wp = CarryWeight.getPlayer(player);
        
        if (wp.canBypass() || player.isOnGround() || player.getVelocity().getY() < 0) return;

        //Special cases for blocks players can climb
        if (CLIMBABLE_BLOCKS.contains(event.getTo().getBlock().getType())) return;
        Location locBelow = event.getTo().clone();
                 locBelow.setY(locBelow.getY() - 1);
        if (CLIMBABLE_BLOCKS.contains(locBelow.getBlock().getType())) return;
        //End Special Cases
        
        double current = wp.calculateCarryWeight();
        double max = wp.getMaxCarryWeight();
        
        if (current >= max) {
            event.setTo(event.getFrom());
            
            exec.sendMessage(player, "Cannot jump while over encumbered.");
        }
    }
}
