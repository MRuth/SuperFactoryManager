package ca.teamdman.sfm.common.cablenetwork;

import ca.teamdman.sfm.common.localization.LocalizationKeys;
import ca.teamdman.sfm.common.logging.TranslatableLogger;
import ca.teamdman.sfm.common.util.NotStored;
import ca.teamdman.sfm.common.util.SFMDirections;
import ca.teamdman.sfm.common.util.SFMStreamUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class CableNetwork {

    protected final Level LEVEL;
    protected final LongSet CABLE_POSITIONS = new LongOpenHashSet();
    protected final CapabilityCache CAPABILITY_CACHE = new CapabilityCache();

    public CableNetwork(Level level) {
        this.LEVEL = level;
    }

    /**
     * Only cable blocks are valid network members
     */
    public static boolean isCable(
            @Nullable Level world,
            @NotStored BlockPos cablePos
    ) {
        if (world == null) return false;
        return world
                .getBlockState(cablePos)
                .getBlock() instanceof ICableBlock;
    }

    public void rebuildNetwork(@NotStored BlockPos start) {
        CABLE_POSITIONS.clear();
        CAPABILITY_CACHE.clear();
        discoverCables(getLevel(), start).forEach(this::addCable);
    }

    public void rebuildNetworkFromCache(
            @NotStored BlockPos start,
            CableNetwork other
    ) {
        CABLE_POSITIONS.clear();
        CAPABILITY_CACHE.clear();

        // discover connected cables
        var cables = SFMStreamUtils.<BlockPos, BlockPos>getRecursiveStream((current, next, results) -> {
            results.accept(current);
            BlockPos.MutableBlockPos target = new BlockPos.MutableBlockPos();
            for (Direction d : SFMDirections.DIRECTIONS) {
                target.set(current).move(d);
                if (other.containsCablePosition(target)) {
                    next.accept(target.immutable());
                }
            }
        }, start).toList();
        for (BlockPos cablePos : cables) {
            CABLE_POSITIONS.add(cablePos.asLong());
        }

        // discover capabilities
        cables
                .stream()
                .flatMap(cablePos -> Arrays
                        .stream(SFMDirections.DIRECTIONS)
                        .map(Direction::getNormal)
                        .map(cablePos::offset))
                .distinct()
                .forEach(pos -> CAPABILITY_CACHE.overwriteFromOther(pos, other.CAPABILITY_CACHE));
    }

    public static Stream<BlockPos> discoverCables(
            Level level,
            @NotStored BlockPos startPos
    ) {
        return SFMStreamUtils.getRecursiveStream((current, next, results) -> {
            results.accept(current);
            BlockPos.MutableBlockPos target = new BlockPos.MutableBlockPos();
            for (Direction d : SFMDirections.DIRECTIONS) {
                target.set(current).move(d);
                if (isCable(level, target)) {
                    next.accept(target.immutable());
                }
            }
        }, startPos);
    }

    public void addCable(@NotStored BlockPos pos) {
        CABLE_POSITIONS.add(pos.asLong());
    }

    public Level getLevel() {
        return LEVEL;
    }

    @Override
    public String toString() {
        return "CableNetwork{level="
               + getLevel().dimension().location()
               + ", #cables="
               + getCableCount()
               + ", #cache="
               + CAPABILITY_CACHE.size()
               + "}";
    }

    /**
     * Cables should only join the network if they would be touching a cable already in the network
     *
     * @param pos Candidate cable position
     * @return {@code true} if adjacent to cable in network
     */
    public boolean isAdjacentToCable(@NotStored BlockPos pos) {
        BlockPos.MutableBlockPos target = new BlockPos.MutableBlockPos();
        for (Direction direction : SFMDirections.DIRECTIONS) {
            target.set(pos).move(direction);
            if (containsCablePosition(target)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsCablePosition(@NotStored BlockPos pos) {
        return CABLE_POSITIONS.contains(pos.asLong());
    }

    public <CAP> @NotNull LazyOptional<CAP> getCapability(
            Capability<CAP> cap,
            @NotStored BlockPos pos,
            @Nullable Direction direction,
            TranslatableLogger logger
    ) {
        // any BlockPos can have labels assigned
        // we must only proceed here if there is an adjacent cable from this network
        if (!isAdjacentToCable(pos)) {
            logger.warn(x -> x.accept(LocalizationKeys.LOGS_MISSING_ADJACENT_CABLE.get(pos)));
            return LazyOptional.empty();
        }
        return CAPABILITY_CACHE.getOrDiscoverCapability(LEVEL, pos, cap, direction, logger);
    }

    public int getCableCount() {
        return CABLE_POSITIONS.size();
    }

    /**
     * Merges a network into this one, such as when a cable connects two networks
     *
     * @param other Foreign network
     */
    public void mergeNetwork(CableNetwork other) {
        CABLE_POSITIONS.addAll(other.CABLE_POSITIONS);
        CAPABILITY_CACHE.putAll(other.CAPABILITY_CACHE);
    }

    public boolean isEmpty() {
        return CABLE_POSITIONS.isEmpty();
    }

    public Stream<BlockPos> getCablePositions() {
        return CABLE_POSITIONS.longStream().mapToObj(BlockPos::of);
    }

    public LongSet getCablePositionsRaw() {
        return CABLE_POSITIONS;
    }

    public Stream<BlockPos> getCapabilityProviderPositions() {
        return CAPABILITY_CACHE.getPositions();
    }

    public void bustCacheForChunk(ChunkAccess chunkAccess) {
        CAPABILITY_CACHE.bustCacheForChunk(chunkAccess);
    }

    /**
     * Discover what networks would exist if this network did not have a cable at {@code cablePos}.
     *
     * @param cablePos cable position to be removed
     * @return resulting networks to replace this network
     */
    protected List<CableNetwork> withoutCable(@NotStored BlockPos cablePos) {
        CABLE_POSITIONS.remove(cablePos.asLong());
        List<CableNetwork> branches = new ArrayList<>();
        BlockPos.MutableBlockPos target = new BlockPos.MutableBlockPos();
        for (Direction direction : SFMDirections.DIRECTIONS) {
            target.set(cablePos).move(direction);
            if (!containsCablePosition(target)) continue;
            // make sure that a branch network doesn't already contain this cable
            if (branches.stream().anyMatch(n -> n.containsCablePosition(target))) continue;
            var branchNetwork = new CableNetwork(this.getLevel());
            branchNetwork.rebuildNetworkFromCache(target, this);
            branches.add(branchNetwork);
        }
        return branches;
    }
}
