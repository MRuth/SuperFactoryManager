package ca.teamdman.sfml.ast;

public record Limit(
        long quantity,
        long retention
) implements ASTNode {

    public Limit() {
        this(-1, -1);
    }

    public Limit withDefaults(long quantity, long retention) {
        if (quantity() < 0 && retention() < 0)
            return new Limit(quantity, retention);
        else if (quantity() < 0)
            return new Limit(quantity, retention());
        else if (retention() < 0)
            return new Limit(quantity(), retention);
        return this;
    }
}
