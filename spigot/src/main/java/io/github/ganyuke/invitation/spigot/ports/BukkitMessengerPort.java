package io.github.ganyuke.invitation.spigot.ports;

import io.github.ganyuke.invitation.core.ports.Audience;
import io.github.ganyuke.invitation.core.ports.MessengerPort;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class BukkitMessengerPort implements MessengerPort {

    @Override
    public void sendPlain(Audience audience, String message) {
        CommandSender sender = resolveSender(audience);
        if (sender != null) {
            sender.sendMessage(message);
        }
    }

    @Override
    public void sendPlainIfOnline(UUID playerId, String message) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            player.sendMessage(message);
        }
    }

    @Override
    public void sendInviteSuccess(UUID inviterUuid, String invitedName) {
        Player player = Bukkit.getPlayer(inviterUuid);
        if (player == null) {
            return;
        }

        TextComponent message = new TextComponent("Invited " + invitedName + ". They are now whitelisted on the server. ");
        message.setColor(ChatColor.GREEN);

        TextComponent undo = new TextComponent("Undo");
        undo.setColor(ChatColor.YELLOW);
        undo.setUnderlined(true);
        undo.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/uninvite"));
        undo.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("Revoke this invite").create()
        ));

        player.spigot().sendMessage(message, undo);
    }

    @Override
    public void sendInviteLogNav(Audience audience, boolean hasPrev, boolean hasNext,
                                 String prevCommand, String nextCommand) {
        CommandSender sender = resolveSender(audience);
        if (sender == null) {
            return;
        }

        if (sender instanceof Player player) {
            List<TextComponent> parts = new ArrayList<>();
            if (hasPrev) {
                parts.add(navLink("← Prev", prevCommand, "Previous page"));
            }
            if (hasPrev && hasNext) {
                TextComponent sep = new TextComponent("  ");
                sep.setColor(ChatColor.DARK_GRAY);
                parts.add(sep);
            }
            if (hasNext) {
                parts.add(navLink("Next →", nextCommand, "Next page"));
            }
            player.spigot().sendMessage(parts.toArray(TextComponent[]::new));
            return;
        }

        if (hasPrev) {
            sender.sendMessage("§ePrev: §f" + prevCommand);
        }
        if (hasNext) {
            sender.sendMessage("§eNext: §f" + nextCommand);
        }
    }

    private static TextComponent navLink(String label, String command, String hover) {
        TextComponent link = new TextComponent(label);
        link.setColor(ChatColor.YELLOW);
        link.setUnderlined(true);
        link.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        link.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(hover).create()
        ));
        return link;
    }

    private static CommandSender resolveSender(Audience audience) {
        if (audience instanceof Audience.Player player) {
            return Bukkit.getPlayer(player.uuid());
        }
        return Bukkit.getConsoleSender();
    }
}
