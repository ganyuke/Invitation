package io.github.ganyuke.invitation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * In-memory undo window for invites. Does not touch the database.
 */
public final class UninviteRegistry {
    public record Pending(UUID invitedUuid, String invitedName, long invitedAtMillis) {}

    public enum Status {
        SUCCESS,
        NOT_FOUND,
        EXPIRED,
        CLAIM_HOLDER_MISMATCH,
    }

    public record Result(Status status, UUID invitedUuid, String invitedName) {}

    private final Map<UUID, Pending> lastInviteByInviter = new HashMap<>();
    private final Map<UUID, UUID> whitelistClaim = new HashMap<>();

    public synchronized void record(UUID inviterUuid, UUID invitedUuid, String invitedName) {
        lastInviteByInviter.put(inviterUuid, new Pending(invitedUuid, invitedName, System.currentTimeMillis()));
        whitelistClaim.put(invitedUuid, inviterUuid);
    }

    /**
     * If the inviter has a pending invite still inside {@code windowMs} and still holds the
     * whitelist claim for that invitee, removes the pending entry and returns it.
     */
    public synchronized Result tryRevoke(UUID inviterUuid, long windowMs) {
        Pending pending = lastInviteByInviter.get(inviterUuid);
        if (pending == null) {
            return new Result(Status.NOT_FOUND, null, null);
        }

        if (windowMs >= 0 && System.currentTimeMillis() - pending.invitedAtMillis() > windowMs) {
            lastInviteByInviter.remove(inviterUuid);
            return new Result(Status.EXPIRED, null, null);
        }

        UUID claimHolder = whitelistClaim.get(pending.invitedUuid());
        if (!inviterUuid.equals(claimHolder)) {
            lastInviteByInviter.remove(inviterUuid);
            return new Result(Status.CLAIM_HOLDER_MISMATCH, null, null);
        }

        lastInviteByInviter.remove(inviterUuid);
        whitelistClaim.remove(pending.invitedUuid(), inviterUuid);
        return new Result(Status.SUCCESS, pending.invitedUuid(), pending.invitedName());
    }
}
