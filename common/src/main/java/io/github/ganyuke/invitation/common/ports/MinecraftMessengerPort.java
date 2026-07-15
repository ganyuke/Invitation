package io.github.ganyuke.invitation.common;

import io.github.ganyuke.invitation.ports.Audience;
import io.github.ganyuke.invitation.ports.MessengerPort;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class MinecraftMessengerPort implements MessengerPort {
    private final MinecraftServer server;

    public MinecraftMessengerPort(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void sendPlain(Audience audience, String message) {
        sendToAudience(audience, fromLegacySection(message));
    }

    @Override
    public void sendPlainIfOnline(UUID playerId, String message) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player != null) {
            player.sendSystemMessage(fromLegacySection(message));
        }
    }

    @Override
    public void sendInviteSuccess(UUID inviterUuid, String invitedName) {
        ServerPlayer player = server.getPlayerList().getPlayer(inviterUuid);
        if (player == null) {
            return;
        }

        MutableComponent message = Component.literal("Invited " + invitedName + ". They are now whitelisted on the server. ")
                .withStyle(ChatFormatting.GREEN);
        MutableComponent undo = Component.literal("Undo")
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.YELLOW)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent.RunCommand("/uninvite"))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("Revoke this invite"))));

        player.sendSystemMessage(message.append(undo));
    }

    @Override
    public void sendInviteLogNav(Audience audience, boolean hasPrev, boolean hasNext,
                                 String prevCommand, String nextCommand) {
        if (audience instanceof Audience.Console) {
            if (hasPrev) {
                sendPlain(audience, "§ePrev: §f" + prevCommand);
            }
            if (hasNext) {
                sendPlain(audience, "§eNext: §f" + nextCommand);
            }
            return;
        }

        ServerPlayer player = server.getPlayerList().getPlayer(((Audience.Player) audience).uuid());
        if (player == null) {
            return;
        }

        MutableComponent nav = Component.empty();
        if (hasPrev) {
            nav.append(navLink("← Prev", prevCommand, "Previous page"));
        }
        if (hasPrev && hasNext) {
            nav.append(Component.literal("  ").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (hasNext) {
            nav.append(navLink("Next →", nextCommand, "Next page"));
        }
        player.sendSystemMessage(nav);
    }

    private static MutableComponent navLink(String label, String command, String hover) {
        return Component.literal(label)
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.YELLOW)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent.RunCommand(command))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal(hover))));
    }

    private void sendToAudience(Audience audience, Component component) {
        if (audience instanceof Audience.Player playerAudience) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerAudience.uuid());
            if (player != null) {
                player.sendSystemMessage(component);
            }
            return;
        }
        server.sendSystemMessage(component);
    }

    private static MutableComponent fromLegacySection(String message) {
        MutableComponent result = Component.empty();
        Style style = Style.EMPTY;
        StringBuilder segment = new StringBuilder();

        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (c == '§' && i + 1 < message.length()) {
                flushSegment(result, segment, style);
                ChatFormatting formatting = ChatFormatting.getByCode(message.charAt(++i));
                if (formatting != null) {
                    style = formatting == ChatFormatting.RESET ? Style.EMPTY : style.applyFormat(formatting);
                }
                continue;
            }
            segment.append(c);
        }

        flushSegment(result, segment, style);
        return result;
    }

    private static void flushSegment(MutableComponent result, StringBuilder segment, Style style) {
        if (segment.isEmpty()) {
            return;
        }
        result.append(Component.literal(segment.toString()).withStyle(style));
        segment.setLength(0);
    }
}
