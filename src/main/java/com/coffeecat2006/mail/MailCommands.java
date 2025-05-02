package com.coffeecat2006.mail;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class MailCommands {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("mail")
                // 打開信箱
                .then(CommandManager.literal("open")
                    .executes(ctx -> MailManager.openInbox(ctx.getSource(), 1))
                    .then(CommandManager.argument("page", IntegerArgumentType.integer(1))
                        .executes(ctx -> MailManager.openInbox(
                            ctx.getSource(),
                            IntegerArgumentType.getInteger(ctx, "page")
                        ))
                    )
                )
                // 寄送信件
                .then(CommandManager.literal("send")
                    .requires(src -> src.hasPermissionLevel(2))
                    .then(CommandManager.argument("player", StringArgumentType.word())
                        .then(CommandManager.argument("title", StringArgumentType.greedyString())
                            .then(CommandManager.argument("content", StringArgumentType.greedyString())
                                .then(CommandManager.argument("item", BoolArgumentType.bool())
                                    .executes(ctx -> MailManager.send(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "player"),
                                        StringArgumentType.getString(ctx, "title"),
                                        StringArgumentType.getString(ctx, "content"),
                                        BoolArgumentType.getBool(ctx, "item")
                                    ))))))
                )
                // 閱讀信件
                .then(CommandManager.literal("read")
                    .then(CommandManager.argument("id", StringArgumentType.word())
                        .executes(ctx -> MailManager.read(
                            ctx.getSource(),
                            StringArgumentType.getString(ctx, "id")
                        ))
                    )
                )
                // 領取包裹
                .then(CommandManager.literal("pickup")
                    .then(CommandManager.argument("id", StringArgumentType.word())
                        .executes(ctx -> MailManager.pickup(
                            ctx.getSource(),
                            StringArgumentType.getString(ctx, "id")
                        ))
                    )
                )
                // 刪除信件，含 confirm/cancel
                .then(CommandManager.literal("delete")
                    .then(CommandManager.argument("target", StringArgumentType.word())
                        .executes(ctx -> MailManager.delete(
                            ctx.getSource(),
                            StringArgumentType.getString(ctx, "target"),
                            false
                        ))
                        .then(CommandManager.literal("confirm")
                            .executes(ctx -> MailManager.delete(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "target"),
                                true
                            ))
                        )
                        .then(CommandManager.literal("cancel")
                            .executes(ctx -> MailManager.delete(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "target"),
                                false
                            ))
                        )
                    )
                )
                // 幫助
                .then(CommandManager.literal("help")
                    .executes(ctx -> MailManager.help(ctx.getSource()))
                )
                // 日誌
                .then(CommandManager.literal("log")
                    .requires(src -> src.hasPermissionLevel(2))
                    .then(CommandManager.argument("page", IntegerArgumentType.integer(1))
                        .executes(ctx -> MailManager.log(
                            ctx.getSource(),
                            IntegerArgumentType.getInteger(ctx, "page")
                        ))
                    )
                )
                // 黑名單
                .then(CommandManager.literal("blacklist")
                    .executes(ctx -> MailManager.showBlacklist(ctx.getSource()))
                    .then(CommandManager.literal("add")
                        .then(CommandManager.argument("player", StringArgumentType.word())
                            .executes(ctx -> MailManager.blacklistAdd(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "player"), false
                            ))
                            .then(CommandManager.literal("confirm")
                                .executes(ctx -> MailManager.blacklistAdd(
                                    ctx.getSource(),
                                    StringArgumentType.getString(ctx, "player"), true
                                ))
                            )
                            .then(CommandManager.literal("cancel")
                                .executes(ctx -> MailManager.blacklistAdd(
                                    ctx.getSource(),
                                    StringArgumentType.getString(ctx, "player"), false
                                ))
                            )
                        )
                    )
                    .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("player", StringArgumentType.word())
                            .executes(ctx -> MailManager.blacklistRemove(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "player")
                            ))
                        )
                    )
                )
        );
    }
}