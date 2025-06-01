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
                    //.requires(src -> src.hasPermissionLevel(2))
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                        .then(CommandManager.argument("title", StringArgumentType.string())
                            .then(CommandManager.argument("content", StringArgumentType.string())
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
                    .then(CommandManager.argument("id", StringArgumentType.string())
                        .executes(ctx -> MailManager.read(
                            ctx.getSource(),
                            StringArgumentType.getString(ctx, "id")
                        ))
                    )
                )
                // 領取包裹
                .then(CommandManager.literal("pickup")
                    .then(CommandManager.argument("id", StringArgumentType.string())
                        .executes(ctx -> MailManager.pickup(
                            ctx.getSource(),
                            StringArgumentType.getString(ctx, "id")
                        ))
                    )
                )
                // 刪除信件，含 confirm/cancel
                .then(CommandManager.literal("delete")
                    .then(CommandManager.argument("target", StringArgumentType.string())
                        .executes(ctx -> MailManager.delete( // Initial call, shows confirmation
                            ctx.getSource(),
                            StringArgumentType.getString(ctx, "target"),
                            false
                        ))
                        .then(CommandManager.literal("confirm") // Confirms deletion of "target"
                            .executes(ctx -> MailManager.delete(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "target"), // Uses "target" from parent
                                true
                            )))
                        .then(CommandManager.literal("cancel") // Cancels deletion of "target"
                            .executes(ctx -> MailManager.delete(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "target") + " cancel_true", // Uses "target" from parent and appends marker
                                true // Process immediately
                            )))
                    )
                )
                // 幫助
                .then(CommandManager.literal("help")
                    .executes(ctx -> MailManager.help(ctx.getSource()))
                )
                // 日誌
                .then(CommandManager.literal("log")
                    .requires(src -> src.hasPermissionLevel(2)) // Admin only
                    .then(CommandManager.literal("all")
                        .executes(ctx -> MailManager.mailLog(ctx.getSource(), null, 0, 1)) // targetPlayerName=null for all
                        .then(CommandManager.argument("page", IntegerArgumentType.integer(1))
                            .executes(ctx -> MailManager.mailLog(ctx.getSource(), null, 0, IntegerArgumentType.getInteger(ctx, "page")))))
                    .then(CommandManager.literal("player")
                        .then(CommandManager.argument("playerId", EntityArgumentType.player())
                            .executes(ctx -> MailManager.mailLog(ctx.getSource(), StringArgumentType.getString(ctx, "playerName"), 0, 1))
                            .then(CommandManager.argument("page", IntegerArgumentType.integer(1))
                                .executes(ctx -> MailManager.mailLog(ctx.getSource(), StringArgumentType.getString(ctx, "playerName"), 0, IntegerArgumentType.getInteger(ctx, "page")))))))
                // Admin Pickup
                .then(CommandManager.literal("adminpickup")
                    .requires(src -> src.hasPermissionLevel(2))
                    .then(CommandManager.argument("mailId", StringArgumentType.string())
                        .executes(ctx -> MailManager.adminPickup(ctx.getSource(), StringArgumentType.getString(ctx, "mailId")))))
                // 黑名單
                .then(CommandManager.literal("blacklist")
                    .executes(ctx -> MailManager.showBlacklist(ctx.getSource()))
                    .then(CommandManager.literal("add")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
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
                        .then(CommandManager.argument("player", EntityArgumentType.player())
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