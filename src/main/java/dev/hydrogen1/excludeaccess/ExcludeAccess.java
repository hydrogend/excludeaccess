package dev.hydrogen1.excludeaccess;

import club.minnced.discord.webhook.WebhookClient;
import lombok.val;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class ExcludeAccess extends JavaPlugin {
    private LoginHandler loginHandler;
    @Override
    public void onEnable() {
        saveDefaultConfig();
        loginHandler = new LoginHandler(this);
        getServer().getPluginManager().registerEvents(loginHandler, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(args.length > 1 && args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            loginHandler.load();
            sender.sendMessage("Config reloaded.");
            return true;
        }
        sender.sendMessage("Usage: /excludeaccess reload");
        return false;
    }

    public void discordLog(String message) {
        if(getConfig().getString("discord-webhook", "").isEmpty()) return;
        try(val client = WebhookClient.withUrl(getConfig().getString("discord-webhook", ""))) {
            client.send(message);
        }
    }
}
