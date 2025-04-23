package com.coffeecat2006.redeem;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.world.ServerWorld;

public class RedeemMod implements ModInitializer {
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            RedeemCommands.register(dispatcher)
        );

        ServerWorldEvents.LOAD.register((server, world) -> {
            if (!world.getRegistryKey().getValue().toString().endsWith("overworld")) return;

            RedeemState state = world.getPersistentStateManager()
                .getOrCreate(RedeemState.TYPE, "redeemmod_codes");
            RedeemManager.init(state);
        });
    }
}
