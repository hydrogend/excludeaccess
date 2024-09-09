package dev.hydrogen1.excludeaccess;

import com.maxmind.geoip2.exception.GeoIp2Exception;
import dev.hydrogen1.excludeaccess.util.GeoIPDatabase;
import dev.hydrogen1.excludeaccess.util.PermissionManager;
import inet.ipaddr.IPAddressString;
import io.papermc.paper.ban.BanListType;
import lombok.val;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.io.IOException;
import java.net.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;

public final class LoginHandler implements Listener {
    private final ExcludeAccess plugin;
    private final Map<UUID,Integer> loginAttempts = new HashMap<>();
    LoginHandler(ExcludeAccess plugin) {
        this.plugin = plugin;
    }

    /**
     * When a player tries to log in, check the country of the player.
     * If the player is not from which you permit, kick the player.
     * @param event The event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onLogin(AsyncPlayerPreLoginEvent event) {
        if(Bukkit.isWhitelistEnforced()) return;
        if(Bukkit.getWhitelistedPlayers().stream()
                .map(OfflinePlayer::getUniqueId)
                .anyMatch(player -> player.equals(event.getUniqueId()))) return;
        if(PermissionManager.hasPermission(event.getUniqueId(), "excludeaccess.bypass")) return;
        if(Bukkit.getBannedPlayers().stream()
                .map(OfflinePlayer::getUniqueId)
                .anyMatch(player -> player.equals(event.getUniqueId()))) return;
        val addr = event.getAddress();
        if(checkIfLocal(addr)) return;
        if(plugin.getConfig().getBoolean("check-only-allowed-ips", false)) {
            if (plugin.getFilter().isRegistered(addr)) {
                plugin.getLogger().info("Allowed Player " + event.getName() + " from " + addr.getHostAddress());
                return;
            }
            kick(event, "You are not allowed to join.");
            plugin.getLogger().info("Unknown Player " + event.getName() + " from " + addr.getHostAddress());
            plugin.discordLog(event.getName() + " (" + addr.getHostAddress().substring(0,8) + ") のログインを拒否しました。");
            return;
        }
        try {
            val country = GeoIPDatabase.getCountry(addr);
            if(!plugin.getConfig().getStringList("allowed-countries").contains(country.getIsoCode())) {
                kick(event, "You are not allowed to join.");
                plugin.discordLog(event.getName() + " (" + country.getIsoCode() + ") のログインを拒否しました。");
            }
            plugin.getLogger().info("Player " + event.getName() + " from " + country.getName());
        } catch (IOException | GeoIp2Exception e) {
            kick(event, "Failed to check your country.");
            plugin.getLogger().warning("Failed to check country: " + e.getMessage());
            plugin.getLogger().info("Player " + event.getName() + " from unknown country");
            plugin.discordLog(event.getName() + " (不明) のログインを拒否しました。");
        }
    }

    /**
     * Kick the player with the specified reason.
     * When the process is delayed, this method kick the player, surely.
     *
     * @param event The event
     * @param reason The reason
     */
    private void kick(AsyncPlayerPreLoginEvent event, String reason) {
        val msg = Component.text(reason);
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, msg);
        loginAttempts.put(event.getUniqueId(), loginAttempts.get(event.getUniqueId()) == null ? 1 : loginAttempts.get(event.getUniqueId()) + 1);
        val player = Bukkit.getPlayer(event.getName());
        if(player != null) {
            player.kick(msg);
        }
        val max = plugin.getConfig().getInt("temporarily-ban-threshold", 3);
        if(max == 0) return;
        if(loginAttempts.get(event.getUniqueId()) >= max) {
            val days = plugin.getConfig().getLong("temporarily-ban-days", 1L);
            val term = Instant.now().plusSeconds(days * 24 * 60 * 60);
            Bukkit.getBanList(BanListType.PROFILE)
                    .addBan(event.getPlayerProfile(), "You are not allowed to join.", term, "ExcludeAccess");
            loginAttempts.remove(event.getUniqueId());
            plugin.getLogger().warning("Temporarily banned " + event.getName() + " for " + days + " days.");
        }
    }

    /**
     * Check if the address is local.
     * @param address The address
     * @return If the address is local, true
     */
    private boolean checkIfLocal(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress()) {
            return true;
        }
        // Check if the address is a site-local address
        try {
            return NetworkInterface.getByInetAddress(address) != null;
        } catch (SocketException e) {
            return false;
        }
    }
}
