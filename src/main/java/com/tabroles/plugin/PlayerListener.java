package com.tabroles.plugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final TabRoles plugin;
    private final TabManager tabManager;

    public PlayerListener(TabRoles plugin, TabManager tabManager) {
        this.plugin = plugin;
        this.tabManager = tabManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        // Slight delay so client finishes connecting before we set scoreboard
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            tabManager.updatePlayer(e.getPlayer());
        }, 5L); // 5 ticks = 0.25s
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        tabManager.resetPlayer(e.getPlayer());
    }
}
