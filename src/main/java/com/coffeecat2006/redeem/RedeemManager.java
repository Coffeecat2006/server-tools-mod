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
import net.minecraft.nbt.NbtCompound;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class RedeemManager {
    private static RedeemState state;
    private static Map<String, Redeem> codes;

    public static void init(RedeemState st) {
        state = st;
        codes = state.getCodes();
    }

    public static class Redeem {
        public String code;
        public String message;
        public int limit;
        public long expiryEpoch;
        public boolean singleUse;
        public List<ItemStack> items;
        public int redeemedCount;
        public Set<UUID> usedPlayers = new HashSet<>();

        public Instant getExpiry() {
            return Instant.ofEpochSecond(expiryEpoch);
        }
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
        if (Instant.now().isAfter(r.getExpiry())) {
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
            Duration left = Duration.between(Instant.now(), r.getExpiry());
            String remain = left.isNegative() ? "已過期" : left.toMinutes() + " 分鐘";

            MutableText line = Text.literal(r.code)
                .formatted(Formatting.AQUA, Formatting.UNDERLINE)
                .styled(style ->
                    style
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
                    String id = item.getItem().toString();
                    String cnt = String.valueOf(item.getCount());
                    NbtCompound tag = item.getOrCreateNbt();
                    String tagString = (tag != null && !tag.isEmpty()) ? tag.toString() : "";
                    String giveCommand = "/give @s " + id + " 1" + (!tagString.isEmpty() ? " " + tagString : "");
                    MutableText it = Text.literal("[" + cnt + "x" + id + "]")
                        .formatted(Formatting.GOLD, Formatting.UNDERLINE)
                        .styled(style ->
                            style
                                .withHoverEvent(new HoverEvent.ShowText(Text.literal("點擊獲取此物品")))
                                .withClickEvent(new ClickEvent.RunCommand(giveCommand))
                        );
                    src.sendFeedback(() -> it, false);
                }
            }
        }

        return 1;
    }

    public static int add(ServerCommandSource src, String code, String text, String limitStr, String timeStr, String rulesStr) {
        if (codes.containsKey(code)) {
            src.sendFeedback(() -> Text.literal("此禮包碼已存在"), false);
            return 0;
        }

        Redeem r = new Redeem();
        r.code = code.equalsIgnoreCase("random")
            ? UUID.randomUUID().toString().replace("-", "").substring(0, new Random().nextInt(5) + 12)
            : code;
        r.message = text;
        r.limit = limitStr.equalsIgnoreCase("infinity") ? -1 : Integer.parseInt(limitStr);
        r.expiryEpoch = timeStr.equalsIgnoreCase("infinity")
            ? Long.MAX_VALUE
            : Instant.now().plus(Duration.ofMinutes(Long.parseLong(timeStr))).getEpochSecond();
        r.singleUse = Boolean.parseBoolean(rulesStr);

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
            Text.literal(r.code).formatted(Formatting.AQUA, Formatting.UNDERLINE)
                .styled(style ->
                    style
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
}
