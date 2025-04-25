package com.coffeecat2006.redeem;

import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.datafixer.DataFixTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import java.util.*;

public class RedeemState extends PersistentState {
    private final Map<String, RedeemManager.Redeem> codes;

    public RedeemState() {
        super();
        this.codes = new HashMap<>();
    }

    private RedeemState(Map<String, RedeemManager.Redeem> codes) {
        super();
        this.codes = new HashMap<>(codes);
    }

    public Map<String, RedeemManager.Redeem> getCodes() {
        return codes;
    }

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
            Codec.unboundedMap(Codec.STRING, Codec.STRING)
                 .fieldOf("events").forGetter(r -> r.events)
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
                 .fieldOf("codes")
                 .forGetter(RedeemState::getCodes)
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
