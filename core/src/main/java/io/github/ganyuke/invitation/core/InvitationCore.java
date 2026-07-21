package io.github.ganyuke.invitation.core;

import io.github.ganyuke.invitation.core.commands.InviteLogService;
import io.github.ganyuke.invitation.core.commands.InviteService;
import io.github.ganyuke.invitation.core.commands.UninviteRegistry;
import io.github.ganyuke.invitation.core.commands.UninviteService;
import io.github.ganyuke.invitation.core.config.InvitationConfig;
import io.github.ganyuke.invitation.core.db.Database;
import io.github.ganyuke.invitation.core.db.PlayerNameService;
import io.github.ganyuke.invitation.core.ports.LoggerPort;
import io.github.ganyuke.invitation.core.ports.MessengerPort;
import io.github.ganyuke.invitation.core.ports.SchedulerPort;
import io.github.ganyuke.invitation.core.ports.WhitelistPort;

import java.nio.file.Files;
import java.nio.file.Path;

public final class InvitationCore {
    private final Database database;
    private final UninviteRegistry uninviteRegistry;
    private final InvitationConfig config;
    private final InviteService inviteService;
    private final UninviteService uninviteService;
    private final InviteLogService inviteLogService;
    private final PlayerNameService playerNameService;

    private InvitationCore(Database database,
                           UninviteRegistry uninviteRegistry,
                           InvitationConfig config,
                           InviteService inviteService,
                           UninviteService uninviteService,
                           InviteLogService inviteLogService,
                           PlayerNameService playerNameService) {
        this.database = database;
        this.uninviteRegistry = uninviteRegistry;
        this.config = config;
        this.inviteService = inviteService;
        this.uninviteService = uninviteService;
        this.inviteLogService = inviteLogService;
        this.playerNameService = playerNameService;
    }

    public static InvitationCore start(Path dataDir,
                                       InvitationConfig config,
                                       WhitelistPort whitelist,
                                       SchedulerPort scheduler,
                                       MessengerPort messenger,
                                       LoggerPort logger) throws SqliteUnavailableException {
        if (!Files.isDirectory(dataDir)) {
            try {
                Files.createDirectories(dataDir);
            } catch (Exception e) {
                throw new IllegalStateException("Could not create plugin data folder", e);
            }
        }

        Database database = new Database(dataDir, logger);
        database.init();

        UninviteRegistry uninviteRegistry = new UninviteRegistry();

        InviteService inviteService = new InviteService(
                database, uninviteRegistry, config, scheduler, messenger, whitelist, logger);
        UninviteService uninviteService = new UninviteService(
                uninviteRegistry, config, messenger, whitelist, logger);
        InviteLogService inviteLogService = new InviteLogService(
                database, scheduler, messenger, logger);
        PlayerNameService playerNameService = new PlayerNameService(database, scheduler);

        return new InvitationCore(
                database, uninviteRegistry, config,
                inviteService, uninviteService, inviteLogService, playerNameService);
    }

    public void stop() {
        database.shutdown();
    }

    public Database database() {
        return database;
    }

    public UninviteRegistry uninviteRegistry() {
        return uninviteRegistry;
    }

    public InvitationConfig config() {
        return config;
    }

    public InviteService inviteService() {
        return inviteService;
    }

    public UninviteService uninviteService() {
        return uninviteService;
    }

    public InviteLogService inviteLogService() {
        return inviteLogService;
    }

    public PlayerNameService playerNameService() {
        return playerNameService;
    }
}
