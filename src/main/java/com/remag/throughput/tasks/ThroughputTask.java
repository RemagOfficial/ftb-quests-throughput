package com.remag.throughput.tasks;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Date;
import java.util.List;

public class ThroughputTask extends Task {

    public static TaskType TYPE;

    public enum Mode {
        ITEMS,
        FLUIDS,
        ENERGY,
        NONE
    }

    private Mode mode = Mode.NONE;
    private long requiredRate = 100L;   // items/mB/FE per second required
    private int window = 40;           // not used directly but kept for editor
    private int sustain = 40;           // seconds of required throughput needed

    public ThroughputTask(long id, Quest quest) {
        super(id, quest);
    }

    // ------------------------------------------------------------
    // BASIC OVERRIDES
    // ------------------------------------------------------------

    @Override
    public TaskType getType() {
        return TYPE;
    }

    @Override
    public long getMaxProgress() {
        return sustain;
    }

    // ------------------------------------------------------------
    // TASK PROGRESS ENTRY POINT (called by Meter BE)
    // ------------------------------------------------------------

    /**
     * Called every second by the Throughput Meter block entity.
     * @param data team progress data
     * @param rate the measured throughput rate this second
     */
    public void submit(TeamData data, long rate) {

        // Already completed? Stop.
        if (data.isCompleted(this)) {
            return;
        }

        // If rate is below requirement → RESET PROGRESS
        if (rate < requiredRate) {
            data.resetProgress(this);
            return;
        }

        // Otherwise, sustain requirement for +1 tick
        long current = data.getProgress(this);
        long next = current + 1;

        data.setProgress(this, next);

        // If sustained long enough → COMPLETE
        if (next >= sustain) {
            data.setCompleted(this.id, new Date());
        }
    }

    // ------------------------------------------------------------
    // SAVING & LOADING
    // ------------------------------------------------------------

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

    // ------------------------------------------------------------
    // NETWORK SYNC
    // ------------------------------------------------------------

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

    // ------------------------------------------------------------
    // CONFIG SCREEN
    // ------------------------------------------------------------

    @Override
    @OnlyIn(Dist.CLIENT)
    public MutableComponent getAltTitle() {
        String modeTitle = mode.name().charAt(0) + mode.name().substring(1).toLowerCase();
        return Component.literal("Throughput (" + modeTitle + ")");
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);

        NameMap.Builder<Mode> builder = NameMap.of(Mode.ITEMS, List.of(Mode.values()));
        builder.id(m -> m.name().toLowerCase());
        builder.baseNameKey("throughput.throughput.mode");
        NameMap<Mode> MODE_MAP = builder.create();

        config.addEnum("mode", mode, v -> mode = v, MODE_MAP, Mode.ITEMS)
                .setNameKey("throughput.throughput.mode");
        config.addLong("requiredRate", requiredRate, v -> requiredRate = v, 100L, 1L, Long.MAX_VALUE)
                .setNameKey("throughput.throughput.required_rate");
        config.addInt("window", window, v -> window = v, 100, 1, 24000)
                .setNameKey("throughput.throughput.window");
        config.addInt("sustain", sustain, v -> sustain = v, 40, 1, 24000)
                .setNameKey("throughput.throughput.sustain");
    }

    // ------------------------------------------------------------
    // GETTERS (used by Meter BE)
    // ------------------------------------------------------------

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
