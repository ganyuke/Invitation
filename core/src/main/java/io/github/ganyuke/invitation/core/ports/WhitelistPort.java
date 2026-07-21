package io.github.ganyuke.invitation.core.ports;

import java.util.UUID;

public interface WhitelistPort {
    boolean isEnabled();

    boolean isWhitelisted(UUID uuid);

    void add(UUID uuid, String name);

    void remove(UUID uuid);
}
