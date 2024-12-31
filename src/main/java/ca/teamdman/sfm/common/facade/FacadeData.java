package ca.teamdman.sfm.common.facade;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public record FacadeData(
        BlockState facadeBlockState,
        Direction facadeDirection,
        FacadeTextureMode facadeTextureMode
) {
    public void save(CompoundTag tag) {
        CompoundTag facadeTag = new CompoundTag();
        facadeTag.put("block_state", NbtUtils.writeBlockState(this.facadeBlockState()));
        facadeTag.putString("direction", this.facadeDirection().getSerializedName());
        facadeTag.putString("texture_mode", this.facadeTextureMode().getSerializedName());
        tag.put("sfm:facade", facadeTag);
    }

    public static @Nullable FacadeData load(CompoundTag tag) {
        if (tag.contains("sfm:facade", CompoundTag.TAG_COMPOUND)) {
            CompoundTag facadeTag = tag.getCompound("sfm:facade");
            BlockState facadeState = NbtUtils.readBlockState(facadeTag.getCompound("block_state"));
            Direction facadeDirection = Direction.byName(facadeTag.getString("direction"));
            FacadeTextureMode facadeTextureMode = FacadeTextureMode.byName(facadeTag.getString("texture_mode"));
            if (facadeTextureMode != null && facadeDirection != null) {
                return new FacadeData(facadeState, facadeDirection, facadeTextureMode);
            }
        }
        return null;
    }
}
