package com.coffeecat2006.redeem;

import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.datafixer.DataFixTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.*;

public class RedeemState extends PersistentState {
    public static final String ID = "redeemmod_codes";
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
            ItemStack.CODEC.listOf().fieldOf("items").forGetter(r -> r.items),
            Codec.INT.fieldOf("redeemedCount").forGetter(r -> r.redeemedCount),
            Codec.STRING.listOf()
                .xmap(
                    list -> { 
                        Set<UUID> s = new HashSet<>(); 
                        for (String u: list) {
                            try {
                                s.add(UUID.fromString(u)); 
                            } catch (IllegalArgumentException e) {
                                // Skip invalid UUIDs
                            }
                        }
                        return s; 
                    },
                    set -> { 
                        List<String> ls = new ArrayList<>(); 
                        for (UUID u: set) ls.add(u.toString()); 
                        return ls; 
                    }
                )
                .fieldOf("usedPlayers").forGetter(r -> r.usedPlayers)
        ).apply(instance, (code, message, limit, expiryEpoch, singleUse, items, redeemedCount, usedPlayers) -> {
            RedeemManager.Redeem r = new RedeemManager.Redeem();
            r.code = code;
            r.message = message;
            r.limit = limit;
            r.expiryEpoch = expiryEpoch;
            r.singleUse = singleUse;
            r.items = new ArrayList<>(items);
            r.redeemedCount = redeemedCount;
            r.usedPlayers = new HashSet<>(usedPlayers);
            return r;
        })
    );

    public static final Codec<RedeemState> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.unboundedMap(Codec.STRING, REDEEM_CODEC)
                 .fieldOf("codes")
                 .forGetter(RedeemState::getCodes)
        ).apply(instance, RedeemState::new)
    );

    // Create a type for Minecraft's persistence system
    public static final Type<RedeemState> TYPE = new Type<>(
        RedeemState::new, 
        (state, codec) -> codec.encodeStart(CODEC, state)
            .getOrThrow(false, error -> {
                throw new RuntimeException("Failed to encode RedeemState: " + error);
            }),
        CODEC
    );

    // Helper method to get or create state from world
    public static RedeemState getOrCreate(ServerWorld world) {
        PersistentStateManager persistentStateManager = world.getPersistentStateManager();
        
        return persistentStateManager.getOrCreate(
            TYPE,
            ID
        );
    }
}