package io.github.ganyuke.invitation.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.github.ganyuke.invitation.core.InvitationCore;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class InvitationCommands {
    private InvitationCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                InvitationCore core,
                                CommandPermissionGate permissions) {
        dispatcher.register(Commands.literal("invite")
                .requires(permissions.inviteUse())
                .then(Commands.argument("player", StringArgumentType.word())
                        .executes(context -> {
                            boolean handled = core.inviteService().handleInvite(
                                    CommandSources.toAudience(context.getSource()),
                                    "invite",
                                    StringArgumentType.getString(context, "player")
                            );
                            return handled ? 1 : 0;
                        })));

        dispatcher.register(Commands.literal("uninvite")
                .requires(permissions.inviteUse())
                .executes(context -> {
                    boolean handled = core.uninviteService().handleUninvite(
                            CommandSources.toAudience(context.getSource())
                    );
                    return handled ? 1 : 0;
                }));

        dispatcher.register(Commands.literal("invitelog")
                .requires(permissions.inviteLog())
                .executes(context -> runInviteLog(core, context.getSource(), new String[0]))
                .then(Commands.literal("recent")
                        .executes(context -> runInviteLog(core, context.getSource(), new String[]{"recent"}))
                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                .executes(context -> runInviteLog(core, context.getSource(), new String[]{
                                        "recent",
                                        String.valueOf(IntegerArgumentType.getInteger(context, "page"))
                                }))))
                .then(Commands.literal("sent")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(nameSuggestions(core))
                                .executes(context -> runInviteLog(core, context.getSource(), new String[]{
                                        "sent",
                                        StringArgumentType.getString(context, "player")
                                }))
                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                        .executes(context -> runInviteLog(core, context.getSource(), new String[]{
                                                "sent",
                                                StringArgumentType.getString(context, "player"),
                                                String.valueOf(IntegerArgumentType.getInteger(context, "page"))
                                        })))))
                .then(Commands.literal("recieved")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(nameSuggestions(core))
                                .executes(context -> runInviteLog(core, context.getSource(), new String[]{
                                        "recieved",
                                        StringArgumentType.getString(context, "player")
                                }))
                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                        .executes(context -> runInviteLog(core, context.getSource(), new String[]{
                                                "recieved",
                                                StringArgumentType.getString(context, "player"),
                                                String.valueOf(IntegerArgumentType.getInteger(context, "page"))
                                        })))))
                .then(Commands.literal("received")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(nameSuggestions(core))
                                .executes(context -> runInviteLog(core, context.getSource(), new String[]{
                                        "received",
                                        StringArgumentType.getString(context, "player")
                                }))
                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                        .executes(context -> runInviteLog(core, context.getSource(), new String[]{
                                                "received",
                                                StringArgumentType.getString(context, "player"),
                                                String.valueOf(IntegerArgumentType.getInteger(context, "page"))
                                        }))))));
    }

    private static int runInviteLog(InvitationCore core, CommandSourceStack source, String[] args) {
        boolean handled = core.inviteLogService().handleInviteLog(
                CommandSources.toAudience(source),
                "/invitelog [recent [page]] OR /invitelog <sent|recieved> <player> [page]",
                args
        );
        return handled ? 1 : 0;
    }

    private static SuggestionProvider<CommandSourceStack> nameSuggestions(InvitationCore core) {
        return (context, builder) -> {
            String remaining = builder.getRemaining().toLowerCase();
            for (String name : core.database().getKnownPlayerNames(remaining)) {
                if (name.toLowerCase().startsWith(remaining)) {
                    builder.suggest(name);
                }
            }
            return builder.buildFuture();
        };
    }
}
