package io.github.ganyuke.invitation.core.commands;

import java.util.List;

public record InviteLogPage(List<InviteLogEntry> entries, boolean hasNext) {
    public static InviteLogPage empty() {
        return new InviteLogPage(List.of(), false);
    }
}
