package com.coffeecat2006.mail;

import net.minecraft.util.Hand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.network.message.SignedMessage;
import com.mojang.authlib.GameProfile;
import java.util.stream.Collectors; // Added import

public class MailManager {
    private static MailState state;
    private static final int PAGE_SIZE = 5;

    public static void init(MailState st) {
        state = st;
    }

    // 開啟信箱
    public static int openInbox(ServerCommandSource src, int page) {
        String player = src.getName();

        // Original line:
        // List<String> list = state.getByRecipient().getOrDefault(player, Collections.emptyList());

        // Defensive modification:
        List<String> mailIdsFromState = state.getByRecipient().get(player);
        List<String> list = (mailIdsFromState != null) ? new ArrayList<>(mailIdsFromState) : new ArrayList<>();

        // The rest of the method remains the same:
        int total = (list.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        int p = Math.min(Math.max(page, 1), total == 0 ? 1 : total);
        int from = (p - 1) * PAGE_SIZE;
        int to = Math.min(list.size(), from + PAGE_SIZE);

        src.sendFeedback(() -> Text.literal("=== 信箱 (" + p + "/" + total + ") ==="), false);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        for (int i = from; i < to; i++) {
            MailState.Mail m = state.getMails().get(list.get(i));
            String time = LocalDateTime.ofEpochSecond(m.timestamp, 0, ZoneOffset.UTC).format(fmt);
            MutableText entry = Text.literal("[" + time + "] ")
                .formatted(Formatting.DARK_GRAY)
                .append(Text.literal(m.id + "  " + m.title))
                .formatted(Formatting.AQUA);

            // 已讀標記
            if (m.isRead) {
                entry = entry.append(Text.literal(" [已讀]")
                    .formatted(Formatting.YELLOW)
                    .styled(s -> s.withClickEvent(new ClickEvent.RunCommand("/mail read \"" + m.id + "\"")))
                    .styled(s -> s.withHoverEvent(new HoverEvent.ShowText(Text.literal("點擊查看信件內容"))))
                );
            } else {
                // 查看
                entry = entry.append(Text.literal(" [查看]")
                    .formatted(Formatting.YELLOW)
                    .styled(s -> s.withClickEvent(new ClickEvent.RunCommand("/mail read \"" + m.id + "\"")))
                    .styled(s -> s.withHoverEvent(new HoverEvent.ShowText(Text.literal("點擊查看信件內容"))))
                );
                entry = entry.append(Text.literal(" [未讀]").formatted(Formatting.RED));
            }
            // 包裹
            if (m.hasItem) {
                if (m.isPickedUp) entry = entry.append(Text.literal(" [已領取]").formatted(Formatting.GRAY));
                else entry = entry.append(Text.literal(" [領取包裹]")
                    .formatted(Formatting.GOLD)
                    .styled(s -> s.withClickEvent(new ClickEvent.RunCommand("/mail pickup \"" + m.id + "\"")))
                    .styled(s -> s.withHoverEvent(new HoverEvent.ShowText(Text.literal("點擊領取包裹"))))
                );
            }
            final MutableText sendEntry = entry;
            src.sendFeedback(() -> sendEntry, false);
        }
        src.sendFeedback(() -> Text.literal("[刪除所有信件]")
            .formatted(Formatting.RED)
            .styled(s -> s.withClickEvent(new ClickEvent.RunCommand("/mail delete all")))
            .styled(s -> s.withHoverEvent(new HoverEvent.ShowText(Text.literal("點擊刪除所有信件")))), false);
        src.sendFeedback(() -> Text.literal("[刪除所有已讀信件]")
            .formatted(Formatting.RED)
            .styled(s -> s.withClickEvent(new ClickEvent.RunCommand("/mail delete read")))
            .styled(s -> s.withHoverEvent(new HoverEvent.ShowText(Text.literal("點擊刪除所有已讀信件")))), false);
        src.sendFeedback(() -> Text.literal("[刪除所有已領取信件]")
            .formatted(Formatting.RED)
            .styled(s -> s.withClickEvent(new ClickEvent.RunCommand("/mail delete received")))
            .styled(s -> s.withHoverEvent(new HoverEvent.ShowText(Text.literal("點擊刪除所有已領取信件")))), false);
        // 分頁導航
        if (total > 1) {
            MutableText nav = Text.literal("<");
            if (p > 1) nav.styled(s -> s.withClickEvent(new ClickEvent.RunCommand("/mail open " + (p - 1))));
            nav = nav.append(Text.literal(" | Page " + p + "/" + total + " | ")); 
            MutableText next = Text.literal(">");
            if (p < total) next.styled(s -> s.withClickEvent(new ClickEvent.RunCommand("/mail open " + (p + 1))));
            nav = nav.append(next);
            final MutableText sendNav = nav;
            src.sendFeedback(() -> sendNav, false);
        }
        return 1;
    }

    // 寄送信件
    public static int send(ServerCommandSource src, String recipientArg, String title, String content, boolean withItem) {
        // Permission check for @a, @all
        if ((recipientArg.equalsIgnoreCase("@a") || recipientArg.equalsIgnoreCase("@all")) && !src.hasPermissionLevel(2)) {
            src.sendFeedback(() -> Text.literal("無權使用 @a 或 @all 功能"), false);
            return 0;
        }

        // Blacklist check for original sender (if not @a or @all, and is a player)
        ServerPlayerEntity senderPlayer = src.getPlayer();
        if (!recipientArg.equalsIgnoreCase("@a") && !recipientArg.equalsIgnoreCase("@all") && senderPlayer != null) {
            if (state.getBlacklist().contains(src.getName())) {
                src.sendFeedback(() -> Text.literal("你已被列入黑名單，無法寄送信件"), false);
                return 0;
            }
        }

        ItemStack itemToSend = ItemStack.EMPTY;
        boolean actualWithItem = withItem; // Use a mutable boolean for item status

        if (actualWithItem && senderPlayer != null) {
            ItemStack offHand = senderPlayer.getOffHandStack();
            if (offHand != null && !offHand.isEmpty()) {
                itemToSend = offHand.copy(); // Copy once for potential multiple sends

                // ALL players (including OPs) get the item removed from their off-hand after sending.
                // This is consistent with the earlier "item duplication fix" which cleared OP's offhand.
                senderPlayer.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);

            } else { // Off-hand is empty
                actualWithItem = false;
            }
        } else if (actualWithItem && senderPlayer == null) { // Non-player (e.g. command block) trying to send item
            actualWithItem = false;
            src.sendFeedback(() -> Text.literal("非玩家無法附加物品"), false);
        }

        if (recipientArg.equalsIgnoreCase("@a")) {
            List<ServerPlayerEntity> onlinePlayers = src.getServer().getPlayerManager().getPlayerList();
            int count = 0;
            String senderDisplayName = src.hasPermissionLevel(2) ? "管理員" : src.getName();
            for (ServerPlayerEntity targetPlayer : onlinePlayers) {
                // Avoid sending to self if sender is a player and part of @a
                if (senderPlayer != null && senderPlayer.getUuid().equals(targetPlayer.getUuid())) continue;
                sendSingleMail(src.getServer(), targetPlayer.getName().getString(), senderDisplayName, title, content, actualWithItem, itemToSend);
                count++;
            }
            final int finalCountA = count;
            src.sendFeedback(() -> Text.literal("已發送信件給 " + finalCountA + " 位線上玩家"), false);
            state.getLogs().add(new MailState.LogEntry(Instant.now().getEpochSecond(), src.getName(), "@a", "MASS_SEND_A", "SEND", title));
            state.markDirty();
            return count;
        } else if (recipientArg.equalsIgnoreCase("@all")) {
            Set<UUID> allPlayerUuids = state.getKnownPlayersUuids();
            int count = 0;
            String senderDisplayName = src.hasPermissionLevel(2) ? "管理員" : src.getName();
            for (UUID uuid : allPlayerUuids) {
                String targetName = state.getKnownPlayerNames().getOrDefault(uuid, uuid.toString());
                 // Avoid sending to self if sender is a player and part of @all
                if (senderPlayer != null && senderPlayer.getUuid().equals(uuid)) continue;
                sendSingleMail(src.getServer(), targetName, senderDisplayName, title, content, actualWithItem, itemToSend);
                count++;
            }
            final int finalCountAll = count;
            src.sendFeedback(() -> Text.literal("已發送信件給 " + finalCountAll + " 位已知玩家"), false);
            state.getLogs().add(new MailState.LogEntry(Instant.now().getEpochSecond(), src.getName(), "@all", "MASS_SEND_ALL", "SEND", title));
            state.markDirty();
            return count;
        } else {
            return sendSingleMail(src.getServer(), recipientArg, src.getName(), title, content, actualWithItem, itemToSend, src);
        }
    }

