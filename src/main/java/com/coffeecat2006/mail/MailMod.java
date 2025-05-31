package com.coffeecat2006.mail;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.text.ClickEvent;
import java.time.Instant;
// UUID import is already covered by java.util.UUID in the existing code if needed explicitly
// For stream: import java.util.stream.Collectors; (Not directly needed for count)

public class MailMod implements ModInitializer {
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            MailCommands.register(dispatcher);
        });

        ServerWorldEvents.LOAD.register((server, world) -> {
            if (world.getRegistryKey() != World.OVERWORLD) return;
            MailState state = world.getPersistentStateManager().getOrCreate(MailState.TYPE);
            MailManager.init(state);
        });

        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            MailState mailState = server.getOverworld().getPersistentStateManager().getOrCreate(MailState.TYPE);
            java.util.UUID playerUuid = player.getUuid();
            String playerName = player.getName().getString(); // Used for recipient check

            // Update known player list (existing logic)
            boolean changedKnownPlayer = false;
            if (!mailState.getKnownPlayersUuids().contains(playerUuid)) {
                mailState.getKnownPlayersUuids().add(playerUuid);
                changedKnownPlayer = true;
            }
            if (!playerName.equals(mailState.getKnownPlayerNames().get(playerUuid))) {
                mailState.getKnownPlayerNames().put(playerUuid, playerName);
                changedKnownPlayer = true;
            }
            if (changedKnownPlayer) {
                mailState.markDirty(); // Mark dirty for known player updates
            }

            // Offline Mail Notification Logic
            long lastLoginTime = mailState.getPlayerLastLoginTimestamps().getOrDefault(playerUuid, 0L);
            long newMailCount = mailState.getMails().values().stream()
                .filter(mail -> mail.recipient.equals(playerName) && !mail.isRead && mail.timestamp > lastLoginTime)
                .count();

            if (newMailCount > 0) {
                MutableText message = Text.literal("您不在时收到了 " + newMailCount + " 封新信件 ")
                    .append(Text.literal("[点我查看]")
                        .formatted(Formatting.GOLD, Formatting.UNDERLINE)
                        .styled(style -> style.withClickEvent(ClickEvent.runCommand("/mail open")))); // Corrected line
                player.sendMessage(message, false);
            }

            // Update last login timestamp for the player
            mailState.getPlayerLastLoginTimestamps().put(playerUuid, Instant.now().getEpochSecond());
            mailState.markDirty(); // Mark dirty for login timestamp update
        });
    }
}