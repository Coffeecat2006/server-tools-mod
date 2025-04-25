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

import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class RedeemManager {
    private static RedeemState state;
    private static Map<String, Redeem> codes;

    public static class Redeem {
        public String code;
        public String message;
        public int limit;
        public long expiryEpoch;
        public boolean singleUse;
        public List<ItemStack> items;
        public int redeemedCount;
        public Set<UUID> usedPlayers = new HashSet<>();
        public boolean available = true;
        public Map<String, String> events = new HashMap<>();
        public Instant getExpiry() { return Instant.ofEpochSecond(expiryEpoch); }
    }

    public static void init(RedeemState st) {
        state = st;
        codes = state.getCodes();
    }

    public static int redeem(ServerCommandSource src, String code) {
        ServerPlayerEntity player;
        try { player = src.getPlayer(); }
        catch (Exception e) {
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
            return 0;
        }
        if (r.limit >= 0 && r.redeemedCount >= r.limit) {
            src.sendFeedback(() -> Text.literal("此禮包碼已達使用上限"), false);
            return 0;
        }
        if (r.singleUse && r.usedPlayers.contains(player.getUuid())) {
            src.sendFeedback(() -> Text.literal("你已經領取過此禮包碼"), false);
            return 0;
        }
        for (ItemStack item : r.items) {
            ItemStack give = item.copy();
            give.setCount(item.getCount());
            player.getInventory().offerOrDrop(give);
        }
        r.events.forEach((ename, cmd) -> {
            String playerName = player.getName().getString();
            src.getServer().getCommandManager()
                .execute(cmd.replace("@s", playerName), src);
        });

        r.redeemedCount++;
        r.usedPlayers.add(player.getUuid());
        state.markDirty();
        src.sendFeedback(() -> Text.literal(r.message), false);
        return 1;
    }

    public static int list(ServerCommandSource src) {
        src.sendFeedback(() -> Text.literal("=== Redeem Codes ==="), false);
        for (Redeem r : codes.values()) {
            String remain;
            if (r.expiryEpoch == Long.MAX_VALUE) {
                remain = "永不過期";
            } else {
                Instant expiry = Instant.ofEpochSecond(r.expiryEpoch);
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
                MutableText evtBtn = Text.literal(" [事件]")
                    .formatted(Formatting.LIGHT_PURPLE, Formatting.UNDERLINE)
                    .styled(style -> style
                        .withHoverEvent(new HoverEvent.ShowText(Text.literal("點擊查看此 code 的所有事件")))
                        .withClickEvent(new ClickEvent.RunCommand("/redeem preview event " + r.code + " all"))
                    );
                entry.append(evtBtn);
            }
    
            src.sendFeedback(() -> entry, false);

            src.sendFeedback(() -> line.append(info), false);
            if (!r.items.isEmpty()) {
                for (ItemStack item : r.items) {
                    String label = item.getCount() + "x" + item.getItem().toString();
                    MutableText it = Text.literal("[" + label + "]")
                        .formatted(Formatting.GOLD, Formatting.UNDERLINE)
                        .styled(style -> style
                            .withHoverEvent(new HoverEvent.ShowText(Text.literal("點擊獲取此物品")))
                            .withClickEvent(new ClickEvent.RunCommand("/redeem_preview item " + r.code))
                        );
                    src.sendFeedback(() -> it, false);
                }
            }
        }
        return 1;
    }

    public static int add(ServerCommandSource src, String code, String text, String limitStr, String timeStr, boolean singleUse) {
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
        try {
            ServerPlayerEntity player = src.getPlayer();
            ItemStack off = player.getOffHandStack();
            r.items = (off != null && off.getItem() != Items.AIR)
                ? Collections.singletonList(off.copy())
                : new ArrayList<>();
        } catch (Exception e) {
            r.items = new ArrayList<>();
        }
        codes.put(r.code, r);
        state.markDirty();
        MutableText ok = Text.literal("已建立: ").append(
            Text.literal(r.code)
                .formatted(Formatting.AQUA, Formatting.UNDERLINE)
                .styled(style -> style
                    .withHoverEvent(new HoverEvent.ShowText(Text.literal("點擊複製")))
                    .withClickEvent(new ClickEvent.CopyToClipboard(r.code))
                )
        );
        src.sendFeedback(() -> ok, false);
        return 1;
    }

    public static int remove(ServerCommandSource src, String code) {
        if (codes.remove(code) != null) {
            state.markDirty();
            src.sendFeedback(() -> Text.literal("已移除禮包碼: " + code), false);
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
        return feedback(src, "已重置物品");
    }

    public static int modifyItemTransform(ServerCommandSource src, String code) {
        Redeem r = codes.get(code);
        if (r == null) return feedback(src, "無此禮包碼: " + code);
        try {
            ItemStack off = src.getPlayer().getOffHandStack();
            r.items = off.getItem() != Items.AIR
                ? Collections.singletonList(off.copy())
                : new ArrayList<>();
        } catch (Exception e) {
            r.items.clear();
        }
        state.markDirty();
        return feedback(src, "已將物品轉換為副手物品");
    }

    public static int modifyItemAdd(ServerCommandSource src, String code) {
        Redeem r = codes.get(code);
        if (r == null) return feedback(src, "無此禮包碼: " + code);
        try {
            ItemStack off = src.getPlayer().getOffHandStack();
            if (off.getItem() != Items.AIR) r.items.add(off.copy());
        } catch (Exception ignored) {}
        state.markDirty();
        return feedback(src, "已新增副手物品");
    }

    public static int modifyCode(ServerCommandSource src, String code, String newCode) {
        if (!codes.containsKey(code)) return feedback(src, "無此禮包碼: " + code);
        if (codes.containsKey(newCode)) return feedback(src, "新 code 已存在");
        Redeem r = codes.remove(code);
        r.code = newCode;
        codes.put(newCode, r);
        state.markDirty();
        return feedback(src, "已更新 code 為: " + newCode);
    }

    public static int modifyText(ServerCommandSource src, String code, String text) {
        Redeem r = codes.get(code);
        if (r == null) return feedback(src, "無此禮包碼: " + code);
        r.message = text;
        state.markDirty();
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
        return feedback(src, "已更新過期時間");
    }

    public static int modifyRules(ServerCommandSource src, String code, boolean singleUse) {
        Redeem r = codes.get(code);
        if (r == null) return feedback(src, "無此禮包碼: " + code);
        r.singleUse = singleUse;
        state.markDirty();
        return feedback(src, "已更新單次領取規則");
    }

    public static int modifyReceiveStatus(ServerCommandSource src, String code, String playerId, boolean status) {
        Redeem r = codes.get(code);
        if (r == null) return feedback(src, "無此禮包碼: " + code);
        try {
            UUID uuid = UUID.fromString(playerId);
            if (status) r.usedPlayers.add(uuid);
            else r.usedPlayers.remove(uuid);
        } catch (IllegalArgumentException e) {
            return feedback(src, "playerId 格式錯誤");
        }
        state.markDirty();
        return feedback(src, "已更新該玩家的領取狀態");
    }

    public static int modifyAvailable(ServerCommandSource src, String code, boolean available) {
        Redeem r = codes.get(code);
        if (r == null) return feedback(src, "無此禮包碼: " + code);
        r.available = available;
        state.markDirty();
        return feedback(src, available ? "已設為可用" : "已隱藏該禮包碼");
    }

    public static int modifyEventAdd(ServerCommandSource src, String code, String name, String cmd) {
        Redeem r = codes.get(code);
        if (r == null) return feedback(src, "無此禮包碼: " + code);
        r.events.put(name, cmd);
        state.markDirty();
        return feedback(src, "已新增事件: " + name);
    }

    public static int modifyEventRemove(ServerCommandSource src, String code, String name) {
        Redeem r = codes.get(code);
        if (r == null) return feedback(src, "無此禮包碼: " + code);
        r.events.remove(name);
        state.markDirty();
        return feedback(src, "已移除事件: " + name);
    }

    public static int modifyEventReset(ServerCommandSource src, String code) {
        Redeem r = codes.get(code);
        if (r == null) return feedback(src, "無此禮包碼: " + code);
        r.events.clear();
        state.markDirty();
        return feedback(src, "已清空所有事件");
    }

    private static int feedback(ServerCommandSource src, String msg) {
        src.sendFeedback(() -> Text.literal(msg), false);
        return 1;
    }
}
