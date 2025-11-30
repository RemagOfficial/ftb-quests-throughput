package com.remag.throughput.block.blockentities;

import com.remag.throughput.registry.ModBlockEntities;
import com.remag.throughput.tasks.ThroughputTask;
import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftbquests.util.ConfigQuestObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ThroughputMeterBlockEntity extends BlockEntity {

    private long taskId = -1;                        // assigned task
    private ThroughputTask.Mode mode = ThroughputTask.Mode.ITEMS;

    private int sustainTicks = 0;                    // will matter later
    private int windowPos = 0;
    private long lastTickAmount = 0;

    // placeholder — will become your rolling window later
    private long[] sampleWindow = new long[100];

    public ThroughputMeterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.THROUGHPUT_METER.get(), pos, state);
    }

    public ConfigGroup createConfigGroup(Player player) {
        ConfigGroup cg0 = new ConfigGroup("throughput_meter", accepted -> {
            if (accepted) {
                // When the player clicks "OK"
                // send updated values to server
                NetworkManager.sendToServer(new UpdateMeterConfigPacket(getBlockPos(), taskId, mode));
            }
        });

        cg0.setNameKey("throughput.throughput"); // your title

        ConfigGroup cg = cg0.getOrCreateSubgroup("settings");

        // Task selector
        cg.add(
                "task",
                new ConfigQuestObject<>(
                        o -> o instanceof ThroughputTask, // filter
                        this::formatLine
                ),
                getTask(),
                this::setTask,
                null
        ).setNameKey("throughput.task");

        return cg0;
    }

    // --------- Ticking (server only) ----------
    public static void serverTick(Level level, BlockPos pos, BlockState state, ThroughputMeterBlockEntity be) {
        be.tick();
    }

    public void tick() {
        // no logic yet — will be implemented later
    }

    // --------- Getters/Setters ----------
    public void setTask(long id, ThroughputTask.Mode newMode) {
        this.taskId = id;
        this.mode = newMode;
        this.setChanged();
    }

    public long getTaskId() {
        return taskId;
    }

    public ThroughputTask.Mode getMode() {
        return mode;
    }

    // --------- NBT ----------
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);

        tag.putLong("TaskId", taskId);
        tag.putString("Mode", mode.name());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);

        this.taskId = tag.getLong("TaskId");

        try {
            this.mode = ThroughputTask.Mode.valueOf(tag.getString("Mode"));
        } catch (Exception e) {
            this.mode = ThroughputTask.Mode.ITEMS;
        }
    }
}
