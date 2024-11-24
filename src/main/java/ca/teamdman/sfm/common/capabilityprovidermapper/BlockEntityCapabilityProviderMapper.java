package ca.teamdman.sfm.common.capabilityprovidermapper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.Nullable;

public class BlockEntityCapabilityProviderMapper implements CapabilityProviderMapper {
    @Override
    public @Nullable ICapabilityProvider getProviderFor(LevelAccessor level, BlockPos pos) {
        return level.getBlockEntity(pos);
    }
}
