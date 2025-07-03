package com.coffeecat2006.invisible;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;

public class InvisibleMod implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            InvisibleCommands.register(dispatcher);
        });
        
        // 監聽玩家加入事件，確保新玩家看不到隱形玩家
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity newPlayer = handler.player;
            
            // 對新加入的玩家隱藏所有隱形玩家
            InvisibleManager.hideInvisiblePlayersFrom(newPlayer);
            
            // 如果新加入的玩家本身是隱形的，恢復其隱形狀態
            if (InvisibleManager.isInvisible(newPlayer)) {
                newPlayer.setInvisible(true);
            }
        });
        
        // 定期檢查並隱藏隱形玩家的實體（處理裝備和ID顯示問題）
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (InvisibleManager.isInvisible(player)) {
                    // 確保隱形玩家對其他玩家完全不可見
                    for (ServerPlayerEntity otherPlayer : server.getPlayerManager().getPlayerList()) {
                        if (!otherPlayer.equals(player)) {
                            // 定期發送實體銷毀包來確保隱形效果
                            // 這會隱藏玩家的裝備、手持物品和頭上的ID
                            otherPlayer.networkHandler.sendPacket(
                                new net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket(player.getId())
                            );
                        }
                    }
                }
            }
        });
    }
}
