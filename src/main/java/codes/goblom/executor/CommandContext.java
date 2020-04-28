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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author Bryan Larson
 */
public class CommandContext {
    
    @Getter
    private final CommandSender sender;
    @Getter
    private final Executor executor;
    @Getter
    private final String[] args;
    
    private List<String> tabComplete;
    @Getter 
    final boolean tabExecutor;
    
    CommandContext(Executor exec, CommandSender sender, String[] args) {
        this.executor = exec;
        this.sender = sender;
        this.args = args;
        this.tabExecutor = false;
    }
    
    CommandContext(Executor exec, CommandSender sender, String[] args, boolean executing) {
        this.sender = sender;
        this.executor = exec;
        this.args = Arrays.copyOfRange(args, 1, args.length); //Dont have start arg... that is origination
        this.tabExecutor = !executing;
    }
    
    public List<String> getTabComplete() {
        if (tabComplete == null) return null;
        
        if (sender instanceof Player) {
            return tabComplete;
        }
        
        return Lists.transform(tabComplete, ChatColor::stripColor);
    }
    
    public boolean hasArg(int i) {
        try {
            return args[i] != null && !args[i].trim().isEmpty() && args[i].replace(" ", "").length() != 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    public String getArg(int i) {
        try {
            return ChatColor.stripColor(args[i]);
        } catch (Exception e) {
            return null; //return null instead of exception
        }
    }
    
    public String combineRemaining(int start) {
        if (!hasArg(start)) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder();

        for (int i = start; i < args.length; i++) {
            sb.append(getArg(i)).append(" ");
        }

        return sb.toString().trim();
    }
    
    public String getArgs(int start, int end) {
        if (!hasArg(start) || !hasArg(end)) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder();

        for (int i = start; i < args.length && i != end; i++) {
            sb.append(getArg(i)).append(" ");
        }

        return sb.toString().trim();
    }
    
    public int argsLength() {
        return args.length;
    }
    
    public void suggest(String suggestion) {
        if (tabComplete == null) {
            this.tabComplete = new ArrayList();
        }
        
        tabComplete.add(ChatColor.stripColor(suggestion));
    }
    
    public void message(String... messages) {
//        System.out.println("isTabExecutor = " + isTabExecutor());
        if (isTabExecutor()) {
            for (String m : messages) {
                suggest(m);
            }
            
            return;
        }
        
        for (String message : messages) {
            executor.sendMessage(sender, message);
        }
    }
}
