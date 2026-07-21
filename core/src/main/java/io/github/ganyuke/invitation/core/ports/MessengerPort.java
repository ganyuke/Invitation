package io.github.ganyuke.invitation.core.ports;

import java.util.UUID;

public interface MessengerPort {
    void sendPlain(Audience audience, String message);

    void sendPlainIfOnline(UUID playerId, String message);

    void sendInviteSuccess(UUID inviterUuid, String invitedName);

    void sendInviteLogNav(Audience audience, boolean hasPrev, boolean hasNext, String prevCommand, String nextCommand);
}
