package com.coffeecat2006;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.PersistentState;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.Map;

public class RedeemState extends PersistentState {
    private static final Gson GSON = new Gson();
    private Map<String, RedeemManager.Redeem> codes = new HashMap<>();

    public RedeemState() { super(); }

    public static RedeemState fromNbt(CompoundTag tag) {
        RedeemState state = new RedeemState();
        if (tag.contains("data", 8)) {
            String json = tag.getString("data");
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                RedeemManager.Redeem r = GSON.fromJson(e.getValue(), RedeemManager.Redeem.class);
                state.codes.put(e.getKey(), r);
            }
        }
        return state;
    }

    @Override
    public CompoundTag writeNbt(CompoundTag tag) {
        JsonObject obj = new JsonObject();
        for (Map.Entry<String, RedeemManager.Redeem> e : codes.entrySet()) {
            obj.add(e.getKey(), GSON.toJsonTree(e.getValue()));
        }
        tag.putString("data", obj.toString());
        return tag;
    }

    public Map<String, RedeemManager.Redeem> getCodes() {
        return codes;
    }
}