    private static int sendSingleMail(MinecraftServer server, String recipientName, String displayedSenderName, String title, String content, boolean withItem, ItemStack actualItemToSend) {
        return sendSingleMail(server, recipientName, displayedSenderName, title, content, withItem, actualItemToSend, null);
    }

    private static int sendSingleMail(MinecraftServer mcServer, String recipientName, String displayedSenderName, String title, String content, boolean withItem, ItemStack actualItemToSend, ServerCommandSource originalSourceForFeedback) {
        // Generate ID
        String id = "#" + randomAlphaNum(6);
        MailState.Mail m = new MailState.Mail();
        m.id = id;
        m.sender = displayedSenderName;
        m.recipient = recipientName;
        m.title = title;
        m.content = content;
        m.timestamp = Instant.now().getEpochSecond();

        m.hasItem = withItem && actualItemToSend != null && !actualItemToSend.isEmpty();
        if (m.hasItem) {
            m.packageItem = actualItemToSend.copy(); // Ensure each mail gets its own copy
        } else {
            m.packageItem = ItemStack.EMPTY;
        }
        m.isRead = false;
        m.isPickedUp = false;

        // Store
        state.getMails().put(id, m);
        state.getByRecipient()
            .computeIfAbsent(recipientName, k -> new ArrayList<>())
            .add(0, id);

        // Log entry for this specific mail
        state.getLogs().add(new MailState.LogEntry(
            m.timestamp,
            displayedSenderName, // This is who the mail appears 'from'
            recipientName,
            m.id,
            "SEND",
            m.title
        ));
        state.markDirty();

        // Online notification for recipient
        ServerPlayerEntity recv = mcServer.getPlayerManager().getPlayer(recipientName);
        if (recv != null) {
            MutableText notice = Text.literal(displayedSenderName + " 寄給你一封信: " + title)
                .formatted(Formatting.GREEN)
                .styled(s -> s.withClickEvent(new ClickEvent.RunCommand("/mail open 1")))
                .styled(s -> s.withHoverEvent(new HoverEvent.ShowText(Text.literal("點擊查看信件內容"))));
            recv.sendMessage(notice, false);
            if (m.hasItem) {
                MutableText itemNotice = Text.literal("信件中包含物品，請查看信件").formatted(Formatting.YELLOW);
                itemNotice = itemNotice.styled(s -> s.withHoverEvent(new HoverEvent.ShowText(Text.literal("點擊查看信件內容"))));
                itemNotice = itemNotice.styled(s -> s.withClickEvent(new ClickEvent.RunCommand("/mail open 1")));
                // Send item notice if there's an item
                recv.sendMessage(itemNotice, false);
            }
        }

        // 只在 originalSourceForFeedback 非空（單一玩家寄信）時，才廣播給管理員
        if (originalSourceForFeedback != null) {
            for (ServerPlayerEntity player : mcServer.getPlayerManager().getPlayerList()) {
                // 只對 OP（管理員）發送
                if (mcServer.getPlayerManager().isOperator(player.getGameProfile())) {
                    // 不重複寄給收件者，也不寄給自己
                    if ((recv == null || !player.getUuid().equals(recv.getUuid()))
                        && !player.getName().getString().equals(displayedSenderName)) {
                        player.sendMessage(
                            Text.literal(displayedSenderName + " 寄給 " + recipientName + " 一封信: " + title)
                                .formatted(Formatting.GREEN),
                            false
                        );
                    }
                }
            }
            originalSourceForFeedback.sendFeedback(
                () -> Text.literal("已寄送信件 " + id + " 給 " + recipientName),
                false
            );
        }
        return 1;
    }
    // 閱讀信件
    public static int read(ServerCommandSource src, String id) {
        MailState.Mail m = state.getMails().get(id);
        if (m == null) {
            src.sendFeedback(() -> Text.literal("找不到信件: " + id), false);
            return 0;
        }
        if (!src.hasPermissionLevel(2) && !m.recipient.equals(src.getName())) {
            src.sendFeedback(() -> Text.literal("無權查看此信件"), false);
            return 0;
        }
        m.isRead = true;
        state.markDirty();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String time = LocalDateTime.ofEpochSecond(m.timestamp, 0, ZoneOffset.UTC).format(fmt);
        src.sendFeedback(() -> Text.literal("[" + time + "]"), false);
        src.sendFeedback(() -> Text.literal(m.id + "  " + m.title), false);
        for (String line : m.content.split("&n")) {
            src.sendFeedback(() -> Text.literal(line), false);
        }
        if (m.hasItem) {
            src.sendFeedback(() -> Text.literal("包裹: " + (m.isPickedUp ? "已領取" : "未領取")), false);
            if (!m.isPickedUp) {
                src.sendFeedback(() -> Text.literal("[領取包裹]")
                    .formatted(Formatting.GOLD)
                    .styled(s -> s.withClickEvent(new ClickEvent.RunCommand("/mail pickup \"" + m.id + "\"")))
                    .styled(s -> s.withHoverEvent(new HoverEvent.ShowText(Text.literal("點擊領取包裹")))), false);
            } else {
                src.sendFeedback(() -> Text.literal("包裹內容: " + m.packageItem.getName().getString()), false);
                if (m.packageItem.getCount() > 1) {
                    src.sendFeedback(() -> Text.literal("數量: " + m.packageItem.getCount()), false);
                }
            }
        } else {
            src.sendFeedback(() -> Text.literal("此信件無包裹"), false);
        }
        // 所有按鈕
        // 刪除信件按鈕
        src.sendFeedback(() -> Text.literal(" [刪除信件]")
            .formatted(Formatting.RED)
            .styled(s -> s.withClickEvent(new ClickEvent.RunCommand("/mail delete \"" + m.id + "\"")))
            .styled(s -> s.withHoverEvent(new HoverEvent.ShowText(Text.literal("點擊刪除信件")))), false);
            
        // 將寄件者設為黑名單按鈕
        src.sendFeedback(() -> Text.literal(" [將寄件者 " + m.sender + " 加入黑名單]")
            .formatted(Formatting.RED)
            .styled(s -> s.withClickEvent(new ClickEvent.RunCommand("/mail blacklist add " + m.sender)))
            .styled(s -> s.withHoverEvent(new HoverEvent.ShowText(Text.literal("點擊加入黑名單")))), false);

        // 回信按鈕
        src.sendFeedback(() -> Text.literal(" [回信]")
            .formatted(Formatting.YELLOW)
            .styled(s -> s.withClickEvent(new ClickEvent.SuggestCommand("/mail send " + m.sender + " \"Re: " + m.title + "\" \"\" false")))
            .styled(s -> s.withHoverEvent(new HoverEvent.ShowText(Text.literal("點擊回信")))), false);

        return 1;
    }

