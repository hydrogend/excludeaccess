package dev.hydrogen1.excludeaccess.util;

import lombok.val;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public final class PermissionManager {
    private static boolean isLuckPermsEnabled = true;
    private static LuckPerms perm;

    /**
     * Initialize the permission manager. This method should be called in the onEnable method of the plugin.
     * If LuckPerms is not found, the permission system is disabled.
     * @param plugin The plugin instance
     */
    public static void init(JavaPlugin plugin) {
        if(!Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            isLuckPermsEnabled = false;
            plugin.getLogger().warning("LuckPerms is not found. Permission system is disabled.");
            return;
        }
        try {
            perm = LuckPermsProvider.get();
        } catch (IllegalStateException e) {
            isLuckPermsEnabled = false;
            plugin.getLogger().warning("Failed to get LuckPerms API. Permission system is disabled.");
        }
    }

    /**
     * Check if the player has the permission. If LuckPerms is not found, this method will return true if the player is an operator.
     * @param uuid The player's UUID
     * @param permission The permission
     * @return Whether the player has the permission
     */
    public static boolean hasPermission(UUID uuid, String permission) {
        if(!isLuckPermsEnabled) return Bukkit.getOperators().stream().anyMatch(op -> op.getUniqueId().equals(uuid));
        val user =  perm.getUserManager().getUser(uuid);
        if(user == null) return false;
        return user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
    }
    private PermissionManager() {}
}
