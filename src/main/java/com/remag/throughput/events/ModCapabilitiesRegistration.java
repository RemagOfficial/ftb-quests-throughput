package com.remag.throughput.events;

import com.remag.throughput.block.blockentities.ThroughputMeterBlockEntity;
import com.remag.throughput.registry.ModBlockEntities;
import net.minecraft.core.Direction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = com.remag.throughput.FTBQuestsThroughputAddon.MODID)
public class ModCapabilitiesRegistration {

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // Register item handler for the throughput meter BE
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.THROUGHPUT_METER.get(),
                ThroughputMeterBlockEntity::getItemHandler
        );

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.THROUGHPUT_METER.get(),
                ThroughputMeterBlockEntity::getFluidHandler
        );
    }
}
