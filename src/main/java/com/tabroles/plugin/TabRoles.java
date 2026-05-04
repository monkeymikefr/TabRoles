package com.tabroles.plugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;

public class TabRoles extends JavaPlugin {

    private RoleManager roleManager;
    private TabManager tabManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        roleManager = new RoleManager(this);
        tabManager = new TabManager(this, roleManager);

        getCommand("role").setExecutor(new RoleCommand(this, roleManager, tabManager));
        getServer().getPluginManager().registerEvents(new PlayerListener(this, tabManager), this);

        // Apply tab to all currently online players on reload
        for (Player p : Bukkit.getOnlinePlayers()) {
            tabManager.updatePlayer(p);
        }

        getLogger().info("TabRoles enabled! Eaglercraft compatible.");
    }

    @Override
    public void onDisable() {
        // Clean up scoreboards
        for (Player p : Bukkit.getOnlinePlayers()) {
            tabManager.resetPlayer(p);
        }
        getLogger().info("TabRoles disabled.");
    }

    public RoleManager getRoleManager() { return roleManager; }
    public TabManager getTabManager() { return tabManager; }
}
