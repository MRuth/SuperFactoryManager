package ca.teamdman.sfm.gametest.declarative;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SFMTestSpec {
    private final List<TestBlockDef<?>> blocks = new ArrayList<>();
    private @Nullable String program = null;
    private final List<String> preConditions = new ArrayList<>();
    private final List<String> postConditions = new ArrayList<>();

    public SFMTestSpec setProgram(String program) {
        this.program = program.stripTrailing().stripIndent();
        return this;
    }

    public SFMTestSpec addBlock(TestBlockDef<?> def) {
        this.blocks.add(def);
        return this;
    }

    public SFMTestSpec preCondition(String conditionDsl) {
        this.preConditions.add(conditionDsl);
        return this;
    }

    public SFMTestSpec postCondition(String conditionDsl) {
        this.postConditions.add(conditionDsl);
        return this;
    }

    public String program() {
        return program;
    }

    public List<TestBlockDef<?>> blocks() {
        return blocks;
    }

    public List<String> preConditions() {
        return preConditions;
    }

    public List<String> postConditions() {
        return postConditions;
    }
}
