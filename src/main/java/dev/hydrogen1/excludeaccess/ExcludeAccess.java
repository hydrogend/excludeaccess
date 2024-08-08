package dev.hydrogen1.excludeaccess;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class ExcludeAccess extends JavaPlugin implements CommandExecutor {
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
}
