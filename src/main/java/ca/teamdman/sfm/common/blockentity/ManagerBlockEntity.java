package ca.teamdman.sfm.common.blockentity;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.common.config.SFMConfig;
import ca.teamdman.sfm.common.containermenu.ManagerContainerMenu;
import ca.teamdman.sfm.common.handler.OpenContainerTracker;
import ca.teamdman.sfm.common.item.DiskItem;
import ca.teamdman.sfm.common.localization.LocalizationEntry;
import ca.teamdman.sfm.common.localization.LocalizationKeys;
import ca.teamdman.sfm.common.logging.TranslatableLogger;
import ca.teamdman.sfm.common.net.ClientboundManagerGuiUpdatePacket;
import ca.teamdman.sfm.common.net.ClientboundManagerLogLevelUpdatedPacket;
import ca.teamdman.sfm.common.net.ClientboundManagerLogsPacket;
import ca.teamdman.sfm.common.program.LabelPositionHolder;
import ca.teamdman.sfm.common.registry.SFMBlockEntities;
import ca.teamdman.sfm.common.registry.SFMPackets;
import ca.teamdman.sfm.common.util.SFMContainerUtil;
import ca.teamdman.sfml.ast.Program;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;

public class ManagerBlockEntity extends BaseContainerBlockEntity {
    public static final int TICK_TIME_HISTORY_SIZE = 20;
    public final TranslatableLogger logger;
    private final NonNullList<ItemStack> ITEMS = NonNullList.withSize(1, ItemStack.EMPTY);
    private final long[] tickTimeNanos = new long[TICK_TIME_HISTORY_SIZE];
    private @Nullable Program program = null;
    private int tick = 0;
    private int unprocessedRedstonePulses = 0; // used by redstone trigger
    private boolean shouldRebuildProgram = false;
    private boolean shouldRebuildProgramLock = false;
    private int tickIndex = 0;

    public ManagerBlockEntity(
            BlockPos blockPos,
            BlockState blockState
    ) {
        this(SFMBlockEntities.MANAGER_BLOCK_ENTITY.get(), blockPos, blockState);
    }

    public ManagerBlockEntity(
            BlockEntityType<?> pType,
            BlockPos blockPos,
            BlockState blockState
    ) {
        super(pType, blockPos, blockState);
        // Logger name should be unique to (isClient,managerpos)
        // We can't check isClient here, so instead to guarantee uniqueness we can just use hash
        // This is necessary because setLogLevel in game tests will get clobbered when the client constructs the block entity
        // so the name must be unique so that the client default logger construction doesn't overwrite changes to the server logger
        String loggerName = SFM.MOD_ID
                            + ":manager@"
                            + blockPos.toShortString() + "@" + Integer.toHexString(System.identityHashCode(this));
        logger = new TranslatableLogger(loggerName);
    }

    @Override
    public String toString() {
        return "ManagerBlockEntity{" +
               "hasDisk=" + (getDisk() != null) +
               '}';
    }

    /**
     * Used to prevent tests which modify configs from interfering with other tests.
     * <p>
     * When the manager detects a config change and rebuilds, it clobbers the monkey patching used by the tests.
     */
    public void enableRebuildProgramLock() {
        shouldRebuildProgramLock = true;
    }

