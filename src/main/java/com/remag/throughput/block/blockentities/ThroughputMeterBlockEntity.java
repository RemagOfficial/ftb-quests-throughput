package com.remag.throughput.block.blockentities;

import com.remag.throughput.packets.UpdateMeterConfigPacket;
import com.remag.throughput.registry.ModBlockEntities;
import com.remag.throughput.tasks.ThroughputTask;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftbquests.api.FTBQuestsAPI;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.util.ConfigQuestObject;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EmptyEnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.EmptyFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.text.NumberFormat;
import java.util.Arrays;

public class ThroughputMeterBlockEntity extends BlockEntity {

    // -------------------------------------------------------------
    //  TASK + MODE
    // -------------------------------------------------------------
    private long taskId = 0;
    private Task cachedTask = null;
    private ThroughputTask.Mode mode = ThroughputTask.Mode.NONE;

    // -------------------------------------------------------------
    //  STATS / ROLLING WINDOW
    // -------------------------------------------------------------
    private static final int MAX_WINDOW = 400; // 20 seconds max, tweak if you want

    // actual window size this meter is using (copied from the task)
    private int windowSize = 100;

    // ring buffer, always MAX_WINDOW sized; we only use [0, windowSize)
    private final long[] sampleWindow = new long[MAX_WINDOW];
    private int sampleIndex = 0;
    private int questUpdateTimer = 0;

    // Item stats
    private long itemsMovedThisTick = 0;
    private long itemsPerSecond = 0;

    // Fluid stats
    private long fluidsMovedThisTick = 0;
    private long milliBucketsPerSecond = 0;

    // Energy stats
    private long energyMovedThisTick = 0;
    private long energyPerSecond = 0;

    private boolean editingConfig = false;

