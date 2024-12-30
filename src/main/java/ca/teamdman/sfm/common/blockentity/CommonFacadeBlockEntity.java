package ca.teamdman.sfm.common.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

public abstract class CommonFacadeBlockEntity<T extends IFacadeBlockEntity.FacadeData> extends BlockEntity implements IFacadeBlockEntity<T> {
    protected @Nullable T facadeData = null;

    public CommonFacadeBlockEntity(
            BlockEntityType<?> pType,
            BlockPos pPos,
            BlockState pBlockState
    ) {
        super(pType, pPos, pBlockState);
    }

    @Override
    public @Nullable T getFacadeData() {
        return facadeData;
    }

    @Override
    public void updateFacadeData(
            BlockState newFacadeState,
            Direction hitDirection
    ) {
        T newData = createFacadeData(newFacadeState, hitDirection);
        if (newData.equals(facadeData)) return;
        this.facadeData = newData;
        setChanged();
        requestModelDataUpdate();
    }

    @Override
    public abstract ModelData getModelData();

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        T tried = loadFacadeData(pTag);
        if (facadeData != null) {
            this.facadeData = tried;
            requestModelDataUpdate();
        }
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag pTag = new CompoundTag();
        saveAdditional(pTag);
        return pTag;
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        super.saveAdditional(pTag);
        if (facadeData != null) {
            saveFacadeData(pTag, facadeData);
        }
    }
}
