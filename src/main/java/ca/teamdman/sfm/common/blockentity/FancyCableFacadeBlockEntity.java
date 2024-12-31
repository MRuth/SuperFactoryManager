package ca.teamdman.sfm.common.blockentity;

import ca.teamdman.sfm.common.registry.SFMBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

public class FancyCableFacadeBlockEntity extends CommonFacadeBlockEntity<FancyCableFacadeBlockEntity.FancyFacadeData> {
    public static final ModelProperty<Direction> FACADE_DIRECTION = new ModelProperty<>();

    public FancyCableFacadeBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(SFMBlockEntities.FANCY_CABLE_FACADE_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public FancyFacadeData createFacadeData(
            BlockState facadeState,
            Direction hitDirection
    ) {
        return new FancyFacadeData(facadeState, hitDirection, TextureMode.FILL);
    }

    @Override
    public @Nullable FancyCableFacadeBlockEntity.FancyFacadeData loadFacadeData(CompoundTag tag) {
        if (tag.contains("sfm:facade")) {
            BlockState facadeState = NbtUtils.readBlockState(tag.getCompound("sfm:facade"));
            Direction facadeDirection = Direction.byName(tag.getString("sfm:facade_direction"));
            TextureMode textureMode = TextureMode.byName(tag.getString("sfm:texture_mode"));
            if (textureMode != null && facadeDirection != null) {
                return new FancyFacadeData(facadeState, facadeDirection, textureMode);
            }
        }
        return null;
    }

    @Override
    public void saveFacadeData(
            CompoundTag tag,
            FancyFacadeData data
    ) {
        tag.put("sfm:facade", NbtUtils.writeBlockState(data.facadeState));
        tag.putString("sfm:facade_direction", data.facadeDirection().getSerializedName());
        tag.putString("sfm:texture_mode", data.textureMode().getSerializedName());
    }

    @Override
    public ModelData getModelData() {
        if (getFacadeData() != null) {
            return ModelData.builder()
                    .with(IFacadeBlockEntity.FACADE_BLOCK_STATE, getFacadeData().facadeState())
                    .with(FACADE_DIRECTION, getFacadeData().facadeDirection())
                    .build();
        }
        return ModelData.EMPTY;
    }

    public enum TextureMode implements StringRepresentable {
        STRETCH,
        FILL;

        @SuppressWarnings("deprecation")
        public static final StringRepresentable.EnumCodec<TextureMode> CODEC = StringRepresentable.fromEnum(TextureMode::values);

        @Override
        public String getSerializedName() {
            return name();
        }

        public static @Nullable TextureMode byName(@Nullable String pName) {
            return CODEC.byName(pName);
        }

    }

    public record FancyFacadeData(
            BlockState facadeState,
            Direction facadeDirection,
            TextureMode textureMode
    ) implements FacadeData {
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
