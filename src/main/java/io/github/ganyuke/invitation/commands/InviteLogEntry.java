package io.github.ganyuke.invitation;

public record InviteLogEntry(
        String inviterUuid,
        String inviterName,
        String invitedUuid,
        String invitedName,
        long time
) {
}