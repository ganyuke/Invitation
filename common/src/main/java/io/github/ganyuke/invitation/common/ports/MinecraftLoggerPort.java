package io.github.ganyuke.invitation.common;

import io.github.ganyuke.invitation.ports.LoggerPort;
import org.slf4j.Logger;

import java.util.function.Supplier;

public final class MinecraftLoggerPort implements LoggerPort {
    private final Logger logger;

    public MinecraftLoggerPort(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void info(String message) {
        logger.info(message);
    }

    @Override
    public void info(Supplier<String> message) {
        logger.info(message.get());
    }

    @Override
    public void warning(String message) {
        logger.warn(message);
    }

    @Override
    public void severe(String message) {
        logger.error(message);
    }
}
