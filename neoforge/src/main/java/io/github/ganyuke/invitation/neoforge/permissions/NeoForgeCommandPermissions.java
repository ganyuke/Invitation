package io.github.ganyuke.invitation.neoforge.permissions;

import io.github.ganyuke.invitation.common.commands.CommandPermissionGate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.permission.PermissionAPI;

import java.util.function.Predicate;

public final class NeoForgeCommandPermissions implements CommandPermissionGate {
    @Override
    public Predicate<CommandSourceStack> inviteUse() {
        return source -> {
            if (!source.isPlayer()) {
                return true;
            }
            ServerPlayer player = source.getPlayer();
            return player != null && PermissionAPI.getPermission(player, InvitationPermissionNodes.INVITE_USE);
        };
    }

    @Override
    public Predicate<CommandSourceStack> inviteLog() {
        return source -> {
            if (!source.isPlayer()) {
                return true;
            }
            ServerPlayer player = source.getPlayer();
            return player != null && PermissionAPI.getPermission(player, InvitationPermissionNodes.INVITE_LOG);
        };
    }
}
