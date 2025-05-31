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

public class MailManager {
    private static MailState state;
    private static final int PAGE_SIZE = 5;

    public static void init(MailState st) {
        state = st;
    }

    // 開啟信箱
    public static int openInbox(ServerCommandSource src, int page) {
        String player = src.getName();
        List<String> list = state.getByRecipient().getOrDefault(player, Collections.emptyList());
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
    public static int send(ServerCommandSource src,
                           String recipient,
                           String title,
                           String content,
                           boolean withItem) {
        // 黑名單檢查：若寄件者被列入黑名單，靜默阻擋
            String sender = src.getName();
            if (state.getBlacklist().contains(sender)) {
                return 0;
            }

        // 生成 ID
        String id = "#" + randomAlphaNum(6);
        MailState.Mail m = new MailState.Mail();
        m.id = id;
        m.sender = src.getName();
        m.recipient = recipient;
        m.title = title;
        m.content = content;
        m.timestamp = Instant.now().getEpochSecond();
        m.hasItem = withItem;
        m.isRead = false;
        m.isPickedUp = false;
        // 副手物品打包
        if (withItem) {
            try {
                ServerPlayerEntity p = src.getPlayer();
                ItemStack off = p.getOffHandStack();
                if (!src.hasPermissionLevel(2)) {
                    if (off != null && off.getItem() != Items.AIR) {
                        m.packageItem = off.copy();
                        p.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
                    }
                } else { // src.hasPermissionLevel(2) or more
                    if (off != null && off.getItem() != Items.AIR) {
                        m.packageItem = off.copy();
                        p.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY); // << ADD THIS LINE
                    }
                }
            } catch (Exception e) {
                // 非玩家寄送跳過
            }
        }
        // 儲存
        state.getMails().put(id, m);
        state.getByRecipient()
            .computeIfAbsent(recipient, k -> new ArrayList<>())
            .add(0, id);
        state.markDirty();
        // Online notification for recipient
        MinecraftServer server = src.getServer();
        ServerPlayerEntity recv = server.getPlayerManager().getPlayer(recipient);
        UUID recipientUuid = null; // Store UUID for later comparison
        MutableText notice = Text.literal(src.getName() + " 寄給你一封信: " + title)
            .formatted(Formatting.GREEN)
            .styled(s -> s.withClickEvent(new ClickEvent.RunCommand("/mail open 1")))
            .styled(s -> s.withHoverEvent(new HoverEvent.ShowText(Text.literal("點擊查看信件內容"))));

        if (recv != null) {
            recipientUuid = recv.getUuid(); // Get UUID if recipient is online
            recv.sendMessage(notice, false);
            if (withItem) {
                MutableText itemNotice = Text.literal("信件中包含物品，請查看信件").formatted(Formatting.YELLOW);
                itemNotice = itemNotice.styled(s -> s.withHoverEvent(new HoverEvent.ShowText(Text.literal("點擊查看信件內容"))));
                recv.sendMessage(itemNotice, false);
            }
        }

        // Broadcast to other online players
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            // Only send if player is not the recipient (check by UUID)
            if (recipientUuid == null || !player.getUuid().equals(recipientUuid)) {
                player.sendMessage(Text.literal(src.getName() + " 寄給 " + recipient + " 一封信: " + title)
                    .formatted(Formatting.GREEN), false);
            }
        }

        // Add log entry
        MailState.LogEntry logEntry = new MailState.LogEntry(
            m.timestamp,
            m.sender,
            m.recipient,
            m.id
        );
        state.getLogs().add(logEntry); // Add to the logs list

        // 回傳給寄件者的反饋
        src.sendFeedback(() -> Text.literal("已寄送信件 " + id + " 給 " + recipient), false);
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
        for (String line : m.content.split("\\n")) {
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
