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
package codes.goblom.executor;

import com.google.common.collect.Lists;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 *
 * @author Bryan Larson
 */
public abstract class Executor implements TabExecutor {

    private final List<ExecMap> execMap = new ArrayList();
    
    @Setter
    private String noPermissionMessage;
    
    private final Plugin plugin;
    
    public Executor(Plugin plugin) {
        this.plugin = plugin;
        
        if (this instanceof CommandListener) {
            addExecutor((CommandListener) this);
        }
    }

    public final List<CommandInfo> getRegisteredCommands() {
        return Lists.transform(execMap, ExecMap::getInfo);
    }
    
    public final void addExecutor(CommandListener exec) {
        Class<?> clazz = exec.getClass();
        
        while (clazz != null) {
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.isAnnotationPresent(CommandInfo.class)) {
                    CommandInfo info = m.getAnnotation(CommandInfo.class);
                    m.setAccessible(true);
                    execMap.add(new ExecMap(m, info, exec));
                    
//                    plugin.getLogger().info("Executor found method[" + m.getName() + "] with command of [" + info.name() + "] in [" + clazz.toString() + "]");
                }
            }
            
            clazz = clazz.getSuperclass();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(new CommandContext(this, sender, args));
            return true;
        }
        
        String cmd = null;
        try {
            cmd = ChatColor.stripColor(args[0]);
        } catch (Exception e) {
            sendHelp(new CommandContext(this, sender, args));
            return true;
        }
        
        if (cmd == null || cmd.isEmpty()) {
            sendHelp(new CommandContext(this, sender, args));
            return true;
        }
        
        ExecMap found = null;
        
        for (ExecMap map : execMap) {
            CommandInfo info = map.getInfo();
            
            if (info.name().equalsIgnoreCase(cmd)) {
                found = map;
                break;
            } else {
                if (info.alias().length != 0) {
                    for (String alias : info.alias()) {
                        if (alias.equalsIgnoreCase(cmd)) {
                            found = map;
                            break;
                        }
                    }
                }
            }
        }
        
        if (found == null) {
            sendHelp(new CommandContext(this, sender, args));
            return true;
        }
        
        if (!sender.hasPermission(found.getInfo().permission())) {
            if (noPermissionMessage != null) {
                sendMessage(sender, noPermissionMessage);
            }
            return true;
        }
        
        final CommandContext context = new CommandContext(this, sender, args, true);
        final ExecMap ffound = found;
        
        Runnable r = () -> {
            try {
                ffound.getMethod().invoke(ffound.getListener(), context);
            } catch (Throwable e) { //Use generic Throwable because a command may throw a different error
                onError(sender, e);
            }
        };
        
        if (found.getInfo().async()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, r);
        } else {
            r.run();
        }
        
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggest = new ArrayList();
        
        ExecMap found = null;
        
        try {
            String cmd = ChatColor.stripColor(args[0]);
            
            for (ExecMap map : execMap) {
                CommandInfo info = map.getInfo();

                if (info.name().equalsIgnoreCase(cmd)) {
                    found = map;
                    break;
                } else {
                    if (info.alias().length != 0) {
                        for (String a : info.alias()) {
                            if (a.equalsIgnoreCase(cmd)) {
                                found = map;
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) { }
        
        if (found == null) {
            if (args.length > 1) {
                suggest.add(ChatColor.BOLD + "" + ChatColor.RED + "Command " + args[0] + " not found.");
                if (sender instanceof Player) {
                    return suggest;
                }
            
                return Lists.transform(suggest, ChatColor::stripColor);
            }
            
            execMap.stream().filter((map) -> !(!sender.hasPermission(map.getInfo().permission()))).forEachOrdered((map) -> {
                String name = map.getInfo().name();
                try {
                    String cmd = args[0];
                    
                    if (name.startsWith(cmd) || name.endsWith(cmd)) {
                        suggest.add(name);
                    }
                } catch (Exception e) { }
            });

            return Lists.transform(suggest, ChatColor::stripColor);
        }
        
        if (!sender.hasPermission(found.getInfo().permission())) {
            if (noPermissionMessage != null) {
                suggest.add(noPermissionMessage);
            }
            
            if (sender instanceof Player) {
                return suggest;
            }
            
            return Lists.transform(suggest, ChatColor::stripColor);
        }
        
        final CommandContext context = new CommandContext(this, sender, args, false);
        final ExecMap ffound = found;
        
        try {
            ffound.getMethod().invoke(ffound.getListener(), context);
        } catch (Exception e) { //Use generic Exception because a command may throw a different error
            onError(sender, e);
        }
        
        if (context.getTabComplete() != null && !context.getTabComplete().isEmpty()) {
            return context.getTabComplete();
        }
        
        return suggest;
    }
    
    /**
     * Triggered only if unable to find a command
     */
    public abstract void sendHelp(CommandContext context);
    
    /**
     * This is only called if there is an error when running a @CommandInfo command.
     * Not triggered anywhere else.
     * 
     * @param sender CommandSender who ran command
     * @param e Exception thrown
     */
    public abstract void onError(CommandSender sender, Throwable e);
    
    public abstract void sendMessage(CommandSender sender, String message);
    
    public static List<String> copyPartialMatches(Iterable<String> orig, String token) {
        List<String> coll = new ArrayList();
        
        for (String str : orig) {            
            if (token != null && (str.length() < token.length())) continue;
            
            if (str.regionMatches(true, 0, token, 0, token.length())) {
                coll.add(str);
            }
        }
        
        return coll;
    }
}
