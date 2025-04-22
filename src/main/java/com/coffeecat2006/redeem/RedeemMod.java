package com.coffeecat2006.redeem;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.world.ServerWorld;

public class RedeemMod implements ModInitializer {
    @Override
    public void onInitialize() {
        // 註冊 /redeem 系列指令
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            RedeemCommands.register(dispatcher);
        });

        // 世界載入時透過 TYPE 讀取或建立 PersistentState
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (!world.getRegistryKey().getValue().toString().endsWith("overworld")) return;

            RedeemState state = world.getPersistentStateManager()
                .getOrCreate(RedeemState.TYPE);  // 僅傳入 TYPE :contentReference[oaicite:3]{index=3}
            RedeemManager.init(state);
        });
    }
}
