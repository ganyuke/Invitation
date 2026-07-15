package io.github.ganyuke.invitation.ports;

import java.util.function.Supplier;

public interface LoggerPort {
    void info(String message);

    void info(Supplier<String> message);

    void warning(String message);

    void severe(String message);
}
