package ca.teamdman.sfm.common.program;

import ca.teamdman.sfm.common.localization.LocalizationKeys;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@SuppressWarnings("UnusedReturnValue")
public record LabelPositionHolder(Map<String, HashSet<BlockPos>> labels) {
    private final static WeakHashMap<ItemStack, LabelPositionHolder> CACHE = new WeakHashMap<>();

    private LabelPositionHolder() {
        this(new HashMap<>());
    }

    private LabelPositionHolder(LabelPositionHolder other) {
        this();
        other.labels().forEach((key, value) -> this.labels().put(key, new HashSet<>(value)));
    }


    /**
     * Get the label position holder for this disk.
     * <p>
     * Saves it in the cache for faster future lookups.
     * <p>
     * This mutably borrows the cache entry.
     */
    public static LabelPositionHolder from(ItemStack stack) {
        return CACHE.computeIfAbsent(stack, s -> {
            var tag = stack.getOrCreateTag().getCompound("sfm:labels");
            return deserialize(tag);
        });
    }

    public static LabelPositionHolder empty() {
        return new LabelPositionHolder();
    }

    public static LabelPositionHolder deserialize(CompoundTag tag) {
        var labels = LabelPositionHolder.empty();
        for (var label : tag.getAllKeys()) {
            // old: storing BlockPos as long
            labels.addAll(label, tag.getList(label, Tag.TAG_LONG).stream()
                    .map(LongTag.class::cast)
                    .mapToLong(LongTag::getAsLong)
                    .mapToObj(BlockPos::of).collect(Collectors.toList()));

            // new: storing BlockPos as compound
            labels.addAll(label, tag.getList(label, Tag.TAG_COMPOUND).stream()
                    .map(CompoundTag.class::cast)
                    .map(NbtUtils::readBlockPos)
                    .collect(Collectors.toList()));
        }
        return labels;
    }

    public LabelPositionHolder save(ItemStack stack) {
        stack.getOrCreateTag().put("sfm:labels", serialize());
        CACHE.put(stack, new LabelPositionHolder(this));
        return this;
    }

    public static void purge(ItemStack stack) {
        stack.getOrCreateTag().remove("sfm:labels");
        CACHE.remove(stack);
    }

    public CompoundTag serialize() {
        var tag = new CompoundTag();
        for (var entry : labels().entrySet()) {
            var list = new ListTag();
            list.addAll(entry.getValue()
                                .stream()
                                .map(NbtUtils::writeBlockPos)
                                .toList());
            tag.put(entry.getKey(), list);
        }
        return tag;
    }

    public boolean contains(
            String label,
            BlockPos pos
    ) {
        HashSet<BlockPos> positionsForLabel = this.labels().get(label);
        if (positionsForLabel == null) {
            return false;
        } else {
            return positionsForLabel.contains(pos);
        }
    }

    public LabelPositionHolder toggle(
            String label,
            BlockPos pos
    ) {
        if (contains(label, pos)) {
            remove(label, pos);
        } else {
            add(label, pos);
        }
        return this;
    }

    public Set<BlockPos> getPositions(String label) {
        return labels().getOrDefault(label, new HashSet<>());
    }

    public Set<BlockPos> getPositionsMut(String label) {
        return labels().computeIfAbsent(label, s -> new HashSet<>());
    }

    public LabelPositionHolder addAll(
            String label,
            Collection<BlockPos> positions
    ) {
        getPositionsMut(label).addAll(positions);
        return this;
    }

    public LabelPositionHolder addReferencedLabel(String label) {
        getPositionsMut(label);
        return this;
    }

    public List<Component> asHoverText() {
        var rtn = new ArrayList<Component>();
        if (labels().isEmpty()) return rtn;
        rtn.add(LocalizationKeys.DISK_ITEM_TOOLTIP_LABEL_HEADER
                        .getComponent()
                        .withStyle(ChatFormatting.UNDERLINE));
        for (var entry : labels().entrySet()) {
            rtn.add(LocalizationKeys.DISK_ITEM_TOOLTIP_LABEL.getComponent(
                    entry.getKey(),
                    entry.getValue().size()
            ).withStyle(ChatFormatting.GRAY));
        }
        return rtn;
    }

    public LabelPositionHolder removeAll(BlockPos value) {
        labels().values().forEach(list -> list.remove(value));
        return this;
    }

    public LabelPositionHolder prune() {
        labels().entrySet().removeIf(entry -> entry.getValue().isEmpty());
        return this;
    }

    public LabelPositionHolder clear() {
        labels().clear();
        return this;
    }

    public LabelPositionHolder add(
            String label,
            BlockPos position
    ) {
        getPositionsMut(label).add(position);
        return this;
    }

    public LabelPositionHolder remove(
            String label,
            BlockPos pos
    ) {
        getPositionsMut(label).remove(pos);
        return this;
    }

    public LabelPositionHolder removeIf(BiPredicate<String, BlockPos> predicate) {
        labels().forEach((key, value) -> value.removeIf(pos -> predicate.test(key, pos)));
        return this;
    }

    public LabelPositionHolder removeIf(Predicate<String> predicate) {
        labels().keySet().removeIf(predicate);
        return this;
    }

    public LabelPositionHolder forEach(BiConsumer<String, BlockPos> consumer) {
        labels().forEach((key, value) -> value.forEach(pos -> consumer.accept(key, pos)));
        return this;
    }

    @Override
    public String toString() {
        return "LabelPositionHolder{size=" + labels().values().stream().mapToInt(Set::size).sum() + "; " +
               labels()
                       .entrySet()
                       .stream()
                       .map(entry -> entry.getKey() + "=" + entry.getValue().size())
                       .collect(Collectors.joining(", ")) +
               "}";
    }

    public LabelPositionHolder toOwned() {
        return new LabelPositionHolder(this);
    }

    public Set<String> getLabels(BlockPos pos) {
        return labels().entrySet().stream()
                .filter(entry -> entry.getValue().contains(pos))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public boolean isEmpty() {
        return labels().isEmpty();
    }
}
