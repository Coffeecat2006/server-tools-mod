package com.coffeecat2006.invisible;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
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

                // 對所有線上玩家廣播：從 TAB 列表移除並隱藏實體
                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                    if (!p.equals(player)) {
                        // 從 Tab 列表移除
                        p.networkHandler.sendPacket(
                            new PlayerListS2CPacket(Action.UPDATE_LISTED, player)
                        );
                        
                        // 讓其他玩家無法看到隱形玩家的實體
                        p.networkHandler.sendPacket(
                            new EntitiesDestroyS2CPacket(player.getId())
                        );
                    }
                }
            }
        } else {
            if (invisiblePlayers.remove(uuid)) {
                player.setInvisible(false);

                // 對所有線上玩家廣播：重新加入 TAB 列表
                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                    if (!p.equals(player)) {
                        // 重新加入 Tab 列表
                        p.networkHandler.sendPacket(
                            new PlayerListS2CPacket(Action.ADD_PLAYER, player)
                        );
                    }
                }
            }
        }
    }

    /**
     * 對特定玩家隱藏所有隱形玩家
     */
    public static void hideInvisiblePlayersFrom(ServerPlayerEntity targetPlayer) {
        MinecraftServer server = targetPlayer.getServer();
        if (server == null) return;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (InvisibleManager.isInvisible(player) && !player.equals(targetPlayer)) {
                targetPlayer.networkHandler.sendPacket(
                    new EntitiesDestroyS2CPacket(player.getId())
                );
            }
        }
    }

    /**
     * 確保所有隱形玩家對所有其他玩家完全不可見
     * 這個方法會定期調用來處理裝備和ID顯示問題
     */
    public static void ensureInvisiblePlayersHidden(MinecraftServer server) {
        if (server == null) return;
        
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (InvisibleManager.isInvisible(player)) {
                // 確保隱形玩家對其他玩家完全不可見
                for (ServerPlayerEntity otherPlayer : server.getPlayerManager().getPlayerList()) {
                    if (!otherPlayer.equals(player)) {
                        // 定期發送實體銷毀包來確保隱形效果
                        otherPlayer.networkHandler.sendPacket(
                            new EntitiesDestroyS2CPacket(player.getId())
                        );
                    }
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