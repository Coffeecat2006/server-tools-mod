package com.coffeecat2006.redeem;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.PersistentState;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.Map;

public class RedeemState extends PersistentState {
    private static final Gson GSON = new Gson();
    private final Map<String, RedeemManager.Redeem> codes = new HashMap<>();

    public RedeemState() {
        super();
    }

    public static RedeemState fromNbt(NbtCompound nbt) {
        RedeemState state = new RedeemState();
        if (nbt.contains("data")) {
            // 解包 Optional<String>
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
    public NbtCompound writeNbt(NbtCompound nbt) {
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
}
