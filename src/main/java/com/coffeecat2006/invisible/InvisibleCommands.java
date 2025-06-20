package com.coffeecat2006.invisible;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

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
                        Text.literal("隱形狀態已設為: " + active),
                        false
                    );
                    return 1;
                })
            )
        );
    }
}
