package ca.teamdman.sfm.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;

public class LeftRightManagerTest extends SFMTestBuilder {

    public LeftRightManagerTest(GameTestHelper helper) {
        super(helper);
    }

    @Override
    protected void setupStructure(BlockPos offset) {
        setupChests(offset);
        setupManager(offset);
    }

    protected void setupChests(BlockPos offset) {
        addChest("left", new BlockPos(2, 2, 0).offset(offset));
        addChest("right", new BlockPos(0, 2, 0).offset(offset));
    }
}

