package com.coffeecat2006;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.time.Instant;
import java.time.Duration;
import java.util.*;

public class RedeemManager {
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

    private static RedeemState state;
    private static Map<String, Redeem> codes;

    public static void init(RedeemState st) {
        state = st;
        codes = state.getCodes();
    }

    private static void save() {
        state.markDirty();
    }

    public static int redeem(ServerCommandSource src, String code) {
        ServerPlayerEntity player;
        try { player = src.getPlayer(); } catch (Exception e) {
            src.sendFeedback(new LiteralText("僅玩家可使用此指令"), false); return 0;
        }
        Redeem r = codes.get(code);
        if (r == null) { src.sendFeedback(new LiteralText("無此禮包碼: " + code), false); return 0; }
        if (Instant.now().isAfter(r.getExpiry())) { src.sendFeedback(new LiteralText("此禮包碼已過期"), false); return 0; }
        if (r.limit>=0 && r.redeemedCount>=r.limit) { src.sendFeedback(new LiteralText("此禮包碼已達使用上限"), false); return 0; }
        if (r.singleUse && r.usedPlayers.contains(player.getUuid())) { src.sendFeedback(new LiteralText("你已經領取過此禮包碼"), false); return 0; }
        for (ItemStack item : r.items) {
            ItemStack give = item.copy(); give.setCount(item.getCount());
            player.inventory.offerOrDrop(src.getWorld(), give);
        }
        r.redeemedCount++;
        r.usedPlayers.add(player.getUuid());
        save();
        src.sendFeedback(new LiteralText(r.message), false);
        return 1;
    }

    public static int list(ServerCommandSource src) {
        src.sendFeedback(new LiteralText("=== Redeem Codes ==="), false);
        codes.values().forEach(r -> {
            Duration left = Duration.between(Instant.now(), r.getExpiry());
            String remain = left.isNegative() ? "已過期" : left.toMinutes()+" 分鐘";
            LiteralText line = new LiteralText(r.code)
                .styled(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("點擊複製")))
                                   .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, r.code)));
            LiteralText info = new LiteralText(String.format(" [%d/%s] 剩餘: %s 訊息: %s",
                r.redeemedCount, r.limit<0?"∞":r.limit, remain, r.message));
            src.sendFeedback(line.append(info), false);
            if (!r.items.isEmpty()) r.items.forEach(item -> {
                String id = item.getItem().toString(), cnt = String.valueOf(item.getCount());
                LiteralText it = new LiteralText("["+cnt+"x"+id+"]")
                    .styled(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("點擊獲取此物品")))
                                       .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/give @s "+id+" 1")));
                src.sendFeedback(it, false);
            });
        });
        return 1;
    }

    public static int add(ServerCommandSource src, String code, String text, String limitStr, String timeStr, String rulesStr) {
        Redeem r = new Redeem();
        r.code = code.equalsIgnoreCase("random")
            ? UUID.randomUUID().toString().replace("-","").substring(0,new Random().nextInt(5)+12)
            : code;
        r.message = text;
        r.limit = limitStr.equalsIgnoreCase("infinity")?-1:Integer.parseInt(limitStr);
        r.expiryEpoch = timeStr.equalsIgnoreCase("infinity")?Long.MAX_VALUE:Instant.now().plus(Duration.ofMinutes(Long.parseLong(timeStr))).getEpochSecond();
        r.singleUse = Boolean.parseBoolean(rulesStr);
        try {
            ServerPlayerEntity player = src.getPlayer();
            ItemStack off = player.getOffHandStack();
            r.items = (off!=null && off.getItem()!=Items.AIR)?Collections.singletonList(off.copy()):new ArrayList<>();
        } catch (Exception e) { r.items = new ArrayList<>(); }
        codes.put(r.code,r);
        save();
        LiteralText ok = new LiteralText("已建立: ")
            .append(new LiteralText(r.code)
                .styled(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,new LiteralText("點擊複製")))
                                   .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD,r.code))));
        src.sendFeedback(ok,false);
        return 1;
    }

    public static int remove(ServerCommandSource src, String code) {
        if (codes.remove(code)!=null) { save(); src.sendFeedback(new LiteralText("已移除禮包碼: " + code), false); }
        else src.sendFeedback(new LiteralText("找不到禮包碼: " + code), false);
        return 1;
    }
}