    // 領取包裹
    public static int pickup(ServerCommandSource src, String id) {
        MailState.Mail m = state.getMails().get(id);
        if (m == null) {
            src.sendFeedback(() -> Text.literal("找不到信件: " + id), false);
            return 0;
        }
        if (!src.hasPermissionLevel(2) && !m.recipient.equals(src.getName())) {
            src.sendFeedback(() -> Text.literal("無權領取此包裹"), false);
            return 0;
        }
        if (!m.hasItem) {
            src.sendFeedback(() -> Text.literal("此信件無包裹"), false);
            return 0;
        }
        if (m.isPickedUp) {
            src.sendFeedback(() -> Text.literal("已領取過包裹"), false);
            return 0;
        }
        if (m.packageItem != null) {
            ServerPlayerEntity p = src.getPlayer();
            p.getInventory().offerOrDrop(m.packageItem.copy());
        }
        m.isPickedUp = true;
        // Log pickup
        String pickerName = src.getName();
        state.getLogs().add(new MailState.LogEntry(
            Instant.now().getEpochSecond(),
            pickerName,       // Person who performed the pickup action
            m.recipient,      // Original recipient of the mail
            m.id,             // Mail ID
            "PICKUP_SELF",    // Action
            m.packageItem != null && !m.packageItem.isEmpty() ? m.packageItem.getName().getString() : "" // Item name as details
        ));
        state.markDirty();
        src.sendFeedback(() -> Text.literal("已領取包裹: " + id), false);
        return 1;
    }

