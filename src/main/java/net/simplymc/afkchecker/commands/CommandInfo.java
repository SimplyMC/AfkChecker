package net.simplymc.afkchecker.commands;

import net.kyori.adventure.text.Component;
import net.simplymc.afkchecker.AfkCheckerPlugin;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class CommandInfo implements CommandExecutor {
    private final AfkCheckerPlugin plugin;

    public CommandInfo(AfkCheckerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        ArrayList<UUID> afkPlayerUuids = this.plugin.getAfkPlayers();
        List<Player> afkPlayers = new ArrayList<>();

        for (UUID uuid : afkPlayerUuids) {
            afkPlayers.add(this.plugin.getServer().getPlayer(uuid));
        }

        String afkPlayerString = afkPlayers.stream().map(Player::getName).collect(Collectors.joining(", "));
        int onlinePLayers = this.plugin.getServer().getOnlinePlayers().size();

        Component chatMessage = Component.text(
                        String.format("Players AFK: (%d): %s",
                                afkPlayers.size(),
                                afkPlayerString
                        )
                ).append(Component.newline())
                .append(Component.text(String.format("Players online: %d", onlinePLayers)));

        World world = this.plugin.getServer().getWorld(this.plugin.getWorld());
        if (world != null) {
            double sleepingPercentage = world.getGameRuleValue(GameRule.PLAYERS_SLEEPING_PERCENTAGE) / 100.0;
            int neededSleepers = (int) Math.ceil((onlinePLayers - afkPlayers.size()) * sleepingPercentage);

            chatMessage = chatMessage.append(Component.newline())
                    .append(Component.text(String.format("Currently need %d players to sleep", neededSleepers)));
        } else {
            this.plugin.getLogger().severe(String.format("Can not find world with name '%s', please make sure your config is correct", this.plugin.getWorld()));
        }

        sender.sendMessage(chatMessage);

        return true;
    }
}
