package io.github.ganyuke.invitation.fabric;

import io.github.ganyuke.invitation.InvitationPermissions;
import io.github.ganyuke.invitation.common.CommandPermissionGate;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

import java.util.function.Predicate;

public final class FabricCommandPermissions implements CommandPermissionGate {
    @Override
    public Predicate<CommandSourceStack> inviteUse() {
        return source -> check(source, InvitationPermissions.USE, true);
    }

    @Override
    public Predicate<CommandSourceStack> inviteLog() {
        return source -> {
            if (!source.isPlayer()) {
                return true;
            }
            return check(source, InvitationPermissions.LOG, false);
        };
    }

    private static boolean check(CommandSourceStack source, String permission, boolean defaultValue) {
        TriState state = Permissions.getPermissionValue(source, permission);
        if (state != TriState.DEFAULT) {
            return state.get();
        }
        if (InvitationPermissions.LOG.equals(permission)) {
            ServerPlayer player = source.getPlayer();
            return player != null && source.getServer().getPlayerList().isOp(
                    new NameAndId(player.getUUID(), player.getPlainTextName()));
        }
        return defaultValue;
    }
}
