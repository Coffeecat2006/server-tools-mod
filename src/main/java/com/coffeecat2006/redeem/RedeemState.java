// RedeemState.java
package com.coffeecat2006.redeem;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.datafixer.DataFixTypes;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RedeemState extends PersistentState {
    private static final Gson GSON = new Gson();
    private final Map<String, RedeemManager.Redeem> codes = new HashMap<>();

    public RedeemState() {
        super();
    }

    /** 反序列化（供 readNbt 與 getOrCreate 用） */
    public static RedeemState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        RedeemState state = new RedeemState();
        if (nbt.contains("data")) {
            String json = nbt.getString("data").orElse("");
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                RedeemManager.Redeem r = GSON.fromJson(e.getValue(), RedeemManager.Redeem.class);
                state.codes.put(e.getKey(), r);
            }
        }
        return state;
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        codes.clear();
        if (nbt.contains("data")) {
            String json = nbt.getString("data").orElse("");
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                RedeemManager.Redeem r = GSON.fromJson(e.getValue(), RedeemManager.Redeem.class);
                codes.put(e.getKey(), r);
            }
        }
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        JsonObject obj = new JsonObject();
        for (Map.Entry<String, RedeemManager.Redeem> e : codes.entrySet()) {
            obj.add(e.getKey(), GSON.toJsonTree(e.getValue()));
        }
        nbt.putString("data", obj.toString());
        return nbt;
    }

    public Map<String, RedeemManager.Redeem> getCodes() {
        return codes;
    }

    // 最簡 Codec stub——僅為了讓 TYPE 建構子能編譯通過，實務上應換成真正的序/反序列化 codec
    public static final Codec<RedeemState> DUMMY_CODEC = Codec.unit(new RedeemState());

    /** TYPE 定義，供 PersistentStateManager.getOrCreate(TYPE) 使用  */
    public static final PersistentStateType<RedeemState> TYPE =
        new PersistentStateType<>(
            "redeemmod_redeem_codes",
            RedeemState::new,
            DUMMY_CODEC,
            DataFixTypes.SAVED_DATA
        );
}
