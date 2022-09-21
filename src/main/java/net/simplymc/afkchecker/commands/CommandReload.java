package net.simplymc.afkchecker.commands;

import net.simplymc.afkchecker.AfkCheckerPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class CommandReload implements CommandExecutor {
    private AfkCheckerPlugin plugin;

    public CommandReload(AfkCheckerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length != 0 && args[0].equals("reload")) {
            this.plugin.reloadConfig();
            this.plugin.reloadConfigValues();

            sender.sendMessage("Reloaded!");
            return true;
        }

        return false;
    }
}
