package com.remag.throughput.registry;

import com.remag.throughput.FTBQuestsThroughputAddon;
import com.remag.throughput.block.blockentities.ThroughputMeterBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, FTBQuestsThroughputAddon.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ThroughputMeterBlockEntity>> THROUGHPUT_METER =
            BLOCK_ENTITIES.register("throughput_meter",
                    () -> BlockEntityType.Builder.of(
                            ThroughputMeterBlockEntity::new,
                            ModBlocks.THROUGHPUT_METER.get()
                    ).build(null));

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}
