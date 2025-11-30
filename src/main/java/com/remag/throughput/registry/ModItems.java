package com.remag.throughput.registry;

import com.remag.throughput.FTBQuestsThroughputAddon;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FTBQuestsThroughputAddon.MODID);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
