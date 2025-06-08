package com.coffeecat2006.redeem;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Formatting;
import net.minecraft.server.MinecraftServer;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class RedeemManager {
    private static RedeemState state;
    private static Map<String, Redeem> codes;

    public static class Redeem {
        public String code;
        public String message;
        public int limit;
        public long expiryEpoch;
        public boolean singleUse;
        public boolean available = true;
        public List<ItemStack> items;
        public int redeemedCount;
        public Set<UUID> usedPlayers = new HashSet<>();
        public Map<String, String> events = new HashMap<>();

        public Instant getExpiry() {
            return Instant.ofEpochSecond(expiryEpoch);
        }
    }

    private static void writeLog(
            ServerCommandSource src,
            String actor,
            String source,
            String action,
            String target
    ) {
        RedeemState.LogEntry le = new RedeemState.LogEntry();
        le.timestamp = Instant.now().getEpochSecond();
        le.actor = actor;
        le.source = (source == null ? "" : source);
        le.action = action;
        le.target = target;
        state.getLogs().add(le);
        state.markDirty();
    }

    public static void init(RedeemState st) {
        state = st;
        codes = state.getCodes();
    }

    public static int redeem(ServerCommandSource src, String code) {
        ServerPlayerEntity player;
        try {
            player = src.getPlayer();
        } catch (Exception e) {
            src.sendFeedback(() -> Text.literal("僅玩家可使用此指令"), false);
            return 0;
        }

        Redeem r = codes.get(code);
        if (r == null || !r.available) {
            src.sendFeedback(() -> Text.literal("無此禮包碼: " + code), false);
            return 0;
        }

        if (r.expiryEpoch != Long.MAX_VALUE && Instant.now().isAfter(r.getExpiry())) {
            src.sendFeedback(() -> Text.literal("此禮包碼已過期"), false);
            writeLog(src, player.getName().getString(), null, "試圖領取禮包碼但過期了", code);
            return 0;
        }

        if (r.limit >= 0 && r.redeemedCount >= r.limit) {
            src.sendFeedback(() -> Text.literal("此禮包碼已達使用上限"), false);
            writeLog(src, player.getName().getString(), null, "試圖領取禮包碼但已達使用上限", code);
            return 0;
        }

        if (r.singleUse && r.usedPlayers.contains(player.getUuid())) {
            src.sendFeedback(() -> Text.literal("你已經領取過此禮包碼"), false);
            writeLog(src, player.getName().getString(), null, "試圖重複領取禮包碼", code);
            return 0;
        }

        for (ItemStack item : r.items) {
            ItemStack give = item.copy();
            give.setCount(item.getCount());
            player.getInventory().offerOrDrop(give);
        }

        MinecraftServer server = src.getServer();
        CommandSourceStack serverSource = server.createCommandSourceStack().withPermission(4);

        r.events.forEach((ename, cmd) -> {
            String playerName = player.getName().getString();
            String fullCmd = String.format(
                "execute as %s at %s run %s",
                playerName,
                playerName,
                cmd.replace("@s", playerName)
            );
            // 由伺服端（最高權限）執行
            server.getCommandManager()
                .executeWithPrefix(serverSource, fullCmd);
        });

        r.redeemedCount++;
        r.usedPlayers.add(player.getUuid());
        state.markDirty();

        src.sendFeedback(() -> Text.literal(r.message), false);
        writeLog(src, player.getName().getString(), null, "領取了禮包碼", code);
        return 1;
    }

    public static int list(ServerCommandSource src) {
        src.sendFeedback(() -> Text.literal("=== Redeem Codes ==="), false);

        for (Redeem r : codes.values()) {
            String remain;
            if (r.expiryEpoch == Long.MAX_VALUE) {
                remain = "永不過期";
            } else {
                Instant expiry = r.getExpiry();
                if (Instant.now().isAfter(expiry)) {
                    remain = "已過期";
                } else {
                    long mins = Duration.between(Instant.now(), expiry).toMinutes();
                    remain = mins + " 分鐘";
                }
            }

            MutableText line = Text.literal(r.code)
                .formatted(Formatting.AQUA, Formatting.UNDERLINE)
                .styled(style -> style
                    .withHoverEvent(new HoverEvent.ShowText(Text.literal("點擊複製")))
                    .withClickEvent(new ClickEvent.CopyToClipboard(r.code))
                );

            MutableText info = Text.literal(String.format(
                " [%d/%s] 剩餘: %s 訊息: %s",
                r.redeemedCount,
                r.limit < 0 ? "∞" : r.limit,
                remain,
                r.message
            ));

            MutableText entry = line.append(info);
            if (!r.events.isEmpty()) {
                entry.append(
                    Text.literal(" [事件]")
                        .formatted(Formatting.LIGHT_PURPLE, Formatting.UNDERLINE)
                        .styled(style -> style
                            .withHoverEvent(new HoverEvent.ShowText(Text.literal("點擊查看事件")))
                            .withClickEvent(new ClickEvent.RunCommand("/redeem preview event " + r.code + " all"))
                        )
                );
            }

            src.sendFeedback(() -> entry, false);

            if (!r.items.isEmpty()) {
                for (ItemStack item : r.items) {
                    String label = item.getCount() + "x" + item.getItem().toString();
                    MutableText it = Text.literal("[" + label + "]")
                        .formatted(Formatting.GOLD, Formatting.UNDERLINE)
                        .styled(style -> style
                            .withHoverEvent(new HoverEvent.ShowText(Text.literal("點擊獲取此物品")))
                            .withClickEvent(new ClickEvent.RunCommand("/redeem preview item " + r.code))
                        );
                    src.sendFeedback(() -> it, false);
                }
            }
        }
        return 1;
    }

    public static int add(
            ServerCommandSource src,
            String code,
            String text,
            String limitStr,
            String timeStr,
            boolean singleUse
    ) {
        if (codes.containsKey(code)) {
            src.sendFeedback(() -> Text.literal("此禮包碼已存在"), false);
            return 0;
        }

        Redeem r = new Redeem();
        r.code = code.equalsIgnoreCase("random")
            ? UUID.randomUUID().toString().replace("-", "").substring(0, new Random().nextInt(5) + 12)
            : code;
        r.message = text;
        if (limitStr.equalsIgnoreCase("infinity")) {
            r.limit = -1;
        } else {
            try {
                r.limit = Integer.parseInt(limitStr);
            } catch (NumberFormatException e) {
                src.sendFeedback(() -> Text.literal("limit 必須是數字或 infinity"), false);
                return 0;
            }
        }
        if (timeStr.equalsIgnoreCase("infinity")) {
            r.expiryEpoch = Long.MAX_VALUE;
        } else {
            try {
                long minutes = Long.parseLong(timeStr);
                r.expiryEpoch = Instant.now().plus(Duration.ofMinutes(minutes)).getEpochSecond();
            } catch (NumberFormatException e) {
                src.sendFeedback(() -> Text.literal("time 必須是數字或 infinity"), false);
                return 0;
            }
        }
        r.singleUse = singleUse;
        r.items = new ArrayList<>(); // Initialize with empty list

        String itemsLogDetail;
        ServerPlayerEntity player = src.getPlayer();
        String actorName = player != null ? player.getName().getString() : "System";
        String sourceName = src.getName();

        if (player != null) { // Player context
            ItemStack off = player.getOffHandStack();
            if (off != null && off.getItem() != Items.AIR) {
                r.items.add(off.copy());
                itemsLogDetail = "(含副手物品)";
            } else {
                src.sendFeedback(() -> Text.literal("Warning: Off-hand is empty. Redeem code '" + r.code + "' created with no items."), false);
                itemsLogDetail = "(副手為空)";
            }
        } else { // Non-player context
            src.sendFeedback(() -> Text.literal("Redeem code '" + r.code + "' created without items as command was not run by a player."), false);
            itemsLogDetail = "(非玩家執行,無物品)";
        }
        writeLog(src, actorName, sourceName, "新增了禮包碼 " + r.code + " " + itemsLogDetail, r.code);

        codes.put(r.code, r);
        state.markDirty();

        src.sendFeedback(() ->
            Text.literal("已建立: ")
                .append(
                    Text.literal(r.code)
                        .formatted(Formatting.AQUA, Formatting.UNDERLINE)
                        .styled(style -> style
                            .withHoverEvent(new HoverEvent.ShowText(Text.literal("點擊複製")))
                            .withClickEvent(new ClickEvent.CopyToClipboard(r.code))
                        )
                ),
            false
        );
        return 1;
    }

    public static int remove(ServerCommandSource src, String code) {
        if (codes.remove(code) != null) {
            state.markDirty();
            src.sendFeedback(() -> Text.literal("已移除禮包碼: " + code), false);
            writeLog(src, "管理員", src.getName(), "移除了禮包碼 ", code);
        } else {
            src.sendFeedback(() -> Text.literal("找不到禮包碼: " + code), false);
        }
        return 1;
    }

    public static int previewItem(ServerCommandSource src, String code) {
        ServerPlayerEntity player;
        try {
            player = src.getPlayer();
        } catch (Exception e) {
            src.sendFeedback(() -> Text.literal("僅玩家可使用此指令"), false);
            return 0;
        }

        Redeem r = codes.get(code);
        if (r == null) {
            src.sendFeedback(() -> Text.literal("無此禮包碼: " + code), false);
            return 0;
        }

        for (ItemStack item : r.items) {
            ItemStack give = item.copy();
            give.setCount(item.getCount());
            player.getInventory().offerOrDrop(give);
        }

        return 1;
    }

    public static int previewText(ServerCommandSource src, String code) {
        Redeem r = codes.get(code);
        if (r == null) {
            src.sendFeedback(() -> Text.literal("無此禮包碼: " + code), false);
            return 0;
        }

        src.sendFeedback(() -> Text.literal(r.message), false);
        return 1;
    }

    public static int previewEvent(ServerCommandSource src, String code, String eventName) {
        Redeem r = codes.get(code);
        if (r == null) {
            return feedback(src, "無此禮包碼: " + code);
        }
        String cmd = r.events.get(eventName);
        if (cmd == null) {
            return feedback(src, "找不到事件: " + eventName);
        }

        MutableText line = Text.literal(eventName + " → " + cmd)
            .formatted(Formatting.GREEN, Formatting.UNDERLINE)
            .styled(style -> style
                .withHoverEvent(new HoverEvent.ShowText(Text.literal("點擊複製指令")))
                .withClickEvent(new ClickEvent.CopyToClipboard(cmd))
            );
        src.sendFeedback(() -> line, false);
        return 1;
    }

    public static int previewAllEvents(ServerCommandSource src, String code) {
        Redeem r = codes.get(code);
        if (r == null) {
            return feedback(src, "無此禮包碼: " + code);
        }
        if (r.events.isEmpty()) {
            return feedback(src, "此禮包碼尚無任何事件");
        }

        src.sendFeedback(() -> Text.literal("=== 事件列表 ==="), false);
        r.events.forEach((name, cmd) -> {
            MutableText line = Text.literal(name + ": " + cmd)
                .formatted(Formatting.GOLD, Formatting.UNDERLINE)
                .styled(style -> style
                    .withHoverEvent(new HoverEvent.ShowText(Text.literal("點擊複製指令")))
                    .withClickEvent(new ClickEvent.CopyToClipboard(cmd))
                );
            src.sendFeedback(() -> line, false);
        });

        return 1;
    }

    public static int modifyItemReset(ServerCommandSource src, String code) {
        Redeem r = codes.get(code);
        if (r == null) return feedback(src, "無此禮包碼: " + code);
        r.items.clear();
        state.markDirty();
        writeLog(src, "管理員", src.getName(), "重製禮包碼 " + code + " 物品", code);
        return feedback(src, "已重置物品");
    }

    public static int modifyItemTransform(ServerCommandSource src, String code) {
        Redeem r = codes.get(code);
        if (r == null) return feedback(src, "無此禮包碼: " + code);

        ServerPlayerEntity player = src.getPlayer();
        if (player == null) {
            return feedback(src, "無法轉換物品: 指令必須由玩家執行.");
        }

        ItemStack off = player.getOffHandStack();
        r.items.clear();
        String logMessageAction;
        String feedbackMessage;

        if (off != null && off.getItem() != Items.AIR) {
            r.items.add(off.copy());
            logMessageAction = "物品轉換為副手物品";
            feedbackMessage = "已將禮包碼 '" + code + "' 的物品轉換為副手物品";
        } else {
            logMessageAction = "物品已被清空 (副手為空)";
            feedbackMessage = "副手物品為空. 禮包碼 '" + code + "' 的物品已被清空.";
        }

        state.markDirty();
        writeLog(src, player.getName().getString(), src.getName(), "禮包碼 " + code + ": " + logMessageAction, code);
        return feedback(src, feedbackMessage);
    }

    public static int modifyItemAdd(ServerCommandSource src, String code) {
        Redeem r = codes.get(code);
        if (r == null) return feedback(src, "無此禮包碼: " + code);

        ServerPlayerEntity player = src.getPlayer();
        if (player == null) {
            return feedback(src, "無法新增物品: 指令必須由玩家執行.");
        }

        ItemStack off = player.getOffHandStack();
        if (off != null && off.getItem() != Items.AIR) {
            r.items.add(off.copy());
            state.markDirty();
            writeLog(src, player.getName().getString(), src.getName(), "禮包碼 " + code + ": 新增副手物品", code);
            return feedback(src, "已新增副手物品至禮包碼 '" + code + "'.");
        } else {
            return feedback(src, "副手物品為空. 未新增物品至禮包碼 '" + code + "'.");
        }
    }

    public static int modifyCode(ServerCommandSource src, String code, String newCode) {
        if (!codes.containsKey(code)) return feedback(src, "無此禮包碼: " + code);
        if (codes.containsKey(newCode)) return feedback(src, "新 code 已存在");
        Redeem r = codes.remove(code);
        r.code = newCode;
        codes.put(newCode, r);
        state.markDirty();
        writeLog(src, "管理員", src.getName(), "禮包碼由 " + code + " 變更為 " + newCode, newCode);
        writeLog(src, "管理員", src.getName(), "將禮包碼 " + code + " 變更為 " + newCode, code);
        return feedback(src, "已更新 code 為: " + newCode);
    }

    public static int modifyText(ServerCommandSource src, String code, String text) {
        Redeem r = codes.get(code);
        if (r == null) return feedback(src, "無此禮包碼: " + code);
        r.message = text;
        state.markDirty();
        writeLog(src, "管理員", src.getName(), "更新禮包碼 " + code + " 訊息文字", code);
        return feedback(src, "已更新訊息文字");
    }

    public static int modifyLimit(ServerCommandSource src, String code, String limitStr) {
        Redeem r = codes.get(code);
        if (r == null) return feedback(src, "無此禮包碼: " + code);
        try {
            r.limit = limitStr.equalsIgnoreCase("infinity") ? -1 : Integer.parseInt(limitStr);
        } catch (NumberFormatException e) {
            return feedback(src, "limit 必須是數字或 infinity");
        }
        state.markDirty();
        writeLog(src, "管理員", src.getName(), "更新禮包碼 " + code + " 使用上限", code);
        return feedback(src, "已更新使用上限");
    }

    public static int modifyTime(ServerCommandSource src, String code, String timeStr) {
        Redeem r = codes.get(code);
        if (r == null) return feedback(src, "無此禮包碼: " + code);
        try {
            r.expiryEpoch = timeStr.equalsIgnoreCase("infinity")
                ? Long.MAX_VALUE
                : Instant.now().plus(Duration.ofMinutes(Long.parseLong(timeStr))).getEpochSecond();
        } catch (NumberFormatException e) {
            return feedback(src, "time 必須是數字或 infinity");
        }
        state.markDirty();
        writeLog(src, "管理員", src.getName(), "更新禮包碼 " + code + " 過期時間", code);
        return feedback(src, "已更新過期時間");
    }

    public static int modifyRules(ServerCommandSource src, String code, boolean singleUse) {
        Redeem r = codes.get(code);
        if (r == null) return feedback(src, "無此禮包碼: " + code);
        r.singleUse = singleUse;
        state.markDirty();
        writeLog(src, "管理員", src.getName(), "更新禮包碼 " + code + " 單次領取規則", code);
        return feedback(src, "已更新單次領取規則");
    }

    public static int modifyReceiveStatus(ServerCommandSource src, String code, String playerId, boolean status) {
        Redeem r = codes.get(code);
        if (r == null) return feedback(src, "無此禮包碼: " + code);
    
        UUID uuid;
        try {
            uuid = UUID.fromString(playerId);
        } catch (IllegalArgumentException e) {
            ServerPlayerEntity pl = src.getServer().getPlayerManager().getPlayer(playerId);
            if (pl == null) return feedback(src, "找不到玩家: " + playerId);
            uuid = pl.getUuid();
        }
        if (status) r.usedPlayers.add(uuid);
        else         r.usedPlayers.remove(uuid);
        state.markDirty();
        writeLog(src, "管理員", src.getName(), "更新玩家 " + playerId + " 領取狀態", code);
        return feedback(src, "已更新玩家領取狀態");
    }
    

    public static int modifyAvailable(ServerCommandSource src, String code, boolean available) {
        Redeem r = codes.get(code);
        if (r == null) return feedback(src, "無此禮包碼: " + code);
        r.available = available;
        state.markDirty();
        writeLog(src, "管理員", src.getName(), available ? "設為可用禮包碼: " : "隱藏禮包碼: ", code);
        return feedback(src, available ? "已設為可用" : "已隱藏該禮包碼");
    }

    public static int modifyEventAdd(ServerCommandSource src, String code, String name, String cmd) {
        Redeem r = codes.get(code);
        if (r == null) return feedback(src, "無此禮包碼: " + code);
        r.events.put(name, cmd);
        state.markDirty();
        writeLog(src, "管理員", src.getName(), "新增事件 " + name + " 禮包碼: ", code);
        return feedback(src, "已新增事件: " + name);
    }

    public static int modifyEventRemove(ServerCommandSource src, String code, String name) {
        Redeem r = codes.get(code);
        if (r == null) return feedback(src, "無此禮包碼: " + code);
        r.events.remove(name);
        state.markDirty();
        writeLog(src, "管理員", src.getName(), "移除事件 " + name + " 禮包碼: ", code);
        return feedback(src, "已移除事件: " + name);
    }

    public static int modifyEventReset(ServerCommandSource src, String code) {
        Redeem r = codes.get(code);
        if (r == null) return feedback(src, "無此禮包碼: " + code);
        r.events.clear();
        state.markDirty();
        writeLog(src, "管理員", src.getName(), "清空禮包碼的所有事件 禮包碼: ", code);
        return feedback(src, "已清空所有事件");
    }

    private static List<RedeemState.LogEntry> queryLogs(
            List<RedeemState.LogEntry> all,
            Predicate<RedeemState.LogEntry> filter,
            int recent,
            int page
    ) {
        List<RedeemState.LogEntry> list = all.stream()
            .filter(filter)
            .collect(Collectors.toList());
        if (recent > 0 && list.size() > recent) {
            list = list.subList(list.size() - recent, list.size());
        }
        int perPage = 10;
        int from = Math.min(list.size(), (page - 1) * perPage);
        int to = Math.min(list.size(), from + perPage);
        return list.subList(from, to);
    }

    private static int showLogList(
            ServerCommandSource src,
            List<RedeemState.LogEntry> entries,
            String baseCommand,
            int recent,
            int page
    ) {
        int perPage = 10;
        src.sendFeedback(() -> Text.literal("=== Redeem Logs (Page " + page + ") ==="), false);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (RedeemState.LogEntry le : entries) {
            String time = LocalDateTime
                .ofEpochSecond(le.timestamp, 0, ZoneOffset.UTC)
                .format(fmt);
        
            MutableText timeText = Text.literal("[" + time + "] ")
                .formatted(Formatting.AQUA);
        
            MutableText line = timeText;
            if (le.source != null && !le.source.isEmpty()) {
                line = line.append(Text.literal(le.actor + " " + le.source + " "));
            } else {
                line = line.append(Text.literal(le.actor + " "));
            }
            String tgt = le.target != null ? " " + le.target : "";
            line = line.append(Text.literal(le.action + tgt));
        
            final MutableText sendLine = line;
            src.sendFeedback(() -> sendLine, false);
        }        
        

        String recentPart = recent > 0 
            ? String.valueOf(recent) 
            : "9999";

        String prevCmd = page > 1
            ? baseCommand + " " + recentPart + " " + (page - 1)
            : baseCommand + " " + recentPart + " " + page; // Current page, typically not clickable but ensure format consistency

        MutableText prev = Text.literal("« 上一頁");
        if (page <= 1) {
            prev.formatted(Formatting.DARK_GRAY);
        } else {
            prev.formatted(Formatting.GRAY, Formatting.UNDERLINE)
                .styled(style -> style
                    .withClickEvent(new ClickEvent.RunCommand(prevCmd))
                );
        }

        String nextCmd = baseCommand + " " + recentPart + " " + (page + 1);

        MutableText next = Text.literal("下一頁 »");
        if (entries.size() < perPage) {
            next.formatted(Formatting.DARK_GRAY);
        } else {
            next.formatted(Formatting.GRAY, Formatting.UNDERLINE)
                .styled(style -> style
                    .withClickEvent(new ClickEvent.RunCommand(nextCmd))
                );
        }

        MutableText nav = Text.literal("")
            .append(prev)
            .append(Text.literal(" | ").formatted(Formatting.WHITE))
            .append(next);

        src.sendFeedback(() -> nav, false);
        return 1;
    }

    public static int logAll(ServerCommandSource src, int recent, int page) {
        return showLogList(
            src,
            queryLogs(state.getLogs(), e -> true, recent, page),
            "/redeem log all",
            recent,
            page
        );
    }

    public static int logEdits(ServerCommandSource src, String code, int recent, int page) {
        return showLogList(
            src,
            queryLogs(
                state.getLogs(),
                e -> e.target.equals(code) && !e.action.contains("領取"),
                recent,
                page
            ),
            "/redeem log code " + code + " edits",
            recent,
            page
        );
    }

    public static int logRedeems(ServerCommandSource src, String code, int recent, int page) {
        return showLogList(
            src,
            queryLogs(
                state.getLogs(),
                e -> e.target.equals(code) && e.action.contains("領取"),
                recent,
                page
            ),
            "/redeem log code " + code + " redeems",
            recent,
            page
        );
    }

    public static int logPlayer(ServerCommandSource src, String player, int recent, int page) {
        return showLogList(
            src,
            queryLogs(
                state.getLogs(),
                e -> e.actor.equals(player),
                recent,
                page
            ),
            "/redeem log player " + player,
            recent,
            page
        );
    }

    public static int helpAll(ServerCommandSource src) {
        src.sendFeedback(() -> Text.literal("=== /redeem 指令總覽 ==="), false);

        addHelpLine(src, "add",      "新增一組 Code",          "/redeem add <code> \"<msg>\" <limit> <time> <rules>");
        addHelpLine(src, "remove",   "刪除 Code",              "/redeem remove <code>");
        addHelpLine(src, "list",     "列出所有 Code",          "/redeem list");
        addHelpLine(src, "preview",  "預覽物品/文字/事件",      "/redeem preview <item|text|event> ...");
        addHelpLine(src, "modify",   "修改已存在的 Code",      "/redeem modify <code> <field> ...");
        addHelpLine(src, "log",      "查看操作日誌",            "/redeem log <all|code|player> ...");
        addHelpLine(src, "help",     "顯示指令說明",            "/redeem help [<子指令>]");
        return 1;
    }

    public static int helpCommand(ServerCommandSource src, String cmd) {
        switch (cmd) {
            case "add":
                src.sendFeedback(() -> Text.literal(
                    "新增一組 Code：\n" +
                    "  /redeem add <code> \"<message>\" <limit> <time> <singleUse>\n" +
                    "  - limit: 數字 or infinity\n" +
                    "  - time : 分鐘 or infinity\n" +
                    "  - singleUse: true(單次)／false(不限次)"), false);
                break;
            case "remove":
                src.sendFeedback(() -> Text.literal(
                    "刪除 Code：\n" +
                    "  /redeem remove <code>"), false);
                break;
            case "list":
                src.sendFeedback(() -> Text.literal(
                    "列出所有 Code：\n" +
                    "  /redeem list"), false);
                break;
            case "preview":
                src.sendFeedback(() -> Text.literal(
                    "預覽子指令：\n" +
                    "  /redeem preview item <code>\n" +
                    "  /redeem preview text <code>\n" +
                    "  /redeem preview event <code> all\n" +
                    "  /redeem preview event <code> <eventName>"), false);
                break;
            case "modify":
                src.sendFeedback(() -> Text.literal(
                    "修改 Code：\n" +
                    "  /redeem modify <code> item <reset|transform|add>\n" +
                    "  /redeem modify <code> code <newCode>\n" +
                    "  /redeem modify <code> text \"<msg>\"\n" +
                    "  /redeem modify <code> limit <num|infinity>\n" +
                    "  /redeem modify <code> time <min|infinity>\n" +
                    "  /redeem modify <code> rules <true|false>\n" +
                    "  /redeem modify <code> receive_status <uuid> <true|false>\n" +
                    "  /redeem modify <code> available <true|false>\n" +
                    "  /redeem modify <code> event <add|remove|reset> ..."), false);
                break;
            case "log":
                src.sendFeedback(() -> Text.literal(
                    "操作日誌：\n" +
                    "  /redeem log all [recent <n>] [page <p>]\n" +
                    "  /redeem log code <code> edits|redeems [recent <n>] [page <p>]\n" +
                    "  /redeem log player <player> [recent <n>] [page <p>]"), false);
                break;
            case "help":
                src.sendFeedback(() -> Text.literal(
                    "幫助：\n" +
                    "  /redeem help [<子指令>]\n" +
                    "  - 若不指定子指令，則顯示所有指令的總覽\n"
                    ), false);
                break;
            default:
                src.sendFeedback(() -> Text.literal("不支援的子指令： " + cmd), false);
        }
        return 1;
    }

    private static void addHelpLine(
        ServerCommandSource src,
        String name,
        String desc,
        String usage
    ) {
        MutableText line = Text.literal(String.format(
            "/redeem %-8s — %s", name, desc
        ));
        line.append(Text.literal("[詳細]")
            .formatted(Formatting.YELLOW, Formatting.UNDERLINE)
            .styled(style -> style
                .withHoverEvent(new HoverEvent.ShowText(Text.literal("查看 /redeem help " + name)))
                .withClickEvent(new ClickEvent.RunCommand("/redeem help " + name))
            )
        );
        src.sendFeedback(() -> line, false);
    }

    private static int feedback(ServerCommandSource src, String msg) {
        src.sendFeedback(() -> Text.literal(msg), false);
        return 1;
    }
}
