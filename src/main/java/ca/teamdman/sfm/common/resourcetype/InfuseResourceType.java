package ca.teamdman.sfm.common.resourcetype;

import mekanism.api.Action;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.infuse.IInfusionHandler;
import mekanism.api.chemical.infuse.InfuseType;
import mekanism.api.chemical.infuse.InfusionStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.registries.IForgeRegistry;

import static net.minecraftforge.common.capabilities.CapabilityManager.get;

public class InfuseResourceType extends ResourceType<InfusionStack, InfuseType, IInfusionHandler> {
    public static final Capability<IInfusionHandler> CAP = get(new CapabilityToken<>() {
    });

    public InfuseResourceType() {
        super(CAP);
    }

    @Override
    public long getCount(InfusionStack stack) {
        return stack.getAmount();
    }

    @Override
    public InfusionStack getStackInSlot(IInfusionHandler handler, int slot) {
        return handler.getChemicalInTank(slot);
    }

    @Override
    public InfusionStack extract(IInfusionHandler handler, int slot, long amount, boolean simulate) {
        return handler.extractChemical(slot, amount, simulate ? Action.SIMULATE : Action.EXECUTE);
    }

    @Override
    public int getSlots(IInfusionHandler handler) {
        return handler.getTanks();
    }

    @Override
    public InfusionStack insert(
            IInfusionHandler handler,
            int slot,
            InfusionStack stack,
            boolean simulate
    ) {
        return handler.insertChemical(slot, stack, simulate ? Action.SIMULATE : Action.EXECUTE);
    }

    @Override
    public boolean isEmpty(InfusionStack stack) {
        return stack.isEmpty();
    }

    @Override
    public boolean matchesStackType(Object o) {
        return o instanceof InfusionStack;
    }

    @Override
    public boolean matchesCapabilityType(Object o) {
        return o instanceof IInfusionHandler;
    }


    @Override
    public IForgeRegistry<InfuseType> getRegistry() {
        return MekanismAPI.infuseTypeRegistry();
    }

    @Override
    public InfuseType getItem(InfusionStack stack) {
        return stack.getType();
    }
}
