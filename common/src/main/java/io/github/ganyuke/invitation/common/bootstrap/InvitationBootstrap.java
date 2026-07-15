package io.github.ganyuke.invitation.common;

import io.github.ganyuke.invitation.ConfigLoader;
import io.github.ganyuke.invitation.InvitationConfig;
import io.github.ganyuke.invitation.InvitationCore;
import io.github.ganyuke.invitation.SqliteUnavailableException;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Executor;

public final class InvitationBootstrap {
    private InvitationCore core;

    public boolean start(MinecraftServer server, Path configDir, Logger logger, Executor asyncExecutor) {
        MinecraftLoggerPort loggerPort = new MinecraftLoggerPort(logger);
        VanillaWhitelistPort whitelistPort = new VanillaWhitelistPort(server, loggerPort);

        if (!whitelistPort.isEnabled()) {
            logger.warn("why are you using this plugin without a whitelist enabled...? but, sure, you do you :)");
        }

        Path configFile = configDir.resolve("config.yml");
        try {
            ConfigLoader.ensureDefaultFromClasspath(configFile, ConfigLoader.class);
            InvitationConfig config = ConfigLoader.load(configFile);
            core = InvitationCore.start(
                    configDir,
                    config,
                    whitelistPort,
                    new MinecraftSchedulerPort(server, asyncExecutor),
                    new MinecraftMessengerPort(server),
                    loggerPort
            );
            return true;
        } catch (IOException e) {
            logger.error("Failed to load Invitation config: {}", e.getMessage());
            return false;
        } catch (SqliteUnavailableException e) {
            logger.error("SQLite is unavailable; Invitation cannot start. "
                    + "Recommend installing minecraft-sqlite-jdbc.");
            return false;
        }
    }

    public void stop() {
        if (core != null) {
            core.stop();
            core = null;
        }
    }

    public InvitationCore core() {
        return core;
    }
}
