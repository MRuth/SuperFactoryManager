package ca.teamdman.sfm.gametest.declarative;

import ca.teamdman.sfm.common.blockentity.ManagerBlockEntity;
import ca.teamdman.sfm.common.program.DefaultProgramBehaviour;
import ca.teamdman.sfm.common.program.LabelPositionHolder;
import ca.teamdman.sfm.common.program.ProgramContext;
import ca.teamdman.sfm.common.registry.SFMBlocks;
import ca.teamdman.sfm.common.registry.SFMItems;
import ca.teamdman.sfm.common.util.NotStored;
import ca.teamdman.sfm.gametest.SFMGameTestBase;
import ca.teamdman.sfml.ast.ASTBuilder;
import ca.teamdman.sfml.ast.BoolExpr;
import ca.teamdman.sfml.ast.IfStatement;
import ca.teamdman.sfml.ast.Program;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class SFMDeclarativeTestBuilder extends SFMGameTestBase {
    private final GameTestHelper helper;
    private final SFMTestSpec spec;
    private int conditionIndex = 0;

    public SFMDeclarativeTestBuilder(
            GameTestHelper helper,
            SFMTestSpec spec
    ) {
        this.helper = helper;
        this.spec = spec;
    }

    public void run() {
        BlockPos managerPos = calculateManagerPos();
        placeBlocks(managerPos);
        ManagerBlockEntity manager = setupManager(managerPos);
        labelBlocks(managerPos, manager);
        runPreConditions(manager);
        succeedIfManagerDidThingWithoutLagging(
                helper,
                manager,
                () -> runPostConditions(manager)
        );
        // todo: chaos
    }

    private @NotNull BlockPos calculateManagerPos() {
        return BoundingBox.encapsulatingPositions(
                        spec.blocks()
                                .stream()
                                .map(TestBlockDef::posRelativeToManager)
                                .toList()
                )
                .map(bounds -> {
                    int x = bounds.getXSpan();
                    int y = bounds.getYSpan();
                    int z = bounds.getZSpan();
                    return new BlockPos(x / 2, y / 2 + 2, z / 2);
                })
                .orElse(new BlockPos(0, 1, 0));
    }

    private void placeBlocks(@NotStored BlockPos managerPos) {
        for (TestBlockDef<?> blockDef : spec.blocks()) {
            placeBlock(blockDef, managerPos);
        }
    }

    private <T extends BlockEntity> void placeBlock(
            TestBlockDef<T> def,
            @NotStored BlockPos managerPos
    ) {
        BlockPos blockPos = managerPos.offset(def.posRelativeToManager());
        helper.setBlock(blockPos, def.block());
        if (def.blockEntityConfigurer() != null) {
            BlockEntity be = helper.getBlockEntity(blockPos);
            //noinspection unchecked
            Objects.requireNonNull(def.blockEntityConfigurer()).accept((T) be);
        }
    }

    private ManagerBlockEntity setupManager(@NotStored BlockPos managerPos) {
        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());
        if (helper.getBlockEntity(managerPos) instanceof ManagerBlockEntity manager) {
            manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
            manager.setProgram(spec.program());
//            manager.setLogLevel(Level.DEBUG);
            return manager;
        } else {
            throw new GameTestAssertException("Manager block entity not found!");
        }
    }

    private void runPreConditions(ManagerBlockEntity manager) {
        runConditions(manager, "Pre-condition", spec.preConditions());
    }

    private void runPostConditions(ManagerBlockEntity manager) {
        runConditions(manager, "Post-condition", spec.postConditions());
    }

    private void runConditions(
            ManagerBlockEntity manager,
            String conditionKind,
            List<String> conditions
    ) {
        if (conditions.isEmpty()) return;
        List<BoolExpr> expressions = conditions.stream().map(this::getCondition).toList();
        ProgramContext programContext = new ProgramContext(
                new Program(new ASTBuilder(), "temp lol", List.of(), Set.of(), Set.of(), -1),
                manager,
                DefaultProgramBehaviour::new
        );
        for (BoolExpr expr : expressions) {
            boolean passed = expr.test(programContext);
            if (!passed) {
                helper.fail(conditionKind + " " + conditionIndex + " failed: " + expr);
            }
            conditionIndex++;
        }
    }

    private BoolExpr getCondition(String line) {
        // This is where you’d parse lines like:
        //   “a BOTTOM SIDE HAS EQ 1000 fe::”
        // Or something like: “b BOTTOM SIDE HAS EQ 0 fe::”
        Mutable<BoolExpr> rtn = new MutableObject<>();
        String program = "EVERY 20 TICKS DO IF " + line + " THEN END END";
        Program.compile(
                program,
                success -> {
                    BoolExpr condition = (
                            (IfStatement) success
                                    .triggers()
                                    .get(0)
                                    .getBlock()
                                    .getStatements()
                                    .get(0)
                    ).condition();
                    rtn.setValue(condition);
                },
                failure -> {
                    StringBuilder msg = new StringBuilder("Failed to compile program: ").append(program);
                    msg.append('\n');
                    failure.forEach(e -> msg.append(e.toString()).append('\n'));
                    throw new IllegalStateException(msg.toString());
                }
        );
        return rtn.getValue();
    }

    private void labelBlocks(
            @NotStored BlockPos managerPos,
            ManagerBlockEntity manager
    ) {
        LabelPositionHolder labelHolder = LabelPositionHolder.empty();
        for (TestBlockDef<?> def : spec.blocks()) {
            // Actually add the label to the holder
            BlockPos absolutePos = helper.absolutePos(managerPos.offset(def.posRelativeToManager()));
            labelHolder.add(def.label(), absolutePos);
        }
        labelHolder.save(Objects.requireNonNull(manager.getDisk()));

    }
}
