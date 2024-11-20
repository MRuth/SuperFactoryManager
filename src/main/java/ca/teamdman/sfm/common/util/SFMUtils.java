package ca.teamdman.sfm.common.util;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.common.capabilityprovidermapper.CapabilityProviderMapper;
import ca.teamdman.sfm.common.program.LimitedInputSlot;
import ca.teamdman.sfm.common.registry.SFMCapabilityProviderMappers;
import ca.teamdman.sfm.common.registry.SFMResourceTypes;
import ca.teamdman.sfm.common.resourcetype.ResourceType;
import ca.teamdman.sfml.ast.Number;
import ca.teamdman.sfml.ast.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.capabilities.CapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class SFMUtils {

    public static final int MAX_TRANSLATION_ELEMENT_LENGTH = 10240;

    /**
     * Gets a stream using a self-feeding mapping function. Prevents the
     * re-traversal of elements that have been visited before.
     *
     * @param operator Consumes queue elements to build the result set and
     *                 append the next queue elements
     * @param first    Initial value, not checked against the filter
     * @param <T>      Type that the mapper consumes and produces
     * @return Stream result after termination of the recursive mapping process
     */
    public static <T, R> Stream<R> getRecursiveStream(
            RecursiveBuilder<T, R> operator,
            T first
    ) {
        Set<T> visitDebounce = new HashSet<>();
        Deque<T> toVisit = new ArrayDeque<>();
        toVisit.add(first);
        visitDebounce.add(first);
        return getRecursiveStream(operator, visitDebounce, toVisit);
    }

    public static <T, R> Stream<R> getRecursiveStream(
            RecursiveBuilder<T, R> operator,
            Set<T> visitDebounce,
            Deque<T> toVisit
    ) {
        Stream.Builder<R> builder = Stream.builder();
        while (!toVisit.isEmpty()) {
            T current = toVisit.pop();
            operator.accept(
                    current,
                    next -> {
                        if (!visitDebounce.contains(next)) {
                            visitDebounce.add(next);
                            toVisit.add(next);
                        }
                    },
                    builder::add
            );
        }
        return builder.build();
    }


    public static TranslatableContents deserializeTranslation(CompoundTag tag) {
        var key = tag.getString("key");
        var args = tag
                .getList("args", Tag.TAG_STRING)
                .stream()
                .map(StringTag.class::cast)
                .map(StringTag::getAsString)
                .toArray();
        return getTranslatableContents(key, args);
    }

    public static CompoundTag serializeTranslation(TranslatableContents contents) {
        CompoundTag tag = new CompoundTag();
        tag.putString("key", contents.getKey());
        ListTag args = new ListTag();
        for (var arg : contents.getArgs()) {
            args.add(StringTag.valueOf(arg.toString()));
        }
        tag.put("args", args);
        return tag;
    }

    public static void encodeTranslation(
            TranslatableContents contents,
            FriendlyByteBuf buf
    ) {
        buf.writeUtf(contents.getKey(), MAX_TRANSLATION_ELEMENT_LENGTH);
        buf.writeVarInt(contents.getArgs().length);
        for (var arg : contents.getArgs()) {
            buf.writeUtf(String.valueOf(arg), MAX_TRANSLATION_ELEMENT_LENGTH);
        }
    }

    public static TranslatableContents decodeTranslation(FriendlyByteBuf buf) {
        String key = buf.readUtf(MAX_TRANSLATION_ELEMENT_LENGTH);
        int argCount = buf.readVarInt();
        Object[] args = new Object[argCount];
        for (int i = 0; i < argCount; i++) {
            args[i] = buf.readUtf(MAX_TRANSLATION_ELEMENT_LENGTH);
        }
        return getTranslatableContents(key, args);
    }

    /**
     * Helper method to avoid noisy git merges between versions
     */
    public static TranslatableContents getTranslatableContents(
            String key,
            Object... args
    ) {
        return new TranslatableContents(key, args);
    }

    /**
     * Helper method to avoid noisy git merges between versions
     */
    public static TranslatableContents getTranslatableContents(String key) {
        return getTranslatableContents(key, new Object[]{});
    }

    public static <STACK, ITEM, CAP> Optional<InputStatement> getInputStatementForSlot(
            LimitedInputSlot<STACK, ITEM, CAP> slot,
            LabelAccess labelAccess
    ) {
        STACK potential = slot.peekExtractPotential();
        ResourceType<STACK, ITEM, CAP> resourceType = slot.type;
        if (resourceType.isEmpty(potential)) return Optional.empty();
        long toMove = resourceType.getAmount(potential);
        toMove = Long.min(toMove, slot.tracker.getResourceLimit().limit().quantity().number().value());
        long remainingObligation = slot.tracker.getRemainingRetentionObligation(resourceType, potential);
        toMove -= Long.min(toMove, remainingObligation);
        potential = resourceType.withCount(potential, toMove);
        STACK stack = potential;

        return SFMResourceTypes.DEFERRED_TYPES
                .get()
                .getResourceKey(resourceType)
                .map(x -> {
                    //noinspection unchecked,rawtypes
                    return (ResourceKey<ResourceType<STACK, ITEM, CAP>>) (ResourceKey) x;
                })
                .map((ResourceKey<ResourceType<STACK, ITEM, CAP>> resourceTypeResourceKey) -> SFMUtils.getInputStatementForStack(
                        resourceTypeResourceKey,
                        resourceType,
                        stack,
                        "temp",
                        slot.slot,
                        false,
                        null
                ))
                // update the labels
                .map(inputStatement -> new InputStatement(new LabelAccess(
                        labelAccess.labels(),
                        labelAccess.directions(),
                        inputStatement.labelAccess()
                                .slots(),
                        RoundRobin.disabled()
                ), inputStatement.resourceLimits(), inputStatement.each()));
    }

    public static <STACK, ITEM, CAP> InputStatement getInputStatementForStack(
            ResourceKey<ResourceType<STACK, ITEM, CAP>> resourceTypeResourceKey,
            ResourceType<STACK, ITEM, CAP> resourceType,
            STACK stack,
            String label,
            int slot,
            boolean each,
            @Nullable Direction direction
    ) {
        LabelAccess labelAccess = new LabelAccess(
                List.of(new Label(label)),
                new DirectionQualifier(
                        direction == null
                                ? EnumSet.noneOf(Direction.class)
                                : EnumSet.of(direction)),
                new NumberRangeSet(
                        new NumberRange[]{new NumberRange(slot, slot)}
                ),
                RoundRobin.disabled()
        );
        Limit limit = new Limit(
                new ResourceQuantity(
                        new Number(resourceType.getAmount(stack)),
                        ResourceQuantity.IdExpansionBehaviour.NO_EXPAND
                ),
                new ResourceQuantity(
                        new Number(0),
                        ResourceQuantity.IdExpansionBehaviour.NO_EXPAND
                )
        );
        ResourceLocation stackId = resourceType.getRegistryKey(stack);
        ResourceIdentifier<STACK, ITEM, CAP> resourceIdentifier = new ResourceIdentifier<>(
                resourceTypeResourceKey.location().getNamespace(),
                resourceTypeResourceKey.location().getPath(),
                stackId.getNamespace(),
                stackId.getPath()
        );
        ResourceLimit resourceLimit = new ResourceLimit(
                new ResourceIdSet(List.of(resourceIdentifier)),
                limit,
                With.ALWAYS_TRUE
        );
        ResourceLimits resourceLimits = new ResourceLimits(
                List.of(resourceLimit),
                ResourceIdSet.EMPTY
        );

        // todo: add WITH logic here to also build code to match any item/block tags present
        return new InputStatement(
                labelAccess,
                resourceLimits,
                each
        );
    }

    public static String truncate(
            String input,
            int maxLength
    ) {
        if (input.length() > maxLength) {
            SFM.LOGGER.warn(
                    "input too big, truncation has occurred! (len={}, max={}, over={})",
                    input.length(),
                    maxLength,
                    maxLength - input.length()
            );
            String truncationWarning = "\n...truncated";
            return input.substring(0, maxLength - truncationWarning.length()) + truncationWarning;
        }
        return input;
    }

    /**
     * Find a {@link CapabilityProvider} as provided by the registered capability provider mappers.
     * If multiple {@link CapabilityProviderMapper}s match, the first one is returned.
     */
    @SuppressWarnings("UnstableApiUsage") // for the javadoc lol
    public static @Nullable ICapabilityProvider discoverCapabilityProvider(
            Level level,
            BlockPos pos
    ) {
        if (!level.isLoaded(pos)) return null;

        Collection<CapabilityProviderMapper> mappers = SFMCapabilityProviderMappers.DEFERRED_MAPPERS.get().getValues();
        for (CapabilityProviderMapper mapper : mappers) {
            ICapabilityProvider capabilityProvider = mapper.getProviderFor(level, pos);
            if (capabilityProvider != null) {
                return capabilityProvider;
            }
        }

        return null;
    }

    public static Stream<BlockPos> get3DNeighboursIncludingKittyCorner(BlockPos pos) {
        Stream.Builder<BlockPos> builder = Stream.builder();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    builder.accept(pos.offset(x, y, z));
                }
            }
        }
        return builder.build();
    }

    public static boolean isMekanismBlock(
            Level level,
            BlockPos pos
    ) {
        Block block = level.getBlockState(pos).getBlock();
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);
        assert blockId != null;
        return blockId.getNamespace().equals("mekanism");
    }

    public interface RecursiveBuilder<T, R> {
        void accept(
                T current,
                Consumer<T> next,
                Consumer<R> results
        );
    }
}
