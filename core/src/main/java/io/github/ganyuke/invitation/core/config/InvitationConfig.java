package io.github.ganyuke.invitation.core.config;

public record InvitationConfig(long inviteCooldownMs, long undoWindowMs) {
    private static final int DEFAULT_COOLDOWN_SECONDS = 7;
    private static final int DEFAULT_UNDO_SECONDS = 60;

    public static InvitationConfig defaults() {
        return fromSeconds(DEFAULT_COOLDOWN_SECONDS, DEFAULT_UNDO_SECONDS);
    }

    public static InvitationConfig fromSeconds(int cooldownSeconds, int undoSeconds) {
        return new InvitationConfig(
                Math.max(0, cooldownSeconds) * 1000L,
                Math.max(0, undoSeconds) * 1000L
        );
    }
}
