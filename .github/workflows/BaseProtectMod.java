package com.groundone.baseprotect;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;

import java.util.UUID;

public class BaseProtectMod implements ModInitializer {
    public static final String MOD_ID = "baseprotect";

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerCommands(dispatcher);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            BaseProtectState state = BaseProtectState.getServerState(server);
            if (!state.isEnabled() || !state.areCornersSet()) return;

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.hasPermissions(2) || state.isSafe(player.getUUID())) {
                    continue;
                }

                if (state.isInside(player.getX(), player.getZ())) {
                    LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(player.serverLevel());
                    if (lightning != null) {
                        lightning.moveTo(player.position());
                        player.serverLevel().addFreshEntity(lightning);
                    }
                }
            }
        });
    }

    private void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (int i = 1; i <= 4; i++) {
            final int cornerIndex = i - 1;
            dispatcher.register(Commands.literal("cor" + i)
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    BlockPos pos = player.blockPosition();
                    BaseProtectState state = BaseProtectState.getServerState(ctx.getSource().getServer());
                    state.setCorner(cornerIndex, pos);
                    ctx.getSource().sendSuccess(() -> Component.literal("Corner " + (cornerIndex + 1) + " set to " + pos.toShortString()), true);
                    return 1;
                })
            );
        }

        dispatcher.register(Commands.literal("saveperson")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> {
                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                    BaseProtectState state = BaseProtectState.getServerState(ctx.getSource().getServer());
                    if (state.addSafePlayer(target.getUUID())) {
                        ctx.getSource().sendSuccess(() -> Component.literal(target.getScoreboardName() + " is now safe."), true);
                    } else {
                        ctx.getSource().sendFailure(Component.literal(target.getScoreboardName() + " is already on the safe list."));
                    }
                    return 1;
                })
            )
        );

        dispatcher.register(Commands.literal("unsaveperson")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> {
                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                    BaseProtectState state = BaseProtectState.getServerState(ctx.getSource().getServer());
                    if (state.removeSafePlayer(target.getUUID())) {
                        ctx.getSource().sendSuccess(() -> Component.literal(target.getScoreboardName() + " was removed from the safe list."), true);
                    } else {
                        ctx.getSource().sendFailure(Component.literal(target.getScoreboardName() + " is not in the safe list."));
                    }
                    return 1;
                })
            )
        );

        dispatcher.register(Commands.literal("baseprotect")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("sub", com.mojang.brigadier.arguments.StringArgumentType.word())
                .executes(ctx -> {
                    String sub = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "sub");
                    BaseProtectState state = BaseProtectState.getServerState(ctx.getSource().getServer());
                    if (sub.equalsIgnoreCase("on")) {
                        state.setEnabled(true);
                        ctx.getSource().sendSuccess(() -> Component.literal("Base protection enabled."), true);
                    } else if (sub.equalsIgnoreCase("off")) {
                        state.setEnabled(false);
                        ctx.getSource().sendSuccess(() -> Component.literal("Base protection disabled."), true);
                    } else if (sub.equalsIgnoreCase("clear")) {
                        state.clearCorners();
                        ctx.getSource().sendSuccess(() -> Component.literal("Corners cleared."), true);
                    } else if (sub.equalsIgnoreCase("status")) {
                        ctx.getSource().sendSuccess(() -> Component.literal("Protection: " + (state.isEnabled() ? "ON" : "OFF")), false);
                        for (int i = 0; i < 4; i++) {
                            BlockPos c = state.getCorner(i);
                            String val = (c == null) ? "Not set" : c.toShortString();
                            int finalI = i;
                            ctx.getSource().sendSuccess(() -> Component.literal("Corner " + (finalI + 1) + ": " + val), false);
                        }
                    } else if (sub.equalsIgnoreCase("list")) {
                        ctx.getSource().sendSuccess(() -> Component.literal("Safe list count: " + state.getSafePlayers().size()), false);
                    }
                    return 1;
                })
            )
        );
    }
}
