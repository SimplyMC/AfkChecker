package net.simplymc.afkchecker;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.simplymc.afkchecker.commands.CommandInfo;
import net.simplymc.afkchecker.commands.CommandReload;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;

public final class AfkCheckerPlugin extends JavaPlugin implements Listener {
    // Time since last activity is stored in ticks
    private final HashMap<UUID, Long> lastActivityMap = new HashMap<>();
    private final HashMap<UUID, Location> lastLocationMap = new HashMap<>();
    private final ArrayList<UUID> afkPlayers = new ArrayList<>();

    private BukkitTask task = null;

    // Config values
    private Long taskPeriod;
    private Long afkDuration;
    private String afkSuffix;
    private boolean sendNotice;
    private String afkNotice;
    private String activeNotice;
    private String world;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfigValues();

        // Register commands
        getCommand("afkchecker").setExecutor(new CommandReload(this));
        getCommand("afkinfo").setExecutor(new CommandInfo(this));

        // Run location check every 5 seconds
        this.task = getServer().getScheduler().runTaskTimer(this, this::doAfkCheck, 20L * 10L, this.taskPeriod);
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        this.task.cancel();
    }

    public void reloadConfigValues() {
        FileConfiguration config = getConfig();
        this.afkDuration = (long) config.getInt("afkDuration");
        this.afkSuffix = config.getString("afkSuffix");
        this.sendNotice = config.getBoolean("sendNotice");
        this.afkNotice = config.getString("afkNotice");
        this.activeNotice = config.getString("activeNotice");
        this.world = config.getString("world", "world");
        Long oldTaskPeriod = this.taskPeriod;
        this.taskPeriod = (long) config.getInt("checkPeriod");

        // Re-register the task if we have a new task period
        if (!this.taskPeriod.equals(oldTaskPeriod) && this.task != null) {
            this.task.cancel();
            this.task = getServer().getScheduler().runTaskTimer(this, this::doAfkCheck, 20L * 10L, this.taskPeriod);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        populatePlayerData(event.getPlayer());
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        clearPlayerData(event.getPlayer().getUniqueId());
    }

    private void doAfkCheck() {
        // Calculate current ticks since last location change
        for (Player player : getServer().getOnlinePlayers()) {
            if (player.getGameMode() != GameMode.SURVIVAL) return;

            UUID uuid = player.getUniqueId();
            Location currentLocation = player.getLocation();
            Location lastLocation = this.lastLocationMap.get(uuid);

            if (currentLocation.equals(lastLocation)) {
                this.lastActivityMap.merge(uuid, this.taskPeriod, Long::sum);
            } else {
                this.lastActivityMap.put(uuid, 0L);
                this.lastLocationMap.put(uuid, currentLocation);
            }
        }

        // Mark players with extended time since last activity as AFK
        for (Entry<UUID, Long> entry : this.lastActivityMap.entrySet()) {
            UUID uuid = entry.getKey();
            Long ticksSinceActivity = entry.getValue();

            if (ticksSinceActivity >= (20L * this.afkDuration)) {
                setPlayerAfk(uuid);
            } else {
                setPlayerActive(uuid);
            }
        }
    }

    private void setPlayerAfk(UUID uuid) {
        if (this.afkPlayers.contains(uuid)) return;

        Player player = getServer().getPlayer(uuid);
        // Player probably disconnected
        if (player == null) {
            clearPlayerData(uuid);
            return;
        }

        player.setSleepingIgnored(true);
        Component afkName = player.playerListName().append(Component.text(String.format(" %s", this.afkSuffix)).decorate(TextDecoration.ITALIC).color(NamedTextColor.DARK_GRAY));
        player.playerListName(afkName);

        this.afkPlayers.add(uuid);
        if (this.sendNotice) player.sendMessage(Component.text(this.afkNotice).decorate(TextDecoration.ITALIC).color(NamedTextColor.DARK_GRAY));
    }

    private void setPlayerActive(UUID uuid) {
        if (!this.afkPlayers.contains(uuid)) return;

        Player player = getServer().getPlayer(uuid);
        // Player probably disconnected
        if (player == null) {
            clearPlayerData(uuid);
            return;
        }

        player.setSleepingIgnored(false);
        player.playerListName(player.displayName());

        this.afkPlayers.remove(uuid);
        if (this.sendNotice) player.sendMessage(Component.text(this.activeNotice).decorate(TextDecoration.ITALIC).color(NamedTextColor.DARK_GRAY));
    }

    private void populatePlayerData(Player player) {
        UUID uuid = player.getUniqueId();

        this.lastLocationMap.put(uuid, player.getLocation());
        this.lastActivityMap.put(uuid, 0L);
    }

    private void clearPlayerData(UUID uuid) {
        this.afkPlayers.remove(uuid);
        this.lastLocationMap.remove(uuid);
        this.lastActivityMap.remove(uuid);
    }

    public ArrayList<UUID> getAfkPlayers() {
        return afkPlayers;
    }

    public String getWorld() {
        return world;
    }
}
