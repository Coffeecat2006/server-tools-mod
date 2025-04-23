package com.coffeecat2006.redeem;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.dimension.DimensionTypes;

public class RedeemMod implements ModInitializer {
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            RedeemCommands.register(dispatcher)
        );
        
        // Initialize the redeem manager when the overworld is loaded
        ServerWorldEvents.LOAD.register((server, world) -> {
            // Only initialize once using the overworld
            if (world.getDimensionKey() != DimensionTypes.OVERWORLD) {
                return;
            }
            
            RedeemState state = RedeemState.getOrCreate(world);
            RedeemManager.init(state);
        });
    }
}