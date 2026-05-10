package com.deletemod;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.ClearTitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DeleteMod implements ModInitializer {

    public static final String MOD_ID = "deletemod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // UUID → 残りtick数
    private static final Map<UUID, Integer> DELETE_TIMERS = new HashMap<>();
    // UUID → ワールド
    private static final Map<UUID, ServerWorld> ENTITY_WORLDS = new HashMap<>();

    @Override
    public void onInitialize() {
        LOGGER.info("[DeleteMod] /delete コマンドを登録中...");

        // /delete コマンド登録
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(
                CommandManager.literal("delete")
                    .requires(src -> src.hasPermissionLevel(2))
                    .then(
                        CommandManager.argument("target", EntityArgumentType.entities())
                            .executes(this::executeDelete)
                    )
            )
        );

        // 毎tickタイマーを処理
        ServerTickEvents.END_SERVER_TICK.register(server -> tickTimers());

        LOGGER.info("[DeleteMod] 登録完了！");
    }

    // /delete が実行されたとき
    private int executeDelete(CommandContext<ServerCommandSource> ctx) {
        try {
            Collection<? extends Entity> targets = EntityArgumentType.getEntities(ctx, "target");

            for (Entity entity : targets) {
                UUID uuid = entity.getUuid();
                ServerWorld world = (ServerWorld) entity.getWorld();

                // 200tick = 10秒
                DELETE_TIMERS.put(uuid, 200);
                ENTITY_WORLDS.put(uuid, world);

                // プレイヤーならWARNINGタイトルを表示
                if (entity instanceof ServerPlayerEntity player) {
                    showWarning(player);
                }

                // パーティクル発生
                spawnParticle(world, entity.getPos());
            }

            ctx.getSource().sendFeedback(() ->
                Text.literal("§c[Delete] §f" + targets.size() + "体に警告を発令。10秒後にY=-1024へ移動します。"),
                true
            );

            return targets.size();

        } catch (Exception e) {
            LOGGER.error("[DeleteMod] エラー: ", e);
            return 0;
        }
    }

    // 毎tick呼ばれる処理
    private void tickTimers() {
        if (DELETE_TIMERS.isEmpty()) return;

        new HashMap<>(DELETE_TIMERS).forEach((uuid, ticksLeft) -> {
            ServerWorld world = ENTITY_WORLDS.get(uuid);
            if (world == null) { cleanup(uuid); return; }

            Entity entity = world.getEntity(uuid);
            if (entity == null || entity.isRemoved()) { cleanup(uuid); return; }

            int newTick = ticksLeft - 1;

            // 10tickごとにパーティクルとタイトル点滅
            if (newTick % 10 == 0) {
                spawnParticle(world, entity.getPos());

                if (entity instanceof ServerPlayerEntity player) {
                    if ((newTick / 10) % 2 == 0) {
                        showWarning(player);
                    } else {
                        player.networkHandler.sendPacket(new ClearTitleS2CPacket(false));
                    }
                }
            }

            // 0になったらY=-1024へテレポート
            if (newTick <= 0) {
                entity.teleport(entity.getX(), -1024, entity.getZ());

                if (entity instanceof ServerPlayerEntity player) {
                    player.networkHandler.sendPacket(new ClearTitleS2CPacket(true));
                    player.sendMessage(Text.literal("§c[Delete] §fボイドへ送られました。"), false);
                }

                cleanup(uuid);
            } else {
                DELETE_TIMERS.put(uuid, newTick);
            }
        });
    }

    // WARNINGタイトルを点滅表示
    private void showWarning(ServerPlayerEntity player) {
        player.networkHandler.sendPacket(new TitleFadeS2CPacket(0, 12, 0));
        player.networkHandler.sendPacket(new TitleS2CPacket(
            Text.literal("⚠ WARNING ⚠").styled(s -> s.withColor(0xFF0000).withBold(true))
        ));
    }

    // enchanted_hit パーティクル
    private void spawnParticle(ServerWorld world, Vec3d pos) {
        world.spawnParticles(ParticleTypes.ENCHANTED_HIT,
            pos.x, pos.y + 1.0, pos.z,
            20, 0.4, 0.6, 0.4, 0.1
        );
    }

    // タイマーとワールド参照を削除
    private void cleanup(UUID uuid) {
        DELETE_TIMERS.remove(uuid);
        ENTITY_WORLDS.remove(uuid);
    }
}