    // 刪除信件
    public static int delete(ServerCommandSource src, String target, boolean force) {
        // Handle explicit cancel command
        if (target.endsWith(" cancel_true")) {
            src.sendFeedback(() -> Text.literal("已取消刪除"), false);
            return 1;
        }

        // If force is false, it means it's the initial command, so show confirmation.
        // The original "target" (e.g., mail ID) is passed here.
        if (!force) {
            src.sendFeedback(() -> Text.literal("確定要刪除 " + target + " 嗎? ")
                .append(Text.literal("[確認]").formatted(Formatting.GREEN)
                    .styled(s -> s.withClickEvent(new ClickEvent.RunCommand("/mail delete \"" + target + "\" confirm")))) // confirm will call with force=true
                .append(Text.literal(" "))
                .append(Text.literal("[取消]").formatted(Formatting.RED)
                    // Point to the new cancel mechanism
                    .styled(s -> s.withClickEvent(new ClickEvent.RunCommand("/mail delete \"" + target + "\" cancel")))),
                false
            );
            return 1;
        }

        // If force is true, it's either a confirmation or a direct admin command (though admin usually wouldn't type 'confirm')
        // The 'target' for a confirmation click will be the original mail ID, "all", etc.
        // The 'target' for the cancel click in MailCommands is now "<original_target> cancel_true"

        // Re-evaluate target for the "confirm" flow.
        // If target was "some_id confirm" from the command, MailCommand.java now sends "some_id" and force=true.
        // So, 'target' here is the actual thing to delete.

        List<String> toRemove = new ArrayList<>();
        String playerUuid = src.getPlayer() != null ? src.getPlayer().getUuidAsString() : null; // For permission checks if needed

        if (target.equals("all")) {
            // Only remove mails belonging to the player unless they are op.
            // This part needs careful consideration of who can delete what.
            // For now, assume "all" means all of the current player's mail.
            // If admin should delete all server mail, that needs a different command or check.
            if (playerUuid != null) {
                List<String> playerMailIds = state.getByRecipient().get(src.getName());
                if (playerMailIds != null) {
                    toRemove.addAll(new ArrayList<>(playerMailIds)); // Iterate over a copy
                }
            } else if (src.hasPermissionLevel(4)) { // Server console or OP level 4 can delete all
                toRemove.addAll(new ArrayList<>(state.getMails().keySet()));
            } else {
                src.sendFeedback(() -> Text.literal("無權刪除所有郵件"), false);
                return 0;
            }
        } else if (target.equals("read")) {
            state.getMails().values().stream()
                .filter(m -> m.recipient.equals(src.getName()) && m.isRead)
                .forEach(m -> toRemove.add(m.id));
        } else if (target.equals("received")) {
            state.getMails().values().stream()
                .filter(m -> m.recipient.equals(src.getName()) && m.isPickedUp)
                .forEach(m -> toRemove.add(m.id));
        } else { // Specific mail ID
            MailState.Mail m = state.getMails().get(target);
            if (m != null && (m.recipient.equals(src.getName()) || src.hasPermissionLevel(2))) {
                toRemove.add(target);
            } else {
                src.sendFeedback(() -> Text.literal("找不到信件或無權刪除: " + target), false);
                return 0;
            }
        }

        if (toRemove.isEmpty()) {
            src.sendFeedback(() -> Text.literal("沒有符合條件的信件可刪除"), false);
            return 0;
        }

        for (String id : toRemove) {
            MailState.Mail m = state.getMails().remove(id);
            if (m != null) {
                List<String> recipientList = state.getByRecipient().get(m.recipient);
                if (recipientList != null) {
                    recipientList.remove(id);
                    if (recipientList.isEmpty()) {
                        state.getByRecipient().remove(m.recipient);
                    }
                }
            }
        }
        state.markDirty();
        src.sendFeedback(() -> Text.literal("已刪除 " + toRemove.size() + " 封信件."), false);
        return 1;
    }

