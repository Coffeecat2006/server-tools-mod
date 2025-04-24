package com.coffeecat2006.redeem;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class RedeemCommands {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {

        var list_cmd = CommandManager.literal("list")
                .requires(src -> src.hasPermissionLevel(2))
                .executes(ctx -> RedeemManager.list(ctx.getSource()));

        var add_cmd = CommandManager.literal("add")
                .requires(src -> src.hasPermissionLevel(2))
                .then(CommandManager.argument("code", StringArgumentType.word())
                        .then(CommandManager.argument("text", StringArgumentType.string())
                                .then(CommandManager.argument("limit", StringArgumentType.word())
                                        .then(CommandManager.argument("time", StringArgumentType.word())
                                                .then(CommandManager.argument("rules", BoolArgumentType.bool())
                                                        .executes(ctx -> RedeemManager.add(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "code"),
                                                                StringArgumentType.getString(ctx, "text"),
                                                                StringArgumentType.getString(ctx, "limit"),
                                                                StringArgumentType.getString(ctx, "time"),
                                                                BoolArgumentType.getBool(ctx, "rules")
                                                        )))))));

        var remove_cmd = CommandManager.literal("remove")
                .requires(src -> src.hasPermissionLevel(2))
                .then(CommandManager.argument("code", StringArgumentType.word())
                        .executes(ctx -> RedeemManager.remove(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "code")
                        )));

        var preview_cmd = CommandManager.literal("preview")
                .requires(src -> src.hasPermissionLevel(2))
                .then(CommandManager.literal("item")
                        .then(CommandManager.argument("code", StringArgumentType.word())
                                .executes(ctx -> RedeemManager.previewItem(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "code")
                                ))))
                .then(CommandManager.literal("text")
                        .then(CommandManager.argument("code", StringArgumentType.word())
                                .executes(ctx -> RedeemManager.previewText(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "code")
                                ))));


        var code = CommandManager.argument("code", StringArgumentType.word())
                .executes(ctx -> RedeemManager.redeem(
                        ctx.getSource(),
                        StringArgumentType.getString(ctx, "code")
                ));

        dispatcher.register(CommandManager.literal("redeem")
                .then(code)
                .then(add_cmd)
                .then(remove_cmd)
                .then(list_cmd)
                .then(preview_cmd)

        );


    }
}
