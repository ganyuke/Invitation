package io.github.ganyuke.invitation.core.commands;

public record InviteLogEntry(
        String inviterUuid,
        String inviterName,
        String invitedUuid,
        String invitedName,
        long time
) {
}