    // 幫助
    public static int help(ServerCommandSource src) {
        src.sendFeedback(() -> Text.literal("=== /mail 指令總覽 ==="), false);
        src.sendFeedback(() -> Text.literal("/mail open [page] — 開啟信箱"), false);
        src.sendFeedback(() -> Text.literal("/mail send <player> <title> <content> <item> — 寄送信件"), false);
        src.sendFeedback(() -> Text.literal("/mail read <id> — 查看信件"), false);
        src.sendFeedback(() -> Text.literal("/mail pickup <id> — 領取包裹"), false);
        src.sendFeedback(() -> Text.literal("/mail delete <id|all|read|received> [confirm|cancel] — 刪除郵件"), false);
        src.sendFeedback(() -> Text.literal("/mail log [page] — 查詢寄送紀錄 (管理員)"), false);
        src.sendFeedback(() -> Text.literal("/mail blacklist [add|remove] <player> — 黑名單管理"), false);
        return 1;
    }

    public static int adminPickup(ServerCommandSource src, String mailId) {
        if (!src.hasPermissionLevel(2)) {
            src.sendFeedback(() -> Text.literal("無權執行此指令"), false);
            return 0;
        }

        MailState.Mail m = state.getMails().get(mailId);
        if (m == null) {
            src.sendFeedback(() -> Text.literal("找不到信件: " + mailId), false);
            return 0;
        }

        if (!m.hasItem) {
            src.sendFeedback(() -> Text.literal("此信件無包裹可領取"), false);
            return 0;
        }

        ServerPlayerEntity adminPlayer = src.getPlayer();
        if (adminPlayer == null && !src.getName().equals("Server")) { // Allow console if it can "receive" items, though it can't.
             src.sendFeedback(() -> Text.literal("此指令必須由玩家或控制台執行"), false);
             return 0;
        }

        if (src.getName().equals(m.recipient)) { // Admin is the recipient
            if (!m.isPickedUp) {
                return pickup(src, mailId); // Use standard pickup, logs PICKUP_SELF
            } else {
                src.sendFeedback(() -> Text.literal("你已領取過此信件的包裹"), false);
                return 0;
            }
        } else { // Admin picking for another or re-picking (if somehow isPickedUp was true but item still there)
            if (adminPlayer != null) { // Player admin
                 adminPlayer.getInventory().offerOrDrop(m.packageItem.copy());
            } else { // Console admin - cannot give item, but can mark as picked up by admin
                 src.sendFeedback(() -> Text.literal("控制台無法直接領取物品，但將記錄管理員操作"), false);
            }

            // For admin pickup, we don't set m.isPickedUp = true;
            // This allows the original recipient to still pick it up themselves if they haven't,
            // or for it to be admin-picked multiple times if needed for some reason.
            // The log indicates an admin action.

            src.sendFeedback(() -> Text.literal("已為 " + m.recipient + " 管理領取信件 " + mailId + " 的物品."), false);
            state.getLogs().add(new MailState.LogEntry(
                Instant.now().getEpochSecond(),
                src.getName(), // Admin's name
                m.recipient,   // Original recipient
                m.id,
                "PICKUP_ADMIN",
                m.packageItem != null && !m.packageItem.isEmpty() ? m.packageItem.getName().getString() : ""
            ));
            state.markDirty();
            return 1;
        }
    }

