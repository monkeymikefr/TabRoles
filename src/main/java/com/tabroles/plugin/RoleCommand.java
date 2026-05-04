package com.tabroles.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class RoleCommand implements CommandExecutor, TabCompleter {

    private final TabRoles plugin;
    private final RoleManager roleManager;
    private final TabManager tabManager;

    private static final String PREFIX = ChatColor.GRAY + "[" + ChatColor.GOLD + "TabRoles" + ChatColor.GRAY + "] ";

    public RoleCommand(TabRoles plugin, RoleManager roleManager, TabManager tabManager) {
        this.plugin = plugin;
        this.roleManager = roleManager;
        this.tabManager = tabManager;
        plugin.getCommand("role").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("tabroles.admin")) {
            sender.sendMessage(PREFIX + ChatColor.RED + "You don't have permission.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {

            // /role set <player> <role>
            case "set": {
                if (args.length < 3) { sender.sendMessage(PREFIX + ChatColor.RED + "Usage: /role set <player> <role>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(PREFIX + ChatColor.RED + "Player not found or offline."); return true; }
                String roleName = args[2].toUpperCase();
                if (!roleManager.roleExists(roleName)) {
                    sender.sendMessage(PREFIX + ChatColor.RED + "Role '" + roleName + "' does not exist in config.yml.");
                    listRoles(sender);
                    return true;
                }
                roleManager.setRole(target.getUniqueId(), roleName);
                tabManager.updatePlayer(target);
                sender.sendMessage(PREFIX + ChatColor.GREEN + "Set " + target.getName() + "'s role to " + roleManager.getRoleDisplay(roleName));
                target.sendMessage(PREFIX + ChatColor.YELLOW + "Your role has been set to " + roleManager.getRoleDisplay(roleName));
                break;
            }

            // /role remove <player>
            case "remove": {
                if (args.length < 2) { sender.sendMessage(PREFIX + ChatColor.RED + "Usage: /role remove <player>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(PREFIX + ChatColor.RED + "Player not found or offline."); return true; }
                if (!roleManager.hasRole(target.getUniqueId())) {
                    sender.sendMessage(PREFIX + ChatColor.RED + target.getName() + " has no role assigned.");
                    return true;
                }
                roleManager.removeRole(target.getUniqueId());
                tabManager.updatePlayer(target);
                sender.sendMessage(PREFIX + ChatColor.GREEN + "Removed " + target.getName() + "'s role.");
                target.sendMessage(PREFIX + ChatColor.YELLOW + "Your role has been removed.");
                break;
            }

            // /role check <player>
            case "check": {
                if (args.length < 2) { sender.sendMessage(PREFIX + ChatColor.RED + "Usage: /role check <player>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(PREFIX + ChatColor.RED + "Player not found or offline."); return true; }
                String role = roleManager.getRole(target.getUniqueId());
                if (role == null) {
                    sender.sendMessage(PREFIX + target.getName() + ChatColor.GRAY + " has no role.");
                } else {
                    sender.sendMessage(PREFIX + target.getName() + ChatColor.GRAY + " → " + roleManager.getRoleDisplay(role));
                }
                break;
            }

            // /role list
            case "list": {
                listRoles(sender);
                break;
            }

            // /role reload
            case "reload": {
                plugin.reloadConfig();
                tabManager.refreshAll();
                sender.sendMessage(PREFIX + ChatColor.GREEN + "Config reloaded and tab updated.");
                break;
            }

            default:
                sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender s) {
        s.sendMessage(PREFIX + ChatColor.GOLD + "--- TabRoles Commands ---");
        s.sendMessage(ChatColor.YELLOW + "/role set <player> <role>" + ChatColor.GRAY + " - Assign a role");
        s.sendMessage(ChatColor.YELLOW + "/role remove <player>"     + ChatColor.GRAY + " - Remove a role");
        s.sendMessage(ChatColor.YELLOW + "/role check <player>"      + ChatColor.GRAY + " - Check a player's role");
        s.sendMessage(ChatColor.YELLOW + "/role list"                + ChatColor.GRAY + " - List all roles");
        s.sendMessage(ChatColor.YELLOW + "/role reload"              + ChatColor.GRAY + " - Reload config");
    }

    private void listRoles(CommandSender s) {
        s.sendMessage(PREFIX + ChatColor.GOLD + "Available roles:");
        for (String role : roleManager.getRoleNames()) {
            s.sendMessage("  " + roleManager.getRoleDisplay(role) + ChatColor.GRAY + " (" + role + ")");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("tabroles.admin")) return null;

        if (args.length == 1) {
            return Arrays.asList("set", "remove", "check", "list", "reload");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("check"))) {
            // Return online player names
            return null; // null = bukkit provides player names by default
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return new java.util.ArrayList<>(roleManager.getRoleNames());
        }
        return null;
    }
}
