package com.remag.throughput.tasks;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

public class ThroughputTask extends Task {

    // Assigned in registry
    public static TaskType TYPE;

    public enum Mode {
        ITEMS,
        FLUIDS,
        ENERGY
    }

    private Mode mode = Mode.ITEMS;
    private long requiredRate = 100L;   // items/mB/FE per second
    private int window = 100;          // ticks
    private int sustain = 40;          // ticks at required rate before completion

    public ThroughputTask(long id, Quest quest) {
        super(id, quest);
    }

    // -------------------------------
    // Required overrides
    // -------------------------------

    @Override
    public TaskType getType() {
        return TYPE;
    }

    @Override
    public long getMaxProgress() {
        // Progress bar = sustain time in ticks
        return sustain;
    }

    // -------------------------------
    // Saving & Loading
    // -------------------------------

    @Override
    public void writeData(CompoundTag tag, HolderLookup.Provider provider) {
        super.writeData(tag, provider);

        tag.putString("mode", mode.name());
        tag.putLong("requiredRate", requiredRate);
        tag.putInt("window", window);
        tag.putInt("sustain", sustain);
    }

    @Override
    public void readData(CompoundTag tag, HolderLookup.Provider provider) {
        super.readData(tag, provider);

        mode = Mode.valueOf(tag.getString("mode"));
        requiredRate = tag.getLong("requiredRate");
        window = tag.getInt("window");
        sustain = tag.getInt("sustain");
    }

    // -------------------------------
    // Network Sync (Editor→Client)
    // -------------------------------

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buf) {
        super.writeNetData(buf);

        buf.writeEnum(mode);
        buf.writeVarLong(requiredRate);
        buf.writeVarInt(window);
        buf.writeVarInt(sustain);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buf) {
        super.readNetData(buf);

        mode = buf.readEnum(Mode.class);
        requiredRate = buf.readVarLong();
        window = buf.readVarInt();
        sustain = buf.readVarInt();
    }

    // -------------------------------
    // Config GUI
    // -------------------------------

    @Override
    @OnlyIn(Dist.CLIENT)
    public MutableComponent getAltTitle() {
        String modeName = mode.name().charAt(0) +
                mode.name().substring(1).toLowerCase();
        return Component.literal("Throughput (" + modeName + ")");
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);

        NameMap.Builder<Mode> builder = NameMap.of(
                Mode.ITEMS,
                List.of(Mode.values())
        );

        builder.id(m -> m.name().toLowerCase());

        builder.baseNameKey("throughput.throughput.mode");

        NameMap<Mode> MODE_MAP = builder.create();

        config.addEnum(
                "mode",
                mode,
                v -> mode = v,
                MODE_MAP,
                Mode.ITEMS
        ).setNameKey("throughput.throughput.mode");
        config.addLong("requiredRate", requiredRate, v -> requiredRate = v, 100L, 1L, Long.MAX_VALUE)
                .setNameKey("throughput.throughput.required_rate");
        config.addInt("window", window, v -> window = v, 100, 1, 24000)
                .setNameKey("throughput.throughput.window");
        config.addInt("sustain", sustain, v -> sustain = v, 40, 1, 24000)
                .setNameKey("throughput.throughput.sustain");
    }

    // -------------------------------
    // Getters for block logic
    // -------------------------------

    public Mode getMode() {
        return mode;
    }

    public long getRequiredRate() {
        return requiredRate;
    }

    public int getWindow() {
        return window;
    }

    public int getSustain() {
        return sustain;
    }
}
