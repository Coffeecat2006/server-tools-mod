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
        public Instant getExpiry() { return Instant.ofEpochSecond(expiryEpoch); }
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
        if (r == null) {
            src.sendFeedback(() -> Text.literal("無此禮包碼: " + code), false);
            return 0;
        }
        if (r.expiryEpoch != Long.MAX_VALUE && Instant.now().isAfter(Instant.ofEpochSecond(r.expiryEpoch))) {
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
}
