package com.tabroles.plugin;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class RoleManager {

    private final TabRoles plugin;
    private final Map<UUID, String> playerRoles = new HashMap<>();
    private File dataFile;
    private FileConfiguration dataConfig;

    public RoleManager(TabRoles plugin) {
        this.plugin = plugin;
        loadData();
    }

    // ─── Data file ───────────────────────────────────────────────────────────────

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        if (dataConfig.contains("players")) {
            ConfigurationSection sec = dataConfig.getConfigurationSection("players");
            for (String key : sec.getKeys(false)) {
                try {
                    playerRoles.put(UUID.fromString(key), sec.getString(key));
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    private void saveData() {
        for (Map.Entry<UUID, String> entry : playerRoles.entrySet()) {
            dataConfig.set("players." + entry.getKey().toString(), entry.getValue());
        }
        try { dataConfig.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    // ─── Role operations ──────────────────────────────────────────────────────────

    public void setRole(UUID uuid, String roleName) {
        if (!roleExists(roleName)) return;
        playerRoles.put(uuid, roleName.toUpperCase());
        saveData();
    }

    public void removeRole(UUID uuid) {
        playerRoles.remove(uuid);
        dataConfig.set("players." + uuid.toString(), null);
        try { dataConfig.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    public String getRole(UUID uuid) {
        return playerRoles.get(uuid);
    }

    public boolean hasRole(UUID uuid) {
        return playerRoles.containsKey(uuid);
    }

    // ─── Config role helpers ──────────────────────────────────────────────────────

    public boolean roleExists(String roleName) {
        return plugin.getConfig().contains("roles." + roleName.toUpperCase());
    }

    public Set<String> getRoleNames() {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("roles");
        return sec != null ? sec.getKeys(false) : new java.util.HashSet<>();
    }

    /**
     * Returns the display name with colour applied.
     * e.g.  [MANAGER]
     */
    public String getRoleDisplay(String roleName) {
        String path = "roles." + roleName.toUpperCase();
        String color  = plugin.getConfig().getString(path + ".color", "&f");
        String label  = plugin.getConfig().getString(path + ".display", roleName);
        String bracket= plugin.getConfig().getString(path + ".bracket", "&8[&r%color%%label%&8]");

        bracket = bracket
                .replace("%color%", color)
                .replace("%label%", label);

        return ChatColor.translateAlternateColorCodes('&', bracket);
    }

    /**
     * Returns the raw ChatColor prefix used for scoreboard team ordering.
     * Lower sort value = appears higher in tab.
     */
    public int getRolePriority(String roleName) {
        return plugin.getConfig().getInt("roles." + roleName.toUpperCase() + ".priority", 100);
    }

    /**
     * Returns the color code string (e.g. &4) for the player's name colour in tab.
     */
    public String getNameColor(String roleName) {
        String path = "roles." + roleName.toUpperCase() + ".name-color";
        return plugin.getConfig().getString(path,
               plugin.getConfig().getString("roles." + roleName.toUpperCase() + ".color", "&f"));
    }
}
