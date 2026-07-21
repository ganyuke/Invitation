package io.github.ganyuke.invitation.spigot.commands;

import io.github.ganyuke.invitation.core.InvitationCore;
import io.github.ganyuke.invitation.spigot.ports.BukkitLoggerPort;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

public final class InviteLogCommand implements CommandExecutor, TabCompleter {
    private final InvitationCore core;

    public InviteLogCommand(InvitationCore core) {
        this.core = core;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        return core.inviteLogService().handleInviteLog(
                BukkitLoggerPort.toAudience(sender), cmd.getUsage(), args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return core.inviteLogService().tabComplete(args);
    }
}