    // 日誌查詢
    public static int log(ServerCommandSource src, int page) {
        List<MailState.LogEntry> logs = state.getLogs();
        int PAGE = PAGE_SIZE;
        int total = (logs.size() + PAGE - 1) / PAGE;
        int p = Math.min(Math.max(page, 1), total == 0 ? 1 : total);
        int from = (p - 1) * PAGE; int to = Math.min(logs.size(), from + PAGE);
        src.sendFeedback(() -> Text.literal("=== Mail Logs (" + p + "/" + total + ") ==="), false);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (int i = from; i < to; i++) {
            MailState.LogEntry le = logs.get(i);
            String t = LocalDateTime.ofEpochSecond(le.timestamp,0,ZoneOffset.UTC).format(fmt);
            src.sendFeedback(() -> Text.literal("["+t+"] " + le.sender + " -> " + le.recipient + " " + le.mailId), false);
        }
        return 1;
    }

    private static int showMailLogList(ServerCommandSource src, List<MailState.LogEntry> logEntries, String baseCommandWithoutPage, int totalLogsCount, int page, int pageSize, String contextPlayerName) {
        int totalPagesCalculated = (totalLogsCount + pageSize - 1) / pageSize;
        if (totalPagesCalculated == 0) totalPagesCalculated = 1; // Ensure at least one page even if no logs
        final int currentPage = Math.min(Math.max(page, 1), totalPagesCalculated); // Clamp page number
        final int currentTotalPages = totalPagesCalculated;

        src.sendFeedback(() -> Text.literal("=== 郵件日誌 (" + currentPage + "/" + currentTotalPages + ") ==="), false);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalLogsCount);

