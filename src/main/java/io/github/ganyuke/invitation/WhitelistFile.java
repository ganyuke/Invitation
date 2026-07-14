package io.github.ganyuke.invitation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Update the whitelist.json file so that names show up in /whitelist list.
 */
public final class WhitelistFile {
    private WhitelistFile() {
    }

    public static synchronized void setEntryName(UUID uuid, String name) {
        Logger logger = Bukkit.getLogger();
        Path whitelist = Bukkit.getWorldContainer().toPath().resolve("whitelist.json");
        if (!Files.isRegularFile(whitelist)) {
            logger.warning("whitelist.json not found; could not set name for " + uuid);
            return;
        }

        try {
            String raw = Files.readString(whitelist, StandardCharsets.UTF_8);
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

            Path bak = whitelist.resolveSibling("whitelist.json.bak");
            Files.copy(whitelist, bak, StandardCopyOption.REPLACE_EXISTING);

            match.addProperty("name", name);
            Files.writeString(whitelist, entries.toString(), StandardCharsets.UTF_8);
            Bukkit.reloadWhitelist();
        } catch (IOException | RuntimeException e) {
            logger.warning("Failed to set whitelist name for " + uuid + ": " + e.getMessage());
        }
    }
}