    public static void serverTick(
            @SuppressWarnings("unused") Level level,
            @SuppressWarnings("unused") BlockPos pos,
            @SuppressWarnings("unused") BlockState state,
            ManagerBlockEntity manager
    ) {
        long start = System.nanoTime();
        manager.tick++;
        if (manager.program != null && manager.program.configRevision() != SFMConfig.SERVER.getRevision()) {
            manager.shouldRebuildProgram = true;
        }
        if (manager.shouldRebuildProgram && !manager.shouldRebuildProgramLock) {
            manager.rebuildProgramAndUpdateDisk();
            manager.shouldRebuildProgram = false;
        }
        if (manager.program != null) {
            boolean didSomething = manager.program.tick(manager);
            if (didSomething) {
                long nanoTimePassed = Long.min(System.nanoTime() - start, Integer.MAX_VALUE);
                manager.tickTimeNanos[manager.tickIndex] = (int) nanoTimePassed;
                manager.tickIndex = (manager.tickIndex + 1) % manager.tickTimeNanos.length;
                manager.logger.trace(x -> x.accept(LocalizationKeys.PROGRAM_TICK_TIME_MS.get(nanoTimePassed
                                                                                             / 1_000_000f)));
                manager.sendUpdatePacket();
                manager.logger.pruneSoWeDontEatAllTheRam();

                if (manager.logger.getLogLevel() == org.apache.logging.log4j.Level.TRACE
                    || manager.logger.getLogLevel() == org.apache.logging.log4j.Level.DEBUG
                    || manager.logger.getLogLevel() == org.apache.logging.log4j.Level.INFO) {
                    org.apache.logging.log4j.Level newLevel = org.apache.logging.log4j.Level.OFF;
                    manager.logger.info(x -> x.accept(LocalizationKeys.LOG_LEVEL_UPDATED.get(newLevel)));
                    var oldLevel = manager.logger.getLogLevel();
                    manager.setLogLevel(newLevel);
                    SFM.LOGGER.debug(
                            "SFM updated manager {} {} log level to {} after a single execution at {} level",
                            manager.getBlockPos(),
                            manager.getLevel(),
                            newLevel,
                            oldLevel
                    );
                }
            }
        }
    }

    public void setLogLevel(org.apache.logging.log4j.Level logLevelObj) {
        logger.setLogLevel(logLevelObj);
        sendUpdatePacket();
    }

    public int getTick() {
        return tick;
    }

    public @Nullable Program getProgram() {
        return program;
    }

    public void setProgram(String program) {
        var disk = getDisk();
        if (disk != null) {
            DiskItem.setProgram(disk, program);
            rebuildProgramAndUpdateDisk();
            setChanged();
        }
    }

    public void trackRedstonePulseUnprocessed() {
        unprocessedRedstonePulses++;
    }

    public void clearRedstonePulseQueue() {
        unprocessedRedstonePulses = 0;
    }

    public int getUnprocessedRedstonePulseCount() {
        return unprocessedRedstonePulses;
    }

    public State getState() {
        if (getDisk() == null) return State.NO_DISK;
        if (getProgramString() == null) return State.NO_PROGRAM;
        if (program == null) return State.INVALID_PROGRAM;
        return State.RUNNING;
    }

    public @Nullable String getProgramString() {
        var disk = getDisk();
        if (disk == null) {
            return null;
        }

        var program = DiskItem.getProgram(disk);
        return program.isBlank() ? null : program;
    }

    public String getProgramStringOrEmptyIfNull() {
        var programString = this.getProgramString();
        return programString == null ? "" : programString;
    }

    public Set<String> getReferencedLabels() {
        if (program == null) return Collections.emptySet();
        return program.referencedLabels();
    }

    public @Nullable ItemStack getDisk() {
        var item = getItem(0);
        if (item.getItem() instanceof DiskItem) return item;
        return null;
    }

    public void rebuildProgramAndUpdateDisk() {
        if (level != null && level.isClientSide()) return;
        var disk = getDisk();
        if (disk == null) {
            this.program = null;
        } else {
            this.program = DiskItem.compileAndUpdateErrorsAndWarnings(disk, this);
        }
        sendUpdatePacket();
    }

    @Override
    public int getContainerSize() {
        return ITEMS.size();
    }