        for (int i = fromIndex; i < toIndex; i++) {
            MailState.LogEntry le = logEntries.get(i);
            String time = LocalDateTime.ofEpochSecond(le.timestamp, 0, ZoneOffset.UTC).format(fmt);
            MutableText line = Text.literal("[" + time + "] ").formatted(Formatting.AQUA);

            MailState.Mail mail = state.getMails().get(le.mailId); // For status and buttons

            // Actor Action Recipient/Details MailID
            String actor = le.sender;
            String recipient = le.recipient;
            String details = le.details != null && !le.details.isEmpty() ? " (\"" + le.details + "\")" : "";

            switch (le.action) {
                case "SEND":
                    line.append(Text.literal(actor).formatted(Formatting.YELLOW))
                        .append(" sent to ")
                        .append(Text.literal(recipient).formatted(Formatting.AQUA))
                        .append(": ")
                        .append(Text.literal(le.mailId).formatted(Formatting.GREEN))
                        .append(Text.literal(details).formatted(Formatting.ITALIC));
                    break;
                case "PICKUP_SELF":
                    line.append(Text.literal(actor).formatted(Formatting.YELLOW)) // picker
                        .append(" picked up from ")
                        .append(Text.literal(recipient).formatted(Formatting.AQUA)) // original recipient
                        .append(": ")
                        .append(Text.literal(le.mailId).formatted(Formatting.GREEN))
                        .append(Text.literal(details).formatted(Formatting.ITALIC));
                    break;
                case "PICKUP_ADMIN":
                     line.append(Text.literal(actor).formatted(Formatting.RED)) // admin picker
                        .append(" admin-picked from ")
                        .append(Text.literal(recipient).formatted(Formatting.AQUA)) // original recipient
                        .append(": ")
                        .append(Text.literal(le.mailId).formatted(Formatting.GREEN))
                        .append(Text.literal(details).formatted(Formatting.ITALIC));
                    break;
                default: // Older logs or unknown action
                    line.append(Text.literal(actor).formatted(Formatting.YELLOW))
                        .append(" -> ")
                        .append(Text.literal(recipient).formatted(Formatting.AQUA))
                        .append(": ")
                        .append(Text.literal(le.mailId).formatted(Formatting.GREEN))
                        .append(Text.literal(details).formatted(Formatting.ITALIC));
            }

            // Interaction Buttons
            if (mail != null) {
                final String mailIdForLambda = le.mailId; // Capture for lambda
                line.append(Text.literal(" [查看]")
                    .formatted(Formatting.BLUE, Formatting.UNDERLINE)
                    .styled(s -> s.withClickEvent(new ClickEvent.RunCommand("/mail read \"" + mailIdForLambda + "\""))
                                  .withHoverEvent(new HoverEvent.ShowText(Text.literal("讀取信件內容")))));

                if (le.action.equals("SEND") && mail.hasItem && !mail.isPickedUp && src.hasPermissionLevel(2)) {
                     line.append(Text.literal(" [查看包裹]")
                        .formatted(Formatting.GOLD, Formatting.UNDERLINE)
                        .styled(s -> s.withClickEvent(new ClickEvent.RunCommand("/mail adminpickup \"" + mailIdForLambda + "\""))
                                      .withHoverEvent(new HoverEvent.ShowText(Text.literal("管理員領取此信件物品(不影響收件者領取)")))));
                }
                final String mailStatus = (mail.isRead ? "已讀" : "未讀") + ", " + (mail.isPickedUp ? "已領取" : "未領取");
                line.append(Text.literal(" [狀態]")
                    .formatted(Formatting.DARK_GRAY)
                    .styled(s -> s.withHoverEvent(new HoverEvent.ShowText(Text.literal(mailStatus)))));

            } else {
                 line.append(Text.literal(" [狀態]")
                    .formatted(Formatting.DARK_GRAY)
                    .styled(s -> s.withHoverEvent(new HoverEvent.ShowText(Text.literal("信件已刪除")))));
            }
            src.sendFeedback(() -> line, false);
        }

