package com.remag.throughput.packets;

import com.remag.throughput.FTBQuestsThroughputAddon;
import com.remag.throughput.block.blockentities.ThroughputMeterBlockEntity;
import com.remag.throughput.tasks.ThroughputTask;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

public record UpdateMeterConfigPacket(BlockPos pos, long taskId, ThroughputTask.Mode mode) {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(FTBQuestsThroughputAddon.MODID, "update_meter_config");

    // Called in common setup
    public static void register() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, ID, UpdateMeterConfigPacket::handle);
    }

    // CLIENT → SERVER
    public static void send(BlockPos pos, long taskId, ThroughputTask.Mode mode) {
        // Build buffer manually (same as your working S2C packet)
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.buffer(),
                Minecraft.getInstance().level.registryAccess()
        );

        // Encode all fields
        BlockPos.STREAM_CODEC.encode(buf, pos);
        buf.writeVarLong(taskId);
        buf.writeEnum(mode);

        // Send packet
        NetworkManager.sendToServer(ID, buf);
    }

    // SERVER HANDLER
    public static void handle(RegistryFriendlyByteBuf buf, NetworkManager.PacketContext ctx) {
        BlockPos pos = BlockPos.STREAM_CODEC.decode(buf);
        long taskId = buf.readVarLong();
        ThroughputTask.Mode mode = buf.readEnum(ThroughputTask.Mode.class);

        var player = ctx.getPlayer();
        if (!(player instanceof ServerPlayer sp)) {
            return; // should never happen for C2S, but safe
        }

        ctx.queue(() -> {

            BlockEntity be = player.level().getBlockEntity(pos);
            if (be instanceof ThroughputMeterBlockEntity meter) {

                // SERVER applies the changes
                meter.setTask(taskId, mode);
                meter.setChanged();

                // Notify block update
                player.level().sendBlockUpdated(
                        pos,
                        meter.getBlockState(),
                        meter.getBlockState(),
                        3
                );
            }
        });
    }
}
