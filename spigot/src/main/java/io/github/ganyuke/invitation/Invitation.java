package io.github.ganyuke.invitation;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public final class Invitation extends JavaPlugin {

    private InvitationCore core;

    @Override
    public void onEnable() {
        BukkitLoggerPort loggerPort = new BukkitLoggerPort(getLogger());
        BukkitWhitelistPort whitelistPort = new BukkitWhitelistPort(loggerPort);

        if (!whitelistPort.isEnabled()) {
            getLogger().warning("why are you using this plugin without a whitelist enabled...? but, sure, you do you :)");
        }

        File dataFolder = getDataFolder();

        if (!dataFolder.exists()) {
            if (!dataFolder.mkdirs()) {
                getLogger().severe("Could not create plugin data folder!");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        }

        if (!dataFolder.isDirectory()) {
            getLogger().severe("Plugin data folder path exists but is not a directory!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Path dataPath = dataFolder.toPath();
        Path configFile = dataPath.resolve("config.yml");
        try {
            ConfigLoader.ensureDefaultFromClasspath(configFile, ConfigLoader.class);
            InvitationConfig config = ConfigLoader.load(configFile);
            core = InvitationCore.start(
                    dataPath,
                    config,
                    whitelistPort,
                    new BukkitSchedulerPort(this),
                    new BukkitMessengerPort(),
                    loggerPort
            );
        } catch (IOException e) {
            getLogger().severe("Failed to load Invitation config: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        } catch (SqliteUnavailableException e) {
            getLogger().severe("SQLite is unavailable; Invitation cannot start. "
                    + "Recommend installing minecraft-sqlite-jdbc.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        InviteCommand invite = new InviteCommand(core);
        var inviteCommand = Objects.requireNonNull(getCommand("invite"), "invite command missing from plugin.yml");
        inviteCommand.setExecutor(invite);
        inviteCommand.setTabCompleter(invite);

        UninviteCommand uninvite = new UninviteCommand(core);
        var uninviteCommand = Objects.requireNonNull(getCommand("uninvite"), "uninvite command missing from plugin.yml");
        uninviteCommand.setExecutor(uninvite);
        uninviteCommand.setTabCompleter(uninvite);

        InviteLogCommand inviteLog = new InviteLogCommand(core);
        var inviteLogCommand = Objects.requireNonNull(getCommand("invitelog"), "invitelog command missing from plugin.yml");
        inviteLogCommand.setExecutor(inviteLog);
        inviteLogCommand.setTabCompleter(inviteLog);

        getServer().getPluginManager().registerEvents(new PlayerNameListener(core), this);
    }

    public InvitationCore core() {
        return core;
    }

    @Override
    public void onDisable() {
        if (core != null) {
            core.stop();
        }
    }
}
