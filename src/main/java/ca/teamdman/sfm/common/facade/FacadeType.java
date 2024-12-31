package ca.teamdman.sfm.common.facade;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public enum FacadeType implements StringRepresentable {
    OPAQUE, TRANSLUCENT;
    public static final EnumProperty<FacadeType> FACADE_TYPE_PROPERTY = EnumProperty.create("facade_type", FacadeType.class);

    FacadeType() {
    }

    @Override
    public String getSerializedName() {
        return switch (this) {
            case OPAQUE -> "opaque";
            case TRANSLUCENT -> "translucent";
        };
    }
}