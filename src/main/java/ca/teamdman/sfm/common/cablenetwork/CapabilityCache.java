package ca.teamdman.sfm.common.cablenetwork;

import ca.teamdman.sfm.common.localization.LocalizationKeys;
import ca.teamdman.sfm.common.logging.TranslatableLogger;
import ca.teamdman.sfm.common.registry.SFMCapabilityProviderMappers;
import ca.teamdman.sfm.common.util.SFMDirections;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class CapabilityCache {
    // Position => Capability => Direction => LazyOptional
    // We don't use an EnumMap here for Direction because we need to support the null key
    private final Long2ObjectMap<Object2ObjectOpenHashMap<Capability<?>, SFMDirections.NullableDirectionEnumMap<LazyOptional<?>>>> CACHE = new Long2ObjectOpenHashMap<>();
    // Chunk position => Set of Block positions
    private final Long2ObjectMap<LongArraySet> CHUNK_TO_BLOCK_POSITIONS = new Long2ObjectOpenHashMap<>();

    public void clear() {
        CACHE.clear();
        CHUNK_TO_BLOCK_POSITIONS.clear();
    }

    public int size() {
        return CACHE.values().stream().flatMap(x -> x.values().stream()).mapToInt(SFMDirections.NullableDirectionEnumMap::size).sum();
    }

    public void overwriteFromOther(BlockPos pos, CapabilityCache other) {
        var found = other.CACHE.get(pos.asLong());
        if (found != null) {
            CACHE.put(pos.asLong(), new Object2ObjectOpenHashMap<>(found));
        }
        addToChunkMap(pos);
    }

    public <CAP> @Nullable LazyOptional<CAP> getCapability(
            BlockPos pos,
            Capability<CAP> capKind,
            @Nullable Direction direction
    ) {
        var capMap = CACHE.get(pos.asLong());
        if (capMap != null) {
            var dirMap = capMap.get(capKind);
            if (dirMap != null) {
                var found = dirMap.get(direction);
                if (found != null) {
                    //noinspection unchecked
                    return (LazyOptional<CAP>) found;
                }

            }
        }
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void putAll(CapabilityCache other) {
        for (var entry : other.CACHE.long2ObjectEntrySet()) {
            long pos = entry.getLongKey();

            var capMap = entry.getValue();
            for (var e : capMap.entrySet()) {
                Capability<?> capKind = e.getKey();

                var dirMap = e.getValue();
                for (var direction : Direction.values()) {
                    LazyOptional<?> cap = dirMap.get(direction);
                    if (cap != null) {
                        putCapability(BlockPos.of(pos), (Capability) capKind, direction, cap);
                    }
                }
            }
        }
    }

    public Stream<BlockPos> getPositions() {
        return CACHE.keySet().longStream().mapToObj(BlockPos::of);
    }

    public <CAP> LazyOptional<CAP> getOrDiscoverCapability(
            Level level,
            BlockPos pos,
            Capability<CAP> capKind,
            @Nullable Direction direction,
            TranslatableLogger logger
    ) {
        // Check cache
        var found = getCapability(pos, capKind, direction);
        if (found != null) {
            if (found.isPresent()) {
                logger.trace(x -> x.accept(LocalizationKeys.LOG_CAPABILITY_CACHE_HIT.get(pos, capKind.getName(), direction)));
                return found;
            } else {
                logger.error(x -> x.accept(LocalizationKeys.LOG_CAPABILITY_CACHE_HIT_INVALID.get(pos, capKind.getName(), direction)));
            }
        } else {
            logger.trace(x -> x.accept(LocalizationKeys.LOG_CAPABILITY_CACHE_MISS.get(pos, capKind.getName(), direction)));
        }

        // No capability found, discover it
        var provider = SFMCapabilityProviderMappers.discoverCapabilityProvider(level, pos);
        if (provider != null) {
            var lazyOptional = provider.getCapability(capKind, direction);
            if (lazyOptional.isPresent()) {
                putCapability(pos, capKind, direction, lazyOptional);
                lazyOptional.addListener(x -> remove(pos, capKind, direction));
            } else {
                logger.warn(x -> x.accept(LocalizationKeys.LOGS_EMPTY_CAPABILITY.get(pos, capKind.getName(), direction)));
            }
            return lazyOptional;
        } else {
            logger.warn(x -> x.accept(LocalizationKeys.LOGS_MISSING_CAPABILITY_PROVIDER.get(pos, capKind.getName(), direction)));
            return LazyOptional.empty();
        }
    }

    public void remove(
            BlockPos pos,
            Capability<?> capKind,
            @Nullable Direction direction
    ) {
        var capMap = CACHE.get(pos.asLong());
        if (capMap != null) {
            var dirMap = capMap.get(capKind);
            if (dirMap != null) {
                dirMap.remove(direction);
                if (dirMap.isEmpty()) {
                    capMap.remove(capKind);
                    if (capMap.isEmpty()) {
                        CACHE.remove(pos.asLong());
                    }
                }
                removeFromChunkMap(pos);
            }
        }
    }

    public <CAP> void putCapability(
            BlockPos pos,
            Capability<CAP> capKind,
            @Nullable Direction direction,
            LazyOptional<CAP> cap
    ) {
        var capMap = CACHE.computeIfAbsent(pos.asLong(), k -> new Object2ObjectOpenHashMap<>());
        var dirMap = capMap.computeIfAbsent(capKind, k -> new SFMDirections.NullableDirectionEnumMap<>());
        dirMap.put(direction, cap);
        addToChunkMap(pos);
    }

    public void bustCacheForChunk(ChunkAccess chunkAccess) {
        long chunkKey = chunkAccess.getPos().toLong();
        LongArraySet blockPositions = CHUNK_TO_BLOCK_POSITIONS.get(chunkKey);
        if (blockPositions != null) {
            for (var blockPos : blockPositions) {
                CACHE.remove(blockPos.longValue());
            }
            CHUNK_TO_BLOCK_POSITIONS.remove(chunkKey);
        }
    }

    private void addToChunkMap(BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        long chunkKey = chunkPos.toLong();
        long blockPos = pos.asLong();
        CHUNK_TO_BLOCK_POSITIONS.computeIfAbsent(chunkKey, k -> new LongArraySet()).add(blockPos);
    }

    private void removeFromChunkMap(BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        long chunkKey = chunkPos.toLong();
        long blockPos = pos.asLong();
        LongArraySet blockPosSet = CHUNK_TO_BLOCK_POSITIONS.get(chunkKey);
        if (blockPosSet != null) {
            blockPosSet.remove(blockPos);
            if (blockPosSet.isEmpty()) {
                CHUNK_TO_BLOCK_POSITIONS.remove(chunkKey);
            }
        }
    }
}
