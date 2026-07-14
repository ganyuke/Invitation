package io.github.ganyuke.invitation;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public final class Invitation extends JavaPlugin {

    private static final int DEFAULT_COOLDOWN_SECONDS = 7;
    private static final int DEFAULT_UNDO_SECONDS = 60;

    private Database database;
    private final UninviteRegistry uninviteRegistry = new UninviteRegistry();
    private long inviteCooldownMs = DEFAULT_COOLDOWN_SECONDS * 1000L;
    private long undoWindowMs = DEFAULT_UNDO_SECONDS * 1000L;

    @Override
    public void onEnable() {
        if (!getServer().hasWhitelist()) {
            getLogger().warning("why are you using this plugin without a whitelist enabled...? but, sure, you do you :)");
        }

        saveDefaultConfig();
        inviteCooldownMs = Math.max(0, getConfig().getInt("cooldown-seconds", DEFAULT_COOLDOWN_SECONDS)) * 1000L;
        undoWindowMs = Math.max(0, getConfig().getInt("undo-seconds", DEFAULT_UNDO_SECONDS)) * 1000L;

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

        database = new Database(dataFolder, getLogger());
        database.init();

        InviteCommand invite = new InviteCommand(this);
        var inviteCommand = Objects.requireNonNull(getCommand("invite"), "invite command missing from plugin.yml");
        inviteCommand.setExecutor(invite);
        inviteCommand.setTabCompleter(invite);

        UninviteCommand uninvite = new UninviteCommand(this);
        var uninviteCommand = Objects.requireNonNull(getCommand("uninvite"), "uninvite command missing from plugin.yml");
        uninviteCommand.setExecutor(uninvite);
        uninviteCommand.setTabCompleter(uninvite);

        InviteLogCommand inviteLog = new InviteLogCommand(this);
        var inviteLogCommand = Objects.requireNonNull(getCommand("invitelog"), "invitelog command missing from plugin.yml");
        inviteLogCommand.setExecutor(inviteLog);
        inviteLogCommand.setTabCompleter(inviteLog);

        getServer().getPluginManager().registerEvents(new PlayerNameListener(this), this);
    }

    public Database getDatabase() {
        return database;
    }

    public UninviteRegistry getUninviteRegistry() {
        return uninviteRegistry;
    }

    public long getInviteCooldownMs() {
        return inviteCooldownMs;
    }

    public long getUndoWindowMs() {
        return undoWindowMs;
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.shutdown();
        }
    }
}
