package com.coffeecat2006.redeem;

import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.datafixer.DataFixTypes;
import com.mojang.serialization.Codec;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;

public class RedeemState extends PersistentState {
    private static final Gson GSON = new Gson();

    /** 編碼／解碼器：這裡用 stub，實務上應實作真正序／反序 */
    public static final Codec<RedeemState> CODEC = Codec.unit(new RedeemState());

    /** TYPE 定義：第一個參數為存檔 ID */
    public static final PersistentStateType<RedeemState> TYPE = new PersistentStateType<>(
        "redeemmod:redeem_codes",   // 存檔檔名
        RedeemState::new,           // 建構函式
        CODEC,                      // Codec
        DataFixTypes.SAVED_DATA     // DataFix 類型
    );

    private final Map<String, RedeemManager.Redeem> codes = new HashMap<>();

    public RedeemState() {
        super();
    }

    /** 取得當前所有禮包碼 */
    public Map<String, RedeemManager.Redeem> getCodes() {
        return codes;
    }
}
