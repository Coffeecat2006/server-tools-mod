package com.coffeecat2006.mail;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

public class MailMod implements ModInitializer {
    @Override
    public void onInitialize() {
        MailCommands.register(/* pass dispatcher from registration event */);
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (world.getRegistryKey() != World.OVERWORLD) return;
            MailState state = world.getPersistentStateManager().getOrCreate(MailState.TYPE);
            MailManager.init(state);
        });
    }
}