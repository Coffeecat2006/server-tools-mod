package com.coffeecat2006.redeem;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.datafixer.DataFixTypes;
import com.mojang.serialization.Codec;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RedeemState extends PersistentState {
    private static final Gson GSON = new Gson();
    private final Map<String, RedeemManager.Redeem> codes = new HashMap<>();

    /** Stub Codec：實務上請實作真正的序／反序 */
    public static final Codec<RedeemState> CODEC = Codec.unit(new RedeemState());

    /** TYPE 定義（id, factory, codec, dataFixType） */
    public static final PersistentStateType<RedeemState> TYPE =
        new PersistentStateType<>(
            "redeemmod:redeem_codes",               // 存檔檔名
            RedeemState::new,                        // 建構函式
            CODEC,                                   // 編/解碼器
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE  // 指令儲存資料
        );

    public RedeemState() {
        super();
    }

    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        codes.clear();
        if (nbt.contains("data")) {
            // 處理 getString 返回的 Optional<String>
            Optional<String> jsonOpt = nbt.getString("data");
            if (jsonOpt.isPresent()) {
                String jsonStr = jsonOpt.get();
                JsonObject obj = JsonParser.parseString(jsonStr).getAsJsonObject();
                for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                    RedeemManager.Redeem r = GSON.fromJson(e.getValue(), RedeemManager.Redeem.class);
                    codes.put(e.getKey(), r);
                }
            }
        }
    }

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
}