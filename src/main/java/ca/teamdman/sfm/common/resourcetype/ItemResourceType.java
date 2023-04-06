package ca.teamdman.sfm.common.resourcetype;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

public class ItemResourceType extends ResourceType<ItemStack, IItemHandler> {
    public ItemResourceType() {
        super(ForgeCapabilities.ITEM_HANDLER);
    }

    @Override
    public boolean registryKeyExists(ResourceLocation location) {
        return ForgeRegistries.ITEMS.containsKey(location);
    }

    @Override
    public ResourceLocation getRegistryKey(ItemStack itemStack) {
        return ForgeRegistries.ITEMS.getKey(itemStack.getItem());
    }

    @Override
    public long getCount(ItemStack stack) {
        return stack.getCount();
    }

    @Override
    public ItemStack getStackInSlot(IItemHandler cap, int slot) {
        return cap.getStackInSlot(slot);
    }

    @Override
    public ItemStack extract(IItemHandler handler, int slot, long amount, boolean simulate) {
        int finalAmount = amount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) amount;
        return handler.extractItem(slot, finalAmount, simulate);
    }

    @Override
    public boolean matchesStackType(Object o) {
        return o instanceof ItemStack;
    }

    @Override
    public boolean matchesCapType(Object o) {
        return o instanceof IItemHandler;
    }

    @Override
    public int getSlots(IItemHandler handler) {
        return handler.getSlots();
    }

    /**
     * @param handler
     * @param slot
     * @param stack
     * @param simulate
     * @return remaining stack that was not inserted
     */
    @Override
    public ItemStack insert(IItemHandler handler, int slot, ItemStack stack, boolean simulate) {
        return handler.insertItem(slot, stack, simulate);
    }

    @Override
    public boolean isEmpty(ItemStack stack) {
        return stack.isEmpty();
    }

}
