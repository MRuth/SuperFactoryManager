package ca.teamdman.sfml.ast;

import ca.teamdman.sfm.common.program.InputResourceTracker;
import ca.teamdman.sfm.common.program.LimitedInputSlot;
import ca.teamdman.sfm.common.program.LimitedInputSlotObjectPool;
import ca.teamdman.sfm.common.program.ProgramContext;
import ca.teamdman.sfm.common.resourcetype.ResourceType;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public record InputStatement(
        LabelAccess labelAccess,
        ResourceLimits resourceLimits,
        boolean each
) implements Statement {

    @Override
    public void tick(ProgramContext context) {
        context.addInput(this);
    }

    private static final LimitedInputSlotObjectPool SLOT_POOL = new LimitedInputSlotObjectPool();

    public static void releaseSlots(List<LimitedInputSlot> slots) {
        SLOT_POOL.release(slots);
    }

    public static void releaseSlot(LimitedInputSlot slot) {
        SLOT_POOL.release(slot);
    }

    public void gatherSlots(ProgramContext context, Consumer<LimitedInputSlot<?, ?, ?>> acceptor) {
        Stream<ResourceType> types = resourceLimits
                .resourceLimits()
                .stream()
                .map(ResourceLimit::resourceId)
                .map((ResourceIdentifier x) -> x.getResourceType())
                .distinct();

        if (!each) {
            // create a single matcher to be shared by all capabilities
            List<InputResourceTracker<?, ?, ?>> inputMatchers = resourceLimits.createInputTrackers();
            for (var type : (Iterable<ResourceType>) types::iterator) {
                for (var capability : (Iterable) type.getCapabilities(context, labelAccess)::iterator) {
                    gatherSlots((ResourceType<Object, Object, Object>) type, capability, inputMatchers, acceptor);
                }
            }
        } else {
            for (ResourceType type : (Iterable<ResourceType>) types::iterator) {
                for (var cap : (Iterable<?>) type.getCapabilities(context, labelAccess)::iterator) {
                    List<InputResourceTracker<?, ?, ?>> inputTrackers = resourceLimits.createInputTrackers();
                    gatherSlots((ResourceType<Object, Object, Object>) type, cap, inputTrackers, acceptor);
                }
            }
        }
    }

    private <STACK, ITEM, CAP> void gatherSlots(
            ResourceType<STACK, ITEM, CAP> type,
            CAP capability,
            List<InputResourceTracker<?, ?, ?>> trackers,
            Consumer<LimitedInputSlot<?, ?, ?>> acceptor
    ) {
        for (int slot = 0; slot < type.getSlots(capability); slot++) {
            if (labelAccess.slots().contains(slot)) {
                STACK stack = type.getStackInSlot(capability, slot);
                if (!type.isEmpty(stack)) {
                    for (InputResourceTracker<?, ?, ?> tracker : trackers) {
                        if (tracker.matchesCapabilityType(capability) && tracker.test(stack)) {
                            acceptor.accept(SLOT_POOL.acquire(
                                    capability,
                                    slot,
                                    (InputResourceTracker<STACK, ITEM, CAP>) tracker
                            ));
                        }
                    }
                }
            }
        }
    }
}
