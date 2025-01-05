package ca.teamdman.sfm.common.resourcetype;

import ca.teamdman.sfm.common.localization.LocalizationKeys;
import ca.teamdman.sfm.common.program.CapabilityConsumer;
import ca.teamdman.sfm.common.program.LabelPositionHolder;
import ca.teamdman.sfm.common.program.ProgramContext;
import ca.teamdman.sfm.common.registry.SFMResourceTypes;
import ca.teamdman.sfm.common.util.Stored;
import ca.teamdman.sfml.ast.*;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public abstract class ResourceType<STACK, ITEM, CAP> {
    public final Capability<CAP> CAPABILITY_KIND;
    private final Map<ITEM, ResourceLocation> registryKeyCache = new Object2ObjectOpenHashMap<>();

    public ResourceType(Capability<CAP> CAPABILITY_KIND) {
        this.CAPABILITY_KIND = CAPABILITY_KIND;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResourceType<?, ?, ?> that)) return false;
        return Objects.equals(CAPABILITY_KIND, that.CAPABILITY_KIND);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(CAPABILITY_KIND);
    }

    public abstract long getAmount(STACK stack);

    /**
     * Some resource types may exceed MAX_LONG, this method should be used to get the difference between two stacks
     */
    public long getAmountDifference(
            STACK stack1,
            STACK stack2
    ) {
        return getAmount(stack1) - getAmount(stack2);
    }

    public abstract STACK getStackInSlot(
            CAP cap,
            int slot
    );

    public abstract STACK extract(
            CAP cap,
            int slot,
            long amount,
            boolean simulate
    );

    public abstract int getSlots(CAP handler);

    public abstract long getMaxStackSize(STACK stack);

    public abstract long getMaxStackSizeForSlot(
            CAP cap,
            int slot
    );


    public abstract STACK insert(
            CAP cap,
            int slot,
            STACK stack,
            boolean simulate
    );

    public abstract boolean isEmpty(STACK stack);

    @SuppressWarnings("unused")
    public abstract STACK getEmptyStack();

    public abstract boolean matchesStackType(Object o);

    public boolean matchesStack(
            ResourceIdentifier<STACK, ITEM, CAP> resourceId,
            Object stack
    ) {
        if (!matchesStackType(stack)) return false;
        @SuppressWarnings("unchecked") STACK stack_ = (STACK) stack;
        if (isEmpty(stack_)) return false;
        var stackId = getRegistryKey(stack_);
        return resourceId.matchesResourceLocation(stackId);
    }

    public abstract boolean matchesCapabilityType(Object o);

    public void forEachCapability(
            ProgramContext programContext,
            LabelAccess labelAccess,
            CapabilityConsumer<CAP> consumer
    ) {
        // Log
        programContext
                .getLogger()
                .trace(x -> x.accept(LocalizationKeys.LOG_RESOURCE_TYPE_GET_CAPABILITIES_BEGIN.get(
                        displayAsCode(),
                        displayAsCapabilityClass(),
                        labelAccess
                )));

        DirectionQualifier directions = labelAccess.directions();
        LabelPositionHolder labelPositionHolder = programContext.getLabelPositionHolder();
        ArrayList<Pair<Label, BlockPos>> positions = labelAccess.getLabelledPositions(labelPositionHolder);

        for (var pair : positions) {
            Label label = pair.getFirst();
            BlockPos pos = pair.getSecond();
            forEachDirectionalCapability(
                    programContext,
                    directions,
                    pos,
                    (dir, cap) -> consumer.accept(label, pos, dir, cap)
            );
        }
    }

    public void forEachDirectionalCapability(
            ProgramContext programContext,
            DirectionQualifier directions,
            @Stored BlockPos pos,
            BiConsumer<Direction, CAP> consumer
    ) {
        for (Direction dir : directions) {
            Optional<CAP> maybeCap = programContext.getNetwork()
                    .getCapability(CAPABILITY_KIND, pos, dir, programContext.getLogger())
                    .resolve();
            if (maybeCap.isPresent()) {
                programContext
                        .getLogger()
                        .debug(x -> x.accept(LocalizationKeys.LOG_RESOURCE_TYPE_GET_CAPABILITIES_CAP_PRESENT.get(
                                displayAsCapabilityClass(),
                                pos,
                                dir
                        )));
                CAP cap = maybeCap.get();
                consumer.accept(dir, cap);
            } else {
                // Log error
                programContext
                        .getLogger()
                        .error(x -> x.accept(LocalizationKeys.LOG_RESOURCE_TYPE_GET_CAPABILITIES_CAP_NOT_PRESENT.get(
                                displayAsCapabilityClass(),
                                pos,
                                dir
                        )));
            }
        }
    }

    public abstract Stream<ResourceLocation> getTagsForStack(STACK stack);

    public Stream<STACK> getStacksInSlots(
            CAP cap,
            NumberRangeSet slots
    ) {
        var rtn = Stream.<STACK>builder();
        for (int slot = 0; slot < getSlots(cap); slot++) {
            if (!slots.contains(slot)) continue;
            var stack = getStackInSlot(cap, slot);
            if (!isEmpty(stack)) {
                rtn.add(stack);
            }
        }
        return rtn.build();
    }

    public boolean registryKeyExists(ResourceLocation location) {
        return getRegistry().containsKey(location);
    }

    public ResourceLocation getRegistryKey(STACK stack) {
        ITEM item = getItem(stack);
        var found = registryKeyCache.get(item);
        if (found != null) return found;
        found = getRegistry().getKey(item);
        if (found == null) {
            throw new NullPointerException("Registry key not found for item: " + item);
        }
        registryKeyCache.put(item, found);
        return found;
    }

    public abstract IForgeRegistry<ITEM> getRegistry();

    public abstract ITEM getItem(STACK stack);

    public abstract STACK copy(STACK stack);

    @SuppressWarnings("unused")
    public STACK withCount(
            STACK stack,
            long count
    ) {
        return setCount(copy(stack), count);
    }

    public String displayAsCode() {
        ResourceLocation thisKey = SFMResourceTypes.DEFERRED_TYPES.get().getKey(this);
        return thisKey != null ? thisKey.toString() : "null";
    }

    public String displayAsCapabilityClass() {
        return CAPABILITY_KIND.getName();
    }

    protected abstract STACK setCount(
            STACK stack,
            long amount
    );

}
