package io.github.ganyuke.invitation;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public final class Invitation extends JavaPlugin {

    private Database database;

    @Override
    public void onEnable() {
        if (!getServer().hasWhitelist()) {
            getLogger().warning("why are you using this plugin without a whitelist enabled...? but, sure, you do you :)");
        }

        // Plugin startup logic
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

        database = new Database(dataFolder);
        database.init();

        Objects.requireNonNull(getCommand("invite"), "invite command missing from plugin.yml").setExecutor(new InviteCommand(this));
        Objects.requireNonNull(getCommand("invitelog"), "invitelog command missing from plugin.yml").setExecutor(new InviteLogCommand(this));
        Objects.requireNonNull(getCommand("invitelog"), "invitelog command missing from plugin.yml").setTabCompleter(new InviteLogCommand(this));
    }

    public Database getDatabase() {
        return database;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if (database != null)
            database.shutdown();
    }
}
