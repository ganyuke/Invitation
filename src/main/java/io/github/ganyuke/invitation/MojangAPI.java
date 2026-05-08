package io.github.ganyuke.invitation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class MojangAPI {

    private static final int TIMEOUT_MS = 5_000;

    private MojangAPI() {
    }

    public static MojangProfile lookup(String username) throws IOException {
        URL url = URI.create("https://api.mojang.com/users/profiles/minecraft/" + username).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);

            int responseCode = conn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_NO_CONTENT || responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                return null;
            }

            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new LookupException(responseCode);
            }

            String json = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

            UUID uuid = UUID.fromString(
                    obj.get("id").getAsString()
                            .replaceFirst(
                                    "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                                    "$1-$2-$3-$4-$5"
                            )
            );

            return new MojangProfile(uuid, obj.get("name").getAsString());
        } finally {
            conn.disconnect();
        }
    }

    public static final class LookupException extends IOException {
        private final int responseCode;

        private LookupException(int responseCode) {
            super("Mojang API returned HTTP " + responseCode);
            this.responseCode = responseCode;
        }

        public int responseCode() {
            return responseCode;
        }
    }
}
