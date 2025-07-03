package com.coffeecat2006.invisible;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.server.MinecraftServer;

public class InvisibleCommands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("invisible")
            .requires(src -> src.hasPermissionLevel(2))
            .then(CommandManager.argument("active", BoolArgumentType.bool())
                .executes(context -> {
                    boolean active = BoolArgumentType.getBool(context, "active");
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    InvisibleManager.setInvisible(player, active);
                    context.getSource().sendFeedback(
                        () -> Text.literal("隱形狀態已設為: " + active),
                        false
                    );
                    return 1;
                })
            )
        );
        
        // 添加查看隱形玩家列表的命令
        dispatcher.register(
            CommandManager.literal("invisiblelist")
            .requires(src -> src.hasPermissionLevel(2))
            .executes(context -> {
                ServerCommandSource source = context.getSource();
                MinecraftServer server = source.getServer();
                
                StringBuilder message = new StringBuilder("當前隱形玩家: ");
                boolean hasInvisiblePlayers = false;
                
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    if (InvisibleManager.isInvisible(player)) {
                        if (hasInvisiblePlayers) {
                            message.append(", ");
                        }
                        message.append(player.getName().getString());
                        hasInvisiblePlayers = true;
                    }
                }
                
                if (!hasInvisiblePlayers) {
                    message.append("無");
                }
                
                source.sendFeedback(() -> Text.literal(message.toString()), false);
                return 1;
            })
        );
        
        // 添加測試隱形功能的命令
        dispatcher.register(
            CommandManager.literal("invisibletest")
            .requires(src -> src.hasPermissionLevel(2))
            .executes(context -> {
                ServerCommandSource source = context.getSource();
                ServerPlayerEntity player = source.getPlayer();
                
                if (player != null) {
                    boolean isInvisible = InvisibleManager.isInvisible(player);
                    source.sendFeedback(
                        () -> Text.literal("當前隱形狀態: " + isInvisible + 
                                         " (setInvisible: " + player.isInvisible() + ")"),
                        false
                    );
                }
                
                return 1;
            })
        );
    }
}
