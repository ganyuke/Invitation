package io.github.ganyuke.invitation.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.ganyuke.invitation.core.ports.LoggerPort;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Update the whitelist.json file so that names show up in /whitelist list.
 */
public final class WhitelistJson {
    private WhitelistJson() {
    }

    public static synchronized void setEntryName(Path whitelistFile, UUID uuid, String name,
                                                 LoggerPort logger, Runnable reloadWhitelist) {
        if (!Files.isRegularFile(whitelistFile)) {
            logger.warning("whitelist.json not found; could not set name for " + uuid);
            return;
        }

        try {
            String raw = Files.readString(whitelistFile, StandardCharsets.UTF_8);
            JsonElement root = new JsonParser().parse(raw);
            if (!root.isJsonArray()) {
                logger.warning("whitelist.json is not a JSON array; could not set name for " + uuid);
                return;
            }

            JsonArray entries = root.getAsJsonArray();
            String target = uuid.toString();
            JsonObject match = null;
            for (JsonElement element : entries) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject entry = element.getAsJsonObject();
                if (!entry.has("uuid")) {
                    continue;
                }
                if (target.equalsIgnoreCase(entry.get("uuid").getAsString())) {
                    match = entry;
                    break;
                }
            }

            if (match == null) {
                logger.warning("No whitelist.json entry for " + uuid + "; could not set name");
                return;
            }

            Path bak = whitelistFile.resolveSibling("whitelist.json.bak");
            Files.copy(whitelistFile, bak, StandardCopyOption.REPLACE_EXISTING);

            match.addProperty("name", name);
            Files.writeString(whitelistFile, entries.toString(), StandardCharsets.UTF_8);
            reloadWhitelist.run();
        } catch (IOException | RuntimeException e) {
            logger.warning("Failed to set whitelist name for " + uuid + ": " + e.getMessage());
        }
    }
}
