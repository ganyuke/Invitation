package io.github.ganyuke.invitation;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class InviteLogCommand implements CommandExecutor, TabCompleter {

    private final Invitation plugin;
    private final Set<String> inFlight = ConcurrentHashMap.newKeySet();

    public InviteLogCommand(Invitation plugin) {
        this.plugin = plugin;
    }

    private enum Mode {
        RECENT,
        SENT,
        RECEIVED
    }

    private record Query(Mode mode, String player, int page) {
        static Query recent(int page) {
            return new Query(Mode.RECENT, null, page);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Query query = parse(args);
        if (query == null) {
            sender.sendMessage("§cUsage: " + cmd.getUsage());
            return true;
        }

        String senderKey = sender instanceof Player player
                ? player.getUniqueId().toString()
                : "console";
        if (!inFlight.add(senderKey)) {
            sender.sendMessage("§cPlease wait for the previous invite log to finish loading.");
            return true;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<String> lines = new ArrayList<>();
            boolean failed = false;
            boolean hasNext = false;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

            try {
                InviteLogPage page = fetch(query);
                hasNext = page.hasNext();
                for (InviteLogEntry entry : page.entries()) {
                    lines.add(String.format(
                            "§7%s §f→ §a%s §8(%s)",
                            entry.inviterName(),
                            entry.invitedName(),
                            sdf.format(new Date(entry.time()))
                    ));
                }
            } catch (SQLException e) {
                failed = true;
                plugin.getLogger().severe("Failed to read invite log: " + e.getMessage());
            }

            boolean readFailed = failed;
            boolean showNext = hasNext;
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    if (readFailed) {
                        sender.sendMessage("§cFailed to read invite log.");
                        return;
                    }
                    if (lines.isEmpty()) {
                        sender.sendMessage("§7No invite entries found.");
                        if (query.page() > 1) {
                            sendNav(sender, query, false);
                        }
                        return;
                    }
                    sender.sendMessage(header(query));
                    for (String line : lines) {
                        sender.sendMessage(line);
                    }
                    if (query.page() > 1 || showNext) {
                        sendNav(sender, query, showNext);
                    }
                } finally {
                    inFlight.remove(senderKey);
                }
            });
        });

        return true;
    }

    private static String header(Query query) {
        return switch (query.mode()) {
            case RECENT -> "§6Recent invites §7(page " + query.page() + ")";
            case SENT -> "§6Sent invites for §f" + query.player() + " §7(page " + query.page() + ")";
            case RECEIVED -> "§6Received invites for §f" + query.player() + " §7(page " + query.page() + ")";
        };
    }

    private static String commandFor(Query query, int page) {
        return switch (query.mode()) {
            case RECENT -> "/invitelog recent " + page;
            case SENT -> "/invitelog sent " + query.player() + " " + page;
            case RECEIVED -> "/invitelog recieved " + query.player() + " " + page;
        };
    }

    private static void sendNav(CommandSender sender, Query query, boolean hasNext) {
        boolean hasPrev = query.page() > 1;

        if (sender instanceof Player player) {
            List<TextComponent> parts = new ArrayList<>();
            if (hasPrev) {
                parts.add(navLink("← Prev", commandFor(query, query.page() - 1), "Previous page"));
            }
            if (hasPrev && hasNext) {
                TextComponent sep = new TextComponent("  ");
                sep.setColor(ChatColor.DARK_GRAY);
                parts.add(sep);
            }
            if (hasNext) {
                parts.add(navLink("Next →", commandFor(query, query.page() + 1), "Next page"));
            }
            player.spigot().sendMessage(parts.toArray(TextComponent[]::new));
            return;
        }

        if (hasPrev) {
            sender.sendMessage("§ePrev: §f" + commandFor(query, query.page() - 1));
        }
        if (hasNext) {
            sender.sendMessage("§eNext: §f" + commandFor(query, query.page() + 1));
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

    private Query parse(String[] args) {
        if (args.length == 0) {
            return Query.recent(1);
        }

        Mode mode = parseMode(args[0]);
        if (mode == null) {
            return null;
        }

        if (mode == Mode.RECENT) {
            if (args.length == 1) {
                return Query.recent(1);
            }
            if (args.length == 2) {
                Integer page = parsePage(args[1]);
                return page == null ? null : Query.recent(page);
            }
            return null;
        }

        if (args.length == 2) {
            return new Query(mode, args[1], 1);
        }
        if (args.length == 3) {
            Integer page = parsePage(args[2]);
            return page == null ? null : new Query(mode, args[1], page);
        }

        return null;
    }

    private static Integer parsePage(String raw) {
        try {
            int page = Integer.parseInt(raw);
            return page < 1 ? null : page;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Mode parseMode(String raw) {
        String mode = raw.toLowerCase(Locale.ROOT);
        if (mode.equals("recent")) {
            return Mode.RECENT;
        }
        if (mode.equals("sent")) {
            return Mode.SENT;
        }
        if (mode.equals("recieved") || mode.equals("received")) {
            return Mode.RECEIVED;
        }
        return null;
    }

    private InviteLogPage fetch(Query query) throws SQLException {
        Database db = plugin.getDatabase();
        return switch (query.mode()) {
            case RECENT -> db.getRecentLogs(query.page());
            case SENT -> db.getSentLogs(query.player(), query.page());
            case RECEIVED -> db.getReceivedLogs(query.player(), query.page());
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return Stream.of("recent", "sent", "recieved")
                    .filter(s -> s.startsWith(prefix))
                    .toList();
        }
        if (args.length == 2) {
            Mode mode = parseMode(args[0]);
            if (mode == Mode.SENT || mode == Mode.RECEIVED) {
                return plugin.getDatabase().getKnownPlayerNames(args[1]);
            }
        }
        return List.of();
    }
}
