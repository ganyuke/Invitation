package io.github.ganyuke.invitation;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class InviteLogCommand implements CommandExecutor, TabCompleter {
    private final Invitation plugin;

    public InviteLogCommand(Invitation plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command cmd, @NonNull String label, String[] args) {
        if (args.length > 1) {
            return false;
        }

        String target = args.length == 1 ? args[0] : null;

        sender.sendMessage("§6Invite Log: §7(loading...)");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<String> lines = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

            try {
                var entries = plugin.getDatabase().getLogs(target);
                for (var entry : entries) {
                    lines.add(String.format(
                            "§7%s §f→ §a%s §8(%s)",
                            entry.inviterName(),
                            entry.invitedName(),
                            sdf.format(new Date(entry.time()))
                    ));
                }
            } catch (SQLException e) {
                e.printStackTrace();
                lines.add("§cFailed to read invite log.");
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                // back on main thread for messaging
                if (lines.isEmpty()) {
                    sender.sendMessage("§7No invite entries found.");
                } else {
                    for (String line : lines) sender.sendMessage(line);
                }
            });
        });

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NonNull CommandSender sender, @NonNull Command command, @NonNull String alias, @NonNull String[] args) {
        if (args.length == 1) {
            return plugin.getDatabase().getInvitedPlayerNames(args[0]);
        }
        return List.of();
    }
}
