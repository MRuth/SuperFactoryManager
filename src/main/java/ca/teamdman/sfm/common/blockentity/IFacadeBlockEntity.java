package ca.teamdman.sfm.common.blockentity;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

public interface IFacadeBlockEntity<T extends IFacadeBlockEntity.FacadeData> {
    ModelProperty<BlockState> FACADE_BLOCK_STATE = new ModelProperty<>();

    void updateFacadeData(
            BlockState newFacadeState,
            Direction hitDirection
    );

    T createFacadeData(
            BlockState facadeState,
            Direction hitDirection
    );

    @Nullable T loadFacadeData(CompoundTag tag);

    void saveFacadeData(
            CompoundTag tag,
            T data
    );

    @Nullable T getFacadeData();

    interface FacadeData {
        BlockState getRenderBlockState();
    }
}
