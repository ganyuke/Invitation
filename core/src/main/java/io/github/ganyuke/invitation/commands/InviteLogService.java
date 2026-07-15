package io.github.ganyuke.invitation;

import io.github.ganyuke.invitation.ports.Audience;
import io.github.ganyuke.invitation.ports.LoggerPort;
import io.github.ganyuke.invitation.ports.MessengerPort;
import io.github.ganyuke.invitation.ports.SchedulerPort;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public final class InviteLogService {

    private final Database database;
    private final SchedulerPort scheduler;
    private final MessengerPort messenger;
    private final LoggerPort logger;
    private final Set<String> inFlight = ConcurrentHashMap.newKeySet();

    public InviteLogService(Database database,
                            SchedulerPort scheduler,
                            MessengerPort messenger,
                            LoggerPort logger) {
        this.database = database;
        this.scheduler = scheduler;
        this.messenger = messenger;
        this.logger = logger;
    }

    public enum Mode {
        RECENT,
        SENT,
        RECEIVED
    }

    public record Query(Mode mode, String player, int page) {
        public static Query recent(int page) {
            return new Query(Mode.RECENT, null, page);
        }
    }

    public boolean handleInviteLog(Audience audience, String usage, String[] args) {
        Query query = parse(args);
        if (query == null) {
            messenger.sendPlain(audience, "§cUsage: " + usage);
            return true;
        }

        String senderKey = audience.senderKey();
        if (!inFlight.add(senderKey)) {
            messenger.sendPlain(audience, "§cPlease wait for the previous invite log to finish loading.");
            return true;
        }

        scheduler.runAsync(() -> {
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
                logger.severe("Failed to read invite log: " + e.getMessage());
            }

            boolean readFailed = failed;
            boolean showNext = hasNext;
            scheduler.runSync(() -> {
                try {
                    if (readFailed) {
                        messenger.sendPlain(audience, "§cFailed to read invite log.");
                        return;
                    }
                    if (lines.isEmpty()) {
                        messenger.sendPlain(audience, "§7No invite entries found.");
                        if (query.page() > 1) {
                            sendNav(audience, query, false);
                        }
                        return;
                    }
                    messenger.sendPlain(audience, header(query));
                    for (String line : lines) {
                        messenger.sendPlain(audience, line);
                    }
                    if (query.page() > 1 || showNext) {
                        sendNav(audience, query, showNext);
                    }
                } finally {
                    inFlight.remove(senderKey);
                }
            });
        });

        return true;
    }

    public List<String> tabComplete(String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return Stream.of("recent", "sent", "recieved")
                    .filter(s -> s.startsWith(prefix))
                    .toList();
        }
        if (args.length == 2) {
            Mode mode = parseMode(args[0]);
            if (mode == Mode.SENT || mode == Mode.RECEIVED) {
                return database.getKnownPlayerNames(args[1]);
            }
        }
        return List.of();
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

    private void sendNav(Audience audience, Query query, boolean hasNext) {
        boolean hasPrev = query.page() > 1;
        String prev = hasPrev ? commandFor(query, query.page() - 1) : null;
        String next = hasNext ? commandFor(query, query.page() + 1) : null;
        messenger.sendInviteLogNav(audience, hasPrev, hasNext, prev, next);
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
        return switch (query.mode()) {
            case RECENT -> database.getRecentLogs(query.page());
            case SENT -> database.getSentLogs(query.player(), query.page());
            case RECEIVED -> database.getReceivedLogs(query.player(), query.page());
        };
    }
}
