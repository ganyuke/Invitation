package io.github.ganyuke.invitation.common;

import net.minecraft.commands.CommandSourceStack;

import java.util.function.Predicate;

public interface CommandPermissionGate {
    Predicate<CommandSourceStack> inviteUse();

    Predicate<CommandSourceStack> inviteLog();
}
