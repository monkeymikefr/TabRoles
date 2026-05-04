package com.tabroles.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Uses the Bukkit Scoreboard API (Teams) to:
 *  - Set the prefix shown before a player's name in tab
 *  - Colour the player's name in tab
 *
 * This works on Eaglercraft because Eaglercraft faithfully implements
 * vanilla 1.12 scoreboard/team packets — no ProtocolLib needed.
 */
public class TabManager {

    private final TabRoles plugin;
    private final RoleManager roleManager;

    // One shared scoreboard that every player will see
    private final Scoreboard board;

    // Track which team each player is in so we can remove them later
    private final Map<UUID, String> playerTeam = new HashMap<>();

    public TabManager(TabRoles plugin, RoleManager roleManager) {
        this.plugin = plugin;
        this.roleManager = roleManager;
        ScoreboardManager sbm = Bukkit.getScoreboardManager();
        board = sbm.getNewScoreboard();
        preCreateTeams();
    }

    // ─── Pre-create one team per role ────────────────────────────────────────────

    private void preCreateTeams() {
        for (String role : roleManager.getRoleNames()) {
            getOrCreateTeam(role);
        }
        // Default team for players with no role
        getOrCreateTeam("_default");
    }

    private Team getOrCreateTeam(String role) {
        // Scoreboard team names max 16 chars in 1.12
        String teamName = toTeamName(role);
        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
            if (!role.equals("_default")) {
                applyTeamAppearance(team, role);
            } else {
                // Default: white name, no prefix
                team.setPrefix("");
                team.setSuffix("");
                // Allow friendly fire etc. — sensible defaults
                team.setAllowFriendlyFire(true);
                team.setCanSeeFriendlyInvisibles(false);
            }
        }
        return team;
    }

    private void applyTeamAppearance(Team team, String role) {
        String display  = roleManager.getRoleDisplay(role) + " ";  // space after bracket
        String nameCol  = ChatColor.translateAlternateColorCodes('&', roleManager.getNameColor(role));

        // Prefix max 16 chars in 1.12 — we truncate if needed
        String prefix = display.length() > 16 ? display.substring(0, 16) : display;
        team.setPrefix(prefix);
        // Suffix can hold the name colour reset so vanilla tab looks right
        team.setSuffix(ChatColor.RESET.toString());
        team.setAllowFriendlyFire(true);
        team.setCanSeeFriendlyInvisibles(false);

        // Use priority to set sort order via team name (lower = earlier alphabetically)
        // Teams are sorted alphabetically in 1.12
    }

    // ─── Public API ───────────────────────────────────────────────────────────────

    /** Call when a player joins or their role changes. */
    public void updatePlayer(Player player) {
        // Make sure this player sees our scoreboard
        player.setScoreboard(board);

        // Remove from old team
        String oldTeamName = playerTeam.get(player.getUniqueId());
        if (oldTeamName != null) {
            Team old = board.getTeam(oldTeamName);
            if (old != null) old.removeEntry(player.getName());
        }

        // Determine new team
        String role = roleManager.getRole(player.getUniqueId());
        String teamName = (role != null) ? toTeamName(role) : toTeamName("_default");

        // Ensure team exists (e.g. after config reload)
        if (role != null) getOrCreateTeam(role);
        Team team = board.getTeam(teamName);
        if (team == null) team = getOrCreateTeam("_default");

        team.addEntry(player.getName());
        playerTeam.put(player.getUniqueId(), teamName);

        // Also update display name in the world (optional — keeps things consistent)
        if (role != null) {
            String nameColor = ChatColor.translateAlternateColorCodes('&', roleManager.getNameColor(role));
            player.setDisplayName(nameColor + player.getName() + ChatColor.RESET);
            player.setPlayerListName(
                roleManager.getRoleDisplay(role) + " " + nameColor + player.getName() + ChatColor.RESET
            );
        } else {
            player.setDisplayName(player.getName());
            player.setPlayerListName(player.getName());
        }
    }

    /** Reset a player back to vanilla tab when they leave. */
    public void resetPlayer(Player player) {
        String teamName = playerTeam.remove(player.getUniqueId());
        if (teamName != null) {
            Team team = board.getTeam(teamName);
            if (team != null) team.removeEntry(player.getName());
        }
        player.setDisplayName(player.getName());
        player.setPlayerListName(player.getName());
        // Give them the main scoreboard back
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    /** Refresh all online players (used after config reload). */
    public void refreshAll() {
        preCreateTeams();
        for (Player p : Bukkit.getOnlinePlayers()) {
            updatePlayer(p);
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    /**
     * Convert role name to ≤16-char team name with priority prefix for ordering.
     * Priority 1 → "01_MANAGER", priority 100 → "99_DEFAULT" etc.
     */
    private String toTeamName(String role) {
        if (role.equals("_default")) return "99_default";
        int pri = roleManager.getRolePriority(role);
        String prefix = String.format("%02d", Math.min(pri, 98));
        String raw = prefix + "_" + role.toUpperCase();
        return raw.length() > 16 ? raw.substring(0, 16) : raw;
    }
}
