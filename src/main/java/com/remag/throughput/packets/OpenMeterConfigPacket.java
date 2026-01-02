package com.remag.throughput.packets;

import com.remag.throughput.FTBQuestsThroughputAddon;
import com.remag.throughput.block.blockentities.ThroughputMeterBlockEntity;
import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.ui.EditConfigScreen;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public record OpenMeterConfigPacket(BlockPos pos) {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(FTBQuestsThroughputAddon.MODID, "open_meter_config");

    public static void register() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, ID, OpenMeterConfigPacket::handle);
    }

    public static void sendToPlayer(ServerPlayer player, BlockPos pos) {
        // 1. Create the backing buffer + wrapper
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.buffer(),
                player.serverLevel().registryAccess()
        );

        // 2. Encode the BlockPos
        BlockPos.STREAM_CODEC.encode(buf, pos);

        // 3. Send full buffer to the player
        NetworkManager.sendToPlayer(player, ID, buf);
    }

    public static void handle(RegistryFriendlyByteBuf buf, NetworkManager.PacketContext ctx) {
        BlockPos pos = BlockPos.STREAM_CODEC.decode(buf);

        ctx.queue(() -> {
            Level level = Minecraft.getInstance().level;
            if (level == null) return;

            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof ThroughputMeterBlockEntity meter)) return;

            Player player = Minecraft.getInstance().player;
            if (player == null) return;

            // Build the ConfigGroup from your BE
            ConfigGroup group = meter.createConfigGroup(player);

            // Open the actual config screen
            new EditConfigScreen(group).setAutoclose(true).openGui();
        });
    }
}

