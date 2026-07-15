package io.github.ganyuke.invitation;

import io.github.ganyuke.invitation.ports.LoggerPort;
import io.github.ganyuke.invitation.ports.WhitelistPort;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.nio.file.Path;
import java.util.UUID;

public final class BukkitWhitelistPort implements WhitelistPort {
    private final LoggerPort logger;

    public BukkitWhitelistPort(LoggerPort logger) {
        this.logger = logger;
    }

    @Override
    public boolean isEnabled() {
        return Bukkit.getServer().hasWhitelist();
    }

    @Override
    public boolean isWhitelisted(UUID uuid) {
        return Bukkit.getWhitelistedPlayers().stream()
                .anyMatch(p -> p.getUniqueId().equals(uuid));
    }

    @Override
    public void add(UUID uuid, String name) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        player.setWhitelisted(true);
        Path whitelist = Bukkit.getWorldContainer().toPath().resolve("whitelist.json");
        WhitelistJson.setEntryName(whitelist, uuid, name, logger, Bukkit::reloadWhitelist);
    }

    @Override
    public void remove(UUID uuid) {
        Bukkit.getOfflinePlayer(uuid).setWhitelisted(false);
    }
}
