package io.github.ganyuke.invitation;

import io.github.ganyuke.invitation.ports.Audience;
import io.github.ganyuke.invitation.ports.LoggerPort;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.function.Supplier;
import java.util.logging.Logger;

public final class BukkitLoggerPort implements LoggerPort {
    private final Logger logger;

    public BukkitLoggerPort(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void info(String message) {
        logger.info(message);
    }

    @Override
    public void info(Supplier<String> message) {
        logger.info(message);
    }

    @Override
    public void warning(String message) {
        logger.warning(message);
    }

    @Override
    public void severe(String message) {
        logger.severe(message);
    }

    public static Audience toAudience(CommandSender sender) {
        if (sender instanceof Player player) {
            return new Audience.Player(player.getUniqueId(), player.getName());
        }
        return new Audience.Console();
    }
}
