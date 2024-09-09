package dev.hydrogen1.excludeaccess;

import dev.hydrogen1.excludeaccess.util.GeoIPDatabase;
import dev.hydrogen1.excludeaccess.util.IPFilter;
import dev.hydrogen1.excludeaccess.util.PermissionManager;
import dev.hydrogen1.excludeaccess.util.SimpleWebhook;
import lombok.Getter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.logging.Level;

public final class ExcludeAccess extends JavaPlugin {
    private SimpleWebhook client;
    @Getter
    private IPFilter filter;
    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new LoginHandler(this), this);
        GeoIPDatabase.load(this);
        filter = new IPFilter(getConfig().getStringList("allowed-ips"));
        client = new SimpleWebhook(getConfig().getString("discord-webhook", ""));
        PermissionManager.init(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            reloadAll();
            sender.sendMessage("Config reloaded.");
            return true;
        }
        sender.sendMessage("Usage: /excludeaccess reload");
        return false;
    }

    private void reloadAll() {
        reloadConfig();
        GeoIPDatabase.load(this);
        filter = new IPFilter(getConfig().getStringList("allowed-ips"));
        if(getConfig().getString("discord-webhook", "").isEmpty()) {
            client = null;
        } else
            client = new SimpleWebhook(getConfig().getString("discord-webhook", ""));
    }

    public void discordLog(String message) {
        if(getConfig().getString("discord-webhook", "").isEmpty()) return;
        try {
            client.send(message);
        } catch (IOException e) {
            getLogger().warning("Failed to send a message to Discord.");
            getLogger().log(Level.WARNING, e.getMessage(), e);
        }
    }
}
