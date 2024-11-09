package ca.teamdman.sfm.common.watertanknetwork;

import ca.teamdman.sfm.common.block.WaterTankBlock;
import ca.teamdman.sfm.common.blockentity.WaterTankBlockEntity;
import ca.teamdman.sfm.common.util.SFMDirections;
import ca.teamdman.sfm.common.util.SFMUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public record WaterNetwork(
        Level level,
        Long2ObjectOpenHashMap<WaterTankBlockEntity> members,
        Long2ObjectOpenHashMap<LongArraySet> chunkMemberLookup
) {
    public WaterNetwork(
            Level level
    ) {
        this(level, new Long2ObjectOpenHashMap<>(), new Long2ObjectOpenHashMap<>());
    }

    public void rebuildNetwork(BlockPos start) {
        members.clear();
        discoverMembers(start).forEach(this::addMember);
        updateMembers();
    }

    public @Nullable WaterTankBlockEntity getMember(BlockPos memberPos) {
        return members.get(memberPos.asLong());
    }

    public void addMember(BlockPos pos) {
        addMember((WaterTankBlockEntity) level.getBlockEntity(pos));
    }

    public void addMember(WaterTankBlockEntity blockEntity) {
        members.put(blockEntity.getBlockPos().asLong(), blockEntity);
        blockEntity.setActive(true);
    }

    public void updateMembers() {
        int size = members.size();
        for (WaterTankBlockEntity member : members().values()) {
            member.setConnectedCount(size);
        }
    }

    public Stream<WaterTankBlockEntity> discoverMembers(BlockPos start) {
        return SFMUtils.getRecursiveStream((current, next, results) -> {
            if (!(level.getBlockEntity(current) instanceof WaterTankBlockEntity blockEntity)) return;
            if (!current.equals(start)) {
                BlockState blockState = level.getBlockState(current);
                if (!blockState.getOptionalValue(WaterTankBlock.IN_WATER).orElse(false)) return;
            }
            results.accept(blockEntity);
            for (Direction d : SFMDirections.DIRECTIONS) {
                next.accept(current.offset(d.getNormal()));
            }
        }, start);
    }

    public Stream<WaterTankBlockEntity> discoverMembersFromCache(
            BlockPos start,
            WaterNetwork cache
    ) {
        return SFMUtils.getRecursiveStream((current, next, results) -> {
            WaterTankBlockEntity blockEntity = cache.members.get(current.asLong());
            if (blockEntity == null) return;
            results.accept(blockEntity);
            for (Direction d : SFMDirections.DIRECTIONS) {
                next.accept(current.offset(d.getNormal()));
            }
        }, start);
    }

    public void purgeChunk(ChunkAccess chunkAccess) {
        long chunkKey = chunkAccess.getPos().toLong();
        LongArraySet memberPositions = chunkMemberLookup.get(chunkKey);
        if (memberPositions == null) return;
        members.keySet().removeAll(memberPositions);
        chunkMemberLookup.remove(chunkKey);
    }

    private void rebuildNetworkFromCache(
            BlockPos start,
            WaterNetwork cache
    ) {
        members.clear();
        discoverMembersFromCache(start, cache).forEach(this::addMember);
    }

    void mergeNetwork(WaterNetwork other) {
        members.putAll(other.members);
    }

    List<WaterNetwork> withoutMember(BlockPos pos) {
        members.remove(pos.asLong());
        List<WaterNetwork> branches = new ArrayList<>();
        for (Direction direction : SFMDirections.DIRECTIONS) {
            BlockPos offset = pos.offset(direction.getNormal());
            if (!members.containsKey(offset.asLong())) continue;
            if (branches.stream().anyMatch(branch -> branch.members.containsKey(offset.asLong()))) continue;
            WaterNetwork branch = new WaterNetwork(level);
            branch.rebuildNetworkFromCache(offset, this);
            branches.add(branch);
        }
        return branches;
    }
}
