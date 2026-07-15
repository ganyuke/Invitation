package io.github.ganyuke.invitation.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

public final class InvitationPermissionNodes {
    public static final PermissionNode<Boolean> INVITE_USE = new PermissionNode<>(
            "invite",
            "use",
            PermissionTypes.BOOLEAN,
            (player, uuid, context) -> true
    );

    public static final PermissionNode<Boolean> INVITE_LOG = new PermissionNode<>(
            "invite",
            "log",
            PermissionTypes.BOOLEAN,
            (player, uuid, context) -> player != null && player.level().getServer().getPlayerList().isOp(
                    new NameAndId(player.getUUID(), player.getPlainTextName()))
    );

    private InvitationPermissionNodes() {
    }
}
