package io.github.ganyuke.invitation.common.ports;

import io.github.ganyuke.invitation.core.WhitelistJson;
import io.github.ganyuke.invitation.core.ports.LoggerPort;
import io.github.ganyuke.invitation.core.ports.WhitelistPort;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserWhiteListEntry;

import java.nio.file.Path;
import java.util.UUID;

public final class VanillaWhitelistPort implements WhitelistPort {
    private final MinecraftServer server;
    private final LoggerPort logger;

    public VanillaWhitelistPort(MinecraftServer server, LoggerPort logger) {
        this.server = server;
        this.logger = logger;
    }

    @Override
    public boolean isEnabled() {
        return server.isUsingWhitelist();
    }

    @Override
    public boolean isWhitelisted(UUID uuid) {
        // check if name is in the whitelist
        // need to check this instead of isWhitelisted since that one returns true
        // for all arguments on offline servers, which is bad for Velocity
        return server.getPlayerList().getWhiteList().isWhiteListed(new NameAndId(uuid, ""));
    }

    @Override
    public void add(UUID uuid, String name) {
        server.getPlayerList().getWhiteList().add(new UserWhiteListEntry(new NameAndId(uuid, name)));
        Path whitelist = server.getServerDirectory().resolve("whitelist.json");
        WhitelistJson.setEntryName(whitelist, uuid, name, logger, () -> server.getPlayerList().reloadWhiteList());
    }

    @Override
    public void remove(UUID uuid) {
        server.getPlayerList().getWhiteList().remove(new NameAndId(uuid, ""));
    }
}
