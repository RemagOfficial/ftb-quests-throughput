package com.remag.throughput.packets;

import com.remag.throughput.block.blockentities.ThroughputMeterBlockEntity;
import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.ui.EditConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Supplier;

public record OpenMeterConfigPacket(BlockPos pos) {

    public static void handle(OpenMeterConfigPacket msg, Supplier<NetworkManager.PacketContext> ctx) {
        ctx.get().enqueueWork(() -> {
            Level level = Minecraft.getInstance().level;
            if (level == null) return;

            BlockEntity be = level.getBlockEntity(msg.pos());
            if (be instanceof ThroughputMeterBlockEntity meter) {

                // Create config group
                Player player = Minecraft.getInstance().player;
                ConfigGroup group = meter.createConfigGroup(player);

                // Open the EditConfigScreen
                Minecraft.getInstance().setScreen(new EditConfigScreen(group));
            }
        });

        ctx.get().setHandled(true);
    }
}

