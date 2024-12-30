package ca.teamdman.sfm.common.blockentity;

import ca.teamdman.sfm.common.registry.SFMBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

public class CableFacadeBlockEntity extends CommonFacadeBlockEntity<CableFacadeBlockEntity.SimpleFacadeData> {
    public CableFacadeBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(SFMBlockEntities.CABLE_FACADE_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public SimpleFacadeData createFacadeData(
            BlockState facadeState,
            Direction hitDirection
    ) {
        return new SimpleFacadeData(facadeState);
    }

    @Override
    public @Nullable CableFacadeBlockEntity.SimpleFacadeData loadFacadeData(CompoundTag tag) {
        if (tag.contains("sfm:facade")) {
            return new SimpleFacadeData(NbtUtils.readBlockState(tag.getCompound("sfm:facade")));
        }
        return null;
    }

    @Override
    public void saveFacadeData(
            CompoundTag tag,
            SimpleFacadeData data
    ) {
        tag.put("sfm:facade", NbtUtils.writeBlockState(data.facadeState));
    }

    @Override
    public ModelData getModelData() {
        if (getFacadeData() != null) {
            return ModelData.builder().with(FACADE_BLOCK_STATE, getFacadeData().facadeState()).build();
        }
        return ModelData.EMPTY;
    }

    public record SimpleFacadeData(BlockState facadeState) implements FacadeData {
        @Override
        public BlockState getRenderBlockState() {
            return facadeState();
        }
    }
}
