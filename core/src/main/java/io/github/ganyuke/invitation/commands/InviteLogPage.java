package io.github.ganyuke.invitation;

import java.util.List;

public record InviteLogPage(List<InviteLogEntry> entries, boolean hasNext) {
    public static InviteLogPage empty() {
        return new InviteLogPage(List.of(), false);
    }
}
