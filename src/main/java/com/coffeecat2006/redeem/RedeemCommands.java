package com.coffeecat2006.redeem;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class RedeemCommands {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("redeem")
            .then(CommandManager.argument("code", StringArgumentType.string())
                .executes(ctx -> RedeemManager.redeem(
                    ctx.getSource(), StringArgumentType.getString(ctx, "code")
                ))));
        dispatcher.register(CommandManager.literal("redeem_list")
            .requires(src -> src.hasPermissionLevel(2))
            .executes(ctx -> RedeemManager.list(ctx.getSource())));
        dispatcher.register(CommandManager.literal("redeem_add")
            .requires(src -> src.hasPermissionLevel(2))
            .then(CommandManager.argument("code", StringArgumentType.string())
            .then(CommandManager.argument("text", StringArgumentType.greedyString())
            .then(CommandManager.argument("limit", StringArgumentType.string())
            .then(CommandManager.argument("time", StringArgumentType.string())
            .then(CommandManager.argument("rules", StringArgumentType.string())
            .executes(ctx -> RedeemManager.add(
                ctx.getSource(),
                StringArgumentType.getString(ctx, "code"),
                StringArgumentType.getString(ctx, "text"),
                StringArgumentType.getString(ctx, "limit"),
                StringArgumentType.getString(ctx, "time"),
                StringArgumentType.getString(ctx, "rules")
            ))))))));
        dispatcher.register(CommandManager.literal("redeem_remove")
            .requires(src -> src.hasPermissionLevel(2))
            .then(CommandManager.argument("code", StringArgumentType.string())
                .executes(ctx -> RedeemManager.remove(
                    ctx.getSource(), StringArgumentType.getString(ctx, "code")
                ))));
    }
}