    // -------------------------------------------------------------
    //  INTERNAL BUFFER (1 slot)
    // -------------------------------------------------------------
    private final ItemStackHandler buffer = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            if (!editingConfig) setChanged();
        }
    };

    private final FluidTank fluidBuffer = new FluidTank(16_000) { // 16 buckets buffer
        @Override
        protected void onContentsChanged() {
            if (!editingConfig) setChanged();
        }
    };


    // -------------------------------------------------------------
    //  MEASURING ITEM HANDLER
    // -------------------------------------------------------------
    private final IItemHandler measuringItemHandler = new IItemHandler() {

        @Override
        public int getSlots() { return 1; }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return buffer.getStackInSlot(0);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {

            // Reject if no task or wrong type
            if (mode != ThroughputTask.Mode.ITEMS || getTask() == null)
                return stack;

            return buffer.insertItem(0, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack out = buffer.extractItem(0, amount, simulate);

            if (!simulate && !out.isEmpty()) {
                itemsMovedThisTick += out.getCount();
            }

            return out;
        }

        @Override
        public int getSlotLimit(int slot) { return 64; }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) { return true; }
    };

    private static final IItemHandler EMPTY_HANDLER = new IItemHandler() {
        @Override public int getSlots() { return 0; }
        @Override public ItemStack getStackInSlot(int slot) { return ItemStack.EMPTY; }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return stack; }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return 64; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return false; }
    };

    private final IFluidHandler measuringFluidHandler = new IFluidHandler() {

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return fluidBuffer.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return fluidBuffer.getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return true;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            // Reject unless fluid mode
            if (mode != ThroughputTask.Mode.FLUIDS || getTask() == null)
                return 0;

            return fluidBuffer.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            FluidStack drained = fluidBuffer.drain(resource, action);

            if (action == FluidAction.EXECUTE && !drained.isEmpty()) {
                fluidsMovedThisTick += drained.getAmount();
            }

            return drained;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            FluidStack drained = fluidBuffer.drain(maxDrain, action);

            if (action == FluidAction.EXECUTE && !drained.isEmpty()) {
                fluidsMovedThisTick += drained.getAmount();
            }

            return drained;
        }
    };

    private final IEnergyStorage measuringEnergyStorage = new IEnergyStorage() {

        private static final int MAX_TRANSFER = Integer.MAX_VALUE;

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (mode != ThroughputTask.Mode.ENERGY || getTask() == null)
                return 0;

            if (!simulate) {
                energyMovedThisTick += maxReceive;
            }

            return maxReceive; // accept everything
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0; // never provide energy
        }

        @Override
        public int getEnergyStored() {
            return 0;
        }

        @Override
        public int getMaxEnergyStored() {
            return MAX_TRANSFER;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    };

    // -------------------------------------------------------------
    //  CONSTRUCTOR
    // -------------------------------------------------------------
    public ThroughputMeterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.THROUGHPUT_METER.get(), pos, state);
    }

    // -------------------------------------------------------------
    //  CONFIG SCREEN
    // -------------------------------------------------------------
    public ConfigGroup createConfigGroup(Player player) {
        editingConfig = true;

        Task tsk = getTask();
        TeamData teamData = findTeamData();

        if (tsk instanceof ThroughputTask thr) {
            if (teamData != null && teamData.isCompleted(thr)) {
                // task already completed → reset to NONE
                this.taskId = 0L;
                this.cachedTask = null;
                this.mode = ThroughputTask.Mode.NONE;
                setChanged();
            }
        }

        ConfigGroup cg0 = new ConfigGroup("throughput_meter", accepted -> {
            editingConfig = false;

            if (accepted) {
                UpdateMeterConfigPacket.send(
                        getBlockPos(),
                        taskId,
                        mode
                );
            }
        });

        cg0.setNameKey("throughput.throughput");

        ConfigGroup cg = cg0.getOrCreateSubgroup("settings");

        // TASK SELECTOR
        cg.add(
                "task",
                new ConfigQuestObject<>(
                        o -> {
                            // must be a throughput task
                            if (!(o instanceof ThroughputTask t)) return false;

                            // must not be completed
                            TeamData data = findTeamData();
                            return data == null || !data.isCompleted(t);
                        },
                        o -> formatLine(o, player)
                ),
                getTask(),
                obj -> {
                    if (obj instanceof Task t) {
                        this.taskId = t.id;
                        this.cachedTask = t;

                        if (t instanceof ThroughputTask thr) {
                            // auto mode selection
                            this.mode = thr.getMode();

                            // COPY the window from the task into this block entity
                            int w = thr.getWindow();
                            this.windowSize = Math.max(1, Math.min(w, MAX_WINDOW));

                            // reset samples when changing task/window
                            Arrays.fill(this.sampleWindow, 0L);
                            this.sampleIndex = 0;
                        }
                    } else {
                        this.taskId = 0L;
                        this.cachedTask = null;
                        this.mode = ThroughputTask.Mode.NONE;

                        // optionally reset window too
                        this.windowSize = 100;
                        Arrays.fill(this.sampleWindow, 0L);
                        this.sampleIndex = 0;
                    }

                    setChanged();

                    if (level != null) {
                        level.invalidateCapabilities(worldPosition);
                    }
                },
                null
        ).setNameKey("throughput.task");

        return cg0;
    }

    private Component formatLine(Task task, Player player) {
        if (task == null) return Component.empty();

        Component questTxt = Component.literal(" [")
                .append(task.getQuest().getTitle())
                .append("]")
                .withStyle(ChatFormatting.GREEN);

        return ConfigQuestObject.formatEntry(task).copy().append(questTxt);
    }

    // -------------------------------------------------------------
    //  TASK LOOKUP
    // -------------------------------------------------------------
    public Task getTask() {
        if (level == null || taskId == 0) return null;

        cachedTask = FTBQuestsAPI.api()
                .getQuestFile(level.isClientSide)
                .getTask(taskId);

        return cachedTask;
    }


    public void setTask(long id, ThroughputTask.Mode newMode) {
        this.taskId = id;
        this.cachedTask = null;
        this.mode = newMode;
        setChanged();

        if (level != null) {
            level.invalidateCapabilities(worldPosition);
        }
    }

    // -------------------------------------------------------------
    //  SERVER TICK
    // -------------------------------------------------------------
    public static void serverTick(Level level, BlockPos pos, BlockState state, ThroughputMeterBlockEntity be) {
        be.tickServer();
    }

    public void tickServer() {
        if (level == null || level.isClientSide) return;

        // Rolling window
        int w = Mth.clamp(windowSize, 1, MAX_WINDOW);

        // store this tick's count into the ring buffer
        long movedThisTick = switch (mode) {
            case ITEMS  -> itemsMovedThisTick;
            case FLUIDS -> fluidsMovedThisTick;
            case ENERGY -> energyMovedThisTick;
            default     -> 0;
        };

        sampleWindow[sampleIndex] = movedThisTick;
        sampleIndex = (sampleIndex + 1) % w;

        itemsMovedThisTick = 0;
        fluidsMovedThisTick = 0;
        energyMovedThisTick = 0;

        // compute moving average over [0, w)
        long sum = 0;
        for (int i = 0; i < w; i++) {
            sum += sampleWindow[i];
        }

        // average ticks → items per second
        switch (mode) {
            case ITEMS  -> itemsPerSecond = (sum * 20L) / w;
            case FLUIDS -> milliBucketsPerSecond = (sum * 20L) / w;
            case ENERGY -> energyPerSecond = (sum * 20L) / w;
        }

        questUpdateTimer++;
        if (questUpdateTimer >= 20) {
            questUpdateTimer = 0;
            pushProgressToQuest();
        }

        Task task = getTask();
        TeamData data = findTeamData();

        if (task instanceof ThroughputTask thr) {
            if (data != null && data.isCompleted(thr)) {
                // reset if completed
                taskId = 0L;
                cachedTask = null;
                mode = ThroughputTask.Mode.NONE;
                setChanged();
            }
        }

        // Sync once per window if not editing config
        if (!editingConfig && sampleIndex == 0) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void pushProgressToQuest() {
        Task t = getTask();
        if (!(t instanceof ThroughputTask thr)) return;

        TeamData data = findTeamData();
        if (data == null) return;

        if (mode == ThroughputTask.Mode.ITEMS) {
            thr.submit(data, itemsPerSecond);
        } else if (mode == ThroughputTask.Mode.FLUIDS) {
            thr.submit(data, milliBucketsPerSecond);
        } else if (mode == ThroughputTask.Mode.ENERGY) {
            thr.submit(data, energyPerSecond);
        }
    }

    @Nullable
    private TeamData findTeamData() {
        if (level == null) return null;

        // Same radius Task Screens use (~32 block cube)
        final double maxDistSq = 32 * 32;

        for (Player p : level.players()) {
            double d = p.distanceToSqr(
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.5,
                    worldPosition.getZ() + 0.5
            );

            if (d <= maxDistSq) {
                return TeamData.get(p);
            }
        }

        return null;
    }

    // -------------------------------------------------------------
    //  CAPABILITY EXPOSURE
    // -------------------------------------------------------------
    public IItemHandler getItemHandler(Direction side) {
        if (mode == ThroughputTask.Mode.ITEMS && getTask() != null)
            return measuringItemHandler;

        return EMPTY_HANDLER;
    }

    public IFluidHandler getFluidHandler(Direction side) {
        if (mode == ThroughputTask.Mode.FLUIDS && getTask() != null)
            return measuringFluidHandler;

        return EmptyFluidHandler.INSTANCE;
    }

    public IEnergyStorage getEnergyStorage(Direction side) {
        if (mode == ThroughputTask.Mode.ENERGY && getTask() != null)
            return measuringEnergyStorage;

        return EmptyEnergyStorage.INSTANCE;
    }
    // -------------------------------------------------------------
    //  RENDER TEXT
    // -------------------------------------------------------------

    private static final NumberFormat INT_FORMAT = NumberFormat.getIntegerInstance();

    public String getTaskNameForRender() {
        Task t = getTask();
        return t == null ? "No Task" : t.getTitle().getString();
    }

    public String getThroughputDisplay() {
        return switch (mode) {
            case ITEMS  -> INT_FORMAT.format(itemsPerSecond) + " items/s";
            case FLUIDS -> INT_FORMAT.format(milliBucketsPerSecond) + " mB/s";
            case ENERGY -> INT_FORMAT.format(energyPerSecond) + " FE/s";
            default -> "";
        };
    }

    public String getProgressDisplay() {
        Task t = getTask();
        if (!(t instanceof ThroughputTask thr)) return "";

        TeamData data = findTeamData();
        if (data == null) return "0 / " + thr.getSustain();

        long value = data.getProgress(t);
        long max = thr.getSustain();

        return value + " / " + max;
    }

    public float getProgressPercent() {
        Task t = getTask();
        if (!(t instanceof ThroughputTask thr)) return 0F;

        TeamData data = findTeamData();
        if (data == null) return 0F;

        float pct = (float) data.getProgress(t) / thr.getSustain();
        return Mth.clamp(pct, 0F, 1F);
    }

    // -------------------------------------------------------------
    //  NBT SAVE/LOAD
    // -------------------------------------------------------------
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);

        tag.putLong("TaskId", taskId);
        tag.putString("Mode", mode.name());
        tag.putInt("WindowSize", windowSize);
        tag.put("Buffer", buffer.serializeNBT(provider));
        tag.put("FluidBuffer", fluidBuffer.writeToNBT(provider, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);

        taskId = tag.getLong("TaskId");
        cachedTask = null;

        try {
            mode = ThroughputTask.Mode.valueOf(tag.getString("Mode"));
        } catch (Exception ignored) {
            mode = ThroughputTask.Mode.NONE;
        }

        windowSize = tag.contains("WindowSize")
                ? Mth.clamp(tag.getInt("WindowSize"), 1, MAX_WINDOW)
                : 100; // fallback

        if (tag.contains("Buffer"))
            buffer.deserializeNBT(provider, tag.getCompound("Buffer"));

        if (tag.contains("FluidBuffer"))
            fluidBuffer.readFromNBT(provider, tag.getCompound("FluidBuffer"));
    }

    // -------------------------------------------------------------
    //  SYNC PACKETS
    // -------------------------------------------------------------
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider provider) {
        handleUpdateTag(pkt.getTag(), provider);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = super.getUpdateTag(provider);

        tag.putLong("TaskId", taskId);
        tag.putString("Mode", mode.name());
        if (mode == ThroughputTask.Mode.ITEMS) {
            tag.putLong("ItemsPerSec", itemsPerSecond);
        } else if (mode == ThroughputTask.Mode.FLUIDS) {
            tag.putLong("FluidPerSec", milliBucketsPerSecond);
        } else if (mode == ThroughputTask.Mode.ENERGY) {
            tag.putLong("EnergyPerSec", energyPerSecond);
        }

        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        super.handleUpdateTag(tag, provider);

        this.taskId = tag.getLong("TaskId");
        this.cachedTask = null;

        try {
            this.mode = ThroughputTask.Mode.valueOf(tag.getString("Mode"));
        } catch (Exception ignored) {
            this.mode = ThroughputTask.Mode.NONE;
        }

        if (tag.contains("ItemsPerSec")) {
            this.itemsPerSecond = tag.getLong("ItemsPerSec");
        }

        if (tag.contains("FluidPerSec")) {
            this.milliBucketsPerSecond = tag.getLong("FluidPerSec");
        }

        if (tag.contains("EnergyPerSec")) {
            this.energyPerSecond = tag.getLong("EnergyPerSec");
        }
    }
}
