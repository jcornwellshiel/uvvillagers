/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.uvnode.uvvillagers;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author James Cornwell-Shiel
 */
public class CommandHandler implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] arguments) {
        if (command.getName().equalsIgnoreCase("uvv")) {
            if(sender instanceof Player) {
                processPlayerCommand(arguments, (Player) sender);
            } else {
                processConsoleCommand(arguments, sender);
            }
        }
        return false;
    }

    private void processConsoleCommand(String[] arguments, CommandSender sender) {
        if (arguments.length > 0) {
            
        } else {
            displayHelp(sender, true, true, true, true);
        }
    }

    private void processPlayerCommand(String[] arguments, Player player) {
        if (arguments.length > 0) {
            
        } else {
            displayHelp(player, player.hasPermission("uvv.siegeinfo"), player.hasPermission("uvv.villageinfo"), player.hasPermission("uvv.rename"), player.hasPermission("uvv.admin"));
        }
    }

    private void displayHelp(CommandSender sender, boolean showSiege, boolean showVillage, boolean showRename, boolean showAdmin) {
        sender.sendMessage("Commands:");
        if (showAdmin) {
            if (!(sender instanceof Player))
                sender.sendMessage(" /uvv debug - enables debug mode");
            sender.sendMessage(" /uvv reload - reloads the configuration from disk");
            sender.sendMessage(" /uvv reload villages - reloads the village list from disk (changes since last save will be lost)");
            sender.sendMessage(" /uvv save - saves villages to disk");
        }
        if (showVillage) {
            sender.sendMessage(" /uvv list - lists  all known villages");
            sender.sendMessage(" /uvv loaded - lists villages currently loaded");
            sender.sendMessage(" /uvv nearby - lists villages you're near");
            sender.sendMessage(" /uvv current - lists  your current village");
        }
        if (showRename) {
            sender.sendMessage(" /uvv rename - rename the village you're in");
        }
        if (showSiege) {
            sender.sendMessage(" /uvv siegeinfo - displays siege status data");
            if (showAdmin)
                sender.sendMessage(" /uvv startsiege - forces a siege to start. no tribute bonus granted if it's daytime.");
        }
    }
    
    private void displayHeader(CommandSender sender, String text) {
        sender.sendMessage(ChatColor.GOLD + " - UVVillagers " + text + " - ");
    }
}