    @Override
    public boolean isEmpty() {
        return ITEMS.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot < 0 || slot >= ITEMS.size()) return ItemStack.EMPTY;
        return ITEMS.get(slot);
    }

    @Override
    public ItemStack removeItem(
            int slot,
            int amount
    ) {
        var result = ContainerHelper.removeItem(ITEMS, slot, amount);
        if (slot == 0) rebuildProgramAndUpdateDisk();
        setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        var result = ContainerHelper.takeItem(ITEMS, slot);
        if (slot == 0) rebuildProgramAndUpdateDisk();
        setChanged();
        return result;
    }

    @Override
    public void setItem(
            int slot,
            ItemStack stack
    ) {
        if (slot < 0 || slot >= ITEMS.size()) return;
        ITEMS.set(slot, stack);
        if (slot == 0) rebuildProgramAndUpdateDisk();
        setChanged();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(
            int slot,
            ItemStack stack
    ) {
        return stack.getItem() instanceof DiskItem;
    }

    @Override
    public boolean stillValid(Player player) {
        return SFMContainerUtil.stillValid(this, player);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ContainerHelper.loadAllItems(tag, ITEMS);
        this.shouldRebuildProgram = true;
        if (level != null) {
            this.tick = level.random.nextInt();
        }
    }

    @Override
    public void clearContent() {
        ITEMS.clear();
    }

    public void reset() {
        var disk = getDisk();
        if (disk != null) {
            LabelPositionHolder.purge(disk);
            disk.setTag(null);
            setItem(0, disk);
            setChanged();
        }
    }

    public long[] getTickTimeNanos() {
        // tickTimeNanos is used as a cyclical buffer, transform it to have the first index be the most recent tick
        long[] result = new long[tickTimeNanos.length];
        System.arraycopy(tickTimeNanos, tickIndex, result, 0, tickTimeNanos.length - tickIndex);
        System.arraycopy(tickTimeNanos, 0, result, tickTimeNanos.length - tickIndex, tickIndex);
        return result;
    }

    public void sendUpdatePacket() {
        // Create one packet and clone it for each receiver
        var managerUpdatePacket = new ClientboundManagerGuiUpdatePacket(
                -1,
                getProgramStringOrEmptyIfNull(),
                getState(),
                getTickTimeNanos()
        );

        OpenContainerTracker.getOpenManagerMenus(getBlockPos())
                .forEach(entry -> {
                    ManagerContainerMenu menu = entry.getValue();

                    // Send a copy of the manager update packet
                    SFMPackets.sendToPlayer(entry::getKey, managerUpdatePacket.cloneWithWindowId(menu.containerId));

                    // The rest of the sync is only relevant if the log screen is open
                    if (!menu.isLogScreenOpen) return;

                    // Send log level changes
                    if (!menu.logLevel.equals(logger.getLogLevel().name())) {
                        SFMPackets.sendToPlayer(entry::getKey, new ClientboundManagerLogLevelUpdatedPacket(
                                menu.containerId,
                                logger.getLogLevel().name()
                        ));
                        menu.logLevel = logger.getLogLevel().name();
                    }

                    // Send new logs
                    MutableInstant hasSince = new MutableInstant();
                    if (!menu.logs.isEmpty()) {
                        hasSince.initFrom(menu.logs.getLast().instant());
                    }
                    var logsToSend = logger.getLogsAfter(hasSince);
                    if (!logsToSend.isEmpty()) {
                        // Add the latest entry to the server copy
                        // since the server copy is only used for checking what the latest log timestamp is
                        menu.logs.add(logsToSend.getLast());

                        // Send the logs
                        while (!logsToSend.isEmpty()) {
                            int remaining = logsToSend.size();
                            SFMPackets.sendToPlayer(entry::getKey, ClientboundManagerLogsPacket.drainToCreate(
                                    menu.containerId,
                                    logsToSend
                            ));
                            if (logsToSend.size() >= remaining) {
                                throw new IllegalStateException("Failed to send logs, infinite loop detected");
                            }
                        }
                    }
                });
    }

    @Override
    protected Component getDefaultName() {
        return LocalizationKeys.MANAGER_CONTAINER.getComponent();
    }

    @Override
    protected AbstractContainerMenu createMenu(
            int windowId,
            Inventory inv
    ) {
        return new ManagerContainerMenu(windowId, inv, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, ITEMS);
    }

    public enum State {
        NO_PROGRAM(
                ChatFormatting.RED,
                LocalizationKeys.MANAGER_GUI_STATE_NO_PROGRAM
        ), NO_DISK(
                ChatFormatting.RED,
                LocalizationKeys.MANAGER_GUI_STATE_NO_DISK
        ), RUNNING(ChatFormatting.GREEN, LocalizationKeys.MANAGER_GUI_STATE_RUNNING), INVALID_PROGRAM(
                ChatFormatting.DARK_RED,
                LocalizationKeys.MANAGER_GUI_STATE_INVALID_PROGRAM
        );

        public final ChatFormatting COLOR;
        public final LocalizationEntry LOC;

        State(
                ChatFormatting color,
                LocalizationEntry loc
        ) {
            COLOR = color;
            LOC = loc;
        }
    }

}
