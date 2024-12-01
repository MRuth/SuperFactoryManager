package ca.teamdman.sfm.gametest;

import ca.teamdman.sfm.common.blockentity.ManagerBlockEntity;
import ca.teamdman.sfm.common.registry.SFMBlocks;
import ca.teamdman.sfm.common.registry.SFMItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

public class LeftRightManagerTest extends SFMTestBuilder {

    public LeftRightManagerTest(GameTestHelper helper) {
        super(helper);
        setupChests();
    }

    @Override
    protected void setupManager() {
        BlockPos managerPos = new BlockPos(1, 2, 0);
        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());
        manager = (ManagerBlockEntity) helper.getBlockEntity(managerPos);
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram(program.stripTrailing().stripIndent());
    }

    private void setupChests() {
        addChest("left", new BlockPos(2, 2, 0));
        addChest("right", new BlockPos(0, 2, 0));
    }
}

