package com.coffeecat2006.invisible;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.MinecraftServer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class InvisibleMod implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer(MinecraftServer server) {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            InvisibleCommands.register(dispatcher);
        });

        ServerLifecycleEvents.SERVER_STARTED.register(srv -> {
            InvisibleManager.init();
        });
    }
}
