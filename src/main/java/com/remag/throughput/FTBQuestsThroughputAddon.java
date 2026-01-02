package com.remag.throughput;

import com.remag.throughput.packets.UpdateMeterConfigPacket;
import com.remag.throughput.registry.ModBlockEntities;
import com.remag.throughput.registry.ModBlocks;
import com.remag.throughput.registry.ModItems;
import com.remag.throughput.tasks.ThroughputTask;
import com.remag.throughput.packets.OpenMeterConfigPacket;
import dev.ftb.mods.ftblibrary.FTBLibrary;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.quest.task.TaskTypes;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(FTBQuestsThroughputAddon.MODID)
public class FTBQuestsThroughputAddon {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "throughput";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public FTBQuestsThroughputAddon(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        registerQuests();
        OpenMeterConfigPacket.register();
        UpdateMeterConfigPacket.register();

        ModBlockEntities.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
    }

    private void registerQuests() {
        ThroughputTask.TYPE = TaskTypes.register(ResourceLocation.fromNamespaceAndPath(FTBQuestsThroughputAddon.MODID, "throughput"), ThroughputTask::new, () -> Icon.getIcon("minecraft:item/hopper"));
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == FTBLibrary.getCreativeModeTab().getKey()) {
            event.accept(ModBlocks.THROUGHPUT_METER.get().asItem());
        }
    }
}
