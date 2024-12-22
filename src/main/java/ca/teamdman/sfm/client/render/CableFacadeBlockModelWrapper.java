package ca.teamdman.sfm.client.render;

import ca.teamdman.sfm.common.block.CableFacadeBlock;
import ca.teamdman.sfm.common.registry.SFMBlocks;
import ca.teamdman.sfm.common.util.FacadeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CableFacadeBlockModelWrapper extends BakedModelWrapper<BakedModel> {

    private static final ChunkRenderTypeSet SOLID = ChunkRenderTypeSet.of(RenderType.solid());
    private static final ChunkRenderTypeSet ALL = ChunkRenderTypeSet.all();

    public CableFacadeBlockModelWrapper(BakedModel originalModel) {
        super(originalModel);
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(
            @Nullable BlockState state,
            @Nullable Direction side,
            @NotNull RandomSource rand,
            @NotNull ModelData extraData,
            @Nullable RenderType renderType
    ) {
        BlockState mimicState = extraData.get(CableFacadeBlock.FACADE_BLOCK_STATE);
        if (mimicState == null) {
            return originalModel.getQuads(state, side, rand, ModelData.EMPTY, renderType);
        }
        Minecraft minecraft = Minecraft.getInstance();
        BlockRenderDispatcher blockRenderer = minecraft.getBlockRenderer();
        if (mimicState.getBlock() == SFMBlocks.CABLE_FACADE_BLOCK.get()) {
            // facade blocks should only exist with some other block to be shown
            return minecraft
                    .getModelManager()
                    .getMissingModel()
                    .getQuads(mimicState, side, rand, ModelData.EMPTY, renderType);
        }

        BakedModel mimicModel = blockRenderer.getBlockModel(mimicState);
        ChunkRenderTypeSet renderTypes = mimicModel.getRenderTypes(mimicState, rand, extraData);

        if (renderType == null || renderTypes.contains(renderType)) {
            return mimicModel.getQuads(mimicState, side, rand, ModelData.EMPTY, renderType);
        }

        return List.of();
    }

    @Override
    public @NotNull ChunkRenderTypeSet getRenderTypes(
            @NotNull BlockState cableBlockState,
            @NotNull RandomSource rand,
            @NotNull ModelData data
    ) {
        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
        BlockState paintBlockState = data.get(CableFacadeBlock.FACADE_BLOCK_STATE);
        if (paintBlockState == null) {
            return cableBlockState.getValue(CableFacadeBlock.FACADE_TYPE_PROP) == FacadeType.TRANSLUCENT ? ALL : SOLID;
        }
        BakedModel bakedModel = blockRenderer.getBlockModel(paintBlockState);
        return bakedModel.getRenderTypes(paintBlockState, rand, ModelData.EMPTY);
    }
}
