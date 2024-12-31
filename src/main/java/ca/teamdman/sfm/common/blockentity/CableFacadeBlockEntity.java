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
        return new SimpleFacadeData(facadeState, hitDirection);
    }

    @Override
    public @Nullable CableFacadeBlockEntity.SimpleFacadeData loadFacadeData(CompoundTag tag) {
        if (tag.contains("sfm:facade")) {
            BlockState facadeState = NbtUtils.readBlockState(tag.getCompound("sfm:facade"));
            Direction facadeDirection = Direction.byName(tag.getString("sfm:facade_direction"));
            if (facadeDirection != null) {
                return new SimpleFacadeData(facadeState, facadeDirection);
            }
        }
        return null;
    }

    @Override
    public void saveFacadeData(
            CompoundTag tag,
            SimpleFacadeData data
    ) {
        tag.put("sfm:facade", NbtUtils.writeBlockState(data.facadeState));
        tag.putString("sfm:facade_direction", data.facadeDirection().getSerializedName());
    }

    @Override
    public ModelData getModelData() {
        if (getFacadeData() != null) {
            return ModelData.builder().with(FACADE_BLOCK_STATE, getFacadeData().facadeState()).build();
        }
        return ModelData.EMPTY;
    }

    public record SimpleFacadeData(BlockState facadeState, Direction facadeDirection) implements FacadeData {
        @Override
        public BlockState getRenderBlockState() {
            return facadeState();
        }

        @Override
        public Direction getRenderHitDirection() {
            return facadeDirection();
        }
    }
}
