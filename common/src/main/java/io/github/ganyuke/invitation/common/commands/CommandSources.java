package io.github.ganyuke.invitation.common.commands;

import io.github.ganyuke.invitation.core.ports.Audience;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

public final class CommandSources {
    private CommandSources() {
    }

    public static Audience toAudience(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return new Audience.Player(player.getUUID(), player.getPlainTextName());
        }
        return new Audience.Console();
    }
}
