package com.coffeecat2006.redeem;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import com.mojang.brigadier.arguments.IntegerArgumentType;

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
                                ))))

                        .then(CommandManager.literal("event")
                            .then(CommandManager.argument("code", StringArgumentType.word())
                                .then(CommandManager.literal("all")
                                    .executes(ctx -> RedeemManager.previewAllEvents(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "code")
                                    ))
                                )
                                .then(CommandManager.argument("eventName", StringArgumentType.word())
                                    .executes(ctx -> RedeemManager.previewEvent(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "code"),
                                        StringArgumentType.getString(ctx, "eventName")
                                    )))));

        var modify_cmd = CommandManager.literal("modify")
        .requires(src -> src.hasPermissionLevel(2))
        .then(CommandManager.argument("code", StringArgumentType.word())
                .then(CommandManager.literal("item")
                .then(CommandManager.literal("reset")
                        .executes(ctx -> RedeemManager.modifyItemReset(
                        ctx.getSource(), StringArgumentType.getString(ctx, "code")))
                )
                .then(CommandManager.literal("transform")
                        .executes(ctx -> RedeemManager.modifyItemTransform(
                        ctx.getSource(), StringArgumentType.getString(ctx, "code")))
                )
                .then(CommandManager.literal("add")
                        .executes(ctx -> RedeemManager.modifyItemAdd(
                        ctx.getSource(), StringArgumentType.getString(ctx, "code")))
                )
                )
                .then(CommandManager.literal("code")
                .then(CommandManager.argument("newcode", StringArgumentType.word())
                        .executes(ctx -> RedeemManager.modifyCode(
                        ctx.getSource(),
                        StringArgumentType.getString(ctx, "code"),
                        StringArgumentType.getString(ctx, "newcode")))
                )
                )
                .then(CommandManager.literal("text")
                .then(CommandManager.argument("text", StringArgumentType.string())
                        .executes(ctx -> RedeemManager.modifyText(
                        ctx.getSource(),
                        StringArgumentType.getString(ctx, "code"),
                        StringArgumentType.getString(ctx, "text")))
                )
                )
                .then(CommandManager.literal("limit")
                .then(CommandManager.argument("limit", StringArgumentType.word())
                        .executes(ctx -> RedeemManager.modifyLimit(
                        ctx.getSource(),
                        StringArgumentType.getString(ctx, "code"),
                        StringArgumentType.getString(ctx, "limit")))
                )
                )
                .then(CommandManager.literal("time")
                .then(CommandManager.argument("time", StringArgumentType.word())
                        .executes(ctx -> RedeemManager.modifyTime(
                        ctx.getSource(),
                        StringArgumentType.getString(ctx, "code"),
                        StringArgumentType.getString(ctx, "time")))
                )
                )
                .then(CommandManager.literal("rules")
                .then(CommandManager.argument("rules", BoolArgumentType.bool())
                        .executes(ctx -> RedeemManager.modifyRules(
                        ctx.getSource(),
                        StringArgumentType.getString(ctx, "code"),
                        BoolArgumentType.getBool(ctx, "rules")))
                )
                )
                .then(CommandManager.literal("receive_status")
                .then(CommandManager.argument("player", StringArgumentType.word())
                        .then(CommandManager.argument("status", BoolArgumentType.bool())
                        .executes(ctx -> RedeemManager.modifyReceiveStatus(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "code"),
                                StringArgumentType.getString(ctx, "player"),
                                BoolArgumentType.getBool(ctx, "status")))
                        )
                )
                )
                .then(CommandManager.literal("available")
                .then(CommandManager.argument("available", BoolArgumentType.bool())
                        .executes(ctx -> RedeemManager.modifyAvailable(
                        ctx.getSource(),
                        StringArgumentType.getString(ctx, "code"),
                        BoolArgumentType.getBool(ctx, "available")))
                )
                )
                .then(CommandManager.literal("event")
                .then(CommandManager.literal("add")
                        .then(CommandManager.argument("ename", StringArgumentType.word())
                        .then(CommandManager.argument("cmd", StringArgumentType.greedyString())
                                .executes(ctx -> RedeemManager.modifyEventAdd(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "code"),
                                StringArgumentType.getString(ctx, "ename"),
                                StringArgumentType.getString(ctx, "cmd")))
                        )
                        )
                )
                .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("ename", StringArgumentType.word())
                        .executes(ctx -> RedeemManager.modifyEventRemove(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "code"),
                                StringArgumentType.getString(ctx, "ename")))
                        )
                )
                .then(CommandManager.literal("reset")
                        .executes(ctx -> RedeemManager.modifyEventReset(
                        ctx.getSource(),
                        StringArgumentType.getString(ctx, "code")))
                )
                )
        );

        var log_cmd = CommandManager.literal("log")
                .requires(src -> src.hasPermissionLevel(2))
                .then(CommandManager.literal("all")
                        .executes(ctx -> RedeemManager.logAll(ctx.getSource(), 0, 1))
                        .then(CommandManager.argument("recent", IntegerArgumentType.integer(1))
                        .executes(ctx -> RedeemManager.logAll(
                                ctx.getSource(),
                                IntegerArgumentType.getInteger(ctx, "recent"),
                                1
                        ))
                        .then(CommandManager.argument("page", IntegerArgumentType.integer(1))
                                .executes(ctx -> RedeemManager.logAll(
                                ctx.getSource(),
                                IntegerArgumentType.getInteger(ctx, "recent"),
                                IntegerArgumentType.getInteger(ctx, "page")
                                ))
                        )
                        )
                )
                .then(CommandManager.literal("code")
                        .then(CommandManager.argument("code", StringArgumentType.word())
                        .then(CommandManager.literal("edits")
                                .executes(ctx -> RedeemManager.logEdits(ctx.getSource(),
                                StringArgumentType.getString(ctx, "code"), 0, 1))
                                .then(CommandManager.argument("recent", IntegerArgumentType.integer(1))
                                .executes(ctx -> RedeemManager.logEdits(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "code"),
                                        IntegerArgumentType.getInteger(ctx, "recent"), 1
                                ))
                                .then(CommandManager.argument("page", IntegerArgumentType.integer(1))
                                        .executes(ctx -> RedeemManager.logEdits(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "code"),
                                        IntegerArgumentType.getInteger(ctx, "recent"),
                                        IntegerArgumentType.getInteger(ctx, "page")
                                        ))
                                )
                                )
                        )
                        .then(CommandManager.literal("redeems")
                                .executes(ctx -> RedeemManager.logRedeems(ctx.getSource(),
                                StringArgumentType.getString(ctx, "code"), 0, 1))
                                .then(CommandManager.argument("recent", IntegerArgumentType.integer(1))
                                .executes(ctx -> RedeemManager.logEdits(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "code"),
                                        IntegerArgumentType.getInteger(ctx, "recent"), 1
                                ))
                                .then(CommandManager.argument("page", IntegerArgumentType.integer(1))
                                        .executes(ctx -> RedeemManager.logEdits(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "code"),
                                        IntegerArgumentType.getInteger(ctx, "recent"),
                                        IntegerArgumentType.getInteger(ctx, "page")
                                        ))
                                )
                                )
                        )
                        )
                )
                .then(CommandManager.literal("player")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(ctx -> RedeemManager.logPlayer(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "player"),
                                0, 1
                        ))
                        .then(CommandManager.argument("recent", IntegerArgumentType.integer(1))
                                .executes(ctx -> RedeemManager.logPlayer(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "player"),
                                IntegerArgumentType.getInteger(ctx, "recent"), 1
                                ))
                                .then(CommandManager.argument("page", IntegerArgumentType.integer(1))
                                .executes(ctx -> RedeemManager.logPlayer(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "player"),
                                        IntegerArgumentType.getInteger(ctx, "recent"),
                                        IntegerArgumentType.getInteger(ctx, "page")
                                ))
                                )
                        )
                        )
                );
        
        var help_cmd = CommandManager.literal("help")
        .executes(ctx -> RedeemManager.helpAll(ctx.getSource()))
        .then(CommandManager.argument("command", StringArgumentType.word())
                .suggests((ctx, sb) -> {
                for (String cmd : new String[]{"add","remove","list","preview","modify","log","help"}) {
                        sb.suggest(cmd);
                }
                return sb.buildFuture();
                })
                .executes(ctx -> RedeemManager.helpCommand(
                ctx.getSource(),
                StringArgumentType.getString(ctx, "command")
                ))
        );

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
                .then(modify_cmd)
                .then(log_cmd)
                .then(help_cmd)
        );


    }
}
