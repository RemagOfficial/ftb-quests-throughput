package com.remag.throughput.packets;

import com.remag.throughput.FTBQuestsThroughputAddon;
import com.remag.throughput.block.blockentities.ThroughputMeterBlockEntity;
import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.ui.EditConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Supplier;

public record OpenMeterConfigPacket(BlockPos pos) {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(FTBQuestsThroughputAddon.MODID, "open_meter_config");

    public static void register() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, ID, OpenMeterConfigPacket::handle);
    }

    public static void sendToPlayer(ServerPlayer player, BlockPos pos) {
        NetworkManager.sendToPlayer(player, ID, buf -> buf.writeBlockPos(pos));
    }

    public static void handle(FriendlyByteBuf buf, NetworkManager.PacketContext ctx) {
        BlockPos pos = buf.readBlockPos();

        ctx.queue(() -> {
            Level level = Minecraft.getInstance().level;
            if (level == null) return;

            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ThroughputMeterBlockEntity meter) {
                Player player = Minecraft.getInstance().player;
                ConfigGroup group = meter.createConfigGroup(player);

                Minecraft.getInstance().setScreen(new EditConfigScreen(group).getPrevScreen());
            }
        });

        ctx.setHandled(true);
    }
}

