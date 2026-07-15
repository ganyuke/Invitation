package io.github.ganyuke.invitation.common;

import io.github.ganyuke.invitation.InvitationCore;
import net.minecraft.server.level.ServerPlayer;

public final class PlayerJoinHandler {
    private PlayerJoinHandler() {
    }

    public static void onJoin(InvitationCore core, ServerPlayer player) {
        core.playerNameService().onPlayerJoin(player.getUUID(), player.getPlainTextName());
    }
}