        // Pagination controls
        if (currentTotalPages > 1) { // Corrected variable name
            MutableText nav = Text.empty();
            final String finalBaseCommand = baseCommandWithoutPage;
            final int finalPage = currentPage; // Use the clamped and final currentPage

            if (finalPage > 1) {
                nav.append(Text.literal("« 上一頁")
                    .formatted(Formatting.YELLOW)
                    .styled(s -> s.withClickEvent(new ClickEvent.RunCommand(finalBaseCommand + " " + (finalPage - 1)))
                                  .withHoverEvent(new HoverEvent.ShowText(Text.literal("前往上一頁")))));
            } else {
                nav.append(Text.literal("« 上一頁").formatted(Formatting.DARK_GRAY));
            }
            nav.append(Text.literal(" | ")); // Spacer
            if (finalPage < currentTotalPages) {
                nav.append(Text.literal("下一頁 »")
                    .formatted(Formatting.YELLOW)
                    .styled(s -> s.withClickEvent(new ClickEvent.RunCommand(finalBaseCommand + " " + (finalPage + 1)))
                                  .withHoverEvent(new HoverEvent.ShowText(Text.literal("前往下一頁")))));
            } else {
                nav.append(Text.literal("下一頁 »").formatted(Formatting.DARK_GRAY));
            }
            src.sendFeedback(() -> nav, false);
        }
        return 1;
    }

    public static int mailLog(ServerCommandSource src, String targetPlayerName, int recent, int page) {
        // 'recent' is not used in this implementation for now.
        List<MailState.LogEntry> allLogs = new ArrayList<>(state.getLogs());
        Collections.reverse(allLogs); // Newest first

        List<MailState.LogEntry> filteredLogs;
        String baseCommand;
        String contextPlayerName = null; // For formatting help in showMailLogList, not for filtering here

        if (targetPlayerName == null || targetPlayerName.equalsIgnoreCase("all")) {
            filteredLogs = allLogs;
            baseCommand = "/mail log all";
        } else {
            contextPlayerName = targetPlayerName; // Pass for potential formatting hints
            String finalTargetPlayerName = targetPlayerName; // For lambda
            filteredLogs = allLogs.stream()
                .filter(le -> finalTargetPlayerName.equalsIgnoreCase(le.sender) || finalTargetPlayerName.equalsIgnoreCase(le.recipient))
                .collect(Collectors.toList());
            baseCommand = "/mail log player " + targetPlayerName;
        }

        return showMailLogList(src, filteredLogs, baseCommand, filteredLogs.size(), page, 10, contextPlayerName); // pageSize = 10
    }

    // 黑名單
    public static int showBlacklist(ServerCommandSource src) {
        src.sendFeedback(() -> Text.literal("=== 黑名單 ==="), false);
        for (String p : state.getBlacklist()) {
            src.sendFeedback(() -> Text.literal(p + " [移除]")
                .styled(s -> s.withClickEvent(new ClickEvent.RunCommand("/mail blacklist remove " + p))), false);
        }
        return 1;
    }

    public static int blacklistAdd(ServerCommandSource src, String player, boolean confirm) {
        if (!confirm) {
            src.sendFeedback(() -> Text.literal("確定要將 " + player + " 加入黑名單嗎? ")
                .append(Text.literal("[確認]").formatted(Formatting.GREEN)
                    .styled(s -> s.withClickEvent(new ClickEvent.RunCommand("/mail blacklist add " + player + " confirm")))),
                false);
            return 1;
        }
        state.getBlacklist().add(player);
        state.markDirty();
        src.sendFeedback(() -> Text.literal(player + " 已加入黑名單"), false);
        return 1;
    }

    public static int blacklistRemove(ServerCommandSource src, String player) {
        state.getBlacklist().remove(player);
        state.markDirty();
        src.sendFeedback(() -> Text.literal(player + " 已從黑名單移除"), false);
        return 1;
    }

    // 隨機 ID 工具方法
    private static String randomAlphaNum(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random rand = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<length; i++) sb.append(chars.charAt(rand.nextInt(chars.length())));
        return sb.toString();
    }
}
