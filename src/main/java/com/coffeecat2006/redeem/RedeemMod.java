package com.coffeecat2006;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;

public class RedeemMod implements ModInitializer {
    @Override
    public void onInitialize() {
        // 指令註冊
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            RedeemCommands.register(dispatcher);
        });
        // 世界載入時讀取持久化
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (!world.getRegistryKey().getValue().toString().endsWith("overworld")) return;
            // 使用 fromNbt + new + key 三參 getOrCreate
            RedeemState state = world.getPersistentStateManager()
+               .getOrCreate(RedeemState.TYPE);
            RedeemManager.init(state);
        });
    }
}