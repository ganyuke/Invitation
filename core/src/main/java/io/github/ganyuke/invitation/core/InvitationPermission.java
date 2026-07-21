package io.github.ganyuke.invitation.core;

public enum InvitationPermission {
    USE("invite.use"),
    LOG("invite.log");

    private final String node;

    InvitationPermission(String node) {
        this.node = node;
    }

    public String getNode() {
        return node;
    }
}
