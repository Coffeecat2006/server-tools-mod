package com.coffeecat2006.redeem;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentStateManager;

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
            PersistentStateManager manager = world.getPersistentStateManager();
            RedeemState state = manager.getOrCreate(RedeemState.TYPE);
            RedeemManager.init(state);
        });
    }
}
