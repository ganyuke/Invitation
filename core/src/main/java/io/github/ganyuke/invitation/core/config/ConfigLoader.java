package io.github.ganyuke.invitation.core.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigLoader {
    private ConfigLoader() {
    }

    public static InvitationConfig load(Path configFile) throws IOException {
        int cooldown = 7;
        int undo = 60;

        if (!Files.isRegularFile(configFile)) {
            return InvitationConfig.fromSeconds(cooldown, undo);
        }

        for (String line : Files.readAllLines(configFile, StandardCharsets.UTF_8)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            int colon = trimmed.indexOf(':');
            if (colon <= 0) {
                continue;
            }

            String key = trimmed.substring(0, colon).trim();
            String value = trimmed.substring(colon + 1).trim();
            if (value.isEmpty()) {
                continue;
            }

            try {
                int seconds = Integer.parseInt(value);
                if ("cooldown-seconds".equals(key)) {
                    cooldown = seconds;
                } else if ("undo-seconds".equals(key)) {
                    undo = seconds;
                }
            } catch (NumberFormatException ignored) {
                // skip bad lines
            }
        }

        return InvitationConfig.fromSeconds(cooldown, undo);
    }

    public static void ensureDefaultFromClasspath(Path configFile, Class<?> resourceAnchor) throws IOException {
        if (!Files.isRegularFile(configFile)) {
            Files.createDirectories(configFile.getParent());
            try (InputStream in = resourceAnchor.getResourceAsStream("/config.yml")) {
                if (in == null) {
                    throw new IOException("Default config.yml missing from jar");
                }
                Files.copy(in, configFile);
            }
        }
    }
}
