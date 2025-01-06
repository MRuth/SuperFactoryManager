package ca.teamdman.sfm.gametest.declarative;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SFMTestSpec {
    // The user can define each block they want placed
    private final List<TestBlockDef<?>> blocks = new ArrayList<>();

    // Program to be loaded into the manager
    private @Nullable String program = null;

    // In a more advanced approach, these could be typed AST nodes instead of Strings
    private final List<String> preconditions = new ArrayList<>();
    private final List<String> postconditions = new ArrayList<>();

    // The offset used to place everything in the test world
    // e.g. if your manager is placed at (1, 2, 0), you can store that offset
    private BlockPos offset = BlockPos.ZERO;

    /* -- Constructors / Builders -- */

    public SFMTestSpec offset(BlockPos pos) {
        this.offset = pos;
        return this;
    }

    public SFMTestSpec setProgram(String program) {
        this.program = program.stripIndent().stripTrailing();
        return this;
    }

    public SFMTestSpec addBlock(TestBlockDef<?> def) {
        this.blocks.add(def);
        return this;
    }

    public SFMTestSpec precondition(String conditionDsl) {
        this.preconditions.add(conditionDsl);
        return this;
    }

    public SFMTestSpec postcondition(String conditionDsl) {
        this.postconditions.add(conditionDsl);
        return this;
    }

    // Getters
    public BlockPos offset() {
        return offset;
    }

    public String program() {
        return program;
    }

    public List<TestBlockDef<?>> blocks() {
        return blocks;
    }

    public List<String> preconditions() {
        return preconditions;
    }

    public List<String> postconditions() {
        return postconditions;
    }
}
