package com.coffeecat2006.invisible;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public class InvisibleManager {
    // 存放目前被標記為隱形的玩家 UUID
    private static final Set<UUID> invisiblePlayers = new HashSet<>();

    /**
     * 設定玩家隱形狀態，並通知所有線上玩家更新 Tab 列表
     *
     * @param player    要設定的玩家
     * @param invisible true 表示隱形、false 表示恢復可見
     */
    public static void setInvisible(ServerPlayerEntity player, boolean invisible) {
        UUID uuid = player.getUuid();
        MinecraftServer server = player.getServer();
        if (server == null) return;

        if (invisible) {
            if (invisiblePlayers.add(uuid)) {
                player.setInvisible(true);

                // 對所有線上玩家廣播：從 TAB 列表移除
                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                    p.networkHandler.sendPacket(
                        new PlayerListS2CPacket(Action.REMOVE_PLAYER, player)
                    );
                }
            }
        } else {
            if (invisiblePlayers.remove(uuid)) {
                player.setInvisible(false);

                // 對所有線上玩家廣播：重新加入 TAB 列表
                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                    p.networkHandler.sendPacket(
                        new PlayerListS2CPacket(Action.ADD_PLAYER, player)
                    );
                }
            }
        }
    }

    public static boolean isInvisible(UUID uuid) {
        return invisiblePlayers.contains(uuid);
    }

    public static boolean isInvisible(ServerPlayerEntity player) {
        return isInvisible(player.getUuid());
    }
}