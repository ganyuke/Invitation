package io.github.ganyuke.invitation.core.ports;

import java.util.UUID;

public interface Audience {
    String senderKey();

    record Player(UUID uuid, String name) implements Audience {
        @Override
        public String senderKey() {
            return uuid.toString();
        }
    }

    record Console() implements Audience {
        @Override
        public String senderKey() {
            return "console";
        }
    }
}
