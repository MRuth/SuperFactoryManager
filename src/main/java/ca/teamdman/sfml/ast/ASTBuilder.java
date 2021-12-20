package ca.teamdman.sfml.ast;

import ca.teamdman.sfml.SFMLBaseVisitor;
import ca.teamdman.sfml.SFMLParser;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ASTBuilder extends SFMLBaseVisitor<ASTNode> {
    private final Set<Label> LABELS = new HashSet<>();

    private void trackLabel(Label label) {
        LABELS.add(label);
    }

    public Set<Label> getLabels() {
        return LABELS;
    }

    @Override
    public StringHolder visitName(SFMLParser.NameContext ctx) {
        if (ctx == null) return new StringHolder("");
        return visitString(ctx.string());
    }

    @Override
    public ItemIdentifier visitItem(SFMLParser.ItemContext ctx) {
        var params = ctx.IDENTIFIER().stream().map(TerminalNode::getText).collect(Collectors.toList());
        if (params.size() == 1) return new ItemIdentifier(params.get(0));
        return new ItemIdentifier(params.get(0), params.get(1));
    }

    @Override
    public StringHolder visitString(SFMLParser.StringContext ctx) {
        var content = ctx.getText();
        return new StringHolder(content.substring(1, content.length() - 1));
    }

    @Override
    public Label visitLabel(SFMLParser.LabelContext ctx) {
        var label = new Label(ctx.getText());
        trackLabel(label);
        return label;
    }

    @Override
    public Program visitProgram(SFMLParser.ProgramContext ctx) {
        var name = visitName(ctx.name());
        var triggers = ctx
                .trigger()
                .stream()
                .map(this::visit)
                .map(Trigger.class::cast)
                .collect(Collectors.toList());
        var labels = getLabels()
                .stream()
                .map(Label::name)
                .collect(Collectors.toSet());
        return new Program(name.value(), triggers, labels);
    }

    @Override
    public ASTNode visitTimerTrigger(SFMLParser.TimerTriggerContext ctx) {
        var time = (Interval) visit(ctx.interval());
        if (time.getSeconds() < 1) throw new IllegalArgumentException("Minimum trigger interval is 1 second.");
        var block = visitBlock(ctx.block());
        return new TimerTrigger(time, block);
    }

    @Override
    public Quantity visitNumber(SFMLParser.NumberContext ctx) {
        return new Quantity(Integer.parseInt(ctx.getText()));
    }

    @Override
    public Interval visitTicks(SFMLParser.TicksContext ctx) {
        var num = visitNumber(ctx.number());
        return Interval.fromTicks(num.value());
    }

    @Override
    public Interval visitSeconds(SFMLParser.SecondsContext ctx) {
        var num = visitNumber(ctx.number());
        return Interval.fromSeconds(num.value());
    }

    @Override
    public InputStatement visitInputStatementStatement(SFMLParser.InputStatementStatementContext ctx) {
        return (InputStatement) visit(ctx.inputstatement());
    }

    @Override
    public OutputStatement visitOutputStatementStatement(SFMLParser.OutputStatementStatementContext ctx) {
        return (OutputStatement) visit(ctx.outputstatement());
    }


    @Override
    public InputStatement visitInputstatement(SFMLParser.InputstatementContext ctx) {
        var labelAccess = visitLabelaccess(ctx.labelaccess());
        var matchers    = visitInputmatchers(ctx.inputmatchers());
        var each        = ctx.EACH() != null;
        return new InputStatement(labelAccess, matchers, each);
    }

    @Override
    public OutputStatement visitOutputstatement(SFMLParser.OutputstatementContext ctx) {
        var labelAccess = visitLabelaccess(ctx.labelaccess());
        var matchers    = visitOutputmatchers(ctx.outputmatchers());
        var each        = ctx.EACH() != null;
        return new OutputStatement(labelAccess, matchers, each);
    }

    @Override
    public LabelAccess visitLabelaccess(SFMLParser.LabelaccessContext ctx) {
        return new LabelAccess(
                ctx.label().stream().map(this::visitLabel).collect(Collectors.toList()),
                visitSidequalifier(ctx.sidequalifier()),
                visitSlotqualifier(ctx.slotqualifier())
        );
    }

    @Override
    public BoolExpr visitBooleanTrue(SFMLParser.BooleanTrueContext ctx) {
        return new BoolExpr(__ -> true);
    }

    @Override
    public BoolExpr visitBooleanHas(SFMLParser.BooleanHasContext ctx) {
        var labelAccess    = visitLabelaccess(ctx.labelaccess());
        var itemComparison = visitItemcomparison(ctx.itemcomparison());
        return BoolExpr.from(labelAccess, itemComparison);
    }

    @Override
    public ItemComparer visitItemcomparison(SFMLParser.ItemcomparisonContext ctx) {
        var op   = visitComparisonOp(ctx.comparisonOp());
        var num  = visitNumber(ctx.number());
        var item = visitItem(ctx.item());
        return new ItemComparer(op, num, item);
    }

    @Override
    public ComparisonOperator visitComparisonOp(SFMLParser.ComparisonOpContext ctx) {
        return ComparisonOperator.from(ctx.getText());
    }

    @Override
    public BoolExpr visitBooleanConjunction(SFMLParser.BooleanConjunctionContext ctx) {
        var left  = (BoolExpr) visit(ctx.boolexpr(0));
        var right = (BoolExpr) visit(ctx.boolexpr(1));
        return new BoolExpr(left.and(right));
    }

    @Override
    public BoolExpr visitBooleanDisjunction(SFMLParser.BooleanDisjunctionContext ctx) {
        var left  = (BoolExpr) visit(ctx.boolexpr(0));
        var right = (BoolExpr) visit(ctx.boolexpr(1));
        return new BoolExpr(left.or(right));
    }

    @Override
    public BoolExpr visitBooleanFalse(SFMLParser.BooleanFalseContext ctx) {
        return new BoolExpr(__ -> false);
    }

    @Override
    public BoolExpr visitBooleanParen(SFMLParser.BooleanParenContext ctx) {
        return (BoolExpr) visit(ctx.boolexpr());
    }

    @Override
    public BoolExpr visitBooleanNegation(SFMLParser.BooleanNegationContext ctx) {
        var x = (BoolExpr) visit(ctx.boolexpr());
        return new BoolExpr(x.negate());
    }

    @Override
    public Limit visitQuantityRetentionLimit(SFMLParser.QuantityRetentionLimitContext ctx) {
        var quantity = visitQuantity(ctx.quantity());
        var retain   = visitRetention(ctx.retention());
        return new Limit(quantity.value(), retain.value());
    }

    @Override
    public Matchers visitInputmatchers(SFMLParser.InputmatchersContext ctx) {
        if (ctx == null) return new Matchers(List.of(new ItemLimit(new Limit(Integer.MAX_VALUE, 0))));
        return ((Matchers) visit(ctx.itemmovement())).withDefaults(Integer.MAX_VALUE, 0);
    }

    @Override
    public Matchers visitOutputmatchers(SFMLParser.OutputmatchersContext ctx) {
        if (ctx == null) return new Matchers(List.of(new ItemLimit(new Limit(Integer.MAX_VALUE, Integer.MAX_VALUE))));
        return ((Matchers) visit(ctx.itemmovement())).withDefaults(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    @Override
    public Matchers visitItemLimitMovement(SFMLParser.ItemLimitMovementContext ctx) {
        return new Matchers(ctx.itemlimit().stream()
                                    .map(this::visitItemlimit)
                                    .collect(Collectors.toList()));
    }

    @Override
    public Matchers visitLimitMovement(SFMLParser.LimitMovementContext ctx) {
        return new Matchers(List.of(new ItemLimit((Limit) this.visit(ctx.limit()))));
    }

    @Override
    public ASTNode visitItemNoLimitMovement(SFMLParser.ItemNoLimitMovementContext ctx) {
        return new Matchers(ctx
                                    .item()
                                    .stream()
                                    .map(this::visitItem)
                                    .map(ItemLimit::new)
                                    .collect(Collectors.toList()));
    }

    @Override
    public ItemLimit visitItemlimit(SFMLParser.ItemlimitContext ctx) {
        var limit = (Limit) visit(ctx.limit());
        var item  = (ItemIdentifier) visitItem(ctx.item());
        return new ItemLimit(limit, item);
    }

    @Override
    public NumberRangeSet visitSlotqualifier(SFMLParser.SlotqualifierContext ctx) {
        return visitRangeset(ctx == null ? null : ctx.rangeset());
    }

    @Override
    public NumberRangeSet visitRangeset(SFMLParser.RangesetContext ctx) {
        if (ctx == null) return new NumberRangeSet(List.of(new NumberRange(Integer.MIN_VALUE, Integer.MAX_VALUE)));
        return new NumberRangeSet(ctx.range().stream().map(this::visitRange).collect(Collectors.toList()));
    }

    @Override
    public NumberRange visitRange(SFMLParser.RangeContext ctx) {
        var iter  = ctx.number().stream().map(this::visitNumber).mapToInt(Quantity::value).iterator();
        var start = iter.next();
        if (iter.hasNext()) {
            var end = iter.next();
            return new NumberRange(start, end);
        } else {
            return new NumberRange(start, start);
        }
    }


    @Override
    public Limit visitRetentionLimit(SFMLParser.RetentionLimitContext ctx) {
        var retain = visitRetention(ctx.retention());
        return new Limit(-1, retain.value());
    }

    @Override
    public Limit visitQuantityLimit(SFMLParser.QuantityLimitContext ctx) {
        var quantity = visitQuantity(ctx.quantity());
        return new Limit(quantity.value(), -1);
    }

    @Override
    public Quantity visitRetention(SFMLParser.RetentionContext ctx) {
        if (ctx == null) return new Quantity(-1);
        return visitNumber(ctx.number());
    }

    @Override
    public Quantity visitQuantity(SFMLParser.QuantityContext ctx) {
        if (ctx == null) return new Quantity(Integer.MAX_VALUE);
        return visitNumber(ctx.number());
    }

    @Override
    public DirectionQualifier visitSidequalifier(SFMLParser.SidequalifierContext ctx) {
        if (ctx == null) return new DirectionQualifier(Stream.empty());
        var sides = ctx.side().stream().map(this::visitSide);
        return new DirectionQualifier(sides);
    }

    @Override
    public Side visitSide(SFMLParser.SideContext ctx) {
        return Side.valueOf(ctx.getText().toUpperCase(Locale.ROOT));
    }

    @Override
    public Block visitBlock(SFMLParser.BlockContext ctx) {
        var statements = ctx
                .statement()
                .stream()
                .map(this::visit)
                .map(Statement.class::cast)
                .collect(Collectors.toList());
        return new Block(statements);
    }
}
