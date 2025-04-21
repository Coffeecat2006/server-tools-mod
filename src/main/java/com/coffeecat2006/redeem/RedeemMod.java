// RedeemMod.java
package com.coffeecat2006.redeem;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.world.ServerWorld;

public class RedeemMod implements ModInitializer {
    @Override
    public void onInitialize() {
        // 註冊指令
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            RedeemCommands.register(dispatcher);
        });

        // 世界載入時初始化 PersistentState
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (!world.getRegistryKey().getValue().toString().endsWith("overworld")) return;

            // 新版 API：只用 TYPE 取回狀態 
            RedeemState state = world.getPersistentStateManager()
                .getOrCreate(RedeemState.TYPE);
            RedeemManager.init(state);
        });
    }
}
