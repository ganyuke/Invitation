package io.github.ganyuke.invitation.common.listeners;

import io.github.ganyuke.invitation.core.InvitationCore;
import net.minecraft.server.level.ServerPlayer;

public final class PlayerJoinHandler {
    private PlayerJoinHandler() {
    }

    public static void onJoin(InvitationCore core, ServerPlayer player) {
        core.playerNameService().onPlayerJoin(player.getUUID(), player.getPlainTextName());
    }
}
