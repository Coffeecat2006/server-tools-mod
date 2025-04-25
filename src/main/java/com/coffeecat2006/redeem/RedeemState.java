package com.coffeecat2006.redeem;

import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.datafixer.DataFixTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import java.util.Collections;

import java.util.*;

public class RedeemState extends PersistentState {
    private final Map<String, RedeemManager.Redeem> codes;
    private final List<LogEntry> logs;

    public RedeemState() {
        super();
        this.codes = new HashMap<>();
        this.logs = new ArrayList<>();
    }

    private RedeemState(Map<String, RedeemManager.Redeem> codes, List<LogEntry> logs) {
        super();
        this.codes = new HashMap<>(codes);
        this.logs = new ArrayList<>(logs);
    }

    public Map<String, RedeemManager.Redeem> getCodes() {
        return codes;
    }

    public List<LogEntry> getLogs() {
        return logs;
    }

    public static class LogEntry {
        public long timestamp;
        public String actor;
        public String source;
        public String action;
        public String target;

        public LogEntry() {}

        public LogEntry(long timestamp, String actor, String source, String action, String target) {
            this.timestamp = timestamp;
            this.actor = actor;
            this.source = source;
            this.action = action;
            this.target = target;
        }
    }

    public static final Codec<LogEntry> LOG_ENTRY_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.LONG.fieldOf("timestamp").forGetter(le -> le.timestamp),
            Codec.STRING.fieldOf("actor").forGetter(le -> le.actor),
            Codec.STRING.fieldOf("source").forGetter(le -> le.source),
            Codec.STRING.fieldOf("action").forGetter(le -> le.action),
            Codec.STRING.fieldOf("target").forGetter(le -> le.target)
        ).apply(instance, LogEntry::new)
    );

    public static final Codec<RedeemManager.Redeem> REDEEM_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.fieldOf("code").forGetter(r -> r.code),
            Codec.STRING.fieldOf("message").forGetter(r -> r.message),
            Codec.INT.fieldOf("limit").forGetter(r -> r.limit),
            Codec.LONG.fieldOf("expiryEpoch").forGetter(r -> r.expiryEpoch),
            Codec.BOOL.fieldOf("singleUse").forGetter(r -> r.singleUse),
            Codec.BOOL.fieldOf("available").forGetter(r -> r.available),
            ItemStack.CODEC.listOf().fieldOf("items").forGetter(r -> r.items),
            Codec.INT.fieldOf("redeemedCount").forGetter(r -> r.redeemedCount),
            Codec.STRING.listOf()
                 .xmap(
                     list -> { Set<UUID> s = new HashSet<>(); for (String u: list) s.add(UUID.fromString(u)); return s; },
                     set  -> { List<String> ls = new ArrayList<>(); for (UUID u: set) ls.add(u.toString()); return ls; }
                 )
                 .fieldOf("usedPlayers").forGetter(r -> r.usedPlayers),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("events").forGetter(r -> r.events)
        )
        .apply(instance, (code, message, limit, expiryEpoch, singleUse, available, items, redeemedCount, usedPlayers, events) -> {
            RedeemManager.Redeem r = new RedeemManager.Redeem();
            r.code = code;
            r.message = message;
            r.limit = limit;
            r.expiryEpoch = expiryEpoch;
            r.singleUse = singleUse;
            r.available = available;
            r.items = new ArrayList<>(items);
            r.redeemedCount = redeemedCount;
            r.usedPlayers = new HashSet<>(usedPlayers);
            r.events = new HashMap<>(events);
            return r;
        })
    );

    public static final Codec<RedeemState> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.unboundedMap(Codec.STRING, REDEEM_CODEC)
                 .fieldOf("codes").forGetter(RedeemState::getCodes),
            LOG_ENTRY_CODEC.listOf()
                .optionalFieldOf("logs", Collections.emptyList()).forGetter(RedeemState::getLogs)
        )
        .apply(instance, RedeemState::new)
    );

    public static final PersistentStateType<RedeemState> TYPE =
        new PersistentStateType<>(
            "redeemmod_codes",
            RedeemState::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
        );
}
