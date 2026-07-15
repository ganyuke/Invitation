package io.github.ganyuke.invitation;

import io.github.ganyuke.invitation.ports.Audience;
import io.github.ganyuke.invitation.ports.LoggerPort;
import io.github.ganyuke.invitation.ports.MessengerPort;
import io.github.ganyuke.invitation.ports.WhitelistPort;

public final class UninviteService {

    private final UninviteRegistry uninviteRegistry;
    private final InvitationConfig config;
    private final MessengerPort messenger;
    private final WhitelistPort whitelist;
    private final LoggerPort logger;

    public UninviteService(UninviteRegistry uninviteRegistry,
                           InvitationConfig config,
                           MessengerPort messenger,
                           WhitelistPort whitelist,
                           LoggerPort logger) {
        this.uninviteRegistry = uninviteRegistry;
        this.config = config;
        this.messenger = messenger;
        this.whitelist = whitelist;
        this.logger = logger;
    }

    public boolean handleUninvite(Audience audience) {
        if (!(audience instanceof Audience.Player player)) {
            messenger.sendPlain(audience, "§cOnly players can use this command.");
            return true;
        }

        var result = uninviteRegistry.tryRevoke(player.uuid(), config.undoWindowMs());
        return switch (result.status()) {
            case NOT_FOUND -> {
                messenger.sendPlain(audience, "§cNo recent invite to revoke.");
                yield true;
            }
            case EXPIRED -> {
                messenger.sendPlain(audience, "§cRevocation window for that invite has expired.");
                yield true;
            }
            case CLAIM_HOLDER_MISMATCH -> {
                messenger.sendPlain(audience, "§cThis player has already been re-invited by someone else.");
                yield true;
            }
            case SUCCESS -> {
                whitelist.remove(result.invitedUuid());

                logger.info(() -> String.format(
                        "Revoked invite for %s (%s) by %s (%s)",
                        result.invitedName(), result.invitedUuid(),
                        player.name(), player.uuid()
                ));

                messenger.sendPlain(audience, "§aRevoked invite for " + result.invitedName() + ".");
                yield true;
            }
        };
    }
}
