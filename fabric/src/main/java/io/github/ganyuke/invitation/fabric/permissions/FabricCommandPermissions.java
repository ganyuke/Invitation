package io.github.ganyuke.invitation.fabric.permissions;

import io.github.ganyuke.invitation.common.commands.CommandPermissionGate;
import io.github.ganyuke.invitation.core.InvitationPermission;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.util.TriState;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

public final class FabricCommandPermissions implements CommandPermissionGate {
    private final PermissionChecker checker;

    public FabricCommandPermissions(Logger logger) {
        // fallback to default permission requirements if fabric-permissions-api was not downloaded
        if (FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0")) {
            logger.info("Fabric Permissions API found. Invitation can now use permissions.");
            this.checker = (source, perm) -> {
                TriState state = Permissions.getPermissionValue(source, perm.getNode());
                return state != TriState.DEFAULT ? state.get() : fallback(source, perm);
            };
        } else {
            logger.warn("Fabric Permissions API was missing. Falling back to default permissions.");
            this.checker = this::fallback;
        }
    }

    @Override
    public Predicate<CommandSourceStack> inviteUse() {
        return source -> checker.test(source, InvitationPermission.USE);
    }

    @Override
    public Predicate<CommandSourceStack> inviteLog() {
        return source -> !source.isPlayer() || checker.test(source, InvitationPermission.LOG);
    }

    private boolean fallback(CommandSourceStack source, InvitationPermission permission) {
        return switch (permission) {
            // INVITE can be used by anyone
            case USE:
                yield true;
            // LOG can only be used by operators
            case LOG: {
                ServerPlayer player = source.getPlayer();
                if (player != null) {
                    yield source.getServer().getPlayerList().isOp(player.nameAndId());
                } else {
                    // if player is null, the console must be running this
                    yield true;
                }
            }
        };
    }

    private interface PermissionChecker extends BiPredicate<CommandSourceStack, InvitationPermission> {
    }
}
